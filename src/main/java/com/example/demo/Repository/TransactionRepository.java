package com.example.demo.Repository;

import com.example.demo.model.JarType;
import com.example.demo.model.Transaction;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserOrderByTransactionDateDesc(User user);

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.category LEFT JOIN FETCH t.account WHERE t.user = :user ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserWithDetailsOrderByTransactionDateDesc(@Param("user") User user);

    List<Transaction> findByAccountId(Long accountId);

    List<Transaction> findByCategoryId(Long categoryId);

    // Tính tổng chi tiêu theo Category trong khoảng thời gian
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user.id = :userId AND t.category.id = :categoryId AND (t.transactionType = 'EXPENSE' OR t.transactionType IS NULL) AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate")
    BigDecimal getTotalSpentByCategoryAndPeriod(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Tương thích cho 6 lọ nếu cần
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user.id = :userId AND t.category.jarType = :jarType AND (t.transactionType = 'EXPENSE' OR t.transactionType IS NULL) AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate")
    BigDecimal getTotalSpentByJarAndMonth(
            @Param("userId") Long userId,
            @Param("jarType") JarType jarType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t")
    BigDecimal getTotalVolumeAllTransactions();

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.transactionDate >= :since")
    long countTransactionsSince(@Param("since") LocalDateTime since);
}