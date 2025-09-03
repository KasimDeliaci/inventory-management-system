package com.petek.inventoryService.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.petek.inventoryService.dto.CustomerCreateRequest;
import com.petek.inventoryService.dto.CustomerResponse;
import com.petek.inventoryService.dto.CustomerUpdateRequest;
import com.petek.inventoryService.entity.Customer;
import com.petek.inventoryService.mapper.CustomerMapper;
import com.petek.inventoryService.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {
    
    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    /**
     * Create a new customer.
     */
    public CustomerResponse createCustomer(CustomerCreateRequest request) {
        Customer customer = mapper.toCustomer(request);
        customer.setCreatedAt(Instant.now());
        return mapper.toResponse(repository.save(customer));
    }

    /**
     * Get a customer by ID.
     */
    public CustomerResponse getCustomerById(Long customerId) {
        return repository.findById(customerId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found with id: " + customerId));
    }

    /**
     * Update a customer.
     */
    public CustomerResponse updateCustomer(Long customerId, CustomerUpdateRequest request) {
        Customer existingCustomer = repository.findById(customerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

        Optional.ofNullable(request.getCustomerName())
            .filter(name -> !name.trim().isEmpty())
            .ifPresent(existingCustomer::setCustomerName);

        Optional.ofNullable(request.getCustomerSegment())
            .ifPresent(existingCustomer::setCustomerSegment);

        Optional.ofNullable(request.getEmail())
            .filter(email -> !email.trim().isEmpty())
            .ifPresent(existingCustomer::setEmail);

        Optional.ofNullable(request.getPhone())
            .filter(phone -> !phone.trim().isEmpty())
            .ifPresent(existingCustomer::setPhone);

        Optional.ofNullable(request.getCity())
            .filter(city -> !city.trim().isEmpty())
            .ifPresent(existingCustomer::setCity);

        return mapper.toResponse(repository.save(existingCustomer));
    }

    /**
     * Delete a customer.
     */
    public void deleteCustomer(Long customerId) {
        Customer existingCustomer = repository.findById(customerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        repository.delete(existingCustomer);
    }

}
