package com.petek.inventory_service.dto;

public record PageInfo(
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
