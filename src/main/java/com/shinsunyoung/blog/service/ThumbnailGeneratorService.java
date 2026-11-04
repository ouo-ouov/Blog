// ============================================================
// [파일 설명] AI 로 블로그 썸네일 이미지를 만드는 서비스입니다.
// 전체 순서
//   ① 제목·본문으로 프롬프트(AI 에게 줄 글)를 만든다
//   ② 이미지 AI 에게 그림을 요청한다 (결과는 이미지 인터넷 주소로 온다)
//   ③ 그 주소에서 이미지 파일 내용을 내려받는다
//   ④ 내 서버의 uploads 폴더에 저장한다
//   ⑤ 저장된 주소를 돌려준다
//
// [호출하는 곳]
//   BlogApiController.thumbnailGenerator() → generateThumbnail()
// ============================================================
package com.shinsunyoung.blog.service;

// GeneratorThumbnailRequest : 요청 상자(제목, 본문) (DTO)
// GeneratorThumbnailResponse : 응답 상자(이미지 주소) (DTO)
// UploadResponse : 파일 저장 결과 상자 (DTO)
import com.shinsunyoung.blog.dto.GeneratorThumbnailRequest;
import com.shinsunyoung.blog.dto.GeneratorThumbnailResponse;
import com.shinsunyoung.blog.dto.UploadResponse;

// PromptTemplate : 파일의 문장 틀({title} 같은 자리)에 값을 채워 넣는 도구입니다. (Spring AI 제공)
import org.springframework.ai.chat.prompt.PromptTemplate;

// Image : AI 가 만든 이미지 1장의 정보입니다. getUrl() 로 이미지 인터넷 주소를 얻습니다.
import org.springframework.ai.image.Image;

// ImageModel : 이미지 생성 AI 와 통신하는 객체의 규격입니다. (실제 구현은 OpenAI 쪽이 맡습니다)
import org.springframework.ai.image.ImageModel;

// ImagePrompt : 이미지 AI 에게 보낼 요청 1건을 담는 객체입니다.
import org.springframework.ai.image.ImagePrompt;

// ImageResponse : 이미지 AI 가 돌려준 응답 전체입니다.
import org.springframework.ai.image.ImageResponse;

// @Value : 설정 값이나 파일 경로를 변수에 넣어 줄 때 쓰는 표시입니다.
import org.springframework.beans.factory.annotation.Value;

// Resource : 프로젝트 안의 파일 1개를 가리키는 객체입니다.
import org.springframework.core.io.Resource;

// @Service : 이 클래스를 스프링 객체로 등록합니다.
import org.springframework.stereotype.Service;

// RestClient : 인터넷 주소에 요청을 보내 응답을 받는 HTTP 도구입니다. (스프링 6.1 이상)
import org.springframework.web.client.RestClient;

// URI : 인터넷 주소를 나타내는 객체입니다.
import java.net.URI;

@Service
public class ThumbnailGeneratorService {

    // 이미지 AI 와 통신하는 객체입니다. 스프링이 OpenAI 설정(application.yml)을 보고 만들어 넣어 줍니다.
    private final ImageModel imageModel;

    // 프롬프트 틀(prompts/thumbnail-generator.st) 을 읽어 둔 객체입니다.
    private final PromptTemplate template;

    // 저장 기능을 맡는 객체입니다. 현재는 LocalFileStorageService 가 들어옵니다.
    private final FileStorageService fileStorageService;

    // 인터넷에서 이미지 파일을 내려받을 때 쓰는 HTTP 도구입니다.
    //   RestClient.create() : 기본 설정의 HTTP 도구 객체를 만듭니다. 이 클래스 안에서 한 번만 만듭니다.
    private final RestClient restClient = RestClient.create();

    // 생성자 : 스프링이 이 클래스를 만들 때 필요한 재료를 넣어 줍니다.
    //   ImageModel          : 스프링이 자동으로 만든 이미지 AI 객체
    //   FileStorageService  : 저장 기능 객체 (LocalFileStorageService)
    //   @Value("classpath:prompts/thumbnail-generator.st") : 프로젝트 안 resources/prompts 폴더의 틀 파일을 읽어 옵니다.
    public ThumbnailGeneratorService(
            ImageModel imageModel,
            FileStorageService fileStorageService,
            @Value("classpath:prompts/thumbnail-generator.st") Resource promptResource
    ) {
        this.imageModel = imageModel;                           // 받은 이미지 AI 객체를 필드에 저장합니다.
        this.fileStorageService = fileStorageService;           // 받은 저장 객체를 필드에 저장합니다.
        this.template = new PromptTemplate(promptResource);     // 틀 파일을 읽어서 PromptTemplate 객체를 만듭니다.
    }

    // generateThumbnail : 제목과 본문을 받아 썸네일을 만들고, 저장된 이미지 주소를 돌려줍니다.
    //   호출하는 곳 : BlogApiController.thumbnailGenerator()
    public GeneratorThumbnailResponse generateThumbnail(GeneratorThumbnailRequest request) {

        // 1) 프롬프트 만들기
        //    request.toMap() 으로 {title, content} 값을 Map 으로 만들고,
        //    template.create(...) 가 틀의 {title}, {content} 자리를 채웁니다.
        //    .getContents() 로 완성된 문장을 꺼내 prompt 에 담습니다.
        String prompt = template.create(request.toMap()).getContents();

        // 2) 이미지 AI 에게 그림 요청
        //    new ImagePrompt(prompt) : 완성된 문장으로 요청 객체를 만듭니다.
        //    imageModel.call(...) : AI 서버에 요청하고 응답을 기다립니다. (몇 초 걸릴 수 있습니다)
        ImageResponse response = imageModel.call(new ImagePrompt(prompt));

        // 3) 응답에서 첫 번째 이미지 정보를 꺼냅니다.
        //    getResult().getOutput() : 응답 안의 결과 중 이미지 정보 1개를 꺼냅니다.
        Image image = response.getResult().getOutput();

        // 4) 이미지 주소에서 실제 이미지 파일 내용(바이트 배열)을 내려받습니다.
        //    image.getUrl() : AI 가 알려 준 이미지의 인터넷 주소
        //    .retrieve().body(byte[].class) : 응답 내용을 byte 배열 형태로 받습니다.
        byte[] bytes = restClient.get()
                .uri(URI.create(image.getUrl()))
                .retrieve()
                .body(byte[].class);

        // 5) 내려받은 이미지를 내 서버에 저장합니다. 이름은 "thumbnail.png" 를 넘기지만,
        //    LocalFileStorageService 가 확장자(.png)만 쓰고 실제 이름은 UUID 로 새로 만듭니다.
        UploadResponse saved = fileStorageService.store(bytes, "thumbnail.png");

        // 6) 저장된 주소(예: /uploads/uuid.png)를 응답 상자에 담아 돌려줍니다.
        return new GeneratorThumbnailResponse(saved.imageUrl());
    }
}
