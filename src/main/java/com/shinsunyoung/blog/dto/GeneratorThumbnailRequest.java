// ============================================================
// [파일 설명] "AI 썸네일 생성" 요청에서 보내는 제목과 본문을 담는 상자입니다.
// record 는 자바 16 이상에서 쓸 수 있는 문법으로, 값을 담는 클래스를 짧게 만들 수 있습니다.
//   record 이름(매개변수)을 적으면 필드, 생성자, getter 가 자동으로 만들어집니다.
//   예: request.title() 로 제목을 꺼냅니다. (getTitle() 이 아니라 title() 이라고 부릅니다)
//
// [데이터 흐름]
//   article.js 의 fetch('/api/ai-thumbnails', POST) → JSON {"title":..., "content":...}
//   → BlogApiController.thumbnailGenerator(@RequestBody GeneratorThumbnailRequest request)
//   → ThumbnailGeneratorService.generateThumbnail(request)
//   → request.toMap() 으로 프롬프트 틀에 넣을 값을 Map 으로 만듦
// ============================================================
package com.shinsunyoung.blog.dto;

// Map : 키와 값을 짝지어 담는 자료 구조입니다.
import java.util.Map;

// record GeneratorThumbnailRequest(String title, String content)
//   이 record 가 만들어 주는 것: 필드 title, content 와 생성자, title(), content() 메소드
public record GeneratorThumbnailRequest(
        String title,       // 글 제목
        String content      // 글 본문
) {
    // toMap : 제목과 본문을 Map 으로 바꿉니다.
    //   Map 의 키("title", "content")는 prompts/thumbnail-generator.st 파일의 {title}, {content} 자리와 이름이 같아야 합니다.
    //   호출하는 곳 : ThumbnailGeneratorService.generateThumbnail() 의 template.create(request.toMap())
    public Map<String, Object> toMap() {
        return Map.of(
                "title", title,       // 키 "title" 에 제목 값
                "content", content    // 키 "content" 에 본문 값
        );
    }
}
