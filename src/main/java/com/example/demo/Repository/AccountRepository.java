package com.example.demo.Repository;

import com.example.demo.model.Account;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUser(User user);

    List<Account> findByUserId(Long userId);

    Optional<Account> findByUserAndAccountName(User user, String accountName);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a")
    java.math.BigDecimal getTotalBalanceAllAccounts();
}