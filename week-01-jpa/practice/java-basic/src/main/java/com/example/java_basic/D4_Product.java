package com.example.java_basic;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class D4_Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int stock;

    public D4_Product(int stock) { this.stock = stock; }

    public void removeStock(int quantity) {
        if (this.stock < quantity) throw new RuntimeException("재고 부족");
        this.stock -= quantity;
    }
}
