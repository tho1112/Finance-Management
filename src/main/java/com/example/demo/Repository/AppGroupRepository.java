package com.example.demo.Repository;

import com.example.demo.model.AppGroup;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppGroupRepository extends JpaRepository<AppGroup, Long> {

    List<AppGroup> findByCreatedBy(User createdBy);

    @Query("SELECT g FROM AppGroup g JOIN g.members m WHERE m.user.id = :userId")
    List<AppGroup> findAllByMemberUserId(@Param("userId") Long userId);
}
