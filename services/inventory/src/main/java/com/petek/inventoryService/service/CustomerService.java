package com.petek.inventoryService.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petek.inventoryService.dto.PageResponse;
import com.petek.inventoryService.dto.PageResponse.PageInfo;
import com.petek.inventoryService.dto.customer.CustomerCreateRequest;
import com.petek.inventoryService.dto.customer.CustomerFilterRequest;
import com.petek.inventoryService.dto.customer.CustomerResponse;
import com.petek.inventoryService.dto.customer.CustomerUpdateRequest;
import com.petek.inventoryService.entity.Customer;
import com.petek.inventoryService.mapper.CustomerMapper;
import com.petek.inventoryService.repository.CustomerRepository;
import com.petek.inventoryService.spec.CustomerSpecifications;
import com.petek.inventoryService.utils.SortUtils;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {
    
    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "customerId", "customerName", "customerSegment", "email", "city", "updatedAt"
    );

    /**
     * Get all customers.
     */
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getCustomers(CustomerFilterRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), SortUtils.createSort(request.getSort(), ALLOWED_SORT_FIELDS));
        Specification<Customer> spec = CustomerSpecifications.withFilters(request);

        Page<Customer> customerPage = repository.findAll(spec, pageable);

        List<CustomerResponse> customerResponses = customerPage.getContent()
            .stream()
            .map(mapper::toCustomerResponse)
            .toList();
        
        PageInfo pageInfo = new PageInfo(
            customerPage.getNumber(),
            customerPage.getSize(),
            customerPage.getTotalElements(),
            customerPage.getTotalPages()
        );

        return new PageResponse<CustomerResponse>(customerResponses, pageInfo);
    }

    /**
     * Create a new customer.
     */
    public CustomerResponse createCustomer(CustomerCreateRequest request) {
        Customer customer = mapper.toCustomer(request);
        customer.setCreatedAt(Instant.now());
        customer.setUpdatedAt(Instant.now());
        return mapper.toCustomerResponse(repository.save(customer));
    }

    /**
     * Get a customer by ID.
     */
    public CustomerResponse getCustomerById(Long customerId) {
        return repository.findById(customerId)
                .map(mapper::toCustomerResponse)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + customerId));
    }

    /**
     * Update a customer.
     */
    public CustomerResponse updateCustomer(Long customerId, CustomerUpdateRequest request) {
        Customer existingCustomer = repository.findById(customerId)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + customerId));

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

        return mapper.toCustomerResponse(repository.save(existingCustomer));
    }

    /**
     * Delete a customer.
     */
    public void deleteCustomer(Long customerId) {
        Customer existingCustomer = repository.findById(customerId)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + customerId));
        repository.delete(existingCustomer);
    }

}
