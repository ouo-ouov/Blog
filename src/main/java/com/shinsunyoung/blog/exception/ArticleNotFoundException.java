// ============================================================
// 파일 경로
// src/main/java/me/shinsunyoung/springbootdeveloper/exception/ArticleNotFoundException.java
// ============================================================
//
// [이 클래스의 역할]
//
// 요청한 글 번호가 DB 에 없을 때 발생시키는 예외(오류) 클래스입니다.
//
// [왜 만들었나요?]
//
// 예전에는 IllegalArgumentException 을 던졌는데, 이 오류는 웹 요청에서 "서버 내부 오류(500)"로 처리되었습니다.
// 글이 없는 것은 서버 잘못이 아니라 "찾을 수 없음(404)" 이므로, 전용 예외를 따로 만들었습니다.
//
// [호출하는 곳]
//
// 1. BlogService.findById()  : 글을 찾지 못했을 때
// 2. BlogService.update()    : 수정할 글을 찾지 못했을 때
// 3. BlogService.delete()    : 삭제할 글이 없을 때

package com.shinsunyoung.blog.exception;


// [1. 상태 코드 관련 import]
//
// HttpStatus : HTTP 상태 번호를 이름으로 쓸 수 있게 한 값입니다. NOT_FOUND 는 404 입니다.
import org.springframework.http.HttpStatus;

// [2. 예외와 응답 연결 관련 import]
//
// @ResponseStatus : 이 예외가 발생하면 어떤 HTTP 상태 코드로 응답할지 정하는 표시입니다.
import org.springframework.web.bind.annotation.ResponseStatus;


// [3. 어노테이션 설명]
//
// @ResponseStatus(HttpStatus.NOT_FOUND)
// - 이 예외가 컨트롤러 밖으로 던져지면, 스프링이 응답 상태 코드를 404(Not Found)로 만들어 보냅니다.
// - REST API 에서는 JSON 응답 상태가 404 가 되고, 화면 요청에서는 404 오류 페이지가 보입니다.
@ResponseStatus(HttpStatus.NOT_FOUND)

// [4. 클래스 선언]
// extends RuntimeException
// - RuntimeException 은 실행 중에 발생하는 예외의 부모 클래스입니다.
// - 이 클래스를 상속하면 메서드 뒤에 throws 를 적지 않아도 던질 수 있습니다.
public class ArticleNotFoundException extends RuntimeException {

    // [5. 생성자]
    // - 매개변수 id : 찾지 못한 글 번호입니다.
    // - super(...) : 부모 클래스(RuntimeException)의 생성자를 호출해서 오류 메시지를 저장합니다.
    //   만들어지는 메시지 예: "글을 찾을 수 없습니다. id=9999"
    public ArticleNotFoundException(long id) {
        super("글을 찾을 수 없습니다. id=" + id);
    }
}
