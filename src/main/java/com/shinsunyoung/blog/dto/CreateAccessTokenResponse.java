package com.shinsunyoung.blog.dto;
// 파일 위치: src/main/java/me/shinsunyoung/springbootdeveloper/dto/CreateAccessTokenResponse.java
//
// ============================================================
// 이 파일이 하는 일  [교안 10.3 · 01단계]
// ============================================================
// POST /api/token 요청의 "응답"으로 돌려줄 새 액세스 토큰을 담는 그릇(DTO)입니다.
// 스프링이 이 객체를 JSON 글자로 바꿔서 클라이언트에게 보냅니다.
//
//   자바 객체                                      응답 본문(JSON)
//   new CreateAccessTokenResponse("eyJ...")  ──▶  { "accessToken": "eyJ..." }
//
// ============================================================
// 전체 흐름에서의 위치
// ============================================================
//   클라이언트 ──(CreateAccessTokenRequest: refreshToken)──▶ TokenApiController
//                                                              │
//                                                              ▼
//                                                         TokenService (검증 + 2시간짜리 새 토큰 발급)
//                                                              │
//   클라이언트 ◀──(201 Created + CreateAccessTokenResponse: accessToken)──┘
//
// ============================================================
// 꼭 알아둘 낱말
// ============================================================
// DTO                 : 데이터를 옮기기 위한 객체(Data Transfer Object)입니다. 일하는 코드 없이 값만 담습니다.
// 응답 본문            : 서버가 돌려주는 내용 부분입니다. 여기서는 JSON 모양입니다.
// @AllArgsConstructor : 모든 변수를 받는 생성자를 자동으로 만듭니다. 예) new CreateAccessTokenResponse(토큰)
// @Getter             : getAccessToken() 을 자동으로 만듭니다.
//
// ============================================================
// 꼭 조심할 점
// ============================================================
// 1. JSON 으로 바꿀 때 스프링은 getAccessToken() 을 보고 "accessToken" 이라는 이름을 정합니다.
//    그래서 @Getter 가 없으면 응답이 비어 버립니다.
// 2. 테스트(TokenApiControllerTest)는 jsonPath("$.accessToken") 으로 이 이름을 확인합니다.
//    변수 이름을 바꾸면 테스트도 함께 바꿔야 합니다.
// ============================================================

import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor  // TokenApiController 에서 new CreateAccessTokenResponse(newAccessToken) 으로 만듭니다.
@Getter              //  JSON 으로 바꿀 때 getAccessToken() 이 쓰입니다.
public class CreateAccessTokenResponse {

    // 새로 발급한 액세스 토큰 문자열입니다. (유효 기간 2시간)
    // 클라이언트는 이 값을 저장해 두었다가, 다음 요청부터 헤더에
    // "Authorization: Bearer 이값" 으로 붙여 보냅니다.
    private String accessToken;


}
