package com.obracerta.project.service;

import com.obracerta.project.domain.Project;
import com.obracerta.project.domain.ProjectStatus;
import com.obracerta.project.domain.ProjectType;
import com.obracerta.project.dto.ProjectRequest;
import com.obracerta.project.dto.ProjectResponse;
import com.obracerta.project.mapper.ProjectMapper;
import com.obracerta.project.repository.ProjectRepository;
import com.obracerta.shared.exception.BusinessException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository repository;
    private final ProjectMapper mapper;

    @Transactional(readOnly = true)
    public Page<ProjectResponse> list(ProjectStatus status, ProjectType type, String search, Pageable pageable) {
        String searchLike = (search != null && !search.isBlank())
            ? "%" + search.toLowerCase() + "%"
            : null;
        return repository.findWithFilters(status, type, searchLike, pageable)
            .map(this::toResponseWithCalculations);
    }

    @Transactional(readOnly = true)
    public ProjectResponse findById(Long id) {
        Project project = repository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado com id: " + id));
        return toResponseWithCalculations(project);
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        Project project = Project.builder()
            .name(request.name())
            .type(request.type())
            .address(request.address())
            .description(request.description())
            .build();
        project = repository.save(project);
        return toResponseWithCalculations(project);
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = findEntity(id);
        if (project.isCompleted()) {
            throw new BusinessException(
                "Projeto concluído não pode ser editado.",
                HttpStatus.CONFLICT
            );
        }
        project.setName(request.name());
        project.setType(request.type());
        project.setAddress(request.address());
        project.setDescription(request.description());
        project = repository.save(project);
        return toResponseWithCalculations(project);
    }

    @Transactional
    public ProjectResponse updateStatus(Long id, ProjectStatus newStatus) {
        Project project = findEntity(id);
        ProjectStatus currentStatus = project.getStatus();

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new BusinessException(
                String.format("Transição de status '%s' para '%s' não é permitida.", currentStatus, newStatus),
                HttpStatus.CONFLICT
            );
        }

        if (currentStatus == ProjectStatus.IN_BUDGET && newStatus == ProjectStatus.IN_PROGRESS) {
            boolean hasItems = repository.hasItems(id);
            if (!hasItems) {
                throw new BusinessException(
                    "Para iniciar a obra, o projeto deve ter ao menos um ambiente com um item cadastrado.",
                    HttpStatus.UNPROCESSABLE_ENTITY
                );
            }
        }

        project.setStatus(newStatus);
        project = repository.save(project);
        return toResponseWithCalculations(project);
    }

    @Transactional
    public void delete(Long id) {
        Project project = findEntity(id);
        if (project.getStatus() != ProjectStatus.IN_BUDGET) {
            throw new BusinessException(
                "Apenas projetos com status IN_BUDGET podem ser excluídos.",
                HttpStatus.CONFLICT
            );
        }
        project.setDeletedAt(LocalDateTime.now());
        repository.save(project);
    }

    // ---- helpers ----

    private Project findEntity(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado com id: " + id));
    }

    private ProjectResponse toResponseWithCalculations(Project project) {
        BigDecimal totalBudget = repository.calculateTotalBudget(project.getId());
        BigDecimal totalActual = repository.calculateTotalActual(project.getId());
        BigDecimal completionPercentage = repository.calculateCompletionPercentage(project.getId());

        BigDecimal remainingBalance = totalBudget.subtract(totalActual);

        BigDecimal actualPercentage = BigDecimal.ZERO;
        if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
            actualPercentage = totalActual
                .multiply(BigDecimal.valueOf(100))
                .divide(totalBudget, 2, RoundingMode.HALF_UP);
        }

        boolean overBudget = totalActual.compareTo(totalBudget) > 0;
        boolean hasDelayedItems = repository.hasDelayedItems(project.getId());
        int roomCount = project.getEnvironments().size();

        return mapper.toResponseWithCalculations(
            project,
            totalBudget,
            totalActual,
            remainingBalance,
            actualPercentage,
            completionPercentage,
            overBudget,
            hasDelayedItems,
            roomCount
        );
    }
}
