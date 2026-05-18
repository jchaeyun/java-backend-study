package com.example.java_basic;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    // 1. FetchType.LAZY 설정 (OSIV OFF 환경에서 N+1 문제의 원인이 됨)
    // 지연 로딩(LAZY)이 기본값이므로, User 엔티티를 조회할 때 이 테이블은 즉시 조회되지 않습니다.

    @ElementCollection(fetch = FetchType.LAZY)
    // 이 테이블이 원본 User 테이블을 참조할 외래키(Foreign Key) 컬럼을 "user_id"로 설정합니다.
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    // "user_roles" 테이블 내에서 권한 문자열이 저장될 실제 컬럼명을 "role"로 지정합니다.
    @Column(name = "role")
    private List<String> roles = new ArrayList<>();// 권한 데이터를 담을 리스트입니다. NullPointerException을 방지하기 위해 빈 리스트로 초기화합니다.

    // Builder 패턴을 적용하여 객체 생성 시 가독성을 높입니다.
    @Builder
    public User(String email, String password, List<String> roles) {
        this.email = email;
        this.password = password;
        this.roles = roles;
    }
}
