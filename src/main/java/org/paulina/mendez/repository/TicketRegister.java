package org.paulina.mendez.repository;

import org.paulina.mendez.domain.Ticket;

public interface TicketRegister {
    void guardar(Ticket ticket);
    void actualizar(Ticket ticket);
}
