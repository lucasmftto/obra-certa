package com.obracerta.expense.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpenseResponse(
    Long id,
    Long itemId,
    Long categoryId,
    String categoryName,
    BigDecimal amount,
    LocalDate paidAt,
    String description,
    String receiptUrl,
    LocalDateTime createdAt
) {}
