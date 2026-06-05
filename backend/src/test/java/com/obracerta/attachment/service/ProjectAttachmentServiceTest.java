package com.obracerta.attachment.service;

import com.obracerta.attachment.domain.ProjectAttachment;
import com.obracerta.attachment.dto.AttachmentResponse;
import com.obracerta.attachment.repository.ProjectAttachmentRepository;
import com.obracerta.project.domain.Project;
import com.obracerta.project.domain.ProjectStatus;
import com.obracerta.project.domain.ProjectType;
import com.obracerta.project.repository.ProjectRepository;
import com.obracerta.shared.exception.BusinessException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectAttachmentService — Unit Tests")
class ProjectAttachmentServiceTest {

    @Mock
    private ProjectAttachmentRepository attachmentRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectAttachmentService service;

    private Project project;
    private ProjectAttachment attachment;

    @BeforeEach
    void setUp() {
        project = Project.builder()
            .id(1L).name("Casa da Praia").type(ProjectType.HOUSE)
            .status(ProjectStatus.IN_PROGRESS)
            .environments(new ArrayList<>())
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build();

        attachment = ProjectAttachment.builder()
            .id(10L)
            .project(project)
            .fileName("planta.pdf")
            .contentType("application/pdf")
            .fileSize(1024L)
            .content(new byte[]{1, 2, 3})
            .build();
        attachment.setCreatedAt(LocalDateTime.now());
    }

    // ------------------------------------------------------------------ //
    //  list                                                                //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("list — should return attachments for existing project")
    void list_shouldReturnAttachments_whenProjectExists() {
        when(projectRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(project));
        when(attachmentRepository.findByProjectIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(attachment));

        List<AttachmentResponse> result = service.list(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fileName()).isEqualTo("planta.pdf");
        assertThat(result.get(0).contentType()).isEqualTo("application/pdf");
        assertThat(result.get(0).projectId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("list — should throw EntityNotFoundException when project does not exist")
    void list_shouldThrow_whenProjectNotFound() {
        when(projectRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.list(99L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("99");
    }

    // ------------------------------------------------------------------ //
    //  upload                                                              //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("upload — should save attachment when file is valid PDF")
    void upload_shouldSaveAttachment_whenFileIsValidPdf() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "planta.pdf", "application/pdf", new byte[1024]
        );

        when(projectRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(project));
        when(attachmentRepository.save(any())).thenReturn(attachment);

        AttachmentResponse response = service.upload(1L, file);

        assertThat(response.fileName()).isEqualTo("planta.pdf");
        assertThat(response.contentType()).isEqualTo("application/pdf");
        verify(attachmentRepository).save(argThat(a ->
            a.getFileName().equals("planta.pdf") &&
            a.getProject().getId().equals(1L)
        ));
    }

    @Test
    @DisplayName("upload — should save attachment when file is valid image")
    void upload_shouldSaveAttachment_whenFileIsValidImage() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "foto.jpg", "image/jpeg", new byte[2048]
        );
        ProjectAttachment imgAttachment = ProjectAttachment.builder()
            .id(11L).project(project).fileName("foto.jpg")
            .contentType("image/jpeg").fileSize(2048L)
            .content(new byte[2048]).build();
        imgAttachment.setCreatedAt(LocalDateTime.now());

        when(projectRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(project));
        when(attachmentRepository.save(any())).thenReturn(imgAttachment);

        AttachmentResponse response = service.upload(1L, file);

        assertThat(response.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("upload — should throw BusinessException when file is empty")
    void upload_shouldThrow_whenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file", "vazio.pdf", "application/pdf", new byte[0]
        );

        when(projectRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service.upload(1L, emptyFile))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        verify(attachmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("upload — should throw BusinessException when file exceeds 10 MB")
    void upload_shouldThrow_whenFileTooLarge() {
        byte[] bigContent = new byte[11 * 1024 * 1024];
        MockMultipartFile bigFile = new MockMultipartFile(
            "file", "grande.pdf", "application/pdf", bigContent
        );

        when(projectRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service.upload(1L, bigFile))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY))
            .hasMessageContaining("10 MB");

        verify(attachmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("upload — should throw BusinessException when content type is not allowed")
    void upload_shouldThrow_whenContentTypeNotAllowed() {
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file", "script.js", "application/javascript", new byte[512]
        );

        when(projectRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service.upload(1L, invalidFile))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        verify(attachmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("upload — should throw EntityNotFoundException when project does not exist")
    void upload_shouldThrow_whenProjectNotFound() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.pdf", "application/pdf", new byte[512]
        );

        when(projectRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upload(99L, file))
            .isInstanceOf(EntityNotFoundException.class);
    }

    // ------------------------------------------------------------------ //
    //  getContent                                                          //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("getContent — should return attachment entity when found")
    void getContent_shouldReturnAttachment_whenFound() {
        when(attachmentRepository.findById(10L)).thenReturn(Optional.of(attachment));

        ProjectAttachment result = service.getContent(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getFileName()).isEqualTo("planta.pdf");
        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("getContent — should throw EntityNotFoundException when not found")
    void getContent_shouldThrow_whenNotFound() {
        when(attachmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getContent(99L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("99");
    }

    // ------------------------------------------------------------------ //
    //  delete                                                              //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("delete — should delete attachment when found")
    void delete_shouldDeleteAttachment_whenFound() {
        when(attachmentRepository.findById(10L)).thenReturn(Optional.of(attachment));

        service.delete(10L);

        verify(attachmentRepository).delete(attachment);
    }

    @Test
    @DisplayName("delete — should throw EntityNotFoundException when not found")
    void delete_shouldThrow_whenNotFound() {
        when(attachmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("99");

        verify(attachmentRepository, never()).delete(any());
    }
}
