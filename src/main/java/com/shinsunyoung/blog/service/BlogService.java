// 파일 위치: src/main/java/me/shinsunyoung/springbootdeveloper/service/BlogService.java
//
// ============================================================
// 이 파일이 하는 일
// ============================================================
// 블로그 글의 저장·조회·삭제·수정 "업무 처리"를 맡은 서비스입니다.
//
// ============================================================
// 10장 → 11장, 무엇이 바뀌었나  ("내 글만 고칠 수 있게")
// ============================================================
//   메서드     10장                          11장
//   ────────   ───────────────────────────   ───────────────────────────────────────────────────────
//   save       save(request)                 save(request, userName) → 글쓴이(userName)를 함께 저장
//   delete     deleteById(id) 바로 삭제       글을 찾아서 → 글쓴이 확인 → delete(article)
//   update     찾아서 바로 수정               글을 찾아서 → 글쓴이 확인 → 수정
//   (새 메서드)  -                            authorizeArticleAuthor(): 글쓴이와 요청한 사람 비교
//
// ============================================================
// "지금 요청한 사람"은 어디서 알까요?
// ============================================================
//   1. 브라우저가 Authorization: Bearer 액세스토큰 헤더를 붙여 요청
//   2. TokenAuthenticationFilter 가 토큰 검사 → TokenProvider.getAuthentication(token)
//        → 이름(name) = 토큰의 sub = 이메일 인 인증 정보를 SecurityContextHolder 에 넣음
//   3. 이 서비스에서 SecurityContextHolder.getContext().getAuthentication().getName() 으로 꺼냄
//        → "gildong@gmail.com"
//   SecurityContextHolder 는 "지금 이 요청을 처리하는 스레드"마다 따로 있는 보관함이라서,
//   컨트롤러에서 값을 넘겨받지 않아도 서비스에서 바로 꺼낼 수 있습니다.
//
// ============================================================
// 수정·삭제 흐름 예 (남의 글을 지우려 할 때)
// ============================================================
//   DELETE /api/articles/1  (요청자: gildong@gmail.com, 1번 글 author: user1)
//     → delete(1) → 글 찾음 → authorizeArticleAuthor(): "user1" ≠ "gildong@gmail.com"
//     → ResponseStatusException(403, "not authorized") 발생 → 삭제 안 됨   ([수정·삭제 완성] 예전: IllegalArgumentException, 500)
//     → 예외를 처리하는 코드가 없으므로 응답은 500 오류 → article.js 가 "삭제 실패했습니다." 알림
//
// ============================================================
// 꼭 조심할 점
// ============================================================
// 1. ✅ [해결] 권한이 없을 때 예전에는 IllegalArgumentException 이라 응답이 500 이었습니다.
//    지금은 ResponseStatusException(HttpStatus.FORBIDDEN) 을 던져 403(권한 없음)으로 응답합니다. (authorizeArticleAuthor 참고)
// 2. findAll(), findById() 는 글쓴이를 확인하지 않습니다. 글 읽기는 누구나 할 수 있습니다.
// 3. @Transactional 을 jakarta.transaction 패키지에서 가져왔습니다. (10장은 org.springframework.transaction.annotation)
//    스프링은 두 종류 모두 알아보므로 이 프로젝트에서는 동작이 같습니다.
// ============================================================


// 이 파일이 속한 폴더 이름입니다. 업무 처리 클래스들을 모아 두는 폴더입니다.
package com.shinsunyoung.blog.service;

import com.shinsunyoung.blog.repository.BlogRepository;
import jakarta.transaction.Transactional;                                // 메서드 전체를 하나의 작업 단위(트랜잭션)로 묶는 표시입니다. [11장] jakarta 쪽으로 바뀜
import lombok.RequiredArgsConstructor;                                   // final 변수를 받는 생성자를 만듭니다.
import com.shinsunyoung.blog.domain.Article;                // 글 클래스입니다.
import com.shinsunyoung.blog.dto.AddArticleRequest;        // 글 등록 요청 그릇입니다.
import com.shinsunyoung.blog.dto.UpdateArticleRequest;     // 글 수정 요청 그릇입니다.
import com.shinsunyoung.blog.repository.BlogRepository;    // article 표를 다루는 도구입니다.
import org.springframework.security.core.context.SecurityContextHolder;  // [11장] 지금 요청한 사용자의 인증 정보가 들어 있는 보관함입니다.
import org.springframework.stereotype.Service;                           // 스프링 빈(서비스)으로 등록하는 표시입니다.
import org.springframework.http.HttpStatus;                              // [수정·삭제 완성 추가] 403(FORBIDDEN) 같은 상태 코드 상수
import org.springframework.web.server.ResponseStatusException;           // [수정·삭제 완성 추가] 던지면 지정한 상태 코드로 응답되는 예외

import java.util.List; // 여러 글을 담는 목록 자료형입니다.

@RequiredArgsConstructor
@Service
public class BlogService {

    private final BlogRepository blogRepository; // article 표를 다루는 도구. 스프링이 넣어 줍니다.

    // ------------------------------------------------------------
    // save: 글 저장
    // 누가 부르나요: BlogApiController.addArticle() — principal.getName()(이메일)을 userName 으로 넘김
    // 받는 값   : request(제목·본문·이미지), userName(글쓴이 = 토큰 주인 이메일)
    // 돌려주는 값: 저장된 Article (id, createdAt 이 채워진 상태)
    // ------------------------------------------------------------
    public Article save(AddArticleRequest request, String userName) {
        return blogRepository.save(request.toEntity(userName));
    }

    // findAll: 모든 글 조회. 글쓴이 확인 없음 (누구나 읽기 가능)
    public List<Article> findAll() {
        return blogRepository.findAll();
    }

    // findById: 글 한 개 조회. 없으면 IllegalArgumentException("not found: 번호")
    public Article findById(long id) {
        return blogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("not found: " + id));
    }

    // ------------------------------------------------------------
    // delete: 글 삭제 (글쓴이만 가능)
    // 순서: ① 글 찾기 → ② 글쓴이 확인 → ③ 삭제
    // 10장은 deleteById(id) 로 바로 지웠지만, 글쓴이를 확인하려면 먼저 글을 꺼내야 해서 순서가 바뀌었습니다.
    // ------------------------------------------------------------
    public void delete(long id) {
        // ① 글 찾기. 없으면 "not found : 번호" 오류
        Article article = blogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("not found : " + id));

        // ② 지금 요청한 사람이 글쓴이인지 확인. 아니면 여기서 오류가 나고 아래 줄은 실행되지 않습니다.
        authorizeArticleAuthor(article);
        // ③ 삭제. 실행 SQL 예) DELETE FROM article WHERE id = 1
        blogRepository.delete(article);
    }

    // ------------------------------------------------------------
    // update: 글 수정 (글쓴이만 가능)
    // @Transactional: 메서드가 끝날 때 바뀐 값을 데이터베이스에 반영(UPDATE)합니다. (변경 감지)
    // ------------------------------------------------------------
    @Transactional
    public Article update(long id, UpdateArticleRequest request) {
        // ① 글 찾기
        Article article = blogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("not found : " + id));

        // ② 글쓴이 확인
        authorizeArticleAuthor(article);
        // ③ 값 바꾸기 (save 를 부르지 않아도 트랜잭션이 끝날 때 UPDATE 됨)
        article.update(request.getTitle(), request.getContent(), request.getImageUrl());

        return article;
    }


    // ------------------------------------------------------------
    // [11장 추가] authorizeArticleAuthor: 글쓴이 확인
    // ------------------------------------------------------------
    // private static : 이 클래스 안에서만, 객체 없이 쓰는 도우미 메서드입니다.
    // 1. SecurityContextHolder.getContext()  : 지금 요청의 보안 상자를 꺼냅니다.
    // 2. .getAuthentication()                : 그 안의 인증 정보 (TokenAuthenticationFilter 가 넣은 것)
    // 3. .getName()                          : 사용자 이름 = 토큰의 sub = 이메일
    // 4. 글의 author 와 다르면 "not authorized" 오류를 던집니다.
    // ⚠️ 인증 정보가 없으면 getAuthentication() 이 null 이라 오류가 날 수 있지만,
    //    /api/** 는 WebOAuthSecurityConfig 에서 authenticated 라서 인증 없이 여기까지 오지 못합니다.
    //
    // [수정·삭제 완성 - 변경] 예외 종류를 IllegalArgumentException → ResponseStatusException(403 FORBIDDEN) 으로 바꿨습니다.
    //   예전 : 남의 글을 수정·삭제하면 처리되지 않은 예외라서 응답이 500(서버 오류)이었습니다.
    //          화면에서는 "실패했습니다" 만 보여서, 서버 고장인지 권한 문제인지 알 수 없었습니다.
    //   지금 : ResponseStatusException 은 스프링 MVC 가 알아보는 예외라서, 던지면 지정한 상태 코드(403)로 응답합니다.
    //          403 = "누구인지는 알지만(로그인은 됨) 이 일을 할 권한이 없다" 는 뜻의 표준 상태 코드입니다.
    //          article.js 의 fail(response) 가 403 을 보고 "본인이 작성한 글만 …" 안내를 띄웁니다.
    //   ※ 글쓴이 본인이면 예외가 나지 않으므로 수정·삭제는 그대로 성공(200)합니다.
    private static void authorizeArticleAuthor(Article article) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!article.getAuthor().equals(userName)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not authorized"); // [변경] 500 → 403
        }
    }
}