package com.obracerta.environment.domain;

import com.obracerta.project.domain.Project;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "environment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Environment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false, precision = 5, scale = 2, name = "completion_percentage")
    @Builder.Default
    private BigDecimal completionPercentage = BigDecimal.ZERO;
}
