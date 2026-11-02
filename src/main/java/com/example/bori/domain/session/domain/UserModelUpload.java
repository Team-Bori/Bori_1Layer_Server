package com.example.bori.domain.session.domain;

import com.example.bori.domain.board.domain.CloudBoard;
import com.example.bori.domain.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 학생이 GitHub 링크로 올린 모델
@Getter
@Entity
@Table(name = "user_model_upload")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserModelUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cloud_board_id")
    private CloudBoard cloudBoard;

    @Column(name = "github_url", nullable = false, length = 500)
    private String githubUrl;

    @Column(name = "model_format", nullable = false, length = 30)
    private String modelFormat;

    // 처음엔 학생이 입력한 값, 내려받은 뒤엔 실제 zip 크기(MB, 올림)
    @Column(name = "size_mb", nullable = false)
    private int sizeMb;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public UserModelUpload(User user, CloudBoard cloudBoard, String githubUrl, String modelFormat, int sizeMb) {
        this.user = user;
        this.cloudBoard = cloudBoard;
        this.githubUrl = githubUrl;
        this.modelFormat = modelFormat;
        this.sizeMb = sizeMb;
    }

    public void updateActualSize(int sizeMb) {
        this.sizeMb = sizeMb;
    }

    // "owner/repo"
    public String repoPath() {
        return githubUrl.replace("https://github.com/", "");
    }
}
