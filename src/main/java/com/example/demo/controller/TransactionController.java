package com.example.demo.controller;

import com.example.demo.Repository.AccountRepository;
import com.example.demo.Repository.TransactionRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.dto.TransactionRequest;
import com.example.demo.dto.TransactionResponse;
import com.example.demo.model.Account;
import com.example.demo.model.Transaction;
import com.example.demo.model.User;
import com.example.demo.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final com.example.demo.Repository.BudgetRepository budgetRepository;

    /**
     * 1. TẠO GIAO DỊCH
     */
    @PostMapping
    public ResponseEntity<?> createTransaction(
            @Valid @RequestBody TransactionRequest request,
            BindingResult result,
            Principal principal) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("message", result.getAllErrors().get(0).getDefaultMessage()));
        }

        try {
            User user = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            Transaction savedTx = transactionService.processTransaction(user.getId(), request, user);

            return ResponseEntity.ok(Map.of(
                    "message", "Lưu giao dịch thành công!",
                    "transactionId", savedTx.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * 2. LẤY DANH SÁCH GIAO DỊCH
     */
    @GetMapping
    public ResponseEntity<?> getAllTransactions(Principal principal) {
        try {
            User user = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow(() -> new RuntimeException("Chưa đăng nhập"));

            List<Transaction> transactions = transactionRepository.findByUserWithDetailsOrderByTransactionDateDesc(user);

            List<TransactionResponse> responseList = transactions.stream().map(t -> {
                Long catId = t.getCategory() != null ? t.getCategory().getId() : null;
                String catName = t.getCategory() != null ? t.getCategory().getName() : "Thu nhập";
                String catIcon = t.getCategory() != null ? t.getCategory().getIcon() : "payments";
                String catColor = t.getCategory() != null ? t.getCategory().getColorCode() : "#4CAF50";

                String type = t.getTransactionType();
                if (type == null || type.isBlank()) {
                    type = (t.getCategory() == null) ? "INCOME" : "EXPENSE";
                }

                Long accId = t.getAccount() != null ? t.getAccount().getId() : null;
                String accName = t.getAccount() != null ? t.getAccount().getAccountName() : "Ví";

                return TransactionResponse.builder()
                        .id(t.getId())
                        .amount(t.getAmount())
                        .note(t.getNote())
                        .transactionDate(t.getTransactionDate())
                        .transactionType(type)
                        .type(type)
                        .accountId(accId)
                        .accountName(accName)
                        .walletId(accId)
                        .walletName(accName)
                        .categoryId(catId)
                        .categoryName(catName)
                        .categoryIcon(catIcon)
                        .categoryColor(catColor)
                        .aiRawText(t.getAiRawText())
                        .receiptImageUrl(t.getReceiptImageUrl())
                        .isRecurring(t.getIsRecurring())
                        .createdAt(t.getCreatedAt())
                        .build();
            }).collect(Collectors.toList());

            return ResponseEntity.ok(responseList);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi khi tải giao dịch: " + e.getMessage()));
        }
    }

    /**
     * 3. XÓA GIAO DỊCH (Hoàn lại cả số dư ví và số tiền đã chi ngân sách)
     */
    @DeleteMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id, Principal principal) {
        try {
            User user = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow(() -> new RuntimeException("Chưa đăng nhập"));

            Transaction tx = transactionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch"));

            if (!tx.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("message", "Bạn không có quyền xóa giao dịch này!"));
            }

            // Hoàn lại số dư tài khoản/ví
            Account account = tx.getAccount();
            if (account != null) {
                BigDecimal currentBalance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
                if ("INCOME".equalsIgnoreCase(tx.getTransactionType())) {
                    account.setBalance(currentBalance.subtract(tx.getAmount()));
                } else {
                    account.setBalance(currentBalance.add(tx.getAmount()));
                }
                accountRepository.save(account);
            }

            // Hoàn lại ngân sách nếu là khoản chi tiêu
            if (tx.getCategory() != null && ("EXPENSE".equalsIgnoreCase(tx.getTransactionType()) || tx.getTransactionType() == null)) {
                try {
                    java.time.LocalDate txDate = tx.getTransactionDate().toLocalDate();
                    java.util.List<com.example.demo.model.Budget> catBudgets = budgetRepository.findActiveBudgetsByUserAndCategory(user.getId(), tx.getCategory().getId(), txDate);
                    if (!catBudgets.isEmpty()) {
                        for (com.example.demo.model.Budget b : catBudgets) {
                            BigDecimal currentSpent = b.getSpentAmount() != null ? b.getSpentAmount() : BigDecimal.ZERO;
                            BigDecimal newSpent = currentSpent.subtract(tx.getAmount());
                            b.setSpentAmount(newSpent.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newSpent);
                            budgetRepository.save(b);
                        }
                    } else if (tx.getCategory().getJarType() != null) {
                        String monthString = txDate.toString().substring(0, 7);
                        java.util.List<com.example.demo.model.Budget> jarBudgets = budgetRepository.findByUserIdAndMonthStringAndJarType(user.getId(), monthString, tx.getCategory().getJarType());
                        if (!jarBudgets.isEmpty()) {
                            for (com.example.demo.model.Budget b : jarBudgets) {
                                BigDecimal currentSpent = b.getSpentAmount() != null ? b.getSpentAmount() : BigDecimal.ZERO;
                                BigDecimal newSpent = currentSpent.subtract(tx.getAmount());
                                b.setSpentAmount(newSpent.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newSpent);
                                budgetRepository.save(b);
                            }
                        }
                    }
                } catch (Exception budgetEx) {
                    System.err.println("Cảnh báo hoàn lại ngân sách khi xóa giao dịch: " + budgetEx.getMessage());
                }
            }

            transactionRepository.delete(tx);
            return ResponseEntity.ok(Map.of("message", "Đã xóa giao dịch và hoàn lại số dư/ngân sách thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi xóa giao dịch: " + e.getMessage()));
        }
    }
}