package com.shinsunyoung.blog.service;

import com.shinsunyoung.blog.dto.UploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/*
 * LocalFileStorageService : 이미지를 내 PC(프로젝트루트/uploads 폴더)에 저장하는 서비스
 *   - 인텔리제이에서 개발·테스트할 때만 사용한다.
 *   - 일래스틱 빈스톡에서는 AwsS3Service 가 대신 사용된다.
 *
 * [배포 서버에서 로컬 저장을 쓰지 않는 이유]
 *   빈스톡이 새 버전을 배포하거나 오토 스케일링으로 EC2 를 바꾸면 EC2 의 디스크 내용이 사라지고,
 *   EC2 가 2대 이상이면 1번 서버에 저장한 파일을 2번 서버는 갖고 있지 않다.
 *   그래서 배포 서버에서는 모든 서버가 함께 쓰는 S3 에 저장한다.
 */
// @Profile("local") : 활성화된 프로파일에 "local" 이 있을 때만 이 클래스를 빈으로 등록한다.
//   ConditionEvaluator 객체가 ProfileCondition 클래스로 검사한다.
//   인텔리제이 [구성 편집] → [활성화된 프로파일]에 local 을 입력하면
//   StandardEnvironment 객체의 activeProfiles 에 "local" 이 들어가 조건이 참이 된다.
//   AwsS3Service 의 @Profile("!local") 과 정확히 반대 조건이라 둘 중 하나만 등록된다.
//   (둘 다 등록되면 UploadController 주입 시 NoUniqueBeanDefinitionException,
//    둘 다 없으면 NoSuchBeanDefinitionException 이 발생한다.)
// @Service : 컴포넌트 스캔 대상 표시
@Profile("local")
@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path uploadDir;

    // @Value("${app.upload-dir:uploads}") : application.yml 의 app.upload-dir 값을 넣는다.
    //   콜론(:) 뒤 uploads 는 기본값. 현재 yml 에 app.upload-dir 이 없으므로 "uploads" 가 들어간다.
    //   생성자가 1개이므로 DefaultListableBeanFactory 객체가 이 생성자를 호출하며,
    //   매개변수 값은 AutowiredAnnotationBeanPostProcessor 의 값 해석 과정에서 StandardEnvironment 객체로부터 꺼낸다.
    public LocalFileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        // "uploads" → 실행 폴더 기준 절대 경로. 인텔리제이 실행 시 보통 프로젝트루트/uploads
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    // UploadController.uploadFile() 에서 호출됨 → UploadResponse 를 UploadController 로 return
    @Override
    public UploadResponse store(byte[] bytes, String filename) {
        try {
            // uploads 폴더가 없으면 만든다
            Files.createDirectories(uploadDir);

            // 확장자 추출 : "cat.PNG" → ".PNG" (AwsS3Service 와 달리 점 포함, 대소문자 그대로)
            String ext = filename.contains(".")
                    ? filename.substring(filename.lastIndexOf('.'))
                    : ".png";

            // 겹치지 않는 저장 파일 이름
            String saved = UUID.randomUUID() + ext;

            // 프로젝트루트/uploads/<UUID>.PNG 로 파일 쓰기
            Files.write(uploadDir.resolve(saved), bytes);

            // 브라우저가 접근할 주소 "/uploads/<UUID>.PNG" 를 return
            //   → 이 주소 요청은 WebMvcConfig 에 등록한 ResourceHttpRequestHandler 객체가
            //     프로젝트루트/uploads 폴더에서 파일을 찾아 응답한다.
            return new UploadResponse("/uploads/" + saved);

        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
}
