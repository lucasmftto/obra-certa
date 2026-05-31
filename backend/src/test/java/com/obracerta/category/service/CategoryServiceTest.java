package com.obracerta.category.service;

import com.obracerta.category.domain.Category;
import com.obracerta.category.dto.CategoryResponse;
import com.obracerta.category.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService — Unit Tests")
class CategoryServiceTest {

    @Mock private CategoryRepository repository;

    @InjectMocks
    private CategoryService service;

    @Test
    @DisplayName("listAll — should return all categories ordered by name")
    void listAll_shouldReturnAllCategories() {
        List<Category> categories = List.of(
            Category.builder().id(1L).name("Fees").color("#F44336").build(),
            Category.builder().id(2L).name("Labor").color("#4CAF50").build(),
            Category.builder().id(3L).name("Materials").color("#2196F3").build(),
            Category.builder().id(4L).name("Other").color("#607D8B").build(),
            Category.builder().id(5L).name("Tools").color("#9C27B0").build(),
            Category.builder().id(6L).name("Transport").color("#FF9800").build()
        );

        when(repository.findAllByOrderByNameAsc()).thenReturn(categories);

        List<CategoryResponse> result = service.listAll();

        assertThat(result).hasSize(6);
        assertThat(result.get(0).name()).isEqualTo("Fees");
        assertThat(result.get(2).name()).isEqualTo("Materials");
        assertThat(result.get(5).name()).isEqualTo("Transport");

        verify(repository).findAllByOrderByNameAsc();
    }
}
