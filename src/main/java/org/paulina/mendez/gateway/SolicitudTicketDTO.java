package org.paulina.mendez.gateway;

public record SolicitudTicketDTO(
        String usuarioId,
        String asunto,
        String descripcion
) {}
