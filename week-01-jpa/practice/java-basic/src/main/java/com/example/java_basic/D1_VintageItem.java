package com.example.java_basic;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor
public class D1_VintageItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String name;

    @Setter
    private int price;
    /*
    1일차 요약 문서에서 "엔티티에 무분별한 @Setter는 금지"라고 했던 것 기억하시나요?
    공부 단계에서는 편의상 쓰지만, 나중에는 changeBidPrice(int price) 처럼
    비즈니스 의미가 담긴 메서드를 직접 만드는 습관을 들이는 게 좋습니다.
     */

    private List<Bid>

    public D1_VintageItem(String name, int price) {
        this.name = name;
        this.price = price;
    }

}
