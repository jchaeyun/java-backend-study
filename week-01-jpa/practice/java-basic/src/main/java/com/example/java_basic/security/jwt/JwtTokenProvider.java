package com.example.java_basic.security.jwt;

import io.jsonwebtoken.Jwts;
import java.util.Collections;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

//토큰 생성,검증,정보 추출 담당
@Component
public class JwtTokenProvider {
    private final String secretKey = "vla-auction-platform-amber-secret-key-2026-spring-bright-istp";
    private final long tokenValidityInMilliseconds = 30 * 60 * 1000L;
    private Key key;

    @PostConstruct
    protected void init() {
        // StandardCharsets.UTF_8을 사용하여 바이트 배열로 변환 후 키 생성
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    // 1. 토큰 생성 (Fluent API 스타일)
    //유저의 식별 정보를 가져와서 서버의 secretkey로 signiture 생성
    public String createToken(String userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + tokenValidityInMilliseconds);

        //신분증 조립
        return Jwts.builder()
                .subject(userId)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    // 2. 토큰에서 인증 정보 조회
    public Authentication getAuthentication(String token) {
        //JWT의 배를 갈라 그 안에 있는 유저 정보를 꺼냄
        String userId = Jwts.parser()
                .verifyWith((SecretKey) key) //서버의 비밀키로 이 신분증 진짜인지 검증
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();

        //토큰을 읽고, 스프링 시큐리티 전용 신분증(객체)로 번역
        //(userId-토큰에서 추출한 값,비밀번호-이미 토큰으로 인증,필요없으니 빈칸.권한 리스트(실습용이라 빈칸))
        return new UsernamePasswordAuthenticationToken(userId, "", Collections.emptyList());
    }

    // 3. 토큰 유효성 및 만료일자 확인
    // 클라이언트가 가져온 토큰의 서명이 내 비밀키로 계산한 값과 일치하는지 확인. 수정되었다면 false 발생
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith((SecretKey) key)
                    .build()
                    .parseSignedClaims(token);   // 서명이 틀리거나 만료되면 여기서 Exception 발생
            return true;
        } catch (Exception e) {
            // 로깅이 필요하다면 여기에 추가 (예: 만료됨, 지원되지 않음 등)
            return false;
        }
    }
}