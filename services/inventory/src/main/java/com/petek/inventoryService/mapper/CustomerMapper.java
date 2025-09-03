package com.petek.inventoryService.mapper;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.CustomerCreateRequest;
import com.petek.inventoryService.dto.CustomerResponse;
import com.petek.inventoryService.entity.Customer;

@Service
public class CustomerMapper {

    /**
     * Map CustomerCreateRequest to Customer entity.
     */
    public Customer toCustomer(CustomerCreateRequest request) {
        return Customer.builder()
            .customerName(request.getCustomerName())
            .customerSegment(request.getCustomerSegment())
            .email(request.getEmail())
            .phone(request.getPhone())
            .city(request.getCity())
            .build();
    }

    /**
     * Map Customer entity to CustomerResponse.
     */
    public CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.builder()
            .customerId(customer.getCustomerId())
            .customerName(customer.getCustomerName())
            .customerSegment(customer.getCustomerSegment())
            .email(customer.getEmail())
            .phone(customer.getPhone())
            .city(customer.getCity())
            .createdAt(customer.getCreatedAt())
            .build();
    }

}
