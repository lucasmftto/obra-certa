package com.obracerta.expense.mapper;

import com.obracerta.expense.domain.Expense;
import com.obracerta.expense.dto.ExpenseResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExpenseMapper {

    @Mapping(source = "item.id", target = "itemId")
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    ExpenseResponse toResponse(Expense expense);
}
