package com.example.java_basic;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;


@SpringBootTest
@Transactional
public class D1_PersistenceBasicTest {
    @Autowired
    EntityManager em;

    //1차 캐시 실습
    @Test
    public void first_level_cache() {
        //준비:DB에 데이터 1건 강제 삽입
        D1_VintageItem vintageItem = new D1_VintageItem("Levi's 501",100000);
        em.persist(vintageItem); //1차 캐시에 넣음,스냅샷 저장,ActionQueue에 쿼리 넣음
        em.flush(); //queue에 있던 쿼리를 db로 전송(임시 처리, 쿼리는 나가지만 메모리단에서 처리,아직 디스크에 저장 안됨)
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
        em.persist(item); //IDENTITY라 바로 쿼리 넣고 id 받아와서 1차 캐시에 저장
        em.flush(); // DB에 쿼리 전송 (스냅샷 생성됨)

        System.out.println("=== 가격 수정 시작 ===");

        // 엔티티의 필드 값만 변경함. em.update()나 em.save() 호출 안 함.
        item.setPrice(55000);

        System.out.println("=== 가격 수정 완료 ===");

        //작업 공간의 변경 사항을 DB에 동기화(UPDATE 쿼리 전송)
        em.flush();

        System.out.println("=== flush 후 UPDATE 쿼리 확인 ===");
    }
}
