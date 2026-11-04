// ============================================================
// [파일 설명] users 테이블(회원)에 접근하는 리포지토리 인터페이스입니다.
// 이 프로젝트에서 직접 만든 메소드는 findByEmail 하나입니다.
//
// [호출하는 곳]
//   UserService             : 회원가입 시 userRepository.save() 호출
//   UserDetailService       : 로그인 시 findByEmail() 호출
// ============================================================
package com.shinsunyoung.blog.repository;

// User : 이 리포지토리가 다루는 회원 엔티티 클래스입니다.
import com.shinsunyoung.blog.domain.User;

// JpaRepository : 기본 저장·조회·삭제 기능을 제공하는 부모 인터페이스입니다.
import org.springframework.data.jpa.repository.JpaRepository;

// Optional : "값이 있을 수도 있고 없을 수도 있다"를 표현하는 상자입니다. null 대신 쓰는 안전한 자료형입니다.
import java.util.Optional;

// UserRepository 는 User 엔티티를 다루고, 기본키의 타입은 Long 입니다.
public interface UserRepository extends JpaRepository<User, Long> {

    // findByEmail : 이메일로 회원 1명을 찾는 메소드입니다.
    //
    // [메소드 이름 규칙 해석]
    //   find   : 조회한다
    //   By     : 조건을 시작한다
    //   Email  : User 엔티티의 email 필드를 기준으로 찾는다
    //   → 스프링 데이터 JPA 가 이 이름을 보고
    //     SELECT * FROM users WHERE email = ?  같은 SQL 을 자동으로 만들어 실행합니다.
    //
    // [매개변수] String email : 찾을 이메일 문자열입니다. (예: "user@gmail.com")
    // [반환값]   Optional<User>
    //   - 같은 이메일의 회원이 있으면 User 객체가 들어 있는 상자를 돌려줍니다.
    //   - 없으면 빈 상자(Optional.empty)를 돌려줍니다.
    //   호출하는 곳 : UserDetailService.loadUserByUsername()
    Optional<User> findByEmail(String email);



    // [참고] 이 아래는 쿼리 메소드 이름 규칙 설명입니다.
    //   이름 규칙으로 메소드를 만들면 SQL 을 직접 쓰지 않고 조회 기능을 만들 수 있습니다.
    //
    //   [기본 구조] findBy + 필드명 + 조건
    //     findByName(String name)              → WHERE name = ?
    //     findByNameAndAge(String, Integer)    → WHERE name = ? AND age = ?
    //     findByNameOrAge(String, Integer)     → WHERE name = ? OR age = ?
    //     findByAgeLessThan(Integer)           → WHERE age < ?      (30 이면 30은 포함 안 됨)
    //     findByAgeLessThanEqual(Integer)      → WHERE age <= ?
    //     findByAgeGreaterThan(Integer)        → WHERE age > ?
    //     findByAgeBetween(Integer, Integer)   → WHERE age BETWEEN ? AND ?
    //     findByNameIsNull()                   → WHERE name IS NULL  (NULL 과 빈 문자열 "" 은 다릅니다)
    //     findByNameContaining(String)         → WHERE name LIKE '%?%'
    //     findByNameStartingWith(String)       → WHERE name LIKE '?%'
    //     findByNameEndingWith(String)         → WHERE name LIKE '%?'
    //     findByNameOrderByAgeAsc(String)      → 이름이 같은 회원을 나이 오름차순으로 정렬
    //     existsByEmail(String)                → 이메일이 있으면 true, 없으면 false
    //     countByName(String)                  → 이름이 같은 회원 수를 숫자로 돌려줌
    //
    //   [반환 타입 고르기]
    //     Optional<User> : 회원 1명 조회
    //     List<User>     : 회원 여러 명 조회
    //     boolean        : 존재 여부 확인
    //     long           : 개수 확인
    //
    //   [중요] 메소드 이름에 쓰는 필드명은 엔티티에 실제로 있어야 합니다.
    //   User 엔티티에는 email, password, nickname 만 있고, name 이나 age 는 없습니다.
    //   그래서 위의 name, age 예시는 User 에서 그대로 쓸 수 없습니다.
    //   User 에서 쓸 수 있는 예: findByNickname(String), existsByEmail(String), findByEmailAndNickname(String, String)
}
