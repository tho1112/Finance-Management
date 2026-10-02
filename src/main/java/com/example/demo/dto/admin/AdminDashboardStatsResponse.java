package com.example.demo.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsResponse {
    private long totalUsers;
    private long activeUsersToday;
    private long activeUsersThisMonth;
    private long newUsersThisWeek;
    private long newUsersThisMonth;

    private long totalAccounts;
    private BigDecimal totalBalance;

    private long totalTransactions;
    private BigDecimal totalVolume;
    private long transactionsToday;

    private long pendingTickets;
    private long totalTickets;
    private long failedLoginsToday;

    private Map<String, Long> userStatusCounts;
    private Map<String, Long> userRoleCounts;
}
