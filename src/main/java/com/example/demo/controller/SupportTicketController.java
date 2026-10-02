package com.example.demo.controller;

import com.example.demo.Repository.UserRepository;
import com.example.demo.dto.admin.SupportTicketCreateRequest;
import com.example.demo.dto.admin.TicketReplyRequest;
import com.example.demo.model.*;
import com.example.demo.service.SupportTicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class SupportTicketController {

    private final SupportTicketService supportTicketService;
    private final UserRepository userRepository;

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Chưa đăng nhập");
        }
        return userRepository.findByUsername(principal.getName())
                .or(() -> userRepository.findByEmail(principal.getName()))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    private User getAuthenticatedAdmin(Principal principal) {
        User user = getAuthenticatedUser(principal);
        if (user.getRole() != UserRole.ROLE_ADMIN && user.getRole() != UserRole.ROLE_SUPPORT) {
            throw new SecurityException("Bạn không có quyền thực hiện thao tác này!");
        }
        return user;
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // ==========================================
    // KHU VỰC DÀNH CHO NGƯỜI DÙNG (USER API)
    // ==========================================

    @PostMapping("/api/support/tickets")
    public ResponseEntity<?> createTicket(
            @Valid @RequestBody SupportTicketCreateRequest request,
            Principal principal) {
        try {
            User user = getAuthenticatedUser(principal);
            SupportTicket ticket = supportTicketService.createTicket(user, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/api/support/my-tickets")
    public ResponseEntity<?> getMyTickets(Principal principal) {
        try {
            User user = getAuthenticatedUser(principal);
            return ResponseEntity.ok(supportTicketService.getUserTickets(user.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/api/support/tickets/{id}")
    public ResponseEntity<?> getTicketDetails(@PathVariable Long id, Principal principal) {
        try {
            User user = getAuthenticatedUser(principal);
            SupportTicket ticket = supportTicketService.getTicketById(id);

            boolean isAdmin = user.getRole() == UserRole.ROLE_ADMIN || user.getRole() == UserRole.ROLE_SUPPORT;
            if (!isAdmin && !ticket.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền xem ticket này"));
            }

            List<TicketMessage> messages = supportTicketService.getTicketMessages(id, isAdmin);
            Map<String, Object> result = new HashMap<>();
            result.put("ticket", ticket);
            result.put("messages", messages);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/api/support/tickets/{id}/reply")
    public ResponseEntity<?> replyMyTicket(
            @PathVariable Long id,
            @Valid @RequestBody TicketReplyRequest request,
            Principal principal) {
        try {
            User user = getAuthenticatedUser(principal);
            SupportTicket ticket = supportTicketService.getTicketById(id);
            if (!ticket.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền trả lời ticket này"));
            }
            // User reply cannot be internal note
            request.setIsInternalNote(false);
            TicketMessage msg = supportTicketService.replyTicket(id, user, request, false);
            return ResponseEntity.ok(msg);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ==========================================
    // KHU VỰC DÀNH CHO ADMIN / SUPPORT
    // ==========================================

    @GetMapping("/api/admin/support/tickets")
    public ResponseEntity<?> getAllTicketsForAdmin(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Principal principal) {
        try {
            getAuthenticatedAdmin(principal);
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(supportTicketService.getAllTickets(status, pageable));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/api/admin/support/tickets/{id}/reply")
    public ResponseEntity<?> replyTicketAsAdmin(
            @PathVariable Long id,
            @Valid @RequestBody TicketReplyRequest request,
            Principal principal) {
        try {
            User admin = getAuthenticatedAdmin(principal);
            TicketMessage msg = supportTicketService.replyTicket(id, admin, request, true);
            return ResponseEntity.ok(msg);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/api/admin/support/tickets/{id}/status")
    public ResponseEntity<?> updateTicketStatus(
            @PathVariable Long id,
            @RequestParam TicketStatus status,
            HttpServletRequest servletRequest,
            Principal principal) {
        try {
            User admin = getAuthenticatedAdmin(principal);
            String ip = getClientIp(servletRequest);
            return ResponseEntity.ok(supportTicketService.updateTicketStatus(id, status, admin, ip));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/api/admin/support/tickets/{id}/assign")
    public ResponseEntity<?> assignTicket(
            @PathVariable Long id,
            @RequestParam Long staffId,
            HttpServletRequest servletRequest,
            Principal principal) {
        try {
            User admin = getAuthenticatedAdmin(principal);
            String ip = getClientIp(servletRequest);
            return ResponseEntity.ok(supportTicketService.assignTicket(id, staffId, admin, ip));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
