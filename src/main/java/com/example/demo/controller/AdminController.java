package com.example.demo.controller;

import com.example.demo.Repository.UserRepository;
import com.example.demo.dto.admin.*;
import com.example.demo.model.User;
import com.example.demo.model.UserRole;
import com.example.demo.model.UserStatus;
import com.example.demo.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;

    private User getAuthenticatedAdmin(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Chưa xác thực người dùng");
        }
        User user = userRepository.findByUsername(principal.getName())
                .or(() -> userRepository.findByEmail(principal.getName()))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (user.getRole() != UserRole.ROLE_ADMIN) {
            throw new SecurityException("Bạn không có quyền truy cập trang Quản trị (Admin)!");
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
     * 1. LẤY THỐNG KÊ DASHBOARD QUẢN TRỊ
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats(Principal principal) {
        try {
            getAuthenticatedAdmin(principal);
            AdminDashboardStatsResponse stats = adminService.getDashboardStats();
            return ResponseEntity.ok(stats);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi lấy thống kê admin: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 2. DANH SÁCH NGƯỜI DÙNG CÓ TÌM KIẾM, LỌC VÀ PHÂN TRANG
     */
    @GetMapping("/users")
    public ResponseEntity<?> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) UserRole role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            Principal principal) {
        try {
            getAuthenticatedAdmin(principal);

            Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            return ResponseEntity.ok(adminService.searchUsers(keyword, status, role, pageable));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 3. XEM CHI TIẾT NGƯỜI DÙNG (Tài khoản ví, giao dịch, lịch sử truy cập)
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserDetail(@PathVariable Long id, Principal principal) {
        try {
            getAuthenticatedAdmin(principal);
            return ResponseEntity.ok(adminService.getUserDetail(id));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 4. CẬP NHẬT TRẠNG THÁI NGƯỜI DÙNG (Khóa / Mở khóa / Đình chỉ)
     */
    @PutMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request,
            HttpServletRequest servletRequest,
            Principal principal) {
        try {
            User admin = getAuthenticatedAdmin(principal);
            String ip = getClientIp(servletRequest);
            return ResponseEntity.ok(adminService.updateUserStatus(admin, id, request.getStatus(), request.getReason(), ip));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 5. CẬP NHẬT VAI TRÒ NGƯỜI DÙNG (ROLE_USER, ROLE_SUPPORT, ROLE_ADMIN)
     */
    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleUpdateRequest request,
            HttpServletRequest servletRequest,
            Principal principal) {
        try {
            User admin = getAuthenticatedAdmin(principal);
            String ip = getClientIp(servletRequest);
            return ResponseEntity.ok(adminService.updateUserRole(admin, id, request.getRole(), ip));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 6. LẤY LỊCH SỬ TRUY CẬP HỆ THỐNG
     */
    @GetMapping("/access-logs")
    public ResponseEntity<?> getAccessLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Principal principal) {
        try {
            getAuthenticatedAdmin(principal);
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(adminService.getAccessLogs(pageable));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 7. LẤY NHẬT KÝ KIỂM TOÁN (AUDIT LOGS)
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Principal principal) {
        try {
            getAuthenticatedAdmin(principal);
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(adminService.getAuditLogs(pageable));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
