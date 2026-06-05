package com.obracerta.attachment.domain;

import com.obracerta.project.domain.Project;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_attachment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 255, name = "file_name")
    private String fileName;

    @Column(nullable = false, length = 100, name = "content_type")
    private String contentType;

    @Column(nullable = false, name = "file_size")
    private Long fileSize;

    @Column(nullable = false, columnDefinition = "BYTEA")
    private byte[] content;

    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onPersist() {
        createdAt = LocalDateTime.now();
    }
}
