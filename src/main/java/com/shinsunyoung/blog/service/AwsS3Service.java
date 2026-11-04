package com.shinsunyoung.blog.service;

import lombok.RequiredArgsConstructor;
import com.shinsunyoung.blog.dto.UploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

/*
 * AwsS3Service : 업로드된 이미지를 AWS S3 버킷의 uploads/ 폴더에 저장하고, 접근 URL 을 돌려주는 서비스
 *
 * [호출 흐름]
 *   article.js 의 fetch('/api/upload', { method: 'POST', body: FormData })
 *     → DispatcherServlet 객체가 요청을 받음
 *     → RequestMappingHandlerMapping 객체가 UploadController.uploadFile() 을 찾음
 *     → uploadFile() 이 fileStorageService.store(bytes, filename) 호출  ← 이 클래스의 store() 실행
 *     → store() 가 S3 에 업로드 후 UploadResponse(url) 을 return
 *     → UploadController 가 201 Created + {"imageUrl":"https://…"} JSON 으로 응답
 *     → article.js 가 imageUrl 을 받아 글의 이미지 주소로 사용
 */
// @Profile("!local") : 활성화된 프로파일에 "local" 이 없을 때만 이 클래스를 빈으로 등록한다.
//   ConditionEvaluator 객체가 ProfileCondition 클래스로 이 조건을 검사하고,
//   조건이 맞지 않으면 ClassPathBeanDefinitionScanner 객체가 이 클래스를 빈 등록 대상에서 뺀다.
//   - 일래스틱 빈스톡 : java -jar 로 프로파일 지정 없이 실행 → "local" 없음 → 이 클래스가 빈으로 등록됨
//   - 인텔리제이      : [구성 편집]에서 활성화된 프로파일 = local → 이 클래스는 등록되지 않음
//   - ./gradlew build : 테스트도 프로파일 없이 실행 → 이 클래스가 등록됨
// @Service : ClassPathBeanDefinitionScanner 객체가 컴포넌트 스캔으로 찾아 빈으로 등록하게 하는 표시
// @RequiredArgsConstructor : 롬복(Lombok) 애너테이션 프로세서가 컴파일할 때
//   final 필드(s3Client)를 매개변수로 받는 생성자 AwsS3Service(S3Client s3Client) 를 만들어 넣는다.
//   생성자가 1개뿐이므로 DefaultListableBeanFactory 객체가 이 생성자를 호출하면서
//   S3Config.s3Client() 로 등록된 S3Client 빈을 인자로 넣어 준다(생성자 주입).
@Profile("!local")
@Service
@RequiredArgsConstructor
public class AwsS3Service implements FileStorageService {

    // S3Config.java 에서 만든 S3Client 빈이 생성자를 통해 들어온다.
    private final S3Client s3Client;

    // @Value("${cloud.aws.s3.bucket}") : application.yml 의 cloud.aws.s3.bucket 값을 필드에 넣는다.
    //   빈 생성 직후 AutowiredAnnotationBeanPostProcessor 객체가 이 필드에 값을 대입한다.
    //   → bucket = "blog-ajufresh"
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    // UploadController.uploadFile() 에서 호출됨 → UploadResponse 를 UploadController 로 return
    @Override
    public UploadResponse store(byte[] bytes, String filename) {
        // 확장자 추출 : "cat.PNG" → "png" (점 뒤 문자열을 소문자로). 점이 없으면 "png" 로 간주
        String ext = filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase()
                : "png";

        // key : S3 버킷 안에서 파일을 구분하는 이름(경로 포함). 예) uploads/3f2a…-….png
        //   UUID.randomUUID() 로 매번 다른 이름을 만들어 같은 이름의 파일을 덮어쓰지 않게 한다.
        //   "uploads/" 로 시작해야 하는 이유: S3 버킷 정책의 Resource 가
        //   arn:aws:s3:::버킷이름/uploads/* 이므로 이 경로 아래 파일만 누구나 읽을 수 있다(s3:GetObject 허용).
        String key = "uploads/" + UUID.randomUUID() + "." + ext;

        // PutObjectRequest : S3 의 PutObject API(파일 업로드)에 보낼 요청 정보를 담는 객체.
        //   builder() 로 PutObjectRequest.Builder 객체를 얻어 값을 채운 뒤 build() 로 완성한다.
        PutObjectRequest request = PutObjectRequest.builder()
                                                    // 업로드할 버킷 이름
                                                    .bucket(bucket)
                                                    // Content-Type 헤더 값. "image/png" 로 저장해야 브라우저가 이 URL 을 열었을 때
                                                    // 파일을 다운로드하지 않고 이미지로 화면에 표시한다.
                                                    .contentType("image/" + ext)
                                                    // 버킷 안에 저장될 이름(경로)
                                                    .key(key)
                                                    .build();

        // putObject() : 실제로 S3 에 HTTPS PUT 요청을 보내 파일을 업로드한다.
        //   - RequestBody.fromBytes(bytes) : byte[] 를 요청 본문으로 쓰는 RequestBody 객체로 변환(배열을 복사해 보관)
        //   - 요청 전송 : DefaultS3Client 객체 → ApacheHttpClient 객체가 https://버킷이름.s3.ap-northeast-2.amazonaws.com/uploads/… 로 전송
        //   - 이때 DefaultCredentialsProvider 객체가 EC2 인스턴스 메타데이터에서 IAM 역할의 임시 키를 꺼내
        //     요청에 서명(AWS Signature V4)한다.
        //   - IAM 역할 aws-elasticbeanstalk-ec2-role 에 s3:PutObject 권한이 없으면
        //     여기서 S3Exception(403 AccessDenied)이 발생 → 브라우저에는 500 에러로 보인다.
        s3Client.putObject(request, RequestBody.fromBytes(bytes));

        // 업로드한 파일의 접근 URL 만들기
        //   s3Client.utilities() : S3Utilities 객체를 돌려준다.
        //   getUrl(builder -> builder.bucket(bucket).key(key)) : 버킷·키·리전으로 URL 객체를 계산한다.
        //     (네트워크 요청 없이 문자열 조합만 한다. 파일이 실제로 있는지 확인하지 않는다.)
        //   toExternalForm() : java.net.URL 객체를 문자열로 변환
        //   결과 예) https://springboot-developer-ajufresh.s3.ap-northeast-2.amazonaws.com/uploads/3f2a….png
        String url = s3Client.utilities()
                                .getUrl(builder -> builder.bucket(bucket).key(key))
                                .toExternalForm();

        // UploadController.uploadFile() 로 return → JSON {"imageUrl": url} 로 변환되어 브라우저에 응답
        return new UploadResponse(url);
    }
}
