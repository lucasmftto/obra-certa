package com.obracerta.expense.repository;

import com.obracerta.expense.domain.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByItemIdOrderByPaidAtDesc(Long itemId);

    boolean existsByItemId(Long itemId);

    @Query(value = """
        SELECT e.id, e.description, e.amount, e.paid_at, e.created_at, env.name AS environment_name
        FROM expense e
        JOIN item i ON e.item_id = i.id
        JOIN environment env ON i.environment_id = env.id
        WHERE env.project_id = :projectId
        ORDER BY e.created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findRecentByProject(@Param("projectId") Long projectId, @Param("limit") int limit);
}
