package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_histories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "auth_method", length = 50)
    private String authMethod; // LOCAL, GOOGLE_OAUTH

    @Column(name = "is_success")
    private Boolean isSuccess;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "login_at")
    private LocalDateTime loginAt;

    @PrePersist
    protected void onCreate() {
        if (this.loginAt == null) {
            this.loginAt = LocalDateTime.now();
        }
    }
}
