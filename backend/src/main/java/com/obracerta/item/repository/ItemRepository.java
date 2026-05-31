package com.obracerta.item.repository;

import com.obracerta.item.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByEnvironmentIdOrderByCreatedAtAsc(Long environmentId);

    @Query(value = "SELECT COALESCE(SUM(amount),0) FROM expense WHERE item_id = :itemId",
           nativeQuery = true)
    BigDecimal sumActualByItem(@Param("itemId") Long itemId);

    @Query(value = "SELECT CASE WHEN COUNT(id) > 0 THEN true ELSE false END FROM item WHERE environment_id = :envId AND delayed = true",
           nativeQuery = true)
    boolean hasDelayedItems(@Param("envId") Long envId);
}
