package com.shinsunyoung.blog.config.jwt;


import com.shinsunyoung.blog.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

// @RequiredArgsConstructor
//   뜻: 아래에 있는 final 변수(jwtProperties)를 받는 생성자를 자동으로 만들어 줍니다.
//   이유: 스프링이 설정값 객체를 이 생성자를 통해 넣어 주기 때문입니다.
@RequiredArgsConstructor
// @Service
//   뜻: 이 클래스의 객체를 스프링이 하나 만들어 관리하게 합니다.
//   이유: 다른 클래스들이 이 객체를 직접 만들지 않고 받아서 쓸 수 있게 하기 위해서입니다.
@Service
public class TokenProvider {

    // JWT 토큰 생성을 위한 설정값(발급자 이메일, 비밀키)을 담아 두는 변수입니다.
    // 값은 application.yml 파일의 jwt: 항목에서 읽어 옵니다.
    // 비밀키를 코드 안에 직접 쓰지 않기 위해 설정 파일에 둡니다.
    private final JwtProperties jwtProperties;

    // ============================================================
    // [1] 토큰 만들기: generateToken(...) 메소드
    // 누가 부르나요: TokenService.createNewAccessToken() (액세스 토큰 재발급, 2시간)
    //               ※ 11장에서는 OAuth2 로그인 성공 처리기(OAuth2SuccessHandler)도 이 메서드를 부릅니다.
    // 받는 값: user(토큰의 주인), expiredAt(토큰이 유효한 기간)
    // 돌려주는 값: 완성된 토큰 문자열
    // ============================================================
    public String generateToken(User user, Duration expiredAt) {
        // 지금 이 순간의 날짜와 시각을 만듭니다.
        // 토큰 만료 시각을 계산할 때 기준이 되는 현재 시각입니다.
        Date now = new Date();

        // 토큰 만료 시각을 계산해서 makeToken 에 넘깁니다.
        // 계산 순서:
        //   1) now.getTime() 은 지금 시각을 숫자로 바꿉니다. (밀리초 단위, 1000분의 1초)
        //   2) expiredAt.toMillis() 는 유효 기간을 같은 단위의 숫자로 바꿉니다.
        //   3) 두 숫자를 더하면 "지금부터 유효 기간이 지난 시각"의 숫자가 됩니다.
        //   4) new Date(숫자) 는 그 숫자를 날짜로 바꿉니다. 이것이 만료 시각입니다.
        // makeToken 이 만든 토큰을 그대로 돌려줍니다.
        return makeToken(new Date(now.getTime() + expiredAt.toMillis()), user); // 만료 시각 계산 후 makeToken 호출
    }

    // ============================================================
    // [2] 토큰 조립하기: makeToken (이 파일 안에서만 씁니다)
    // 누가 부르나요: generateToken()
    // 받는 값: expiry(토큰 만료 시각), user(토큰의 주인)
    // 돌려주는 값: 토큰 문자열
    // ============================================================
    private String makeToken(Date expiry, User user) {

        // 토큰 발급 시각으로 쓸 현재 시각을 다시 만듭니다.
        Date now = new Date();

        // 토큰을 한 줄씩 조립합니다.
        // 각 줄 끝의 점(.)은 "다음 항목을 이어서 넣겠다"는 뜻입니다.
        // 마지막 compact() 가 모든 항목을 합쳐서 토큰 문자열 하나로 만듭니다.
        return Jwts.builder()

                // [헤더 typ] 헤더에 "이 토큰은 JWT 형식이다"라는 표시(typ: JWT)를 넣습니다.
                // alg(HS256)는 아래 signWith() 가 헤더에 자동으로 넣어 줍니다.
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)

                // [내용 iss · 등록된 클레임] 토큰을 만든 곳(발급자 이메일)을 넣습니다. 값은 설정 파일의 jwt.issuer 입니다.
                .setIssuer(jwtProperties.getIssuer())

                // [내용 iat · 등록된 클레임] 토큰을 만든 시각(발급 시각)을 넣습니다.
                .setIssuedAt(now)

                // [내용 exp · 등록된 클레임] 토큰의 만료 시각을 넣습니다. 이 시각이 지나면 토큰을 쓸 수 없습니다.
                .setExpiration(expiry)

                // [내용 sub · 등록된 클레임] 토큰의 주인(사용자 이메일)을 넣습니다.
                // getAuthentication() 에서 claims.getSubject() 로 이 이메일을 다시 꺼냅니다.
                .setSubject(user.getEmail())

                // [내용 id · 비공개 클레임] 사용자 번호를 "id"라는 이름으로 넣습니다.
                // 표준에 없는 이름이라 우리 서버와 클라이언트끼리만 뜻을 아는 "비공개 클레임"입니다.
                // 나중에 getUserId() 로 이 번호를 꺼냅니다.
                .claim("id", user.getId())

                // [서명] 비밀키와 HS256(HMAC + SHA-256) 방식으로 서명을 만듭니다.
                // 서명 = HS256( 헤더를 Base64로 바꾼 값 + "." + 내용을 Base64로 바꾼 값 , 비밀키 )
                // 서명 덕분에 토큰이 바뀌었는지 알 수 있습니다.
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecretKey())

                // 지금까지 넣은 항목을 모두 합쳐서 JWT 토큰 문자열 하나로 만듭니다.
                .compact();
    }

    // ============================================================
    // [3] JWT 토큰이 진짜인지(유효한 토큰인지) 확인하기: validToken
    // 누가 부르나요: 요청이 올 때마다 (TokenAuthenticationFilter), TokenService
    // 받는 값: token(요청으로 받은 토큰 문자열)
    // 돌려주는 값: 진짜이고 토큰 유효기간이 남았으면 true, 아니면 false
    // ============================================================
    public boolean validToken(String token) {

        // try 안의 코드는 문제가 생길 수 있으니 "한번 해 봐라"라는 뜻입니다.
        // 문제가 생기면 아래 catch 로 넘어갑니다.
        try {

            //  JWT 토큰 문자열을 읽는 JwtParser객체 도구를 준비합니다.
            Jwts.parser()

                    // 서명을 확인할 때 쓸 비밀키를 알려 줍니다.
                    // JWT 토큰을 만들 때 쓴 비밀키와 같은 값이어야 확인이 됩니다.
                    .setSigningKey(jwtProperties.getSecretKey())

                    // JWT 토큰을 실제로 읽으면서 두 가지를 확인합니다.
                    //   1) 서명이 맞는가? (토큰이 바뀌지 않았는가?)
                    //   2) 토큰 만료 시각이 지나지 않았는가? (기간이 남았는가?)
                    // 둘 중 하나라도 틀리면 예외가 생기고, 아래 return true 는 실행되지 않습니다.
                    .parseClaimsJws(token);

            // 예외 없이 여기까지 왔다면 두 가지 확인을 모두 통과한 것입니다.
            return true;

        } catch (Exception e) {
            // 예외가 생기면 이 줄이 실행됩니다.
            // 토큰이 바뀌었거나, 기간이 지났거나, 형식이 깨졌을 때 여기로 옵니다.
            // 이 코드는 실패 이유를 나누지 않고 모두 false 로 처리합니다.
            return false;
        }
    }

    // ============================================================
    // [4] 스프링이 알아보는 "인증 정보" 만들기: getAuthentication
    // 누가 부르나요: TokenAuthenticationFilter (토큰을 확인한 뒤에만 부릅니다)
    // 받는 값: token(확인이 끝난 토큰 문자열)
    // 돌려주는 값: 스프링 시큐리티의 인증 정보 Authentication 객체
    // 주의: 이 메서드는 토큰을 다시 확인하지 않습니다. validToken() 을 먼저 부르세요.
    // ============================================================
    public Authentication getAuthentication(String token) {

        // 토큰 내용을 꺼냅니다. 아래에서 이메일을 꺼내기 위해 씁니다.
        Claims claims = getClaims(token);

        // 권한 목록을 만듭니다.
        // 이 프로젝트의 모든 사용자는 ROLE_USER 권한 하나를 가집니다.
        // ROLE_ 로 시작하는 이름은 스프링 시큐리티가 정한 규칙입니다.
        Set<SimpleGrantedAuthority> authorities =
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));

        // 스프링 시큐리티가 사용자를 나타낼 때 쓰는 객체를 만듭니다.
        // 주의: 이것은 우리 프로젝트의 User 가 아니라 스프링 시큐리티의 User 입니다.
        // 값은 세 가지입니다.
        //   첫째(이름): 토큰 안의 이메일입니다. claims.getSubject() 가 꺼내 줍니다.
        //   둘째(비밀번호): 빈 문자열("")입니다. 토큰으로 이미 확인했으므로 비밀번호는 필요 없습니다.
        //   셋째(권한): 위에서 만든 ROLE_USER 목록입니다.
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(claims.getSubject(), "", authorities);

        // 인증 정보 객체를 만들어 돌려줍니다.
        // 값은 세 가지입니다.
        //   첫째(누구인가): 위에서 만든 사용자 객체
        //   둘째(인증 수단): 토큰 문자열
        //   셋째(권한): ROLE_USER 목록
        // 이 객체는 TokenAuthenticationFilter 가 받아서 스프링에 저장합니다.
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    // ============================================================
    // [5] JWT 토큰 기반으로 사용자 id 꺼내기: getUserId
    // 누가 부르나요: 토큰을 확인한 뒤, 사용자 id가 필요할 때
    // 받는 값: token(확인이 끝난 토큰 문자열)
    // 돌려주는 값: 사용자 id(Long 타입의 숫자)
    // ============================================================
    public Long getUserId(String token) {

        // 토큰 내용을 꺼냅니다.
        Claims claims = getClaims(token);

        // 토큰 내용에서 이름이 "id"인 값을 찾아 숫자(Long)로 꺼냅니다.
        // 토큰을 만들 때 .claim("id", user.getId()) 로 넣어 둔 값입니다.
        // 값이 없으면 null 이 나올 수 있습니다.
        return claims.get("id", Long.class);
    }



    // ============================================================
    // [보조] getClaims: 토큰 내용만 꺼내는 메서드 (이 파일 안에서만 씁니다)
    // 누가 부르나요: getAuthentication(), getUserId()
    // 받는 값: token(토큰 문자열)
    // 돌려주는 값: 토큰 내용(Claims)
    // 주의: 이 메서드도 서명을 확인합니다. 토큰이 바뀌었으면 여기서 예외가 생깁니다.
    // ============================================================
    private Claims getClaims(String token) {
        return Jwts.parser()

                // 서명을 확인할 비밀키를 알려 줍니다.
                .setSigningKey(jwtProperties.getSecretKey())

                // JWT 토큰을 읽으면서 서명과 기간을 확인합니다.
                // 확인에 실패하면 예외가 생기고, 아래 .getBody() 는 실행되지 않습니다.
                .parseClaimsJws(token)

                // 확인을 통과한 JWT 토큰에서 "내용" 부분만 꺼냅니다.
                // 자바에서는 이 내용을 body 라고 부릅니다.
                .getBody();
    }




}
