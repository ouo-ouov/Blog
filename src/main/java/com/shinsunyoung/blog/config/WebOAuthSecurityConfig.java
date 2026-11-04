// 파일 위치: src/main/java/me/shinsunyoung/springbootdeveloper/config/WebOAuthSecurityConfig.java
//
// ============================================================================
// 이 파일이 하는 일  [11장 · OAuth2 + JWT 보안 설정]
// ============================================================================
// 10장까지 쓰던 WebSecurityConfig(세션 + 폼 로그인)를 대신하는 "새 보안 설정"입니다.
// 11장에서는 WebSecurityConfig.java 의 코드를 통째로 주석 처리하고, 이 파일이 그 자리를 맡습니다.
//
// 이 파일 하나로 다음 세 가지가 연결됩니다.
//   1. 구글 로그인(OAuth2)   : 비밀번호 입력 대신 "구글 계정으로 로그인" 버튼으로 로그인합니다.
//   2. JWT 발급             : 구글 로그인이 성공하면 우리 서버가 액세스 토큰·리프레시 토큰을 만들어 줍니다.
//   3. JWT 검사             : 그 뒤 /api/** 요청마다 TokenAuthenticationFilter 가 토큰을 검사합니다.
//
// ============================================================================
// 10장 → 11장, 무엇이 바뀌었나
// ============================================================================
//   항목                  10장 WebSecurityConfig             11장 WebOAuthSecurityConfig (이 파일)
//   ───────────────────   ────────────────────────────────   ─────────────────────────────────────────
//   로그인 방식            이메일·비밀번호 폼(formLogin)       구글 로그인(oauth2Login), 폼 로그인 끔
//   로그인 상태 보관        서버 세션(JSESSIONID 쿠키)          토큰(JWT). 세션을 쓰지 않음(STATELESS)
//   요청마다 사용자 확인    세션에서 꺼냄                       Authorization: Bearer 토큰을 검사
//   TokenAuthenticationFilter  만들기만 하고 등록 안 함      addFilterBefore 로 필터 체인에 등록
//   /api/token            로그인해야 호출 가능(막혀 있었음)     permitAll (누구나 호출 가능)
//   막히는 주소            login/signup/user 빼고 전부          /api/** 만 막음, 나머지는 전부 허용
//   로그인 안 했을 때       /login 화면으로 이동(302)           /api/** 는 401 숫자만 돌려줌
//   로그아웃               시큐리티 LogoutFilter 가 처리        시큐리티 로그아웃 기능 끔(logout disable)
//   정적 파일 예외         /static/**                          /img/**, /css/**, /js/**
//
// ============================================================================
// 전체 흐름 한눈에 보기 (처음 로그인 → 글쓰기 → 토큰 만료)
// ============================================================================
//
//  [1] 로그인 화면
//      브라우저 ── GET /login ──▶ UserViewController.login() ──▶ oauthLogin.html
//      화면에는 구글 버튼 하나만 있습니다. 버튼 주소: /oauth2/authorization/google
//
//  [2] 구글로 보내기
//      브라우저 ── GET /oauth2/authorization/google ──▶ OAuth2AuthorizationRequestRedirectFilter (시큐리티 기본 필터)
//        ├─ "인가 요청" 정보(state 값 등)를 만들어
//        ├─ OAuth2AuthorizationRequestBasedOnCookieRepository 로 쿠키(oauth2_auth_request)에 저장하고
//        └─ 302 로 구글 로그인 페이지(accounts.google.com)로 보냅니다.
//
//  [3] 구글에서 로그인·동의
//      사용자가 구글 계정을 고르고 "email, profile 정보 제공"에 동의합니다.
//
//  [4] 구글이 우리 서버로 되돌려 보냄
//      구글 ── 302 ──▶ GET /login/oauth2/code/google?code=...&state=...
//      OAuth2LoginAuthenticationFilter (시큐리티 기본 필터)가 받습니다.
//        ├─ 쿠키에서 [2]의 인가 요청을 다시 꺼내 state 값이 같은지 확인합니다. (가짜 요청 방지)
//        ├─ code 를 구글에 보내 "구글 액세스 토큰"으로 바꿉니다.
//        └─ 그 토큰으로 구글에 사용자 정보(email, name …)를 물어봅니다.
//             → 이때 OAuth2UserCustomService.loadUser() 가 불립니다. (users 표에 저장 또는 이름 갱신)
//
//  [5] 로그인 성공 → 우리 서버의 JWT 발급
//      OAuth2SuccessHandler.onAuthenticationSuccess()
//        ├─ 리프레시 토큰(14일) 생성 → refresh_token 표에 저장 → refresh_token 쿠키로 내려보냄
//        ├─ 액세스 토큰(1일) 생성
//        ├─ [2]에서 만든 oauth2_auth_request 쿠키 삭제
//        └─ 302 → /articles?token=액세스토큰
//
//  [6] 글 목록 화면
//      articleList.html 의 token.js 가 주소창의 token 값을 localStorage("access_token")에 저장합니다.
//
//  [7] API 호출 (예: 글 등록)
//      article.js ── POST /api/articles + 헤더 "Authorization: Bearer 액세스토큰" ──▶
//      TokenAuthenticationFilter: 토큰 검사 → 통과하면 SecurityContextHolder 에 인증 정보 저장
//      AuthorizationFilter: /api/** 는 authenticated → 인증 정보가 있으니 통과
//      BlogApiController.addArticle(…, Principal) → 글쓴이 = 토큰의 sub(이메일)
//
//  [8] 액세스 토큰이 만료되면
//      TokenAuthenticationFilter 는 토큰이 틀리면 아무것도 넣지 않음 → 인증 없음 → 401
//      article.js 가 401 을 받으면 refresh_token 쿠키 값을 들고 POST /api/token (permitAll)
//      → TokenService 가 새 액세스 토큰(2시간) 발급 → localStorage 갱신 → 원래 요청을 다시 보냄
//
// ============================================================================
// 시큐리티 필터 순서 (이 설정으로 만들어지는 필터 체인, 중요한 것만)
// ============================================================================
//   요청 ▶ ① SecurityContextHolderFilter       : 요청마다 빈 보안 상자(SecurityContext)를 준비
//          ② OAuth2AuthorizationRequestRedirectFilter : /oauth2/authorization/google 이면 구글로 보냄  [2]
//          ③ OAuth2LoginAuthenticationFilter   : /login/oauth2/code/google 이면 로그인 처리      [4][5]
//          ④ TokenAuthenticationFilter (우리 것): Bearer 토큰 검사                                [7]
//             ↑ addFilterBefore(…, UsernamePasswordAuthenticationFilter.class) 로 이 자리에 들어감
//          ⑤ AnonymousAuthenticationFilter     : 아직 인증이 없으면 "익명 사용자"로 채움
//          ⑥ ExceptionTranslationFilter        : 인증 없음 오류 → 401 또는 /login 이동으로 바꿈
//          ⑦ AuthorizationFilter               : authorizeHttpRequests 규칙으로 통과/거절 결정
//          ▶ DispatcherServlet ▶ 컨트롤러
//
//   ※ formLogin 을 껐기 때문에 UsernamePasswordAuthenticationFilter 자체는 체인에 없습니다.
//     하지만 "그 필터가 설 자리(순서 번호)"는 정해져 있어서, 그 앞자리에 우리 필터를 끼울 수 있습니다.
//   ※ ④가 ⑤보다 앞에 있어야 합니다. 그래야 토큰이 맞을 때 익명 사용자로 덮어쓰지 않습니다.
//
// ============================================================================
// 주소별 허용 규칙 정리
// ============================================================================
//   주소                          규칙             설명
//   ───────────────────────────   ──────────────   ────────────────────────────────────────────
//   /h2-console/**, /img/**,      ignoring         보안 필터를 아예 거치지 않음 (configure())
//   /css/**, /js/**
//   /api/token                    permitAll        토큰 없이도 호출 가능. 액세스 토큰 재발급용
//   /api/** (그 밖의 API 전부)     authenticated    올바른 액세스 토큰이 있어야 함. 없으면 401
//   그 밖의 모든 주소              permitAll        /login, /articles, /articles/{id}, /new-article,
//                                                  /signup, /user, /logout, /uploads/**, /error 등
//
//   ⚠️ 화면 주소(/articles, /new-article …)는 누구나 볼 수 있습니다.
//      글 등록·수정·삭제 같은 "실제 데이터 변경"은 화면 뒤의 /api/** 호출에서 막힙니다.
//
// ============================================================================
// 꼭 알아둘 낱말
// ============================================================================
// OAuth2        : "다른 서비스(구글)의 로그인을 빌려 쓰는" 표준 방식입니다.
//                 우리 서버는 사용자의 구글 비밀번호를 전혀 보지 않습니다.
// 리소스 오너    : 정보의 주인, 즉 로그인하는 사용자입니다.
// 클라이언트     : 정보를 받아 가려는 쪽, 즉 우리 스프링 부트 서버입니다.
// 인가 서버      : 로그인을 확인하고 code·토큰을 내주는 쪽, 즉 구글입니다.
// 인가 코드(code): 구글이 "로그인 성공했어요"라며 잠깐 쓰라고 주는 일회용 번호입니다.
//                 우리 서버는 이 code 를 구글 액세스 토큰으로 바꿔 사용자 정보를 받습니다.
// state         : [2]에서 만들어 쿠키에 보관한 무작위 값입니다. [4]에서 같은지 비교해 가짜 요청을 막습니다.
// 무상태(STATELESS): 서버가 로그인 상태를 기억하지 않는 방식입니다. 요청마다 토큰으로 확인합니다.
// 401 Unauthorized: "누구인지 확인이 안 됨(인증 실패)"을 뜻하는 HTTP 상태 번호입니다.
//
// ※ 구글 액세스 토큰 vs 우리 액세스 토큰
//   흐름 [4]의 "구글 액세스 토큰"은 구글에 사용자 정보를 물어볼 때 한 번 쓰고 끝납니다.
//   흐름 [5]부터 브라우저가 들고 다니는 것은 우리 서버가 TokenProvider 로 만든 "우리 JWT" 입니다.
//
// ============================================================================
// 관련 파일
// ============================================================================
//   config/
//   ├─ WebOAuthSecurityConfig.java   ← 지금 이 파일
//   ├─ WebSecurityConfig.java        ← 10장 설정. 11장에서 전체 주석 처리됨
//   ├─ TokenAuthenticationFilter.java← 요청마다 Bearer 토큰 검사 (10장에서 만들고 11장에서 등록)
//   ├─ jwt/TokenProvider.java        ← 토큰 만들기·검사·인증 정보 만들기
//   └─ oauth/
//      ├─ OAuth2UserCustomService.java                       ← 구글 사용자 정보 → users 표 저장
//      ├─ OAuth2SuccessHandler.java                          ← 로그인 성공 후 JWT 발급·쿠키·이동
//      └─ OAuth2AuthorizationRequestBasedOnCookieRepository.java ← 인가 요청을 세션 대신 쿠키에 보관
//   util/CookieUtil.java             ← 쿠키 추가·삭제·직렬화 도우미
//   resources/application.yml       ← spring.security.oauth2.client.registration.google (client-id 등)
//   resources/templates/oauthLogin.html ← 구글 로그인 버튼 화면
//   resources/static/js/token.js    ← 주소창 ?token= 값을 localStorage 에 저장
//   resources/static/js/article.js  ← API 호출 시 Bearer 토큰 첨부, 401 이면 재발급 후 재시도
// ============================================================================

// 이 파일이 속한 폴더 이름입니다. 설정 클래스들을 모아 두는 config 폴더입니다.
package com.shinsunyoung.blog.config;

import lombok.RequiredArgsConstructor;                                                                     // final 변수를 받는 생성자를 자동으로 만들어 줍니다. (스프링이 그 생성자로 필요한 객체를 넣어 줍니다)
import com.shinsunyoung.blog.config.jwt.TokenProvider;                                      // 우리 JWT 를 만들고 검사하는 클래스입니다.
import com.shinsunyoung.blog.config.oauth.OAuth2AuthorizationRequestBasedOnCookieRepository; // OAuth2 인가 요청을 쿠키에 저장·조회·삭제하는 클래스입니다.
import com.shinsunyoung.blog.config.oauth.OAuth2SuccessHandler;                             // 구글 로그인 성공 직후 JWT 를 발급하는 클래스입니다.
import com.shinsunyoung.blog.config.oauth.OAuth2UserCustomService;                          // 구글에서 받은 사용자 정보를 users 표에 저장하는 클래스입니다.
import com.shinsunyoung.blog.repository.RefreshTokenRepository;                             // refresh_token 표를 다루는 도구입니다. (성공 핸들러에 넘겨 줌)
import com.shinsunyoung.blog.service.UserService;                                           // 이메일로 회원을 찾는 서비스입니다. (성공 핸들러에 넘겨 줌)
import org.springframework.context.annotation.Bean;                                                       // 메서드가 돌려준 객체를 스프링이 관리하는 빈으로 등록하는 표시입니다.
import org.springframework.context.annotation.Configuration;                                              // "이 클래스는 설정 클래스"라는 표시입니다.
import org.springframework.http.HttpStatus;                                                               // 200, 401 같은 HTTP 상태 번호 모음입니다.
import org.springframework.security.config.annotation.web.builders.HttpSecurity;                          // 보안 규칙을 한 줄씩 조립하는 도구입니다.
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;            // 보안 검사 자체를 건너뛸 주소를 정하는 도구입니다.
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;             // 기능을 끌 때 쓰는 disable 메서드를 가진 클래스입니다.
import org.springframework.security.config.http.SessionCreationPolicy;                                    // 세션을 만들지 말지 정하는 선택지 모음입니다. (STATELESS 등)
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;                                  // 비밀번호를 BCrypt 방식으로 암호화하는 도구입니다.
import org.springframework.security.crypto.password.PasswordEncoder;                                      // 비밀번호 암호화 도구들의 공통 약속(인터페이스)입니다.
import org.springframework.security.web.SecurityFilterChain;                                              // 완성된 보안 필터 묶음 타입입니다.
import org.springframework.security.web.authentication.HttpStatusEntryPoint;                              // 인증이 없을 때 "상태 번호만" 돌려주는 처리기입니다.
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;              // 폼 로그인 필터입니다. 우리 필터를 "이 필터 앞"에 끼우는 기준으로만 씁니다.

import static org.springframework.boot.security.autoconfigure.web.servlet.PathRequest.toH2Console;        // H2 콘솔 주소(/h2-console/**)를 알려 주는 도구입니다.

// @RequiredArgsConstructor
//   아래 private final 변수 4개를 받는 생성자를 Lombok 이 만들어 줍니다.
//   스프링은 그 생성자를 보고 OAuth2UserCustomService, TokenProvider, RefreshTokenRepository, UserService 를 넣어 줍니다.
// @Configuration
//   설정 클래스 표시입니다. 안의 @Bean 메서드들이 돌려준 객체가 스프링 빈이 됩니다.
//
// ※ @EnableWebSecurity 가 없어도 됩니다.
//   스프링 부트가 spring-boot-starter-security 를 보고 웹 보안을 자동으로 켜 줍니다.
//   (10장 WebSecurityConfig 에는 붙어 있었지만, 이 파일처럼 빠져도 동작은 같습니다.)
@RequiredArgsConstructor
@Configuration
public class WebOAuthSecurityConfig {


    // 구글에서 받은 사용자 정보를 users 표에 저장(또는 이름 갱신)하는 서비스입니다.
    // oauth2Login 의 userInfoEndpoint 에 연결합니다. (흐름 [4])
    private final OAuth2UserCustomService oAuth2UserCustomService;
    // JWT 를 만들고 검사하는 도구입니다.
    // TokenAuthenticationFilter 와 OAuth2SuccessHandler 를 만들 때 넘겨 줍니다.
    private final TokenProvider tokenProvider;
    // refresh_token 표를 다루는 도구입니다. OAuth2SuccessHandler 가 리프레시 토큰을 저장할 때 씁니다.
    private final RefreshTokenRepository refreshTokenRepository;
    // 회원 정보를 찾는 서비스입니다. OAuth2SuccessHandler 가 이메일로 회원을 찾을 때 씁니다.
    // ⚠️ 이 클래스가 UserService 를 받기 때문에, UserService 는 이 클래스의 PasswordEncoder 빈을 받을 수 없습니다.
    //    (서로가 서로를 기다리는 "순환 참조"가 생김) → 그래서 11장 UserService 는
    //    PasswordEncoder 를 주입받지 않고 new BCryptPasswordEncoder() 를 직접 만들어 씁니다.
    private final UserService userService;


    // ------------------------------------------------------------------------
    // configure(): 보안 검사를 "아예 하지 않을" 주소 정하기
    // ------------------------------------------------------------------------
    // ignoring() 에 넣은 주소는 시큐리티 필터 체인을 통째로 건너뜁니다.
    //   → TokenAuthenticationFilter 도, AuthorizationFilter 도 실행되지 않습니다.
    // permitAll() 과의 차이
    //   permitAll : 필터는 모두 지나가되 "통과"시킴 (SecurityContext 를 쓸 수 있음)
    //   ignoring  : 필터를 아예 안 지나감 (더 가볍지만 보안 기능도 전혀 없음)
    // 그래서 그림·CSS·JS 처럼 "누가 봐도 되는 정적 파일"에만 씁니다.
    //
    // ※ 10장의 "/static/**" 는 실제 브라우저 주소와 맞지 않았습니다.
    //   src/main/resources/static/js/token.js 의 실제 주소는 /js/token.js 이지 /static/js/… 가 아닙니다.
    //   11장에서는 실제 주소 모양(/img, /css, /js)으로 바로잡았습니다.
    @Bean
    public WebSecurityCustomizer configure() {
        return (web) -> web.ignoring()
                .requestMatchers(toH2Console()) // H2 데이터베이스 관리 화면(/h2-console/**)은 보안 검사를 건너뜁니다. (개발용)
                .requestMatchers(
                        "/img/**", // 그림 파일. oauthLogin.html 의 구글 버튼 그림(/img/google.png)이 여기 있습니다.
                        "/css/**", // 스타일 파일
                        "/js/**"   // 자바스크립트 파일. token.js, article.js 가 여기 있습니다.
                );
    }

    // ------------------------------------------------------------------------
    // filterChain(): 보안 규칙의 본체
    // ------------------------------------------------------------------------
    // HttpSecurity 에 규칙을 한 줄씩 이어 붙이고, 마지막 build() 로 필터 묶음(SecurityFilterChain)을 완성합니다.
    // 스프링은 이 빈을 보고 "모든 요청이 지나갈 보안 필터 순서"를 만듭니다.
    //
    // ── 1) 토큰 방식에 필요 없는 기본 기능 끄기 ──
    //   csrf 끄기
    //     CSRF 공격은 "브라우저가 쿠키(세션)를 자동으로 붙여 보내는 점"을 노립니다.
    //     우리 API 는 쿠키가 아니라 Authorization 헤더의 토큰으로 사용자를 확인하므로 끕니다.
    //     (헤더는 브라우저가 자동으로 붙이지 않고, 우리 JS 가 직접 붙여야만 갑니다.)
    //   httpBasic 끄기
    //     요청 헤더에 "아이디:비밀번호"를 담아 보내는 가장 단순한 로그인 방식입니다. 쓰지 않으므로 끕니다.
    //   formLogin 끄기
    //     10장의 이메일·비밀번호 로그인 폼 처리(POST /login)를 끕니다.
    //     → login.html, signup.html 은 남아 있지만 "비밀번호로 로그인"은 더 이상 되지 않습니다.
    //   logout 끄기
    //     시큐리티의 LogoutFilter 를 끕니다. 그래서 GET /logout 은 이제 컨트롤러까지 갑니다.
    //     (10장에서는 LogoutFilter 가 먼저 가로채서 UserApiController.logout() 이 실행되지 않았습니다.)
    //     토큰 방식의 로그아웃은 "브라우저의 토큰 삭제 + DB 리프레시 토큰 삭제"로 해야 합니다.
    //
    // ── 2) 세션 쓰지 않기 (sessionManagement) ──
    //   STATELESS: 시큐리티가 로그인 상태를 세션에 저장하지도, 세션에서 꺼내지도 않습니다.
    //     → 요청마다 TokenAuthenticationFilter 가 토큰으로 사용자를 새로 확인합니다.
    //     → 서버를 여러 대로 늘려도 "어느 서버에 세션이 있나" 걱정이 없습니다.
    //   다른 선택지: ALWAYS(항상 만듦), IF_REQUIRED(필요하면 만듦, 기본값), NEVER(직접 만들진 않지만 있으면 씀)
    //
    // ── 3) 우리 토큰 필터 등록 (addFilterBefore) ──
    //   addFilterBefore(A, B): A 필터를 B 필터 자리 "바로 앞"에 넣습니다.
    //   10장에서는 TokenAuthenticationFilter 를 만들기만 하고 등록하지 않아서 실행되지 않았습니다.
    //   11장에서 이 한 줄로 실제 요청마다 실행되기 시작합니다.
    //
    // ── 4) 주소별 허용 규칙 (authorizeHttpRequests) ──
    //   위에서부터 차례로 검사하고, 먼저 맞는 규칙이 적용됩니다.
    //   "/api/token" permitAll
    //     액세스 토큰 재발급 API 입니다. 토큰이 만료된 상태에서 부르는 주소이므로 누구나 호출할 수 있어야 합니다.
    //     (리프레시 토큰이 맞는지는 TokenService 가 직접 검사합니다.)
    //     ⚠️ 순서가 중요합니다. 아래 "/api/**" 보다 먼저 써야 합니다.
    //        거꾸로 쓰면 /api/token 이 "/api/**" 규칙에 먼저 걸려 막힙니다.
    //   "/api/**" authenticated
    //     /api/로 시작하는 나머지 모든 주소는 인증(올바른 액세스 토큰)이 있어야 통과합니다.
    //     예) /api/articles, /api/articles/1, /api/upload, /api/ai-suggestions, /api/ai-thumbnails
    //   anyRequest() permitAll
    //     그 밖의 모든 주소는 누구나 통과합니다. 화면(HTML), 로그인 관련 주소, /error 등
    //
    // ── 5) 구글 로그인 설정 (oauth2Login) ──
    //   oauth2Login 을 켜면 시큐리티가 다음 두 주소를 자동으로 처리합니다. (우리가 컨트롤러를 만들 필요 없음)
    //     GET /oauth2/authorization/{등록이름}   → 구글 로그인 페이지로 보내기   (흐름 [2])
    //     GET /login/oauth2/code/{등록이름}       → 구글이 돌려준 code 처리하기   (흐름 [4])
    //     {등록이름} 은 application.yml 의 registration 아래 이름, 즉 "google" 입니다.
    //   loginPage("/login")
    //     우리가 만든 /login (oauthLogin.html) 을 로그인 화면으로 쓰겠다는 뜻입니다.
    //     정하지 않으면 시큐리티가 기본 로그인 화면을 자동으로 만들어 보여 줍니다.
    //   authorizationEndpoint(… authorizationRequestRepository(쿠키 보관함))
    //     흐름 [2]의 인가 요청(state 등)을 어디에 보관할지 정합니다.
    //     기본값은 "세션"인데, 우리는 세션을 쓰지 않기로 했으므로 "쿠키" 보관함으로 바꿉니다.
    //   userInfoEndpoint(… userService(oAuth2UserCustomService))
    //     흐름 [4]에서 구글 사용자 정보를 받는 서비스로, 기본 서비스 대신 우리 서비스를 씁니다.
    //     → 받은 정보를 users 표에 저장합니다.
    //     ⚠️ scope 에 openid 를 넣으면 OIDC 방식(OidcUserService)이 쓰여서 이 서비스가 불리지 않습니다.
    //        application.yml 의 scope 가 email, profile 두 개뿐인 이유입니다.
    //   successHandler(oAuth2SuccessHandler())
    //     흐름 [5]. 기본 동작(원래 가려던 주소로 이동) 대신
    //     JWT 발급 → refresh_token 쿠키 저장 → /articles?token=… 이동을 합니다.
    //
    // ── 6) 인증이 없을 때의 응답 방식 (exceptionHandling) ──
    //   인증이 필요한 주소에 인증 없이 오면, ExceptionTranslationFilter 가 "진입점(EntryPoint)"을 부릅니다.
    //   oauth2Login 의 기본 진입점은 로그인 화면(/login)으로 302 이동시키는 것입니다.
    //   하지만 API 를 부르는 쪽은 자바스크립트(fetch)이므로, HTML 로그인 화면을 받아 봐야 쓸모가 없습니다.
    //   그래서 /api/ 로 시작하는 요청은 화면 이동 대신 "401 Unauthorized" 숫자만 돌려주게 합니다.
    //     → article.js 의 httpRequest() 가 401 을 보고 토큰 재발급(/api/token)을 시도할 수 있습니다.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)      // 1) CSRF 보호 끔 (헤더 토큰 방식이라 필요 없음)
                .httpBasic(AbstractHttpConfigurer::disable) // 1) HTTP Basic 로그인 끔
                .formLogin(AbstractHttpConfigurer::disable) // 1) 이메일·비밀번호 폼 로그인 끔 (POST /login 처리 안 함)
                .logout(AbstractHttpConfigurer::disable)    // 1) 시큐리티 로그아웃 필터 끔 (GET /logout 이 컨트롤러로 감)
                .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 2) 로그인 상태를 세션에 저장하지 않음
                .addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)        // 3) 우리 토큰 검사 필터를 로그인 필터 자리 앞에 끼움
                .authorizeHttpRequests(auth -> auth                                                              // 4) 주소별 허용 규칙
                        .requestMatchers("/api/token").permitAll()  //    토큰 재발급 API 는 누구나 (반드시 /api/** 보다 먼저!)
                        .requestMatchers("/api/**").authenticated() //    나머지 API 는 인증된 사용자만 (없으면 401)
                        .anyRequest().permitAll())                  //    그 밖의 주소(화면 등)는 누구나
                .oauth2Login(oauth2 -> oauth2                                                                    // 5) 구글 로그인 켜기
                        .loginPage("/login") // 로그인 화면 = 우리 oauthLogin.html
                        .authorizationEndpoint(authorizationEndpoint -> authorizationEndpoint.authorizationRequestRepository(oAuth2AuthorizationRequestBasedOnCookieRepository())) // [2] 인가 요청을 쿠키에 보관
                        .userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint.userService(oAuth2UserCustomService)) // [4] 사용자 정보 → users 표 저장
                        .successHandler(oAuth2SuccessHandler()) // [5] 로그인 성공 → JWT 발급
                )
                .exceptionHandling(ex -> ex                                                                      // 6) 인증 없음 처리
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),        // 돌려줄 응답: 본문 없이 401 숫자만
                                request -> request.getRequestURI().startsWith("/api/")    // 적용 조건: 주소가 /api/ 로 시작할 때
                        )
                )
                .build(); // 지금까지 이어 붙인 규칙으로 필터 묶음을 완성합니다.
    }




    // ------------------------------------------------------------------------
    // oAuth2SuccessHandler(): 구글 로그인 성공 처리기를 빈으로 만들기
    // ------------------------------------------------------------------------
    // 필요한 재료 4개를 생성자로 넘겨 줍니다.
    //   tokenProvider           : 액세스·리프레시 토큰 만들기
    //   refreshTokenRepository  : 리프레시 토큰 DB 저장
    //   쿠키 보관함              : 로그인이 끝나면 oauth2_auth_request 쿠키 지우기
    //   userService             : 구글 이메일로 우리 회원(User) 찾기
    //
    // ※ OAuth2SuccessHandler 클래스에는 @Component 도 붙어 있어서, 스프링이 객체를 하나 더 만듭니다.
    //   빈 이름이 달라서(oAuth2SuccessHandler / OAuth2SuccessHandler) 충돌은 나지 않습니다.
    //   실제로 보안 설정에 연결되는 것은 위 successHandler(oAuth2SuccessHandler()) 로 넘긴, 이 메서드의 객체입니다.
    @Bean
    public OAuth2SuccessHandler oAuth2SuccessHandler() {
        return new OAuth2SuccessHandler(tokenProvider,
                                        refreshTokenRepository,
                                        oAuth2AuthorizationRequestBasedOnCookieRepository(),
                                        userService
        );
    }


    // ------------------------------------------------------------------------
    // tokenAuthenticationFilter(): 우리 토큰 검사 필터를 빈으로 만들기
    // ------------------------------------------------------------------------
    // 위 addFilterBefore 에서 이 메서드를 불러 필터를 넣습니다.
    // @Configuration 클래스 안에서는 @Bean 메서드를 여러 번 불러도 같은 객체(싱글톤)가 돌아옵니다.
    //
    // ※ 필터를 빈으로 등록하면, 스프링 부트가 이 필터를 "일반 서블릿 필터"로도 한 번 더 등록합니다.
    //   그래도 TokenAuthenticationFilter 는 OncePerRequestFilter 를 물려받았으므로
    //   한 요청에서 두 번째로 불리면 건너뛰고, 실제 검사는 한 번만 일어납니다.
    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter(tokenProvider);
    }


    // ------------------------------------------------------------------------
    // oAuth2AuthorizationRequestBasedOnCookieRepository(): 인가 요청 쿠키 보관함 빈
    // ------------------------------------------------------------------------
    // 위에서 두 군데 씁니다.
    //   1. authorizationEndpoint(...)  : 흐름 [2] 에서 저장, 흐름 [4] 에서 꺼내기
    //   2. oAuth2SuccessHandler()      : 흐름 [5] 에서 쿠키 지우기
    // 두 곳 모두 같은 객체를 받습니다. (@Configuration 의 싱글톤 보장)
    @Bean
    public OAuth2AuthorizationRequestBasedOnCookieRepository oAuth2AuthorizationRequestBasedOnCookieRepository() {
        return new OAuth2AuthorizationRequestBasedOnCookieRepository();
    }


    // ------------------------------------------------------------------------
    // passwordEncoder(): 비밀번호 암호화 도구 빈
    // ------------------------------------------------------------------------
    // 10장 WebSecurityConfig 에 있던 빈을 그대로 옮겨 왔습니다.
    // 구글 로그인 사용자는 비밀번호가 없으므로(users.password = null) 이 빈은 거의 쓰이지 않습니다.
    // ※ UserService 는 순환 참조 때문에 이 빈을 주입받지 않고 BCryptPasswordEncoder 를 직접 만듭니다. (위 userService 설명 참고)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}