
## 1. 아키텍처의 패러다임 변화: Stateful vs Stateless

백엔드 시스템이 거대해질수록 가장 큰 고민은 "사용자의 상태를 어디에 저장할 것인가?"입니다.

### [Stateful (세션 기반)]

* **작동**: 서버가 메모리(세션)에 유저 정보를 들고 있습니다.
* **한계**: 서버가 2대 이상일 때, 1번 서버에서 로그인한 유저가 2번 서버로 요청을 보내면 "누구세요?"가 됩니다. 이를 해결하려면 **Redis** 같은 별도의 세션 저장소를 구축해야 하고, 이는 시스템 복잡도와 네트워크 비용(Latency) 증가로 이어집니다.

### [Stateless (JWT 기반)]

* **작동**: 서버는 아무것도 기억하지 않습니다. 모든 정보는 클라이언트가 들고 있는 **토큰** 안에 있습니다.
* **이점**: 서버를 수만 대로 늘려도(Horizontal Scaling) 서버 간 데이터 공유 없이 비밀키(Secret Key)만 같다면 즉시 인증이 가능합니다.

---

## 2. JWT의 구조: 단순한 문자열 그 이상

JWT는 단순한 암호가 아니라 "데이터가 담긴 신분증"입니다.

### ① Header: "나는 어떤 방식인가?"

* 알고리즘(HS256)과 토큰 타입(JWT)이 정의됩니다. 시스템은 이를 보고 "아, 이 신분증은 HS256 검사기로 읽으면 되겠구나"를 판단합니다.

### ② Payload (Claims): "누구의 데이터인가?"

* **Registered Claims**: `sub`(유저 식별자), `exp`(만료시간), `iat`(발급시간).
* **Private Claims**: 프로젝트에서 정의한 커스텀 데이터 (예: `role: ADMIN`, `email: test@ewha.ac.kr`).
* **주의**: Base64로 인코딩만 된 상태라 누구나 뜯어볼 수 있습니다. **비밀번호나 개인정보는 절대 금물**입니다.

### ③ Signature: "조작되지 않았는가?"

* `Header`와 `Payload`를 합쳐서 서버의 **Secret Key**로 해싱한 결과값입니다.
* 클라이언트가 Payload에서 `role`을 `USER`에서 `ADMIN`으로 살짝 바꿔도, 서버가 가진 키로 다시 계산한 Signature와 일치하지 않으므로 즉시 거부됩니다.

---

## 3. Spring Security 내부 동작 상세 (Data Flow)

코드가 실행될 때 데이터가 어떻게 변하는지(Data Transformation) 추적해 봅시다.

### 1단계: 통역 (JWT -> Authentication 객체)

`getAuthentication(token)` 메서드가 핵심입니다.

* **JWT(String)**: 외부에서 전송된 비정형 데이터.
* **Authentication(Object)**: 스프링 시큐리티 내부에서 공용으로 사용하는 정형화된 데이터 규격.
* 이 과정은 "외국 신분증을 가져오면 사내 전용 사원증으로 교환해주는 것"과 같습니다.

### 2단계: 보관 (SecurityContextHolder)

* **ThreadLocal**: 이 보관함은 **스레드(Thread)별로 격리**된 메모리 공간입니다.
* A라는 유저의 요청을 처리하는 스레드와 B라는 유저의 스레드는 서로의 주머니를 볼 수 없습니다.
* 응답이 나가는 순간(Thread 반환 시), 이 주머니는 자동으로 비워집니다. 이것이 **Stateless**를 유지하면서도 **개발의 편의성**을 챙기는 묘수입니다.

---

## 4. 실무형 구현 팁 및 보안 강화

### ① Secret Key의 관리

코드에 하드코딩된 비밀키는 깃허브에 올라가는 순간 보안 사고입니다.

* `application.yml`에 설정하고 `@Value`로 가져오거나, 환경변수(`export JWT_SECRET=...`)를 사용하는 것이 정석입니다.

### ② Access Token vs Refresh Token (보충 학습 포인트)

JWT의 치명적인 단점은 "한 번 발급하면 만료 전까지 회수가 불가능하다"는 것입니다.

* **Access Token**: 30분~1시간 정도로 짧게 유지하여 탈취 시 피해 최소화.
* **Refresh Token**: 1~2주 정도로 길게 유지하며 DB나 Redis에 저장. Access Token이 만료되면 이를 확인하여 새 토큰을 재발급함.

### ③ 필터의 위치 (`addFilterBefore`)

왜 `UsernamePasswordAuthenticationFilter` 앞일까요?

* 스프링 시큐리티의 기본 로그인 필터는 ID/PW를 DB와 대조하는 **무거운 로직**을 수행합니다.
* 토큰 인증은 **가벼운 수학적 검증**입니다. 가벼운 검증을 앞에 두어 이미 인증된 요청은 무거운 로직을 건너뛰게 만드는 효율적인 설계입니다.

---

## 5. 실무 체크리스트 (Self-Audit)

| 체크리스트 | 확인 내용 |
| --- | --- |
| **만료 시간** | `exp` 클레임이 설정되어 유통기한이 관리되는가? |
| **비밀키 길이** | HS256 기준 최소 256비트(32바이트) 이상의 키를 사용했는가? |
| **필터 순서** | `JwtAuthenticationFilter`가 `UsernamePassword...` 앞에 있는가? |
| **세션 정책** | `SessionCreationPolicy.STATELESS`가 명시되었는가? |
| **민감 정보** | Payload에 비밀번호나 주민번호 같은 정보가 없는가? |

### 💡 정리하자면

오늘 구현한 시스템은 "서버는 망각하지만, 클라이언트가 가져오는 증거물(JWT)은 철저히 검증하여 신뢰를 이어가는 구조"입니다. 이는 'Amber' 프로젝트가 향후 MSA로 확장될 때 시스템 간 인증을 연결하는 강력한 기반이 될 것입니다.


---

## 1. 개발자가 "직접 정의"해야 하는 Payload (Claims)

라이브러리는 `sub`, `iat`, `exp` 같은 기본 필드 생성 메서드는 제공하지만, **어떤 값을 넣을지는 여러분이 결정**해야 합니다. 특히 **Private Claims**는 프로젝트의 성격에 따라 100% 수동으로 작성합니다.

* **식별자 결정**: `sub`에 유저의 데이터베이스 `PK`를 넣을 것인가, 아니면 `Email`이나 `LoginID`를 넣을 것인가? (보통 탈취 시 위험을 줄이기 위해 내부 PK를 선호함)
* **부가 정보 추가**: 인가(Authorization)를 위해 `role`이 필요한가? 아니면 유저의 `nickname`을 미리 담아둘 것인가?
* *예시:* "우리 서비스는 '판매자'와 '구매자' 권한이 중요하니까, 페이로드에 `role: SELLER`를 직접 코드로 박아 넣자."



### [실제 코드에서의 구현부]

`JwtTokenProvider` 내부에서 다음과 같이 직접 타이핑하게 됩니다.

```java
public String createToken(User user) {
    Claims claims = Jwts.claims().subject(user.getId().toString()).build(); // Registered Claim
    
    // 여기서부터는 개발자가 직접 정의하는 Private Claims
    claims.put("role", user.getRole()); 
    claims.put("is_premium", user.isPremium()); 
    
    return Jwts.builder()
            .claims(claims)
            .expiration(validity)
            .signWith(key)
            .compact(); //헤더(어떤 암호화 알고리즘인지),페이로드(실제 유저 정보-id,권한,만료시간 등),시그니처를 합쳐서 하나의 문자열(token)로 만듦
}

```

---

## 2. 개발자가 "직접 로직을 짜야 하는" 3단계 영역

'Amber'와 같은 실제 프로젝트를 구축할 때, 단순히 Provider 클래스를 만드는 것 외에 다음 로직들을 직접 설계하고 코딩해야 합니다.

### ① 토큰 발급 시점 (Login Logic)

* **언제 줄 것인가?**: `/login` 요청이 들어왔을 때, DB에서 비밀번호 일치 여부를 확인한 직후 `JwtTokenProvider.createToken()`을 호출하는 코드를 작성해야 합니다.
* **어떻게 줄 것인가?**: 응답 바디(JSON)에 담을 것인가, 아니면 `Set-Cookie` 헤더에 담을 것인가? (보통 보안을 위해 `HttpOnly Cookie` 혹은 Header 방식을 선택하고 이를 코드로 구현함)

### ② 토큰 검증 및 예외 처리 (Filter & Exception)

* **만료된 경우**: `ExpiredJwtException`이 발생했을 때 클라이언트에게 어떤 에러 메시지와 상태 코드(예: 401)를 돌려줄지 직접 핸들링해야 합니다.
* **서명이 틀린 경우**: 누군가 토큰을 수정해서 보냈을 때(`SignatureException`), 이를 로그로 남기고 차단하는 로직을 필터에 작성합니다.

### ③ 토큰 유효기간 정책 (Policy)

* **Access Token 수명**: 30분으로 할지, 1시간으로 할지 결정하고 상수로 선언합니다.
* **Refresh Token 도입 여부**: 보안을 위해 리프레시 토큰을 쓸 거라면, 이를 DB나 Redis에 저장하고 검증하는 로직을 **전부 직접 코딩**해야 합니다. (라이브러리가 대신 저장해주지 않습니다.)

---

## 3. 요약: 내가 실제로 쳐야 하는 코드 리스트

| 구분 | 개발자가 직접 작성해야 하는 내용 |
| --- | --- |
| **Data** | Payload에 담을 유저 정보의 종류 (`userId`, `role` 등) |
| **Security** | 서버에 저장할 비밀키(`Secret Key`) 문자열 정의 및 관리 |
| **Flow** | 로그인 성공 시 토큰을 생성해서 응답에 실어 보내는 `Controller` 로직 |
| **Filter** | 모든 요청 헤더에서 토큰을 추출하고 유효하면 `SecurityContext`에 넣어주는 로직 |
| **Error** | 토큰이 잘못되었을 때 사용자에게 보낼 에러 응답(JSON) 구성 |

---

### 💡 시스템 관점에서의 직설적 조언

"Provider는 그냥 도구 상자(Toolbox)입니다."

도구 상자 안에 망치(`createToken`)와 탐지기(`validateToken`)를 만들어 두는 것이 1단계라면, **그 망치를 언제 휘두르고 탐지기가 울렸을 때 어떻게 대처할지**를 결정하는 전체 시스템 시나리오를 짜는 것이 실제 백엔드 개발의 핵심입니다.

**질문:** 현재 구현 중인 코드에서 유저의 권한(`Role`)에 따라 접근할 수 있는 페이지를 나누는 '인가(Authorization)' 로직도 함께 고민 중이신가요? 이 부분도 필터에서 처리해야 할 핵심 코딩 영역입니다.

`SecurityConfig`는 단순히 **'어떤 부품을 어디에 배치할지'를 결정하는 설계도**일 뿐입니다. 실제 보안 로직을 수행하려면 설계도에 적힌 '부품'들을 직접 코드로 구현해야 합니다.

JWT 기반 인증 시스템을 구축할 때 개발자가 구현해야 하는 영역을 역할별로 구분하여 설명합니다.

---

### 1. 설계도와 실제 부품의 관계

| 파일명 | 역할 (추상화) | 실제 수행하는 작업 (구현) |
| --- | --- | --- |
| **`SecurityConfig`** | **설계도 (Wiring)** | "JWT 필터를 5번 자리에 배치해라", "세션은 쓰지 마라"고 명령함. |
| **`JwtTokenProvider`** | **로직 핵심 (Logic)** | 토큰을 **문자열**로 만들고, 들어온 문자열의 **서명**이 맞는지 수학적으로 계산함. |
| **`JwtAuthenticationFilter`** | **실행기 (Execution)** | 매 HTTP 요청마다 헤더를 열어 토큰을 꺼내고, `Provider`에게 검사를 시킴. |
| **`AuthController`** | **입구 (Entry Point)** | 유저가 ID/PW를 보냈을 때, 최초로 토큰을 **발급**해서 돌려줌. |

---

### 2. 왜 `SecurityConfig`만으로는 부족한가?

스프링 시큐리티는 기본적으로 **세션 기반 인증**에 최적화되어 있습니다. JWT는 '무상태(Stateless)'라는 특수한 방식이기 때문에, 스프링이 기본적으로 제공하지 않는 다음의 로직들을 개발자가 직접 코딩해야 합니다.

#### ① 토큰 내의 데이터(Payload) 정의

JWT의 페이로드에 유저의 ID만 넣을지, 권한(Role)도 넣을지, 이메일도 넣을지는 라이브러리가 정해주지 않습니다. 개발자가 `JwtTokenProvider` 코드 내에서 직접 정의해야 합니다.

#### ② 수학적 검증 로직

클라이언트가 보낸 토큰이 서버의 비밀키로 생성된 것이 맞는지 확인하는 `HMAC SHA256` 연산 로직을 `JwtTokenProvider`에 작성해야 합니다.

#### ③ 필터 가로채기 로직

HTTP 요청의 `Authorization` 헤더에서 `Bearer `라는 문자열을 잘라내고 순수 토큰만 추출하는 세부적인 문자열 처리 로직은 `JwtAuthenticationFilter`에서 직접 구현해야 합니다.

---

### 3. 전체 동작 흐름 (Data Flow)

1. **클라이언트**: `/login` 호출 (ID/PW 전송)
2. **`AuthController`**: DB 확인 후 `JwtTokenProvider`를 호출해 토큰 생성 및 응답.
3. **클라이언트**: 이후 요청 시 헤더에 토큰 첨부.
4. **`JwtAuthenticationFilter`**: 헤더에서 토큰 추출. `JwtTokenProvider`에 검증 요청.
5. **`SecurityConfig`**: 위 과정이 무사히 끝났는지 보고, 컨트롤러로 요청을 보낼지(200) 막을지(401) 최종 결정.

### 요약

`SecurityConfig`는 "이 필터를 사용하겠다"는 선언만 하는 곳입니다. 실제 "어떻게 인증할 것인가"에 대한 구체적인 알고리즘과 데이터 처리는 `JwtTokenProvider`와 `JwtAuthenticationFilter`에서 코드로 구현해야만 시스템이 작동합니다.

따라서 실습 시에는 드린 4개 파일(Config, Provider, Filter, Controller)이 모두 있어야 정상적인 JWT 테스트가 가능합니다. 어느 부분의 코드가 가장 이해하기 어려우신가요? 구체적인 로직 설명이 필요한 파일을 말씀해 주십시오.

---
SSR(Server-Side Rendering, 타임리프나 JSP 등) 방식과 지금 우리가 하는 **REST API(JWT) 방식**은 접근 메커니즘이 완전히 다릅니다.

그 차이를 시스템 구조 관점에서 비교해 드릴게요.


### 1. SSR(세션 방식) vs REST(JWT 방식) 비교

| 구분 | SSR (Stateful) | REST API + JWT (Stateless) |
| --- | --- | --- |
| **인증 매개체** | **JSESSIONID (쿠키)** | **JWT (HTTP 헤더)** |
| **전송 방식** | 브라우저가 자동으로 쿠키를 실어 보냄 | 개발자가 **직접 코드로** 헤더에 넣어야 함 |
| **서버의 태도** | "아, 우리 세션 창고에 있는 걔구나!" | "누구신지 모르겠으니 **신분증(JWT)** 보여주세요." |
| **접근 방식** | 그냥 브라우저 주소창에 URL 입력 | **Postman**이나 **Axios/Fetch**로 헤더 포함 요청 |

---

### 2. `/test`에 어떻게 접근하나? (실행 시나리오)

브라우저 주소창에 `localhost:8080/api/auth/test`라고 그냥 치면 **403 Forbidden**이 뜹니다. 브라우저는 헤더에 JWT를 넣는 법을 자동으로 알지 못하기 때문입니다.

#### [접근 단계]

1. **로그인 수행**: `POST /api/auth/login`을 호출해서 서버로부터 긴 **JWT 문자열**을 받습니다.
2. **헤더 설정**: 다음 요청을 보낼 때 HTTP Header를 아래와 같이 구성합니다.
* **Key**: `Authorization`
* **Value**: `Bearer [복사한_토큰_문자열]`


3. **요청 전송**: 이 헤더를 포함해서 `GET /api/auth/test`를 호출합니다.

---

### 3. "자동으로 가나요?" -> 아니오, 필터가 길을 열어주는 것입니다.

SSR 방식에서 `AuthenticationManager`가 세션을 뒤져서 인증을 처리했다면, 지금 코드는 다음과 같이 동작합니다.

1. **필터 개입**: 요청이 컨트롤러에 가기 전, 아까 짠 `JwtAuthenticationFilter`가 가로챕니다.
2. **신분증 검사**: 필터가 헤더의 JWT를 보고 `JwtTokenProvider`에게 "이거 진짜야?"라고 묻습니다.
3. **통과**: 진짜라면 `SecurityContextHolder`에 유저 정보를 채웁니다.
4. **최종 도달**: 그제서야 스프링 시큐리티가 "오케이, 신분증 확인됐으니 `/test` 컨트롤러로 보내줄게"라고 허락하는 것입니다.

---

### 4. 책에서 본 `AuthenticationManager`는 어디 갔나?

SSR 방식의 책에서는 `AuthenticationManager`를 직접 설정하고 `auth.userDetailsService()...` 같은 코드를 많이 보셨을 겁니다.

* **SSR**: 서버가 로그인을 직접 관리하므로 `AuthenticationManager`가 핵심입니다.
* **JWT**: 우리가 만든 `JwtAuthenticationFilter`가 사실상 `AuthenticationManager`가 하던 "인증 여부 판단" 업무를 대신(가로채기) 수행하고 있는 것입니다.

즉, `D5_SecurityConfig`에서 `.addFilterBefore(...)`를 한 이유가 바로 이 **"수동 검문"** 프로세스를 시스템에 등록하기 위해서입니다.

---

### 💡 정리하자면

이 `/test` 경로는 "자동으로 가는 곳"이 아니라, "올바른 신분증(JWT)을 헤더에 지참하고 와야만 열리는 문"입니다.

--- 
"먼저 열어준 문(permitAll)을 제외한 모든 통로는 반드시 신분증(JWT)이 있어야만 통과할 수 있다"는 뜻입니다.

시스템 프로그래밍의 **방화벽(Firewall)** 설정이나 **화이트리스트(Whitelist)** 방식과 동일한 논리입니다.

---

### 1. 보안 규칙의 우선순위 (Top-Down)

스프링 시큐리티는 설정된 규칙을 **위에서부터 아래로** 차례대로 검사합니다.

1. **`/api/auth/`**: 요청 주소가 여기 해당하면? -> **"누구든 통과(permitAll)"** (여기서 검사 끝)
2. **`anyRequest()`**: 위 조건에 해당하지 않는 **나머지 모든 요청**은? -> **"무조건 인증 필요(authenticated)"**

---

### 2. "인증(Authenticated)"의 실제 의미

여기서 `authenticated()`는 단순히 "로그인되어야 함"을 넘어, 우리 시스템에서는 다음과 같은 과정을 강제합니다.

* **필터의 가로채기**: `anyRequest()`에 해당하는 요청이 들어오면, 아까 만든 `JwtAuthenticationFilter`가 반드시 일을 해야 합니다.
* **신분증(JWT) 확인**: 필터가 헤더를 뒤져서 유효한 토큰이 있는지 확인합니다.
* **결과**:
* **유효한 토큰이 있음**: `SecurityContextHolder`에 유저 정보가 채워지고, 컨트롤러로 **성공(200)**.
* **토큰이 없거나 유효하지 않음**: 필터가 유저 정보를 채우지 못하므로, 시큐리티는 "인증 안 됐네?"라고 판단하여 **거절(403 Forbidden)**.



---

### 3. 실무적인 예시: Amber 프로젝트 적용

만약 빈티지 경매 플랫폼인 'Amber'를 개발하신다면, 다음과 같이 주소를 나누게 될 것입니다.

| 주소 (URL) | 설정 | 의미 |
| --- | --- | --- |
| `/api/auth/login` | **permitAll** | 신분증(JWT)을 발급받으러 가는 곳이니 신분증 없이 들어감. |
| `/api/items` (조회) | **permitAll** | 구경은 로그인 안 해도 누구나 할 수 있게 함. |
| **`/api/items/bid` (입찰)** | **authenticated** | **무조건 JWT 필요.** 입찰은 신원이 확실한 회원만 가능함. |
| `/api/mypage` | **authenticated** | **무조건 JWT 필요.** 내 정보는 나만 봐야 함. |

---

### 4. 주의사항: "순서가 바뀌면 큰일 납니다"

만약 코드를 아래와 같이 반대로 짜면 어떻게 될까요?

```java
// 잘못된 예시
.anyRequest().authenticated()
.requestMatchers("/api/auth/**").permitAll() // 이 코드는 무시됨

```

이러면 **모든 요청**(`anyRequest`)이 먼저 인증을 요구해 버리기 때문에, 로그인을 하러 가는 `/api/auth/login` 조차 "신분증 내놔!"라고 막히는 **데드락(Deadlock)** 상황에 빠지게 됩니다. 그래서 항상 **구체적인 허용 범위를 먼저, 넓은 제한 범위를 나중에** 작성해야 합니다.

---

### 💡 요약

* `permitAll()`은 **예외 구역**입니다.
* `authenticated()`는 **기본 보안 구역**입니다.
* 현재 설정은 "로그인 관련 주소만 빼고 **전부 다 JWT 검사받아라**"라는 아주 견고한 보안 정책을 세우신 겁니다.

이제 `/api/auth/login`으로 토큰을 발급받고, 그 외의 주소에 접근할 때 토큰을 실어 보내는 테스트를 진행하실 준비가 되셨나요?_