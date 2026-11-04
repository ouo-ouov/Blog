package com.shinsunyoung.blog.service;

import com.shinsunyoung.blog.config.jwt.TokenProvider;                    // [로그아웃 추가] 토큰에서 사용자 id 를 꺼내는 도구
import com.shinsunyoung.blog.domain.RefreshToken;
import com.shinsunyoung.blog.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;                  // [로그아웃 추가] 인증 정보(누가 로그인했는지) 객체
import org.springframework.security.core.context.SecurityContextHolder;   // [로그아웃 추가] 지금 요청의 인증 정보 보관함
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;          // [로그아웃 추가] 삭제를 하나의 트랜잭션으로 묶음

@RequiredArgsConstructor
@Service
public class RefreshTokenService {

    //리플러시 토큰을 DB의 RefreshToken테이블에서 조회할 리파지터리 객체 생성자 자동 주입
    private final RefreshTokenRepository refreshTokenRepository;

    // [로그아웃 추가] 액세스 토큰에서 사용자 id(claim "id")를 꺼낼 때 씁니다. (TokenProvider.getUserId)
    //   @RequiredArgsConstructor 가 만든 생성자로 스프링이 TokenProvider 빈을 넣어 줍니다.
    private final TokenProvider tokenProvider;

    //==== findByRefreshToken() : 요청한 리플러시 토큰 문자열을 매개변수로 받아  DB에서 조회한 토큰의 행을 조회해서 반환
    // 돌려주는 값: 조회한 토큰 행 정보가 저장된  RefreshToken 엔티티 객체 (그 안에 요청한 사용자 id 들어 있습니다)
    // 조회하지 못하면? "Unexpected token" 오류를 던집니다.
    public RefreshToken findByRefreshToken(String refreshToken){

        return refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(()-> new IllegalArgumentException("Unexpected token"));
    }

    // ═════════════════════════════════════════════════════════════════════════════════════════════
    // [로그아웃 추가] delete() : 지금 로그인한 사용자의 리프레시 토큰을 DB 에서 삭제합니다.
    // ═════════════════════════════════════════════════════════════════════════════════════════════
    // 누가 부르나? TokenApiController.deleteRefreshToken()  (DELETE /api/refresh-token, 로그아웃 링크 클릭 시)
    //
    // 전체 흐름 (요청 1번 안에서 순서대로):
    //   (1) 브라우저가 "Authorization: Bearer <액세스 토큰>" 머리글을 붙여 DELETE /api/refresh-token 요청
    //   (2) TokenAuthenticationFilter 가 토큰이 유효하면 tokenProvider.getAuthentication(token) 으로
    //       UsernamePasswordAuthenticationToken(principal=이메일 사용자, credentials=액세스 토큰 문자열) 을 만들어
    //       SecurityContextHolder 에 넣습니다.  ← 그래서 여기서 getCredentials() 로 토큰 문자열을 다시 꺼낼 수 있습니다.
    //   (3) 이 메소드가 그 토큰에서 사용자 id 를 꺼내고 → refresh_token 테이블의 그 사용자 행을 삭제합니다.
    //
    // @Transactional : 메소드 전체를 트랜잭션 하나로 묶습니다.
    //   deleteByUserId 는 "조회 → 삭제" 두 단계라 트랜잭션이 꼭 필요합니다. 메소드가 끝나면 커밋되면서 DELETE SQL 이 나갑니다.
    @Transactional
    public void delete() {
        // ① 지금 요청의 인증 정보 꺼내기
        //    SecurityContextHolder.getContext() : 이 요청(스레드)에 붙어 있는 보안 보관함
        //    .getAuthentication()               : TokenAuthenticationFilter 가 넣어 둔 인증 객체
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // ② 인증 객체의 credentials(자격 증명)에 들어 있는 "액세스 토큰 문자열" 꺼내기
        //    TokenProvider.getAuthentication() 에서 new UsernamePasswordAuthenticationToken(principal, token, authorities)
        //    처럼 두 번째 값으로 토큰을 넣었기 때문에 getCredentials() 가 토큰 문자열을 돌려줍니다.
        String token = authentication.getCredentials().toString();

        // ③ 토큰의 claim "id" 값(= users 테이블의 id)을 꺼냅니다.
        //    OAuth2SuccessHandler 가 토큰을 만들 때 .claim("id", user.getId()) 로 넣어 둔 값입니다.
        Long userId = tokenProvider.getUserId(token);

        // ④ refresh_token 테이블에서 이 사용자의 행을 삭제합니다.
        //    → SQL: select ... from refresh_token where user_id=?  →  delete from refresh_token where id=?
        //    이제 브라우저에 리프레시 토큰이 남아 있더라도 /api/token 으로 새 액세스 토큰을 받을 수 없습니다.
        //    (TokenService.createNewAccessToken → findByRefreshToken 에서 "Unexpected token" 오류)
        refreshTokenRepository.deleteByUserId(userId);
    }

}
