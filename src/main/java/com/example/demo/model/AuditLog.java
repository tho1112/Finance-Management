package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Column(nullable = false, length = 50)
    private String action; // LOCK_USER, UNLOCK_USER, UPDATE_ROLE, RESOLVE_TICKET, BROADCAST_ANNOUNCEMENT...

    @Column(name = "target_entity", length = 50)
    private String targetEntity; // USER, TICKET, ANNOUNCEMENT, SYSTEM

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "detail_description", columnDefinition = "TEXT")
    private String detailDescription;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}
