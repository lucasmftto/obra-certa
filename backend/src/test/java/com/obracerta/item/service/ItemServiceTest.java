package com.obracerta.item.service;

import com.obracerta.environment.domain.Environment;
import com.obracerta.environment.repository.EnvironmentRepository;
import com.obracerta.expense.repository.ExpenseRepository;
import com.obracerta.item.domain.Item;
import com.obracerta.item.domain.ItemType;
import com.obracerta.item.dto.ItemRequest;
import com.obracerta.item.dto.ItemResponse;
import com.obracerta.item.mapper.ItemMapper;
import com.obracerta.item.repository.ItemRepository;
import com.obracerta.project.domain.Project;
import com.obracerta.project.domain.ProjectStatus;
import com.obracerta.project.domain.ProjectType;
import com.obracerta.shared.exception.BusinessException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemService — Unit Tests")
class ItemServiceTest {

    @Mock private ItemRepository itemRepository;
    @Mock private EnvironmentRepository environmentRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private ItemMapper mapper;

    @InjectMocks
    private ItemService service;

    private Project projectInBudget;
    private Project projectCompleted;
    private Environment environmentEditable;
    private Environment environmentCompleted;
    private Item sampleItem;
    private ItemRequest sampleRequest;

    @BeforeEach
    void setUp() {
        projectInBudget = Project.builder()
            .id(1L).name("Casa").type(ProjectType.HOUSE)
            .status(ProjectStatus.IN_BUDGET)
            .build();

        projectCompleted = Project.builder()
            .id(2L).name("Obra Concluída").type(ProjectType.HOUSE)
            .status(ProjectStatus.COMPLETED)
            .build();

        environmentEditable = Environment.builder()
            .id(10L).name("Sala").project(projectInBudget).build();

        environmentCompleted = Environment.builder()
            .id(11L).name("Quarto").project(projectCompleted).build();

        sampleItem = Item.builder()
            .id(100L)
            .environment(environmentEditable)
            .description("Cimento")
            .quantity(new BigDecimal("10.000"))
            .unit("saco")
            .unitPrice(new BigDecimal("45.00"))
            .type(ItemType.MATERIAL)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        sampleRequest = new ItemRequest(
            "Cimento",
            new BigDecimal("10.000"),
            "saco",
            new BigDecimal("45.00"),
            ItemType.MATERIAL
        );
    }

    @Test
    @DisplayName("create — should save item when project is editable (IN_BUDGET)")
    void create_shouldSaveItem_whenProjectIsEditable() {
        when(environmentRepository.findById(10L)).thenReturn(Optional.of(environmentEditable));
        when(itemRepository.save(any(Item.class))).thenReturn(sampleItem);
        when(itemRepository.sumActualByItem(100L)).thenReturn(BigDecimal.ZERO);

        ItemResponse response = new ItemResponse(
            100L, 10L, "Cimento",
            new BigDecimal("10.000"), "saco", new BigDecimal("45.00"),
            ItemType.MATERIAL,
            new BigDecimal("450.00"), BigDecimal.ZERO,
            false, false,
            LocalDateTime.now(), LocalDateTime.now()
        );
        when(mapper.toResponseWithCalculations(any(), any(), any())).thenReturn(response);

        ItemResponse result = service.create(10L, sampleRequest);

        assertThat(result).isNotNull();
        assertThat(result.description()).isEqualTo("Cimento");
        verify(itemRepository).save(argThat(i ->
            i.getDescription().equals("Cimento") &&
            i.getType() == ItemType.MATERIAL
        ));
    }

    @Test
    @DisplayName("create — should throw BusinessException when project is COMPLETED")
    void create_shouldThrow_whenProjectIsCompleted() {
        when(environmentRepository.findById(11L)).thenReturn(Optional.of(environmentCompleted));

        assertThatThrownBy(() -> service.create(11L, sampleRequest))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("concluído");
    }

    @Test
    @DisplayName("delete — should throw BusinessException when item has expenses")
    void delete_shouldThrow_whenItemHasExpenses() {
        when(itemRepository.findById(100L)).thenReturn(Optional.of(sampleItem));
        when(expenseRepository.existsByItemId(100L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(100L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("gastos vinculados");
    }

    @Test
    @DisplayName("findById — should throw EntityNotFoundException when item not found")
    void findById_shouldThrow_whenNotFound() {
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("999");
    }
}
