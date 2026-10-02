package com.example.demo.service;

import com.example.demo.Repository.AccountRepository;
import com.example.demo.Repository.CategoryRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.model.Account;
import com.example.demo.model.Category;
import com.example.demo.model.JarType;
import com.example.demo.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Xác thực Google ID Token và Đăng nhập / Đăng ký tự động
     */
    @Transactional
    public User authenticateGoogleToken(String credential) {
        if (credential == null || credential.isBlank()) {
            throw new RuntimeException("Google Credential Token không được để trống!");
        }

        try {
            // 1. Gọi trực tiếp API Google tokeninfo để xác thực token và lấy profile thật
            String googleVerifyUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + credential.trim();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(googleVerifyUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("[GoogleAuthService] Google tokeninfo trả về lỗi {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("Xác thực token Google thất bại! Token không hợp lệ hoặc đã hết hạn.");
            }

            JsonNode payload = objectMapper.readTree(response.body());
            String email = payload.has("email") ? payload.get("email").asText().toLowerCase().trim() : null;
            boolean emailVerified = payload.has("email_verified") && payload.get("email_verified").asBoolean();

            if (email == null || email.isBlank()) {
                throw new RuntimeException("Không tìm thấy địa chỉ Email trong tài khoản Google!");
            }

            if (!emailVerified) {
                throw new RuntimeException("Tài khoản Google này chưa được xác thực Email!");
            }

            String fullName = payload.has("name") ? payload.get("name").asText() : email.split("@")[0];
            String picture = payload.has("picture") ? payload.get("picture").asText() : null;

            log.info("[GoogleAuthService] Xác thực thành công từ Google. Email thật: {}, Tên: {}", email, fullName);

            // 2. Tìm kiếm người dùng theo Email
            return userRepository.findByEmail(email).map(existingUser -> {
                // Cập nhật avatar hoặc tên nếu trước đó chưa có
                boolean updated = false;
                if ((existingUser.getAvatarUrl() == null || existingUser.getAvatarUrl().isBlank()) && picture != null) {
                    existingUser.setAvatarUrl(picture);
                    updated = true;
                }
                if ((existingUser.getFullName() == null || existingUser.getFullName().isBlank()) && fullName != null) {
                    existingUser.setFullName(fullName);
                    updated = true;
                }
                return updated ? userRepository.save(existingUser) : existingUser;
            }).orElseGet(() -> {
                // 3. Nếu người dùng chưa từng tồn tại, tự động tạo mới User
                log.info("[GoogleAuthService] Tạo tài khoản mới từ Google OAuth2 cho: {}", email);
                String userCode = generateUniqueUserCode();
                User newUser = User.builder()
                        .email(email)
                        .username(email)
                        .userCode(userCode)
                        .fullName(fullName)
                        .avatarUrl(picture)
                        .authProvider("GOOGLE")
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .language("vi")
                        .currency("VND")
                        .build();

                User savedUser = userRepository.save(newUser);

                // Tạo ví chính mặc định
                Account account = Account.builder()
                        .accountName("Ví chính")
                        .type("CASH")
                        .balance(BigDecimal.ZERO)
                        .user(savedUser)
                        .build();
                accountRepository.save(account);

                // Tạo danh mục chi tiêu mặc định
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
            });

        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            log.error("[GoogleAuthService] Lỗi khi xác thực Google Token: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi hệ thống khi xác thực tài khoản Google: " + e.getMessage());
        }
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
