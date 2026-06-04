package com.obracerta.project.service;

import com.obracerta.environment.domain.Environment;
import com.obracerta.environment.repository.EnvironmentRepository;
import com.obracerta.expense.repository.ExpenseRepository;
import com.obracerta.project.domain.Project;
import com.obracerta.project.dto.ProjectSummaryResponse;
import com.obracerta.project.dto.ProjectSummaryResponse.EnvironmentSummary;
import com.obracerta.project.dto.ProjectSummaryResponse.RecentExpense;
import com.obracerta.project.dto.ProjectSummaryResponse.SummaryTotals;
import com.obracerta.project.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectSummaryService {

    private static final int RECENT_LIMIT = 5;

    private final ProjectRepository projectRepository;
    private final EnvironmentRepository environmentRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public ProjectSummaryResponse summary(Long projectId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado com id: " + projectId));

        List<Environment> environments = environmentRepository.findByProjectIdOrderByNameAsc(projectId);

        List<EnvironmentSummary> envSummaries = environments.stream()
            .map(this::buildEnvironmentSummary)
            .toList();

        BigDecimal totalBudget = projectRepository.calculateTotalBudget(projectId);
        BigDecimal totalActual = projectRepository.calculateTotalActual(projectId);
        BigDecimal completionPct = projectRepository.calculateCompletionPercentage(projectId);

        BigDecimal balance = totalBudget.subtract(totalActual);
        BigDecimal actualPct = computePercentage(totalActual, totalBudget);
        boolean overBudget = totalActual.compareTo(totalBudget) > 0;

        SummaryTotals totals = new SummaryTotals(
            totalBudget, totalActual, balance, actualPct, completionPct, overBudget
        );

        List<RecentExpense> recentExpenses = expenseRepository
            .findRecentByProject(projectId, RECENT_LIMIT)
            .stream()
            .map(this::mapToRecentExpense)
            .toList();

        return new ProjectSummaryResponse(
            project.getId(),
            project.getName(),
            project.getStatus(),
            totals,
            envSummaries,
            recentExpenses
        );
    }

    // ---- helpers ----

    private EnvironmentSummary buildEnvironmentSummary(Environment env) {
        Long envId = env.getId();
        BigDecimal budget = projectRepository.calculateBudgetByEnvironment(envId);
        BigDecimal actual = projectRepository.calculateActualByEnvironment(envId);
        BigDecimal balance = budget.subtract(actual);
        BigDecimal actualPct = computePercentage(actual, budget);
        boolean overBudget = actual.compareTo(budget) > 0;

        return new EnvironmentSummary(
            envId, env.getName(),
            budget, actual, balance, actualPct,
            env.getCompletionPercentage(),
            overBudget
        );
    }

    private RecentExpense mapToRecentExpense(Object[] row) {
        Long expenseId = toLong(row[0]);
        String description = (String) row[1];
        BigDecimal amount = toBigDecimal(row[2]);
        LocalDate paidAt = row[3] != null ? toLocalDate(row[3]) : null;
        String environmentName = (String) row[5];

        return new RecentExpense(expenseId, description, amount, paidAt, environmentName);
    }

    private BigDecimal computePercentage(BigDecimal value, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return value.multiply(BigDecimal.valueOf(100))
            .divide(total, 2, RoundingMode.HALF_UP);
    }

    private Long toLong(Object o) {
        if (o instanceof Long l) return l;
        if (o instanceof Number n) return n.longValue();
        return null;
    }

    private BigDecimal toBigDecimal(Object o) {
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Number n) return new BigDecimal(n.toString());
        return BigDecimal.ZERO;
    }

    private LocalDate toLocalDate(Object o) {
        if (o instanceof LocalDate ld) return ld;
        if (o instanceof Date d) return d.toLocalDate();
        return null;
    }
}
