package com.example.demo.controller;

import com.example.demo.Repository.AccountRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.dto.AccountRequest;
import com.example.demo.dto.AccountResponse;
import com.example.demo.model.Account;
import com.example.demo.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AccountController {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    /**
     * 1. LẤY DANH SÁCH TÀI KHOẢN / VÍ
     */
    @GetMapping
    public ResponseEntity<?> getAccounts(Principal principal) {
        try {
            User user = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

            List<Account> accounts = accountRepository.findByUser(user);

            List<AccountResponse> responseList = accounts.stream().map(acc ->
                    AccountResponse.builder()
                            .id(acc.getId())
                            .accountName(acc.getAccountName())
                            .name(acc.getAccountName())
                            .type(acc.getType())
                            .balance(acc.getBalance())
                            .createdAt(acc.getCreatedAt())
                            .build()
            ).collect(Collectors.toList());

            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * 2. TẠO TÀI KHOẢN / VÍ MỚI
     */
    @PostMapping
    public ResponseEntity<?> createAccount(
            @Valid @RequestBody AccountRequest request,
            BindingResult result,
            Principal principal) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("message", result.getAllErrors().get(0).getDefaultMessage()));
        }

        try {
            User user = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

            String accountName = request.getAccountName().trim();

            if (accountRepository.findByUserAndAccountName(user, accountName).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tên ví này đã tồn tại! Vui lòng chọn tên khác."));
            }

            Account account = Account.builder()
                    .accountName(accountName)
                    .type(request.getType() != null ? request.getType() : "CASH")
                    .balance(request.getBalance())
                    .user(user)
                    .build();

            Account saved = accountRepository.save(account);

            return ResponseEntity.ok(Map.of(
                    "message", "Tạo tài khoản thành công!",
                    "account", AccountResponse.builder()
                            .id(saved.getId())
                            .accountName(saved.getAccountName())
                            .name(saved.getAccountName())
                            .type(saved.getType())
                            .balance(saved.getBalance())
                            .createdAt(saved.getCreatedAt())
                            .build()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * 3. XÓA TÀI KHOẢN / VÍ
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAccount(@PathVariable Long id, Principal principal) {
        try {
            User user = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow(() -> new RuntimeException("Chưa đăng nhập"));

            Account account = accountRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

            if (!account.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("message", "Cảnh báo: Bạn không có quyền xóa ví này!"));
            }

            accountRepository.delete(account);
            return ResponseEntity.ok(Map.of("message", "Đã xóa ví thành công!"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không thể xóa! Ví này đang chứa lịch sử giao dịch."));
        }
    }
}