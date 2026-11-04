package com.shinsunyoung.blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/*
 * S3Config : AWS S3 와 통신하는 S3Client 객체를 딱 1개 만들어 스프링 컨테이너에 빈(객체)으로 등록하는 설정 클래스
 *
 * [호출 흐름]
 *   SpringBootDeveloperApplication.main() 실행
 *     → 스프링 컨테이너(AnnotationConfigServletWebServerApplicationContext 객체) 생성
 *     → ConfigurationClassPostProcessor 객체가 @Configuration 클래스를 찾아 이 클래스의 @Bean 메서드를 등록
 *     → DefaultListableBeanFactory 객체가 s3Client() 메서드를 1번 호출하고
 *       return 된 S3Client 객체를 "s3Client" 라는 이름으로 보관
 *     → AwsS3Service 생성자가 S3Client 를 요구하면 보관해 둔 이 객체를 넣어 준다.
 *
 * [이 코드가 동작하기 전에 S3 콘솔에서 해 둘 설정]
 *   ① S3 → [버킷 만들기]
 *      - AWS 리전          : 아시아 태평양(서울) ap-northeast-2  → application.yml cloud.aws.region 과 같아야 함
 *      - 버킷 이름         :  blog-본인아이디       → application.yml cloud.aws.s3.bucket 과 같아야 함
 *                            (버킷 이름은 전 세계 AWS 에서 하나뿐이어야 하므로 본인 아이디를 붙인다)
 *      - 객체 소유권       : ACL 활성화됨
 *      - 퍼블릭 액세스 차단 : "모든 퍼블릭 액세스 차단" 체크 해제 + 경고 확인 체크
 *                            (차단된 상태에서는 아래 버킷 정책을 저장할 수 없다)
 *   ② 버킷 → [권한] → [버킷 정책] → [편집] 에 아래 JSON 을 저장
 *      {
 *        "Version": "2012-10-17",                         ← 정책 문법 버전(고정값)
 *        "Statement": [
 *          {
 *            "Sid": "PublicReadUploads",                  ← 이 규칙의 이름(아무 이름 가능)
 *            "Effect": "Allow",                           ← 허용
 *            "Principal": "*",                            ← 누구에게나 (로그인하지 않은 브라우저 포함)
 *            "Action": "s3:GetObject",                    ← 파일 읽기(다운로드)만 허용. 업로드·삭제는 허용 안 함
 *            "Resource": "arn:aws:s3:::blog-본인아이디/uploads/*"
 *                                                         ← 이 버킷의 uploads/ 아래 파일에만 적용
 *          }
 *        ]
 *      }
 *      → AwsS3Service 가 만든 https://…amazonaws.com/uploads/xxx.png 주소를
 *        블로그 화면의 <img> 태그가 바로 불러올 수 있게 된다.
 *   ③ 업로드(s3:PutObject) 권한은 버킷 정책이 아니라 EC2 의 IAM 역할에 준다 (build() 아래 주석 참고).
 */
// @Configuration : 이 클래스가 빈 설정 클래스임을 표시.
//   ConfigurationClassPostProcessor 객체가 이 클래스를 CGLIB 로 상속한 프록시 클래스로 바꿔
//   s3Client() 를 여러 번 불러도 같은 객체 1개만 돌려주도록 만든다.
@Configuration
public class S3Config {

    // @Bean : 이 메서드의 return 값(S3Client 객체)을 스프링 빈으로 등록.
    //   빈 이름은 메서드 이름인 "s3Client".
    // @Value("${cloud.aws.region}") : application.yml 의 cloud.aws.region 값("ap-northeast-2")을
    //   매개변수 region 에 넣는다. 값을 찾아 넣는 일은 PropertySourcesPlaceholderConfigurer 객체가
    //   StandardEnvironment 객체에서 값을 조회해 수행한다.
    @Bean
    public S3Client s3Client(@Value("${cloud.aws.region}") String region) {

        // S3Client.builder() : DefaultS3ClientBuilder 객체를 돌려준다. 이 객체에 설정을 하나씩 넣은 뒤 build() 한다.
        return S3Client.builder()
                // Region.of("ap-northeast-2") : 문자열을 Region 객체로 변환.
                //   이 리전 값으로 요청 주소 https://s3.ap-northeast-2.amazonaws.com 이 정해진다.
                .region(Region.of(region))
                // build() : 설정을 바탕으로 DefaultS3Client 객체를 생성하여 return.
                //
                // [액세스 키를 넣지 않았는데 S3 에 접근할 수 있는 이유]
                //   credentialsProvider(...) 를 지정하지 않으면 SDK 가 DefaultCredentialsProvider 객체를 사용한다.
                //   이 객체는 실제 요청을 보낼 때 아래 순서로 자격 증명을 찾아 처음 발견한 것을 쓴다.
                //     ① 자바 시스템 속성 aws.accessKeyId / aws.secretAccessKey
                //     ② 운영체제 환경 변수 AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY
                //     ③ 웹 아이덴티티 토큰
                //     ④ 내 PC 의 ~/.aws/credentials 파일
                //     ⑤ 컨테이너(ECS) 자격 증명
                //     ⑥ EC2 인스턴스 메타데이터(IMDS, http://169.254.169.254)
                //   일래스틱 빈스톡의 EC2 에는 ①~⑤가 없으므로 ⑥에서
                //   EC2 인스턴스 프로파일(IAM 역할 aws-elasticbeanstalk-ec2-role)의 임시 키를 받아 쓴다.
                //   → 처음 배포 후 업로드하면 403 AccessDenied(…is not authorized to perform: s3:PutObject…)가
                //     나는 이유가 이 IAM 역할에 S3 권한이 없기 때문이다.
                //     IAM → [역할] → aws-elasticbeanstalk-ec2-role → [권한 추가] → [정책 연결] →
                //     AmazonS3FullAccess 를 연결하면 해결된다.
                //   → 키를 코드나 yml 에 적지 않으므로 jar 파일이나 Git 저장소에 키가 노출될 일이 없다.
                //   build() 시점에는 자격 증명을 조회하지 않으므로, 키가 없는 내 PC 의
                //   테스트(./gradlew build)에서도 이 빈 생성은 실패하지 않는다.
                .build();
    }
}
