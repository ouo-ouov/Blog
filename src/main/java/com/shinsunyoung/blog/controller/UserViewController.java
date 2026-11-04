// ============================================================
// [파일 설명] 로그인 화면과 회원가입 화면을 보여 주는 컨트롤러입니다.
// 화면 파일 이름만 돌려주면, 스프링이 templates 폴더에서 HTML 을 찾아 보여 줍니다.
//
// [전체 주소 목록]
//   GET /login  → login()  → templates/login.html
//   GET /signup → signup() → templates/signup.html
// ============================================================
package com.shinsunyoung.blog.controller;

// @Controller : 화면을 돌려주는 컨트롤러라는 표시입니다.
import org.springframework.stereotype.Controller;

// @GetMapping : GET 방식 요청 주소를 지정하는 표시입니다.
import org.springframework.web.bind.annotation.GetMapping;

// @Controller : 이 클래스가 웹 요청을 받는 컨트롤러라는 표시입니다.
//   (JSON 이 아니라 "화면 파일 이름"을 돌려줍니다)
@Controller
public class UserViewController {

    // ---------------------------------------------------------------
    // 로그인 화면 : GET /login
    // ---------------------------------------------------------------
    // GET /login → templates/oauthLogin.html 을 보여 줍니다. (11장 변경)
    // WebOAuthSecurityConfig.java 설정파일 에서 /api/** 이외는 permitAll 이라 누구나 볼 수 있습니다.
    @GetMapping("/login")
    public String login() {

        // "login" 이라는 문자열은 templates/oauthLogin.html 파일 이름입니다.
        // 스프링의 뷰 리졸버(화면 찾는 도구)가 이 이름으로 HTML 파일을 찾아 브라우저에 보냅니다.
        return "oauthLogin";
    }

    // ---------------------------------------------------------------
    // 회원가입 화면 : GET /signup
    // ---------------------------------------------------------------
    @GetMapping("/signup")
    public String signup() {

        // "signup" 은 templates/signup.html 파일 이름입니다.
        return "signup";
    }
}
