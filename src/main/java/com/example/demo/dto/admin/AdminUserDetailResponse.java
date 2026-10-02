package com.example.demo.dto.admin;

import com.example.demo.dto.UserResponse;
import com.example.demo.model.Account;
import com.example.demo.model.LoginHistory;
import com.example.demo.model.SupportTicket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailResponse {
    private UserResponse user;
    private int totalAccounts;
    private BigDecimal totalBalance;
    private int totalTransactions;
    private List<Account> accounts;
    private List<LoginHistory> recentLoginHistory;
    private List<SupportTicket> recentTickets;
}
