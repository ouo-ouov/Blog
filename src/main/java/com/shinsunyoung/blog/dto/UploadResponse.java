// ============================================================
// [파일 설명] 이미지 파일 업로드가 끝났을 때 돌려주는 응답 상자입니다.
// 저장된 파일의 주소(예: /uploads/uuid.png) 하나를 담습니다.
//
// [데이터 흐름]
//   LocalFileStorageService.store() 가 new UploadResponse("/uploads/" + 저장파일명) 으로 생성
//   → UploadController.uploadFile() 이 JSON 으로 돌려줌
//   → article.js 가 data.imageUrl 을 꺼내 image-url 입력칸에 넣음
// ============================================================
package com.shinsunyoung.blog.dto;

// record UploadResponse(String imageUrl)
//   JSON 으로 바뀌면 {"imageUrl":"/uploads/..."} 형태가 됩니다.
public record UploadResponse(
        String imageUrl     // 저장된 이미지의 주소
) {}
