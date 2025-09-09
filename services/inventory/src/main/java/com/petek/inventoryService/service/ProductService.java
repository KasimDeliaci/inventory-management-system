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

import com.petek.inventoryService.dto.PageResponse.PageInfo;
import com.petek.inventoryService.dto.product.ProductCreateRequest;
import com.petek.inventoryService.dto.product.ProductFilterRequest;
import com.petek.inventoryService.dto.product.ProductItemResponse;
import com.petek.inventoryService.dto.product.ProductResponse;
import com.petek.inventoryService.dto.product.ProductUpdateRequest;
import com.petek.inventoryService.dto.stock.CurrentStockResponse;
import com.petek.inventoryService.dto.PageResponse;
import com.petek.inventoryService.entity.Product;
import com.petek.inventoryService.mapper.ProductMapper;
import com.petek.inventoryService.repository.ProductRepository;
import com.petek.inventoryService.spec.ProductSpecifications;
import com.petek.inventoryService.utils.SortUtils;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    
    private final ProductRepository repository;
    private final ProductMapper mapper;

    private final CurrentStockService currentStockService;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "productId", "productName", "category", "currentPrice", "updatedAt"
    );  
    
    /**
     * Get all products.
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductItemResponse> getAllProducts(ProductFilterRequest request) {
        // Validate price range
        if (request.getPriceGte() != null && request.getPriceLte() != null && 
            request.getPriceGte().compareTo(request.getPriceLte()) > 0) {
            throw new IllegalArgumentException("price_gte cannot be greater than price_lte");
        }
        
        // Validate safety stock range
        if (request.getSafetyGte() != null && request.getSafetyLte() != null && 
            request.getSafetyGte().compareTo(request.getSafetyLte()) > 0) {
            throw new IllegalArgumentException("safety_gte cannot be greater than safety_lte");
        }
        
        // Validate reorder point range
        if (request.getReorderGte() != null && request.getReorderLte() != null && 
            request.getReorderGte().compareTo(request.getReorderLte()) > 0) {
            throw new IllegalArgumentException("reorder_gte cannot be greater than reorder_lte");
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), SortUtils.createSort(request.getSort(), ALLOWED_SORT_FIELDS));
        Specification<Product> spec = ProductSpecifications.withFilters(request);
        
        Page<Product> productPage = repository.findAll(spec, pageable);
        
        List<ProductItemResponse> productResponses = productPage.getContent()
            .stream()
            .map(product -> {
                CurrentStockResponse currentStockResponse = currentStockService.getCurrentStockById(product.getProductId());
                return mapper.toProductItemResponse(product, currentStockResponse);
            })
            .toList();
        
        PageInfo pageInfo = new PageInfo(
            productPage.getNumber(),
            productPage.getSize(),
            productPage.getTotalElements(),
            productPage.getTotalPages()
        );
        
        return new PageResponse<ProductItemResponse>(productResponses, pageInfo);
    }

    /**
     * Create a new product.
     */
    public ProductResponse createProduct(ProductCreateRequest request) {
        Product product = mapper.toProduct(request);
        product.setCreatedAt(Instant.now());
        product.setUpdatedAt(Instant.now());
        return mapper.toProductResponse(repository.save(product));
    }

    /**
     * Get a product by ID.
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long productId) {
        return repository.findById(productId)
            .map(mapper::toProductResponse)
            .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));    
    }

    /**
     * Update a product.
     */
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        Product existingProduct = repository.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));

        Optional.ofNullable(request.getProductName())
            .filter(name -> !name.trim().isEmpty())
            .ifPresent(existingProduct::setProductName);
            
        Optional.ofNullable(request.getDescription())
            .filter(desc -> !desc.trim().isEmpty())
            .ifPresent(existingProduct::setDescription);
            
        Optional.ofNullable(request.getCategory())
            .filter(cat -> !cat.trim().isEmpty())
            .ifPresent(existingProduct::setCategory);
            
        Optional.ofNullable(request.getUnitOfMeasure())
            .filter(unit -> !unit.trim().isEmpty())
            .ifPresent(existingProduct::setUnitOfMeasure);
            
        Optional.ofNullable(request.getSafetyStock())
            .ifPresent(existingProduct::setSafetyStock);
            
        Optional.ofNullable(request.getReorderPoint())
            .ifPresent(existingProduct::setReorderPoint);
            
        Optional.ofNullable(request.getCurrentPrice())
            .ifPresent(existingProduct::setCurrentPrice);
        
        existingProduct.setUpdatedAt(Instant.now());

        return mapper.toProductResponse(repository.save(existingProduct));
    }

    /**
     * Delete a product.
     */
    public void deleteProduct(Long productId) {
        Product existingProduct = repository.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));
        repository.delete(existingProduct);
    }

}
