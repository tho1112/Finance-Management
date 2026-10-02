package com.example.demo.service;

import com.example.demo.Repository.AccountRepository;
import com.example.demo.Repository.CategoryRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.dto.UserProfileUpdateRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.model.Account;
import com.example.demo.model.Category;
import com.example.demo.model.JarType;
import com.example.demo.model.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final OtpService otpService;

    /**
     * XỬ LÝ ĐĂNG KÝ
     */
    @Transactional
    public User register(RegisterRequest request) {

        // 1. Kiểm tra xác nhận mật khẩu (nếu có gửi lên)
        if (request.getConfirmPassword() != null && !request.getConfirmPassword().isBlank()) {
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                throw new RuntimeException("Mật khẩu xác nhận không trùng khớp!");
            }
        }

        // 2. Chuẩn hoá email
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email không được để trống!");
        }
        String cleanEmail = request.getEmail().trim().toLowerCase();

        // 3. Xác thực mã OTP email thật (bảo đảm người dùng sở hữu email thật)
        if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
            throw new RuntimeException("Vui lòng nhập mã OTP xác thực đã được gửi về email của bạn!");
        }
        if (!otpService.verifyOtp(cleanEmail, request.getOtp().trim())) {
            throw new RuntimeException("Mã xác thực OTP không chính xác hoặc đã hết hạn (5 phút)! Vui lòng bấm 'Gửi mã OTP' lại.");
        }

        // 4. Kiểm tra trùng lặp Email
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new RuntimeException("Email này đã được sử dụng! Vui lòng đăng nhập hoặc dùng email khác.");
        }

        // 4. Bỏ logic xác thực tên đăng nhập: Sử dụng chính Email làm định danh tài khoản (tương thích chuẩn OAuth2)
        String username = (request.getUsername() != null && !request.getUsername().trim().isEmpty())
                ? request.getUsername().trim()
                : cleanEmail;

        // 5. Sinh mã số định danh duy nhất (userCode) tránh trùng lặp
        String userCode = generateUniqueUserCode();

        // 6. Chuyển đổi dữ liệu từ DTO sang Entity (Map đầy đủ các thuộc tính mở rộng)
        User user = User.builder()
                .username(username)
                .email(cleanEmail)
                .userCode(userCode)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName() != null ? request.getFullName().trim() : "")
                .nickname(request.getNickname() != null ? request.getNickname().trim() : null)
                .gender(request.getGender())
                .avatarUrl(request.getAvatarUrl())
                .language(request.getLanguage() != null ? request.getLanguage() : "vi")
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .authProvider(request.getAuthProvider() != null ? request.getAuthProvider() : "LOCAL")
                .build();

        User savedUser = userRepository.save(user);

        // 3. Tạo Tài khoản / Ví mặc định
        Account account = Account.builder()
                .accountName("Ví chính")
                .type("CASH")
                .balance(BigDecimal.ZERO)
                .user(savedUser)
                .build();
        accountRepository.save(account);

        // 4. Tạo bộ Danh mục cơ bản cho user mới
        List<Category> defaultCategories = List.of(
                Category.builder().name("Ăn uống").type("EXPENSE").iconName("restaurant").colorCode("#FF5722").jarType(JarType.NECESSITIES).user(savedUser).build(),
                Category.builder().name("Tiền nhà / Sinh hoạt").type("EXPENSE").iconName("home").colorCode("#2196F3").jarType(JarType.NECESSITIES).user(savedUser).build(),
                Category.builder().name("Di chuyển").type("EXPENSE").iconName("directions_car").colorCode("#FF9800").jarType(JarType.NECESSITIES).user(savedUser).build(),
                Category.builder().name("Mua sắm").type("EXPENSE").iconName("shopping_bag").colorCode("#00BCD4").jarType(JarType.PLAY).user(savedUser).build(),
                Category.builder().name("Học tập").type("EXPENSE").iconName("school").colorCode("#9C27B0").jarType(JarType.EDUCATION).user(savedUser).build(),
                Category.builder().name("Giải trí").type("EXPENSE").iconName("movie").colorCode("#E91E63").jarType(JarType.PLAY).user(savedUser).build(),
                Category.builder().name("Đầu tư").type("EXPENSE").iconName("trending_up").colorCode("#4CAF50").jarType(JarType.FINANCIAL_FREE).user(savedUser).build(),
                Category.builder().name("Lương thưởng").type("INCOME").iconName("payments").colorCode("#009688").user(savedUser).build(),
                Category.builder().name("Thu nhập phụ").type("INCOME").iconName("account_balance_wallet").colorCode("#673AB7").user(savedUser).build()
        );
        categoryRepository.saveAll(defaultCategories);

        return savedUser;
    }

    /**
     * XỬ LÝ ĐĂNG NHẬP (Hỗ trợ cả Username và Email)
     */
    public User login(String identifier, String rawPassword) {
        log.info("Hệ thống đang xác thực cho: {}", identifier);

        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseGet(() -> {
                    if ("admin@gmail.com".equalsIgnoreCase(identifier) || "admin".equalsIgnoreCase(identifier)) {
                        log.info("Khởi tạo tự động tài khoản admin mặc định: admin@gmail.com / admin123");
                        User newAdmin = User.builder()
                                .username("admin")
                                .email("admin@gmail.com")
                                .fullName("Quản Trị Viên Hệ Thống")
                                .nickname("Admin")
                                .password(passwordEncoder.encode("admin123"))
                                .legacyPassword("admin123")
                                .role(com.example.demo.model.UserRole.ROLE_ADMIN)
                                .status(com.example.demo.model.UserStatus.ACTIVE)
                                .userCode("999999")
                                .currency("VND")
                                .language("vi")
                                .authProvider("LOCAL")
                                .build();
                        return userRepository.save(newAdmin);
                    }
                    throw new RuntimeException("Tài khoản hoặc Email không tồn tại!");
                });

        // Đảm bảo quyền ROLE_ADMIN và cập nhật mật khẩu nếu đăng nhập bằng admin123
        if ("admin@gmail.com".equalsIgnoreCase(user.getEmail()) || "admin".equalsIgnoreCase(user.getUsername())) {
            if (user.getRole() != com.example.demo.model.UserRole.ROLE_ADMIN) {
                user.setRole(com.example.demo.model.UserRole.ROLE_ADMIN);
                userRepository.save(user);
            }
            if ("admin123".equals(rawPassword) && !passwordEncoder.matches(rawPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode("admin123"));
                user.setLegacyPassword("admin123");
                userRepository.save(user);
            }
        }

        if (user.getStatus() == com.example.demo.model.UserStatus.BANNED) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa vĩnh viễn! Vui lòng liên hệ trung tâm hỗ trợ.");
        }
        if (user.getStatus() == com.example.demo.model.UserStatus.SUSPENDED) {
            throw new RuntimeException("Tài khoản của bạn đang bị tạm ngưng! Vui lòng liên hệ quản trị viên.");
        }

        boolean isMatch = passwordEncoder.matches(rawPassword, user.getPassword());

        if (!isMatch) {
            int attempts = user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() + 1 : 1;
            user.setFailedLoginAttempts(attempts);
            userRepository.save(user);
            log.warn("Cảnh báo bảo mật: Sai mật khẩu cho ({}) lần thứ {}", identifier, attempts);
            throw new RuntimeException("Sai mật khẩu!");
        }

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        log.info("Xác thực thành công cho: {}", identifier);
        return user;
    }

    /**
     * CẬP NHẬT THÔNG TIN CÁ NHÂN (Profile)
     */
    @Transactional
    public User updateProfile(String identifier, UserProfileUpdateRequest request) {
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (request.getFullName() != null) user.setFullName(request.getFullName().trim());
        if (request.getNickname() != null) user.setNickname(request.getNickname().trim());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getLanguage() != null) user.setLanguage(request.getLanguage());
        if (request.getCurrency() != null) user.setCurrency(request.getCurrency());

        return userRepository.save(user);
    }

    private String generateUniqueUserCode() {
        String code;
        int attempts = 0;
        do {
            code = String.valueOf(100000 + (int) (Math.random() * 900000));
            attempts++;
            if (attempts > 20) {
                code = String.valueOf(10000000 + (int) (Math.random() * 90000000));
            }
        } while (userRepository.existsByUserCode(code));
        return code;
    }
}