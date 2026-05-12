package com.example.java_basic;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy="item")
    private List<D2_Bid> bids=new ArrayList<>();

    public D1_VintageItem(String name, int price) {
        this.name = name;
        this.price = price;
    }

}
