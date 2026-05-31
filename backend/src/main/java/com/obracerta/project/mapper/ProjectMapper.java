package com.obracerta.project.mapper;

import com.obracerta.project.domain.Project;
import com.obracerta.project.dto.ProjectResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(target = "totalBudget", ignore = true)
    @Mapping(target = "totalActual", ignore = true)
    @Mapping(target = "remainingBalance", ignore = true)
    @Mapping(target = "actualPercentage", ignore = true)
    @Mapping(target = "completionPercentage", ignore = true)
    @Mapping(target = "overBudget", ignore = true)
    @Mapping(target = "hasDelayedItems", ignore = true)
    @Mapping(target = "roomCount", ignore = true)
    ProjectResponse toResponse(Project project);

    default ProjectResponse toResponseWithCalculations(
            Project project,
            BigDecimal totalBudget,
            BigDecimal totalActual,
            BigDecimal remainingBalance,
            BigDecimal actualPercentage,
            BigDecimal completionPercentage,
            boolean overBudget,
            boolean hasDelayedItems,
            int roomCount) {
        return new ProjectResponse(
            project.getId(),
            project.getName(),
            project.getType(),
            project.getAddress(),
            project.getDescription(),
            project.getStatus(),
            totalBudget,
            totalActual,
            remainingBalance,
            actualPercentage,
            completionPercentage,
            overBudget,
            hasDelayedItems,
            roomCount,
            project.getCreatedAt(),
            project.getUpdatedAt()
        );
    }
}
