package com.example.demo.Repository;

import com.example.demo.model.GroupTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupTransactionRepository extends JpaRepository<GroupTransaction, Long> {

    List<GroupTransaction> findByGroupIdOrderByTransactionDateDesc(Long groupId);

    List<GroupTransaction> findByPayerId(Long payerId);

    void deleteByGroupId(Long groupId);
}
