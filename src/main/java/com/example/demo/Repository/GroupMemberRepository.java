package com.example.demo.Repository;

import com.example.demo.model.GroupMember;
import com.example.demo.model.GroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {

    List<GroupMember> findByGroupId(Long groupId);

    List<GroupMember> findByUserId(Long userId);

    boolean existsById(GroupMemberId id);

    void deleteByGroupId(Long groupId);
}
