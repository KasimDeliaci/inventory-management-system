package com.petek.inventoryService.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductFilterRequest(
    Integer page,
    Integer size,
    List<String> sort,
    String q,
    List<String> category,
    List<String> uom,
    BigDecimal priceGte,
    BigDecimal priceLte,
    Integer safetyGte,
    Integer safetyLte,
    Integer reorderGte,
    Integer reorderLte,
    LocalDateTime updatedAfter
) {}
