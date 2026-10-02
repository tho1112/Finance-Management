package com.example.demo.service;

import com.example.demo.Repository.*;
import com.example.demo.dto.UserResponse;
import com.example.demo.dto.admin.AdminDashboardStatsResponse;
import com.example.demo.dto.admin.AdminUserDetailResponse;
import com.example.demo.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminDashboardStatsResponse getDashboardStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime oneWeekAgo = now.minusDays(7);
        LocalDateTime oneMonthAgo = now.minusMonths(1);

        long totalUsers = userRepository.count();
        long activeUsersToday = loginHistoryRepository.countUniqueActiveUsersSince(startOfToday);
        long activeUsersThisMonth = loginHistoryRepository.countUniqueActiveUsersSince(oneMonthAgo);
        long newUsersThisWeek = userRepository.countUsersCreatedSince(oneWeekAgo);
        long newUsersThisMonth = userRepository.countUsersCreatedSince(oneMonthAgo);

        long totalAccounts = accountRepository.count();
        BigDecimal totalBalance = accountRepository.getTotalBalanceAllAccounts();

        long totalTransactions = transactionRepository.count();
        BigDecimal totalVolume = transactionRepository.getTotalVolumeAllTransactions();
        long transactionsToday = transactionRepository.countTransactionsSince(startOfToday);

        long pendingTickets = supportTicketRepository.countPendingTickets();
        long totalTickets = supportTicketRepository.count();
        long failedLoginsToday = loginHistoryRepository.countFailedLoginsSince(startOfToday);

        Map<String, Long> userStatusCounts = new HashMap<>();
        for (UserStatus s : UserStatus.values()) {
            userStatusCounts.put(s.name(), userRepository.countByStatus(s));
        }

        Map<String, Long> userRoleCounts = new HashMap<>();
        for (UserRole r : UserRole.values()) {
            userRoleCounts.put(r.name(), userRepository.countByRole(r));
        }

        return AdminDashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsersToday(activeUsersToday)
                .activeUsersThisMonth(activeUsersThisMonth)
                .newUsersThisWeek(newUsersThisWeek)
                .newUsersThisMonth(newUsersThisMonth)
                .totalAccounts(totalAccounts)
                .totalBalance(totalBalance != null ? totalBalance : BigDecimal.ZERO)
                .totalTransactions(totalTransactions)
                .totalVolume(totalVolume != null ? totalVolume : BigDecimal.ZERO)
                .transactionsToday(transactionsToday)
                .pendingTickets(pendingTickets)
                .totalTickets(totalTickets)
                .failedLoginsToday(failedLoginsToday)
                .userStatusCounts(userStatusCounts)
                .userRoleCounts(userRoleCounts)
                .build();
    }

    public Page<UserResponse> searchUsers(String keyword, UserStatus status, UserRole role, Pageable pageable) {
        Page<User> users = userRepository.searchUsers(keyword, status, role, pageable);
        return users.map(this::mapToUserResponse);
    }

    public AdminUserDetailResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        List<Account> accounts = accountRepository.findByUserId(userId);
        BigDecimal totalBalance = accounts.stream()
                .map(Account::getBalance)
                .filter(b -> b != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Transaction> transactions = transactionRepository.findByUserOrderByTransactionDateDesc(user);
        List<LoginHistory> recentLogins = loginHistoryRepository.findTop20ByUserIdOrderByLoginAtDesc(userId);
        List<SupportTicket> recentTickets = supportTicketRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return AdminUserDetailResponse.builder()
                .user(mapToUserResponse(user))
                .totalAccounts(accounts.size())
                .totalBalance(totalBalance)
                .totalTransactions(transactions.size())
                .accounts(accounts)
                .recentLoginHistory(recentLogins)
                .recentTickets(recentTickets)
                .build();
    }

    @Transactional
    public UserResponse updateUserStatus(User adminUser, Long targetUserId, UserStatus newStatus, String reason, String ipAddress) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        UserStatus oldStatus = target.getStatus();
        target.setStatus(newStatus);
        userRepository.save(target);

        // Audit Log
        recordAuditLog(adminUser, "UPDATE_USER_STATUS", "USER", targetUserId,
                String.format("Đổi trạng thái người dùng %s từ %s sang %s. Lý do: %s", target.getEmail(), oldStatus, newStatus, reason != null ? reason : "Không có"),
                ipAddress);

        return mapToUserResponse(target);
    }

    @Transactional
    public UserResponse updateUserRole(User adminUser, Long targetUserId, UserRole newRole, String ipAddress) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        UserRole oldRole = target.getRole();
        target.setRole(newRole);
        userRepository.save(target);

        // Audit Log
        recordAuditLog(adminUser, "UPDATE_USER_ROLE", "USER", targetUserId,
                String.format("Phân quyền người dùng %s từ %s sang %s", target.getEmail(), oldRole, newRole),
                ipAddress);

        return mapToUserResponse(target);
    }

    public Page<LoginHistory> getAccessLogs(Pageable pageable) {
        return loginHistoryRepository.findAllByOrderByLoginAtDesc(pageable);
    }

    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }

    @Transactional
    public void recordAuditLog(User adminUser, String action, String targetEntity, Long targetId, String description, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .admin(adminUser)
                    .action(action)
                    .targetEntity(targetEntity)
                    .targetId(targetId)
                    .detailDescription(description)
                    .ipAddress(ipAddress)
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Lỗi khi ghi Audit Log: {}", e.getMessage());
        }
    }

    public UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .nickname(user.getNickname())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .avatarUrl(user.getAvatarUrl())
                .language(user.getLanguage())
                .currency(user.getCurrency())
                .authProvider(user.getAuthProvider())
                .userCode(user.getUserCode())
                .role(user.getRole())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
