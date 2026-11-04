// 파일 위치: src/main/java/me/shinsunyoung/springbootdeveloper/config/oauth/OAuth2SuccessHandler.java
//
// ============================================================================
// 이 파일이 하는 일  [11장 · 구글 로그인 성공 → 우리 JWT 발급]
// ============================================================================
// 구글 로그인이 성공한 "바로 그 순간"에 스프링 시큐리티가 부르는 클래스입니다.
// 구글 로그인은 "이 사람이 누구인지" 확인해 줄 뿐이고,
// 그 뒤 우리 API 를 쓸 때 필요한 출입증(JWT)은 우리 서버가 직접 만들어 줘야 합니다. 그 일을 여기서 합니다.
//
//   onAuthenticationSuccess() 가 하는 일 (순서대로)
//   ┌───────────────────────────────────────────────────────────────────────┐
//   │ 1. 구글 정보에서 이메일을 꺼내 우리 회원(User)을 찾는다                  │
//   │ 2. 리프레시 토큰(14일)을 만든다                                         │
//   │ 3.   └ refresh_token 표(테이블)에 저장한다 (있으면 갱신, 없으면 새로)            │
//   │ 4.   └ 브라우저 쿠키 "refresh_token" 으로 내려보낸다                    │
//   │ 5. 액세스 토큰(1일)을 만든다                                            │
//   │ 6. 이동할 주소를 만든다 → /articles?token=액세스토큰                     │
//   │ 7. 로그인 중에 쓰던 임시 정보(인가 요청 쿠키 등)를 지운다                 │
//   │ 8. 브라우저를 6번 주소로 이동(302 redirect)시킨다                         │
//   └───────────────────────────────────────────────────────────────────────┘
//   그 다음 articleList.html 의 token.js 가 주소의 token 값을 localStorage 에 저장합니다.
//
// ============================================================================
// 토큰 두 개는 각각 어디에 보관되나요?
// ============================================================================
//   토큰          유효기간   서버 보관               브라우저 보관                     쓰임
//   ───────────   ────────   ────────────────────   ────────────────────────────────   ───────────────────────────
//   액세스 토큰    1일        보관 안 함              localStorage("access_token")        API 요청 헤더 Bearer …
//   리프레시 토큰  14일       refresh_token 표        쿠키 "refresh_token"(14일)           POST /api/token 재발급용
//
//   ※ 기간 비교: 여기서 처음 만드는 액세스 토큰은 1일(ACCESS_TOKEN_DURATION)이고,
//     나중에 /api/token 으로 다시 받는 액세스 토큰은 2시간(TokenService)입니다. 두 값이 서로 다릅니다.
//
// ============================================================================
// 꼭 알아둘 낱말
// ============================================================================
// SimpleUrlAuthenticationSuccessHandler : "로그인 성공 후 어떤 주소로 보낼지" 기본 기능을 가진 시큐리티 클래스입니다.
//                                         이 클래스는 그것을 물려받아 onAuthenticationSuccess() 를 새로 정의했습니다.
// Authentication   : 로그인 결과 묶음입니다. getPrincipal() 로 로그인한 사람 정보를 꺼냅니다.
//                    구글 로그인에서는 그 안에 OAuth2User(구글 사용자 정보)가 들어 있습니다.
// 리다이렉트(302)   : 서버가 "이 주소로 다시 가세요"라고 브라우저에게 알려 주는 응답입니다.
// 쿼리 파라미터     : 주소 뒤 ?이름=값 부분입니다. 예) /articles?token=eyJhbGciOi…
//
// ============================================================================
// 꼭 조심할 점
// ============================================================================
// 1. ⚠️ 액세스 토큰을 주소(?token=…)에 실어 보냅니다. 이 주소는 브라우저 방문 기록, 서버 접속 기록에 남을 수 있습니다.
//    수업용으로 간단히 만든 방식이며, 실제 서비스에서는 더 안전한 전달 방법을 고민해야 합니다.
// 2. ⚠️ refresh_token 쿠키에는 HttpOnly 설정이 없습니다. (CookieUtil.addCookie 참고)
//    그래서 article.js 가 document.cookie 로 읽을 수 있지만, 반대로 악성 스크립트도 읽을 수 있습니다.
// 3. 이 클래스는 @Component 로도 등록되고, WebOAuthSecurityConfig 의 @Bean 으로도 만들어집니다.
//    실제 보안 설정에 연결되는 것은 @Bean 쪽 객체입니다.
// ============================================================================

// 이 파일이 속한 폴더 이름입니다. OAuth2(구글 로그인) 관련 클래스를 모아 두는 config/oauth 폴더입니다.
package com.shinsunyoung.blog.config.oauth;

import com.shinsunyoung.blog.config.jwt.TokenProvider;
import com.shinsunyoung.blog.repository.RefreshTokenRepository;
import com.shinsunyoung.blog.service.UserService;
import jakarta.servlet.http.HttpServletRequest;                                         // 브라우저가 보낸 요청 정보(쿠키 등)를 담은 객체입니다.
import jakarta.servlet.http.HttpServletResponse;                                        // 브라우저로 보낼 응답(쿠키, 이동 주소 등)을 담는 객체입니다.
import lombok.RequiredArgsConstructor;                                                  // final 변수 4개를 받는 생성자를 자동으로 만들어 줍니다.
import com.shinsunyoung.blog.config.jwt.TokenProvider;                   // 우리 JWT 를 만드는 도구입니다.
import com.shinsunyoung.blog.domain.RefreshToken;                         // refresh_token 표의 한 줄을 나타내는 클래스입니다.
import com.shinsunyoung.blog.domain.User;                                 // 우리 회원 클래스입니다.
import com.shinsunyoung.blog.repository.RefreshTokenRepository;          // refresh_token 표에 저장·조회하는 도구입니다.
import com.shinsunyoung.blog.service.UserService;                        // 이메일로 회원을 찾는 서비스입니다.
import com.shinsunyoung.blog.util.CookieUtil;                             // 쿠키 추가·삭제 도우미입니다.
import org.springframework.security.core.Authentication;                               // 로그인 결과 묶음입니다.
import org.springframework.security.oauth2.core.user.OAuth2User;                       // 구글에서 받은 사용자 정보입니다.
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler; // 로그인 성공 처리 기본 클래스입니다.
import org.springframework.stereotype.Component;                                       // 스프링 빈으로 등록하는 표시입니다.
import org.springframework.web.util.UriComponentsBuilder;                              // 주소(URL)를 안전하게 조립하는 도구입니다.

import java.io.IOException;  // 응답을 보내다 입출력 문제가 생길 때 나는 예외입니다.
import java.time.Duration;   // "14일", "1일" 같은 시간 길이를 나타내는 자료형입니다.

// @RequiredArgsConstructor : 아래 final 변수 4개를 받는 생성자를 만듭니다.
//                            WebOAuthSecurityConfig.oAuth2SuccessHandler() 가 이 생성자로 객체를 만듭니다.
// @Component               : 스프링 빈으로도 등록됩니다. (위 "조심할 점" 3번 참고)
@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    // public static final : 바뀌지 않는 공용 값(상수)입니다. 다른 클래스에서도 OAuth2SuccessHandler.이름 으로 쓸 수 있습니다.
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token"; // 리프레시 토큰을 담을 쿠키 이름. article.js 의 getCookie('refresh_token') 과 같아야 합니다.
    public static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(14); // 리프레시 토큰 유효기간 14일 (쿠키 수명도 같은 값 사용)
    public static final Duration ACCESS_TOKEN_DURATION = Duration.ofDays(1);   // 처음 발급하는 액세스 토큰 유효기간 1일
    public static final String REDIRECT_PATH = "/articles";                    // 로그인 성공 후 이동할 주소 (글 목록 화면)

    private final TokenProvider tokenProvider;                                         // JWT 만들기
    private final RefreshTokenRepository refreshTokenRepository;                       // 리프레시 토큰 DB 저장
    private final OAuth2AuthorizationRequestBasedOnCookieRepository authorizationRequestRepository; // 인가 요청 쿠키 지우기
    private final UserService userService;                                             // 이메일로 회원 찾기

    // ------------------------------------------------------------------------
    // onAuthenticationSuccess: 구글 로그인 성공 시 시큐리티가 부르는 메서드
    // ------------------------------------------------------------------------
    // 받는 값: request(요청), response(응답), authentication(로그인 결과)
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        // [1] 로그인 결과에서 구글 사용자 정보를 꺼냅니다.
        //     getPrincipal() 은 Object 타입이라 OAuth2User 로 바꿔(형변환) 씁니다.
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        //     구글 정보의 "email" 값으로 우리 users 표의 회원을 찾습니다.
        //     (OAuth2UserCustomService 가 바로 앞에서 저장해 두었으므로 반드시 있습니다.)
        User user = userService.findByEmail((String) oAuth2User.getAttributes().get("email"));

        // [2] 리프레시 토큰(14일)을 만듭니다. 내용(sub=이메일, id=회원번호)은 액세스 토큰과 같고 기간만 깁니다.
        String refreshToken = tokenProvider.generateToken(user, REFRESH_TOKEN_DURATION);
        // [3] DB(refresh_token 표)에 저장합니다. 나중에 /api/token 요청 때 "진짜 우리가 준 토큰인지" 비교합니다.
        saveRefreshToken(user.getId(), refreshToken);
        // [4] 브라우저 쿠키로도 내려보냅니다. 브라우저가 보관하다가 재발급 요청 때 꺼내 씁니다.
        addRefreshTokenToCookie(request, response, refreshToken);

        // [5] 액세스 토큰(1일)을 만듭니다.
        String accessToken = tokenProvider.generateToken(user, ACCESS_TOKEN_DURATION);
        // [6] 이동할 주소를 만듭니다. 예) /articles?token=eyJhbGciOiJIUzI1NiJ9…
        String targetUrl = getTargetUrl(accessToken);

        // [7] 로그인 과정에서 쓰던 임시 정보를 지웁니다.
        clearAuthenticationAttributes(request, response);

        // [8] 브라우저를 targetUrl 로 보냅니다. (응답: 302 Found, Location: /articles?token=…)
        //     getRedirectStrategy() 는 부모 클래스(SimpleUrlAuthenticationSuccessHandler)가 가진 "이동 도구"입니다.
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    // ------------------------------------------------------------------------
    // saveRefreshToken: 리프레시 토큰을 DB 에 저장 (있으면 갱신, 없으면 새로)
    // ------------------------------------------------------------------------
    // RefreshToken 표는 user_id 가 unique 라서, 회원 한 명당 한 줄만 있어야 합니다.
    // 그래서 다시 로그인하면 새 줄을 만들지 않고 기존 줄의 토큰 값만 바꿉니다.
    //   처음 로그인  : INSERT INTO refresh_token (user_id, refresh_token) VALUES (1, 'eyJ…')
    //   다시 로그인  : UPDATE refresh_token SET refresh_token = 'eyJ…(새 값)' WHERE id = …
    // → 예전 리프레시 토큰은 DB 값과 달라지므로 더는 재발급에 쓸 수 없습니다.
    private void saveRefreshToken(Long userId, String newRefreshToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByUserId(userId) // 이 회원의 기존 줄 찾기
                .map(entity -> entity.update(newRefreshToken))                 // 있으면 토큰 값만 새 값으로 교체
                .orElse(new RefreshToken(userId, newRefreshToken));            // 없으면 새 줄 객체 만들기

        refreshTokenRepository.save(refreshToken); // INSERT 또는 UPDATE 실행
    }

    // ------------------------------------------------------------------------
    // addRefreshTokenToCookie: 리프레시 토큰을 쿠키로 내려보내기
    // ------------------------------------------------------------------------
    private void addRefreshTokenToCookie(HttpServletRequest request, HttpServletResponse response, String refreshToken) {
        // 쿠키 수명(초 단위). 14일 = 14 × 24 × 60 × 60 = 1,209,600초
        int cookieMaxAge = (int) REFRESH_TOKEN_DURATION.toSeconds();

        // 예전 refresh_token 쿠키가 있으면 먼저 지우고(수명 0으로 덮어쓰기)
        CookieUtil.deleteCookie(request, response, REFRESH_TOKEN_COOKIE_NAME);
        // 새 값으로 다시 만듭니다. 응답 헤더 예) Set-Cookie: refresh_token=eyJ…; Max-Age=1209600; Path=/
        CookieUtil.addCookie(response, REFRESH_TOKEN_COOKIE_NAME, refreshToken, cookieMaxAge);
    }

    // ------------------------------------------------------------------------
    // clearAuthenticationAttributes: 로그인 과정의 임시 정보 지우기
    // ------------------------------------------------------------------------
    private void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {

        // 부모 기능: 세션에 남아 있을 수 있는 "로그인 실패 오류 정보"를 지웁니다. (세션이 없으면 아무 일도 안 함)
        super.clearAuthenticationAttributes(request);

        // 우리 기능: 흐름 [2]에서 만든 oauth2_auth_request 쿠키를 지웁니다. 로그인이 끝났으니 더 필요 없습니다.
        authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }

    // ------------------------------------------------------------------------
    // getTargetUrl: 이동할 주소 만들기
    // ------------------------------------------------------------------------
    // "/articles" 뒤에 ?token=액세스토큰 을 붙입니다.
    // UriComponentsBuilder 를 쓰면 ? 와 & 를 직접 이어 붙이지 않아도 되고, 특수문자도 알맞게 처리됩니다.
    private String getTargetUrl(String token) {
        return UriComponentsBuilder.fromUriString(REDIRECT_PATH) // 시작 주소: /articles
                                    .queryParam("token", token)                      // ?token=… 추가
                                    .build()                                         // 주소 조립 완료
                                    .toUriString();                                  // 문자열로 꺼내기 → "/articles?token=eyJ…"
    }
}