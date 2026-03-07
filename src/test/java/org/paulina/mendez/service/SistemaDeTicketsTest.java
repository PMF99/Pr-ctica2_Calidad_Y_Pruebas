package org.paulina.mendez.service;

import java.time.Instant;
import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.paulina.mendez.domain.Ticket;
import org.paulina.mendez.domain.*;
import org.paulina.mendez.gateway.SolicitudTicketDTO;
import org.paulina.mendez.gateway.TicketDTO;
import org.paulina.mendez.repository.TicketRegister;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
class SistemaDeTicketsTest {
    @Mock
    SolicitudTicketDTO solicitudDTO;
    @Mock
    TicketDTO ticketDTO;
    @Mock
    TicketRegister registro;
    @Mock
    GeneradorID ids;
    @Mock
    NotificadorTickets notificador;
    @Mock
    Reloj reloj;

    @InjectMocks

    SistemaDeTickets sistema;

    @Nested
    class crearTicket {
        crearTicket() {
            Objects.requireNonNull(SistemaDeTicketsTest.this);
            super();
        }

        @Test
        @DisplayName("crearTicket Crea Un Ticket")
        void crear() {
            SolicitudTicket solicitudTicket = new SolicitudTicket("Usher1", "Compra", "Pago efectivo");
            Instant ahora = Instant.now();
            String idEsperado = "TK-123";
            when(ticketDTO.aDominio(solicitudDTO)).thenReturn(solicitudTicket);
            when(ids.nuevoId()).thenReturn(idEsperado);
            when(reloj.ahora()).thenReturn(ahora);
            Ticket resultado = sistema.crear(solicitudDTO);
            assertNotNull(resultado);
            assertEquals(idEsperado, resultado.ticketId());
            assertEquals(EstadoTicket.CREADO, resultado.estado());
            verify(registro).guardar(resultado);
            verify(notificador).notificarCreacion("Usher1", idEsperado);
        }

        @Test
        @DisplayName("crearTicket Lanza Excepción Cuando El DTO Es Inválido")
        void ExcepcionCuandoDtoInvalido() {
            when(ticketDTO.aDominio(solicitudDTO)).thenThrow(new IllegalArgumentException("El UsuarioId No Puede Ser Nulo O Vacío"));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> sistema.crear(solicitudDTO));
            assertEquals("El UsuarioId No Puede Ser Nulo O Vacío", ex.getMessage());
            verifyNoInteractions(registro);
            verifyNoInteractions(notificador);
        }

        @Test
        @DisplayName("crearTicket Debe Normalizar Espacios En Blanco En El DTO")
        void normalizaEspacios() {
            SolicitudTicketDTO dtoConEspacios = new SolicitudTicketDTO("  User123  ", " Falla ", " Desc ");
            SolicitudTicket solicitudNormalizada = new SolicitudTicket("User123", "Falla", "Desc");
            when(ticketDTO.aDominio(dtoConEspacios)).thenReturn(solicitudNormalizada);
            when(ids.nuevoId()).thenReturn("TK-1");
            when(reloj.ahora()).thenReturn(Instant.now());
            Ticket resultado = sistema.crear(dtoConEspacios);
            assertEquals("User123", resultado.usuarioId());
            verify(notificador).notificarCreacion("User123", "TK-1");
        }

        @Test
        @DisplayName("crearTicket Debe Fallar Si El Repositorio Lanza Una Excepción")
        void ExcepcionSiRepoFalla() {
            when(ticketDTO.aDominio(any())).thenReturn(new SolicitudTicket("U1", "A", "D"));
            when(ids.nuevoId()).thenReturn("ID-ERR");
            when(reloj.ahora()).thenReturn(Instant.now());
            doThrow(new RuntimeException("Error de persistencia")).when(registro).guardar(any());
            assertThrows(RuntimeException.class, () -> sistema.crear(solicitudDTO));
            verifyNoInteractions(notificador);
        }

        @Test
        @DisplayName("crearTicket Debe Registrar La Hora Exacta Del Reloj")
        void usarHoraDelReloj() {
            Instant horaFija = Instant.parse("2023-10-01T10:00:00Z");
            when(ticketDTO.aDominio(any())).thenReturn(new SolicitudTicket("U1", "A", "D"));
            when(ids.nuevoId()).thenReturn("TK-TIME");
            when(reloj.ahora()).thenReturn(horaFija);
            Ticket resultado = sistema.crear(solicitudDTO);
            assertEquals(horaFija, resultado.creadoEn());
            assertEquals(horaFija, resultado.actualizadoEn());
            assertEquals(resultado.creadoEn(), resultado.actualizadoEn());
        }
    }

    @Nested
    class asignarTicket {
        @Test
        @DisplayName("asignar Cambia El Estado Y Notifica Al Agente")
        void asignarExitosamente() {
            Instant ahora = Instant.now();
            Ticket ticketInicial = new Ticket("TK-01", "Usher1", "Asunto", "Desc", EstadoTicket.CREADO, null, ahora, ahora);
            String agenteId = "Agente_Bond";
            when(reloj.ahora()).thenReturn(ahora.plusSeconds(60));
            Ticket resultado = sistema.asignar(ticketInicial, agenteId);
            assertEquals(EstadoTicket.ASIGNADO, resultado.estado());
            assertEquals(agenteId, resultado.agenteId());
            verify(registro).actualizar(resultado);
            verify(notificador).notificarAsignacion("Usher1", "TK-01", agenteId);
        }

        @Test
        @DisplayName("asignar falla si el ID del agente es solo espacios")
        void errorAgenteIdVacio() {
            Instant ahora = Instant.now();
            Ticket ticket = new Ticket("TK-01", "U1", "A", "D", EstadoTicket.CREADO, null, ahora, ahora);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> sistema.asignar(ticket, "   "));
            assertEquals("El AgenteId No Puede Ser Nulo O Vacío", ex.getMessage());
        }

        @Test
        @DisplayName("asignar Debe Actualizar La Fecha De ActualizadoEn")
        void actFechaModificacion() {
            Instant fechaCreacion = Instant.parse("2023-10-01T10:00:00Z");
            Instant fechaAsignacion = Instant.parse("2023-10-01T10:15:00Z");
            Ticket ticketOriginal = new Ticket("TK-01", "U1", "A", "D", EstadoTicket.CREADO, null, fechaCreacion, fechaCreacion);
            when(reloj.ahora()).thenReturn(fechaAsignacion);
            Ticket resultado = sistema.asignar(ticketOriginal, "Agente1");
            assertEquals(fechaCreacion, resultado.creadoEn());
            assertEquals(fechaAsignacion, resultado.actualizadoEn());
            assertNotEquals(resultado.creadoEn(), resultado.actualizadoEn());
        }

        @Test
        @DisplayName("asignar No Debe Modificar El Objeto Ticket Original (Inmutabilidad)")
        void verificarInmutabilidadDelRecord() {
            Instant ahora = Instant.now();
            Ticket ticketOriginal = new Ticket("TK-01", "U1", "A", "D", EstadoTicket.CREADO, null, ahora, ahora);
            when(reloj.ahora()).thenReturn(ahora.plusSeconds(5));
            Ticket resultado = sistema.asignar(ticketOriginal, "Agente2");
            assertNotSame(ticketOriginal, resultado);
            assertEquals(EstadoTicket.CREADO, ticketOriginal.estado());
            assertNull(ticketOriginal.agenteId());
            assertEquals(EstadoTicket.ASIGNADO, resultado.estado());
        }

        @Test
        @DisplayName("asignar Debe Persistir El Ticket Con El Estado Correcto En El Repositorio")
        void verificarObjeto() {
            Instant ahora = Instant.now();
            Ticket ticketOriginal = new Ticket("TK-100", "User1", "Asunto", "Desc", EstadoTicket.CREADO, null, ahora, ahora);
            String agenteId = "Agente_XYZ";
            Instant fechaAsignacion = ahora.plusSeconds(30);
            when(reloj.ahora()).thenReturn(fechaAsignacion);
            ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
            sistema.asignar(ticketOriginal, agenteId);
            verify(registro).actualizar(ticketCaptor.capture());
            Ticket ticketPersistido = ticketCaptor.getValue();
            assertEquals(EstadoTicket.ASIGNADO, ticketPersistido.estado());
            assertEquals("Agente_XYZ", ticketPersistido.agenteId());
            assertEquals(fechaAsignacion, ticketPersistido.actualizadoEn());
            assertEquals("TK-100", ticketPersistido.ticketId());
        }
    }

    @Nested
    class cerrarTicket {

        @Test
        @DisplayName("cerrar Cambia El Estado Del Ticket A CERRADO")
        void cerrarExitosamente() {
            Instant ahora = Instant.now();
            Ticket ticket = new Ticket("TK-01","User1",
                    "Asunto", "Desc",EstadoTicket.ASIGNADO,
                    "Agente1",ahora,ahora);
            Instant cierre = ahora.plusSeconds(60);
            when(reloj.ahora()).thenReturn(cierre);
            Ticket resultado = sistema.cerrar(ticket);
            assertEquals(EstadoTicket.CERRADO, resultado.estado());
            verify(registro).actualizar(resultado);
            verify(notificador).notificarCierre("User1","TK-01");
        }

        @Test
        @DisplayName("cerrar lanza excepción si el ticket es nulo")
        void errorTicketNulo() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> sistema.cerrar(null)
            );
            assertEquals("El Ticket No Puede Ser Nulo", ex.getMessage());
        }

        @Test
        @DisplayName("cerrar Debe Actualizar La Fecha actualizadoEn")
        void actualizarFecha() {
            Instant creado = Instant.parse("2023-10-01T10:00:00Z");
            Instant cierre = Instant.parse("2023-10-01T10:15:00Z");
            Ticket ticket = new Ticket("TK-02","User1",
                    "A","D",EstadoTicket.ASIGNADO,
                    "Agente1",creado,creado);
            when(reloj.ahora()).thenReturn(cierre);
            Ticket resultado = sistema.cerrar(ticket);
            assertEquals(creado, resultado.creadoEn());
            assertEquals(cierre, resultado.actualizadoEn());
        }

        @Test
        @DisplayName("cerrar No Debe Modificar El Ticket Original")
        void verificarInmutabilidad() {
            Instant ahora = Instant.now();
            Ticket original = new Ticket("TK-03","User1",
                    "A","D",EstadoTicket.ASIGNADO,
                    "Agente1",ahora,ahora);
            when(reloj.ahora()).thenReturn(ahora.plusSeconds(30));
            Ticket resultado = sistema.cerrar(original);
            assertNotSame(original, resultado);
            assertEquals(EstadoTicket.ASIGNADO, original.estado());
            assertEquals(EstadoTicket.CERRADO, resultado.estado());
        }

        @Test
        @DisplayName("cerrar Debe Persistir El Ticket Cerrado En El Repositorio")
        void verificarPersistencia() {
            Instant ahora = Instant.now();
            Instant cierre = ahora.plusSeconds(20);
            Ticket ticket = new Ticket("TK-04","User1",
                    "A","D",EstadoTicket.ASIGNADO,
                    "Agente1",ahora,ahora);
            when(reloj.ahora()).thenReturn(cierre);
            ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
            sistema.cerrar(ticket);
            verify(registro).actualizar(captor.capture());
            Ticket persistido = captor.getValue();
            assertEquals(EstadoTicket.CERRADO, persistido.estado());
            assertEquals("TK-04", persistido.ticketId());
            assertEquals(cierre, persistido.actualizadoEn());
        }
    }

    @Nested
    class cancelarTicket {

        @Test
        @DisplayName("cancelar Notifica Correctamente La Cancelación")
        void cancelarCorrectamente() {
            sistema.cancelar("User1","TK-10","No es necesario");
            verify(notificador).notificarCancelacion("User1",
                    "TK-10","No es necesario");
        }

        @Test
        @DisplayName("cancelar lanza excepción si usuarioId es nulo")
        void errorUsuarioIdNulo() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> sistema.cancelar(null,"TK-10","Motivo")
            );
            assertEquals("El UsuarioId No Puede Ser Nulo O Vacío", ex.getMessage());
        }

        @Test
        @DisplayName("cancelar lanza excepción si usuarioId está vacío")
        void errorUsuarioIdVacio() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> sistema.cancelar("   ","TK-10","Motivo")
            );
            assertEquals("El UsuarioId No Puede Ser Nulo O Vacío", ex.getMessage());
        }

        @Test
        @DisplayName("cancelar lanza excepción si ticketId es vacío")
        void errorTicketIdVacio() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> sistema.cancelar("User1","   ","Motivo")
            );
            assertEquals("El TicketId No Puede Ser Nulo O Vacío", ex.getMessage());
        }

        @Test
        @DisplayName("cancelar lanza excepción si motivo es vacío")
        void errorMotivoVacio() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> sistema.cancelar("User1","TK-10","   ")
            );
            assertEquals("El Motivo No Puede Ser Nulo O Vacío", ex.getMessage());
        }
    }
}