package com.shinsunyoung.blog.domain;
// 파일 위치: src/main/java/me/shinsunyoung/springbootdeveloper/domain/RefreshToken.java
//
// ============================================================
// 이 파일이 하는 일
// ============================================================
// 로그인한 사용자의 "리프레시 토큰"을 데이터베이스에 저장하는 설계도입니다.
// 리프레시 토큰은 액세스 토큰이 만료되었을 때 새 액세스 토큰을 받기 위해 쓰는 토큰입니다.
// 이 클래스 하나가 데이터베이스의 표(테이블) 한 종류에 대응합니다.
//
// ============================================================
// 데이터베이스 표 모양 (이 클래스로 만들어지는 표)
// ============================================================
// 표(테이블) 이름: refresh_token
// ----------------------------------------------------------
//  id (기본키)  | user_id (사용자 번호) | refresh_token (토큰 문자열)
//  ------------+----------------------+---------------------------
//      1        |         5            |  eyJhbGciOi...
//      2        |         7            |  eyJhbGciOi...
// ----------------------------------------------------------
// 한 명의 사용자는 표에 한 줄만 가집니다. (user_id 는 중복될 수 없습니다.)
//

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// @NoArgsConstructor(access = AccessLevel.PROTECTED)
//   뜻: 값 없이 객체를 만드는 기본생성자를 자동으로 만듭니다. 단, 이 클래스 밖에서는 쓸 수 없게 막습니다(PROTECTED).
//   이유: JPA(데이터베이스 연결 도구)가 내부에서 객체를 만들 때 이 기본 생성자가 필요하기 때문입니다.
//         밖에서 아무 값 없이 만들면 데이터가 빈 채로 저장될 수 있어서 막아 둡니다.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // @Column(name = "id", updatable = false)
    //   name = "id"     : 표의 열 이름은 id 입니다.
    //   updatable = false: 한 번 정해진 id는 나중에 바꾸지 못합니다.
    @Column(name = "id", updatable = false  )
    private Long id;      //리플러시 토큰 행 정보를 구분할 id

    // @Column(name = "user_id", nullable = false, unique = true)
    //   name = "user_id"  : 표(테이블)의 열 이름은 user_id 입니다.
    //   nullable = false  : 빈칸(null)으로 두면 안 됩니다. 반드시 값이 있어야 합니다.
    //   unique = true     : 같은 값이 두 번 들어갈 수 없습니다.
    //                       즉, 한 사용자 번호(user_id열값)에 리프레시 토큰 줄이 두 개 생기지 않습니다.
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;   //리플러시 토큰을 가진 사용자의 번호(id)

    // @Column(name = "refresh_token", nullable = false)
    //   name = "refresh_token" : 표(테이블)의 열 이름은 refresh_token 입니다.
    //   nullable = false       : null 두면 안 됩니다.(리플러시 토큰 문자열 무조건 열에 저장되어야 한다)
    @Column(name = "refresh_token", nullable = false)
    private String refreshToken; //리플러시 토큰 문자열

    // ============================================================
    // 생성자: 사용자 번호(id)와 리프레시 토큰을 받아 새 객체를 만듭니다.
    // 누가 쓰나요: 사용자가 처음 로그인해서 리프레시 토큰이 처음 생길 때
    // 받는 값: userId(사용자 번호), refreshToken(토큰 문자열)
    // ============================================================
    public RefreshToken(Long userId, String refreshToken){
        this.userId = userId;            //사용자의 id 저장
        this.refreshToken = refreshToken;//리플러시 토큰 문자열 저장
    }
    // ============================================================
    // update: 기존 리프레시 토큰을 새 토큰으로 바꿉니다.
    // 누가 쓰나요: 같은 사용자가 다시 토큰을 받을 때 (새 줄을 만들지 않고 값만 바꿉니다)
    // 매개변수로 받는 값: newRefreshToken(새 토큰 문자열)
    // 돌려주는 값: 자기 자신(this). 바꾼 뒤 자기 자신 new RefreshToken(....) 객체를 그대로 돌려줍니다.
    // ============================================================
    public RefreshToken update(String newRefreshToken){

            //new RefreshToken(....) 자기 자신(this)객체에 refreshToken 변수에 새토큰 문자열 저장
            this.refreshToken = newRefreshToken;

            //자기 자신(this). 새토큰 문자열이 바뀐 뒤 자기 자신 new RefreshToken(....) 객체를 그대로 돌려줍니다.
            return this;
    }
}
