// ============================================================
// [파일 설명] 글 "수정" 요청에서 브라우저가 보낸 JSON 을 담는 상자입니다.
// AddArticleRequest 와 구조가 같지만, 수정이라는 역할을 구분하기 위해 따로 만들었습니다.
//
// [데이터 흐름]
//   article.js 의 fetch('/api/articles/{id}', PUT) → JSON 본문
//   → BlogApiController.updateArticle(@PathVariable id, @RequestBody UpdateArticleRequest request)
//   → BlogService.update(id, request) → request.getTitle() 등으로 값을 꺼내 Article.update() 호출
// ============================================================
package com.shinsunyoung.blog.dto;

// @NoArgsConstructor : JSON 을 객체로 바꿀 때 쓰는 기본 생성자입니다.
// @AllArgsConstructor : 모든 필드를 받는 생성자입니다.
// @Getter : getTitle(), getContent(), getImageUrl() 을 만듭니다. BlogService 가 이 메소드로 값을 꺼냅니다.
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UpdateArticleRequest {

    // 수정할 제목, 본문, 이미지 주소입니다.
    private String title;
    private String content;
    private String imageUrl;

    // 이미지 주소 없이 제목과 본문만 받는 생성자입니다. 테스트 코드(BlogApiControllerTest.updateArticle)에서 사용합니다.
    public UpdateArticleRequest(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
