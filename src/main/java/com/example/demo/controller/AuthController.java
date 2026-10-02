package com.example.demo.controller;

import com.example.demo.Repository.UserRepository;
import com.example.demo.dto.GoogleLoginRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.SendOtpRequest;
import com.example.demo.dto.UserProfileUpdateRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.model.User;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.AuthService;
import com.example.demo.service.GoogleAuthService;
import com.example.demo.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final OtpService otpService;
    private final GoogleAuthService googleAuthService;
    private final com.example.demo.Repository.LoginHistoryRepository loginHistoryRepository;

    /**
     * 0. API GỬI MÃ OTP XÁC THỰC EMAIL THẬT
     */
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody SendOtpRequest request, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("message", result.getAllErrors().get(0).getDefaultMessage()));
        }

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email này đã được sử dụng! Vui lòng chọn email khác hoặc đăng nhập."));
        }

        try {
            otpService.generateAndSendOtp(email);
            return ResponseEntity.ok(Map.of(
                    "message", "Mã xác thực OTP đã được gửi đến " + email + ". Vui lòng kiểm tra hộp thư (hoặc console server)!",
                    "email", email
            ));
        } catch (Exception e) {
            log.error("Lỗi khi gửi OTP: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi gửi mã OTP: " + e.getMessage()));
        }
    }

    /**
     * API ĐĂNG NHẬP / ĐĂNG KÝ BẰNG GOOGLE OAUTH2
     */
    @PostMapping("/google")
    public ResponseEntity<?> loginWithGoogle(
            @RequestBody GoogleLoginRequest request,
            jakarta.servlet.http.HttpServletRequest servletRequest) {
        if (request == null || request.getCredential() == null || request.getCredential().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Google Credential Token không được để trống!"));
        }

        try {
            User user = googleAuthService.authenticateGoogleToken(request.getCredential());
            if (user.getStatus() == com.example.demo.model.UserStatus.BANNED) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Tài khoản của bạn đã bị khóa!"));
            }

            user.setLastLoginAt(java.time.LocalDateTime.now());
            user.setFailedLoginAttempts(0);
            userRepository.save(user);

            recordLoginHistory(user, servletRequest, "GOOGLE_OAUTH", true, null);

            String token = jwtTokenProvider.generateToken(user.getUsername() != null ? user.getUsername() : user.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("fullName", user.getFullName());
            response.put("nickname", user.getNickname());
            response.put("email", user.getEmail());
            response.put("gender", user.getGender());
            response.put("avatarUrl", user.getAvatarUrl());
            response.put("currency", user.getCurrency());
            response.put("language", user.getLanguage());
            response.put("authProvider", user.getAuthProvider());
            response.put("userCode", user.getUserCode());
            response.put("role", user.getRole() != null ? user.getRole().name() : "ROLE_USER");
            response.put("status", user.getStatus() != null ? user.getStatus().name() : "ACTIVE");
            response.put("message", "Đăng nhập Google thành công!");

            return ResponseEntity.ok(response);
        } catch (RuntimeException re) {
            log.error("Lỗi xác thực Google OAuth2: {}", re.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", re.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi xử lý Google OAuth2: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi hệ thống khi đăng nhập Google: " + e.getMessage()));
        }
    }

    /**
     * 1. API ĐĂNG KÝ
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("message", result.getAllErrors().get(0).getDefaultMessage()));
        }

        try {
            User registeredUser = authService.register(request);

            UserResponse response = UserResponse.builder()
                    .id(registeredUser.getId())
                    .username(registeredUser.getUsername())
                    .fullName(registeredUser.getFullName())
                    .nickname(registeredUser.getNickname())
                    .email(registeredUser.getEmail())
                    .gender(registeredUser.getGender())
                    .avatarUrl(registeredUser.getAvatarUrl())
                    .authProvider(registeredUser.getAuthProvider())
                    .language(registeredUser.getLanguage())
                    .currency(registeredUser.getCurrency())
                    .createdAt(registeredUser.getCreatedAt())
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Đăng ký thành công!",
                    "user", response
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private void recordLoginHistory(User user, jakarta.servlet.http.HttpServletRequest request, String method, boolean success, String failReason) {
        if (user == null) return;
        try {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = request.getRemoteAddr();
            } else {
                ip = ip.split(",")[0].trim();
            }
            String ua = request.getHeader("User-Agent");
            String device = "Desktop";
            if (ua != null) {
                String uaLower = ua.toLowerCase();
                if (uaLower.contains("mobile") || uaLower.contains("android") || uaLower.contains("iphone")) {
                    device = "Mobile";
                } else if (uaLower.contains("ipad") || uaLower.contains("tablet")) {
                    device = "Tablet";
                }
            }
            com.example.demo.model.LoginHistory history = com.example.demo.model.LoginHistory.builder()
                    .user(user)
                    .ipAddress(ip)
                    .userAgent(ua != null && ua.length() > 500 ? ua.substring(0, 500) : ua)
                    .deviceType(device)
                    .authMethod(method)
                    .isSuccess(success)
                    .failureReason(failReason)
                    .loginAt(java.time.LocalDateTime.now())
                    .build();
            loginHistoryRepository.save(history);
        } catch (Exception e) {
            log.error("Không thể ghi LoginHistory: {}", e.getMessage());
        }
    }

    /**
     * 2. API ĐĂNG NHẬP
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            BindingResult result,
            jakarta.servlet.http.HttpServletRequest servletRequest) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("message", result.getAllErrors().get(0).getDefaultMessage()));
        }

        log.info("Yêu cầu đăng nhập cho: {}", request.getUsername());

        try {
            User user = authService.login(request.getUsername(), request.getPassword());
            String token = jwtTokenProvider.generateToken(user.getUsername() != null ? user.getUsername() : user.getEmail());

            recordLoginHistory(user, servletRequest, "LOCAL", true, null);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("fullName", user.getFullName());
            response.put("nickname", user.getNickname());
            response.put("email", user.getEmail());
            response.put("gender", user.getGender());
            response.put("avatarUrl", user.getAvatarUrl());
            response.put("currency", user.getCurrency());
            response.put("language", user.getLanguage());
            response.put("authProvider", user.getAuthProvider());
            response.put("userCode", user.getUserCode());
            response.put("role", user.getRole() != null ? user.getRole().name() : "ROLE_USER");
            response.put("status", user.getStatus() != null ? user.getStatus().name() : "ACTIVE");
            response.put("message", "Đăng nhập thành công");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Xác thực thất bại: {}", e.getMessage());
            userRepository.findByUsername(request.getUsername())
                    .or(() -> userRepository.findByEmail(request.getUsername()))
                    .ifPresent(u -> recordLoginHistory(u, servletRequest, "LOCAL", false, e.getMessage()));

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage() != null && e.getMessage().contains("khóa") ? e.getMessage() : "Tên đăng nhập hoặc mật khẩu không chính xác!"));
        }
    }

    /**
     * 3. API LẤY THÔNG TIN CÁ NHÂN (Profile)
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập"));
        }

        try {
            User user = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            UserResponse response = UserResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .nickname(user.getNickname())
                    .email(user.getEmail())
                    .avatarUrl(user.getAvatarUrl())
                    .gender(user.getGender())
                    .dateOfBirth(user.getDateOfBirth())
                    .language(user.getLanguage())
                    .currency(user.getCurrency())
                    .authProvider(user.getAuthProvider())
                    .userCode(user.getUserCode())
                    .role(user.getRole())
                    .status(user.getStatus())
                    .lastLoginAt(user.getLastLoginAt())
                    .createdAt(user.getCreatedAt())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 4. API CẬP NHẬT THÔNG TIN CÁ NHÂN (Update Profile)
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập"));
        }

        try {
            User updatedUser = authService.updateProfile(principal.getName(), request);

            UserResponse response = UserResponse.builder()
                    .id(updatedUser.getId())
                    .username(updatedUser.getUsername())
                    .fullName(updatedUser.getFullName())
                    .nickname(updatedUser.getNickname())
                    .email(updatedUser.getEmail())
                    .avatarUrl(updatedUser.getAvatarUrl())
                    .gender(updatedUser.getGender())
                    .dateOfBirth(updatedUser.getDateOfBirth())
                    .language(updatedUser.getLanguage())
                    .currency(updatedUser.getCurrency())
                    .authProvider(updatedUser.getAuthProvider())
                    .userCode(updatedUser.getUserCode())
                    .createdAt(updatedUser.getCreatedAt())
                    .build();

            return ResponseEntity.ok(Map.of(
                    "message", "Cập nhật thông tin thành công!",
                    "user", response
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi cập nhật: " + e.getMessage()));
        }
    }

    /**
     * 5. API TẢI LÊN ẢNH ĐẠI DIỆN (Upload Avatar Multipart)
     */
    @PostMapping(value = "/avatar", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAvatar(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập"));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng chọn tệp ảnh hợp lệ!"));
        }

        try {
            User user = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            String uploadDir = "uploads/avatars";
            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = ".jpg";
            if (originalFilename != null && originalFilename.lastIndexOf(".") > 0) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String filename = "avatar_" + user.getId() + "_" + System.currentTimeMillis() + extension;
            java.nio.file.Path targetLocation = uploadPath.resolve(filename);
            java.nio.file.Files.copy(file.getInputStream(), targetLocation, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String avatarUrl = "/api/auth/avatar/" + filename;
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "message", "Tải ảnh đại diện thành công!",
                    "avatarUrl", avatarUrl
            ));
        } catch (Exception e) {
            log.error("Lỗi khi tải ảnh avatar: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi tải ảnh: " + e.getMessage()));
        }
    }

    /**
     * 6. Phục vụ hiển thị file ảnh đại diện đã tải lên
     */
    @GetMapping("/avatar/{filename:.+}")
    public ResponseEntity<org.springframework.core.io.Resource> serveAvatar(@PathVariable String filename) {
        try {
            java.nio.file.Path rootPath = java.nio.file.Paths.get("uploads/avatars").toAbsolutePath().normalize();
            java.nio.file.Path filePath = rootPath.resolve(filename).normalize();

            // Bảo vệ chống Path Traversal (CWE-22)
            if (!filePath.startsWith(rootPath)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = null;
            try {
                contentType = java.nio.file.Files.probeContentType(filePath);
            } catch (java.io.IOException ignored) {}

            if (contentType == null) {
                contentType = org.springframework.http.MediaType.IMAGE_JPEG_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "max-age=86400")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}