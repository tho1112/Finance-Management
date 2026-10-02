package com.example.demo.Repository;

import com.example.demo.model.TransactionComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionCommentRepository extends JpaRepository<TransactionComment, Long> {

    List<TransactionComment> findByGroupIdOrderByCreatedAtAsc(Long groupId);

    List<TransactionComment> findByGroupTransactionIdOrderByCreatedAtAsc(Long groupTransactionId);

    void deleteByGroupId(Long groupId);

    void deleteByGroupTransactionId(Long groupTransactionId);
}
