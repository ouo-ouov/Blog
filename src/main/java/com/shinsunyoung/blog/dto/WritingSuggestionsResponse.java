// ============================================================
// [파일 설명] AI 가 준 글쓰기 제안 목록을 담아 브라우저에 돌려주는 응답 상자입니다.
//
// [데이터 흐름]
//   WritingAssistantService : outputConverter.convert(response) 로 이 객체를 만듦
//   → BlogApiController.writingAssist() 가 ResponseEntity 로 감싸서 JSON 응답
//   → article.js 가 data.suggestions 배열을 꺼내 화면에 제안 목록으로 표시
// ============================================================
package com.shinsunyoung.blog.dto;

// List : 여러 개의 문자열을 순서대로 담는 목록입니다.
import java.util.List;

// record WritingSuggestionsResponse(List<String> suggestions)
//   JSON 으로 바뀌면 {"suggestions":["제안1","제안2","제안3"]} 형태가 됩니다.
//   이 모양은 BeanOutputConverter 가 AI 에게 지시하는 JSON 모양과 같아야 합니다.
public record WritingSuggestionsResponse(List<String> suggestions) {}
