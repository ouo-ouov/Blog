// ============================================================
// [파일 설명] 이미지 파일을 업로드받는 REST API 컨트롤러입니다.
// 글쓰기 화면에서 파일을 고르면 article.js 가 이 주소로 파일을 보냅니다.
//
// [전체 주소]
//   POST /api/upload → uploadFile() → FileStorageService.store() → JSON 응답 {"imageUrl":"/uploads/..."}
// ============================================================
package com.shinsunyoung.blog.controller;

// UploadResponse : 응답 상자 (저장된 이미지 주소)
import com.shinsunyoung.blog.dto.UploadResponse;
import com.shinsunyoung.blog.service.FileStorageService;
import com.shinsunyoung.blog.dto.UploadResponse;

// FileStorageService : 파일 저장 규격입니다. 실제 구현은 LocalFileStorageService 입니다.
import com.shinsunyoung.blog.service.FileStorageService;

// HttpStatus, ResponseEntity : 응답 상태 코드와 본문을 담는 도구입니다.
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

// @PostMapping : POST 요청 주소 표시
// @RequestParam : 요청에 담긴 값 1개를 꺼내는 표시
// @RestController : 결과를 JSON 으로 돌려주는 컨트롤러 표시
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// MultipartFile : 브라우저가 보낸 업로드 파일 1개를 담는 객체입니다.
import org.springframework.web.multipart.MultipartFile;

// IOException : 파일을 읽을 때 생길 수 있는 오류입니다. 메소드 뒤의 throws 로 넘깁니다.
import java.io.IOException;

// @RestController : JSON 응답 컨트롤러라는 표시입니다.
@RestController
public class UploadController {

    // 파일 저장 기능을 가진 객체입니다.
    private final FileStorageService fileStorageService;

    // 생성자 : 스프링이 FileStorageService 구현 객체(LocalFileStorageService)를 넣어 줍니다.
    public UploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    // ---------------------------------------------------------------
    // 파일 업로드 : POST /api/upload
    // ---------------------------------------------------------------
    // @RequestParam("file") MultipartFile file :
    //   요청에 "file"이라는 이름으로 실린 업로드 파일을 꺼내 file 변수에 담습니다.
    //   (article.js 에서 FormData 의 키 이름이 "file" 이어야 합니다)
    @PostMapping("/api/upload")
    public ResponseEntity<UploadResponse> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {

        // 1) file.getBytes() : 업로드된 파일 내용을 byte 배열로 꺼냅니다.
        // 2) file.getOriginalFilename() : 사용자가 올린 원래 파일 이름입니다. (확장자 추출에 쓰입니다)
        // 3) fileStorageService.store(...) : 파일을 uploads 폴더에 저장하고 주소(UploadResponse)를 받습니다.
        // 4) 상태 코드 201(Created)과 함께 그 주소를 JSON 으로 돌려줍니다.
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fileStorageService.store(
                        file.getBytes(),
                        file.getOriginalFilename()
                ));
    }
}
