package com.petek.inventory_service.dto;

import java.util.List;

public record PageResponse<T>(
    List<T> content,
    PageInfo page
) {}
