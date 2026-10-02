package com.example.demo.Repository;

import com.example.demo.model.GroupTransactionSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupTransactionSplitRepository extends JpaRepository<GroupTransactionSplit, Long> {

    List<GroupTransactionSplit> findByGroupTransactionId(Long groupTransactionId);

    List<GroupTransactionSplit> findByUserIdAndIsSettled(Long userId, Boolean isSettled);

    void deleteByGroupTransactionId(Long groupTransactionId);
}
