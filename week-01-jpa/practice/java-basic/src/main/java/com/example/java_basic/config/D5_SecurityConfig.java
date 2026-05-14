package com.example.java_basic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity // 스프링 시큐리티 활성화
public class D5_SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // 실습 편의를 위해 CSRF 비활성화
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/public/**").permitAll() // 해당 경로는 누구나 접근 가능
                        .anyRequest().authenticated()             // 그 외 모든 요청은 인증 필요
                )
                .formLogin(withDefaults()) // 기본 로그인 페이지 활성화
                .httpBasic(withDefaults()); // HTTP Basic 인증 활성화

        return http.build();
    }
}
