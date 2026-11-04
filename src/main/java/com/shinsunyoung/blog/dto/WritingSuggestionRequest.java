// ============================================================
// [파일 설명] "AI 글쓰기 도우미" 요청에서 보내는 제목, 본문, 질문을 담는 상자입니다.
//
// [데이터 흐름]
//   article.js 의 fetch('/api/ai-suggestions', POST) → JSON {"title", "content", "question"}
//   → BlogApiController.writingAssist(@RequestBody WritingSuggestionRequest request)
//   → WritingAssistantService.getWritingAssist(request) → request.toMap(형식지시문) 으로 프롬프트 값을 만듦
// ============================================================
package com.shinsunyoung.blog.dto;

// Map : 프롬프트 틀에 넣을 값들을 키와 값으로 담는 자료 구조입니다.
import java.util.Map;

// record WritingSuggestionRequest(String title, String content, String question)
//   필드 3개를 가진 상자입니다. 값은 title(), content(), question() 메소드로 꺼냅니다.
public record WritingSuggestionRequest(
        String title,       // 지금 작성 중인 글 제목
        String content,     // 지금 작성 중인 글 본문
        String question     // 사용자가 AI 에게 묻고 싶은 질문
) {
    // toMap : 프롬프트 틀(writing-assistant.st)의 {title}, {content}, {question}, {format} 자리에 넣을 값을 Map 으로 묶습니다.
    //   format 은 AI 답변을 JSON 모양으로 받기 위한 지시 글자입니다. (WritingAssistantService 에서 만들어 넘겨 줍니다)
    //   Map 의 키 이름과 .st 파일의 {} 이름이 같아야 값이 들어갑니다.
    public Map<String, Object> toMap(String format) {
        return Map.of(
                "title", title,         // {title} 자리
                "content", content,     // {content} 자리
                "question", question,   // {question} 자리
                "format", format        // {format} 자리 (JSON 형식 지시문)
        );
    }
}
