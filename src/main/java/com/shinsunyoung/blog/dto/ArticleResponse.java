// ============================================================
// [파일 설명] REST API(/api/articles)가 JSON 으로 돌려주는 글 1개의 데이터 상자입니다.
// id 와 createdAt 은 담지 않고 제목, 본문, 이미지 주소만 돌려줍니다.
//
// [데이터 흐름]
//   BlogApiController.findAllArticles() : 목록 조회 시 ArticleResponse 목록을 만듦
//   BlogApiController.findArticle()     : 한 개 조회 시 ArticleResponse 1개를 만듦
//   → @RestController 가 객체를 JSON 문자열로 바꿔서 브라우저에 보냄
// ============================================================
package com.shinsunyoung.blog.dto;

// @Getter : getTitle(), getContent(), getImageUrl() 을 만듭니다. Jackson 이 JSON 으로 바꿀 때 이 메소드들을 호출합니다.
import lombok.Getter;

// Article : 변환의 원본 엔티티입니다.
import com.shinsunyoung.blog.domain.Article;

@Getter
public class ArticleResponse {

    // final : 생성 후 바꿀 수 없습니다.
    private final String title;
    private final String content;
    private final String imageUrl;

    // Article 객체에서 필요한 값만 복사합니다.
    //   호출하는 곳 : BlogApiController 의 .map(ArticleResponse::new) 및 new ArticleResponse(article)
    public ArticleResponse(Article article) {
        this.title = article.getTitle();            // 제목 복사
        this.content = article.getContent();        // 본문 복사
        this.imageUrl = article.getImageUrl();      // 이미지 주소 복사
    }
}
