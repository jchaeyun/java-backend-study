package com.example.java_basic;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) //기본생성자 만듦(JPA가 내부적으로 객체 생성 시 꼭 필요)
public class D2_Bid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int bidPrice;

    //여기가 핵심! 반드시 LAZY로 설정해야 프록시가 작동합니다.
    //입찰(Bid)은 하나의 상품(VintageItem)에 속하므로 ManyToOne 관계
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="item_id") //DB 컬럼 설정: DB에는 item_id라는 FK 컬럼이 생김
    private D1_VintageItem item;

    //필드를 받아서 객체를 생성
    public D2_Bid(D1_VintageItem item, int bidPrice){
        this.item = item;
        this.bidPrice = bidPrice;
    }
}
