package com.obracerta.environment.service;

import com.obracerta.environment.domain.Environment;
import com.obracerta.environment.dto.EnvironmentRequest;
import com.obracerta.environment.dto.EnvironmentResponse;
import com.obracerta.environment.mapper.EnvironmentMapper;
import com.obracerta.environment.repository.EnvironmentRepository;
import com.obracerta.item.repository.ItemRepository;
import com.obracerta.project.domain.Project;
import com.obracerta.project.domain.ProjectStatus;
import com.obracerta.project.repository.ProjectRepository;
import com.obracerta.shared.exception.BusinessException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final ProjectRepository projectRepository;
    private final ItemRepository itemRepository;
    private final EnvironmentMapper mapper;

    @Transactional(readOnly = true)
    public List<EnvironmentResponse> list(Long projectId) {
        findProject(projectId);
        return environmentRepository.findByProjectIdOrderBySortOrderAsc(projectId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public EnvironmentResponse findById(Long id) {
        return toResponse(findEnvironment(id));
    }

    @Transactional
    public EnvironmentResponse create(Long projectId, EnvironmentRequest request) {
        Project project = findProject(projectId);
        validateProjectEditable(project);

        Environment environment = Environment.builder()
            .project(project)
            .name(request.name())
            .description(request.description())
            .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
            .completionPercentage(request.completionPercentage() != null
                ? request.completionPercentage()
                : BigDecimal.ZERO)
            .build();

        return toResponse(environmentRepository.save(environment));
    }

    @Transactional
    public EnvironmentResponse update(Long id, EnvironmentRequest request) {
        Environment environment = findEnvironment(id);
        validateProjectEditable(environment.getProject());

        environment.setName(request.name());
        environment.setDescription(request.description());
        if (request.sortOrder() != null) environment.setSortOrder(request.sortOrder());
        if (request.completionPercentage() != null) environment.setCompletionPercentage(request.completionPercentage());

        return toResponse(environmentRepository.save(environment));
    }

    @Transactional
    public void delete(Long id) {
        Environment environment = findEnvironment(id);
        validateProjectEditable(environment.getProject());
        environmentRepository.delete(environment);
    }

    // ---- helpers ----

    private EnvironmentResponse toResponse(Environment env) {
        boolean hasDelayed = itemRepository.hasDelayedItems(env.getId());
        return mapper.toResponseWithFlags(env, hasDelayed);
    }

    private Project findProject(Long projectId) {
        return projectRepository.findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado com id: " + projectId));
    }

    private Environment findEnvironment(Long id) {
        return environmentRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Ambiente não encontrado com id: " + id));
    }

    private void validateProjectEditable(Project project) {
        if (project.getStatus() == ProjectStatus.COMPLETED) {
            throw new BusinessException(
                "Não é permitido modificar ambientes de um projeto concluído.",
                HttpStatus.CONFLICT
            );
        }
    }
}
