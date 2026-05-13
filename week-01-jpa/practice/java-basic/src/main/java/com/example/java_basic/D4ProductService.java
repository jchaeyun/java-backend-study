package com.example.java_basic;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class D4ProductService {
    private final D4ProductRepository productRepository;

    @Transactional
    public void decreaseStock(Long id, int quantity) {
        D4_Product product = productRepository.findByIdWithLock(id)
                .orElseThrow(() -> new RuntimeException("상품 없음"));
        product.removeStock(quantity);
    }
}
