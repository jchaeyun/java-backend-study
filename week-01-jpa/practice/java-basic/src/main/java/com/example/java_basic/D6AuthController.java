package com.example.java_basic;

import com.example.java_basic.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class D6AuthController {
    private final JwtTokenProvider jwtTokenProvider;

    // 테스트용 로그인: ID만 던지면 바로 토큰 발급
    @PostMapping("/login")
    public String login(@RequestBody Map<String, String> request) {
        // 실제로는 입력받은 비밀번호와 DB 비밀번호를 대조하는 로직이 필요함. (실습 생략)
        return jwtTokenProvider.createToken(request.get("email"));
    }

    // 토큰이 있어야만 접근 가능한 테스트 페이지
    // 앞선 보안 필터에서 SecurityContext에 저장해둔 로그인 유저 정보(UserDetails)를 주입받습니다.
    // 5. SecurityContext에 세팅된 Authentication 객체가 컨트롤러까지 정상 전달되는지 확인
    @GetMapping("/test")
    public String test(@AuthenticationPrincipal UserDetails userDetails) {
        return "접근 성공! 현재 로그인된 유저: " + userDetails.getUsername() +
                ", 권한: " + userDetails.getAuthorities();
    }
}
