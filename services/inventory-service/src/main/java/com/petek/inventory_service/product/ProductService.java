package com.petek.inventory_service.product;

import java.time.LocalDateTime;
import java.util.List;

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

}
