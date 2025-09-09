package com.petek.inventoryService.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Sort;

public class SortUtils {
    public static Sort createSort(List<String> sortParams, Set<String> ALLOWED_SORT_FIELDS) {
        List<Sort.Order> orders = new ArrayList<>();
        
        for (String sortParam : sortParams) {
            Sort.Direction direction = Sort.Direction.ASC;
            String field = sortParam;
                
            if (sortParam.startsWith("-")) {
                direction = Sort.Direction.DESC;
                field = sortParam.substring(1);
            }
            
            if (!ALLOWED_SORT_FIELDS.contains(field)) {
                throw new IllegalArgumentException("Invalid sort field: " + field);
            }
            
            orders.add(new Sort.Order(direction, field));
        }
        
        return Sort.by(orders);
    }
}
