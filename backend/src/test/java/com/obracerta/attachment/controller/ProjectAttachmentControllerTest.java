package com.obracerta.attachment.controller;

import com.obracerta.attachment.domain.ProjectAttachment;
import com.obracerta.attachment.dto.AttachmentResponse;
import com.obracerta.attachment.service.ProjectAttachmentService;
import com.obracerta.auth.repository.UserRepository;
import com.obracerta.auth.service.JwtService;
import com.obracerta.project.domain.Project;
import com.obracerta.project.domain.ProjectStatus;
import com.obracerta.project.domain.ProjectType;
import com.obracerta.shared.config.SecurityConfig;
import com.obracerta.shared.exception.BusinessException;
import com.obracerta.shared.exception.GlobalExceptionHandler;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectAttachmentController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
    "app.cors.allowed-origins=http://localhost:4200",
    "app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
    "app.jwt.expiration-ms=604800000"
})
@WithMockUser
@DisplayName("ProjectAttachmentController — WebMvc Tests")
class ProjectAttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectAttachmentService service;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    private AttachmentResponse attachmentStub() {
        return new AttachmentResponse(10L, 1L, "planta.pdf", "application/pdf", 1024L, LocalDateTime.now());
    }

    private ProjectAttachment attachmentEntityStub() {
        Project project = Project.builder()
            .id(1L).name("Casa da Praia").type(ProjectType.HOUSE)
            .status(ProjectStatus.IN_PROGRESS).environments(new ArrayList<>())
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build();

        ProjectAttachment a = ProjectAttachment.builder()
            .id(10L).project(project)
            .fileName("planta.pdf").contentType("application/pdf")
            .fileSize(1024L).content(new byte[]{1, 2, 3})
            .build();
        a.setCreatedAt(LocalDateTime.now());
        return a;
    }

    // ------------------------------------------------------------------ //
    //  GET /api/v1/projects/{projectId}/attachments                        //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("GET /attachments — should return 200 with attachment list")
    void list_shouldReturn200() throws Exception {
        when(service.list(1L)).thenReturn(List.of(attachmentStub()));

        mockMvc.perform(get("/api/v1/projects/1/attachments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(10))
            .andExpect(jsonPath("$[0].fileName").value("planta.pdf"))
            .andExpect(jsonPath("$[0].contentType").value("application/pdf"))
            .andExpect(jsonPath("$[0].projectId").value(1));
    }

    @Test
    @DisplayName("GET /attachments — should return 200 with empty list when no attachments")
    void list_shouldReturn200_whenEmpty() throws Exception {
        when(service.list(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/projects/1/attachments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /attachments — should return 404 when project does not exist")
    void list_shouldReturn404_whenProjectNotFound() throws Exception {
        when(service.list(99L)).thenThrow(new EntityNotFoundException("Projeto não encontrado com id: 99"));

        mockMvc.perform(get("/api/v1/projects/99/attachments"))
            .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ //
    //  POST /api/v1/projects/{projectId}/attachments                       //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("POST /attachments — should return 201 when file is valid")
    void upload_shouldReturn201_whenFileIsValid() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "planta.pdf", "application/pdf", new byte[1024]
        );
        when(service.upload(eq(1L), any())).thenReturn(attachmentStub());

        mockMvc.perform(multipart("/api/v1/projects/1/attachments")
                .file(file)
                .with(csrf()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.fileName").value("planta.pdf"));
    }

    @Test
    @DisplayName("POST /attachments — should return 422 when file is empty")
    void upload_shouldReturn422_whenFileIsEmpty() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "vazio.pdf", "application/pdf", new byte[0]
        );
        when(service.upload(eq(1L), any()))
            .thenThrow(new BusinessException("O arquivo não pode ser vazio.", HttpStatus.UNPROCESSABLE_ENTITY));

        mockMvc.perform(multipart("/api/v1/projects/1/attachments")
                .file(file)
                .with(csrf()))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /attachments — should return 422 when file exceeds 10 MB")
    void upload_shouldReturn422_whenFileTooLarge() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "grande.pdf", "application/pdf", new byte[1024]
        );
        when(service.upload(eq(1L), any()))
            .thenThrow(new BusinessException("O arquivo excede o tamanho máximo permitido de 10 MB.", HttpStatus.UNPROCESSABLE_ENTITY));

        mockMvc.perform(multipart("/api/v1/projects/1/attachments")
                .file(file)
                .with(csrf()))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /attachments — should return 422 when content type is not allowed")
    void upload_shouldReturn422_whenContentTypeNotAllowed() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "script.js", "application/javascript", new byte[512]
        );
        when(service.upload(eq(1L), any()))
            .thenThrow(new BusinessException("Tipo de arquivo não permitido.", HttpStatus.UNPROCESSABLE_ENTITY));

        mockMvc.perform(multipart("/api/v1/projects/1/attachments")
                .file(file)
                .with(csrf()))
            .andExpect(status().isUnprocessableEntity());
    }

    // ------------------------------------------------------------------ //
    //  GET /api/v1/projects/{projectId}/attachments/{id}/content           //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("GET /attachments/{id}/content — should return 200 with PDF content and inline disposition")
    void getContent_shouldReturn200_withInlineDispositionForPdf() throws Exception {
        when(service.getContent(10L)).thenReturn(attachmentEntityStub());

        mockMvc.perform(get("/api/v1/projects/1/attachments/10/content"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/pdf"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("inline")))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("planta.pdf")));
    }

    @Test
    @DisplayName("GET /attachments/{id}/content — should return attachment disposition for Excel")
    void getContent_shouldReturnAttachmentDisposition_forExcel() throws Exception {
        Project project = Project.builder()
            .id(1L).name("Casa").type(ProjectType.HOUSE)
            .status(ProjectStatus.IN_PROGRESS).environments(new ArrayList<>())
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build();
        ProjectAttachment excelAttachment = ProjectAttachment.builder()
            .id(11L).project(project)
            .fileName("orcamento.xlsx")
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .fileSize(512L).content(new byte[]{4, 5, 6})
            .build();
        excelAttachment.setCreatedAt(LocalDateTime.now());

        when(service.getContent(11L)).thenReturn(excelAttachment);

        mockMvc.perform(get("/api/v1/projects/1/attachments/11/content"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("orcamento.xlsx")));
    }

    @Test
    @DisplayName("GET /attachments/{id}/content — should return 404 when attachment not found")
    void getContent_shouldReturn404_whenNotFound() throws Exception {
        when(service.getContent(99L)).thenThrow(new EntityNotFoundException("Anexo não encontrado com id: 99"));

        mockMvc.perform(get("/api/v1/projects/1/attachments/99/content"))
            .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ //
    //  DELETE /api/v1/projects/{projectId}/attachments/{id}                //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("DELETE /attachments/{id} — should return 204")
    void delete_shouldReturn204() throws Exception {
        doNothing().when(service).delete(10L);

        mockMvc.perform(delete("/api/v1/projects/1/attachments/10").with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /attachments/{id} — should return 404 when attachment not found")
    void delete_shouldReturn404_whenNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Anexo não encontrado com id: 99"))
            .when(service).delete(99L);

        mockMvc.perform(delete("/api/v1/projects/1/attachments/99").with(csrf()))
            .andExpect(status().isNotFound());
    }
}
