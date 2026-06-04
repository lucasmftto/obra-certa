package com.obracerta.environment.mapper;

import com.obracerta.environment.domain.Environment;
import com.obracerta.environment.dto.EnvironmentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EnvironmentMapper {

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "hasDelayedItems", ignore = true)
    EnvironmentResponse toResponse(Environment environment);

    default EnvironmentResponse toResponseWithFlags(Environment environment, boolean hasDelayedItems) {
        return new EnvironmentResponse(
            environment.getId(),
            environment.getProject().getId(),
            environment.getName(),
            environment.getDescription(),
            environment.getCompletionPercentage(),
            hasDelayedItems
        );
    }
}
