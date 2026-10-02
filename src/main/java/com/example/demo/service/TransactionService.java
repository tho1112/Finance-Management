package com.example.demo.service;

import com.example.demo.Repository.*;
import com.example.demo.dto.TransactionRequest;
import com.example.demo.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public Transaction processTransaction(Long userId, TransactionRequest request, User user) {
        Long categoryId = request.getCategoryId();
        Long accountId = request.getEffectiveAccountId();
        BigDecimal amount = request.getAmount();
        String note = request.getNote();
        String dateStr = request.getEffectiveTransactionDate();
        String type = request.getEffectiveType();

        // 1. Tìm Danh mục (nếu có)
        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));
        }

        // 2. Tìm Tài khoản / Ví (Nguồn tiền)
        Account account = null;
        if (accountId != null) {
            account = accountRepository.findById(accountId).orElse(null);
        }

        if (account == null) {
            Optional<Account> userFirstAccount = accountRepository.findByUserId(userId).stream().findFirst();
            if (userFirstAccount.isPresent()) {
                account = userFirstAccount.get();
            } else {
                account = Account.builder()
                        .accountName("Ví chính")
                        .type("CASH")
                        .balance(BigDecimal.ZERO)
                        .user(user)
                        .build();
                account = accountRepository.save(account);
            }
        }

        if (account.getUser() != null && !account.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền sử dụng tài khoản này!");
        }

        // 3. Phân tích ngày giờ giao dịch
        LocalDateTime txDate = parseDateTime(dateStr);

        // 4. Lưu giao dịch
        Transaction t = Transaction.builder()
                .amount(amount)
                .note(note)
                .transactionDate(txDate)
                .transactionType(type)
                .aiRawText(request.getAiRawText())
                .receiptImageUrl(request.getReceiptImageUrl())
                .isRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false)
                .user(user)
                .account(account)
                .category(category)
                .build();

        Transaction savedTx = transactionRepository.save(t);

        // 5. Cập nhật số dư Tài khoản / Ví
        BigDecimal currentBalance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
        if ("INCOME".equalsIgnoreCase(type)) {
            account.setBalance(currentBalance.add(amount));
        } else {
            account.setBalance(currentBalance.subtract(amount));

            // 6. Cập nhật Ngân sách nếu có danh mục
            if (category != null) {
                try {
                    LocalDate date = txDate.toLocalDate();
                    java.util.List<Budget> catBudgets = budgetRepository.findActiveBudgetsByUserAndCategory(userId, category.getId(), date);
                    if (!catBudgets.isEmpty()) {
                        for (Budget b : catBudgets) {
                            BigDecimal currentSpent = b.getSpentAmount() != null ? b.getSpentAmount() : BigDecimal.ZERO;
                            b.setSpentAmount(currentSpent.add(amount));
                            budgetRepository.save(b);
                        }
                    } else if (category.getJarType() != null) {
                        String monthString = date.toString().substring(0, 7);
                        java.util.List<Budget> jarBudgets = budgetRepository.findByUserIdAndMonthStringAndJarType(userId, monthString, category.getJarType());
                        if (!jarBudgets.isEmpty()) {
                            for (Budget b : jarBudgets) {
                                BigDecimal currentSpent = b.getSpentAmount() != null ? b.getSpentAmount() : BigDecimal.ZERO;
                                b.setSpentAmount(currentSpent.add(amount));
                                budgetRepository.save(b);
                            }
                        }
                    }
                } catch (Exception budgetEx) {
                    System.err.println("Cảnh báo: Không thể cập nhật ngân sách cho giao dịch: " + budgetEx.getMessage());
                }
            }
        }
        accountRepository.save(account);

        return savedTx;
    }

    @Transactional
    public void processTransaction(Long userId, Long categoryId, Long accountId, BigDecimal amount, String note, String dateStr, String type, User user) {
        TransactionRequest req = TransactionRequest.builder()
                .amount(amount)
                .categoryId(categoryId)
                .accountId(accountId)
                .note(note)
                .transactionDate(dateStr)
                .transactionType(type)
                .build();
        processTransaction(userId, req, user);
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDateTime.now();
        }
        String clean = dateStr.trim().replace(" ", "T");
        if (clean.endsWith("Z")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        int plusIndex = clean.indexOf("+");
        if (plusIndex > 10) {
            clean = clean.substring(0, plusIndex);
        }
        if (clean.length() == 10) {
            clean = clean + "T00:00:00";
        } else if (clean.length() == 16) {
            clean = clean + ":00";
        }
        try {
            return LocalDateTime.parse(clean);
        } catch (Exception e) {
            try {
                return LocalDate.parse(clean.substring(0, 10)).atStartOfDay();
            } catch (Exception ex) {
                return LocalDateTime.now();
            }
        }
    }
}