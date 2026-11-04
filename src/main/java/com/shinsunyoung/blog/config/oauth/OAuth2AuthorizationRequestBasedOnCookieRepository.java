// 파일 위치: src/main/java/me/shinsunyoung/springbootdeveloper/config/oauth/OAuth2AuthorizationRequestBasedOnCookieRepository.java
//
// ============================================================================
// 이 파일이 하는 일  [11장 · 구글 로그인 "인가 요청"을 쿠키에 보관]
// ============================================================================
// 구글 로그인은 "우리 서버 → 구글 → 다시 우리 서버"로 주소가 두 번 오갑니다.
// 처음 구글로 보낼 때 만든 정보(인가 요청)를, 구글에서 돌아왔을 때 다시 꺼내 비교해야 합니다.
// 그 정보를 잠깐 맡아 두는 "보관함"이 이 클래스입니다.
//
//   시큐리티 기본 보관함 : HttpSessionOAuth2AuthorizationRequestRepository → 세션에 보관
//   이 클래스            : 쿠키에 보관  (우리는 STATELESS, 즉 세션을 쓰지 않기로 했기 때문)
//
// ============================================================================
// 언제 무엇이 불리나요?
// ============================================================================
//   [2] GET /oauth2/authorization/google
//       OAuth2AuthorizationRequestRedirectFilter
//         → saveAuthorizationRequest()  : 인가 요청을 쿠키 "oauth2_auth_request" 에 저장
//         → 302 로 구글 이동
//
//   [4] GET /login/oauth2/code/google?code=…&state=…
//       OAuth2LoginAuthenticationFilter
//         → removeAuthorizationRequest() : 쿠키에서 인가 요청을 꺼냄
//         → 꺼낸 요청의 state 와 구글이 돌려준 state 가 같은지 비교 (다르면 로그인 실패)
//
//   [5] 로그인 성공
//       OAuth2SuccessHandler.clearAuthenticationAttributes()
//         → removeAuthorizationRequestCookies() : 쿠키 삭제
//
// ============================================================================
// "인가 요청(OAuth2AuthorizationRequest)" 안에는 무엇이 있나요?
// ============================================================================
//   authorizationUri : 구글 로그인 주소 (https://accounts.google.com/o/oauth2/v2/auth)
//   clientId         : application.yml 의 client-id
//   redirectUri      : 구글이 돌려보낼 주소 (http://localhost:8080/login/oauth2/code/google)
//   scopes           : [email, profile]
//   state            : 무작위 문자열. 가짜 요청(CSRF)을 막는 열쇠
//   attributes       : registration_id = "google" 등
//
// ============================================================================
// 꼭 알아둘 낱말
// ============================================================================
// AuthorizationRequestRepository<T> : "인가 요청을 저장·조회·삭제하는 보관함" 약속(인터페이스)입니다.
//                                     save / load / remove 세 메서드를 만들어야 합니다.
// 직렬화(serialize)   : 자바 객체를 글자(바이트)로 바꾸는 일입니다. 쿠키에는 글자만 담을 수 있어서 필요합니다.
// 역직렬화(deserialize): 글자를 다시 자바 객체로 되돌리는 일입니다.
//
// ============================================================================
// 꼭 조심할 점
// ============================================================================
// 1. removeAuthorizationRequest() 는 이름과 달리 쿠키를 "지우지 않고" 꺼내기만 합니다.
//    실제 삭제는 로그인 성공 후 OAuth2SuccessHandler 가 합니다.
//    → 로그인이 실패하면 쿠키가 수명(5시간)이 끝날 때까지 남아 있습니다.
// 2. 쿠키 값은 사용자가 마음대로 바꿀 수 있는데, 그 값을 자바 역직렬화합니다.
//    수업용 예제 방식이며, 실제 서비스라면 서명·암호화 등으로 쿠키 위조를 막아야 합니다.
// ============================================================================

// 이 파일이 속한 폴더 이름입니다. OAuth2(구글 로그인) 관련 클래스를 모아 두는 config/oauth 폴더입니다.
package com.shinsunyoung.blog.config.oauth;

import com.shinsunyoung.blog.util.CookieUtil;
import jakarta.servlet.http.Cookie;                                              // 쿠키 한 개를 나타내는 클래스입니다.
import jakarta.servlet.http.HttpServletRequest;                                  // 브라우저가 보낸 요청(쿠키 포함)입니다.
import jakarta.servlet.http.HttpServletResponse;                                 // 브라우저로 보낼 응답(쿠키 추가·삭제)입니다.
import com.shinsunyoung.blog.util.CookieUtil;                      // 쿠키 추가·삭제·직렬화 도우미입니다.
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository; // 인가 요청 보관함 약속(인터페이스)입니다.
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;  // 인가 요청 한 건을 나타내는 클래스입니다.
import org.springframework.web.util.WebUtils;                                    // 요청에서 이름으로 쿠키를 찾아 주는 스프링 도우미입니다.

// implements AuthorizationRequestRepository<OAuth2AuthorizationRequest>
//   "OAuth2AuthorizationRequest 를 보관하는 보관함"이 되겠다는 약속입니다.
// ※ @Component 가 없습니다. WebOAuthSecurityConfig 의 @Bean 메서드가 new 로 만들어 빈으로 등록합니다.
public class OAuth2AuthorizationRequestBasedOnCookieRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    // 인가 요청을 담을 쿠키 이름입니다. public 이라 다른 클래스에서도 이 이름을 쓸 수 있습니다.
    public final static String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";

    // 쿠키 수명(초). 18000초 = 300분 = 5시간. 구글 로그인을 마치기에 충분한 시간입니다.
    private final static int COOKIE_EXPIRE_SECONDS = 18000;

    // ------------------------------------------------------------------------
    // removeAuthorizationRequest: 흐름 [4]에서 시큐리티가 부름 → 인가 요청 "꺼내기"
    // ------------------------------------------------------------------------
    // 약속상 "꺼내면서 지우기"를 하는 자리지만, 여기서는 꺼내기(load)만 합니다.
    // 쿠키 삭제는 성공 핸들러의 removeAuthorizationRequestCookies() 가 맡습니다. (위 "조심할 점" 1번)
    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest
                                                                         request, HttpServletResponse response) {
        return this.loadAuthorizationRequest(request);
    }

    // ------------------------------------------------------------------------
    // loadAuthorizationRequest: 쿠키에서 인가 요청 읽기
    // ------------------------------------------------------------------------
    // 1. 요청에서 이름이 "oauth2_auth_request" 인 쿠키를 찾습니다. (없으면 null)
    // 2. 쿠키 값(Base64 글자)을 OAuth2AuthorizationRequest 객체로 되돌립니다.
    // ⚠️ 쿠키가 없을 때(cookie == null) CookieUtil.deserialize 안에서 cookie.getValue() 를 부르면 오류가 납니다.
    //    정상 흐름에서는 [2]에서 쿠키를 먼저 만들기 때문에 문제가 되지 않습니다.
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest
                                                                       request) {
        Cookie cookie = WebUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        // [11.4 수정] 쿠키가 없을 때(만료·삭제 후 /login/oauth2/code/google 재접속 등) cookie 가 null 이라
        //   CookieUtil.deserialize 안의 cookie.getValue() 에서 NullPointerException(500)이 났습니다.
        //   null 을 돌려주면 스프링 시큐리티가 "인가 요청 없음" 으로 처리해 /login?error 로 보내 줍니다.
        if (cookie == null) {
            return null;
        }
        return CookieUtil.deserialize(cookie, OAuth2AuthorizationRequest.class);
    }

    // ------------------------------------------------------------------------
    // saveAuthorizationRequest: 흐름 [2]에서 시큐리티가 부름 → 인가 요청 "저장하기"
    // ------------------------------------------------------------------------
    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest
                                                 authorizationRequest, HttpServletRequest request, HttpServletResponse response) {

        // 시큐리티가 null 을 넘기면 "보관한 것을 지워라"는 뜻입니다. → 쿠키를 지우고 끝냅니다.
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookies(request, response);
            return;
        }
        // 객체 → 글자(Base64)로 바꿔서 쿠키에 담아 응답에 실어 보냅니다.
        // 응답 헤더 예) Set-Cookie: oauth2_auth_request=rO0ABXNy…; Max-Age=18000; Path=/
        CookieUtil.addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
                CookieUtil.serialize(authorizationRequest), COOKIE_EXPIRE_SECONDS);
    }

    // ------------------------------------------------------------------------
    // removeAuthorizationRequestCookies: 인가 요청 쿠키 지우기 (우리가 추가한 메서드)
    // ------------------------------------------------------------------------
    // 부르는 곳: saveAuthorizationRequest(null 일 때), OAuth2SuccessHandler.clearAuthenticationAttributes()
    public void removeAuthorizationRequestCookies(HttpServletRequest request,
                                                  HttpServletResponse response) {
        CookieUtil.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
    }
}