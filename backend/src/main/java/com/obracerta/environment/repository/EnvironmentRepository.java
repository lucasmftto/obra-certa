package com.obracerta.environment.repository;

import com.obracerta.environment.domain.Environment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnvironmentRepository extends JpaRepository<Environment, Long> {

    List<Environment> findByProjectIdOrderBySortOrderAsc(Long projectId);
}
