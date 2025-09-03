package com.petek.inventoryService.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierUpdateRequest {
    @Size(min = 2, max = 200, message = "Supplier name must be between 2 and 200 characters")
    private String supplierName;

    @Size(min = 2, max = 100, message = "Email must be between 2 and 100 characters")
    @Email(message = "Email must be valid")
    private String email;

    @Pattern(regexp = "^(\\+90|0)?[1-9][0-9]{9}$", message = "Phone number must be valid")
    private String phone;

    @Size(min = 2, max = 50, message = "City must be between 2 and 50 characters")
    private String city;
}
