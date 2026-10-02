package com.example.demo.controller;

import com.example.demo.Repository.UserRepository;
import com.example.demo.dto.admin.AnnouncementRequest;
import com.example.demo.model.SystemAnnouncement;
import com.example.demo.model.User;
import com.example.demo.model.UserRole;
import com.example.demo.service.AnnouncementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final UserRepository userRepository;

    private User getAuthenticatedAdmin(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Chưa đăng nhập");
        }
        User user = userRepository.findByUsername(principal.getName())
                .or(() -> userRepository.findByEmail(principal.getName()))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (user.getRole() != UserRole.ROLE_ADMIN) {
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

    /**
     * Lấy các thông báo đang kích hoạt (Người dùng thường cũng xem được)
     */
    @GetMapping("/api/announcements/active")
    public ResponseEntity<?> getActiveAnnouncements() {
        return ResponseEntity.ok(announcementService.getActiveAnnouncements());
    }

    /**
     * Lấy tất cả thông báo cho Admin
     */
    @GetMapping("/api/admin/announcements")
    public ResponseEntity<?> getAllAnnouncements(Principal principal) {
        try {
            getAuthenticatedAdmin(principal);
            return ResponseEntity.ok(announcementService.getAllAnnouncements());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Tạo thông báo mới
     */
    @PostMapping("/api/admin/announcements")
    public ResponseEntity<?> createAnnouncement(
            @Valid @RequestBody AnnouncementRequest request,
            HttpServletRequest servletRequest,
            Principal principal) {
        try {
            User admin = getAuthenticatedAdmin(principal);
            String ip = getClientIp(servletRequest);
            SystemAnnouncement announcement = announcementService.createAnnouncement(admin, request, ip);
            return ResponseEntity.status(HttpStatus.CREATED).body(announcement);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Bật / tắt thông báo
     */
    @PutMapping("/api/admin/announcements/{id}/toggle")
    public ResponseEntity<?> toggleAnnouncement(
            @PathVariable Long id,
            HttpServletRequest servletRequest,
            Principal principal) {
        try {
            User admin = getAuthenticatedAdmin(principal);
            String ip = getClientIp(servletRequest);
            return ResponseEntity.ok(announcementService.toggleAnnouncement(id, admin, ip));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Xóa thông báo
     */
    @DeleteMapping("/api/admin/announcements/{id}")
    public ResponseEntity<?> deleteAnnouncement(
            @PathVariable Long id,
            HttpServletRequest servletRequest,
            Principal principal) {
        try {
            User admin = getAuthenticatedAdmin(principal);
            String ip = getClientIp(servletRequest);
            announcementService.deleteAnnouncement(id, admin, ip);
            return ResponseEntity.ok(Map.of("message", "Đã xóa thông báo thành công"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
