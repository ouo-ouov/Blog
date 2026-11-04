// 파일 위치: src/main/java/me/shinsunyoung/springbootdeveloper/config/oauth/OAuth2UserCustomService.java
//
// ============================================================================
// 이 파일이 하는 일  [11장 · 구글 사용자 정보 → 우리 회원으로 저장]
// ============================================================================
// 구글 로그인 중간에, 스프링 시큐리티가 구글에서 사용자 정보(이메일, 이름 등)를 받아 옵니다.
// 이 클래스는 그 정보를 우리 데이터베이스의 users 표(테이블)에 저장합니다.
//   - 처음 온 사용자  → 새 회원으로 저장 (email, nickname)
//   - 이미 있는 사용자 → 이름(nickname)만 최신 구글 이름으로 바꿔 저장
//
// ============================================================================
// 언제 불리나요? (WebOAuthSecurityConfig 의 흐름 [4])
// ============================================================================
//   구글 ── GET /login/oauth2/code/google?code=… ──▶ OAuth2LoginAuthenticationFilter
//     1. code → 구글 액세스 토큰으로 교환 (시큐리티가 자동으로 함)
//     2. 시큐리티가 loadUser(userRequest) 를 부름   ← 지금 이 클래스
//          super.loadUser() : 구글 사용자 정보 API(userinfo)를 호출해 정보를 받아 옴
//          saveOrUpdate()   : users 표(테이블)에 저장
//     3. 돌려준 OAuth2User 가 "로그인한 사용자"로 Authentication 에 담김
//     4. OAuth2SuccessHandler 가 불림 → 여기서 저장한 회원을 이메일로 다시 찾아 JWT 발급
//
//   연결 위치: WebOAuthSecurityConfig
//     .userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint.userService(oAuth2UserCustomService))
//
// ============================================================================
// 구글이 보내 주는 사용자 정보(attributes) 예시
// ============================================================================
//   {
//     "sub"            : "1098765432109876543",   ← 구글 내부 사용자 번호
//     "name"           : "홍길동",                  ← 이 값을 nickname 으로 저장
//     "given_name"     : "길동",
//     "family_name"    : "홍",
//     "picture"        : "https://lh3.googleusercontent.com/…",
//     "email"          : "gildong@gmail.com",     ← 이 값을 email 로 저장 (우리 회원을 찾는 열쇠)
//     "email_verified" : true,
//     "locale"         : "ko"
//   }
//   application.yml 의 scope 가 email, profile 이라서 이메일·이름·사진 정보가 들어옵니다.
//
// ============================================================================
// 꼭 알아둘 낱말
// ============================================================================
// DefaultOAuth2UserService : 시큐리티가 기본으로 주는 "사용자 정보 받아 오기" 서비스입니다.
//                            이 클래스는 그것을 물려받아(extends) 저장 기능만 덧붙였습니다.
// OAuth2UserRequest        : 사용자 정보를 받으러 갈 때 필요한 재료 묶음입니다.
//                            (어느 서비스인지(google), 구글 액세스 토큰 등)
// OAuth2User               : 구글에서 받은 사용자 한 명을 나타내는 객체입니다. getAttributes() 로 위 정보를 꺼냅니다.
// attributes               : 사용자 정보가 "이름표(key) → 값(value)" 모양으로 담긴 Map 입니다.
//
// ============================================================================
// 꼭 조심할 점
// ============================================================================
// 1. 구글 회원은 비밀번호가 없으므로 users.password 는 null 로 저장됩니다.
//    (User 의 password 열에는 nullable = false 가 없어서 저장할 수 있습니다.)
// 2. ⚠️ User 의 nickname 열은 unique = true 입니다.
//    이름이 같은 다른 구글 계정("홍길동" 두 명)이 로그인하면, 두 번째 저장에서 중복 오류가 나서 로그인에 실패합니다.
// 3. loadUser() 는 우리 User 가 아니라 구글의 OAuth2User 를 그대로 돌려줍니다.
//    그래서 OAuth2SuccessHandler 는 이메일로 우리 User 를 한 번 더 찾습니다.
// ============================================================================

// 이 파일이 속한 폴더 이름입니다. OAuth2(구글 로그인) 관련 클래스를 모아 두는 config/oauth 폴더입니다.
package com.shinsunyoung.blog.config.oauth;

import com.shinsunyoung.blog.repository.UserRepository;
import lombok.RequiredArgsConstructor;                                            // final 변수를 받는 생성자를 자동으로 만들어 줍니다.
import com.shinsunyoung.blog.domain.User;                           // 우리 회원 클래스(users 표)입니다.
import com.shinsunyoung.blog.repository.UserRepository;             // users 표에 저장·조회하는 도구입니다.
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService; // 시큐리티 기본 "구글 사용자 정보 받아 오기" 서비스입니다.
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;     // 사용자 정보를 요청할 때의 재료(등록 정보 + 구글 액세스 토큰)입니다.
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;    // OAuth2 로그인 중 문제가 생기면 나는 예외입니다.
import org.springframework.security.oauth2.core.user.OAuth2User;                  // 구글에서 받은 사용자 한 명을 나타냅니다.
import org.springframework.stereotype.Service;                                    // 이 클래스를 스프링 빈(서비스)으로 등록하는 표시입니다.

import java.util.Map; // "이름표 → 값" 묶음을 담는 자바 기본 자료형입니다.

// @RequiredArgsConstructor : userRepository 를 받는 생성자를 만들어 줍니다.
// @Service                 : 스프링이 이 객체를 만들어 관리합니다. WebOAuthSecurityConfig 가 주입받아 씁니다.
// extends DefaultOAuth2UserService : 기본 서비스의 기능(구글에 사용자 정보 묻기)을 그대로 물려받습니다.
@RequiredArgsConstructor
@Service
public class OAuth2UserCustomService extends DefaultOAuth2UserService {

    // users 테이블를 다루는 도구입니다. 스프링이 넣어 줍니다.
    private final UserRepository userRepository;

    // ------------------------------------------------------------------------
    // loadUser: 시큐리티가 "사용자 정보 줘" 하고 부르는 메서드 (부모 메서드를 다시 정의함)
    // ------------------------------------------------------------------------
    // 받는 값   : userRequest (어느 서비스인지 + 구글 액세스 토큰)
    // 돌려주는 값: 구글 사용자 정보가 담긴 OAuth2User
    // 예외      : 구글 응답에 문제가 있으면 OAuth2AuthenticationException → 로그인 실패 처리
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // [1] 부모(DefaultOAuth2UserService)의 기능으로 구글 사용자 정보 API 를 호출합니다.
        //     내부적으로 "GET https://www.googleapis.com/oauth2/v3/userinfo" + 구글 액세스 토큰 요청이 갑니다.
        OAuth2User user = super.loadUser(userRequest);
        // [2] 받은 정보로 users 표에 저장하거나 이름을 갱신합니다.
        saveOrUpdate(user);

        // [3] 구글 사용자 정보를 그대로 돌려줍니다. 시큐리티가 이것을 로그인 결과(principal)로 씁니다.
        return user;
    }

    // ------------------------------------------------------------------------
    // saveOrUpdate: 있으면 이름 갱신(update), 없으면 새로 저장(save)
    // ------------------------------------------------------------------------
    // 받는 값   : 구글에서 받은 OAuth2User
    // 돌려주는 값: 저장된 우리 User (지금은 돌려준 값을 쓰는 곳은 없습니다)
    private User saveOrUpdate(OAuth2User oAuth2User) {
        // 구글 사용자 정보를 Map 으로 꺼냅니다. (위 "attributes 예시" 참고)
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // Map 의 값은 Object 타입이라 (String) 으로 바꿔 꺼냅니다.
        String email = (String) attributes.get("email"); // 예) "gildong@gmail.com"
        String name = (String) attributes.get("name");   // 예) "홍길동"

        // findByEmail(email) : 같은 이메일 회원이 있는지 찾습니다. 결과는 Optional(있을 수도, 없을 수도).
        //   .map(entity -> entity.update(name))
        //       있으면 → 그 회원의 nickname 을 구글 이름으로 바꾼 객체를 씁니다.
        //   .orElse(User.builder()…build())
        //       없으면 → 이메일·닉네임으로 새 회원 객체를 만듭니다. (password 는 넣지 않음 → null)
        // ※ orElse(…) 안의 새 객체는 "있든 없든" 항상 먼저 만들어집니다. 다만 쓰이지는 않으므로 결과에는 영향이 없습니다.
        User user = userRepository.findByEmail(email)
                .map(entity -> entity.update(name))
                .orElse(User.builder()
                        .email(email)
                        .nickname(name)
                        .build());

        // save(): id 가 없는 새 객체면 INSERT, 이미 있는 회원이면 UPDATE 를 실행합니다.
        //   새 회원 예) INSERT INTO users (email, nickname, password) VALUES ('gildong@gmail.com', '홍길동', NULL)
        //   기존 회원 예) UPDATE users SET nickname = '홍길동', … WHERE id = 1
        return userRepository.save(user);
    }
}