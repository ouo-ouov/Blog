// ============================================================
// [파일 설명] 블로그 "화면"을 보여 주는 컨트롤러입니다.
// JSON 이 아니라 HTML 화면(Thymeleaf 템플릿)을 돌려줍니다.
//
// [@Controller 와 @RestController 의 차이]
//   @Controller : 메소드가 돌려준 문자열을 "화면 파일 이름"으로 보고 templates 폴더의 HTML 을 보여 줍니다.
//   @RestController(BlogApiController) : 돌려준 객체를 JSON 으로 바꿉니다.
//
// [전체 주소 목록]
//   GET /articles        → getArticles   → articleList.html
//   GET /articles/{id}   → getArticle    → article.html
//   GET /new-article     → newArticle    → newArticle.html (id 가 있으면 수정 화면)
// ============================================================
package com.shinsunyoung.blog.controller;

// Lombok : final 필드를 받는 생성자를 자동으로 만듭니다.
import lombok.RequiredArgsConstructor;

// Article : 글 엔티티입니다.
import com.shinsunyoung.blog.domain.Article;

// 화면에 넘길 데이터 상자들입니다.
//   ArticleListViewResponse : 목록 화면용 (id, 제목, 본문, 이미지)
//   ArticleViewResponse     : 상세·작성 화면용 (작성 시간 포함)
import com.shinsunyoung.blog.dto.ArticleListViewResponse;
import com.shinsunyoung.blog.dto.ArticleViewResponse;

// BlogService : 글 업무를 맡는 서비스입니다.
import com.shinsunyoung.blog.service.BlogService;

// @Controller : 화면을 돌려주는 컨트롤러라는 표시입니다.
import org.springframework.stereotype.Controller;

// Model : 화면(HTML)에 넘길 데이터를 이름표를 붙여 담는 통입니다.
//   model.addAttribute("이름", 값) 으로 넣으면, HTML 에서 ${이름} 으로 꺼내 씁니다.
import org.springframework.ui.Model;

// 주소 표시들: @GetMapping(주소 GET 요청), @PathVariable(주소 안의 값), @RequestParam(?id=3 같은 값)
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

// List : 글 목록을 담는 자료형입니다.
import java.util.List;

// 아래 final 필드(blogService)를 받는 생성자를 자동으로 만듭니다.
@RequiredArgsConstructor
@Controller
public class BlogViewController {

    // 글 업무를 맡는 서비스입니다. 스프링이 생성자로 넣어 줍니다.
    private final BlogService blogService;

    // ---------------------------------------------------------------
    // 글 목록 화면 : GET /articles
    // ---------------------------------------------------------------
    @GetMapping("/articles")
    public String getArticles(Model model) {

        // 1) 모든 글을 가져와서 목록 화면용 상자 목록으로 바꿉니다.
        //    blogService.findAll() → stream() → map(ArticleListViewResponse::new) → toList()
        List<ArticleListViewResponse> articles = blogService.findAll().stream()
                .map(ArticleListViewResponse::new)
                .toList();

        // 2) Model 에 "articles" 라는 이름표로 목록을 넣습니다.
        //    articleList.html 의 th:each="item : ${articles}" 가 이 목록을 꺼내 반복 출력합니다.
        model.addAttribute("articles", articles);

        // 3) 돌려주는 문자열 "articleList" 는 templates/articleList.html 파일 이름입니다.
        //    스프링이 이 이름으로 HTML 을 찾아 값을 채운 뒤 브라우저에 보냅니다.
        return "articleList";
    }

    // ---------------------------------------------------------------
    // 글 상세 화면 : GET /articles/{id}
    // ---------------------------------------------------------------
    // @PathVariable Long id : 주소의 {id} 값을 Long 타입으로 꺼냅니다. (예: /articles/3 이면 3)
    @GetMapping("/articles/{id}")
    public String getArticle(@PathVariable Long id, Model model) {

        // 번호로 글 1개를 찾습니다.
        Article article = blogService.findById(id);

        // 상세 화면용 상자로 바꿔서 "article" 이름표로 Model 에 넣습니다.
        //   article.html 의 ${article.title} 등이 이 값을 꺼내 씁니다.
        model.addAttribute("article", new ArticleViewResponse(article));

        // templates/article.html 화면을 보여 줍니다.
        return "article";
    }

    // ---------------------------------------------------------------
    // 글 작성·수정 화면 : GET /new-article
    //   /new-article        → 새 글 작성 (id 없음)
    //   /new-article?id=3   → 3번 글 수정 (id 있음)
    // ---------------------------------------------------------------
    // @RequestParam(required = false) Long id :
    //   주소 뒤의 ?id=3 값을 꺼냅니다. required = false 는 "없어도 된다"는 뜻이고, 없으면 null 이 됩니다.
    @GetMapping("/new-article")
    public String newArticle(@RequestParam(required = false) Long id, Model model) {

        if (id == null) {
            // id 가 없으면 빈 상자를 넣습니다. newArticle.html 에서 제목·본문이 비어 있는 작성 화면이 됩니다.
            model.addAttribute("article", new ArticleViewResponse());
        } else {
            // id 가 있으면 그 글을 찾아서 넣습니다. 제목·본문이 채워진 수정 화면이 됩니다.
            Article article = blogService.findById(id);
            model.addAttribute("article", new ArticleViewResponse(article));
        }

        // templates/newArticle.html 화면을 보여 줍니다.
        return "newArticle";
    }
}
