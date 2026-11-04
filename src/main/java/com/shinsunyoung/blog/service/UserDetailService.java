// ============================================================
// 파일 경로
// src/main/java/me/shinsunyoung/springbootdeveloper/service/UserDetailService.java
// ============================================================
//
// [이 클래스의 역할]
//
// 로그인할 때 이메일로 회원을 찾아서 스프링 시큐리티에 돌려주는 서비스입니다.
// 로그인 화면에서 이메일과 비밀번호를 입력하면, 스프링 시큐리티가 이 클래스의 loadUserByUsername() 을 호출합니다.
//
// [로그인 한 번의 전체 흐름]
//
// 1. login.html 에서 이메일(name="username")과 비밀번호(name="password")를 POST /login 으로 보냅니다.
// 2. 스프링 시큐리티의 DaoAuthenticationProvider 가 요청을 받습니다.
// 3. DaoAuthenticationProvider 가 UserDetailService.loadUserByUsername(이메일) 을 호출합니다. ← 이 파일
// 4. 이 메서드가 찾은 User 객체를 돌려주면, DaoAuthenticationProvider 가 User 의 getPassword() 로 암호문을 꺼냅니다.
// 5. PasswordEncoder.matches() 로 입력 비밀번호와 암호문을 비교합니다.
// 6. 일치하면 로그인 성공, WebSecurityConfig 의 defaultSuccessUrl("/articles") 로 이동합니다.
//
// [등록 방식]
//
// 이 클래스에는 @Service 가 붙어 있어서 스프링이 객체를 자동으로 보관하고 시큐리티에 연결합니다.
// 그래서 WebSecurityConfig 에 따로 등록하는 코드는 필요하지 않습니다.

package com.shinsunyoung.blog.service;


// [1. Lombok 관련 import]
//
// @RequiredArgsConstructor 를 쓰기 위한 import 입니다.
// final 필드(userRepository)를 받는 생성자를 자동으로 만들어 줍니다.
import lombok.RequiredArgsConstructor;


// [2. 프로젝트 내부 import]
//
// UserRepository : users 테이블에서 이메일로 회원을 찾는 리포지토리입니다.
import com.shinsunyoung.blog.repository.UserRepository;


// [3. 스프링 시큐리티 관련 import]
//
// UserDetails             : 시큐리티가 쓰는 회원 정보 규격입니다. 이 메서드의 반환 타입입니다.
// UserDetailsService      : 시큐리티가 "아이디로 회원 찾기"를 요청할 때 호출하는 규격(인터페이스)입니다.
// UsernameNotFoundException : "해당 아이디의 회원이 없다"를 시큐리티에 알리기 위한 전용 예외입니다.
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;


// [4. 스프링 관련 import]
//
// @Service : 이 클래스를 서비스 객체로 스프링 컨테이너에 등록합니다.
import org.springframework.stereotype.Service;


// [5. 클래스 위의 어노테이션]

// @RequiredArgsConstructor
// - 아래의 final 필드 userRepository 를 받는 생성자를 자동으로 만듭니다.
// - 스프링이 이 생성자를 보고 UserRepository 객체를 넣어 줍니다. (생성자 주입)
@RequiredArgsConstructor

// @Service
// - 서비스 객체로 스프링에 등록합니다.
@Service

// [6. 클래스 선언]
// implements UserDetailsService
// - "시큐리티의 회원 조회 규칙을 따르겠다"는 선언입니다.
// - 그래서 아래에 loadUserByUsername() 메서드를 반드시 만들어야 합니다.
public class UserDetailService implements UserDetailsService {


    // [7. 리포지토리 객체]
    // - 이메일로 회원을 찾을 때 사용하는 객체입니다.
    // - final 이므로 생성자에서 한 번 넣으면 바뀌지 않습니다.
    private final UserRepository userRepository;


    // [8. 로그인 회원 조회 메서드]
    //
    // loadUserByUsername(String email)
    // - 호출하는 곳 : 스프링 시큐리티의 DaoAuthenticationProvider (로그인 처리 중)
    // - 매개변수 email : 로그인 폼에서 name="username" 으로 입력한 이메일입니다.
    //   (메서드 이름에는 Username 이 들어 있지만, 이 프로젝트에서는 이메일이 로그인 아이디입니다)
    // - 반환 타입 UserDetails : 시큐리티가 쓰는 회원 정보 규격입니다. User 객체가 이 규격을 구현하고 있습니다.
    @Override
    public UserDetails loadUserByUsername(String email) {

        // [로그인 시 입력한 이메일로 회원 찾기]
        // - userRepository.findByEmail(email) : users 테이블에서 같은 이메일의 회원을 찾습니다.
        // - 결과는 Optional<User> 상자에 담겨 돌아옵니다.
        //   · 회원이 있으면 상자 안에 User 객체가 들어 있습니다.
        //   · 회원이 없으면 상자가 비어 있습니다.
        return userRepository.findByEmail(email)

                // [상자가 비어 있을 때의 처리]
                // - orElseThrow(...) : 상자가 비어 있으면 괄호 안의 예외를 던집니다.
                //   상자에 회원이 들어 있으면 그 User 객체를 꺼내서 return 으로 넘깁니다.
                .orElseThrow(() ->

                        // [예외 객체 만들기]
                        // - UsernameNotFoundException : 스프링 시큐리티가 "회원이 없다"를 알아듣는 전용 예외입니다.
                        // - 예전에는 IllegalArgumentException 을 던졌는데, 시큐리티는 이 예외를 로그인 실패로 처리하지 못해
                        //   오류 화면이 뜰 수 있었습니다. 그래서 전용 예외로 바꿨습니다. [수정 내용]
                        new UsernameNotFoundException(

                                // [8-4. 오류 메시지]
                                // - 문자열과 이메일 값을 이어 붙입니다.
                                // - 예: "회원을 찾을 수 없습니다. email=user@gmail.com"
                                // - 이 메시지는 서버 로그에만 남고, 로그인 화면에는 보안상 "아이디 또는 비밀번호가 일치하지 않습니다"로 표시됩니다.
                                "회원을 찾을 수 없습니다. email=" + email
                        )
                );

        // [최종 결과 설명]
        // - 회원이 있으면 위 return 문이 User 객체를 돌려주고, 이 메서드는 끝납니다.
        // - User 는 UserDetails 를 구현했으므로 별도의 변환 없이 그대로 돌려줄 수 있습니다.
        // - 회원이 없으면 orElseThrow 에서 예외가 먼저 발생하므로 return 까지 도달하지 않습니다.
        //
        // [참고] 예외가 던져지면 DaoAuthenticationProvider 가 이 예외를 잡아서
        //       "아이디 또는 비밀번호가 일치하지 않습니다(Bad credentials)" 로 바꿔 줍니다.
        //       아이디가 존재하는지 노출하지 않으려는 시큐리티의 기본 동작입니다.
    }
}
