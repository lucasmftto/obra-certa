package com.obracerta.project.service;

import com.obracerta.auth.domain.User;
import com.obracerta.auth.repository.UserRepository;
import com.obracerta.member.repository.ProjectMemberRepository;
import com.obracerta.project.domain.Project;
import com.obracerta.project.domain.ProjectStatus;
import com.obracerta.project.domain.ProjectType;
import com.obracerta.project.dto.ProjectRequest;
import com.obracerta.project.dto.ProjectResponse;
import com.obracerta.project.mapper.ProjectMapper;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService — Unit Tests")
class ProjectServiceTest {

    @Mock
    private ProjectRepository repository;

    @Mock
    private ProjectMapper mapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberRepository memberRepository;

    @InjectMocks
    private ProjectService service;

    private static final Long OWNER_ID = 1L;

    private User owner;
    private Project projectInBudget;
    private Project projectInProgress;
    private ProjectResponse responseStub;

    @BeforeEach
    void setUp() {
        owner = User.builder()
            .id(OWNER_ID)
            .name("John")
            .email("john@example.com")
            .password("hashed")
            .build();

        projectInBudget = Project.builder()
            .id(1L)
            .name("Beach House")
            .type(ProjectType.HOUSE)
            .status(ProjectStatus.IN_BUDGET)
            .owner(owner)
            .environments(new ArrayList<>())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        projectInProgress = Project.builder()
            .id(2L)
            .name("Downtown Apartment")
            .type(ProjectType.APARTMENT)
            .status(ProjectStatus.IN_PROGRESS)
            .owner(owner)
            .environments(new ArrayList<>())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        responseStub = new ProjectResponse(
            1L, "Beach House", ProjectType.HOUSE,
            null, null, ProjectStatus.IN_BUDGET,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            false, false, 0,
            LocalDateTime.now(), LocalDateTime.now()
        );

        // default financial stubs
        lenient().when(repository.calculateTotalBudget(anyLong())).thenReturn(BigDecimal.ZERO);
        lenient().when(repository.calculateTotalActual(anyLong())).thenReturn(BigDecimal.ZERO);
        lenient().when(repository.calculateCompletionPercentage(anyLong())).thenReturn(BigDecimal.ZERO);
        lenient().when(repository.hasDelayedItems(anyLong())).thenReturn(false);
        lenient().when(mapper.toResponseWithCalculations(any(), any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), anyInt()))
            .thenReturn(responseStub);
        // default: current user is owner (not blocked by access check)
        lenient().when(memberRepository.existsByIdProjectIdAndIdUserId(anyLong(), anyLong())).thenReturn(false);
    }

    // ------------------------------------------------------------------ //
    //  create                                                              //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("create — should save with IN_BUDGET status")
    void create_shouldSaveWithInBudgetStatus() {
        ProjectRequest request = new ProjectRequest("Beach House", ProjectType.HOUSE, null, null);

        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(repository.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(1L);
            p.setStatus(ProjectStatus.IN_BUDGET);
            return p;
        });

        ProjectResponse response = service.create(request, OWNER_ID);

        assertThat(response).isNotNull();
        verify(repository).save(argThat(p ->
            p.getStatus() == ProjectStatus.IN_BUDGET &&
            p.getName().equals("Beach House") &&
            p.getOwner().equals(owner)
        ));
    }

    // ------------------------------------------------------------------ //
    //  findById                                                            //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("findById — should throw EntityNotFoundException when not found")
    void findById_shouldThrowEntityNotFoundException_whenNotFound() {
        when(repository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L, OWNER_ID))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("99");
    }

    // ------------------------------------------------------------------ //
    //  updateStatus                                                        //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("updateStatus — should transition correctly: IN_PROGRESS to ON_HOLD")
    void updateStatus_shouldTransitionCorrectly() {
        when(repository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(projectInProgress));
        when(repository.save(any())).thenReturn(projectInProgress);

        ProjectResponse response = service.updateStatus(2L, ProjectStatus.ON_HOLD, OWNER_ID);

        assertThat(response).isNotNull();
        verify(repository).save(argThat(p -> p.getStatus() == ProjectStatus.ON_HOLD));
    }

    @Test
    @DisplayName("updateStatus — should throw exception for invalid transition: COMPLETED → ON_HOLD")
    void updateStatus_shouldThrowException_whenTransitionInvalid() {
        Project completed = Project.builder()
            .id(3L).name("Finished Project").type(ProjectType.HOUSE)
            .status(ProjectStatus.COMPLETED)
            .owner(owner)
            .environments(new ArrayList<>())
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build();

        when(repository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> service.updateStatus(3L, ProjectStatus.ON_HOLD, OWNER_ID))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("não é permitida")
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("updateStatus — should allow reopening: COMPLETED → IN_PROGRESS")
    void updateStatus_shouldAllowReopening_whenCompleted() {
        Project completed = Project.builder()
            .id(3L).name("Finished Project").type(ProjectType.HOUSE)
            .status(ProjectStatus.COMPLETED)
            .owner(owner)
            .environments(new ArrayList<>())
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build();

        when(repository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(completed));
        when(repository.save(any())).thenReturn(completed);

        ProjectResponse response = service.updateStatus(3L, ProjectStatus.IN_PROGRESS, OWNER_ID);

        assertThat(response).isNotNull();
        verify(repository).save(argThat(p -> p.getStatus() == ProjectStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("updateStatus — should throw exception when starting without items")
    void updateStatus_shouldThrowException_whenStartingWithoutItems() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(projectInBudget));
        when(repository.hasItems(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.updateStatus(1L, ProjectStatus.IN_PROGRESS, OWNER_ID))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ao menos um ambiente")
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    // ------------------------------------------------------------------ //
    //  delete                                                              //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("delete — should soft-delete by setting deletedAt")
    void delete_shouldSoftDelete() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(projectInBudget));
        when(repository.save(any())).thenReturn(projectInBudget);

        service.delete(1L, OWNER_ID);

        verify(repository).save(argThat(p -> p.getDeletedAt() != null));
    }

    @Test
    @DisplayName("delete — should throw exception when project status does not allow deletion: IN_PROGRESS")
    void delete_shouldThrowException_whenStatusDoesNotAllowDeletion() {
        when(repository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(projectInProgress));

        assertThatThrownBy(() -> service.delete(2L, OWNER_ID))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("IN_BUDGET")
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }
}
