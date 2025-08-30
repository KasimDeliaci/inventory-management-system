package com.petek.inventoryService.dto;

import java.util.List;

public record PageResponse<T>(
    List<T> content,
    PageInfo page
) {}
