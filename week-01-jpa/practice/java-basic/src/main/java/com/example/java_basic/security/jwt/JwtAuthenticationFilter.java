package com.example.java_basic.security.jwt;

import com.example.java_basic.security.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
// OncePerRequestFilter를 상속받아, 1번의 HTTP 요청당 단 1번만 실행됨을 보장합니다.
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService; // DB 대조용 의존성 추가

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // HTTP 헤더에서 토큰 문자열만 정확히 잘라내어 가져옵니다.
        String token = resolveToken(request);

        // 토큰이 존재하고, 유효성 검사(만료 여부, 손상 여부 등)를 통과했다면 내부 로직을 실행합니다.3
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 3. 토큰에서 식별자(email)만 꺼낸 뒤, DB에 직접 조회하여 최신 상태(권한, 정지 여부 등) 대조
            String email = jwtTokenProvider.getSubject(token);
            // 추출한 이메일로 DB를 조회하여 최신 상태의 UserDetails 객체를 가져옵니다.
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

            // DB 대조까지 마친 UserDetails를 바탕으로 '인증된 상태'를 나타내는 토큰 객체를 생성합니다.
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
            // 최종적으로 생성된 인증 객체를 시큐리티 전용 스레드 공간인 SecurityContext에 저장합니다.
            // (이후 컨트롤러에서 @AuthenticationPrincipal로 이 값을 꺼내 쓰게 됩니다)552
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) return bearerToken.substring(7);
        return null;
    }
}
