package com.obracerta.expense.service;

import com.obracerta.category.repository.CategoryRepository;
import com.obracerta.environment.domain.Environment;
import com.obracerta.expense.domain.Expense;
import com.obracerta.expense.dto.ExpenseRequest;
import com.obracerta.expense.dto.ExpenseResponse;
import com.obracerta.expense.mapper.ExpenseMapper;
import com.obracerta.expense.repository.ExpenseRepository;
import com.obracerta.item.domain.Item;
import com.obracerta.item.domain.ItemType;
import com.obracerta.item.repository.ItemRepository;
import com.obracerta.project.domain.Project;
import com.obracerta.project.domain.ProjectStatus;
import com.obracerta.project.domain.ProjectType;
import com.obracerta.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService — Unit Tests")
class ExpenseServiceTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ExpenseMapper mapper;

    @InjectMocks
    private ExpenseService service;

    private Item itemInProgress;
    private Item itemInBudget;
    private Expense sampleExpense;

    @BeforeEach
    void setUp() {
        Project projectInProgress = Project.builder()
            .id(1L).name("Obra Ativa").type(ProjectType.HOUSE)
            .status(ProjectStatus.IN_PROGRESS).build();

        Project projectInBudget = Project.builder()
            .id(2L).name("Obra Planejada").type(ProjectType.HOUSE)
            .status(ProjectStatus.IN_BUDGET).build();

        itemInProgress = Item.builder()
            .id(100L).description("Tinta").quantity(BigDecimal.TEN)
            .unit("lata").unitPrice(new BigDecimal("150.00"))
            .type(ItemType.MATERIAL)
            .environment(Environment.builder().id(10L).name("Sala").project(projectInProgress).build())
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build();

        itemInBudget = Item.builder()
            .id(101L).description("Tijolos").quantity(BigDecimal.TEN)
            .unit("mil").unitPrice(new BigDecimal("800.00"))
            .type(ItemType.MATERIAL)
            .environment(Environment.builder().id(11L).name("Quarto").project(projectInBudget).build())
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build();

        sampleExpense = Expense.builder()
            .id(1000L).item(itemInProgress).amount(new BigDecimal("500.00"))
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("create — should save expense when project is IN_PROGRESS")
    void create_shouldSaveExpense_whenProjectIsInProgress() {
        ExpenseRequest request = new ExpenseRequest(null, new BigDecimal("500.00"), null, "Compra de tinta", null);
        when(itemRepository.findById(100L)).thenReturn(Optional.of(itemInProgress));
        when(expenseRepository.save(any(Expense.class))).thenReturn(sampleExpense);
        ExpenseResponse stub = new ExpenseResponse(1000L, 100L, null, null, new BigDecimal("500.00"),
            null, "Compra de tinta", null, LocalDateTime.now());
        when(mapper.toResponse(any(Expense.class))).thenReturn(stub);

        ExpenseResponse result = service.create(100L, request);

        assertThat(result.amount()).isEqualByComparingTo("500.00");
        verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    @DisplayName("create — should throw BusinessException when project is not IN_PROGRESS")
    void create_shouldThrow_whenProjectIsNotInProgress() {
        when(itemRepository.findById(101L)).thenReturn(Optional.of(itemInBudget));
        ExpenseRequest request = new ExpenseRequest(null, new BigDecimal("200.00"), null, null, null);

        assertThatThrownBy(() -> service.create(101L, request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("IN_PROGRESS");
    }

    @Test
    @DisplayName("create — should throw BusinessException when paidAt is in the future")
    void create_shouldThrow_whenPaidAtIsFuture() {
        when(itemRepository.findById(100L)).thenReturn(Optional.of(itemInProgress));
        ExpenseRequest request = new ExpenseRequest(
            null, new BigDecimal("300.00"), LocalDate.now().plusDays(1), null, null);

        assertThatThrownBy(() -> service.create(100L, request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("futuro");
    }

    @Test
    @DisplayName("list — should return expenses ordered by paidAt desc")
    void list_shouldReturnExpensesByItem() {
        when(itemRepository.findById(100L)).thenReturn(Optional.of(itemInProgress));
        Expense e1 = Expense.builder().id(1L).item(itemInProgress).amount(new BigDecimal("100.00"))
            .paidAt(LocalDate.now()).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        Expense e2 = Expense.builder().id(2L).item(itemInProgress).amount(new BigDecimal("200.00"))
            .paidAt(LocalDate.now().minusDays(1)).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(expenseRepository.findByItemIdOrderByPaidAtDesc(100L)).thenReturn(List.of(e1, e2));
        ExpenseResponse r1 = new ExpenseResponse(1L, 100L, null, null, new BigDecimal("100.00"),
            LocalDate.now(), null, null, LocalDateTime.now());
        ExpenseResponse r2 = new ExpenseResponse(2L, 100L, null, null, new BigDecimal("200.00"),
            LocalDate.now().minusDays(1), null, null, LocalDateTime.now());
        when(mapper.toResponse(e1)).thenReturn(r1);
        when(mapper.toResponse(e2)).thenReturn(r2);

        List<ExpenseResponse> result = service.list(100L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
    }
}
