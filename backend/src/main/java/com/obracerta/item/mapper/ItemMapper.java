package com.obracerta.item.mapper;

import com.obracerta.item.domain.Item;
import com.obracerta.item.dto.ItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    @Mapping(source = "environment.id", target = "environmentId")
    @Mapping(target = "totalBudgeted", ignore = true)
    @Mapping(target = "totalActual", ignore = true)
    ItemResponse toResponse(Item item);

    default ItemResponse toResponseWithCalculations(
            Item item,
            BigDecimal totalBudgeted,
            BigDecimal totalActual) {
        return new ItemResponse(
            item.getId(),
            item.getEnvironment().getId(),
            item.getDescription(),
            item.getQuantity(),
            item.getUnit(),
            item.getUnitPrice(),
            item.getType(),
            totalBudgeted,
            totalActual,
            item.isCompleted(),
            item.isDelayed(),
            item.getCreatedAt(),
            item.getUpdatedAt()
        );
    }
}
