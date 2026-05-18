package com.example.java_basic.security;
import com.example.java_basic.User;
import com.example.java_basic.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

// 스프링 컨테이너에 서비스 빈으로 등록합니다.
@Service
// final로 선언된 필드(UserRepository)의 생성자를 자동으로 만들어 의존성을 주입합니다.
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService{
    private final UserRepository userRepository;

    // UserDetailsService 인터페이스의 필수 구현 메서드입니다. 유저 식별자를 받아 유저 정보를 반환해야 합니다.
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Fetch Join이 적용된 메서드 호출 (쿼리 1번 수행)
        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        // 조회된 도메인 엔티티(User) 데이터를 스프링 시큐리티가 이해할 수 있는 규격인 UserDetails 객체로 변환하여 반환합니다.
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                // List<String> 형태인 권한을 시큐리티 전용 권한 객체(SimpleGrantedAuthority) 리스트로 변환합니다.
                user.getRoles().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
        );
    }
}
