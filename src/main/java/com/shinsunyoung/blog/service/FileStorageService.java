// ============================================================
// [파일 설명] 업로드된 파일을 저장하는 기능의 "약속(규격)"을 정한 인터페이스입니다.
// 인터페이스는 "이런 메소드를 반드시 만들어라"는 목록입니다. 실제 저장 방법은 구현 클래스가 정합니다.
// 현재 구현 클래스는 LocalFileStorageService(내 컴퓨터 폴더에 저장)입니다.
//
// [이렇게 나눈 이유]
//   나중에 AWS S3 같은 다른 저장소로 바꾸고 싶을 때, 이 인터페이스를 쓰는 코드는 고치지 않고
//   새 구현 클래스만 만들면 됩니다.
// ============================================================
package com.shinsunyoung.blog.service;

// UploadResponse : 저장 결과(저장된 파일 주소)를 담는 상자입니다.
import com.shinsunyoung.blog.dto.UploadResponse;

// public interface : 다른 클래스가 이 규격을 구현(implements)하도록 만든 설계도입니다.
public interface FileStorageService {

    // store : 파일 내용(bytes)과 원래 파일 이름(filename)을 받아서 저장하고, 저장된 주소를 돌려주는 메소드 약속입니다.
    //   호출하는 곳 : UploadController.uploadFile(), ThumbnailGeneratorService.generateThumbnail()
    UploadResponse store(byte[] bytes, String filename);
}
