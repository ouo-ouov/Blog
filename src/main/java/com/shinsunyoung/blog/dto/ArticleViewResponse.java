// ============================================================
// [파일 설명] 글 "상세"와 "작성·수정" 화면(article.html, newArticle.html)에 보여 줄 데이터를 담는 상자입니다.
// ArticleListViewResponse 와 달리 createdAt(작성 시간)도 담습니다.
//
// [데이터 흐름]
//   BlogViewController.getArticle()    : /articles/{id} → 글 1개를 담아 article.html 로 전달
//   BlogViewController.newArticle()    : /new-article (id 없음) → 빈 객체를 담아 newArticle.html 로 전달
//   BlogViewController.newArticle()    : /new-article?id=3 (수정) → 글 1개를 담아 newArticle.html 로 전달
// ============================================================
package com.shinsunyoung.blog.dto;

// @NoArgsConstructor : 값 없이 만드는 생성자입니다. 새 글 작성 화면에서 빈 객체를 만들 때 사용합니다.
// @Getter : getId(), getCreatedAt() 등을 만듭니다. 타임리프 ${article.id} 가 이 메소드를 호출합니다.
import lombok.Getter;
import lombok.NoArgsConstructor;

// Article : 변환의 원본 엔티티입니다.
import com.shinsunyoung.blog.domain.Article;

// LocalDateTime : 작성 시간을 담는 자료형입니다.
import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
public class ArticleViewResponse {

    // 빈 객체를 만들 수 있도록 final 을 쓰지 않았습니다. (newArticle 에서 값을 나중에 넣을 수 있게)
    private Long id;
    private String title;
    private String content;
    private String author;            // [11장 추가] 글쓴이 (이메일)
    private String imageUrl;
    private LocalDateTime createdAt;   // article.html 에서 작성 시간을 보여 줄 때 사용합니다.

    // Article 객체의 값을 복사합니다.
    //   호출하는 곳 : BlogViewController.getArticle(), BlogViewController.newArticle() 의 수정 분기
    public ArticleViewResponse(Article article) {
        this.id = article.getId();
        this.title = article.getTitle();
        this.content = article.getContent();
        this.author = article.getAuthor(); // [11장 추가] 글쓴이 (이메일)
        this.imageUrl = article.getImageUrl();
        this.createdAt = article.getCreatedAt();   // 작성 시간은 Article 에서 @CreatedDate 로 자동 기록된 값입니다.
    }
}
