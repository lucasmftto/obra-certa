package com.obracerta.project.repository;

import com.obracerta.project.domain.Project;
import com.obracerta.project.domain.ProjectStatus;
import com.obracerta.project.domain.ProjectType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Page<Project> findByDeletedAtIsNull(Pageable pageable);

    @Query("""
        SELECT p FROM Project p
        WHERE p.deletedAt IS NULL
          AND (:status IS NULL OR p.status = :status)
          AND (:tipo IS NULL OR p.type = :tipo)
          AND (:buscaLike IS NULL
               OR LOWER(p.name) LIKE :buscaLike
               OR (p.address IS NOT NULL AND LOWER(p.address) LIKE :buscaLike))
        """)
    Page<Project> findWithFilters(
        @Param("status") ProjectStatus status,
        @Param("tipo") ProjectType tipo,
        @Param("buscaLike") String buscaLike,
        Pageable pageable
    );

    Optional<Project> findByIdAndDeletedAtIsNull(Long id);

    @Query(value = """
        SELECT COALESCE(SUM(i.quantity * i.unit_price), 0)
        FROM item i
        JOIN environment e ON i.environment_id = e.id
        WHERE e.project_id = :projectId
        """, nativeQuery = true)
    BigDecimal calculateTotalBudget(@Param("projectId") Long projectId);

    @Query(value = """
        SELECT COALESCE(SUM(g.amount), 0)
        FROM expense g
        JOIN item i ON g.item_id = i.id
        JOIN environment e ON i.environment_id = e.id
        WHERE e.project_id = :projectId
        """, nativeQuery = true)
    BigDecimal calculateTotalActual(@Param("projectId") Long projectId);

    @Query(value = """
        SELECT COALESCE(AVG(e.completion_percentage), 0)
        FROM environment e
        WHERE e.project_id = :projectId
        """, nativeQuery = true)
    BigDecimal calculateCompletionPercentage(@Param("projectId") Long projectId);

    @Query(value = """
        SELECT CASE WHEN COUNT(i.id) > 0 THEN true ELSE false END
        FROM item i
        JOIN environment e ON i.environment_id = e.id
        WHERE e.project_id = :projectId
        """, nativeQuery = true)
    boolean hasItems(@Param("projectId") Long projectId);

    @Query(value = """
        SELECT CASE WHEN COUNT(i.id) > 0 THEN true ELSE false END
        FROM item i
        JOIN environment e ON i.environment_id = e.id
        WHERE e.project_id = :projectId AND i.delayed = true
        """, nativeQuery = true)
    boolean hasDelayedItems(@Param("projectId") Long projectId);

    @Query(value = "SELECT COALESCE(SUM(i.unit_price * i.quantity),0) FROM item i JOIN environment e ON i.environment_id = e.id WHERE e.id = :envId",
           nativeQuery = true)
    BigDecimal calculateBudgetByEnvironment(@Param("envId") Long envId);

    @Query(value = "SELECT COALESCE(SUM(ex.amount),0) FROM expense ex JOIN item i ON ex.item_id = i.id WHERE i.environment_id = :envId",
           nativeQuery = true)
    BigDecimal calculateActualByEnvironment(@Param("envId") Long envId);
}
