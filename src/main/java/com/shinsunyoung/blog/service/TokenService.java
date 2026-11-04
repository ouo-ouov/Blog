package com.shinsunyoung.blog.service;

import com.shinsunyoung.blog.config.jwt.TokenProvider;
import com.shinsunyoung.blog.domain.User;
import com.shinsunyoung.blog.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@RequiredArgsConstructor
@Service
public class TokenService {

    //JWT 토큰을 생성하고 확인하는 객체 생성자를 통한 자동 의존주입
    private final TokenProvider tokenProvider;

    //리플러시 토큰을 DB에서 조회명령할 서비스(부장)의 객체를 생성자를 통한 자동 주입
    private final RefreshTokenService refreshTokenService;

    //요청한 id로 사용자 정보를 조회 명령할 서비스(부장)의 객체 생성자를 통한 자동주입
    private final UserService userService;

    //createNewAccessToken() : 사용자가 요청한 리플러시 토큰을 매개변수로 받아 새 액세스 토큰(JWT 토큰)을 만들어 돌려주는 메소드.
    //매개변수로 전달 받는값 : 사용자가 보낸 리프레시 토큰 문자열
    //돌려 주는 값 : 새로 만든 액세스 토큰(JWT 토큰) 문자열 (2시간 동안 유효)
    public String createNewAccessToken(String refreshToken){

        //1단계 : 매개변수로 전달 받은 사용자가 요청할 리플러시 토큰 문자열이 유효한지 확인합니다.
        //유효하지 않는 리플러시 토큰 문자열이면?
        if(!tokenProvider.validToken(refreshToken)){
            throw new IllegalArgumentException("Unexpcted token");
        }
        //2단계 : 데이터베이스에서 이 리플러시 토큰을 where 조건값으로 사용해 리플러시 토큰이 저장됭 열의 행(레코드) 조회해서
        //       RefreshToken 엔티티 객체를 얻고, 이 객체의 사용자 id을 꺼냅니다.
        Long userId = refreshTokenService.findByRefreshToken(refreshToken).getUserId();

        //3단계 : 사용자 아이디(userId)로 사용자 정보를 DB에서 조회하기 위한 명령!
        //        조회한 사용자 정보가 보관된 User 엔티티 객체를 얻어 저장.
        User user = userService.findById(userId);

        //4단계 : 조회한 사용자를 위한 새 엑세스 토큰(JWT 토큰)문자열 새로 만듭니다.
        // Duration.ofHours(2) 는 "2시간"이라는 기간을 뜻합니다.
        // generateToken() 은 사용자 정보와 기간을 받아 토큰 문자열을 만들어 돌려줍니다.
        // 이 값이 createNewAccessToken() 의 결과가 되어 호출한 곳으로 돌아갑니다.
        return   tokenProvider.generateToken(user, Duration.ofHours(2));
    }


}
