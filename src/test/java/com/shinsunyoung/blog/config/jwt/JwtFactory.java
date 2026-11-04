package com.shinsunyoung.blog.config.jwt;

// 파일 위치: 테스트용 파일이라 보통 src/test/java 폴더 안에 둡니다.
//            (실제 위치는 프로젝트에서 한 번 확인해 주세요.)
//
// ============================================================
// 이 파일이 하는 일
// ============================================================
// 테스트 때 쓸 "로그인 토큰"을 쉽게 만들어 주는 파일입니다.
// 실제로 로그인하지 않아도 토큰을 만들 수 있어서 테스트가 편해집니다.
//
// ============================================================
// 사용 순서
// ============================================================
// 1. JwtFactory.withDefaultValues() 를 부릅니다.
//    → 기본값이 모두 들어 있는 JwtFactory 객체가 만들어집니다.
// 2. 특정 값만 바꾸고 싶으면 JwtFactory.builder() 를 씁니다.
// 3. createToken(jwtProperties) 를 부르면 토큰 문자열이 만들어집니다.
//
// ============================================================
// 꼭 알아둘 낱말
// ============================================================
// 토큰          : 로그인 후 사용자에게 주는 긴 문자열입니다.
// 변수(필드)    : 값을 저장해 두는 곳입니다. 이름을 붙여서 씁니다.
// 메서드        : 어떤 일을 하는 묶음입니다. 이름 뒤에 ()가 붙습니다.
// 생성자        : 객체를 처음 만들 때 한 번 실행되는 메서드입니다.
// null          : "값이 없다"는 뜻입니다.
// 기본값        : 값을 주지 않았을 때 자동으로 쓰는 값입니다.
// 빌더          : 값을 하나씩 골라 넣고 마지막에 build()로 완성하는 방법입니다.
// 어노테이션(@) : 클래스나 메서드 위에 붙여서 자동 기능을 켜는 표시입니다.
// subject       : 토큰의 주인입니다. 여기서는 이메일입니다.
// issuer        : 토큰을 만든 곳입니다.
// 만료 시각     : 이 시각이 지나면 토큰을 쓸 수 없습니다.
// claims        : 토큰에 추가로 넣고 싶은 값들의 모음입니다.
// ============================================================



import io.jsonwebtoken.Header;                       // 토큰 머리 부분에 넣을 "항목 이름"과 "값"이 들어 있는 도구입니다.
import io.jsonwebtoken.Jwts;                         // 토큰 만들기를 시작하는 도구입니다.
import io.jsonwebtoken.SignatureAlgorithm;           // 서명을 만드는 방법(HS256 등)을 고르는 도구입니다.
import lombok.Builder;                               // 값을 골라 넣는 빌더 기능을 자동으로 만들어 주는 도구입니다.
import lombok.Getter;                                // 변수 값을 꺼내는 메서드(get○○)를 자동으로 만들어 주는 도구입니다.

import java.time.Duration;                           // "14일", "2시간"처럼 기간을 나타내는 클래스입니다.
import java.util.Date;                               // 날짜와 시각을 나타내는 클래스입니다.
import java.util.Map;                                // "이름과 값"을 짝지어 모아 두는 클래스입니다.

import static java.util.Collections.emptyMap;        // 아무것도 없는 빈 Map을 만드는 기능을 가져옵니다. 아래에서 emptyMap()으로 바로 씁니다.

// @Getter : 아래 변수들의 값을 밖에서 꺼낼 수 있도록 get○○() 메서드를 자동으로 만듭니다.
@Getter
public class JwtFactory { // 이 클래스는 JWT 토큰을 만들 때 필요한 값들을 모아 두었다가 토큰을 만들어 줍니다.

    // ------------------------------------------------------------
    // 변수 (값을 저장하는 곳)
    // ------------------------------------------------------------

    // 토큰 주인(이메일)을 저장합니다.
    // 값을 따로 주지 않으면 "test@email.com"이 기본으로 들어갑니다.
    private String subject = "test@email.com";

    // 토큰을 만든 시각(발급 시각)을 저장합니다.
    // 기본값은 이 객체가 만들어진 지금 시각입니다.
    private Date issuedAt = new Date();

    // 토큰이 만료되는 시각을 저장합니다.
    // 기본값은 "지금부터 14일 뒤"입니다. 계산 순서는 다음과 같습니다.
    //   1) 지금 시각을 숫자로 바꿉니다. (밀리초 단위, 밀리초는 1000분의 1초)
    //   2) 14일을 같은 단위의 숫자로 바꿉니다.
    //   3) 두 숫자를 더합니다.
    //   4) 더한 숫자를 다시 날짜(Date)로 바꿉니다.
    private Date expiration = new Date(new Date().getTime() + Duration.ofDays(14).toMillis());

    // 토큰에 추가로 넣고 싶은 값들의 모음입니다.
    // 기본값은 "아무것도 없는 빈 모음"입니다.
    private Map<String, Object> claims = emptyMap();

    // ------------------------------------------------------------
    // 생성자 (객체를 만들 때 맨 처음 실행됩니다)
    // ------------------------------------------------------------

    // @Builder : 이 줄 덕분에 JwtFactory.builder() 를 쓸 수 있습니다.
    //            값을 하나씩 골라서 넣고 마지막에 build() 를 부르는 방식입니다.
    @Builder
    public JwtFactory(String subject, Date issuedAt, Date expiration,
                      Map<String, Object> claims) {
        // 이 생성자는 밖에서 받은 값을 변수에 옮겨 담습니다.
        // 값이 없으면(null) 위에서 정한 기본값을 그대로 둡니다.
        // 아래 한 줄의 뜻: subject 값이 null 이 아니면 그 값을 쓰고, null 이면 기존 값을 그대로 씁니다.
        this.subject = subject != null ? subject : this.subject;

        // 발급 시각도 같은 방식입니다. 값이 있으면 넣고, 없으면 기본값을 씁니다.
        this.issuedAt = issuedAt != null ? issuedAt : this.issuedAt;

        // 만료 시각도 같은 방식입니다.
        this.expiration = expiration != null ? expiration : this.expiration;

        // 추가 값 모음도 같은 방식입니다. 값이 없으면 빈 모음을 그대로 씁니다.
        this.claims = claims != null ? claims : this.claims;
    }

    // ------------------------------------------------------------
    // 기본값으로 객체 만들기
    // ------------------------------------------------------------

    // 모든 값을 기본값으로 둔 JwtFactory 객체를 만들어서 돌려줍니다.
    // static 이라서 객체를 먼저 만들지 않고 JwtFactory.withDefaultValues() 로 바로 부를 수 있습니다.
    public static JwtFactory withDefaultValues() {
        // JwtFactory.builder()  : 값을 고르는 도구를 꺼냅니다.
        // .build()              : 아무 값도 고르지 않고 객체를 완성합니다.
        //                         값을 주지 않았으므로 위에서 정한 기본값이 그대로 쓰입니다.
        // return                : 완성된 객체를 부른 곳으로 돌려줍니다.
        return JwtFactory.builder().build();
    }

    // ------------------------------------------------------------
    // 토큰 만들기
    // ------------------------------------------------------------

    // 지금까지 정한 값들로 실제 토큰 문자열을 만들어 돌려줍니다.
    // 받는 값 : jwtProperties (발급자 이름과 비밀키가 들어 있는 설정 객체)
    // 돌려주는 값 : 토큰 문자열 (예: eyJhbGciOi... 같은 긴 글자)
    public String createToken(JwtProperties jwtProperties) {

        // 아래 줄들은 토큰의 항목을 하나씩 차례로 채우는 코드입니다.
        // 각 줄 끝의 점(.)은 "다음 항목을 이어서 적겠다"는 뜻입니다.
        // 마지막 compact() 에서 모든 항목이 합쳐져 토큰 문자열이 됩니다.
        return Jwts.builder()

                // 토큰 주인(sub 항목)을 넣습니다. 값은 이 객체의 subject 변수입니다.
                .setSubject(subject)

                // 헤더에 "이 토큰은 JWT 형식이다"라는 표시를 넣습니다.
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)

                // 토큰을 만든 곳(iss 항목)을 넣습니다. 값은 설정 파일의 발급자 이름입니다.
                .setIssuer(jwtProperties.getIssuer())

                // 발급 시각(iat 항목)을 넣습니다. 값은 이 객체의 issuedAt 변수입니다.
                .setIssuedAt(issuedAt)

                // 만료 시각(exp 항목)을 넣습니다. 이 시각이 지나면 토큰을 쓸 수 없습니다.
                .setExpiration(expiration)

                // 추가 값들을 토큰에 한꺼번에 넣습니다.
                // 추가 값이 없으면(빈 모음이면) 아무것도 더해지지 않습니다.
                .addClaims(claims)

                // 토큰이 바뀌지 않았는지 확인할 수 있도록 서명(도장 같은 확인 값)을 붙입니다.
                // HS256 은 서명을 만드는 방법의 이름입니다.
                // 비밀키는 설정 파일에서 읽어 온 값입니다.
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecretKey())

                // 지금까지 넣은 항목을 모두 합쳐 토큰 문자열 하나로 만듭니다.
                // 완성된 문자열을 이 메서드의 결과로 돌려줍니다.
                .compact();
    }
}

