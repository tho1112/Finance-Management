package com.example.demo.Repository;

import com.example.demo.model.Budget;
import com.example.demo.model.JarType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    // 1. Tìm tất cả ngân sách của user
    List<Budget> findByUserId(Long userId);

    // 2. Tìm ngân sách đang hiệu lực trong khoảng ngày
    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.startDate <= :date AND b.endDate >= :date")
    List<Budget> findActiveBudgetsByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    // 3. Tìm ngân sách theo user và danh mục
    List<Budget> findByUserIdAndCategoryId(Long userId, Long categoryId);

    // 4. Tìm ngân sách theo user và danh mục trong khoảng ngày
    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.category.id = :categoryId AND b.startDate <= :date AND b.endDate >= :date")
    List<Budget> findActiveBudgetsByUserAndCategory(@Param("userId") Long userId, @Param("categoryId") Long categoryId, @Param("date") LocalDate date);

    default Optional<Budget> findActiveBudgetByUserAndCategory(Long userId, Long categoryId, LocalDate date) {
        List<Budget> list = findActiveBudgetsByUserAndCategory(userId, categoryId, date);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    // 5. Tương thích ngược với hệ thống cũ (6 chiếc lọ theo tháng)
    List<Budget> findByUserIdAndMonthString(Long userId, String monthString);

    List<Budget> findByUserIdAndMonthStringAndJarType(Long userId, String monthString, JarType jarType);

    default Optional<Budget> findFirstByUserIdAndMonthStringAndJarType(Long userId, String monthString, JarType jarType) {
        List<Budget> list = findByUserIdAndMonthStringAndJarType(userId, monthString, jarType);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}