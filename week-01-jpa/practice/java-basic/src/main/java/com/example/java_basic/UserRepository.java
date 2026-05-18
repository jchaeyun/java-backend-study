package com.example.java_basic;
import com.example.java_basic.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Long>{
    // 2. Fetch Join을 적용한 쿼리 최적화
    // 트랜잭션이 없는 보안 필터 계층에서 User를 조회할 때, Role 정보까지 한 번의 SQL로 가져옴.
    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);
}
