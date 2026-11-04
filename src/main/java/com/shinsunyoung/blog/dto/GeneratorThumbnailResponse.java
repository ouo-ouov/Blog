// ============================================================
// [파일 설명] AI 썸네일 생성이 끝났을 때 브라우저에 돌려주는 응답 상자입니다.
// 이미지 자체가 아니라 "저장된 이미지 주소(예: /uploads/uuid.png)" 한 개만 담습니다.
//
// [데이터 흐름]
//   ThumbnailGeneratorService.generateThumbnail() 이 new GeneratorThumbnailResponse(saved.imageUrl()) 로 생성
//   → BlogApiController.thumbnailGenerator() 가 ResponseEntity 로 감싸서 JSON 응답
//   → article.js 가 data.imageUrl 을 꺼내서 image-url 입력칸에 넣음
// ============================================================
package com.shinsunyoung.blog.dto;

// record GeneratorThumbnailResponse(String imageUrl)
//   값 하나짜리 상자입니다. JSON 으로 바뀔 때 {"imageUrl":"/uploads/..."} 형태가 됩니다.
public record GeneratorThumbnailResponse(String imageUrl) {}
