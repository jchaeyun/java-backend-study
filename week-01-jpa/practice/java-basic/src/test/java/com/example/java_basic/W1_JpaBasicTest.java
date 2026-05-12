package com.example.java_basic;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


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

    //N+1 문제 발생
    @Test
    void findNPlusOne() {
        //아이템 하나에 입찰 두개. 1:N
        D1_VintageItem item1 = new D1_VintageItem("Polo Knit", 50000);
        em.persist(item1);

        //아이템 하나에 입찰 두개. 1:N
        D1_VintageItem item2 = new D1_VintageItem("Polo Knit", 50000);
        em.persist(item2);

        D2_Bid bid = new D2_Bid(item1, 55000);
        em.persist(bid);

        D2_Bid bid2 = new D2_Bid(item1, 90000);
        em.persist(bid2);

        D2_Bid bid3 = new D2_Bid(item1, 70000);
        em.persist(bid3);

        // 2. [중요] 영속성 컨텍스트 완전히 비우기. 1차캐시만 참조하면 쿼리 안나감
        em.flush();
        em.clear();
        System.out.println("=== 영속성 컨텍스트 비우기 완료 (이제부터 진짜 DB 쿼리 나감) ===");

        // 1. 모든 상품 조회 (쿼리 1번)
        // 결과: 상품이 10개라면, 10개의 VintageItem 객체가 생성됨 (bids는 프록시 상태)
        List<D1_VintageItem> items = em.createQuery("select v from D1_VintageItem v", D1_VintageItem.class)
                .getResultList();

        // 2. 각 상품의 입찰 내역을 루프 돌며 접근(반복문 돌면서 프록시 초기화 유도)
        for (D1_VintageItem item : items) {

            // item.getBids()를 호출하고 그 내부 데이터(size 등)에 접근하는 순간 쿼리 발생!
            // 상품이 10개면 여기서 쿼리가 10번 더 나감 (N번),상품이 1개면 1번+입찰내역 가져오기 1번=2번 select
            System.out.println("상품명: " + item.getName() + ", 입찰 수: " + item.getBids().size());
        }
    }
        @Test
        void joinTest() {

        //데이터 넣기. 아이텝 2개 각각 입찰 2개,1개
            D1_VintageItem item1 = new D1_VintageItem("Polo Knit", 50000);
            em.persist(item1);

            //아이템 하나에 입찰 두개. 1:N
            D1_VintageItem item2 = new D1_VintageItem("Polo Knit", 50000);
            em.persist(item2);

            D2_Bid bid = new D2_Bid(item1, 55000);
            em.persist(bid);

            D2_Bid bid2 = new D2_Bid(item1, 90000);
            em.persist(bid2);

            D2_Bid bid3 = new D2_Bid(item2, 70000);
            em.persist(bid3);

            // 2. [중요] 영속성 컨텍스트 완전히 비우기. 1차캐시만 참조하면 쿼리 안나감
            em.flush();
            em.clear();
            System.out.println("=== 영속성 컨텍스트 비우기 완료 (이제부터 진짜 DB 쿼리 나감) ===");

            // JPQL에 join을 썼지만, SELECT 절에는 v(VintageItem)만 있습니다.
            List<D1_VintageItem> items = em.createQuery("select v from D1_VintageItem v join v.bids", D1_VintageItem.class)
                    .getResultList();

            for (D1_VintageItem item : items) {
                // DB에서는 JOIN을 했지만, JPA는 Bid 데이터를 긁어오지 않았습니다.
                // 여전히 bids는 프록시이며, 호출할 때마다 쿼리가 또 나갑니다. (N+1 그대로 발생)
                System.out.println(item.getBids().size());
            }
        }

    @Test
    void fetchJoinTest() {
        // join fetch를 쓰면 SELECT 절에 v와 연관된 bids의 컬럼이 모두 포함됩니다.
        List<D1_VintageItem> items = em.createQuery("select v from D1_VintageItem v join fetch v.bids", D1_VintageItem.class)
                .getResultList();
        //데이터 넣기. 아이텝 2개 각각 입찰 2개,1개
        D1_VintageItem item1 = new D1_VintageItem("Polo Knit", 50000);
        em.persist(item1);

        //아이템 하나에 입찰 두개. 1:N
        D1_VintageItem item2 = new D1_VintageItem("Polo Knit", 50000);
        em.persist(item2);

        D2_Bid bid = new D2_Bid(item1, 55000);
        em.persist(bid);

        D2_Bid bid2 = new D2_Bid(item1, 90000);
        em.persist(bid2);

        D2_Bid bid3 = new D2_Bid(item2, 70000);
        em.persist(bid3);

        // 2. [중요] 영속성 컨텍스트 완전히 비우기. 1차캐시만 참조하면 쿼리 안나감
        em.flush();
        em.clear();
        System.out.println("=== 영속성 컨텍스트 비우기 완료 (이제부터 진짜 DB 쿼리 나감) ===");


        for (D1_VintageItem item : items) {
            // 이미 1차 캐시에 '진짜 Bid 객체'들이 다 들어있습니다.
            // 쿼리가 추가로 전혀 나가지 않습니다. (쿼리 1번으로 끝)
            System.out.println(item.getBids().size());
        }
    }

    // 1. application.yml 에 글로벌 설정 추가
// spring.jpa.properties.hibernate.default_batch_fetch_size: 100

    @Test
    void pagingSolution() {
        //데이터 넣기. 아이텝 2개 각각 입찰 2개,1개
        D1_VintageItem item1 = new D1_VintageItem("Polo Knit", 50000);
        em.persist(item1);

        //아이템 하나에 입찰 두개. 1:N
        D1_VintageItem item2 = new D1_VintageItem("Polo Knit", 50000);
        em.persist(item2);

        D2_Bid bid = new D2_Bid(item1, 55000);
        em.persist(bid);

        D2_Bid bid2 = new D2_Bid(item1, 90000);
        em.persist(bid2);

        D2_Bid bid3 = new D2_Bid(item2, 70000);
        em.persist(bid3);

        // 2. [중요] 영속성 컨텍스트 완전히 비우기. 1차캐시만 참조하면 쿼리 안나감
        em.flush();
        em.clear();
        System.out.println("=== 영속성 컨텍스트 비우기 완료 (이제부터 진짜 DB 쿼리 나감) ===");

        // 페치 조인을 빼고, 그냥 상품만 페이징해서 조회합니다.
        List<D1_VintageItem> items = em.createQuery("select v from D1_VintageItem v", D1_VintageItem.class)
                .setFirstResult(0)
                .setMaxResults(10)
                .getResultList();

        // 루프를 돌 때, BatchSize가 설정되어 있으면
        // 10개 상품에 대한 입찰 내역을 개별 쿼리가 아닌 'IN 쿼리' 1번으로 묶어서 가져옵니다.
        for (D1_VintageItem item : items) {
            System.out.println(item.getBids().size());
        }
    }
    }

