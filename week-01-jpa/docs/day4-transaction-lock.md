

## 1. 트랜잭션의 ACID와 계층별 구현 (Where & How)

단순한 이론이 아니라, 우리가 짠 코드의 어느 부분에서 각 원칙이 지켜지는지가 핵심입니다.

| 원칙 | 의미 | 구현 위치 | 구현 방법 |
| --- | --- | --- | --- |
| **A (원자성)** | All or Nothing | **Service** | `@Transactional` (성공 시 커밋, 실패 시 롤백) |
| **C (일관성)** | 데이터 규칙 준수 | **Entity** | 도메인 로직 (`removeStock` 내부의 재고 검증) |
| **I (격리성)** | 간섭 방지 | **Repository** | `@Lock(PESSIMISTIC_WRITE)` (줄 세우기) |
| **D (지속성)** | 영구 저장 | **DB 엔진** | MySQL의 Redo Log 및 스토리지 엔진 기능 |

---

## 2. 격리 수준(Isolation Level)과 부정합 현상

격리 수준은 "남이 하는 일을 어디까지 볼 것인가"에 대한 설정입니다.

### 정합성 위배 시나리오 4가지

1. **Dirty Read:** 커밋 안 된 남의 '가짜 데이터'를 읽음.
2. **Non-Repeatable Read:** 내가 읽는 중 남이 '수정'해서 내 결과가 계속 바뀜.
3. **Phantom Read:** 내가 읽는 중 남이 '삽입'해서 없던 데이터가 유령처럼 나타남.
4. **Lost Update (중요):** 두 명의 수정자가 동시에 읽고 덮어써서 한 명의 수정 사항이 증발함.

### 격리 수준별 방어 범위

| 격리 수준 | Dirty Read | Non-Repeatable | Phantom Read | **Lost Update** |
| --- | --- | --- | --- | --- |
| **READ UNCOMMITTED** | 발생 | 발생 | 발생 | 발생 |
| **READ COMMITTED** | **방지** | 발생 | 발생 | 발생 |
| **REPEATABLE READ** | **방지** | **방지** | **방지(MySQL)** | **발생(위험!)** |
| **SERIALIZABLE** | **방지** | **방지** | **방지** | **방지** |

---

## 3. 핵심 질문: "왜 REPEATABLE READ에서 락이 필요한가?"

**질문:** MySQL은 기본이 `REPEATABLE READ`라 데이터가 안 변한다면서요? 근데 왜 갱신 분실(Lost Update)이 발생하나요?

**답변:** `REPEATABLE READ`는 **조회(Read)** 시점의 일관성(스냅샷)을 보장할 뿐입니다.

* A와 B가 동시에 재고 10개를 읽습니다(둘 다 스냅샷 10을 봄).
* A가 1을 빼고 9를 저장합니다.
* B도 자기가 본 10에서 1을 빼고 9를 저장합니다.
* 결과적으로 2개가 팔렸는데 재고는 9가 됩니다.
* **해결책:** 조회할 때부터 "나 이거 수정할 거니까 아무도 손대지 마!"라고 선언하는 비관적 락(`FOR UPDATE`)을 걸어야 합니다.

---

## 4. 도메인 모델 패턴: Entity vs Service 역할

**질문:** 왜 로직을 Service가 아니라 Entity에 두나요?

**답변:** 엔티티는 데이터 바구니가 아니라 **자기 데이터를 스스로 보호하는 객체**여야 합니다.

* **Entity (Worker):** "내 재고가 마이너스가 되면 안 돼!"라는 핵심 규칙을 직접 수행.
* **Service (Manager):** DB에서 데이터를 꺼내오고(Repository), 엔티티에게 일을 시키고, 결과를 커밋하는 전체 흐름 관리.

---

## 5. 실습 코드 

### [Repository] 격리성을 강제하는 락 정의

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // 일반적인 조회 (스냅샷 읽기, Lost Update 위험)
    Optional<Product> findById(Long id);

    // 비관적 락 조회 (SELECT ... FOR UPDATE)
    // 다른 트랜잭션이 읽거나 쓰지 못하게 해당 행을 점유함
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);
}

```

### [Entity] 일관성을 지키는 비즈니스 로직

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {
    @Id @GeneratedValue
    private Long id;
    private int stock;

    // Consistency(일관성): 데이터 무결성을 엔티티 내부에서 강제
    public void removeStock(int quantity) {
        if (this.stock < quantity) {
            throw new NotEnoughStockException("재고가 부족합니다.");
        }
        this.stock -= quantity;
    }
}

```

### [Service] 원자성을 보장하는 트랜잭션 관리

```java
@Service
@RequiredArgsConstructor
public class StockService {
    private final ProductRepository productRepository;

    @Transactional // Atomicity(원자성): 전체 성공 아니면 전체 취소
    public void decreaseStock(Long productId, int quantity) {
        // 1. Isolation(격리성): 락을 동반한 조회로 동시 접근 차단
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new EntityNotFoundException("상품 없음"));

        // 2. 비즈니스 로직 실행 (Entity가 스스로 검증)
        product.removeStock(quantity);
        
        // 3. 더티 체킹에 의해 메서드 종료 시 UPDATE 실행 및 커밋
    }
}

```

---

## 1. 락(Lock): `FOR UPDATE`의 정체

로그에서 본 `SELECT ... FOR UPDATE`는 단순한 조회가 아니라 "이 줄(Row)은 이제 내 거니까 아무도 손대지 마!"라고 선언하는 행위입니다.

* **배타적 잠금(Exclusive Lock):** 한 스레드가 `FOR UPDATE`로 특정 행을 점유하면, 다른 스레드는 해당 행에 대해 `UPDATE`, `DELETE`는 물론, 똑같은 `FOR UPDATE` 조차 할 수 없습니다.
* **대기열 발생:** 로그가 뒤죽박죽이었던 이유는 100개의 스레드가 동시에 "나도 락 줘!"라고 외쳤기 때문입니다. 하지만 실제 DB 내부에서는 **먼저 도달한 쿼리 순서대로 락을 할당**하고 나머지는 줄을 세워 대기시킵니다.
* **원자성 보장:** 덕분에 `조회(100개) -> 연산(99개) -> 저장(99개)` 과정이 중단 없이 한 세트로 실행될 수 있었습니다.

---

## 2. 트랜잭션 격리(Transaction Isolation)와 일관성

멀티스레드 환경에서 가장 무서운 건 "남이 고치고 있는 데이터를 내가 읽는 것"입니다.

* **Race Condition 방지:** 만약 락이 없었다면, 스레드 A가 재고를 100에서 99로 바꾸는 사이, 스레드 B도 100을 읽어가는 사고가 터집니다. 결과적으로 둘 다 99로 저장하여 재고가 1개만 줄어들게 되죠.
* **실시간 일관성:** `FOR UPDATE`를 사용하면 트랜잭션 격리 수준이 무엇이든 상관없이(보통 `READ COMMITTED`), **가장 최신의, 그리고 락이 해제된 확정 데이터**만 읽도록 강제합니다.
* **순차적 처리:** 로그에서 `UPDATE` 쿼리가 `SELECT ... FOR UPDATE` 이후에 차례대로 찍히는 것은, 앞선 트랜잭션이 커밋(Commit)되어 락을 풀어줘야만 다음 스레드가 최신 재고 값을 읽고 자기 차례를 시작할 수 있음을 보여줍니다.

---

## 3. 로그 분석 최종 요약

| 단계 | 로그에 나타난 현상 | 실제 벌어지는 일 (락 & 트랜잭션) |
| --- | --- | --- |
| **1. 락 획득** | `SELECT ... FOR UPDATE` 우르르 발생 | DB가 특정 ID의 행에 **비관적 락**을 걸고 다른 접근을 차단함 |
| **2. 대기** | 로그가 섞이고 실행이 지연됨 | 락을 얻지 못한 스레드들이 DB 엔진 안에서 **Wait 상태**로 대기함 |
| **3. 수정** | `UPDATE d4_product SET stock=?` | 락을 쥔 스레드가 안전하게 값을 수정하고 DB에 반영함 |
| **4. 해제** | 다음 스레드의 `SELECT`가 이어짐 | **트랜잭션 커밋**과 동시에 락이 풀리고, 대기하던 다음 순번이 진입함 |

---

### 💡 결론

결국 "뒤죽박죽인 로그"는 수많은 요청이 한정된 자원(재고)을 차지하려는 전쟁터였고, `FOR UPDATE`는 그 전쟁터에서 질서를 유지하는 **강력한 보안 요원** 역할을 한 셈입니다.

이제 이 원리를 이해하셨으니, 어떤 복잡한 동시성 문제도 "로그만 보면 락이 어디서 걸리는지" 한눈에 파악하실 수 있을 거예요!



### 최종 요약 노트

1. 격리 수준(RR)만으로는 동시에 같은 데이터를 수정하는 **Lost Update**를 막을 수 없다.
2. 이를 위해 Repository에서 비관적 락(`@Lock`)을 걸어 순차적으로 처리되게 한다.
3. Service(`@Transactional`)는 전체 작업의 원자성을, Entity(메서드)는 데이터의 일관성을 책임진다.
4. 이 모든 조각이 합쳐져야 **데이터 정합성**이 완벽히 보장된다.

