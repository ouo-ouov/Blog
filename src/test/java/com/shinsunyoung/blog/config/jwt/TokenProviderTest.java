package com.shinsunyoung.blog.config.jwt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.Jwts;                                                           // 토큰을 읽는 도구입니다. 여기서는 테스트 안에서 토큰 내용을 직접 확인할 때 씁니다.
import com.shinsunyoung.blog.config.jwt.JwtProperties;                      // 설정 파일(application.yml)의 발급자와 비밀키를 담은 클래스입니다.
import com.shinsunyoung.blog.config.jwt.TokenProvider;                  // 테스트할 대상입니다. 토큰을 만들고 확인하는 클래스입니다.
import com.shinsunyoung.blog.domain.User;                               // 우리 프로젝트의 사용자 클래스입니다. 데이터베이스와 연결되어 있습니다.
import com.shinsunyoung.blog.repository.UserRepository;                 // 사용자를 데이터베이스에 저장하고 찾는 도구입니다.
import org.junit.jupiter.api.DisplayName;                                             // 테스트 결과 화면에 보일 설명 글을 붙이는 표시입니다.
import org.junit.jupiter.api.Test;                                                    // 이 메서드가 테스트 메서드라는 것을 알려 주는 표시입니다.
import org.springframework.beans.factory.annotation.Autowired;                        // 스프링이 객체를 만들어서 넣어 달라는 표시입니다.
import org.springframework.boot.test.context.SpringBootTest;                          // 프로그램 전체를 실제로 실행해서 테스트하라는 표시입니다.
import org.springframework.security.core.Authentication;                              // 스프링 시큐리티의 인증 정보 타입입니다.
import org.springframework.security.core.userdetails.UserDetails;                     // 스프링 시큐리티의 사용자 정보 타입입니다. 사용자 이름을 꺼낼 때 씁니다.
import java.time.Duration;                                                            // "14일", "2시간"처럼 기간을 나타내는 타입입니다.
import java.util.Date;                                                                // 날짜와 시각을 나타내는 타입입니다.
import java.util.Map;                                                                 // "이름과 값"을 짝지어 모아 두는 타입입니다.
import static org.assertj.core.api.Assertions.assertThat;                             // 결과를 검사하는 assertThat 명령을 바로 쓸 수 있게 가져옵니다.


// @SpringBootTest
//   뜻: 프로그램 전체를 실제로 켠 상태에서 테스트합니다.
//   이유: TokenProvider, UserRepository, JwtProperties 같은 객체가 스프링컨테이너 의해 객체가 준비되어야 하기 때문입니다.
@SpringBootTest
class TokenProviderTest {

    // @Autowired
    //   뜻: 스프링이 TokenProvider 객체를 만들어서 이 변수에 넣어 줍니다.
    //   이유: 테스트 코드가 직접 new TokenProvider() 로 만들지 않고 받아서 쓰기 위해서입니다.
    @Autowired
    private TokenProvider tokenProvider;

    // 스프링이 UserRepository 객체를 넣어 줍니다. 사용자를 데이터베이스에 저장할 때 씁니다.
    @Autowired
    private UserRepository userRepository;

    // 스프링이 설정 파일(application.yml)의 jwt: 항목 값을 담은 JwtProperties 객체를 넣어 줍니다.
    // 테스트에서 JWT 토큰을 만들 때 비밀키가 필요하므로 여기서 받아 씁니다.
    @Autowired
    private JwtProperties jwtProperties;

    // ============================================================
    // 테스트 1: generateToken() 이  JWT 토큰을 제대로 만드는지 확인합니다.
    // 즉! 아래 getnerateToken() 메소드는 토큰을 제데로 생성하는지 TokenProvider에 만들어 놓은 generateToken() 메서드를 테스트하는 메서드 입니다.
    // ============================================================
    @Test
    void generateToken() {
        // ------------------------------------------------------------
        // [given] 준비 단계: 테스트에 필요한 사용자를 DB에 미리 넣어 둡니다.
        // ------------------------------------------------------------
        // ① User.builder() : 사용자 User 엔티티 객체를 만드는 UserBuilder객체를 꺼냅니다.
        //    .email("user@gmail.com") : 사용자 User 엔티티 객체의 이메일 칸에 이 값을 넣습니다.
        //    .password("test")        : 사용자 User 엔티티  객체의 비밀번호 칸에 "test" 를 넣습니다.
        //                               (이 테스트는 암호화 없이 그대로 넣습니다. 실제 가입 과정과 다른 점입니다.)
        //    .build()                 : 지금까지 넣은 값으로 사용자 User 엔티티 객체 한 개를 완성합니다.
        //                               이 시점에는 아직 데이터베이스에 저장되지 않았고, id 는 비어 있습니다(null).
        //
        // userRepository.save(...) : 완성된 사용자 User 엔티티 객체를 데이터베이스 users 표(테이블)에 저장합니다.
        //    실행 중에 일어나는 일:
        //      (a) 스프링 데이터 JPA 가 "id 가 비어 있으니 새 사용자"라고 판단합니다.
        //      (b) users 표에 새 줄을 넣는 SQL(INSERT)을 실행합니다.
        //      (c) 데이터베이스가 id 를 1, 2, 3 ... 순서로 자동으로 정합니다.
        //      (d) 저장된 결과(id 가 채워진 사용자 객체)를 돌려줍니다.
        // User testUser : 돌려받은 사용자 객체를 testUser 라는 이름의 변수에 담습니다.
        //    이후 testUser.getId() 로 방금 정해진 사용자 번호를 꺼낼 수 있습니다.
        User testUser = userRepository.save(User.builder()
                                                .email("user@gamil.com")
                                                .password("test")
                                                .build());
        // ------------------------------------------------------------
        // [when] 실행 단계: 테스트하려는 기능(토큰 만들기)을 실제로 실행합니다.
        // ------------------------------------------------------------
        // ③ tokenProvider.generateToken(testUser, Duration.ofDays(14)) 실행 중에 일어나는 일:
        //    (a) Duration.ofDays(14) : "14일"이라는 토큰 유지 기간을 만듭니다.
        //    (b) generateToken 내부에서 지금 시각(now)을 구합니다.
        //    (c) 만료 시각 = 지금 시각 + 14일 을 계산합니다. (숫자로 더한 뒤 날짜로 바꿉니다)
        //    (d) makeToken() 이 호출되어 토큰을 조립합니다.
        //        - 발급자(iss)   : application.yml 의 jwt.issuer 값
        //        - 발급 시각(iat): 지금 시각
        //        - 만료 시각(exp): (c) 에서 계산한 시각
        //        - 주인(sub)     : testUser 의 이메일 "user@gmail.com"
        //        - 사용자 번호(id): testUser 의 id 값
        //        - 서명         : 비밀키(jwt.secret_key) 와 HS256 방식으로 서명을 붙입니다.
        //    (e) 조립된 토큰은 "헤더.내용.서명" 모양의 긴 문자열 하나가 됩니다.
        //    (f) 그 문자열을 token 이라는 변수에 담습니다.
        String token = tokenProvider.generateToken(testUser, Duration.ofDays(14));

        // ------------------------------------------------------------
        // [then] 확인 단계: 만든 토큰을 다시 읽어서 사용자 id 가 맞는지 확인합니다.
        // ------------------------------------------------------------
        //    Jwts.parser()
        //      토큰을 읽는 JwtParser객체 도구를 준비합니다. 아직 아무 토큰도 읽지 않았습니다.
        //
        //    .setSigningKey(jwtProperties.getSecretKey())
        //      서명을 확인할 때 쓸 비밀키를 알려 줍니다.
        //      토큰을 만들 때 쓴 비밀키와 같은 값이어야 서명 확인이 통과합니다.
        //      값은 application.yml 의 jwt.secret_key 에서 읽어 온 것입니다.
        //
        //    .parseClaimsJws(token)
        //      토큰을 실제로 읽습니다. 이 순간 두 가지를 검사합니다.
        //        1) 서명 검사: 토큰 내용이 바뀌지 않았는지
        //        2) 기한 검사: 만료 시각(exp)이 지나지 않았는지
        //      검사를 통과하면 토큰 내용을 담은 Jws<Claims>객체를 돌려줍니다.
        //      하나라도 틀리면 예외(오류 신호)가 생기고, 아래 줄들은 실행되지 않습니다.
        //
        //    .getBody()
        //      검사를 통과한 토큰에서 "내용"(Claims객체의 정보) 부분만 꺼냅니다.
        //      내용(Claims객체의 정보) 안에는 발급자, 주인 이메일, 토큰 만료 시각, "id" 항목이 들어 있습니다.
        //
        //    .get("id", Long.class)
        //      내용(Claims객체의 정보) 안에서 id가  "id" 인 항목을 찾아 Long(큰 정수) 타입으로 꺼냅니다.
        //      방금 토큰을 만들 때 .claim("id", user.getId()) 로 넣어 둔 값입니다.
        Long userId = Jwts.parser()
                            .setSigningKey(jwtProperties.getSecretKey())
                            .parseClaimsJws(token)
                            .getBody()
                            .get("id", Long.class);

        // 검사: 토큰에서 꺼낸 번호(userId)와 처음 DB의 users테이블에 저장한 사용자 ID(testUser.getId())가 같은지 봅니다.
        //    같으면  : 테스트 통과 (초록)
        //    다르면  : 테스트 실패 (빨강). 실패 메시지에 Expected(기대 값)와 but was(실제 값)가 나옵니다.
        //    이 줄이 실행되면 테스트 한 개가 끝납니다. 같은 파일의 다른 테스트는 별도로 실행됩니다.
        assertThat(userId).isEqualTo(testUser.getId());
    }

    // ============================================================
    // 테스트 2: 기간이 지난 토큰을 validToken() 이 거절하는지 확인합니다.
    //  즉! validToken_invalidToken() 메서드는 토큰이 유효한 토큰인지 검증하는 메서드인???
    //  TokenProvider에 만들어 놓은 validToken() 메서드의 기능을 테스트 하는 메서드 입니다.
    // ============================================================
    // [이 테스트의 핵심 한 줄]
    //   "서명은 맞지만 기간이 지난 토큰"을 만들어서, validToken() 이 false 로 거절하는지 확인합니다.
    //
    // [테스트 시작 전에 이미 준비된 것]
    //   스프링 컨테이너에  tokenProvider 와 jwtProperties 객체를 넣어 둔 상태입니다.
    //   이 테스트는 데이터베이스를 쓰지 않습니다. 사용자를 저장하는 줄이 없습니다.
    //
    // ============================================================
    // 실행 순서 한눈에 보기
    // ============================================================
    //  ① 기간이 7일 지난 토큰을 만든다 (JwtFactory 로 만든다)
    //  ② 그 토큰을 validToken() 에 넣고 결과를 받는다
    //  ③ 결과가 false 인지 검사한다
    // ============================================================
    @Test
    void validToken_invalidToken() {
        // ------------------------------------------------------------
        // [given] 준비 단계: 유효기간이 지난 토큰을 만듭니다.
        // ------------------------------------------------------------
        // ① 아래 줄은 JwtFactory 로 토큰 재료를 고르고, 토큰 문자열을 만드는 과정입니다.
        //
        //    JwtFactory.builder()
        //      JwtFactory 를 만드는 도구(JwtFactoryBuilder객체)를 꺼냅니다.
        //      이 시점에 JwtFactory 의 변수들은 먼저 기본값으로 채워집니다.
        //        subject    = "test@email.com"
        //        issuedAt   = 지금 시각
        //        expiration = 지금부터 14일 뒤
        //        claims     = 비어 있는 모음
        //
        //    .expiration(new Date(...))
        //      만료 시각만 골라서 넣습니다. 여기서는 "7일 전" 시각입니다.
        //      계산 순서:
        //        1) new Date().getTime()       : 지금 시각을 밀리초 숫자로 바꿉니다.
        //        2) Duration.ofDays(7).toMillis(): 7일을 밀리초 숫자로 바꿉니다.
        //        3) 1) - 2)                     : 지금에서 7일을 뺀 과거 시각의 숫자가 됩니다.
        //        4) new Date(숫자)             : 그 숫자를 날짜 객체로 바꿉니다. 이것이 과거의 만료 시각입니다.
        //      나머지 세 값(주인, 발급 시각, 추가 항목)은 기본값을 그대로 씁니다.
        //
        //    .build()
        //      지금까지 고른 값으로 JwtFactory 객체를 완성합니다.
        //      생성자 안에서는 "값이 null 이 아니면 그 값, null 이면 기본값"을 쓰는 검사가 실행됩니다.
        //      이 테스트에서는 expiration() 만 값이 있으므로 그것만 바뀌고, 나머지는 기본값입니다.
        //
        //    .createToken(jwtProperties)
        //      완성된 재료로 JWT 토큰 문자열을 만듭니다. 이때 일어나는 일:
        //        - 주인(sub)      : "test@email.com"
        //        - 발급자(iss)    : application.yml 의 jwt.issuer 값 (jwtProperties 에서 읽음)
        //        - 발급 시각(iat) : JwtFactory 가 만들어진 시각
        //        - 만료 시각(exp) : 위에서 7일 전으로 정한 과거 시각
        //        - 서명           : jwt.secret_key 와 HS256 으로 서명을 붙입니다.
        //      결과: "서명은 정상이지만 만료 시각이 7일 전 과거인" 토큰 문자열이 만들어집니다.
        //      그 문자열을 token 변수에 담습니다.
        String token = JwtFactory.builder()
                                  .expiration(new Date(new Date().getTime() - Duration.ofDays(7).toMillis()))
                                  .build()
                                  .createToken(jwtProperties);

        // ------------------------------------------------------------
        // [when] 실행 단계: 유효 기간이 만료된 토큰을 validToken()메소드의 매개변수로 전달해 넣습니다.
        // ---------------------------------------------------------
        //tokenProvider.validToken(token) 안에서 일어나는 일:
        //    (a) try 블록에 들어갑니다. 여기서 문제가 생기면 catch 로 넘어갑니다.
        //    (b) Jwts.parser() 로 토큰을 읽는 도구를 준비하고, 비밀키를 알려 줍니다.
        //    (c) parseClaimsJws(token) 으로 토큰을 읽으면서 두 가지를 검사합니다.
        //        1) 서명 검사: 비밀키로 다시 계산한 서명이 토큰의 서명과 같은가?
        //           → 같습니다. 이 토큰은 우리 비밀키로 만들어졌고, 내용도 바뀌지 않았습니다.
        //        2) 토큰 유효 기한 검사: 만료 시각(exp)이 지금보다 이전인가?
        //           → 이전입니다. 7일 전이 만료 시각이므로 기간이 끝났습니다.
        //    (d) 기한 검사에서 실패하면 예외(ExpiredJwtException, "기간이 지났다"는 오류 신호)가 던져집니다.
        //        그 순간 아래의 "return true;" 는 실행되지 않습니다.
        //    (e) 예외는 catch (Exception e) 가 잡습니다. 그래서 catch 블록의 "return false;" 가 실행됩니다.
        //    (f) 결과 false 가 boolean result 변수에 담깁니다.
        //
        //    참고: validToken 은 "서명 문제"와 "기간 문제"를 구별하지 않고 모두 false 로 돌려줍니다.
        //          이 테스트는 기간 문제로 거절되는 경우를 확인하는 것입니다.
        boolean result = tokenProvider.validToken(token);


        // ------------------------------------------------------------
        // [then] 확인 단계: validToken()메소드 결과가 false(유효기간이 지난 토큰) 인지 검사합니다.
        // ------------------------------------------------------------
        // assertThat(result).isFalse() 는 "result 가 false 여야 한다"는 검사입니다.
        //    - result 가 false 이면: 기간이 지난 토큰을 제대로 거절한 것이므로 테스트 통과 (초록)
        //    - result 가 true 이면 : 기간이 지난 토큰을 통과시킨 것이므로 테스트 실패 (빨강)
        //                            실패 메시지에 Expected: false, but was: true 가 나옵니다.
        assertThat(result).isFalse();

    }

    // =============================================================================
    // 테스트 3: 기간이 남은 정상 토큰을 validToken() 메소드의 기능으로 검증 통과시키는지 확인합니다.
    // ==============================================================================
    @DisplayName("validToken() : 유효한 토큰인 경우에 유효성 검증에 성공하는지 테스트")
    @Test
    void validToken_validToken(){
        // ------------------------------------------------------------
        // [given] 준비: 정상 토큰을 만듭니다.
        // ------------------------------------------------------------
        // ① JwtFactory.withDefaultValues() : 값을 고르지 않고 기본값으로 JwtFactory 를 만듭니다.
        //                                     주인(sub)=test@email.com, 만료=지금부터 14일 뒤, 추가 항목=없음
        // ② .createToken(jwtProperties)    : 기본값으로 토큰 문자열을 만듭니다.
        //                                     발급자는 설정 파일 application.yml 값, 서명은 비밀키(HS256)로 붙습니다.
        String token = JwtFactory.withDefaultValues().createToken(jwtProperties);

        // ------------------------------------------------------------
        // [when] 실행: 정상 토큰을 validToken()메소드의 매개변수 에 넣습니다.
        // ------------------------------------------------------------
        // ④ validToken() 안에서 try 블록이 실행됩니다.
        // ⑤ parseClaimsJws(token) 이 두 가지를 검사합니다.
        //      - 서명: 비밀키로 다시 계산한 값이 토큰의 서명과 같은지 → 같음
        //      - 토큰 유효기한: 만료 시각(14일 뒤)이 지금보다 뒤인지 → 뒤임
        // ⑥ 두 검사 모두 통과하면 예외가 없으므로 return true 가 실행됩니다.
        //    catch 블록은 실행되지 않습니다.
        // ⑦ 결과 true 가 result 에 담깁니다.
        boolean result = tokenProvider.validToken(token);

        // ------------------------------------------------------------
        // [then] 확인: 결과가 true 인지 검사합니다.
        // ------------------------------------------------------------
        // ⑧ assertThat(result).isTrue()
        //    true 면 통과(초록), false 면 실패(빨강)입니다.
        //    실패하면 "Expected: true but was: false" 메시지가 나옵니다.
        assertThat(result).isTrue();

    }









    // ============================================================
    // 테스트 4: getAuthentication() 이 토큰에서 인증 정보를 제대로 만드는지 확인합니다.
    //
    // [한 줄 요약]
    //   TokenProvider.getAuthentication(token) 은 "토큰 문자열"을 받아서
    //   "인증 정보 객체(Authentication)"를 돌려주는 메서드입니다.
    //   이 테스트는 그 결과 안에 처음 넣은 이메일이 들어 있는지 확인합니다.
    // ============================================================
    @DisplayName("getAuthentication() : 토큰 기반으로 사용자 인증정보를 가져 올수 있는지 테스트")
    @Test
    void getAuthentication() {

        // ------------------------------------------------------------
        // [given] 준비: 주인이 user@email.com 인 토큰 문자열을 만듭니다.
        // ------------------------------------------------------------
        // ① String 객체 userEmail
        //    "user@email.com" 을 담고 있는 문자열 객체입니다.
        //    이후 토큰의 주인 값으로 쓰이고, 마지막 검사의 기준이 됩니다.
        String userEmail = "user@email.com";

        // ② JwtFactory.builder()
        //    JwtFactory 객체를 만드는 JwtFactoryBuilder 객체를 돌려줍니다.
        //    JwtFactoryBuilder 객체는 값을 한 칸씩 골라 넣을 때 쓰는 도구입니다.
        // ③ .subject(userEmail)
        //    JwtFactoryBuilder 객체안의 "주인(subject)" 변수에 userEmail 을 넣습니다.
        //    그리고 같은 JwtFactoryBuilder 객체를 돌려줘서 다음 호출을 이어 갈 수 있게 합니다.
        // ④ .build()
        //    값을 모아 JwtFactory 객체 한 개를 완성해서 돌려줍니다.
        //    토큰 주인 user@email.com 만 바뀌고, 나머지 칸(만료 14일 뒤 등)은 기본값을 씁니다.
        // ⑤ .createToken(jwtProperties)
        //    JwtFactory 객체를 재료로 토큰 문자열(String)을 만듭니다.
        //    jwtProperties 는 발급자와 비밀키를 담은 설정 객체입니다.
        String token = JwtFactory.builder()
                                 .subject(userEmail)
                                 .build()
                                 .createToken(jwtProperties);
        /*
         String token 변수에 저장되는 JWT 토큰 문자열  예

                       헤더.         내용.    서명
            "(eyJ0eXAi...).(eyJzdWIi...).(4f9a2c...)"
         */
        // ------------------------------------------------------------
        // [when] 실행: 토큰 문자열로 인증 정보 객체를 만듭니다.
        // ------------------------------------------------------------
        // ⑥ tokenProvider
        //    TokenProvider 객체입니다. 스프링이 프로그램을 켤 때 한 개 만들어 두고,
        //    @Autowired 로 이 테스트 클래스의 변수에 넣어 준 것입니다.
        // ⑦ getAuthentication(token)
        //    내부에서 getClaims(token) 으로 토큰 내용(Claims 객체)을 꺼냅니다.
        //    이때 서명과 기한도 함께 검사합니다.
        //
        // ⑧ 결과: Authentication 객체 한 개
        //    "인증 정보를 담는 상자"입니다. 실제로는 UsernamePasswordAuthenticationToken 이라는
        //    클래스의 객체이며, 이름과 달리 아이디·비밀번호 로그인과는 관계없이 쓰입니다.
        //    이 안에는 아래 세 가지가 들어 있습니다.
        //      - principal   : "누구인가" → 사용자 객체 (⑨에서 설명)
        //      - credentials : "무엇으로 인증했나" → 토큰 문자열 (여기서는 빈 칸이 아님)
        //      - authorities : "무엇을 할 수 있나" → 권한 목록 (ROLE_USER 하나)
        Authentication authentication = tokenProvider.getAuthentication(token);


        // ------------------------------------------------------------
        // [then] 확인: 인증 정보 안의 사용자 User객체의 이메일을 꺼내서 처음 user@email.com 이메일과 비교합니다.
        // ------------------------------------------------------------
        // ⑨ authentication.getPrincipal()
        //    인증 정보 안의 "사용자 User 객체"를 꺼냅니다.
        //    이 객체는 스프링 시큐리티의 User 클래스 객체입니다.
        //    (우리 프로젝트의 domain.User 엔티티 클래스 가 아닙니다. 이름이 같아서 헷갈리기 쉽습니다.)
        //    꺼낸 값의 타입은 Object 라서 바로 이름을 꺼낼 수 없습니다.
        //
        // ⑩ (UserDetails) 형변환
        //    "UserDetails" 는 사용자 정보를 꺼낼 수 있다는 약속(인터페이스)입니다.
        //    스프링 시큐리티의 User 자식클래스는 이 약속을 지키고 있으므로, 이 타입으로 바꿔 쓸 수 있습니다.
        //
        // ⑪ .getUsername()
        //    UserDetails 약속(인터페이스)에 있는 메서드입니다. 사용자 이름(String)을 돌려줍니다.
        //    이 사용자 User객체의 이름 칸에는 토큰의 주인(sub) 값이 들어 있으므로
        //    "user@email.com" 이 나옵니다.
        // ⑫ isEqualTo(userEmail)
        //    ⑪에서 꺼낸 문자열과 ①의 userEmail 을 비교합니다.
        //    같으면 통과(초록), 다르면 실패(빨강)입니다.
        assertThat(((UserDetails) authentication.getPrincipal()).getUsername()).isEqualTo(userEmail);
    }
    // ============================================================
    // 테스트 5: getUserId() 가 토큰 안의 사용자 번호(id)를 그대로 꺼내는지 확인합니다.
    //
    // [한 줄 요약]
    //   TokenProvider.getUserId(token) 은 "토큰 문자열"을 받아서
    //   "사용자 번호(Long)"를 돌려주는 메서드입니다.
    //   이 테스트는 토큰에 넣은 번호 1이 같은 값으로 돌아오는지 확인합니다.
    // ============================================================
    @DisplayName("getUserId(): jwt토큰으로 유저 ID를 가져올수 있는지 테스트")
    @Test
    void getUserId() {
        // ------------------------------------------------------------
        // [given] 준비: 사용자 번호(id) 1을 "id" 항목으로 넣은 토큰 문자열을 만듭니다.
        // ------------------------------------------------------------
        Long userId = 1L; //토큰에 넣은 사용자 번호(id), 마지막 then에서 검사의 기준값이 됩니다.

        //========= 1을 "id" 항목으로 넣은 토큰 문자열을 만듭니다.=================
        // ② Map.of("id", userId)
        //    "id" 라는 이름(String)과 1(Long)을 짝지은 Map 객체를 만듭니다.
        //    Map 은 "이름 → 값" 짝을 모아 두는 타입입니다. 짝 한 개가 토큰의 항목 한 개가 됩니다.
        //    Map.of 로 만든 Map 은 한번 만들면 내용을 바꿀 수 없습니다.
        //
        // ③ JwtFactory.builder().claims(...)
        //    JwtFactoryBuilder 객체에 "추가 항목 Map" 칸을 채웁니다.
        //    JwtFactory 생성자는 이 칸이 null 이 아니므로 그 Map 을 그대로 씁니다.
        //
        // ④ .build()
        //    값을 모아 JwtFactory 객체를 완성합니다.
        //    토큰 주인, 만료 시각 등 나머지 칸은 기본값입니다.
        //
        // ⑤ .createToken(jwtProperties)
        //    JwtFactory 의 값으로 토큰 문자열(String)을 만듭니다.
        //    내부에서 .addClaims(claims) 가 실행되어, 토큰의 내용(payload)에
        //    "id": 1 이 들어갑니다. 내용은 JSON 모양으로 적힙니다.
        //      예) {"sub":"test@email.com","iss":"…","iat":…,"exp":…,"id":1}
        //    결과 문자열을 token 에 담습니다.
        String token = JwtFactory.builder()
                                .claims(Map.of("id", userId))
                                .build()
                                .createToken(jwtProperties);


        // ------------------------------------------------------------
        // [when] 실행: 토큰 문자열에서 사용자 번호(id)를 꺼냅니다.
        // ------------------------------------------------------------
        // ⑥ tokenProvider.getUserId(token)
        //    TokenProvider 객체(스프링이 만들어 둔 것)의 메서드를 부릅니다.
        //
        // ⑦ 내부의 getClaims(token)
        //    토큰을 읽어서 서명과 기한을 검사합니다.
        //    검사를 통과하면 Claims 객체를 돌려줍니다.
        //    Claims 객체는 토큰 내용(JSON)을 "이름 → 값" 형태로 다루게 해 주는 객체입니다.
        //
        // ⑧ claims.get("id", Long.class)
        //    Claims 안에서 이름이 "id" 인 항목을 찾습니다. 값은 JSON 숫자 1 입니다.
        //    Long.class 는 "이 값을 Long 타입으로 달라"는 요청입니다.
        //    그래서 결과는 Long 타입의 1 이 됩니다.
        //
        // ⑨ 결과 1 이 userIdByToken 변수에 담깁니다.
        Long userIdByToken =  tokenProvider.getUserId(token);

        // ------------------------------------------------------------
        // [then] 확인: 꺼낸 사용자 번호(id)값이 넣은 값(1)과 같은지 봅니다.
        // ------------------------------------------------------------
        // ⑩ assertThat(userIdByToken)
        //    검사할 값(꺼낸 1)을 감쌉니다.
        //
        // ⑪ .isEqualTo(userId)
        //    감싼 값이 userId(1)와 같은지 비교합니다.
        //    같으면 통과(초록), 다르면 실패(빨강)입니다.
        //    실패 메시지는 "Expected: 1 but was: (다른 값)" 형태로 나옵니다.
        assertThat(userIdByToken).isEqualTo(userId);
    }
}