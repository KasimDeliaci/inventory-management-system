package com.petek.inventory_service.product;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository repository;
    private final ProductMapper mapper;

    public Long createProduct(ProductRequest request) {
        Product product = mapper.toProduct(request);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return repository.save(product).getProductId();
    }

    public Product getProductById(Long productId) {
        return repository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
    }

    public List<Product> getAllProducts() {
        return repository.findAll();
    }

    public Product updateProduct(Long productId, ProductRequest request) {
        Product existingProduct = repository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
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
        
        return repository.save(existingProduct);
    }

}
