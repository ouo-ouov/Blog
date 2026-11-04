

package com.shinsunyoung.blog.controller;

import com.shinsunyoung.blog.config.jwt.JwtFactory;
import com.shinsunyoung.blog.config.jwt.JwtProperties;
import com.shinsunyoung.blog.domain.RefreshToken;
import com.shinsunyoung.blog.domain.User;
import com.shinsunyoung.blog.dto.CreateAccessTokenRequest;
import com.shinsunyoung.blog.repository.RefreshTokenRepository;
import com.shinsunyoung.blog.repository.UserRepository;
import com.shinsunyoung.blog.config.jwt.JwtProperties;                 // 설정 파일의 jwt 값(발급자, 비밀키)을 담는 클래스입니다.
import com.shinsunyoung.blog.domain.RefreshToken;                      // refresh_token 테이블의 한 행에 해당하는 클래스입니다.
import com.shinsunyoung.blog.domain.User;                              // users 테이블의 한 행에 해당하는 클래스입니다.
import com.shinsunyoung.blog.dto.CreateAccessTokenRequest;             // 요청 본문(JSON)과 대응하는 클래스입니다.
import com.shinsunyoung.blog.config.jwt.JwtFactory;                           // 테스트에서 토큰을 쉽게 만들기 위한 클래스입니다.
import com.shinsunyoung.blog.repository.RefreshTokenRepository;        // refresh_token 테이블에 저장하고 조회하는 인터페이스입니다.
import com.shinsunyoung.blog.repository.UserRepository;                // users 테이블에 저장하고 조회하는 인터페이스입니다.
import org.junit.jupiter.api.BeforeEach;                                             // 각 테스트 메서드가 실행되기 직전에 자동으로 실행되는 메서드를 표시합니다.
import org.junit.jupiter.api.DisplayName;                                            // 테스트 결과 화면에 보일 이름을 정합니다.
import org.junit.jupiter.api.Test;                                                   // 이 메서드가 테스트 메서드임을 표시합니다.
import org.springframework.beans.factory.annotation.Autowired;                       // 스프링이 객체를 찾아서 변수에 넣어 주도록 표시합니다.
import org.springframework.boot.test.context.SpringBootTest;                         // 스프링 프로그램 전체를 실행한 상태로 테스트하도록 표시합니다.
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;     // MockMvc 객체를 스프링이 자동으로 만들도록 표시합니다.
import org.springframework.http.MediaType;                                           // 데이터 종류(JSON 등)를 나타내는 값 모음입니다.
import org.springframework.test.web.servlet.MockMvc;                                 // 실제 서버 없이 HTTP 요청을 보내는 객체의 타입입니다.
import org.springframework.test.web.servlet.ResultActions;                           // 요청을 보낸 뒤의 결과를 담는 객체의 타입입니다.
import org.springframework.test.web.servlet.setup.MockMvcBuilders;                   // MockMvc 객체를 만드는 도구 클래스입니다.
import org.springframework.web.context.WebApplicationContext;                        // 스프링 웹 설정 정보 전체를 담는 객체의 타입입니다.
import tools.jackson.databind.ObjectMapper;                                          // 자바 객체와 JSON 문자열을 서로 바꾸는 객체의 타입입니다.

import java.util.Map;                                                                // "이름과 값" 짝을 담는 자료형입니다.

import jakarta.servlet.http.Cookie;                                                       // [로그아웃 추가] 요청에 붙일 쿠키
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;   // [로그아웃 추가] 로그인 상태 흉내
import org.springframework.security.core.context.SecurityContext;                         // [로그아웃 추가]
import org.springframework.security.core.context.SecurityContextHolder;                   // [로그아웃 추가]

import static org.assertj.core.api.Assertions.assertThat;                                 // [로그아웃 추가] 결과 비교
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete; // [로그아웃 추가] DELETE 요청
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;   // [로그아웃 추가] 응답 쿠키 검사
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;     // POST 요청을 만드는 메서드 post() 를 바로 쓰도록 가져옵니다.
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;   // JSON 안의 값을 찾는 메서드 jsonPath() 를 바로 쓰도록 가져옵니다.
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;     // HTTP 상태 코드를 검사하는 메서드 status() 를 바로 쓰도록 가져옵니다.


// 파일 위치: src/test/java/me/shinsunyoung/springbootdeveloper/controller/TokenApiControllerTest.java
//
// [이 클래스가 하는 일]
//   POST /api/token 요청을 보냈을 때, 서버가 201 응답과 accessToken 을 돌려주는지 자동으로 확인합니다.
//   실제 톰캣 서버를 켜지 않고, MockMvc 라는 객체로 요청을 흉내 내서 보냅니다.
//
// [이 클래스의 구성]
//   어노테이션 2개 : @SpringBootTest, @AutoConfigureMockMvc
//   변수 6개        : mockMvc, objectMapper, context, jwtProperties, userRepository, refreshTokenRepository
//   메서드 2개      : mockMvcSetUp (준비 메서드), createNewAccessToken (테스트 메서드)

// ============================================================
// @SpringBootTest
//   뜻  : 스프링 프로그램 전체를 실행한 상태에서 테스트합니다.
//   이유 : 컨트롤러, 서비스, 리포지터리 객체가 모두 실제로 만들어져 연결되어야 하기 때문입니다.
//   참고 : 실제 서버 포트는 열지 않고, 요청을 흉내 낼 수 있는 가짜 웹 환경을 테스트 할때 사용합니다.
// ============================================================
@SpringBootTest

// ============================================================
// @AutoConfigureMockMvc
//   뜻  : MockMvc 객체를 스프링이 자동으로 만들어서 준비해 줍니다.
//   참고 : 이 상태에서는 보안 설정(필터)도 함께 포함됩니다.
//          단, 아래 mockMvcSetUp 메서드에서 mockMvc 를 새 객체로 바꾸므로, 실제 테스트에서는 보안 설정이 빠집니다.
// ============================================================
@AutoConfigureMockMvc
class TokenApiControllerTest {

    // ============================================================
    // 변수 1: mockMvc
    //   하는 일 : 톰캣 서버를 켜지 않고 HTTP 요청을 가짜로 보내고 응답을 받는 일을 합니다.
    //   값 넣기 : @Autowired 가 스프링에게 "MockMvc 객체를 찾아서 여기에 넣어라"라고 알려 줍니다.
    //   바뀜    : mockMvcSetUp 메서드가 실행될 때마다 새 MockMvc 객체로 바뀝니다.
    // ============================================================
    @Autowired
    protected MockMvc mockMvc;

    // ============================================================
    // 변수 2: objectMapper
    //   하는 일 : 자바 객체를 JSON 문자열로 바꿉니다.
    //             예) request 객체 → {"refreshToken":"..."}
    //   값 넣기 : @Autowired 로 스프링이 만들어 둔 ObjectMapper 객체를 넣습니다.
    // ============================================================
    @Autowired
    protected ObjectMapper objectMapper;

    // ============================================================
    // 변수 3: context
    //   하는 일 : 스프링 웹 설정 정보 전체를 담고 있습니다.
    //             mockMvcSetUp 메서드에서 MockMvc 를 만들 때 이 정보를 사용합니다.
    //   값 넣기 : @Autowired 로 스프링이 넣어 줍니다.
    // ============================================================
    @Autowired
    private WebApplicationContext context;

    // ============================================================
    // 변수 4: jwtProperties
    //   하는 일 : application.yml 의 jwt 설정값(발급자 이름, 비밀키)을 담고 있습니다.
    //             토큰을 만들 때 JwtFactory 가 이 값을 사용합니다.
    //   값 넣기 : @Autowired 로 스프링이 설정값을 읽어서 넣어 줍니다.
    // ============================================================
    @Autowired
    JwtProperties jwtProperties;

    // ============================================================
    // 변수 5: userRepository
    //   하는 일 : users 테이블에 사용자를 저장(save)하거나 모두 지우기(deleteAll)를 합니다.
    //   값 넣기 : @Autowired 로 스프링이 구현 객체를 만들어서 넣어 줍니다.
    //             개발자가 new 로 직접 만들지 않습니다.
    // ============================================================
    @Autowired
    UserRepository userRepository;

    // ============================================================
    // 변수 6: refreshTokenRepository
    //   하는 일 : refresh_token 테이블에 리프레시 토큰을 저장(save)합니다.
    //   값 넣기 : @Autowired 로 스프링이 구현 객체를 만들어서 넣어 줍니다.
    // ============================================================
    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    // ============================================================
    // 메서드 1: mockMvcSetUp
    //   어노테이션 @BeforeEach : 테스트 메서드 하나가 실행되기 직전에 항상 먼저 실행됩니다.
    //   목적 : 테스트 하나가 끝난 뒤 남은 데이터가 다음 테스트에 영향을 주지 않도록 준비합니다.
    // ============================================================
    @BeforeEach
    public void mockMvcSetUp() {

        // 1) mockMvc 변수에 새 MockMvc 객체를 넣습니다.
        //    MockMvcBuilders.webAppContextSetup(context) : context 정보를 바탕으로 MockMvc 를 만들 준비를 합니다.
        //    .build()                                    : 준비된 내용으로 MockMvc 객체를 완성합니다.
        //    결과를 this.mockMvc 에 넣으면, 기존에 @Autowired 로 들어 있던 객체가 새 객체로 바뀝니다.
        //    참고: 이 방법으로 만든 MockMvc 에는 보안 필터가 자동으로 들어가지 않습니다.
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        // 2) users 테이블의 모든 행을 삭제합니다.
        //    userRepository.deleteAll() 은 users 테이블 전체를 지웁니다.
        //    이전 테스트에서 저장한 사용자가 남아 있지 않도록 하기 위해서입니다.
        //    참고: refresh_token 테이블의 행은 지우지 않습니다.
        userRepository.deleteAll();
    }

    // ============================================================
    // 메서드 2: createNewAccessToken
    //   어노테이션 @Test        : 테스트 실행 도구가 이 메서드를 실행합니다.
    //   어노테이션 @DisplayName : 테스트 결과 화면에 보일 이름을 정합니다.
    //   throws Exception        : 이 메서드 안에서 발생할 수 있는 예외를 밖으로 넘깁니다.
    // ============================================================
    @DisplayName("createNewAccessToken: 새로운 액세스 토큰을 발급한다.")
    @Test
    public void createNewAccessToken() throws Exception {

        // ------------------------------------------------------------
        // [준비] 테스트에 필요한 값과 데이터를 만듭니다.
        // ------------------------------------------------------------

        // 지역 변수 url: 요청을 보낼 주소입니다. 값은 "/api/token" 입니다.
        //   final 은 한 번 넣은 값을 나중에 바꾸지 못하게 합니다.
        //   이 주소는 TokenApiController 의 @PostMapping 주소와 같아야 요청이 연결됩니다.
        final String url = "/api/token";

        // 지역 변수 testUser: 테스트용 사용자입니다.
        //   User.builder() : 사용자를 만드는 빌더를 꺼냅니다.
        //   .email(...)    : 이메일 값을 넣습니다. 값은 "user@gmail.com" 입니다.
        //   .password(...) : 비밀번호 값을 넣습니다. 값은 "test" 입니다. (암호화하지 않고 넣습니다.)
        //   .build()       : 넣은 값으로 User 객체를 만듭니다. 이 시점에는 id 가 비어 있습니다(null).
        //   userRepository.save(...) : users 테이블에 저장합니다.
        //                              저장되면서 데이터베이스가 id 값을 정하고, 그 값이 객체에 채워져 돌아옵니다.
        User testUser = userRepository.save(User.builder()
                                            .email("user@gmail.com")
                                            .password("test")
                                            .build());

        // 지역 변수 refreshToken: 리프레시 토큰 문자열입니다.
        //   JwtFactory.builder()        : 토큰을 만드는 재료 빌더를 꺼냅니다.
        //   .claims(Map.of("id", ...))  : 토큰 안에 "id" 항목을 넣습니다. 값은 testUser 의 id 입니다.
        //   .build()                    : 재료를 모아 JwtFactory 객체를 만듭니다.
        //   .createToken(jwtProperties) : 재료로 토큰 문자열을 만듭니다. 결과 예: eyJhbGciOi....
        String refreshToken = JwtFactory.builder()
                                        .claims(Map.of("id", testUser.getId()))
                                        .build()
                                        .createToken(jwtProperties);

        // refresh_token 테이블에 리프레시 토큰을 저장합니다.
        //   new RefreshToken(userId, refreshToken) : 사용자 id 와 토큰 문자열을 담은 객체를 만듭니다.
        //   refreshTokenRepository.save(...)       : 그 객체를 refresh_token 테이블에 저장합니다.
        //   저장된 이 토큰은 서버가 요청을 처리할 때 다시 조회합니다.
        refreshTokenRepository.save(new RefreshToken(testUser.getId(), refreshToken));

        // 요청 본문에 넣을 객체 request 를 만듭니다.
        //   new CreateAccessTokenRequest() : 빈 객체를 만듭니다.
        //   request.setRefreshToken(...)   : 객체 안의 refreshToken 값에 토큰 문자열을 넣습니다.
        CreateAccessTokenRequest request = new CreateAccessTokenRequest();
        request.setRefreshToken(refreshToken);

        // 지역 변수 requestBody: request 객체를 JSON 문자열로 바꾼 값입니다.
        //   objectMapper.writeValueAsString(request) 의 결과는 다음과 같은 모양입니다.
        //   {"refreshToken":"eyJhbGciOi...."}
        final String requestBody = objectMapper.writeValueAsString(request);

        // ------------------------------------------------------------
        // [실행] 요청을 보냅니다.
        // ------------------------------------------------------------

        // ResultActions 변수 resultActions: 요청을 보낸 결과를 담습니다.
        //   post(url)                              : POST 방식으로 url 주소에 요청을 준비합니다.
        //   .contentType(MediaType.APPLICATION_JSON_VALUE) : 보내는 데이터가 JSON 이라고 알려 줍니다.
        //   .content(requestBody)                  : 요청 본문에 JSON 문자열을 넣습니다.
        //   mockMvc.perform(...)                   : 준비한 요청을 실제로 보내고 결과를 돌려받습니다.
        //
        //   서버 안에서 일어나는 일 (순서대로):
        //     1) "/api/token" 주소의 TokenApiController 메서드를 찾습니다.
        //     2) 요청 본문의 JSON 을 CreateAccessTokenRequest 객체로 바꿉니다.
        //     3) 컨트롤러가 TokenService 를 호출해서 새 액세스 토큰을 만듭니다.
        //     4) 결과를 201 상태 코드와 함께 JSON 으로 응답합니다.
        ResultActions resultActions = mockMvc.perform(post(url)
                                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                                            .content(requestBody));

        // ------------------------------------------------------------
        // [검증] 응답이 기대한 값인지 확인합니다.
        // ------------------------------------------------------------

        // status().isCreated()
        //   응답 상태 코드가 201(Created)인지 확인합니다.
        //   201 이 아니면 테스트는 실패합니다.
        //
        // jsonPath("$.accessToken").isNotEmpty()
        //   응답 JSON 에서 accessToken 항목의 값을 찾습니다.
        //   $ 는 JSON 의 가장 바깥 객체를 뜻합니다.
        //   그 값이 비어 있지 않은지(null 이나 빈 문자열이 아닌지) 확인합니다.
        //
        // andExpect 는 순서대로 검사하고, 하나라도 틀리면 테스트가 실패합니다.
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    // ═════════════════════════════════════════════════════════════════════════════════════════════
    // [로그아웃 추가] deleteRefreshToken : 로그아웃하면 DB 의 리프레시 토큰이 삭제되는지 테스트
    //   연결된 컨트롤러 메소드 : TokenApiController.deleteRefreshToken()  (DELETE /api/refresh-token)
    // ═════════════════════════════════════════════════════════════════════════════════════════════
    @DisplayName("deleteRefreshToken: 리프레시 토큰을 삭제한다.")
    @Test
    public void deleteRefreshToken() throws Exception {
        // ───────────── given 1 : 요청 주소 준비 ─────────────
        final String url = "/api/refresh-token";

        // ───────────── given 2 : 테스트 사용자 저장 ─────────────
        //   이메일이 createNewAccessToken 테스트와 같아도 됩니다. mockMvcSetUp() 이 테스트마다 users 테이블을 비우기 때문입니다.
        User testUser = userRepository.save(User.builder()
                                            .email("user@gmail.com")
                                            .password("test")
                                            .build());

        // ───────────── given 3 : 이 사용자의 토큰 만들기 ─────────────
        //   claim "id" 에 사용자 id 를 넣습니다. RefreshTokenService.delete() 가 이 값을 꺼내 누구의 토큰을 지울지 정합니다.
        String token = JwtFactory.builder()
                                 .claims(Map.of("id", testUser.getId()))
                                 .build()
                                 .createToken(jwtProperties);

        // ───────────── given 4 : DB 에 리프레시 토큰 저장 (로그인 성공 직후 상태 흉내) ─────────────
        //   실제로는 구글 로그인 성공 시 OAuth2SuccessHandler.saveRefreshToken() 이 저장합니다.
        refreshTokenRepository.save(new RefreshToken(testUser.getId(), token));

        // ───────────── given 5 : SecurityContext 에 인증 정보 넣기 (로그인 상태 흉내) ─────────────
        //   이 테스트의 MockMvc 는 보안 필터(TokenAuthenticationFilter)를 거치지 않으므로, 필터가 해 줄 일을 직접 합니다.
        //   new UsernamePasswordAuthenticationToken(principal, credentials, 권한목록)
        //     credentials 자리에 토큰 문자열을 넣는 이유 :
        //       RefreshTokenService.delete() 가 authentication.getCredentials() 로 토큰을 꺼내 사용자 id 를 읽기 때문입니다.
        //       (실제 요청에서도 TokenProvider.getAuthentication() 이 credentials 에 액세스 토큰을 넣습니다)
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(testUser, token, testUser.getAuthorities()));

        // ───────────── when : 로그아웃 요청 (DELETE /api/refresh-token) ─────────────
        //   이 줄 안에서 일어나는 일:
        //     (1) TokenApiController.deleteRefreshToken() 실행
        //     (2) RefreshTokenService.delete() → getCredentials() 로 토큰 꺼냄 → getUserId(token) → deleteByUserId(id)
        //         → SQL: select ... from refresh_token where user_id=?  →  delete from refresh_token where id=?
        //     (3) CookieUtil.deleteCookie(...) : 요청에 쿠키가 없으므로 아무것도 하지 않음
        //     (4) 200 응답
        mockMvc.perform(delete(url).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

        // ───────────── then : DB 에서 그 리프레시 토큰이 사라졌는지 확인 ─────────────
        //   findByRefreshToken(token) 결과(Optional)가 비어 있으면(isEmpty) 삭제 성공입니다.
        assertThat(refreshTokenRepository.findByRefreshToken(token)).isEmpty();
    }

    // ═════════════════════════════════════════════════════════════════════════════════════════════
    // [로그아웃 추가] deleteRefreshTokenCookie : 로그아웃 응답이 브라우저의 refresh_token 쿠키도 지우는지 테스트
    // ═════════════════════════════════════════════════════════════════════════════════════════════
    @DisplayName("deleteRefreshToken: 로그아웃 응답에 refresh_token 쿠키 삭제(Max-Age=0)가 들어 있다.")
    @Test
    public void deleteRefreshTokenCookie() throws Exception {
        // given : 사용자 · 토큰 · DB 저장 · 인증 정보 (위 테스트와 같은 준비)
        User testUser = userRepository.save(User.builder().email("user@gmail.com").password("test").build());
        String token = JwtFactory.builder().claims(Map.of("id", testUser.getId())).build().createToken(jwtProperties);
        refreshTokenRepository.save(new RefreshToken(testUser.getId(), token));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(testUser, token, testUser.getAuthorities()));

        // when : 브라우저처럼 refresh_token 쿠키를 붙여서 로그아웃 요청
        //   new Cookie("refresh_token", token) : OAuth2SuccessHandler 가 로그인 때 만들어 준 쿠키를 흉내 냅니다.
        // then :
        //   cookie().maxAge("refresh_token", 0) : 응답의 Set-Cookie 에 refresh_token 의 Max-Age 가 0 → 브라우저가 쿠키를 지움
        //   cookie().path("refresh_token", "/") : 처음 만들 때와 같은 Path=/ 여야 같은 쿠키로 보고 지웁니다.
        mockMvc.perform(delete("/api/refresh-token").cookie(new Cookie("refresh_token", token)))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("refresh_token", 0))
                .andExpect(cookie().path("refresh_token", "/"));
    }
}

// ============================================================
// 정리
//   변수 6개: mockMvc, objectMapper, context, jwtProperties, userRepository, refreshTokenRepository
//   각 변수는 @Autowired 로 스프링이 값을 넣어 줍니다.
//   mockMvcSetUp 은 테스트마다 실행되어 mockMvc 를 새로 만들고 users 테이블을 비웁니다.
//   createNewAccessToken 은 사용자 저장 → 토큰 만들기 → 요청 보내기 → 결과 확인 순서로 동작합니다.
// ============================================================
