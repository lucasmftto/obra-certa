package com.obracerta.project.dto;

import com.obracerta.project.domain.ProjectStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProjectSummaryResponse(
    Long projectId,
    String name,
    ProjectStatus status,
    SummaryTotals totals,
    List<EnvironmentSummary> byEnvironment,
    List<RecentExpense> recentExpenses
) {

    public record SummaryTotals(
        BigDecimal budget,
        BigDecimal actual,
        BigDecimal balance,
        BigDecimal actualPercentage,
        BigDecimal completionPercentage,
        boolean overBudget
    ) {}

    public record EnvironmentSummary(
        Long environmentId,
        String name,
        BigDecimal budget,
        BigDecimal actual,
        BigDecimal balance,
        BigDecimal actualPercentage,
        BigDecimal completionPercentage,
        boolean overBudget
    ) {}

    public record RecentExpense(
        Long expenseId,
        String description,
        BigDecimal amount,
        LocalDate paidAt,
        String environmentName
    ) {}
}
