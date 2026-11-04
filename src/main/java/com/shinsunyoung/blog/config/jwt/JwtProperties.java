package com.shinsunyoung.blog.config.jwt;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter                          // [Lombok] application.yml에서 읽어온 값을 필드에 채워 넣기 위한 setIssuer(), setSecretKey() 메서드 자동 생성
@Getter                          // [Lombok] 다른 클래스(TokenProvider)가 저장된 설정값을 읽어갈 수 있도록 getIssuer(), getSecretKey() 메서드 자동 생성
@Component                       // [스프링 컨테이너 관리] 이 클래스를 스프링이 컨테이너에 직접 객체(Bean)로 만들어 관리하도록 등록합니다
@ConfigurationProperties("jwt")  // [설정 매핑] application.yml 파일 내의 "jwt:" 항목 아래에 적힌 자식 값들을 이 클래스 필드에 자동으로 1:1 매핑해 줍니다
public class JwtProperties {

    // 토큰 발급자 이메일 입니다. application.yml 의 jwt.issuer 값이 들어옵니다.
    // TokenProvider.makeToken() 에서 setIssuer() 로 토큰의 iss(등록된 클레임)에 기록됩니다.
    private String issuer;

    // JWT 토큰 발급 서명에 쓰는 비밀키입니다. application.yml 의 jwt.secret_key 값이 들어옵니다.
    // (secret_key → secretKey 는 스프링의 느슨한 바인딩 규칙으로 자동 연결됩니다.)
    // 토큰을 만들 때(signWith)와 검사할 때(setSigningKey) 반드시 같은 값을 써야 합니다.
    private String secretKey;

}
