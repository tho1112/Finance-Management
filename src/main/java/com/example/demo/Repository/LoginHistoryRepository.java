package com.example.demo.Repository;

import com.example.demo.model.LoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    List<LoginHistory> findTop20ByUserIdOrderByLoginAtDesc(Long userId);

    Page<LoginHistory> findAllByOrderByLoginAtDesc(Pageable pageable);

    @Query("SELECT COUNT(lh) FROM LoginHistory lh WHERE lh.loginAt >= :since AND lh.isSuccess = true")
    long countSuccessfulLoginsSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT lh.user.id) FROM LoginHistory lh WHERE lh.loginAt >= :since AND lh.isSuccess = true")
    long countUniqueActiveUsersSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(lh) FROM LoginHistory lh WHERE lh.isSuccess = false AND lh.loginAt >= :since")
    long countFailedLoginsSince(@Param("since") LocalDateTime since);
}
