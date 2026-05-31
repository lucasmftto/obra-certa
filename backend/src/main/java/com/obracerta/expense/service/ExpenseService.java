package com.obracerta.expense.service;

import com.obracerta.category.domain.Category;
import com.obracerta.category.repository.CategoryRepository;
import com.obracerta.expense.domain.Expense;
import com.obracerta.expense.dto.ExpenseRequest;
import com.obracerta.expense.dto.ExpenseResponse;
import com.obracerta.expense.mapper.ExpenseMapper;
import com.obracerta.expense.repository.ExpenseRepository;
import com.obracerta.item.domain.Item;
import com.obracerta.item.repository.ItemRepository;
import com.obracerta.project.domain.ProjectStatus;
import com.obracerta.shared.exception.BusinessException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseMapper mapper;

    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(Long itemId) {
        findItem(itemId);
        return expenseRepository.findByItemIdOrderByPaidAtDesc(itemId)
            .stream()
            .map(mapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseResponse findById(Long id) {
        return mapper.toResponse(findExpense(id));
    }

    @Transactional
    public ExpenseResponse create(Long itemId, ExpenseRequest request) {
        Item item = findItem(itemId);
        validateProjectInProgress(item);
        validatePaidAtNotFuture(request.paidAt());

        Category category = resolveCategory(request.categoryId());

        Expense expense = Expense.builder()
            .item(item)
            .category(category)
            .amount(request.amount())
            .paidAt(request.paidAt())
            .description(request.description())
            .receiptUrl(request.receiptUrl())
            .build();

        return mapper.toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Expense expense = findExpense(id);
        validateProjectInProgress(expense.getItem());
        validatePaidAtNotFuture(request.paidAt());

        Category category = resolveCategory(request.categoryId());

        expense.setCategory(category);
        expense.setAmount(request.amount());
        expense.setPaidAt(request.paidAt());
        expense.setDescription(request.description());
        expense.setReceiptUrl(request.receiptUrl());

        return mapper.toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(Long id) {
        Expense expense = findExpense(id);
        expenseRepository.delete(expense);
    }

    // ---- helpers ----

    private Item findItem(Long itemId) {
        return itemRepository.findById(itemId)
            .orElseThrow(() -> new EntityNotFoundException("Item não encontrado com id: " + itemId));
    }

    private Expense findExpense(Long id) {
        return expenseRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Gasto não encontrado com id: " + id));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada com id: " + categoryId));
    }

    private void validateProjectInProgress(Item item) {
        ProjectStatus status = item.getEnvironment().getProject().getStatus();
        if (status != ProjectStatus.IN_PROGRESS) {
            throw new BusinessException(
                "Gastos só podem ser registrados em projetos com status IN_PROGRESS.",
                HttpStatus.CONFLICT
            );
        }
    }

    private void validatePaidAtNotFuture(LocalDate paidAt) {
        if (paidAt != null && paidAt.isAfter(LocalDate.now())) {
            throw new BusinessException(
                "A data do pagamento não pode ser no futuro.",
                HttpStatus.UNPROCESSABLE_ENTITY
            );
        }
    }
}
