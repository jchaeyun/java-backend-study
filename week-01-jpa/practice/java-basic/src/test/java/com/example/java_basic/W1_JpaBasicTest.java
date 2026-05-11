package com.example.java_basic;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;


@SpringBootTest
@Transactional
public class W1_JpaBasicTest {
    @Autowired
    EntityManager em;

    //1차 캐시 실습
    @Test
    public void first_level_cache() {
        //준비:DB에 데이터 1건 강제 삽입
        D1_VintageItem vintageItem = new D1_VintageItem("Levi's 501",100000);
        em.persist(vintageItem); //1차 캐시에 넣음,스냅샷 저장,ActionQueue에 쿼리 넣음
        em.flush(); //queue에 있던 쿼리를 db로 전송(임시 처리, 아직 저장 안됨)
        em.clear(); //영속성 컨텍스트 비움(초기 상태 세팅)
        
        System.out.println("===첫 번째 조회 시작===");
        D1_VintageItem find1=em.find(D1_VintageItem.class, vintageItem.getId());

        System.out.println("===두 번째 조회 시작===");//SELECT 로그 안 찍힘. 1차 캐시 조회함
        D1_VintageItem find2=em.find(D1_VintageItem.class, vintageItem.getId());
        
        System.out.println("find1과 find2는 같은 객체인가?"+(find1==find2));
        
    }

    //쓰기 지연 실습
    @Test
    public void writeBehindTest(){
        D1_VintageItem d1_VintageItem = new D1_VintageItem("knit",8000);
        D1_VintageItem d1_VintageItem2 = new D1_VintageItem("Trench",20000);

        System.out.println("===persist 호출 전===");

        em.persist(d1_VintageItem);
        em.persist(d1_VintageItem);

        System.out.println("===persist 호출 후(쿼리 없음)===");

        //트랜잭션 끝나기 전 수동으로 모아둔 쿼리를 DB로 전송 명력
        em.flush();

        System.out.println("===flush 호출 후(쿼리 2개 발생)===");


    }

    //Dirty Checking 실습
    @Test
    public void dirtyCheckingTest(){
        // 준비: 초기 데이터 삽입
        D1_VintageItem item = new D1_VintageItem("Vintage Denim", 50000);
        em.persist(item);
        em.flush(); // DB에 쿼리 전송 (스냅샷 생성됨)

        System.out.println("=== 가격 수정 시작 ===");

        // 엔티티의 필드 값만 변경함. em.update()나 em.save() 호출 안 함.
        item.setPrice(55000);

        System.out.println("=== 가격 수정 완료 ===");

        //작업 공간의 변경 사항을 DB에 동기화(UPDATE 쿼리 전송)
        em.flush();

        System.out.println("=== flush 후 UPDATE 쿼리 확인 ===");
    }

    @Test
    //프록시 동작 확인 실습
    public void proxyActionTest(){
        // 1. 데이터 준비 (상품 하나와 그에 걸린 입찰 하나)
        D1_VintageItem item = new D1_VintageItem("Polo Knit", 50000);
        em.persist(item);

        D2_Bid bid = new D2_Bid(item, 55000);
        em.persist(bid);

        em.flush(); //db에 보내기만 한거고 db에 영구저장은 아님
        em.clear(); // 영속성 컨텍스트를 비워서 1차 캐시를 제거합니다.

        System.out.println("=== 입찰(Bid)만 조회 시작 ===");
        D2_Bid findBid = em.find(D2_Bid.class, bid.getId());
        //클래스 (D2_Bid.class): "어떤 테이블을 뒤져야 해?" (Mapping 정보)
        //식별자 (bid.getId()): "그 테이블에서 몇 번 행(Row)을 가져와야 해?" (PK 정보)
        //->find는 항상 클래스,아이디로 데이터를 조회함.findByName 이런거는 pk가 아니라 이름으로 찾음
        System.out.println("=== 입찰 조회 종료 ===");

        // 2. 가짜 객체(Proxy) 확인(SELECT 쿼리 나가긴함)
        System.out.println("찾은 입찰의 상품 클래스: " + findBid.getItem().getClass().getName());
        // 출력 결과에 'HibernateProxy'라는 글자가 포함되어야 성공!

        System.out.println("=== 상품(Item) 실제 데이터 접근 시작 ===");
        // 지연 로딩! 실제 이름을 꺼내는 순간, 입찰 정보는 아까 가져왔음에도 SELECT 쿼리가 나갑니다.(아까는 껍데기뿐.proxy가 알아서 보냄)
        // 즉시 로딩이었다면 첫 번째 select에서 join을 써서 상품정보까지 가져왔을 것.
        String itemName = findBid.getItem().getName();
        System.out.println("상품명: " + itemName);
        System.out.println("=== 상품 데이터 접근 종료 ===");
    }
}
