package com.example.java_basic.security.jwt;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

//모든 HTTP 요청이 서블릿에 도달하기 전 거치는 전용 보안 필터
//요청 당 한 번만 실행
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 헤더에서 토큰 추출
        String token = resolveToken(request);

        // 2. 토큰 유효성 검사. 유효하다면
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 3. 신분증을 SecurityContextHolder(임시 보관함)에 저장->해당 요청 처리하는 스레드 내에서만 유효,요청 끝나면 휘발
            // 컨트롤러나 서비스 로직에서 지금 누가 보낸 요청인지 알 수 있는 방법:홀더 확인
            Authentication auth = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    //헤더에서 bearer를 떼고 순수한 토큰 문자열만 추출
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
