package org.paulina.mendez.service;

import java.math.BigDecimal;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.paulina.mendez.domain.Ticket;
import org.paulina.mendez.gateway.SolicitudTicketDTO;
import org.paulina.mendez.gateway.TicketDTO;
import org.paulina.mendez.repository.TicketRegister;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({MockitoExtension.class})
class SistemaDeTicketsTest {
    @Mock
    SolicitudTicketDTO solicitudDTO;
    @Mock
    TicketDTO ticketDTO;
    @Mock
    TicketRegister registro;
    @InjectMocks
    SistemaDeTickets sistema;

    @Nested
    class crearYConfirmarReserva {
        crearYConfirmarReserva() {
            Objects.requireNonNull(SistemaDeTicketsTest.this);
            super();
        }

        @Test
        @DisplayName("crearYConfirmarReserva Crea Un Ticket")
        void crearYConfirmar() {
            
        }
    }
}