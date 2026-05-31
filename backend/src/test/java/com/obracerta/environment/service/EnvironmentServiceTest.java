package com.obracerta.environment.service;

import com.obracerta.environment.domain.Environment;
import com.obracerta.environment.dto.EnvironmentRequest;
import com.obracerta.environment.dto.EnvironmentResponse;
import com.obracerta.environment.mapper.EnvironmentMapper;
import com.obracerta.environment.repository.EnvironmentRepository;
import com.obracerta.project.domain.Project;
import com.obracerta.project.domain.ProjectStatus;
import com.obracerta.project.domain.ProjectType;
import com.obracerta.item.repository.ItemRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnvironmentService — Unit Tests")
class EnvironmentServiceTest {

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private EnvironmentMapper mapper;

    @InjectMocks
    private EnvironmentService service;

    private Project activeProject;
    private Project completedProject;
    private Environment existingEnvironment;
    private EnvironmentResponse responseStub;

    @BeforeEach
    void setUp() {
        activeProject = Project.builder()
            .id(1L).name("Casa da Praia").type(ProjectType.HOUSE)
            .status(ProjectStatus.IN_PROGRESS)
            .environments(new ArrayList<>())
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build();

        completedProject = Project.builder()
            .id(2L).name("Obra Finalizada").type(ProjectType.APARTMENT)
            .status(ProjectStatus.COMPLETED)
            .environments(new ArrayList<>())
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build();

        existingEnvironment = Environment.builder()
            .id(10L).project(activeProject).name("Sala").sortOrder(1)
            .completionPercentage(BigDecimal.ZERO)
            .build();

        responseStub = new EnvironmentResponse(10L, 1L, "Sala", null, 1, BigDecimal.ZERO, false);
    }

    // ------------------------------------------------------------------ //
    //  create                                                              //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("create — should save environment when project is active")
    void create_shouldSaveEnvironment_whenProjectIsActive() {
        EnvironmentRequest request = new EnvironmentRequest("Sala", null, 1, null);

        when(projectRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(activeProject));
        when(environmentRepository.save(any(Environment.class))).thenReturn(existingEnvironment);
        when(itemRepository.hasDelayedItems(anyLong())).thenReturn(false);
        when(mapper.toResponseWithFlags(any(), anyBoolean())).thenReturn(responseStub);

        EnvironmentResponse response = service.create(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Sala");
        verify(environmentRepository).save(argThat(e ->
            e.getName().equals("Sala") && e.getProject().getId().equals(1L)
        ));
    }

    @Test
    @DisplayName("create — should throw BusinessException when project is completed")
    void create_shouldThrowBusinessException_whenProjectIsCompleted() {
        EnvironmentRequest request = new EnvironmentRequest("Quarto", null, 2, null);

        when(projectRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(completedProject));

        assertThatThrownBy(() -> service.create(2L, request))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(environmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("findById — should throw EntityNotFoundException when not found")
    void findById_shouldThrowEntityNotFoundException_whenNotFound() {
        when(environmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("99");
    }

    @Test
    @DisplayName("delete — should delete environment when project is active")
    void delete_shouldDeleteEnvironment_whenProjectIsActive() {
        when(environmentRepository.findById(10L)).thenReturn(Optional.of(existingEnvironment));

        service.delete(10L);

        verify(environmentRepository).delete(existingEnvironment);
    }

    @Test
    @DisplayName("list — should return environments ordered by sortOrder")
    void list_shouldReturnEnvironmentsOrderedBySortOrder() {
        Environment e1 = Environment.builder().id(1L).project(activeProject).name("Sala").sortOrder(1).completionPercentage(BigDecimal.ZERO).build();
        Environment e2 = Environment.builder().id(2L).project(activeProject).name("Quarto").sortOrder(2).completionPercentage(BigDecimal.ZERO).build();
        EnvironmentResponse resp1 = new EnvironmentResponse(1L, 1L, "Sala", null, 1, BigDecimal.ZERO, false);
        EnvironmentResponse resp2 = new EnvironmentResponse(2L, 1L, "Quarto", null, 2, BigDecimal.ZERO, false);

        when(projectRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(activeProject));
        when(environmentRepository.findByProjectIdOrderBySortOrderAsc(1L)).thenReturn(List.of(e1, e2));
        when(itemRepository.hasDelayedItems(anyLong())).thenReturn(false);
        when(mapper.toResponseWithFlags(e1, false)).thenReturn(resp1);
        when(mapper.toResponseWithFlags(e2, false)).thenReturn(resp2);

        List<EnvironmentResponse> result = service.list(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Sala");
        assertThat(result.get(1).name()).isEqualTo("Quarto");
    }
}
