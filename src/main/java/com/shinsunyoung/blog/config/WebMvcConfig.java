// ============================================================
// [파일 설명] 웹 MVC 관련 추가 설정 파일입니다.
// 여기서는 "/uploads/..." 주소로 들어온 요청을 실제 업로드 폴더의 파일로 연결해 줍니다.
// 예: 브라우저가 /uploads/abc.png 를 요청하면 프로젝트 폴더의 uploads/abc.png 파일을 보내 줍니다.
// ============================================================
package com.shinsunyoung.blog.config;

// @Configuration : 이 클래스가 "설정 파일"이라고 스프링에 알려 주는 표시입니다.
//                  스프링은 시작할 때 이 표시가 붙은 클래스를 읽어서 설정을 적용합니다.
import org.springframework.context.annotation.Configuration;

// ResourceHandlerRegistry : "어떤 주소 요청을 어느 폴더에서 찾을지" 등록하는 도구 객체입니다.
//                           이 객체는 WebMvcConfigurer 의 addResourceHandlers 메소드로 스프링이 넘겨 줍니다.
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

// WebMvcConfigurer : 스프링 MVC 설정을 바꿀 때 구현(implements)하는 인터페이스입니다.
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// @Configuration 을 붙여서 이 클래스를 스프링 설정 클래스로 등록합니다.
@Configuration
// implements WebMvcConfigurer : "스프링 MVC 설정 규칙을 따르겠다"는 선언입니다.
public class WebMvcConfig implements WebMvcConfigurer {

    // @Override : 부모(인터페이스)에 이미 있는 addResourceHandlers 메소드를 내 방식대로 다시 만든다는 표시입니다.
    // 스프링이 시작될 때 이 메소드를 자동으로 호출하면서 registry 객체를 넘겨 줍니다.
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // addResourceHandler("/uploads/**") : "/uploads/ 로 시작하는 모든 요청"을 대상으로 지정합니다.
        //   ** 는 "그 아래 경로 전부"라는 뜻입니다. (예: /uploads/a.png, /uploads/2024/b.png 모두 해당)
        // .addResourceLocations("file:uploads/") : 그 요청을 찾을 실제 폴더를 지정합니다.
        //   file: 은 컴퓨터 파일 시스템에서 찾으라는 뜻이고, uploads/ 는 프로젝트 실행 위치의 uploads 폴더입니다.
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
