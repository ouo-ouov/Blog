// ============================================================
// [파일 설명] 회원가입 업무를 담당하는 서비스 클래스입니다.
// 핵심 작업은 "비밀번호를 암호화해서 회원 정보를 저장하는 것"입니다.
//
// [호출하는 곳]
//   UserApiController.signup(AddUserRequest request) → userService.save(request)
// ============================================================
package com.shinsunyoung.blog.service;

// Lombok : final 필드를 받는 생성자를 자동으로 만듭니다.
import lombok.RequiredArgsConstructor;

// User : 회원 엔티티입니다. User.builder() 로 객체를 만듭니다.
import com.shinsunyoung.blog.domain.User;

// AddUserRequest : 회원가입 폼의 입력값을 담은 상자 (DTO)
import com.shinsunyoung.blog.dto.AddUserRequest;

// UserRepository : users 테이블에 접근하는 리포지토리입니다.
import com.shinsunyoung.blog.repository.UserRepository;

// PasswordEncoder : 비밀번호 암호화 도구의 규격(인터페이스)입니다.
//   실제 구현체(BCryptPasswordEncoder)는 WebSecurityConfig.passwordEncoder() 에서 @Bean 으로 등록되어 있습니다.
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// @Service : 업무 처리 담당 객체로 스프링에 등록합니다.
import org.springframework.stereotype.Service;

// @RequiredArgsConstructor : 아래 final 필드 2개를 받는 생성자를 자동으로 만듭니다.
//   스프링이 생성자를 보고 UserRepository 객체와 PasswordEncoder 객체를 넣어 줍니다. (생성자 주입)
@RequiredArgsConstructor
@Service
public class UserService {

    // 회원 테이블 작업을 맡는 리포지토리입니다.
    private final UserRepository userRepository;

    // 비밀번호 암호화 도구입니다. 실제로는 BCryptPasswordEncoder 객체가 들어옵니다.
    // private final PasswordEncoder passwordEncoder;

    // ============================================================
    // findById: 사용자 id로 회원 한 명을 조회해서 User 엔티티 객체에 담아 돌려줍니다.
    // 매개변수로 전달 받는 값: userId(조회할 사용자 id)
    // 돌려주는 값: 조회한  User 엔티티 객체
    // 조회 못하면: 오류(IllegalArgumentException)를 던집니다.
    // ============================================================
    public User findById(Long userId){

        // userRepository.findById(userId)
        //   데이터베이스에서 사용자 번호가 userId 인 줄을 찾습니다.
        //   조회가 되면 그 사용자정보를 Opetional<User 엔티티 객체>, 없으면 빈 결과(Optional)를 돌려줍니다.
        //
        // .orElseThrow(() -> new IllegalArgumentException("Unexpected user"))
        //   조회 결과가 비어 있으면(찾지 못했으면) 아래 오류를 만들어 던집니다.
        //   () -> ... 부분은 "조회 값이 없을 때 실행할 일"을 짧게 적은 것입니다.
        //   오류 메시지는 "Unexpected user"(예상하지 못한 사용자)입니다.
        //   조회 해서 찾은 경우에는 이 오류가 만들어지지 않고, 조회한 사용자 정보가 저장된 User 엔티티 객체를 그대로 돌려줍니다.
        return userRepository.findById(userId)
                .orElseThrow(()-> new IllegalArgumentException("Unexpected user"));
    }


    // save : 회원가입 정보를 받아 암호화 후 DB 에 저장하고, 새 회원의 번호(id)를 돌려줍니다.
    //   호출하는 곳 : UserApiController.signup()
    public Long save(AddUserRequest dto) {

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(); //비밀번호 암호화 객체.

        // 1) User.builder() : User 객체를 한 줄씩 값을 채워 만드는 빌더를 시작합니다.
        // 2) .email(dto.getEmail()) : 폼에서 받은 이메일을 넣습니다.
        // 3) .password(passwordEncoder.encode(dto.getPassword())) :
        //      폼에서 받은 평문 비밀번호(예: "1234")를 BCrypt 암호문(예: "$2a$10$...")으로 바꿔서 넣습니다.
        //      이렇게 해야 DB 에 원래 비밀번호가 남지 않습니다.
        // 4) .build() : 지금까지 넣은 값으로 User 객체를 완성합니다.
        // 5) userRepository.save(...) : 완성된 User 를 users 테이블에 INSERT 합니다.
        //      저장이 끝나면 id 가 채워진 User 객체를 돌려줍니다.
        // 6) .getId() : 그 객체에서 새로 만들어진 id 번호만 꺼내서 호출한 곳으로 돌려줍니다.
        return userRepository.save(User.builder()
                .email(dto.getEmail())
                .password(encoder.encode(dto.getPassword()))
                .build()).getId();
    }

    // ============================================================
    // [11장 추가] findByEmail: 이메일로 회원 한 명을 찾아서 돌려줍니다.
    // 누가 부르나요: OAuth2SuccessHandler.onAuthenticationSuccess()
    //   구글 로그인 결과에는 "구글 사용자 정보"만 있고 우리 회원 번호(id)는 없습니다.
    //   그래서 구글이 알려 준 이메일로 우리 users 표의 회원을 찾아, 그 User 로 JWT 를 만듭니다.
    // 실행되는 SQL 예) SELECT * FROM users WHERE email = 'gildong@gmail.com'
    // 찾지 못하면: IllegalArgumentException("Unexpected user")
    //   (정상 흐름에서는 바로 앞의 OAuth2UserCustomService 가 저장해 두었으므로 항상 찾습니다.)
    // ============================================================
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Unexpected user"));
    }



}
