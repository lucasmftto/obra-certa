package com.obracerta.item.domain;

import com.obracerta.environment.domain.Environment;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = false)
    private Environment environment;

    @Column(nullable = false, length = 300)
    private String description;

    @Column(nullable = false, precision = 9, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, length = 10)
    private String unit;

    @Column(nullable = false, name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "type", length = 20)
    private ItemType type;

    @Builder.Default
    @Column(nullable = false)
    private boolean completed = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean delayed = false;

    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
