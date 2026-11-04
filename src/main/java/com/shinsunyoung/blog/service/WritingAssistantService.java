// ============================================================
// [파일 설명] AI 글쓰기 도우미 서비스입니다.
// 사용자의 제목·본문·질문을 프롬프트로 만들어 AI 에게 묻고,
// AI 가 준 답변을 WritingSuggestionsResponse 객체(제안 3개 목록)로 바꿔서 돌려줍니다.
//
// [호출하는 곳]
//   BlogApiController.writingAssist() → getWritingAssist()
// ============================================================
package com.shinsunyoung.blog.service;

// WritingSuggestionRequest : 요청 상자 (제목, 본문, 질문)
// WritingSuggestionsResponse : 응답 상자 (제안 목록)
import com.shinsunyoung.blog.dto.WritingSuggestionRequest;
import com.shinsunyoung.blog.dto.WritingSuggestionsResponse;

// ChatClient : 채팅형 AI 에게 질문을 보내고 답을 받는 도구입니다. (Spring AI 제공)
import org.springframework.ai.chat.client.ChatClient;

// PromptTemplate : 틀 파일의 {} 자리에 값을 채워 넣는 도구입니다.
import org.springframework.ai.chat.prompt.PromptTemplate;

// BeanOutputConverter : 두 가지 일을 합니다.
//   ① getFormat() : "이런 JSON 모양으로 답해 주세요" 라는 지시 글을 만듭니다.
//   ② convert(글자) : AI 가 준 JSON 글자를 자바 객체(WritingSuggestionsResponse)로 바꿉니다.
import org.springframework.ai.converter.BeanOutputConverter;

// @Value, Resource, @Service : 이전 파일들과 같은 의미입니다.
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class WritingAssistantService {

    // AI 와 대화하는 객체입니다. 생성자에서 ChatClient.Builder 로 만들어 넣습니다.
    private final ChatClient chatClient;

    // 프롬프트 틀(prompts/writing-assistant.st)을 읽어 둔 객체입니다.
    private final PromptTemplate template;

    // AI 답변 모양 변환기입니다. WritingSuggestionsResponse 모양에 맞춰 만들어 둡니다.
    //   new BeanOutputConverter<>(WritingSuggestionsResponse.class) :
    //     WritingSuggestionsResponse 클래스의 구조(suggestions 목록)를 읽어서 JSON 지시문과 변환 규칙을 만듭니다.
    private final BeanOutputConverter<WritingSuggestionsResponse> outputConverter =
            new BeanOutputConverter<>(WritingSuggestionsResponse.class);

    // 생성자 : 스프링이 이 객체를 만들 때 재료를 넣어 줍니다.
    //   ChatClient.Builder : AI 대화 도구를 만드는 재료입니다. 스프링이 application.yml 의 OpenAI 설정으로 만들어 줍니다.
    //   @Value("classpath:prompts/writing-assistant.st") : 프롬프트 틀 파일 위치입니다.
    public WritingAssistantService(
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:prompts/writing-assistant.st") Resource promptResource
    ) {
        this.chatClient = chatClientBuilder.build();            // 빌더로 실제 ChatClient 객체를 완성합니다.
        this.template = new PromptTemplate(promptResource);     // 틀 파일을 읽어 PromptTemplate 을 만듭니다.
    }

    // getWritingAssist : 요청을 받아 AI 에게 묻고, 제안 목록 객체를 돌려줍니다.
    //   호출하는 곳 : BlogApiController.writingAssist()
    public WritingSuggestionsResponse getWritingAssist(WritingSuggestionRequest request) {

        // 1) 프롬프트 만들기
        //    outputConverter.getFormat() : "JSON 모양으로 답하라"는 지시 글을 만듭니다.
        //    request.toMap(지시글) : {title, content, question, format} 을 Map 으로 묶습니다.
        //    template.create(Map) : 틀의 {} 자리를 채웁니다.
        //    .getContents() : 완성된 문장을 꺼냅니다.
        String prompt = template.create(request.toMap(outputConverter.getFormat())).getContents();

        // 2) AI 에게 질문 보내기
        //    .prompt() : 새 요청을 시작합니다.
        //    .user(prompt) : 완성된 문장을 사용자 메시지로 붙입니다.
        //    .call() : AI 서버에 요청하고 답이 올 때까지 기다립니다.
        //    .content() : 답변 중 글자 부분만 꺼냅니다. (AI 가 준 JSON 글자)
        String response = chatClient.prompt().user(prompt).call().content();

        // 3) AI 답변(JSON 글자)을 WritingSuggestionsResponse 객체로 바꿔서 돌려줍니다.
        //    AI 가 지시한 JSON 모양과 다르게 답하면 여기서 변환 오류가 날 수 있습니다.
        return outputConverter.convert(response);
    }
}
