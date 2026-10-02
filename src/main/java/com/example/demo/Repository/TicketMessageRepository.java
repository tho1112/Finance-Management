package com.example.demo.Repository;

import com.example.demo.model.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {

    List<TicketMessage> findByTicketIdOrderBySentAtAsc(Long ticketId);

    List<TicketMessage> findByTicketIdAndIsInternalNoteFalseOrderBySentAtAsc(Long ticketId);
}
