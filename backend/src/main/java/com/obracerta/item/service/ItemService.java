package com.obracerta.item.service;

import com.obracerta.environment.domain.Environment;
import com.obracerta.environment.repository.EnvironmentRepository;
import com.obracerta.expense.repository.ExpenseRepository;
import com.obracerta.item.domain.Item;
import com.obracerta.item.dto.ItemFlagsRequest;
import com.obracerta.item.dto.ItemRequest;
import com.obracerta.item.dto.ItemResponse;
import com.obracerta.item.mapper.ItemMapper;
import com.obracerta.item.repository.ItemRepository;
import com.obracerta.project.domain.ProjectStatus;
import com.obracerta.shared.exception.BusinessException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final EnvironmentRepository environmentRepository;
    private final ExpenseRepository expenseRepository;
    private final ItemMapper mapper;

    @Transactional(readOnly = true)
    public List<ItemResponse> list(Long environmentId) {
        findEnvironment(environmentId);
        return itemRepository.findByEnvironmentIdOrderByCreatedAtAsc(environmentId)
            .stream()
            .map(this::toResponseWithCalculations)
            .toList();
    }

    @Transactional(readOnly = true)
    public ItemResponse findById(Long id) {
        return toResponseWithCalculations(findItem(id));
    }

    @Transactional
    public ItemResponse create(Long environmentId, ItemRequest request) {
        Environment environment = findEnvironment(environmentId);
        validateProjectEditable(environment);

        Item item = Item.builder()
            .environment(environment)
            .description(request.description())
            .quantity(request.quantity())
            .unit(request.unit())
            .unitPrice(request.unitPrice())
            .type(request.type())
            .build();

        item = itemRepository.save(item);
        recalculateEnvironmentCompletion(environment);
        return toResponseWithCalculations(item);
    }

    @Transactional
    public ItemResponse update(Long id, ItemRequest request) {
        Item item = findItem(id);
        validateProjectEditable(item.getEnvironment());

        item.setDescription(request.description());
        item.setQuantity(request.quantity());
        item.setUnit(request.unit());
        item.setUnitPrice(request.unitPrice());
        item.setType(request.type());

        item = itemRepository.save(item);
        return toResponseWithCalculations(item);
    }

    @Transactional
    public ItemResponse updateFlags(Long id, ItemFlagsRequest request) {
        Item item = findItem(id);

        if (request.completed() != null) item.setCompleted(request.completed());
        if (request.delayed() != null) item.setDelayed(request.delayed());

        item = itemRepository.save(item);
        recalculateEnvironmentCompletion(item.getEnvironment());
        return toResponseWithCalculations(item);
    }

    @Transactional
    public void delete(Long id) {
        Item item = findItem(id);
        if (expenseRepository.existsByItemId(id)) {
            throw new BusinessException(
                "Não é possível excluir um item que possui gastos vinculados.",
                HttpStatus.CONFLICT
            );
        }
        Environment environment = item.getEnvironment();
        itemRepository.delete(item);
        recalculateEnvironmentCompletion(environment);
    }

    // ---- helpers ----

    private void recalculateEnvironmentCompletion(Environment environment) {
        List<Item> items = itemRepository.findByEnvironmentIdOrderByCreatedAtAsc(environment.getId());
        BigDecimal percentage;
        if (items.isEmpty()) {
            percentage = BigDecimal.ZERO;
        } else {
            long completedCount = items.stream().filter(Item::isCompleted).count();
            percentage = BigDecimal.valueOf(completedCount * 100L)
                .divide(BigDecimal.valueOf(items.size()), 2, RoundingMode.HALF_UP);
        }
        environment.setCompletionPercentage(percentage);
        environmentRepository.save(environment);
    }

    private Environment findEnvironment(Long environmentId) {
        return environmentRepository.findById(environmentId)
            .orElseThrow(() -> new EntityNotFoundException("Ambiente não encontrado com id: " + environmentId));
    }

    private Item findItem(Long id) {
        return itemRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Item não encontrado com id: " + id));
    }

    private void validateProjectEditable(Environment environment) {
        ProjectStatus status = environment.getProject().getStatus();
        if (status == ProjectStatus.COMPLETED) {
            throw new BusinessException(
                "Projeto concluído não permite alterações.",
                HttpStatus.CONFLICT
            );
        }
    }

    private ItemResponse toResponseWithCalculations(Item item) {
        BigDecimal totalBudgeted = item.getQuantity().multiply(item.getUnitPrice());
        BigDecimal totalActual = itemRepository.sumActualByItem(item.getId());
        return mapper.toResponseWithCalculations(item, totalBudgeted, totalActual);
    }
}
