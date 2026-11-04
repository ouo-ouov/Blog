// ============================================================
// [파일 설명] 타임리프(Thymeleaf) 문법을 연습하기 위한 예제 컨트롤러입니다.
// 실제 블로그 기능과는 관계없이, 화면에 데이터를 넣는 방법만 보여 줍니다.
//
// [전체 주소]
//   GET /thymeleaf/example → thymeleafExample() → templates/example.html
// ============================================================
package com.shinsunyoung.blog.controller;

// Lombok : @Getter(값 읽기 메소드), @Setter(값 넣기 메소드)를 자동으로 만듭니다.
import lombok.Getter;
import lombok.Setter;

// @Controller : 화면을 돌려주는 컨트롤러라는 표시입니다.
import org.springframework.stereotype.Controller;

// Model : 화면에 넘길 데이터를 담는 통입니다.
import org.springframework.ui.Model;

// @GetMapping : GET 요청 주소를 지정하는 표시입니다.
import org.springframework.web.bind.annotation.GetMapping;

// LocalDate : 날짜(예: 2026-10-30)를 담는 자료형입니다.
import java.time.LocalDate;

// List : 목록 자료형입니다. 취미 목록에 사용합니다.
import java.util.List;

@Controller
public class ExampleController {

    // ---------------------------------------------------------------
    // 예제 화면 : GET /thymeleaf/example
    // ---------------------------------------------------------------
    @GetMapping("/thymeleaf/example")
    public String thymeleafExample(Model model) {

        // 1) 화면에 보여 줄 Person 객체를 만들고 값을 넣습니다.
        Person examplePerson = new Person();
        examplePerson.setId(1L);                            // 번호 1
        examplePerson.setName("홍길동");                    // 이름
        examplePerson.setAge(11);                           // 나이
        examplePerson.setHobbies(List.of("운동", "독서"));  // 취미 목록

        // 2) Model 에 이름표를 붙여 넣습니다.
        //    example.html 에서 ${person} 으로 Person 객체를, ${today} 로 날짜를 꺼냅니다.
        model.addAttribute("person", examplePerson);
        model.addAttribute("today", LocalDate.now());   // LocalDate.now() : 지금 날짜

        // 3) templates/example.html 을 보여 줍니다.
        return "example";
    }

    // @Setter, @Getter : 아래 필드들의 setXxx(), getXxx() 메소드를 자동으로 만듭니다.
    //   타임리프가 ${person.name} 을 만나면 getName() 을 호출합니다.
    @Setter
    @Getter
    // 이 클래스는 ExampleController 안에 들어 있는 "안쪽 클래스"입니다. 예제 화면용 데이터 상자입니다.
    class Person {
        private Long id;                // 번호
        private String name;            // 이름
        private int age;                // 나이
        private List<String> hobbies;   // 취미 목록
    }
}
