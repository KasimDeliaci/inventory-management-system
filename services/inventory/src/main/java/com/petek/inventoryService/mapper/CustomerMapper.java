package com.petek.inventoryService.mapper;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.CustomerCreateRequest;
import com.petek.inventoryService.dto.CustomerResponse;
import com.petek.inventoryService.entity.Customer;

@Service
public class CustomerMapper {

    public Customer toCustomer(CustomerCreateRequest request) {
        return Customer.builder()
            .customerName(request.customerName())
            .customerSegment(request.customerSegment())
            .email(request.email())
            .phone(request.phone())
            .city(request.city())
            .build();
    }

    public CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
            customer.getCustomerId(),
            customer.getCustomerName(),
            customer.getCustomerSegment(),
            customer.getEmail(),
            customer.getPhone(),
            customer.getCity(),
            customer.getCreatedAt()
        );
    }
}
