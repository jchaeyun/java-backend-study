package com.example.java_basic;

import com.example.java_basic.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
@RestController
@RequestMapping
@RequiredArgsConstructor
public class D6AuthController {
    private final JwtTokenProvider jwtTokenProvider;

    // 테스트용 로그인: ID만 던지면 바로 토큰 발급
    @PostMapping("/login")
    public String login(@RequestBody Map<String, String> user) {
        return jwtTokenProvider.createToken(user.get("email"));
    }

    // 토큰이 있어야만 접근 가능한 테스트 페이지
    @GetMapping("/test")
    public String test() {
        return "JWT 인증 성공! 이 내용은 신분증이 있는 사람만 보입니다.";
    }
}
