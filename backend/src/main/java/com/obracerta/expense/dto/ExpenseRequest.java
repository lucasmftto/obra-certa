package com.obracerta.expense.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
    Long categoryId,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @PastOrPresent LocalDate paidAt,
    @Size(max = 300) String description,
    @Size(max = 500) String receiptUrl
) {}
