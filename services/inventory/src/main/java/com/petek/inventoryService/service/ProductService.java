package com.petek.inventoryService.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.petek.inventoryService.dto.PageResponse.PageInfo;
import com.petek.inventoryService.dto.PageResponse;
import com.petek.inventoryService.dto.ProductFilterRequest;
import com.petek.inventoryService.dto.ProductCreateRequest;
import com.petek.inventoryService.dto.ProductResponse;
import com.petek.inventoryService.dto.ProductUpdateRequest;
import com.petek.inventoryService.entity.Product;
import com.petek.inventoryService.mapper.ProductMapper;
import com.petek.inventoryService.repository.ProductRepository;
import com.petek.inventoryService.spec.ProductSpecifications;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository repository;
    private final ProductMapper mapper;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "productId", "productName", "category", "currentPrice", "updatedAt"
    );
    
    /**
     * Utils
     */
    private void validateSortRequest(ProductFilterRequest request) {
        // Validate price range
        if (request.priceGte() != null && request.priceLte() != null && 
            request.priceGte().compareTo(request.priceLte()) > 0) {
            throw new IllegalArgumentException("price_gte cannot be greater than price_lte");
        }
        
        // Validate safety stock range
        if (request.safetyGte() != null && request.safetyLte() != null && 
            request.safetyGte() > request.safetyLte()) {
            throw new IllegalArgumentException("safety_gte cannot be greater than safety_lte");
        }
        
        // Validate reorder point range
        if (request.reorderGte() != null && request.reorderLte() != null && 
            request.reorderGte() > request.reorderLte()) {
            throw new IllegalArgumentException("reorder_gte cannot be greater than reorder_lte");
        }
    }
    
    private Sort createSort(List<String> sortParams) {
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

    /**
     * Get all products.
     */
    public PageResponse<ProductResponse> getAllProducts(ProductFilterRequest request) {
        validateSortRequest(request);
        
        Pageable pageable = PageRequest.of(request.page(), request.size(), createSort(request.sort()));
        Specification<Product> spec = ProductSpecifications.withFilters(request);
        
        Page<Product> productPage = repository.findAll(spec, pageable);
        
        List<ProductResponse> productResponses = productPage.getContent()
            .stream()
            .map(mapper::toProductResponse)
            .toList();
        
        PageInfo pageInfo = new PageInfo(
            productPage.getNumber(),
            productPage.getSize(),
            productPage.getTotalElements(),
            productPage.getTotalPages()
        );
        
        return new PageResponse<ProductResponse>(productResponses, pageInfo);
    }

    /**
     * Create a new product.
     */
    public ProductResponse createProduct(ProductCreateRequest request) {
        Product product = mapper.toProduct(request);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return mapper.toProductResponse(repository.save(product));
    }

    /**
     * Get a product by ID.
     */
    public ProductResponse getProductById(Long productId) {
        return repository.findById(productId)
                .map(mapper::toProductResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with id: " + productId));    
    }

    /**
     * Update a product.
     */
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        Product existingProduct = repository.findById(productId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        
        Optional.ofNullable(request.productName())
            .filter(name -> !name.trim().isEmpty())
            .ifPresent(existingProduct::setProductName);
            
        Optional.ofNullable(request.description())
            .filter(desc -> !desc.trim().isEmpty())
            .ifPresent(existingProduct::setDescription);
            
        Optional.ofNullable(request.category())
            .filter(cat -> !cat.trim().isEmpty())
            .ifPresent(existingProduct::setCategory);
            
        Optional.ofNullable(request.unitOfMeasure())
            .filter(unit -> !unit.trim().isEmpty())
            .ifPresent(existingProduct::setUnitOfMeasure);
            
        Optional.ofNullable(request.safetyStock())
            .ifPresent(existingProduct::setSafetyStock);
            
        Optional.ofNullable(request.reorderPoint())
            .ifPresent(existingProduct::setReorderPoint);
            
        Optional.ofNullable(request.currentPrice())
            .ifPresent(existingProduct::setCurrentPrice);
        
        existingProduct.setUpdatedAt(LocalDateTime.now());

        return mapper.toProductResponse(repository.save(existingProduct));
    }

    /**
     * Delete a product.
     */
    public void deleteProduct(Long productId) {
        Product existingProduct = repository.findById(productId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        repository.delete(existingProduct);
    }

}
