package com.shinsunyoung.blog.controller;


import com.shinsunyoung.blog.dto.CreateAccessTokenRequest;
import com.shinsunyoung.blog.dto.CreateAccessTokenResponse;
import com.shinsunyoung.blog.config.oauth.OAuth2SuccessHandler;   // [로그아웃 추가] 쿠키 이름 상수(REFRESH_TOKEN_COOKIE_NAME)
import com.shinsunyoung.blog.service.RefreshTokenService;         // [로그아웃 추가] 리프레시 토큰 삭제 서비스
import com.shinsunyoung.blog.service.TokenService;
import com.shinsunyoung.blog.util.CookieUtil;                     // [로그아웃 추가] 쿠키 삭제 도구
import jakarta.servlet.http.HttpServletRequest;                   // [로그아웃 추가]
import jakarta.servlet.http.HttpServletResponse;                  // [로그아웃 추가]
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;     // [로그아웃 추가] DELETE 요청 연결
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// @RequiredArgsConstructor
//   뜻: 아래 final 변수(tokenService)를 받는 생성자를 자동으로 만들어 줍니다.
//   이유: 스프링이 TokenService 객체를 이 생성자를 통해 넣어 주기 위해서입니다.
@RequiredArgsConstructor
@RestController
//@RestController 이유: 메서드가 리턴 값을 화면VIEW 파일이 아니라 데이터(JSON) 형태로 응답에 담아 보내게 하기 위해서입니다.
public class TokenApiController {

    //JWT 액세스 토큰 만들기 업무를 맡은 서비스 생성자에 의한 의존주입
    private final TokenService tokenService;

    // [로그아웃 추가] 로그아웃 때 DB 의 리프레시 토큰을 지우는 서비스 (생성자 자동 주입)
    private final RefreshTokenService refreshTokenService;

    //@PostMapping("/api/token")
    //   뜻: POST 방식(POST메소드)으로 "/api/token" 주소에 요청이 오면 아래 메서드가 실행됩니다.
    //   POST 는 새 데이터를 만들어 달라는 요청 방식입니다. 여기서는 새 액세스 토큰을 만듭니다.
    //메소드 기능 =>  새 엑세스 토큰(JWT 토큰) 생성 요청을 받아 새 엑세스 토큰을 생성해서 브라우저로 JSON 데이터로 응답.
    // ============================================================
    // 실제 요청 모양 (이 객체로 바뀌는 데이터)
    // ============================================================
    // 사용자가 아래와 같은 내용을 보내면, 스프링이 자동으로 이 객체에 옮겨 담습니다.
    //
    //   {
    //     "refreshToken": "abc" <=== 요청한 리플러시 토큰
    //   }
    @PostMapping("/api/token")
    public ResponseEntity<CreateAccessTokenResponse>  createNewAccessToken(
                                                        // @RequestBody
                                                        //   뜻: 요청 본문(JSON)을 CreateAccessTokenRequest dto객체로 자동으로 바꿔서 넣어 줍니다.
                                                        //   예) {"refreshToken": "abc"} → request.getRefreshToken() 이 "abc" 를 돌려줍니다.
                                                        @RequestBody CreateAccessTokenRequest request){
        //[1단계] TokenService 부장 에게 리플시 토큰을 전달 하면서  새 액세스 토큰 만들기를 맡깁니다.
        // request.getRefreshToken() : 요청에서 받은 리프레시 토큰 문자열을 꺼냅니다.
        // tokenService.createNewAccessToken(...) : 그 토큰을 확인하고 새 액세스 토큰을 만들어 돌려줍니다.
        String newAccessToken = tokenService.createNewAccessToken(request.getRefreshToken());
        System.out.println("새로 생성된 엑세스 토큰 : " + newAccessToken);

        //[2단계] 새로 생성된 엑세스 토큰(JWT 토큰)을 ResponseEntity에 보관하고, 응답 상태 코드를 201(생성 성공)으로 보관후
        //       브라우저로 ResponseEntity를 전달하면 브라우저는 이정보를 HttpResponse 응답 메세지로 만들어서 JSON데이터로 변환후 브라우저에 출력 합니다.
        //요약 : 새로 생성된 액세스 토큰의 JSON 문자열 브라우저로 출력(응답)
        return ResponseEntity.status(201).body(new CreateAccessTokenResponse(newAccessToken) );
    }

    // ═════════════════════════════════════════════════════════════════════════════════════════════
    // [로그아웃 추가] DELETE /api/refresh-token : 로그아웃 요청을 받아 리프레시 토큰을 삭제합니다.
    // ═════════════════════════════════════════════════════════════════════════════════════════════
    // 누가 부르나? articleList.html 상단 [로그아웃] 링크 → article.js 의 logoutButton 클릭 처리
    //              → httpRequest('DELETE', '/api/refresh-token', ...)
    //
    // 보안 검사 : WebOAuthSecurityConfig 에서 "/api/**" 는 authenticated() 입니다.
    //   → 유효한 액세스 토큰이 없으면 이 메소드까지 오지 못하고 401 이 응답됩니다.
    //   → article.js 의 httpRequest 가 401 을 받으면 refresh_token 쿠키로 새 액세스 토큰을 받아 다시 요청합니다.
    //
    // 하는 일:
    //   (1) refreshTokenService.delete()          : DB(refresh_token 테이블)에서 이 사용자의 리프레시 토큰 삭제
    //   (2) CookieUtil.deleteCookie(...)           : 브라우저의 refresh_token 쿠키도 서버가 직접 지우라고 응답에 담음
    //       (Set-Cookie: refresh_token=; Max-Age=0; Path=/  ← 브라우저가 이 머리글을 받으면 쿠키를 지웁니다)
    //       자바스크립트(deleteCookie)로도 지우지만, 서버에서도 지워 두면 확실합니다.
    //   (3) 200(OK) 응답 → article.js 의 success() 가 access_token 을 지우고 /login 으로 이동
    //
    // HttpServletRequest  : 요청에 들어 있는 쿠키 목록을 읽기 위해 받습니다.
    // HttpServletResponse : 쿠키 삭제 머리글(Set-Cookie)을 응답에 추가하기 위해 받습니다.
    @DeleteMapping("/api/refresh-token")
    public ResponseEntity<Void> deleteRefreshToken(HttpServletRequest request, HttpServletResponse response) {
        // (1) DB 의 리프레시 토큰 삭제 (누구의 토큰인지는 SecurityContext 의 액세스 토큰으로 알아냅니다)
        refreshTokenService.delete();

        // (2) 브라우저 쿠키 삭제. 쿠키 이름은 OAuth2SuccessHandler 가 저장할 때 쓴 이름과 같아야 합니다.
        CookieUtil.deleteCookie(request, response, OAuth2SuccessHandler.REFRESH_TOKEN_COOKIE_NAME);

        // (3) 본문 없이 200 응답
        return ResponseEntity.ok().build();
    }



}
