package com.obracerta.attachment.service;

import com.obracerta.attachment.domain.ProjectAttachment;
import com.obracerta.attachment.dto.AttachmentResponse;
import com.obracerta.attachment.repository.ProjectAttachmentRepository;
import com.obracerta.project.domain.Project;
import com.obracerta.project.repository.ProjectRepository;
import com.obracerta.shared.exception.BusinessException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectAttachmentService {

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
        "application/pdf",
        "image/jpeg",
        "image/png",
        "image/webp",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final ProjectAttachmentRepository attachmentRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(Long projectId) {
        findProject(projectId);
        return attachmentRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public AttachmentResponse upload(Long projectId, MultipartFile file) {
        Project project = findProject(projectId);
        validateFile(file);

        try {
            ProjectAttachment attachment = ProjectAttachment.builder()
                .project(project)
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .content(file.getBytes())
                .build();

            return toResponse(attachmentRepository.save(attachment));
        } catch (IOException e) {
            throw new BusinessException("Erro ao processar o arquivo.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public ProjectAttachment getContent(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new EntityNotFoundException("Anexo não encontrado com id: " + attachmentId));
    }

    @Transactional
    public void delete(Long attachmentId) {
        ProjectAttachment attachment = attachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new EntityNotFoundException("Anexo não encontrado com id: " + attachmentId));
        attachmentRepository.delete(attachment);
    }

    // ---- helpers ----

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("O arquivo não pode ser vazio.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException("O arquivo excede o tamanho máximo permitido de 10 MB.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusinessException(
                "Tipo de arquivo não permitido. Tipos aceitos: PDF, JPEG, PNG, WebP, XLS, XLSX.",
                HttpStatus.UNPROCESSABLE_ENTITY
            );
        }
    }

    private Project findProject(Long projectId) {
        return projectRepository.findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado com id: " + projectId));
    }

    private AttachmentResponse toResponse(ProjectAttachment a) {
        return new AttachmentResponse(
            a.getId(),
            a.getProject().getId(),
            a.getFileName(),
            a.getContentType(),
            a.getFileSize(),
            a.getCreatedAt()
        );
    }
}
