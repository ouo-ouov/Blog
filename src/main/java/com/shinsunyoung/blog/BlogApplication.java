package com.shinsunyoung.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;


// @EnableJpaAuditing : 앱 전체에서 자동 기록 기능(auditing)을 켭니다.
// 이 표시가 있어야 Article 엔티티의 @CreatedDate, @LastModifiedDate 가 실제로 시각을 채웁니다.
// 기록을 실제로 하는 쪽은 Article 의 @EntityListeners(AuditingEntityListener.class) 이고, 이 표시는 전체 기능을 켜는 스위치입니다.
@EnableJpaAuditing

// @SpringBootApplication : 스프링 부트 프로젝트의 시작 표시입니다.
// SpringApplication.run 이 이 표시가 붙은 클래스를 기준으로 스프링 컨테이너(ApplicationContext 객체)를 만들고 프로젝트 설정을 읽습니다.
// 이 표시 안에는 아래 세 가지 기능이 들어 있습니다.
// 1) 설정 기능 : 이 클래스에서 설정을 할 수 있습니다.
// 2) 자동 설정 : 추가한 라이브러리를 보고 필요한 설정을 스프링 컨테이너가 자동으로 해 줍니다.
// 3) 컴포넌트 검색 : 이 파일이 있는 패키지(com.shinsunyoung.blog)와 그 아래 패키지
//    (controller, domain, dto, repository, service)를 검색해서 @Controller, @Service, @Repository 등이 붙은 클래스를 찾아
//    객체(빈)를 자동 생성하고 보관합니다.
@SpringBootApplication
public class BlogApplication {

    // main 메소드 : 프로그램이 가장 먼저 실행되는 시작점입니다.
    // 호출하는 곳 : 자바 실행기(JVM). IDE 의 ▶ 버튼이나 ./gradlew bootRun 명령도 결국 JVM 이 이 main 메소드를 호출합니다.
    // 호출한 곳으로 돌려주는 값은 없습니다.
    public static void main(String[] args) {

        // SpringApplication.run(...) : 스프링 부트를 시작하는 메소드입니다.
        // 첫 번째 값 SpringBootDeveloperApplication.class : 설정을 읽을 시작 클래스, 두 번째 값 args : 실행할 때 준 옵션
        // 이 메소드 안에서 아래 순서로 객체가 만들어집니다. (스프링 부트 4.0.1 앱을 실행해서 클래스 이름 확인)
        //   (1) 스프링 컨테이너 생성 : AnnotationConfigServletWebServerApplicationContext 객체를 만듭니다.
        //       이 객체 안에 빈을 만들고 보관하는 DefaultListableBeanFactory 객체가 함께 만들어집니다.
        //   (2) 컴포넌트 검색 : 이 클래스의 패키지와 그 아래 패키지에서 @Controller, @Service, @Repository 등이 붙은 클래스를 찾아
        //       만들 빈 목록(BeanDefinition)으로 DefaultListableBeanFactory 객체에 등록합니다.
        //   (3) 내장 톰캣 생성 : TomcatServletWebServerFactory 객체가 Tomcat 객체(웹 서버)를 만듭니다.
        //       톰캣은 스프링 컨테이너가 만드는 객체입니다. 톰캣이 빈을 만드는 것이 아닙니다.
        //   (4) 빈 생성과 보관 : DefaultListableBeanFactory 객체가 목록의 클래스마다 생성자를 호출해 객체(빈)를 만들고,
        //       @Autowired 변수와 생성자 매개변수에 필요한 빈을 넣은 뒤, 빈 이름과 함께 보관합니다.
        //   (5) 톰캣 시작 : 8080 포트에서 요청을 기다립니다. 들어온 요청은 DispatcherServlet 객체(이것도 빈)가 받아
        //       주소에 맞는 컨트롤러 메소드로 넘깁니다.
        SpringApplication.run(BlogApplication.class, args);
    }

}
