package com.shinsunyoung.blog.repository;

import com.shinsunyoung.blog.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 파일 위치: src/main/java/me/shinsunyoung/springbootdeveloper/repository/RefreshTokenRepository.java
//
// ============================================================
// 이 파일이 하는 일
// ============================================================
// RefreshToken 표(refresh_token 테이블)에 데이터를 저장(추가)하고, 찾고(조회), 삭제 방법을 정해 둔 파일입니다.
// 이 파일에는 실제 동작 코드가 없습니다. "이런 조회을 할 수 있다"는 인터페이스의 메소드만 적혀 있습니다.
// 실제 검색 코드는 스프링이 프로그램을 실행할 때 자동으로 만들어 줍니다.
//
// ============================================================
// 사용 예 (다른 파일에서 이렇게 씁니다)
// ============================================================
// refreshTokenRepository.save(새토큰)                        → 토큰을 표에 저장합니다.
// refreshTokenRepository.findByUserId(5L)                    → 사용자 번호 5번의 토큰을 찾습니다.
// refreshTokenRepository.findByRefreshToken("eyJhbGci...")   → 이 토큰 문자열을 가진 줄을 찾습니다.
//
// ============================================================
// 꼭 알아둘 낱말
// ============================================================
// 인터페이스(interface): 메서드 이름과 모양만 정해 둔 약속입니다. 실제 내용은 스프링이 채웁니다.
// Repository          : 데이터베이스와 주고받는 일을 맡은 도구입니다. "저장소"라고 이해하면 됩니다.
// JpaRepository       : 스프링이 미리 만들어 둔 기본 기능 묶음입니다. 저장, 조회, 삭제 기능이 들어 있습니다.
// 상속(extends)       : 기존 인터페이스의 기능을 그대로 물려받는다는 뜻입니다.
// 제네릭 <RefreshToken, Long>
//                     : 첫 번째 칸에는 "어떤 표(엔티티)를 다룰지", 두 번째 칸에는 "기본키의 타입"을 적습니다.
//                       여기서는 RefreshToken 표를 다루고, 기본키 id 는 Long 타입입니다.
// Optional            : "값이 있을 수도 있고, 없을 수도 있다"는 뜻을 나타내는 타입입니다.
//                       찾는 줄이 없으면 빈 Optional 을 돌려주므로, 없는 경우를 안전하게 처리할 수 있습니다.
// 메서드 이름 규칙     : 이름이 findBy + 열 이름 형태이면, 스프링이 그 열로 검색하는 코드를 자동으로 만듭니다.


//     <RefreshToken, Long>: RefreshToken 표(테이블)를 다루고, 기본키의 타입은 Long 이라는 뜻입니다.
//     JpaRepository인터페이스를  물려받으면 save(), findById(), delete() 같은 기본 기능을 별도로 적지 않아도 바로 쓸 수 있습니다.
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Optional<RefreshToken> findByUserId(Long userId);
    //   이름 해석: find(조회해서 찾다) + By(~로) + UserId(사용자 번호) → "사용자 번호로 조회 해서 찾다"
    //   매개변수로 전달 받는 값: userId (조회 해서 찾고 싶은 사용자 번호)
    //   돌려주는 값: 그 사용자 번호(id)를 가진 RefreshToken 줄이 있으면 그 줄, 없으면 빈 Optional
    //   쓰는 이유: 사용자가 다시 토큰을 요청했을 때, 기존 리프레시 토큰이 있는지 확인하기 위해서입니다.
    Optional<RefreshToken> findByUserId(Long userId);

    // Optional<RefreshToken> findByRefreshToken(String refreshToken);
    //   이름 해석: find(조회해서 찾다) + By(~로) + RefreshToken(토큰 문자열) → "리플러시 토큰 문자열 조회해서 찾다"
    //   매개변수로 전달 받는 값: refreshToken (찾고 싶은 리플러시 토큰 문자열)
    //   돌려주는 값: 조회한 그 리플러시 토큰 문자열값을  가진 줄이 있으면 그 줄, 없으면 빈 Optional
    //   쓰는 이유: 사용자가 요청 보낸 리프레시 토큰이 실제로 저장된 것인지 확인하기 위해서입니다.
    Optional<RefreshToken> findByRefreshToken(String refreshToken);

    // ===== [로그아웃 추가] deleteByUserId : 사용자 id 로 리프레시 토큰 행을 삭제합니다. =====
    // 메소드 이름 규칙(쿼리 메소드) : deleteBy + 필드이름(UserId)
    //   → 스프링 데이터 JPA 가 이름을 읽고 아래 동작을 자동으로 만들어 줍니다. (직접 SQL 을 쓰지 않습니다)
    //     1) select * from refresh_token where user_id = ?   (먼저 지울 행을 조회해서 엔티티로 만들고)
    //     2) delete from refresh_token where id = ?          (조회된 엔티티를 하나씩 삭제)
    // ⚠️ 이름으로 만든 delete 메소드는 반드시 트랜잭션 안에서 실행해야 합니다.
    //    그래서 이 메소드를 부르는 RefreshTokenService.delete() 에 @Transactional 을 붙였습니다.
    //    (트랜잭션 없이 부르면 "No EntityManager with actual transaction available" 오류가 납니다)
    // 로그아웃할 때 서버에 남아 있는 리프레시 토큰을 지워서, 그 토큰으로 새 액세스 토큰을 더는 못 받게 합니다.
    void deleteByUserId(Long userId);



}
