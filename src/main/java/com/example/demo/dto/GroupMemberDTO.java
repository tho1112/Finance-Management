package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberDTO {
    private Long userId;
    private String username;
    private String fullName;
    private String avatarUrl;
    private String role;
    private String nickname;
    private String userCode;
    private LocalDateTime joinedAt;
}
