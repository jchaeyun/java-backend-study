package com.example.java_basic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class StockServiceTest {
    @Autowired
    private D4ProductService stockService;
    @Autowired private D4ProductRepository productRepository;
    private Long productId;

    @BeforeEach
    void setUp() {
        D4_Product saved = productRepository.save(new D4_Product(100));
        this.productId = saved.getId();
    }

    @Test
    void 동시에_100개_차감_테스트() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try { stockService.decreaseStock(productId, 1); }
                finally { latch.countDown(); }
            });
        }
        latch.await();

        D4_Product product = productRepository.findById(productId).orElseThrow();
        assertEquals(0, product.getStock());
        System.out.println("최종 재고: " + product.getStock());
    }
}
