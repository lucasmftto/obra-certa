package com.obracerta.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.obracerta.auth.domain.User;
import com.obracerta.auth.repository.UserRepository;
import com.obracerta.auth.service.JwtService;
import com.obracerta.member.domain.MemberRole;
import com.obracerta.shared.config.SecurityConfig;
import com.obracerta.member.dto.MemberRequest;
import com.obracerta.member.dto.MemberResponse;
import com.obracerta.member.service.ProjectMemberService;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectMemberController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
    "app.cors.allowed-origins=http://localhost:4200",
    "app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
    "app.jwt.expiration-ms=604800000"
})
@DisplayName("ProjectMemberController — WebMvc Tests")
class ProjectMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectMemberService memberService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    private User mockUser() {
        return User.builder()
            .id(1L).name("Owner").email("owner@example.com").password("hashed").build();
    }

    private MemberResponse memberResponseStub() {
        return new MemberResponse(2L, "Member User", "member@example.com", MemberRole.EDITOR);
    }

    // ------------------------------------------------------------------ //
    //  GET /api/v1/projects/{projectId}/members                           //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("GET /projects/{id}/members — deve retornar 200 com lista de membros")
    void list_shouldReturn200() throws Exception {
        when(memberService.list(eq(10L), anyLong())).thenReturn(List.of(memberResponseStub()));

        mockMvc.perform(get("/api/v1/projects/10/members").with(user(mockUser())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].email").value("member@example.com"))
            .andExpect(jsonPath("$[0].role").value("EDITOR"));
    }

    @Test
    @DisplayName("GET /projects/{id}/members — deve retornar 403 quando sem acesso")
    void list_shouldReturn403_whenNoAccess() throws Exception {
        when(memberService.list(eq(10L), anyLong()))
            .thenThrow(new BusinessException("Acesso negado ao projeto.", HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/api/v1/projects/10/members").with(user(mockUser())))
            .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ //
    //  POST /api/v1/projects/{projectId}/members                         //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("POST /projects/{id}/members — deve retornar 201 ao adicionar membro")
    void add_shouldReturn201() throws Exception {
        MemberRequest request = new MemberRequest("member@example.com", MemberRole.EDITOR);
        when(memberService.add(eq(10L), any(), anyLong())).thenReturn(memberResponseStub());

        mockMvc.perform(post("/api/v1/projects/10/members")
                .with(csrf())
                .with(user(mockUser()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userId").value(2))
            .andExpect(jsonPath("$.role").value("EDITOR"));
    }

    @Test
    @DisplayName("POST /projects/{id}/members — deve retornar 409 quando membro já existe")
    void add_shouldReturn409_whenAlreadyMember() throws Exception {
        MemberRequest request = new MemberRequest("member@example.com", MemberRole.EDITOR);
        when(memberService.add(eq(10L), any(), anyLong()))
            .thenThrow(new BusinessException("Usuário já é membro deste projeto.", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/v1/projects/10/members")
                .with(csrf())
                .with(user(mockUser()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ //
    //  DELETE /api/v1/projects/{projectId}/members/{userId}              //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("DELETE /projects/{id}/members/{userId} — deve retornar 204")
    void remove_shouldReturn204() throws Exception {
        doNothing().when(memberService).remove(eq(10L), eq(2L), anyLong());

        mockMvc.perform(delete("/api/v1/projects/10/members/2")
                .with(csrf())
                .with(user(mockUser())))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /projects/{id}/members/{userId} — deve retornar 404 quando membro não encontrado")
    void remove_shouldReturn404_whenMemberNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Membro não encontrado no projeto."))
            .when(memberService).remove(eq(10L), eq(99L), anyLong());

        mockMvc.perform(delete("/api/v1/projects/10/members/99")
                .with(csrf())
                .with(user(mockUser())))
            .andExpect(status().isNotFound());
    }
}
