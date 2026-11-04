// ============================================================
// 파일 경로
// src/main/java/me/shinsunyoung/springbootdeveloper/domain/User.java
// ============================================================
//
// [이 클래스의 역할]
//
// User 클래스는 블로그 회원 1명의 계정 정보(이메일, 비밀번호, 닉네임)를 담는 JPA 엔티티입니다.
// 데이터베이스의 users 테이블 한 줄과 1:1로 연결됩니다.
//
// 이 클래스를 사용하는 곳은 다음과 같습니다.
//
// 1. UserService.save()              : 회원가입할 때 User 객체를 만들어 DB에 저장합니다.
// 2. UserDetailService               : 로그인할 때 이메일로 찾은 User 객체를 시큐리티에 돌려줍니다.
// 3. DaoAuthenticationProvider       : 돌려받은 User 의 getPassword() 로 비밀번호를 비교합니다.
//
// [초보 수강생을 위한 핵심 개념 : implements UserDetails]
//
// 스프링 시큐리티는 로그인 처리를 할 때 "UserDetails" 라는 정해진 규격으로 회원 정보를 다룹니다.
// User 클래스가 UserDetails 인터페이스를 implements(구현)했기 때문에,
// User 객체 하나가 곧 시큐리티가 원하는 회원 정보 규격이 됩니다.
// 그래서 별도의 변환 없이 User 객체를 그대로 돌려줄 수 있습니다.

package com.shinsunyoung.blog.domain;


// [1. JPA 관련 import]
//
// jakarta.persistence.* 는 @Entity, @Table, @Id, @GeneratedValue, @Column 등
// 데이터베이스 연결에 쓰는 어노테이션을 한꺼번에 가져옵니다.
import jakarta.persistence.*;


// [2. Lombok 관련 import]
//
// AccessLevel       : 생성자의 접근 범위(누가 new 로 만들 수 있는지)를 정합니다.
// @Builder          : 값을 하나씩 골라 넣어 객체를 만드는 빌더 메서드를 자동으로 만듭니다.
// @Getter           : 모든 필드의 값을 읽는 getXxx() 메서드를 자동으로 만듭니다.
// @NoArgsConstructor: 값을 받지 않는 기본 생성자를 자동으로 만듭니다.
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


// [3. 스프링 시큐리티 관련 import]
//
// GrantedAuthority        : "권한 1개"를 나타내는 시큐리티 규격입니다.
// SimpleGrantedAuthority  : 권한 이름(문자열)으로 GrantedAuthority 객체를 만드는 구현 클래스입니다.
// UserDetails             : 시큐리티가 요구하는 회원 정보 규격(인터페이스)입니다. 이 클래스가 구현하는 대상입니다.
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;


// [4. 자바 기본 자료형 관련 import]
//
// Collection : 여러 값을 담는 자료 구조의 공통 규격입니다. getAuthorities() 의 반환 타입으로 씁니다.
// List       : 순서가 있는 목록입니다. List.of(...) 로 값 목록을 만듭니다.
import java.util.Collection;
import java.util.List;


// ============================================================
// [5. 클래스에 붙은 어노테이션]
// ============================================================

// @Table(name = "users")
// - 이 엔티티를 DB 의 "users" 테이블과 연결합니다.
// - 테이블 이름을 "user" 로 두면 SQL 에서 예약어와 겹쳐 오류가 날 수 있어서 users 를 사용합니다.

// @NoArgsConstructor(access = AccessLevel.PROTECTED)
// - 값이 없는 기본 생성자를 만듭니다.
// - JPA 는 DB 에서 꺼낸 값을 객체에 담을 때 이 생성자를 사용합니다.
// - PROTECTED 이므로 다른 클래스에서 new User() 로 빈 객체를 만드는 것은 막습니다.

// @Getter
// - getId(), getEmail(), getPassword(), getNickname() 같은 메서드를 자동으로 만듭니다.

// @Entity
// - 이 클래스가 DB 테이블과 연결되는 JPA 엔티티라는 표시입니다.
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class User implements UserDetails {


    // ============================================================
    // [6. 필드(테이블의 컬럼) 선언]
    // ============================================================

    // [6-1. id 컬럼 : 기본키(Primary Key)]
    // @Id
    // - 이 필드가 기본키라는 표시입니다. 회원 한 명을 구별하는 고유 번호입니다.
    //
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    // - 번호를 개발자가 직접 넣지 않고, DB 가 1, 2, 3... 순서로 자동 증가시킵니다.
    //
    // @Column(name = "id", updatable = false)
    // - DB 컬럼 이름은 id 입니다.
    // - updatable = false : 한 번 저장된 번호는 나중에 수정할 수 없습니다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    // [6-2. email 컬럼 : 로그인 아이디]
    // @Column(name = "email", nullable = false, unique = true)
    // - DB 컬럼 이름은 email 입니다.
    // - nullable = false : 반드시 값이 있어야 합니다. 비워 두고 저장할 수 없습니다.
    // - unique = true    : 같은 이메일로는 두 번 가입할 수 없습니다.
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    // [6-3. password 컬럼 : 암호화된 비밀번호]
    // @Column(name = "password")
    // - DB 에는 원래 비밀번호가 아니라 BCrypt 로 암호화된 문자열이 저장됩니다.
    // - 암호화는 UserService.save() 에서 passwordEncoder.encode() 로 한 뒤 이 필드에 넣습니다.
    // - nullable 옵션을 쓰지 않았으므로 비워 둘 수 있습니다. (소셜 로그인 회원은 비밀번호가 없을 수 있어서입니다)
    @Column(name = "password")
    private String password;

    // [6-4. nickname 컬럼 : 화면에 보여 줄 이름]
    // @Column(name = "nickname", unique = true)
    // - DB 컬럼 이름은 nickname 입니다.
    // - unique = true : 같은 닉네임은 쓸 수 없습니다. (값이 비어 있는 경우는 여러 명이 가능합니다)
    @Column(name = "nickname", unique = true)
    private String nickname;


    // ============================================================
    // [7. 생성자 : 회원 객체 만들기]
    // ============================================================

    // [@Builder 생성자]
    // - User.builder().email(...).password(...).nickname(...).build() 형태로 객체를 만들 때 실행됩니다.
    // - 호출하는 곳 : UserService.save() 의 User.builder() 체인
    //
    // 매개변수
    //   email    : 회원 이메일
    //   password : 이미 암호화된 비밀번호 (평문이 아닙니다)
    //   nickname : 회원 닉네임
    @Builder
    public User(String email, String password, String nickname) {

        // this.email = email;
        // - 매개변수 email 값을 이 객체의 email 필드에 넣습니다. this 는 "지금 만들고 있는 이 객체 자신"입니다.
        this.email = email;

        // this.password = password;
        // - 이미 암호화된 비밀번호를 password 필드에 넣습니다.
        this.password = password;

        // this.nickname = nickname;
        // - [수정 내용] 예전에는 이 줄이 주석 처리되어 있어서 닉네임이 항상 null 로 저장되었습니다.
        // - 지금은 주석을 풀어서, 전달받은 닉네임이 실제로 저장되도록 고쳤습니다.
        this.nickname = nickname;
    }


    // ============================================================
    // [8. UserDetails 인터페이스가 요구하는 메서드들]
    // ============================================================
    //
    // 스프링 시큐리티는 로그인과 권한 검사를 할 때 아래 메서드들을 호출해서 회원 상태를 확인합니다.
    // 우리 프로젝트에서 직접 필요한 것은 getPassword() 와 getUsername() 이고,
    // 나머지는 "계정 만료, 잠김, 비활성" 기능을 쓰지 않으므로 항상 정상(true)을 돌려줍니다.

    // [8-1. getAuthorities : 회원의 권한 목록을 돌려줍니다.]
    // - 호출하는 곳 : 스프링 시큐리티 내부 (로그인 성공 후 권한 정보를 만들 때)
    // - 반환 타입 Collection<? extends GrantedAuthority> : 권한 객체들의 목록입니다.
    // - List.of(new SimpleGrantedAuthority("user")) : 권한이 "user" 1개인 목록을 만들어 돌려줍니다.
    // - 주의: 권한 이름은 "user" 이며 "ROLE_USER" 가 아닙니다.
    //   그래서 hasRole("USER") 로는 검사되지 않고, hasAuthority("user") 로 검사해야 합니다.
    //   현재 WebSecurityConfig 는 권한 이름으로 나누지 않고 "로그인했는가"만 검사하므로 문제는 없습니다.
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("user"));
    }

    // [8-2. getUsername : 로그인 아이디를 돌려줍니다.]
    // - 이 프로젝트는 이메일을 로그인 아이디로 쓰므로 email 필드 값을 돌려줍니다.
    @Override
    public String getUsername() {
        return email;
    }

    // [8-3. getPassword : 비교에 쓸 암호문을 돌려줍니다.]
    // - DaoAuthenticationProvider 가 이 메서드로 DB 에 저장된 암호문을 꺼냅니다.
    // - 그다음 passwordEncoder.matches(입력한 비밀번호, 이 암호문) 으로 두 값이 같은지 비교합니다.
    @Override
    public String getPassword() {
        return password;
    }

    // [8-4. isAccountNonExpired : 계정 기간이 만료되지 않았는지]
    // - true : 만료되지 않음. 이 프로젝트는 계정 기간 제한이 없으므로 항상 true 입니다.
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // [8-5. isAccountNonLocked : 계정이 잠기지 않았는지]
    // - true : 잠기지 않음. 이 프로젝트는 비밀번호 여러 번 틀림으로 잠그는 기능이 없으므로 항상 true 입니다.
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    // [8-6. isCredentialsNonExpired : 비밀번호 사용 기간이 만료되지 않았는지]
    // - true : 만료되지 않음. 비밀번호 주기적 변경 기능이 없으므로 항상 true 입니다.
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // [8-7. isEnabled : 계정이 사용 가능한 상태인지]
    // - true : 사용 가능. 이메일 인증 같은 활성화 절차가 없으므로 항상 true 입니다.
    @Override
    public boolean isEnabled() {
        return true;
    }


    // ============================================================
    // [9. 업무 메서드 : 닉네임 변경]
    // ============================================================

    // update(String nickname)
    // - 닉네임을 새 값으로 바꾸고, 바뀐 자기 자신(this)을 돌려줍니다.
    // - 현재 프로젝트에서는 아직 호출하는 곳이 없습니다. 닉네임 변경 기능을 만들 때 사용할 수 있습니다.
    public User update(String nickname) {
        this.nickname = nickname;   // 닉네임 필드 값을 새 값으로 바꿉니다.
        return this;                // 바뀐 자기 자신을 돌려줍니다. (메서드를 이어서 쓸 수 있게 하기 위해서입니다)
    }
}
