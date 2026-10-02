package com.example.demo.controller;

import com.example.demo.Repository.BudgetRepository;
import com.example.demo.Repository.CategoryRepository;
import com.example.demo.Repository.TransactionRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.dto.AutoBudgetRequest;
import com.example.demo.dto.BudgetRequest;
import com.example.demo.dto.BudgetResponse;
import com.example.demo.dto.ManualBudgetRequest;
import com.example.demo.model.Budget;
import com.example.demo.model.Category;
import com.example.demo.model.JarType;
import com.example.demo.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BudgetController {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    /**
     * 1. LẤY DANH SÁCH NGÂN SÁCH (Hỗ trợ theo tháng hoặc lấy toàn bộ ngân sách của user)
     */
    @GetMapping
    public ResponseEntity<?> getBudgets(
            @RequestParam(required = false) String month,
            Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .or(() -> userRepository.findByEmail(principal.getName()))
                .orElseThrow();

        List<Budget> budgets;
        if (month != null && !month.isBlank()) {
            budgets = budgetRepository.findByUserIdAndMonthString(user.getId(), month);
            if (budgets.isEmpty()) {
                try {
                    YearMonth ym = YearMonth.parse(month);
                    LocalDate start = ym.atDay(1);
                    LocalDate end = ym.atEndOfMonth();
                    budgets = budgetRepository.findActiveBudgetsByUserIdAndDate(user.getId(), start);
                } catch (Exception ignored) {
                }
            }
        } else {
            budgets = budgetRepository.findByUserId(user.getId());
        }

        List<BudgetResponse> responseList = budgets.stream().map(b -> {
            BigDecimal spent = b.getSpentAmount();
            if (spent == null || spent.compareTo(BigDecimal.ZERO) == 0) {
                if (b.getJarType() != null && b.getStartDate() != null && b.getEndDate() != null) {
                    spent = transactionRepository.getTotalSpentByJarAndMonth(
                            user.getId(),
                            b.getJarType(),
                            b.getStartDate().atStartOfDay(),
                            b.getEndDate().atTime(23, 59, 59)
                    );
                } else if (b.getCategory() != null && b.getStartDate() != null && b.getEndDate() != null) {
                    spent = transactionRepository.getTotalSpentByCategoryAndPeriod(
                            user.getId(),
                            b.getCategory().getId(),
                            b.getStartDate().atStartOfDay(),
                            b.getEndDate().atTime(23, 59, 59)
                    );
                }
            }

            String catName = (b.getName() != null && !b.getName().isBlank())
                    ? b.getName()
                    : (b.getCategory() != null ? b.getCategory().getName() : "Ngân sách");
            String catIcon = (b.getIcon() != null && !b.getIcon().isBlank())
                    ? b.getIcon()
                    : (b.getCategory() != null ? b.getCategory().getIcon() : "category");

            return BudgetResponse.builder()
                    .id(b.getId())
                    .categoryId(b.getCategory() != null ? b.getCategory().getId() : null)
                    .categoryName(catName)
                    .categoryIcon(catIcon)
                    .amount(b.getAmount())
                    .limitAmount(b.getAmount())
                    .spentAmount(spent != null ? spent : BigDecimal.ZERO)
                    .startDate(b.getStartDate())
                    .endDate(b.getEndDate())
                    .alertThresholdPercent(b.getAlertThresholdPercent())
                    .name(catName)
                    .jarType(b.getJarType())
                    .icon(catIcon)
                    .monthString(b.getMonthString())
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    /**
     * 2. TẠO NGÂN SÁCH MỚI THEO DANH MỤC & KHOẢNG NGÀY (Cấu trúc ERD mới)
     */
    @PostMapping
    public ResponseEntity<?> createBudget(
            @Valid @RequestBody BudgetRequest request,
            BindingResult result,
            Principal principal) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("message", result.getAllErrors().get(0).getDefaultMessage()));
        }

        try {
            User user = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow();

            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục!"));

            String monthString = request.getStartDate() != null
                    ? request.getStartDate().toString().substring(0, 7)
                    : null;

            Budget budget = Budget.builder()
                    .user(user)
                    .category(category)
                    .amount(request.getAmount())
                    .limitAmount(request.getAmount())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .monthString(monthString)
                    .alertThresholdPercent(request.getAlertThresholdPercent() != null ? request.getAlertThresholdPercent() : 80)
                    .name(category.getName())
                    .jarType(category.getJarType())
                    .icon(category.getIcon())
                    .build();

            Budget saved = budgetRepository.save(budget);

            return ResponseEntity.ok(Map.of(
                    "message", "Tạo ngân sách thành công!",
                    "budgetId", saved.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi tạo ngân sách: " + e.getMessage()));
        }
    }

    /**
     * 3. TẠO HOẶC CẬP NHẬT 6 CHIẾC LỌ TỰ ĐỘNG (Tương thích giao diện cũ)
     */
    @PostMapping("/auto-6-jars")
    public ResponseEntity<?> create6Jars(
            @Valid @RequestBody AutoBudgetRequest request,
            BindingResult result,
            Principal principal) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("message", result.getAllErrors().get(0).getDefaultMessage()));
        }

        try {
            User user = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow(() -> new RuntimeException("Chưa đăng nhập"));

            BigDecimal income = request.getIncome();
            String month = request.getMonth();

            YearMonth requestYearMonth = YearMonth.parse(month);

            LocalDate startDate = requestYearMonth.atDay(1);
            LocalDate endDate = requestYearMonth.atEndOfMonth();

            Map<JarType, Double> jarPercentages = Map.of(
                    JarType.NECESSITIES, 0.55,
                    JarType.LONG_TERM_SAVE, 0.10,
                    JarType.FINANCIAL_FREE, 0.10,
                    JarType.EDUCATION, 0.10,
                    JarType.PLAY, 0.10,
                    JarType.GIVE, 0.05
            );

            for (Map.Entry<JarType, Double> entry : jarPercentages.entrySet()) {
                JarType type = entry.getKey();
                BigDecimal limit = income.multiply(BigDecimal.valueOf(entry.getValue()));

                Budget budget = budgetRepository.findFirstByUserIdAndMonthStringAndJarType(user.getId(), month, type)
                        .orElse(new Budget());

                Category category = categoryRepository.findByJarType(type).stream().findFirst().orElse(null);

                budget.setUser(user);
                budget.setCategory(category);
                budget.setMonthString(month);
                budget.setStartDate(startDate);
                budget.setEndDate(endDate);
                budget.setJarType(type);
                budget.setAmount(limit);
                budget.setLimitAmount(limit);
                budget.setAlertThresholdPercent(80);

                switch (type) {
                    case NECESSITIES -> { budget.setName("Thiết yếu (55%)"); budget.setIcon("home"); }
                    case LONG_TERM_SAVE -> { budget.setName("Tiết kiệm (10%)"); budget.setIcon("savings"); }
                    case FINANCIAL_FREE -> { budget.setName("Đầu tư (10%)"); budget.setIcon("trending_up"); }
                    case EDUCATION -> { budget.setName("Giáo dục (10%)"); budget.setIcon("school"); }
                    case PLAY -> { budget.setName("Hưởng thụ (10%)"); budget.setIcon("movie"); }
                    case GIVE -> { budget.setName("Cho đi (5%)"); budget.setIcon("volunteer_activism"); }
                }

                budgetRepository.save(budget);
            }

            return ResponseEntity.ok(Map.of("message", "Đã phân bổ và đồng bộ 6 lọ thành công cho tháng " + month));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi thiết lập ngân sách: " + e.getMessage()));
        }
    }

    /**
     * 4. TẠO NGÂN SÁCH THỦ CÔNG (Tương thích giao diện cũ)
     */
    @PostMapping("/manual")
    public ResponseEntity<?> createManualBudget(
            @Valid @RequestBody ManualBudgetRequest request,
            BindingResult result,
            Principal principal) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("message", result.getAllErrors().get(0).getDefaultMessage()));
        }

        User user = userRepository.findByUsername(principal.getName())
                .or(() -> userRepository.findByEmail(principal.getName()))
                .orElseThrow();
        String month = request.getMonth();

        YearMonth requestYearMonth = YearMonth.parse(month);
        LocalDate start = requestYearMonth.atDay(1);
        LocalDate end = requestYearMonth.atEndOfMonth();

        Category category = categoryRepository.findByUserOrSystem(user).stream().findFirst().orElse(null);

        Budget budget = Budget.builder()
                .user(user)
                .category(category)
                .name(request.getName().trim())
                .amount(request.getLimit())
                .startDate(start)
                .endDate(end)
                .alertThresholdPercent(80)
                .monthString(month)
                .icon("category")
                .jarType(JarType.NECESSITIES)
                .build();

        budgetRepository.save(budget);
        return ResponseEntity.ok(Map.of("message", "Tạo ngân sách thủ công thành công!"));
    }

    /**
     * 5. XÓA NGÂN SÁCH
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBudget(@PathVariable Long id, Principal principal) {
        try {
            User user = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow();

            Budget budget = budgetRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ngân sách"));

            if (!budget.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("message", "Bạn không có quyền xóa ngân sách này!"));
            }

            budgetRepository.delete(budget);
            return ResponseEntity.ok(Map.of("message", "Đã xóa ngân sách thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }
}