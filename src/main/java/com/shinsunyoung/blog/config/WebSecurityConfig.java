package com.shinsunyoung.blog.config; //// ============================================================
//// 파일 경로
//// src/main/java/com/shinsunyoung/blog/config/WebSecurityConfig.java
//// ============================================================
////
//// [이 클래스의 역할]
////
//// WebSecurityConfig는 스프링 시큐리티의 보안 설정을 담당합니다.
////
//// 사용자가 웹사이트에 접속했을 때 다음과 같은 작업을 처리합니다.
////
//// 1. 로그인하지 않아도 접속할 수 있는 페이지 설정       -> /login, /signup, /user
//// 2. 로그인해야 접속할 수 있는 페이지 설정             -> 위 3개를 뺀 나머지 전부 (/articles, /new-article, /api/** ...)
//// 3. 로그인 화면과 로그인 성공 후 이동할 페이지 설정    -> 로그인 화면 /login, 성공 후 /articles
//// 4. 로그아웃 처리 방법 설정                         -> /logout 요청 시 세션 삭제 후 /login 으로 이동
//// 5. 비밀번호 암호화에 사용할 객체 등록               -> BCryptPasswordEncoder
////
//// 예를 들어 로그인하지 않은 사용자가 http://localhost:8080/articles 에 접속하면?
////
////   1. 스프링 시큐리티 보안 필터가 요청을 먼저 가로챕니다. (BlogViewController 까지 요청이 가지 않습니다.)
////   2. 아래 filterChain() 메소드의 .anyRequest().authenticated() 설정에 걸립니다.
////      "로그인 안 한 사람은 /articles 를 볼 수 없다~"
////   3. 아래 .formLogin() 의 .loginPage("/login") 설정에 의해
////      브라우저를 http://localhost:8080/login 으로 강제로 이동(redirect)시킵니다.
////   4. UserViewController 의 @GetMapping("/login") login() 메소드가 실행되어
////      templates/login.html 로그인 화면이 보여집니다.
////
//// 반대로 /signup 은 아래 .requestMatchers("/signup").permitAll() 설정 때문에
//// 로그인하지 않아도 바로 회원가입 화면을 볼 수 있습니다.
////
//// [이 파일과 연결된 파일 한눈에 보기]
////
////   WebSecurityConfig.java (이 파일)
////      ├─ UserViewController.java   : GET /login  -> login.html,  GET /signup -> signup.html
////      ├─ UserApiController.java    : POST /user  -> 회원가입 처리 후 redirect:/login
////      ├─ UserService.java          : 회원 정보 INSERT (비밀번호는 이 파일의 passwordEncoder 로 암호화)
////      ├─ UserDetailService.java    : 로그인할 때 이메일로 회원을 찾아 주는 클래스
////      ├─ UserRepository.java       : users 테이블 SELECT / INSERT
////      └─ User.java                 : users 테이블 한 줄(회원 1명) = UserDetails 구현 객체
//
//package com.shinsunyoung.blog.config;
//
//
//// [1. Lombok 관련 import]
////
//// @RequiredArgsConstructor를 사용하기 위한 import입니다.
//// 이 클래스에서 final 필드를 생성자 주입할 때 사용합니다.
//import lombok.RequiredArgsConstructor;
//
//
//// [2. 스프링 설정 관련 import]
////
//// @Configuration : 설정 클래스임을 나타냅니다.
//// @Bean          : 메서드가 반환하는 객체를 스프링 컨테이너가 관리하는 빈(객체)으로 등록합니다.
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//
//// [3. 스프링 시큐리티의 HTTP 보안 설정]
////
//// HttpSecurity를 이용하면 요청 주소별 접근 권한,
//// 로그인, 로그아웃, CSRF 등의 보안 설정을 구성할 수 있습니다.
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//
//
//// 웹 애플리케이션의 보안 설정을 활성화하는 어노테이션입니다.
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//
//
//// 특정 요청을 스프링 시큐리티의 보안 필터 적용 대상에서
//// 제외할 때 사용하는 설정 인터페이스입니다.
//import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
//
//
//// CSRF 등 일부 보안 설정을 비활성화할 때 사용하는 설정 클래스입니다.
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//
//
//// [4. 비밀번호 암호화 관련 import]
////
//// BCryptPasswordEncoder : 비밀번호를 BCrypt 방식으로 해시 처리합니다.
//// PasswordEncoder      : 비밀번호 인코더의 표준 인터페이스입니다.
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//
//// [5. 보안 필터 설정 관련 import]
////
//// SecurityFilterChain은 HTTP 요청이 통과할 보안 필터 체인을 구성합니다.
//// 로그인 여부 확인, 인증 처리 등의 보안 기능이 이 체인을 통해 동작합니다.
//import org.springframework.security.web.SecurityFilterChain;
//
//
//// H2 데이터베이스 콘솔의 기본 요청 경로를 인식하기 위한 메서드입니다.
//// 현재 프로젝트의 스프링 부트 버전(4.x)에 맞는 패키지 경로를 사용합니다.
////   (스프링 부트 3.x 에서는 org.springframework.boot.autoconfigure.security.servlet.PathRequest 였습니다.)
//import static org.springframework.boot.security.autoconfigure.web.servlet.PathRequest.toH2Console;
//
//
//// ============================================================
//// [6. 보안 설정 클래스 선언]
//// ============================================================
//
//// @Configuration
//// 이 클래스가 스프링의 설정 클래스임을 나타냅니다.
//// 스프링은 프로그램이 시작될 때 이 클래스를 찾아서,
//// 안에 있는 @Bean 메소드들을 한 번씩 실행하고 반환된 객체를 스프링 컨테이너(메모리)에 보관합니다.
//
//// @EnableWebSecurity
//// 스프링 시큐리티의 웹 보안 설정을 활성화합니다.
//// 이 표시가 있어야 아래 filterChain() 에서 만든 보안 규칙이 모든 웹 요청에 적용됩니다.
//
//// @RequiredArgsConstructor
//// final 필드가 있다면 해당 필드를 매개변수로 받는 생성자를 만듭니다.
//// 현재 클래스에는 final 필드가 없으므로 생성자 주입 효과는 없습니다.
//@Configuration
//@EnableWebSecurity
//@RequiredArgsConstructor
//public class WebSecurityConfig {
//
//
//    // =================================================================
//    // [7. 스프링 시큐리티의 모든 기능을 사용하지 않게 보안 설정에서 제외 하는 코드]
//    //   - 즉, 인증, 인가 서비스를 모든 곳에 적용하지 않게 설정 하는 코드로
//    //     일반적으로 정적 리소스(이미지, HTML파일)에 설정합니다.
//    //   - 정적 리소스(이미지, HTML파일)만 스프링 시큐리티 사용을 비활성화 하는데
//    //     static 하위 경로에 있는 리소스와 h2 DB의 데이터를 확인하는데 사용하는 h2-console 하위 url을 대상으로 ignoring() 메소드를 사용합니다.
//    //
//    //   [permitAll() 과 ignoring() 의 차이]
//    //     - permitAll() : 보안 필터는 "통과"하지만 검사는 받습니다. (로그인 안 해도 들어갈 수 있음)
//    //                     -> 아래 [8] filterChain() 의 /login, /signup, /user 가 여기에 해당합니다.
//    //     - ignoring()  : 보안 필터를 "아예 거치지 않습니다." (스프링 시큐리티가 그 요청을 모르는 척 합니다)
//    //                     -> 이 [7] configure() 의 /h2-console, /static/** 이 여기에 해당합니다.
//    //
//    //   ※ 실행 로그에 아래 WARN 경고가 찍히는 이유도 이것 때문입니다. (실행에는 문제 없습니다)
//    //     "You are asking Spring Security to ignore ... This is not recommended -- please use permitAll ..."
//    //     스프링 시큐리티는 ignoring() 대신 permitAll() 을 쓰라고 권장하지만, 학습용이라 그대로 둡니다.
//    // =================================================================
//
//    // @Bean: 이 메서드가 반환하는 WebSecurityCustomizer인터페이스를 구현한 WebSecurity 보안 자식 객체(여기서는 람다식 지침서)를
//    // 스프링 컨테이너에 '빈(Bean == 객체)'으로 등록하라는 의미입니다.
//    // 등록해두면 스프링 시큐리티(보안)이 시작될 때 이 WebSecurity 보안 설정을 톰캣 서버가 자동으로 읽어서 실행합니다.
//    @Bean
//    public WebSecurityCustomizer configure() {
//
//        // WebSecurityCustomizer는 스프링 시큐리티에서 제공하는 인터페이스입니다.
//        // 이 인터페이스의 customize(WebSecurity web) 메서드를 오버라이딩 하여 람다식으로 구현합니다.
//        //
//        // (web) : customize(WebSecurity web) 메서드가 매개변수로 전달받는 WebSecurity 보안 설정 객체입니다.
//        // ->    : 왼쪽의 매개변수 web으로 전달 받은 WebSecurity 보안 설정 객체를  오른쪽의 코드로 실행한다는 의미입니다.
//        //
//        // 즉,  customize(WebSecurity web) 메서드를 이름이 없는 메서드인 람다식을 작성 한 web 매개변수로 WebSecurity 보안 설정 객체를 전달받아 보안 에서 제외할 요청 자원 경로를 설정합니다.
//        //
//        // - 람다식을 풀어서 쓰면 아래 익명 클래스와 같은 의미입니다.
//        //       return new WebSecurityCustomizer() {
//        //           @Override
//        //           public void customize(WebSecurity web) {
//        //               web.ignoring()
//        //                  .requestMatchers(toH2Console())
//        //                  .requestMatchers("/static/**");
//        //           }
//        //       };
//        return (web) -> web
//
//                // [2] .ignoring()
//                // - WebSecurity 보안 설정 객체 안에 있는 메서드로, 특정 요청을 "스프링 시큐리티의 감시 대상(보안 필터 체인)에서 완전히 제외하겠다"는 선언입니다.
//                // - 로그인 여부, 권한 체크 등을 아예 진행하지 않고 통과시킵니다.
//                .ignoring()
//
//                //.requestMatchers(toH2Console())
//                // - ignoring() 뒤에 붙어서 "어떤 요청 경로를 감시에서 제외할 것인가?"를 지정합니다.
//                // - toH2Console(): 개발 및 학습용 H2 데이터베이스 콘솔 주소(예: /h2-console)를 자동으로 인식해주는 기능입니다.
//                // - 이 설정을 통해 개발자는 로그인 없이도 H2 DB 관리 화면에 편하게 접근할 수 있습니다.
//                //
//                // -> http://localhost:8080/h2-console 브라우저에서 요청하면?
//                //   1. 스프링 시큐리티가 이 요청을 검사하지 않고 그냥 통과시킵니다.
//                //   2. application.yaml 의 spring.h2.console.enabled: true 설정으로 켜진 H2 콘솔 화면이 보여집니다.
//                //   3. 접속 화면에서 JDBC URL 에 jdbc:h2:mem:testdb , User Name 에 sa 를 입력하고 Connect 를 누르면
//                //      ARTICLE 테이블, USERS 테이블에 저장된 데이터를 SELECT 해서 직접 볼 수 있습니다.
//                //      (회원가입 후 USERS 테이블을 조회하면 PASSWORD 열에 $2a$10$... 처럼 암호화된 글자가 저장된 것을 확인할 수 있습니다.)
//                .requestMatchers(toH2Console())
//
//                //.requestMatchers("/static/**")
//                // - 또 다른 보안 제외 경로를 추가합니다.
//                // - "/static/**": /static/ 폴더 하위에 있는 모든 파일과 경로를 의미합니다.
//                //   예: /static/css/style.css, /static/js/app.js, /static/images/logo.png 등
//                // - 이유: 이미지, CSS, JS 같은 정적 리소스는 누구나 볼 수 있어야 하고,
//                //   보안 검사를 거치면 웹 페이지 로딩 속도가 느려지거나 화면 스타일이 깨질 수 있기 때문입니다.
//                //
//                // ※ 주의 : 스프링 부트는 src/main/resources/static 폴더의 파일을 주소에 "/static" 없이 제공합니다.
//                //   예) static/js/article.js 파일의 실제 요청 주소는 /static/js/article.js 가 아니라 /js/article.js 입니다.
//                //   그래서 /js/article.js 요청은 이 ignoring 설정에 걸리지 않고, 아래 [8] 의 .anyRequest().authenticated() 에 걸립니다.
//                //   -> 로그인한 사용자가 보는 화면(article.html, newArticle.html)에서만 article.js 를 쓰기 때문에 지금은 문제없이 동작합니다.
//                .requestMatchers("/static/**");
//    }
//
//    // ========================================================
//    // [8. 핵심 보안 설정]
//    // ========================================================
//
//    // @Bean: 메서드가 반환하는 SecurityFilterChain 객체를 스프링 컨테이너(메모리)에 등록하여 보안 필터로 사용합니다.
//    //
//    // - HttpSecurity http 매개변수는 누가 넣어 주나요?
//    //   우리가 직접 new HttpSecurity() 하지 않습니다.
//    //   프로그램이 시작될 때 스프링이 HttpSecurity 객체를 만들어서 이 메소드를 호출하면서 매개변수로 넣어 줍니다.
//    //
//    // - throws Exception : http 설정 도중 오류가 나면 이 메소드를 호출한 스프링에게 예외를 넘기겠다는 뜻입니다.
//    //
//    // - 이 메소드가 만든 SecurityFilterChain 은 "모든 웹 요청이 컨트롤러에 도착하기 전에 거치는 검문소" 입니다.
//    //
//    //      브라우저 요청
//    //          │
//    //          ▼
//    //    [ SecurityFilterChain 검문소 ]  ← 이 메소드가 만든 규칙으로 검사 (로그인 했나? 허용된 주소인가?)
//    //          │ 통과
//    //          ▼
//    //    [ 컨트롤러 (@GetMapping, @PostMapping 메소드) ]
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//
//        // http: 스프링 보안 설정을 조립하는 핵심 빌더 HttpSecurity 객체입니다.
//        // 메서드 체이닝( . 메서드 ) 방식으로 설정을 이어 나갑니다.
//        //   http.authorizeHttpRequests(...)   -> 반환값 HttpSecurity
//        //       .formLogin(...)               -> 반환값 HttpSecurity
//        //       .logout(...)                  -> 반환값 HttpSecurity
//        //       .csrf(...)                    -> 반환값 HttpSecurity
//        //       .build();                     -> 반환값 SecurityFilterChain (최종 완성품)
//        //   각 메소드가 자기 자신(HttpSecurity)을 다시 돌려주기 때문에 점(.)으로 계속 이어 쓸 수 있습니다.
//        return http
//
//                // ----------------------------------------------------------------------
//                // [1] authorizeHttpRequests(Customizer<AuthorizeHttpRequestsConfigurer<...>> authorizeHttpRequestsCustomizer)
//                // ----------------------------------------------------------------------
//                // - 전달받는 람다식의 실제 정체:
//                //   org.springframework.security.config.Customizer<T> 인터페이스의 void customize(T t) 추상 메서드를 오버라이딩(구현)한 람다식입니다.
//                //
//                // - (auth) -> ... 분석:
//                //   - auth의 타입: AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
//                //   - 의미: 스프링이 전달해 준 'URL 권한 관리용 도구(auth)'를 받아서 customize(auth) 메서드의 내부 동작을 정의합니다.
//                //
//                // - 규칙은 "위에서부터 아래로" 순서대로 검사합니다.
//                //   요청 주소가 먼저 나온 규칙에 맞으면 그 규칙이 적용되고, 아래 규칙은 보지 않습니다.
//                //   그래서 permitAll() 규칙을 먼저 쓰고, 나머지 전부(anyRequest)는 맨 마지막에 씁니다.
//                .authorizeHttpRequests(auth -> auth
//
//                        // .requestMatchers(...): 특정 URL 패턴들을 선별하여 등록합니다.
//                        .requestMatchers(
//                                "/login",  // -> http://localhost:8080/login 브라우저에서 로그인 화면 요청 주소 등록 해 놓으면?
//                                           //    (로그인 안 한 사람이 로그인 화면을 못 보면 영원히 로그인을 못 하니까 반드시 허용해야 합니다!)
//                                           //
//                                           //   [GET /login : 로그인 화면 보여주기]
//                                           //
//                                           //   1.  UserViewController 클래스에 만들어 놓은 아래의 login() 메소드가 실행됨.
//                                           //       @GetMapping("/login")
//                                           //       public String login(){  return "login"; }
//                                           //
//                                           //   2.  templates/login.html 파일의 디자인을 브라우저로 전달해서 화면에 로그인 요청화면 보여줌
//                                           //
//                                           //   [POST /login : 이메일, 비밀번호 입력 후 Submit 버튼 눌러 로그인 요청]
//                                           //
//                                           //   1.  login.html 의 <form action="/login" method="POST"> 가
//                                           //       name="username"(이메일), name="password"(비밀번호) 값을 http://localhost:8080/login 으로 보냄.
//                                           //
//                                           //   2.  이 POST /login 요청은 우리가 만든 컨트롤러로 가지 않습니다!!
//                                           //       아래 [2] .formLogin() 설정으로 스프링 시큐리티가 자동으로 만든 로그인 필터가 가로채서 처리합니다.
//                                           //       (그래서 UserViewController 에는 @PostMapping("/login") 메소드가 없습니다.)
//                                           //
//                                           //   3.  스프링 시큐리티가 UserDetailService 클래스에 만들어 놓은 아래 loadUserByUsername() 메소드를 호출함.
//                                           //       "부장 UserDetailService 야~ 이 이메일 가진 회원 DB 에서 찾아 와라~"
//                                           //
//                                           //            @Override
//                                           //            public UserDetails loadUserByUsername(String email) {
//                                           //                return userRepository.findByEmail(email)
//                                           //                        .orElseThrow(() -> new UsernameNotFoundException("회원을 찾을 수 없습니다. email=" + email));
//                                           //            }
//                                           //
//                                           //   4.  userRepository.findByEmail(email);
//                                           //       SELECT * FROM users WHERE email = '입력한 이메일'  실행해서
//                                           //       찾은 회원 한 명을 new User(조회된 정보) 객체에 담아 반환해 옵니다.
//                                           //       (User 클래스는 UserDetails 를 구현(implements)했기 때문에 스프링 시큐리티가 그대로 받을 수 있습니다.)
//                                           //       회원이 없으면 UsernameNotFoundException 예외 -> 로그인 실패.
//                                           //
//                                           //   5.  스프링 시큐리티가 아래 [9] passwordEncoder() 로 등록한 BCryptPasswordEncoder 객체를 꺼내서
//                                           //       passwordEncoder.matches("입력한 비밀번호", "DB에 저장된 암호문") 으로 비밀번호를 비교합니다.
//                                           //
//                                           //   6.  비밀번호가 맞으면 로그인 성공!
//                                           //       - 서버 세션에 로그인 정보를 저장하고, 브라우저에는 JSESSIONID 쿠키를 줍니다.
//                                           //       - 아래 [2] .defaultSuccessUrl("/articles") 설정에 의해 /articles 글 목록 화면으로 이동합니다.
//                                           //       비밀번호가 틀리면 로그인 실패!
//                                           //       - 스프링 시큐리티가 기본으로 /login?error 주소로 다시 이동시킵니다. (로그인 화면이 다시 보임)
//
//                                "/signup", // -> http://localhost:8080/signup 브라우저에서 가입 화면 요청 주소 등록 해 놓으면?
//                                           //   1.  UserViewController 클래스에 만들어 놓은  아래의 signup() 메소드가 실행됨.
//                                           //    @GetMapping("/signup")
//                                           //    public  String signup(){  return "signup"; }
//                                           //
//                                           //   2.  templates/signup.html 파일의 디자인을 브라우저로 전달해서 화면에 회원가입 요청화면보여줌
//                                           //
//                                           //   ※ login.html 화면 아래의 [회원가입] 버튼(onclick="location.href='/signup'")을 눌러도 이 주소로 요청합니다.
//
//                                "/user"    // -> 회원가입 작성후 가입요청하는 페이지 :  templates/signup.html의 화면을 브라우저에서 보고
//                                           //    가입할 이메일 , 비밀번호 입력후  submit(가입버튼) 눌러 가입 요청 한 주소  http://localhost:8080/user 를 등록 해 놓으면?
//                                           //    (signup.html 의 <form th:action="@{/user}" method="POST"> 가 이 주소로 보냅니다.)
//                                           //    가입 요청을 했을때....
//                                           //
//                                           //   1. UserApiController 클래스에 만들어 놓은 아래의  @PostMapping("/user")이 작성된  signup() 메소드가 실행됨.
//                                           //   @PostMapping("/user")
//                                           //    public String signup(AddUserRequest request){
//                                           //          userService.save(request);
//                                           //          return "redirect:/login";
//                                           //    }
//                                           //       - 이때 스프링이 form 의 name="email", name="password" 값을
//                                           //         AddUserRequest 객체의 email, password 변수에 자동으로 담아서 request 매개변수로 넣어 줍니다.
//                                           //
//                                           //   2.  userService.save(request); 가 실행되어 부장 니가 INSERT(회원가입) 시켜라~  AddUserRequest dto넘겨 줄게~
//                                           //
//                                           //   3.  UserService 클래스에 만들어 놓은 save() 가 호출 당해 실행 될때 매개변수로 AddUserRequest dto 를 받아
//                                           //       아래의 save() 메소드가 실행 됩니다.
//                                           //
//                                           //            public Long save(AddUserRequest dto){
//                                           //
//                                           //                  return userRepository.save(User.builder()
//                                           //                                        .email(dto.getEmail())
//                                           //                                        .password( passwordEncoder.encode(dto.getPassword()))
//                                           //                                        .build()).getId();
//                                           //          }
//                                           //
//                                           //       - passwordEncoder.encode(dto.getPassword())
//                                           //         : 아래 [9] passwordEncoder() 메소드로 등록한 BCryptPasswordEncoder 객체가 UserService 에 주입되어
//                                           //           입력한 비밀번호 "1234" 를 "$2a$10$Xy...." 같은 암호문으로 바꿔서 저장합니다.
//                                           //           (DB 에 비밀번호 원본이 그대로 저장되지 않습니다!)
//                                           //
//                                           //  4.   userRepository.save(new User(가입시 입력한 정보));
//                                           //       INSERT INTO users (email, password, nickname) VALUES (?, ?, ?)  실행해서
//                                           //       INSERT(가입) 시킨후 가입시킨 new User(가입된 정보); 를 반환해 옵니다.
//                                           //       마지막으로  return new User(가입된 정보).getId(); 를 UserApiController의  signup메소드 내부로 반환.
//                                           //
//                                           //  5.   UserApiController의 signup메소드 내부의 코드 중에서 아래의 코드 줄이 실행됩니다.
//                                           //
//                                           //        //회원가입 처리 성공시 /login 로 재요청해서 로그인 요청페이지를 보여주기 위해
//                                           //        // 웹 보안 요청주소가 등록된  WebSecurityConfig.java 파일에 작성된
//                                           //        // filterChain(HttpSecurity http) 메소드 내부에 작성된
//                                           //        // .requestMatchers("/login", ......) 메소드 설정에 의한
//                                           //        // UserViewController클래스에 작성해 놓은
//                                           //        //    @GetMapping("/login") public String login(){ return "login"; }
//                                           //        //    위 코드에 의해  templates/login.html 로그인 요청 화면을 보여 주게 된다.
//                                           //        return "redirect:/login";
//                                           //
//                                           //  ※ 만약 "/user" 를 여기에 등록하지 않으면?
//                                           //    로그인 안 한 사람(= 아직 가입 안 한 사람)의 가입 요청이 아래 .anyRequest().authenticated() 에 걸려서
//                                           //    /login 화면으로 튕겨 나가고, 회원가입이 영원히 되지 않습니다.
//                        )
//
//                        // .permitAll(): 지정된 URL 요청은 인증(로그인) 없이 전면 허용합니다.
//                        // - 바로 위 .requestMatchers("/login", "/signup", "/user") 3개 주소에 적용됩니다.
//                        // - GET, POST 방식 상관없이 모두 허용합니다.
//                        .permitAll()
//
//                        // .anyRequest(): 위에서 설정한 URL 목록 이외의 '나머지 모든 요청'을 지칭합니다.
//                        // .authenticated(): 나머지는 반드시 로그인(인증)을 완료해야만 접근을 허용합니다.
//                        //
//                        // -> 로그인 해야만 볼 수 있는 주소 예시 (이 프로젝트에 실제로 있는 주소)
//                        //
//                        //    [화면 주소 - BlogViewController]
//                        //      GET  /articles          : 글 목록 화면    (articleList.html)
//                        //      GET  /articles/{id}     : 글 상세 화면    (article.html)
//                        //      GET  /new-article       : 글 작성/수정 화면 (newArticle.html)
//                        //
//                        //    [API 주소 - BlogApiController, UploadController]
//                        //      POST   /api/articles        : 글 등록
//                        //      GET    /api/articles        : 글 목록 JSON
//                        //      GET    /api/articles/{id}   : 글 한 개 JSON
//                        //      PUT    /api/articles/{id}   : 글 수정
//                        //      DELETE /api/articles/{id}   : 글 삭제
//                        //      POST   /api/ai-suggestions  : AI 글쓰기 도우미
//                        //      POST   /api/ai-thumbnails   : AI 썸네일 생성
//                        //      POST   /api/upload          : 이미지 업로드
//                        //
//                        // -> 로그인 안 한 상태로 http://localhost:8080/articles 를 요청하면?
//                        //   1. 이 .anyRequest().authenticated() 규칙에 걸립니다.
//                        //   2. BlogViewController 의 getArticles() 메소드는 실행되지 않습니다.
//                        //   3. 아래 [2] .loginPage("/login") 설정에 의해 /login 로그인 화면으로 이동합니다.
//                        //   4. 로그인에 성공하면 원래 가려던 /articles 화면으로 다시 돌아갑니다.
//                        .anyRequest().authenticated()
//                )
//
//
//                // ----------------------------------------------------------------------
//                // [2] formLogin(Customizer<FormLoginConfigurer<HttpSecurity>> formLoginCustomizer)
//                // ----------------------------------------------------------------------
//                // - 전달받는 람다식의 실제 정체:
//                //   org.springframework.security.config.Customizer<T> 인터페이스의 void customize(T t) 추상 메서드를 오버라이딩(구현)한 람다식입니다.
//                //
//                // - (formLogin) -> ... 분석:
//                //   - formLogin의 타입: FormLoginConfigurer<HttpSecurity>
//                //   - 의미: 스프링이 전달해 준 '폼 로그인 설정용 도구(formLogin)'를 받아서 customize(formLogin) 메서드의 내부 동작을 정의합니다.
//                //
//                // - 폼 로그인이란?
//                //   HTML <form> 화면에 이메일, 비밀번호를 입력하고 Submit 버튼으로 로그인하는 방식입니다.
//                //   이 설정을 켜면 스프링 시큐리티가 "POST /login 로그인 처리 기능"을 자동으로 만들어 줍니다.
//                //
//                //   - 기본 입력칸 이름 : username(아이디), password(비밀번호)
//                //     그래서 login.html 의 이메일 입력칸은 name="email" 이 아니라 name="username" 으로 작성되어 있습니다.
//                //   - username 으로 받은 값은 UserDetailService 의 loadUserByUsername(String email) 메소드의 email 매개변수로 전달됩니다.
//                .formLogin(formLogin -> formLogin
//
//                        // .loginPage("/login"): Custom 로그인 페이지 경로 지정
//                        // - 비로그인 상태로 권한이 필요한 페이지 요청 시 이동시킬 URL입니다.
//                        // - 실제로 /login URL을 처리하는 Controller 및 HTML/Thymeleaf 뷰가 준비되어 있어야 합니다.
//                        //   -> UserViewController 의 @GetMapping("/login") login() 메소드
//                        //   -> templates/login.html 화면
//                        // - 이 설정이 없으면 스프링 시큐리티가 기본으로 제공하는 하얀 로그인 화면이 대신 보여집니다.
//                        .loginPage("/login")
//
//                        // .defaultSuccessUrl("/articles"): 로그인 성공 시 이동할 기본 URL
//                        // - 인증 완료 후 기본적으로 이동시킬 페이지를 /articles로 지정합니다.
//                        // - 사용자가 직전에 접속하려던 페이지가 있었다면 원래 페이지로 가고,
//                        //   로그인 버튼을 눌러 직통으로 접근한 경우 /articles로 이동합니다.
//                        //   (무조건 /articles로 강제 이동시키려면 .defaultSuccessUrl("/articles", true) 사용)
//                        //
//                        // -> 로그인 성공 후 /articles 로 이동하면?
//                        //   1. 이제 로그인 상태이므로 [1] .anyRequest().authenticated() 검사를 통과합니다.
//                        //   2. BlogViewController 의 @GetMapping("/articles") getArticles() 메소드가 실행됨.
//                        //   3. blogService.findAll() -> blogRepository.findAll()
//                        //      SELECT * FROM article  실행해서 글 목록을 가져옵니다.
//                        //   4. model.addAttribute("articles", 글목록); 으로 화면에 넘겨주고
//                        //      return "articleList"; 에 의해 templates/articleList.html 글 목록 화면을 보여줌.
//                        .defaultSuccessUrl("/articles")
//                )
//
//                // ----------------------------------------------------------------------
//                // [3] logout(Customizer<LogoutConfigurer<HttpSecurity>> logoutCustomizer)
//                // ----------------------------------------------------------------------
//                // - 전달받는 람다식의 실제 정체:
//                //   org.springframework.security.config.Customizer<T> 인터페이스의 void customize(T t) 추상 메서드를 오버라이딩(구현)한 람다식입니다.
//                //
//                // - (logout) -> ... 분석:
//                //   - logout의 타입: LogoutConfigurer<HttpSecurity>
//                //   - 의미: 스프링이 전달해 준 '로그아웃 설정용 도구(logout)'를 받아서 customize(logout) 메서드의 내부 동작을 정의합니다.
//                //
//                // - [수정 내용] 예전에는 로그아웃 처리가 UserApiController 의 /logout 메소드에도 따로 있어서
//                //   두 곳이 같은 일을 하고 있었습니다. 이제 로그아웃은 이 설정 한 곳에서만 처리합니다.
//                //
//                // -> 로그인 한 상태에서 http://localhost:8080/logout 을 요청하면?
//                //   1. 이 요청은 우리가 만든 컨트롤러로 가지 않습니다!!
//                //      스프링 시큐리티의 로그아웃 필터가 먼저 가로채서 처리합니다. (그래서 @GetMapping("/logout") 메소드가 없습니다.)
//                //   2. 아래 .invalidateHttpSession(true) -> 서버에 저장된 로그인 정보(세션)를 삭제합니다.
//                //   3. 아래 .deleteCookies("JSESSIONID") -> 브라우저의 세션 쿠키를 삭제합니다.
//                //   4. 아래 .logoutSuccessUrl("/login") -> /login 로그인 화면으로 이동합니다.
//                //   5. 이제 다시 /articles 를 요청하면 로그인 안 한 상태라서 /login 으로 튕겨 나갑니다.
//                //
//                // ※ 현재 articleList.html 에는 로그아웃 버튼이 없습니다.
//                //   브라우저 주소창에 http://localhost:8080/logout 을 직접 입력해서 로그아웃 할 수 있습니다.
//                //   버튼을 만들고 싶다면 화면에 아래 코드를 추가하면 됩니다.
//                //     <button type="button" class="btn btn-secondary" onclick="location.href='/logout'">로그아웃</button>
//                .logout(logout -> logout
//
//                        // .logoutUrl("/logout"): 로그아웃을 요청하는 주소를 "/logout" 으로 정합니다.
//                        // - 이 요청이 오면 스프링 시큐리티의 로그아웃 필터가 먼저 가로채서 처리합니다.
//                        // - 아래 [4] 에서 CSRF 보호를 껐기 때문에 GET 방식 요청(주소창 입력, location.href)도 로그아웃으로 인정됩니다.
//                        //   (CSRF 보호가 켜져 있으면 로그아웃은 POST 방식 + CSRF 토큰으로만 가능합니다.)
//                        .logoutUrl("/logout")
//
//                        // .logoutSuccessUrl("/login"): 로그아웃 성공 후 이동할 URL
//                        // - 로그아웃 처리(세션 파기, 쿠키 삭제 등)가 완료된 후
//                        //   사용자 브라우저를 이동시킬 페이지 경로(/login)를 지정합니다.
//                        // - /login 은 [1] 에서 permitAll() 했으므로 로그아웃한 사람도 볼 수 있습니다.
//                        //   -> UserViewController 의 login() 메소드 -> templates/login.html 화면
//                        .logoutSuccessUrl("/login")
//
//                        // .deleteCookies("JSESSIONID"): 로그아웃할 때 브라우저에 저장된 세션 쿠키를 삭제합니다.
//                        // - JSESSIONID 는 "이 브라우저가 어떤 세션인지" 알려 주는 쿠키 이름입니다.
//                        //   (로그인에 성공하면 톰캣 서버가 브라우저에게 JSESSIONID=ABC123... 같은 번호표를 줍니다.
//                        //    브라우저는 다음 요청부터 이 번호표를 같이 보내서 "나 아까 로그인한 사람이야~" 라고 알립니다.)
//                        // - 쿠키까지 지우면 브라우저에 로그인 흔적이 남지 않습니다.
//                        .deleteCookies("JSESSIONID")
//
//                        // .invalidateHttpSession(true): HTTP 세션 파기 설정
//                        // - 서버 메모리에 저장되어 있던 사용자의 로그인 상태 및
//                        //   세션 정보(JSESSIONID 관련 데이터)를 완전히 삭제(invalid)합니다.
//                        // - 서버 쪽 기록(세션) 삭제 = invalidateHttpSession(true)
//                        //   브라우저 쪽 번호표(쿠키) 삭제 = deleteCookies("JSESSIONID")
//                        //   두 가지를 모두 해야 깔끔하게 로그아웃 됩니다.
//                        .invalidateHttpSession(true)
//                )
//
//
//                // ----------------------------------------------------------------------
//                // [4] csrf(Customizer<CsrfConfigurer<HttpSecurity>> csrfCustomizer)
//                // ----------------------------------------------------------------------
//                // - 전달받는 코드의 실제 정체:
//                //   org.springframework.security.config.Customizer<T> 인터페이스의 void customize(T t) 추상 메서드를 오버라이딩(구현)한 메서드 참조 표현식입니다.
//                //
//                // - AbstractHttpConfigurer::disable 문법해석:
//                //   1) 원본 익명클래스: new Customizer<CsrfConfigurer<...>>() {
//                //                        @Override
//                //                        public void customize(CsrfConfigurer<...> csrf) {
//                //                            csrf.disable();
//                //                        }
//                //                     }
//                //   2) 람다식 축약형: (csrf) -> csrf.disable()
//                //   3) 메서드 참조형: AbstractHttpConfigurer::disable
//                //                     (CsrfConfigurer가 상속받은 부모 클래스 AbstractHttpConfigurer의 disable() 호출)
//                //
//                // - 기능: CSRF(사이트 간 요청 위조) 공격 방어 기능을 비활성화합니다.
//                //   (학습/개발 환경이나 REST API, H2 콘솔 사용 시 편리함을 위해 설정합니다.)
//                //
//                // - CSRF 공격이란?
//                //   내가 우리 블로그에 로그인한 상태에서 나쁜 사이트를 열었을 때,
//                //   그 사이트가 몰래 우리 블로그로 "글 삭제" 요청을 보내는 공격입니다.
//                //   브라우저가 JSESSIONID 쿠키를 자동으로 같이 보내기 때문에 서버는 내가 보낸 요청인 줄 압니다.
//                //   CSRF 보호가 켜져 있으면 서버가 준 비밀 토큰(_csrf)이 없는 요청은 거절해서 이 공격을 막습니다.
//                //
//                // - 이 프로젝트에서 껐을 때 편해지는 점
//                //   1. static/js/article.js 의 fetch('/api/articles', {method:'POST'...}) 요청에
//                //      CSRF 토큰을 넣지 않아도 글 등록, 수정(PUT), 삭제(DELETE)가 됩니다.
//                //   2. H2 콘솔(/h2-console) 화면의 form 전송이 막히지 않습니다.
//                //   3. /logout 을 GET 방식(주소창 입력)으로도 요청할 수 있습니다.
//                //
//                // ※ 실제 서비스(운영)에서는 CSRF 보호를 켜 두는 것이 안전합니다.
//                .csrf(AbstractHttpConfigurer::disable)
//
//                // ----------------------------------------------------------------------
//                // [.build() 메서드]
//                // ----------------------------------------------------------------------
//                // - 소속 클래스: org.springframework.security.config.annotation.web.builders.HttpSecurity
//                // - 반환 타입: org.springframework.security.web.SecurityFilterChain
//                //
//                // - 역할: Builder 패턴의 마침표 메서드입니다.
//                //   1) 앞서 체이닝 방식으로 설정한 모든 보안 규칙들
//                //      (authorizeHttpRequests, formLogin, logout, csrf 등)을 한데 모읍니다.
//                //   2) 내부적으로 해당 설정들이 적용된 보안 필터(Filter)들을 차례대로 생성합니다.
//                //   3) 최종적으로 HTTP 요청을 순서대로 검사할 SecurityFilterChain 객체를 완성하여 반환합니다.
//                //
//                // - 반환된 SecurityFilterChain 객체는 @Bean 에 의해 스프링 컨테이너에 등록되고,
//                //   이후 들어오는 모든 웹 요청은 컨트롤러에 가기 전에 이 검문소를 먼저 통과합니다.
//                .build();
//    }
//
//
//    // ----------------------------------------------------------------------
//    // [9] passwordEncoder(): 비밀번호 암호화 빈(Bean) 등록
//    // ----------------------------------------------------------------------
//    // - @Bean: 반환되는 BCryptPasswordEncoder 객체를 스프링 컨테이너(메모리)에 등록합니다.
//    // - 반환 타입: org.springframework.security.crypto.password.PasswordEncoder (인터페이스)
//    // - 구체 클래스: org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder (구현체)
//    //
//    // - 객체 구상 패턴 (다형성):
//    //   반환 타입을 인터페이스(PasswordEncoder)로 지정하고,
//    //   실제 객체는 구현체(BCryptPasswordEncoder)를 생성하여 반환합니다.
//    //   이렇게 작성하면 나중에 암호화 알고리즘을 변경하더라도
//    //   다른 Service 클래스들의 코드를 수정할 필요가 없어집니다.
//    //
//    // -> 이 메소드로 등록한 객체는 어디에서 쓰이나요?
//    //
//    //   [회원가입할 때 : 암호화(encode)]
//    //   1. UserService 클래스에 아래 필드가 있습니다.
//    //          private final PasswordEncoder passwordEncoder;
//    //   2. @RequiredArgsConstructor 로 만들어진 생성자를 통해
//    //      스프링이 이 메소드가 반환한 BCryptPasswordEncoder 객체를 자동으로 넣어 줍니다. (생성자 주입)
//    //   3. save() 메소드에서 passwordEncoder.encode(dto.getPassword()) 로
//    //      "1234" -> "$2a$10$Xy...." 암호문으로 바꿔서 users 테이블에 INSERT 합니다.
//    //
//    //   [로그인할 때 : 비교(matches)]
//    //   1. 스프링 시큐리티가 컨테이너에 등록된 PasswordEncoder 객체를 자동으로 찾아서 사용합니다.
//    //   2. UserDetailService.loadUserByUsername() 이 DB 에서 찾아 온 User 의 암호문과
//    //      로그인 화면에서 입력한 비밀번호를 passwordEncoder.matches("1234", "$2a$10$Xy....") 로 비교합니다.
//    //   3. true 면 로그인 성공, false 면 로그인 실패(/login?error).
//    //
//    //   ※ 회원가입 때 암호화한 객체와 로그인 때 비교하는 객체가 "같은 BCrypt 방식" 이어야
//    //     비밀번호 비교가 제대로 됩니다. 그래서 @Bean 으로 하나만 등록해 두고 같이 씁니다.
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//
//        // new BCryptPasswordEncoder():
//        // - BCrypt 해시 함수를 사용하는 암호화 객체를 생성합니다.
//        // - 단방향 해시(One-way Hash): 암호화는 가능하지만, 원래 비밀번호로 복호화(역추적)는 불가능합니다.
//        // - Salt(솔트) 자동 추가: 같은 비밀번호 "1234"를 암호화해도 매번 다른 암호문 문자열이 생성되어 안전합니다.
//        // - 비밀번호 검증 시: passwordEncoder.matches("입력받은비번", "DB저장된암호문") 메서드를 사용합니다.
//        //
//        // - 암호문 모양 예시 : $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
//        //     $2a$ : BCrypt 알고리즘 버전
//        //     10$  : 암호화 반복 강도(2의 10승 = 1024번 반복). 숫자가 클수록 안전하지만 느려집니다.
//        //     나머지 : 솔트(22글자) + 실제 해시값(31글자)
//        return new BCryptPasswordEncoder();
//    }
//
//}
