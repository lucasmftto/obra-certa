package com.obracerta.environment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.obracerta.environment.dto.EnvironmentRequest;
import com.obracerta.environment.dto.EnvironmentResponse;
import com.obracerta.environment.service.EnvironmentService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EnvironmentController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser
@DisplayName("EnvironmentController — WebMvc Tests")
class EnvironmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EnvironmentService service;

    private EnvironmentResponse environmentStub() {
        return new EnvironmentResponse(10L, 1L, "Sala", null, BigDecimal.ZERO, false);
    }

    // ------------------------------------------------------------------ //
    //  GET /api/v1/projects/{projectId}/environments                       //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("GET /projects/{projectId}/environments — deve retornar 200 com lista")
    void list_shouldReturn200() throws Exception {
        when(service.list(1L)).thenReturn(List.of(environmentStub()));

        mockMvc.perform(get("/api/v1/projects/1/environments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Sala"))
            .andExpect(jsonPath("$[0].projectId").value(1));
    }

    @Test
    @DisplayName("GET /projects/{projectId}/environments — deve retornar 404 quando projeto não existe")
    void list_shouldReturn404_whenProjectNotFound() throws Exception {
        when(service.list(99L)).thenThrow(new EntityNotFoundException("Projeto não encontrado com id: 99"));

        mockMvc.perform(get("/api/v1/projects/99/environments"))
            .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ //
    //  POST /api/v1/projects/{projectId}/environments                      //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("POST /projects/{projectId}/environments — deve retornar 201")
    void create_shouldReturn201() throws Exception {
        EnvironmentRequest request = new EnvironmentRequest("Sala", "Sala de estar", null);
        when(service.create(eq(1L), any())).thenReturn(environmentStub());

        mockMvc.perform(post("/api/v1/projects/1/environments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.name").value("Sala"));
    }

    @Test
    @DisplayName("POST /projects/{projectId}/environments — deve retornar 409 quando projeto concluído")
    void create_shouldReturn409_whenProjectIsCompleted() throws Exception {
        EnvironmentRequest request = new EnvironmentRequest("Sala", null, null);
        when(service.create(eq(2L), any()))
            .thenThrow(new BusinessException("Projeto concluído não pode ser modificado.", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/v1/projects/2/environments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /projects/{projectId}/environments — deve retornar 422 quando nome em branco")
    void create_shouldReturn422_whenNameIsBlank() throws Exception {
        EnvironmentRequest request = new EnvironmentRequest("", null, null);

        mockMvc.perform(post("/api/v1/projects/1/environments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity());
    }

    // ------------------------------------------------------------------ //
    //  DELETE /api/v1/environments/{id}                                    //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("DELETE /environments/{id} — deve retornar 204")
    void delete_shouldReturn204() throws Exception {
        doNothing().when(service).delete(10L);

        mockMvc.perform(delete("/api/v1/environments/10").with(csrf()))
            .andExpect(status().isNoContent());
    }
}
