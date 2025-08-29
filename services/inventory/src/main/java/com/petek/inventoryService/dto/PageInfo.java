package com.petek.inventoryService.dto;

public record PageInfo(
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
