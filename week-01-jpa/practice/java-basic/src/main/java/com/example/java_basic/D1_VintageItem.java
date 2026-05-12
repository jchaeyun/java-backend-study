package com.example.java_basic;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
public class D1_VintageItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int price;

    private List<Bid>

    public D1_VintageItem(String name, int price) {
        this.name = name;
        this.price = price;
    }

    public Long getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public void setPrice(int price) {
        this.price = price;
    }
}
