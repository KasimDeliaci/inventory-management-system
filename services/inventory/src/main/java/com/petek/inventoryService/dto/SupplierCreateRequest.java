package com.petek.inventoryService.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SupplierCreateRequest(
    @NotNull(message = "Supplier name is required")
    @Size(min = 2, max = 200, message = "Supplier name must be between 2 and 200 characters")
    String supplierName,
    
    @NotNull(message = "Email is required")
    @Size(min = 2, max = 100, message = "Email must be between 2 and 100 characters")
    @Email(message = "Email must be valid")
    String email,
    
    @NotNull(message = "Phone is required")
    @Pattern(regexp = "^(\\+90|0)?[1-9][0-9]{9}$", message = "Phone number must be valid")
    String phone,
    
    @NotNull(message = "City is required")
    @Size(min = 2, max = 50, message = "City must be between 2 and 50 characters")
    String city 
) {}
