package com.obracerta.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.obracerta.project.domain.ProjectStatus;
import com.obracerta.project.domain.ProjectType;
import com.obracerta.project.dto.ProjectRequest;
import com.obracerta.project.dto.ProjectResponse;
import com.obracerta.project.dto.StatusUpdateRequest;
import com.obracerta.project.service.ProjectService;
import com.obracerta.project.service.ProjectSummaryService;
import com.obracerta.shared.exception.BusinessException;
import com.obracerta.shared.exception.GlobalExceptionHandler;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser
@DisplayName("ProjectController — WebMvc Tests")
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService service;

    @MockBean
    private ProjectSummaryService summaryService;

    private ProjectResponse projectResponseStub() {
        return new ProjectResponse(
            1L, "Casa da Praia", ProjectType.HOUSE,
            "Rua A, 100", "Minha casa",
            ProjectStatus.IN_BUDGET,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            false, false, 0,
            LocalDateTime.now(), LocalDateTime.now()
        );
    }

    // ------------------------------------------------------------------ //
    //  POST /api/v1/projects                                               //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("POST /projects — deve retornar 201 com projeto criado")
    void create_shouldReturn201() throws Exception {
        ProjectRequest request = new ProjectRequest("Casa da Praia", ProjectType.HOUSE, "Rua A, 100", "Minha casa");
        when(service.create(any())).thenReturn(projectResponseStub());

        mockMvc.perform(post("/api/v1/projects")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Casa da Praia"))
            .andExpect(jsonPath("$.status").value("IN_BUDGET"));
    }

    @Test
    @DisplayName("POST /projects — deve retornar 422 quando nome está em branco")
    void create_shouldReturn422_whenNameIsBlank() throws Exception {
        ProjectRequest request = new ProjectRequest("", ProjectType.HOUSE, null, null);

        mockMvc.perform(post("/api/v1/projects")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.campos").isArray());
    }

    @Test
    @DisplayName("POST /projects — deve retornar 422 quando tipo é nulo")
    void create_shouldReturn422_whenTypeIsNull() throws Exception {
        String json = """
            {"name": "Casa da Praia", "type": null}
            """;

        mockMvc.perform(post("/api/v1/projects")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isUnprocessableEntity());
    }

    // ------------------------------------------------------------------ //
    //  GET /api/v1/projects/{id}                                          //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("GET /projects/{id} — deve retornar 200 com projeto")
    void findById_shouldReturn200() throws Exception {
        when(service.findById(1L)).thenReturn(projectResponseStub());

        mockMvc.perform(get("/api/v1/projects/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.type").value("HOUSE"));
    }

    @Test
    @DisplayName("GET /projects/{id} — deve retornar 404 quando não encontrado")
    void findById_shouldReturn404_whenNotFound() throws Exception {
        when(service.findById(99L)).thenThrow(new EntityNotFoundException("Projeto não encontrado com id: 99"));

        mockMvc.perform(get("/api/v1/projects/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.mensagem").value("Projeto não encontrado com id: 99"));
    }

    // ------------------------------------------------------------------ //
    //  PATCH /api/v1/projects/{id}/status                                 //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("PATCH /projects/{id}/status — deve retornar 409 em transição inválida")
    void updateStatus_shouldReturn409_whenTransitionInvalid() throws Exception {
        StatusUpdateRequest request = new StatusUpdateRequest(ProjectStatus.IN_PROGRESS);
        when(service.updateStatus(anyLong(), any()))
            .thenThrow(new BusinessException("Transição inválida.", HttpStatus.CONFLICT));

        mockMvc.perform(patch("/api/v1/projects/1/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ //
    //  DELETE /api/v1/projects/{id}                                       //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("DELETE /projects/{id} — deve retornar 204")
    void delete_shouldReturn204() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete("/api/v1/projects/1").with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /projects/{id} — deve retornar 409 quando projeto não pode ser excluído")
    void delete_shouldReturn409_whenNotAllowed() throws Exception {
        doThrow(new BusinessException("Apenas projetos com status IN_BUDGET podem ser excluídos.", HttpStatus.CONFLICT))
            .when(service).delete(2L);

        mockMvc.perform(delete("/api/v1/projects/2").with(csrf()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Apenas projetos com status IN_BUDGET podem ser excluídos."));
    }

    // ------------------------------------------------------------------ //
    //  GET /api/v1/projects (listagem paginada)                           //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("GET /projects — deve retornar 200 com página de projetos")
    void list_shouldReturn200WithPage() throws Exception {
        when(service.list(any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(projectResponseStub())));

        mockMvc.perform(get("/api/v1/projects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].name").value("Casa da Praia"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }
}
