package com.petek.inventoryService.service;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.petek.inventoryService.dto.CustomerCreateRequest;
import com.petek.inventoryService.dto.CustomerResponse;
import com.petek.inventoryService.entity.Customer;
import com.petek.inventoryService.mapper.CustomerMapper;
import com.petek.inventoryService.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {
    
    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    public CustomerResponse createCustomer(CustomerCreateRequest request) {
        Customer customer = mapper.toCustomer(request);
        customer.setCreatedAt(Instant.now());
        return mapper.toResponse(repository.save(customer));
    }

    public CustomerResponse getCustomerById(Long customerId) {
        return repository.findById(customerId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found with id: " + customerId));
    }

}
