package com.shinsunyoung.blog.util;
// 파일 위치: src/main/java/me/shinsunyoung/springbootdeveloper/util/CookieUtil.java
//
// ============================================================================
// 이 파일이 하는 일  [11장 · 쿠키 도우미]
// ============================================================================
// 쿠키를 만들고, 지우고, 자바 객체를 쿠키에 담을 수 있는 글자로 바꾸는 "공용 도구 상자"입니다.
// 모든 메서드가 static 이라서 객체를 만들지 않고 CookieUtil.addCookie(…) 처럼 바로 부릅니다.
//
//   메서드          하는 일                               쓰는 곳
//   ─────────────   ───────────────────────────────────   ─────────────────────────────────────────────
//   addCookie       쿠키 만들기                            OAuth2SuccessHandler(refresh_token),
//                                                          쿠키 보관함(oauth2_auth_request)
//   deleteCookie    쿠키 지우기(수명 0으로 덮어쓰기)          위와 같음
//   serialize       자바 객체 → Base64 글자                 쿠키 보관함 (인가 요청 저장)
//   deserialize     Base64 글자 → 자바 객체                 쿠키 보관함 (인가 요청 꺼내기)
//
// ============================================================================
// 쿠키 기초
// ============================================================================
// 쿠키 = 서버가 브라우저에게 맡겨 두는 "키=값" 쪽지입니다.
//   서버 → 브라우저 : 응답 헤더  Set-Cookie: refresh_token=eyJ…; Max-Age=1209600; Path=/
//   브라우저 → 서버 : 이후 요청마다 자동으로  Cookie: refresh_token=eyJ…
//
//   설정        뜻
//   ─────────   ───────────────────────────────────────────────────────────────
//   Path=/      이 사이트의 모든 주소로 요청할 때 쿠키를 함께 보냅니다.
//   Max-Age=N   N초 동안 보관합니다. 0 이면 "지금 바로 지워라"는 뜻입니다.
//   HttpOnly    자바스크립트(document.cookie)가 읽지 못하게 합니다.  ← 이 파일에서는 설정하지 않음
//   Secure      https 로만 보냅니다.                                   ← 이 파일에서는 설정하지 않음
//
// ※ 쿠키를 "삭제"하는 API 는 따로 없습니다. 같은 이름·같은 Path 로 Max-Age=0 쿠키를 다시 보내면
//   브라우저가 기존 쿠키를 지웁니다. deleteCookie() 가 바로 이 방법을 씁니다.
// ============================================================================

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.SerializationUtils;

import java.util.Base64;

//쿠키 관리 클래스
public class CookieUtil {

    // ------------------------------------------------------------------------
    // addCookie: 쿠키 만들기
    // ------------------------------------------------------------------------
    // 받는 값: response(응답), name(쿠키 이름), value(쿠키 값), maxAge(보관 시간, 초)
    // 결과   : 응답 헤더에 Set-Cookie: name=value; Max-Age=maxAge; Path=/ 가 추가됩니다.
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value); // 이름과 값으로 쿠키를 만듭니다.
        cookie.setPath("/");                     // 사이트 전체 주소에서 쓰는 쿠키로 정합니다.
        cookie.setMaxAge(maxAge);                // 보관 시간을 정합니다. 예) 1209600 = 14일

        response.addCookie(cookie);              // HTTP응답메세지에 실어 브라우저로 보냅니다.
    }

    // ------------------------------------------------------------------------
    // deleteCookie: 쿠키 지우기
    // ------------------------------------------------------------------------
    // 브라우저가 보낸 쿠키 중 이름이 같은 것을 찾아, 값은 비우고 수명은 0 으로 바꿔 다시 보냅니다.
    // → 브라우저는 "수명 0 = 바로 삭제"로 알아듣고 지웁니다.
    // 브라우저에 그 쿠키가 없으면 아무 일도 하지 않습니다.
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {

        Cookie[] cookies = request.getCookies(); // 브라우저가 보낸 쿠키 전부

        // 쿠키가 하나도 없으면 getCookies() 는 빈 배열이 아니라 null 을 돌려줍니다. 그래서 먼저 확인합니다.
        if (cookies == null) {
            return;
        }

        for (Cookie cookie : cookies) {           // 쿠키를 하나씩 보면서
            if (name.equals(cookie.getName())) {  // 지울 이름과 같으면
                cookie.setValue("");              //   값을 비우고
                cookie.setPath("/");              //   만들 때와 같은 Path 로 맞추고 (Path 가 다르면 다른 쿠키로 취급되어 안 지워짐)
                cookie.setMaxAge(0);              //   수명을 0 으로 (= 즉시 삭제)
                response.addCookie(cookie);       //   http응답에 실어 보냅니다.
            }
        }
    }
    // ------------------------------------------------------------------------
    // serialize: 자바 객체 → 쿠키에 넣을 수 있는 문자열 값으로 변환해서 반환
    // ------------------------------------------------------------------------
    // 1. SerializationUtils.serialize(obj) : 객체를 바이트 배열로 바꿉니다. (객체는 Serializable 이어야 함)
    // 2. Base64.getUrlEncoder()            : 바이트를 주소·쿠키에 안전한 글자로 바꿉니다.
    //    일반 Base64 의 + / 대신 - _ 를 써서 쿠키 값에 문제를 일으키지 않습니다.
    // 예) OAuth2AuthorizationRequest 객체 → "rO0ABXNyAEtvcmcuc3ByaW5n…"
    public static String serialize(Object obj) {

        return Base64.getUrlEncoder()
                .encodeToString(SerializationUtils.serialize(obj));
    }

    // ------------------------------------------------------------------------
    // deserialize: 쿠키에 넣을 수 있는 문자열 값을  → 자바 객체 로 변환해서 반환
    // ------------------------------------------------------------------------
    // <T> T … Class<T> cls : "돌려줄 타입을 부르는 쪽이 정한다"는 제네릭 문법입니다.
    //   예) CookieUtil.deserialize(cookie, OAuth2AuthorizationRequest.class) → OAuth2AuthorizationRequest 를 돌려줌
    // 1. cookie.getValue()              : 쿠키 값(글자)을 꺼냅니다.
    // 2. Base64.getUrlDecoder().decode  : 쿠키 글자 → 바이트
    // 3. SerializationUtils.deserialize : 바이트 → 객체 (Object 타입)
    // 4. cls.cast(...)                  : Object 를 원하는 타입으로 바꿉니다. 타입이 다르면 오류가 납니다.

    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        return cls.cast(
                SerializationUtils.deserialize(
                        Base64.getUrlDecoder().decode(cookie.getValue())
                )
        );
    }




}
