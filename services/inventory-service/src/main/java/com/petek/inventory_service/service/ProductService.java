package com.petek.inventory_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.petek.inventory_service.dto.ProductRequest;
import com.petek.inventory_service.dto.ProductResponse;
import com.petek.inventory_service.dto.ProductUpdateRequest;
import com.petek.inventory_service.entity.Product;
import com.petek.inventory_service.mapper.ProductMapper;
import com.petek.inventory_service.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository repository;
    private final ProductMapper mapper;
    
    public List<ProductResponse> getAllProducts() {
        return repository.findAll()
        .stream()
        .map(mapper::toProductResponse)
        .toList();
    }

    public Long createProduct(ProductRequest request) {
        Product product = mapper.toProduct(request);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return repository.save(product).getProductId();
    }

    public ProductResponse getProductById(Long productId) {
        return repository.findById(productId)
                .map(mapper::toProductResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with id: " + productId));    
    }
    
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

    public void deleteProduct(Long productId) {
        Product existingProduct = repository.findById(productId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        repository.delete(existingProduct);
    }

}
