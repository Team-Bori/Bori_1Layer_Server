package com.example.bori.domain.model.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 개발팀이 검증한 모델
@Getter
@Entity
@Table(name = "ai_model")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 30)
    private String format;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "repo_url", nullable = false, length = 500)
    private String repoUrl;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AiModel(String name, String format, String description, String repoUrl) {
        this.name = name;
        this.format = format;
        this.description = description;
        this.repoUrl = repoUrl;
    }

    public void update(String name, String format, String description, String repoUrl) {
        this.name = name;
        this.format = format;
        this.description = description;
        this.repoUrl = repoUrl;
    }
}
