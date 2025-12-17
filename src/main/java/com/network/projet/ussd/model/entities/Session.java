package com.network.projet.ussd.model.entities;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("sessions")
public class Session {
    
    @Id
    private Long id;
    
    @Column("session_id")
    private String sessionId;
    
    @Column("user_id")
    private Long userId;
    
    @Column("current_state")
    private String currentState;
    
    @Column("is_active")
    private Boolean isActive;
    
    @Column("created_at")
    private LocalDateTime createdAt;
    
    @Column("last_activity")
    private LocalDateTime lastActivity;
    
    @Column("expires_at")
    private LocalDateTime expiresAt;
}
