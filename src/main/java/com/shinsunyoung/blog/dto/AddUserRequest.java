// ============================================================
// [파일 설명] 회원가입 화면(signup.html)의 입력값을 담는 상자(DTO)입니다.
//
// [데이터 흐름]
//   signup.html 의 <form action="/user" method="POST"> 제출
//   → 입력칸 name="email", name="password" 값이 이 객체의 같은 이름 필드에 자동으로 들어감
//   → UserApiController.signup(AddUserRequest request)
//   → UserService.save(request)
//
// [왜 @Setter 가 필요한가?]
//   HTML 폼 데이터는 JSON 이 아니라 "이름=값" 형태로 옵니다.
//   스프링은 빈 객체를 먼저 만든 뒤 setEmail(), setPassword() 를 호출해서 값을 채웁니다.
//   그래서 Setter 가 있어야 합니다. (반면 JSON 을 받는 DTO 들은 Setter 없이도 동작합니다)
// ============================================================
package com.shinsunyoung.blog.dto;

// @Getter : getEmail(), getPassword() 를 만듭니다. UserService 에서 값을 꺼낼 때 씁니다.
import lombok.Getter;

// @Setter : setEmail(), setPassword() 를 만듭니다. 폼 데이터를 객체에 채울 때 씁니다.
import lombok.Setter;

@Getter
@Setter
public class AddUserRequest {

    // 사용자가 입력한 이메일입니다. 나중에 로그인 아이디로 쓰입니다.
    private String email;

    // 사용자가 입력한 평문 비밀번호입니다. 이 상태로 DB 에 저장하지 않고,
    // UserService.save() 에서 passwordEncoder.encode() 로 암호화한 뒤 저장합니다.
    private String password;
}
