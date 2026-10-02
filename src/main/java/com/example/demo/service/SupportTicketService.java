package com.example.demo.service;

import com.example.demo.Repository.SupportTicketRepository;
import com.example.demo.Repository.TicketMessageRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.dto.admin.SupportTicketCreateRequest;
import com.example.demo.dto.admin.TicketReplyRequest;
import com.example.demo.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final UserRepository userRepository;
    private final AdminService adminService;

    @Transactional
    public SupportTicket createTicket(User user, SupportTicketCreateRequest request) {
        String code = "TK-" + (System.currentTimeMillis() % 1000000);
        SupportTicket ticket = SupportTicket.builder()
                .ticketCode(code)
                .user(user)
                .subject(request.getSubject())
                .description(request.getDescription())
                .category(request.getCategory() != null ? request.getCategory() : TicketCategory.OTHER)
                .priority(request.getPriority() != null ? request.getPriority() : TicketPriority.MEDIUM)
                .status(TicketStatus.OPEN)
                .build();

        SupportTicket savedTicket = supportTicketRepository.save(ticket);

        // Lưu tin nhắn đầu tiên chính là mô tả ban đầu
        TicketMessage initialMessage = TicketMessage.builder()
                .ticket(savedTicket)
                .sender(user)
                .content(request.getDescription())
                .isInternalNote(false)
                .build();
        ticketMessageRepository.save(initialMessage);

        return savedTicket;
    }

    public List<SupportTicket> getUserTickets(Long userId) {
        return supportTicketRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Page<SupportTicket> getAllTickets(TicketStatus status, Pageable pageable) {
        if (status != null) {
            return supportTicketRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        }
        return supportTicketRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public SupportTicket getTicketById(Long id) {
        return supportTicketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Ticket hỗ trợ với ID: " + id));
    }

    public List<TicketMessage> getTicketMessages(Long ticketId, boolean isAdmin) {
        if (isAdmin) {
            return ticketMessageRepository.findByTicketIdOrderBySentAtAsc(ticketId);
        } else {
            return ticketMessageRepository.findByTicketIdAndIsInternalNoteFalseOrderBySentAtAsc(ticketId);
        }
    }

    @Transactional
    public TicketMessage replyTicket(Long ticketId, User sender, TicketReplyRequest request, boolean isAdmin) {
        SupportTicket ticket = getTicketById(ticketId);

        boolean isInternal = isAdmin && Boolean.TRUE.equals(request.getIsInternalNote());

        TicketMessage message = TicketMessage.builder()
                .ticket(ticket)
                .sender(sender)
                .content(request.getContent())
                .attachmentUrl(request.getAttachmentUrl())
                .isInternalNote(isInternal)
                .sentAt(LocalDateTime.now())
                .build();

        ticketMessageRepository.save(message);

        // Cập nhật trạng thái ticket tự động
        if (!isInternal) {
            if (isAdmin) {
                if (ticket.getStatus() == TicketStatus.OPEN || ticket.getStatus() == TicketStatus.IN_PROGRESS) {
                    ticket.setStatus(TicketStatus.WAITING_USER);
                }
            } else {
                if (ticket.getStatus() == TicketStatus.WAITING_USER) {
                    ticket.setStatus(TicketStatus.IN_PROGRESS);
                }
            }
            ticket.setUpdatedAt(LocalDateTime.now());
            supportTicketRepository.save(ticket);
        }

        return message;
    }

    @Transactional
    public SupportTicket updateTicketStatus(Long ticketId, TicketStatus newStatus, User adminUser, String ipAddress) {
        SupportTicket ticket = getTicketById(ticketId);
        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(newStatus);
        if (newStatus == TicketStatus.RESOLVED || newStatus == TicketStatus.CLOSED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        SupportTicket updated = supportTicketRepository.save(ticket);

        adminService.recordAuditLog(adminUser, "UPDATE_TICKET_STATUS", "TICKET", ticketId,
                String.format("Đổi trạng thái Ticket %s từ %s sang %s", ticket.getTicketCode(), oldStatus, newStatus),
                ipAddress);

        return updated;
    }

    @Transactional
    public SupportTicket assignTicket(Long ticketId, Long staffId, User adminUser, String ipAddress) {
        SupportTicket ticket = getTicketById(ticketId);
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên hỗ trợ"));

        ticket.setAssignedTo(staff);
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setUpdatedAt(LocalDateTime.now());
        SupportTicket updated = supportTicketRepository.save(ticket);

        adminService.recordAuditLog(adminUser, "ASSIGN_TICKET", "TICKET", ticketId,
                String.format("Giao Ticket %s cho nhân viên %s", ticket.getTicketCode(), staff.getEmail()),
                ipAddress);

        return updated;
    }
}
