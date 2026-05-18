package com.example.java_basic.config;

import com.example.java_basic.security.CustomUserDetailsService;
import com.example.java_basic.security.jwt.JwtAuthenticationFilter;
import com.example.java_basic.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

//설계도
@Configuration
@RequiredArgsConstructor
@EnableWebSecurity // 스프링 시큐리티 활성화
public class D5_SecurityConfig {
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // 세션을 사용하지 않으므로 STATELESS 설정 (중요)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login").permitAll() // 로그인 등은 허용
                        .anyRequest().authenticated()
                )
                // UsernamePasswordAuthenticationFilter 이전에 JWT 필터 실행->이미 토큰이 인증되었다면 로그인 로직 탈 이유 없으므로
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider,customUserDetailsService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
