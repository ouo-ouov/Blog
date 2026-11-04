package com.shinsunyoung.blog.config;

import com.shinsunyoung.blog.config.jwt.TokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 파일 위치: src/main/java/me/shinsunyoung/springbootdeveloper/config/TokenAuthenticationFilter.java
//
// ============================================================
// 이 파일이 하는 일
// ============================================================
// 사용자가 요청을 보낼 때마다 "토큰이 들어 있는지, 진짜인지" 확인하는 검문소 역할을 합니다.
// 토큰이 진짜이면, "이 요청은 이 사용자가 보낸 것"이라는 정보를 스프링 시큐리티에 저장합니다.
// 토큰이 없거나 가짜이면 아무것도 저장하지 않고, 요청을 다음 단계로 그냥 넘깁니다.
//
// ============================================================
// 요청 한 번이 지나가는 순서
// ============================================================
// 1. 요청이 들어오면 doFilterInternal() 이 실행됩니다.
// 2. 요청 머리글(헤더)에서 Authorization 항목을 찾습니다.
// 3. 그 값에서 "Bearer " 글자를 떼어 내고, JWT 토큰 문자열만 꺼냅니다.
// 4. JWT 토큰이 진짜인지 확인합니다. (tokenProvider.validToken())
// 5. 진짜이면 사용자 정보를 만들어 스프링 시큐리티 저장소에 넣습니다.
// 6. JWT 토큰 여부와 상관없이 다음 단계로 요청을 넘깁니다. (filterChain.doFilter())
//
// ============================================================
// 꼭 조심할 점
// ============================================================
// 1. 토큰이 없어도 필터는 요청을 막지 않고 다음 단계로 넘깁니다.
//    실제로 막는 일은 시큐리티 설정 파일의 authorizeHttpRequests(...) 규칙이 합니다.
//    예) 인증이 필요한 주소에 인증 정보 없이 들어오면 시큐리티가 거부합니다.
// 2. 이 클래스에는 @Component 표시가 없습니다.
//    그래서 스프링이 자동으로 객체를 등록하지 않습니다. 시큐리티 설정 파일에서 직접 등록해야 동작합니다.
//    ⚠️ 10장 현재 상태: 필터는 "만들기만" 했고, WebSecurityConfig 에는 아직 등록하지 않았습니다.
//       (교안: "필터 등록은 다음 단계에서" — 11장 WebOAuthSecurityConfig 에서 아래처럼 등록합니다.)
//         http.addFilterBefore(new TokenAuthenticationFilter(tokenProvider),
//                              UsernamePasswordAuthenticationFilter.class);
//       addFilterBefore = "아이디/비밀번호 로그인 필터보다 먼저 이 토큰 필터를 실행하라"는 뜻입니다.
// 3. 토큰 검사 결과가 false 이면 인증 정보를 저장하지 않으므로, 그 요청은 로그인하지 않은 것으로 처리됩니다.
// ============================================================

// @RequiredArgsConstructor
//   뜻: 아래 final 변수(tokenProvider)를 받는 생성자를 자동으로 만들어 줍니다.
//   이유: 스프링이 TokenProvider 객체를 이 생성자를 통해 넣어 주기 위해서입니다.
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    //TokenProvider 객체 : JWT 토큰을 확인하고 사용자 정보를 꺼내는 일을 맡은 객체입니다.
    // 스프링이 생성자를 통해 넣어 줍니다. 직접 new 로 만들지 않습니다.
    // final 이므로 한 번 넣으면 다른 객체로 바꾸지 않습니다.
    private final TokenProvider tokenProvider;

    //HTTP 요청 메세지의 헤더영역에 JWT토큰문자열이 저장되는 속성이름 "Authorization" 저장
    private final static String HEADER_AUTHORIZATION = "Authorization";

    // JWT 토큰 앞에 붙는 글자입니다. "Bearer " 는 뒤에 빈칸(공백) 한 칸까지 포함합니다.
    // 헤더 값이 "Bearer abc123" 이면, 이 글자를 떼어 내고 "abc123" 만 JWT 토큰 문자열로 씁니다.
    private final static String TOKEN_PREFIX = "Bearer ";

    // @Override
    //   뜻: 부모 클래스(OncePerRequestFilter)의 doFilterInternal 메서드를 우리 방식으로 다시 만든다는 표시입니다.
    //   부모가 요청이 올 때마다 이 메서드를 자동으로 불러 줍니다.
    @Override                      //클라이언트가 요청을 보낸 정보, 클라이언트의 브라우저로 보낼 응답 정보, 다음 단계로 요청을 넘길 때 쓰는 연결 고리 객체
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        //요청 메세지의 헤더 영역에서 "Authorization" 항목의 값을 꺼냅니다.
        // 예) "Authorization": "Bearer abc123" 같은 글자입니다. 헤더가 없으면 null 이 됩니다.
        String authorizationHeader = request.getHeader(HEADER_AUTHORIZATION);
        //"Bearer abc123"

        //요청 메세지의 헤더 영역의 "Bearer abc123" 에서 "Bearer " 을 떼어 내고 JWT토큰 문자열 "eyJhbGci..."을 꺼냅니다.
        String token = getAccessToken(authorizationHeader);
        //"abc123" <= JWT 토큰 문자열

        //JWT 토큰 문자열("abc123") 진짜 유효 기간이 남았는지 확인 해서 남았으면?
        if(tokenProvider.validToken(token)){

            //JWT 토큰문자열이 유효기간이 남았으면? 이 토큰으로 인증정보(요청한 사용자 정보)를 만듭니다.
            //인증정보 객체 반환 받아 옴  => new UsernamePasswordAuthenticationToken(principal, token, authorities);
            Authentication authentication = tokenProvider.getAuthentication(token);

            //만든 사용자 인증정보 객체를 스프링시큐리티의 저장소(SecurityContex객체 메모리)에 넣습니다.
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        //JWT 토큰 문자열("abc123") 유효기간이 남든 안남든 상관없이 요청을 그다음 단계로 넘깁니다.
        filterChain.doFilter(request,response);


    }//---------------doFilterInternal () 메소드 끝

    // ---- "Bearer eyJhbGci..." 에서 "Bearer " 을 떼어 내고 JWT토큰 문자열 "eyJhbGci..."을 꺼내 반환하는 메소드.
    // 매개변수로 받는 값: authorizationHeader (헤더 전체 값, 예: "Bearer abc123")
    // 돌려주는 값: 토큰 문자열(예: "abc123"), 또는 조건에 맞지 않으면 null
    private String  getAccessToken(String authorizationHeader){

        //두가지 조건을 모두 확인합니다.
        //      조건1)  "Bearer abc123" 이 실제로  요청한 클라이언트의 브라우저가 생성하는 HttpRequest요청메세지의 헤더영역에 존재하면?
        //              JWT "abc123" 반환
        //      조건2)   startsWith(TOKEN_PREFIX)    : 값이 "Bearer " 로 시작해야 합니다.
        //              JWT "abc123" 반환
        if(authorizationHeader != null  &&  authorizationHeader.startsWith(TOKEN_PREFIX)){

            // substring(TOKEN_PREFIX.length()) 는 앞의 "Bearer " 글자 수만큼 잘라 내고 나머지를 가져옵니다.
            // 예) "Bearer abc123" 에서 앞 7글자("Bearer ")를 잘라 내면 "abc123" 이 남습니다.
            return  authorizationHeader.substring(TOKEN_PREFIX.length());
          //return  "abc123";
        }

        //조건1) 조건2)에 만족하지 않으면 토큰이 없다는 뜻으로 null 반환
        return null;
    }


}








