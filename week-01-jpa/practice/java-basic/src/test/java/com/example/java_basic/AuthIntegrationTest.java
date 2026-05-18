package com.example.java_basic;

import com.example.java_basic.User;
import com.example.java_basic.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc // 실제 톰캣을 띄우지 않고 가짜 서블릿 환경 구성
@Transactional        // 단일 스레드 Mock 환경이므로 테스트 종료 후 완벽히 Rollback 됨
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private String testEmail = "test@ewha.ac.kr";

    @BeforeEach
    void setUp() {
        // 테스트 실행 전 DB에 가짜 유저 세팅 (트랜잭션 내에서 실행되므로 테스트 종료 시 삭제됨)
        User user = User.builder()
                .email(testEmail)
                .password("password123")
                .roles(List.of("ROLE_USER"))
                .build();
        userRepository.save(user);
    }

    @Test
    @DisplayName("로그인 API: 정상적인 이메일 입력 시 JWT 토큰을 발급한다")
    void testLoginAndIssueToken() throws Exception {
        String requestBody = "{\"email\": \"" + testEmail + "\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String token = result.getResponse().getContentAsString();
                    assert !token.isEmpty();
                });
    }

    @Test
    @DisplayName("인가 API: 발급받은 토큰을 헤더에 실어 보호된 자원에 접근한다")
    void testAccessProtectedResourceWithToken() throws Exception {
        // 1. 토큰 발급
        String requestBody = "{\"email\": \"" + testEmail + "\"}";
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();
        String token = loginResult.getResponse().getContentAsString();

        // 2. 발급받은 토큰으로 보호된 API 접근
        mockMvc.perform(get("/api/auth/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("test@ewha.ac.kr")));
    }

    @Test
    @DisplayName("인가 API: 토큰이 없거나 잘못된 경우 403 Forbidden을 반환한다")
    void testAccessProtectedResourceWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/test"))
                .andExpect(status().isForbidden());
    }
}
