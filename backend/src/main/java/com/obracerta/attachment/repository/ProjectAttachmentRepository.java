package com.obracerta.attachment.repository;

import com.obracerta.attachment.domain.ProjectAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectAttachmentRepository extends JpaRepository<ProjectAttachment, Long> {

    @Query("SELECT a FROM ProjectAttachment a WHERE a.project.id = :projectId ORDER BY a.createdAt DESC")
    List<ProjectAttachment> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
