// ============================================================
// 파일 경로
// src/main/java/me/shinsunyoung/springbootdeveloper/controller/UserApiController.java
// ============================================================
//
// [이 클래스의 역할]
//
// 회원가입 폼을 받아서 회원을 저장하는 컨트롤러입니다.
//
// [수정 내용]
//
// 예전에는 이 클래스에 /logout 처리 메서드도 있었습니다.
// 그런데 로그아웃은 WebSecurityConfig 의 .logout() 설정이 이미 처리하고 있어서
// 같은 일을 두 곳에서 하고 있었습니다. 그래서 이 클래스의 로그아웃 메서드는 삭제했습니다.
// 로그아웃은 이제 WebSecurityConfig 한 곳에서만 처리합니다.
//
// [전체 주소 목록]
//
// POST /user  → signup()  : 회원가입 처리 후 /login 으로 이동
// (로그아웃 GET /logout 은 WebSecurityConfig 가 처리합니다)

package com.shinsunyoung.blog.controller;


// [1. Lombok 관련 import]
//
// @RequiredArgsConstructor 를 쓰기 위한 import 입니다.
// final 필드(userService)를 받는 생성자를 자동으로 만들어 줍니다.
import lombok.RequiredArgsConstructor;


// [2. 프로젝트 내부 import]
//
// AddUserRequest : 회원가입 폼 입력값(email, password)을 담는 상자 (DTO)
// UserService    : 회원가입 업무를 맡는 서비스입니다.
import com.shinsunyoung.blog.dto.AddUserRequest;
import com.shinsunyoung.blog.service.UserService;


// [3. 스프링 MVC 관련 import]
//
// @Controller    : 화면 이동(redirect) 등을 처리하는 컨트롤러라는 표시입니다.
// @PostMapping   : POST 방식 요청 주소를 지정하는 표시입니다.
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;


// [4. 클래스 위의 어노테이션]

// @RequiredArgsConstructor
// - 아래 final 필드 userService 를 받는 생성자를 자동으로 만듭니다.
// - 스프링이 이 생성자를 보고 UserService 객체를 넣어 줍니다. (생성자 주입)
@RequiredArgsConstructor

// @Controller
// - 이 클래스가 웹 요청을 받는 컨트롤러라는 표시입니다.
// - 메서드가 돌려주는 문자열을 "화면 이동 주소"로 해석합니다.
@Controller
public class UserApiController {


    // [5. 서비스 필드]
    // - 회원가입 업무를 맡는 객체입니다. 스프링이 생성자로 넣어 줍니다.
    private final UserService userService;


    // [6. 회원가입 처리 : POST /user]
    //
    // - signup.html 의 <form action="/user" method="POST"> 제출이 이 메서드로 들어옵니다.
    // - 매개변수 request : 폼의 email, password 값이 같은 이름의 필드에 자동으로 채워져 들어옵니다.
    //   (폼 데이터는 JSON 이 아니라 Setter 로 값을 넣습니다. AddUserRequest 의 @Setter 가 그 역할을 합니다)
    @PostMapping("/user")
    public String signup(AddUserRequest request) {

        // [6-1] 회원 정보를 암호화해서 DB 에 저장하라고 UserService 에 맡깁니다.
        // - 비밀번호 암호화와 저장은 UserService.save() 안에서 일어납니다.
        userService.save(request);

        // [6-2] "redirect:/login" 은 화면을 보여 주는 대신, 브라우저에게 /login 주소로 다시 가라고 알려 줍니다.
        // - 그래서 회원가입이 끝나면 로그인 화면이 열립니다.
        return "redirect:/login";
    }
}
