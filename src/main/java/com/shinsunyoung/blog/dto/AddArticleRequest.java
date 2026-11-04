// ============================================================
// [파일 설명] 새 글을 등록할 때 브라우저가 보낸 JSON 데이터를 담는 상자(DTO)입니다.
// DTO = 화면과 서버 사이에서 데이터를 옮기기 위해 만든 객체입니다. DB 테이블과 연결되지 않습니다.
//
// [데이터 흐름]
//   article.js 의 fetch('/api/articles', POST) → JSON 본문
//   → Spring 이 JSON 을 이 객체로 변환 → BlogApiController.addArticle(@RequestBody AddArticleRequest request)
//   → BlogService.save(request) → request.toEntity() 로 Article 엔티티 생성 → DB 저장
// ============================================================
package com.shinsunyoung.blog.dto;

// @NoArgsConstructor : 값 없이 만드는 기본 생성자를 자동으로 만듭니다. JSON 을 객체로 바꿀 때 필요합니다.
// @AllArgsConstructor : 모든 필드를 받는 생성자를 자동으로 만듭니다.
// @Getter : getTitle(), getContent(), getImageUrl() 등을 자동으로 만듭니다.
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Article : 엔티티 클래스입니다. toEntity() 에서 이 타입을 만들어 돌려줍니다.
import com.shinsunyoung.blog.domain.Article;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class AddArticleRequest {

    // 제목, 본문, 이미지 주소를 담는 필드입니다. JSON 의 같은 이름 키 값이 여기에 들어옵니다.
    private String title;
    private String content;
    private String imageUrl;

    // 제목과 본문만 받는 생성자입니다. 테스트 코드(BlogApiControllerTest)에서 사용합니다.
    //   이 경우 imageUrl 은 null 로 남습니다.
    public AddArticleRequest(String title, String content) {
        this.title = title;
        this.content = content;
    }

    // toEntity : 이 DTO 의 값을 DB 에 저장할 Article 엔티티로 바꿔서 돌려줍니다.
    //   호출하는 곳 : BlogService.save() 메소드
    public Article toEntity(String author) {
        return Article.builder()        // Article 의 빌더(값을 하나씩 채우는 도구)를 시작합니다.
                .title(title)           // 이 DTO 의 제목을 넣습니다.
                .content(content)       // 본문을 넣습니다.
                .author(author)
                .imageUrl(imageUrl)     // 이미지 주소를 넣습니다.
                .build();               // 지금까지 넣은 값으로 Article 객체를 완성해서 돌려줍니다.
    }
}
