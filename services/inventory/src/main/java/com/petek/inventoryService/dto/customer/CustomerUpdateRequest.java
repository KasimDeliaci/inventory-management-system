package com.petek.inventoryService.dto.customer;

import com.petek.inventoryService.entity.Customer.CustomerSegment;

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
public class CustomerUpdateRequest {
    @Size(min = 2, max = 100, message = "Customer name must be between 2 and 100 characters")
    private String customerName;

    private CustomerSegment customerSegment;

    @Email(message = "Email must be valid")
    private String email;

    @Pattern(regexp = "^(\\+90|0)?[1-9][0-9]{9}$", message = "Phone number must be valid")
    private String phone;

    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    private String city;
}
