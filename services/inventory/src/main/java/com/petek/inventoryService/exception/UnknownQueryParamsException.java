package com.petek.inventoryService.exception;

import java.util.Collection;
import java.util.List;

/**
 * Thrown when strict "400 on unknown query params" rule is violated.
 * Used to enforce the OpenAPI contract that unknown parameters return 400.
 */
public class UnknownQueryParamsException extends RuntimeException {
    private final List<String> invalidParams;

    public UnknownQueryParamsException(Collection<String> invalidParams) {
        super("Unknown/invalid query parameters: " + invalidParams);
        this.invalidParams = List.copyOf(invalidParams);
    }

    public List<String> getInvalidParams() { 
        return invalidParams; 
    }
}
