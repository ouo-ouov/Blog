// ============================================================
// [파일 설명] 글 "목록" 화면(articleList.html)에 보여 줄 데이터를 담는 상자입니다.
// Article 엔티티를 그대로 넘기지 않고, 화면에 필요한 4개 값만 골라서 담습니다.
//
// [데이터 흐름]
//   BlogViewController.getArticles() : blogService.findAll() 결과를 이 객체 목록으로 변환
//   → model.addAttribute("articles", articles)
//   → articleList.html 의 th:each="item : ${articles}" 에서 반복 출력
// ============================================================
package com.shinsunyoung.blog.dto;

// @Getter : getId(), getTitle(), getContent(), getImageUrl() 을 만듭니다.
//   타임리프는 ${item.title} 을 만나면 getTitle() 을 호출해서 값을 꺼냅니다.
import lombok.Getter;

// Article : 변환의 원본이 되는 엔티티입니다.
import com.shinsunyoung.blog.domain.Article;

@Getter
public class ArticleListViewResponse {

    // final : 한 번 값이 정해지면 바꿀 수 없습니다. 생성자에서만 값을 넣습니다.
    private final Long id;
    private final String title;
    private final String content;
    private final String imageUrl;

    // Article 객체를 받아서 필요한 값만 복사하는 생성자입니다.
    //   호출하는 곳 : BlogViewController.getArticles() 의 .map(ArticleListViewResponse::new)
    public ArticleListViewResponse(Article article) {
        this.id = article.getId();                  // 글 번호를 복사합니다.
        this.title = article.getTitle();            // 제목을 복사합니다.
        this.content = article.getContent();        // 본문을 복사합니다.
        this.imageUrl = article.getImageUrl();      // 이미지 주소를 복사합니다.
    }
}
