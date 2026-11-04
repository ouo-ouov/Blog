package com.shinsunyoung.blog.controller;

import com.shinsunyoung.blog.domain.Article;
import com.shinsunyoung.blog.domain.User;                      // [11.5 추가] 테스트용 로그인 사용자 엔티티
import com.shinsunyoung.blog.dto.AddArticleRequest;
import com.shinsunyoung.blog.dto.UpdateArticleRequest;
import com.shinsunyoung.blog.repository.BlogRepository;
import com.shinsunyoung.blog.repository.UserRepository;        // [11.5 추가] users 테이블 저장소
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;                                     // [11.5 추가] 가짜(Mock) Principal 객체를 만드는 도구
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // [11.5 추가]
import org.springframework.security.core.context.SecurityContext;                       // [11.5 추가]
import org.springframework.security.core.context.SecurityContextHolder;                 // [11.5 추가]
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.security.Principal;                                 // [11.5 추가] addArticle() 이 받는 로그인 사용자 타입
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;




// @SpringBootTest : 테스트를 시작할 때 Blog 웹앱 전체 설정으로 스프링 컨테이너(ApplicationContext 객체)를 만듭니다.
//   스프링 컨테이너 안의 DefaultListableBeanFactory 객체가 빈을 만들어 보관합니다. 내장 톰캣은 띄우지 않습니다.
@SpringBootTest
// @AutoConfigureMockMvc : MockMvc 객체를 만들어 스프링 컨테이너에 빈(객체)으로 등록하도록 설정합니다.
//   MockMvc 객체 : 브라우저 요청을 흉내 내서 DispatcherServlet 객체에 직접 요청 URL을 넘겨 주는 테스트 도구입니다.
//                  톰캣 서버를 켜지 않고도 이 MockMvc 객체로 요청을 보낼수 있습니다.
@AutoConfigureMockMvc
class BlogApiControllerTest {

    // @Autowired MockMvc : 스프링 컨테이너가 보관하는 MockMvc 빈(객체)을 이 변수에 넣습니다. (의존성 주입)
    //                       아래 mockMvcSetUp() 에서 이 변수에 새 MockMvc객체 를 다시 넣습니다.
    @Autowired
    protected MockMvc mockMvc;

    // @Autowired ObjectMapper : Jackson.jar 라이브러리에서 제공하는 클래스로  JSON 변환 클래스의 객체를 의존 주입.
    //                           writeValueAsString(객체) : 자바 객체를 JSON 문자열로 변경해서 제공해 줍니다.
    @Autowired
    protected ObjectMapper objectMapper;

    // @Autowired WebApplicationContext : 스프링 웹 설정 객체입니다. mockMvcSetUp()메소드 안에서
    //                                    WebApplicationContext로 MockMvc 객체를 만듭니다.
    @Autowired
    private WebApplicationContext context;

    // @Autowired BlogRepository : article 테이블에 접근하는 저장소 객체입니다.
    //                             테스트 데이터를 넣고, 결과를 DB 에서 직접 확인할 때 씁니다.
    @Autowired
    BlogRepository blogRepository;

    // [11.5 추가] users 테이블 저장소입니다. 테스트용 사용자를 저장할 때 씁니다.
    @Autowired
    UserRepository userRepository;

    // [11.5 추가] 테스트에서 "로그인한 사용자" 역할을 하는 User 객체입니다.
    //   createDefaultArticle() 이 이 사용자의 이름(이메일)을 글쓴이(author)로 넣습니다.
    User user;

    // ═════════════════════════════════════════════════════════════════════════════════════════════════
    // [11.5 추가] 공통 준비 2 : 인증 정보(SecurityContext) 설정
    // ═════════════════════════════════════════════════════════════════════════════════════════════════
    // 왜 필요한가?
    //   11장에서 BlogService.update()/delete() 가 authorizeArticleAuthor() 로
    //   SecurityContextHolder 의 로그인 사용자 이름과 글의 author 가 같은지 검사하게 바뀌었습니다.
    //   테스트는 실제 로그인을 하지 않으므로, 여기서 직접 SecurityContext 에 인증 정보를 넣어 줍니다.
    @BeforeEach
    void setSecurityContext() {
        userRepository.deleteAll();   // users 테이블 비우기 (같은 이메일 중복 저장 오류 방지)

        // 테스트용 사용자 저장 → getUsername() 은 email("user@gmail.com") 을 돌려줍니다.
        user = userRepository.save(User.builder()
                .email("user@gmail.com")
                .password("test")
                .build());

        // SecurityContext 에 "이 사용자가 로그인했다" 는 인증 객체를 넣습니다.
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities()));
    }

    // ═════════════════════════════════════════════════════════════════════════════════════════════════
    // 공통 준비 : 모든 테스트 메소드가 실행되기 직전마다 실행됩니다.
    // ═════════════════════════════════════════════════════════════════════════════════════════════════
    // @BeforeEach : 아래 @Test 메소드 하나가 실행되기 직전마다 먼저 실행됩니다. 테스트가 8개이면 이 메소드도 8번 실행됩니다.
    @BeforeEach
    public void mockMvcSetUp() {

        // ───────────── 준비 1 : MockMvc 만들기 ─────────────
        // MockMvcBuilders.webAppContextSetup(context) : WebApplicationContext 객체로 MockMvc 를 만들기 시작합니다.
        // .build() : MockMvc 를 완성합니다. 완성된 객체를 위의 mockMvc 변수에 넣습니다.
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        // ───────────── 준비 2 : 테이블 비우기 ─────────────
        // blogRepository.deleteAll() : article 테이블의 행을 모두 지우도록 예약합니다.
        //   SimpleJpaRepository.deleteAll 이 findAll(select)로 행을 읽고, 객체마다 remove 를 예약합니다.
        //   이 뒤에 저장(save)이나 조회가 나오면 자동 flush 로 DELETE 가 DB 에 나갑니다.
        //   이 파일의 import.sql 은 부록B 프로젝트에서 글 3개(작성자 user1~3)를 넣습니다. 이 메소드가 그 글도 지웁니다.
        blogRepository.deleteAll();

    }

    // ═════════════════════════════════════════════════════════════════════════════════════════════════
    // 테스트 1 : addArticle — 글 등록 (POST /api/articles)
    // ═════════════════════════════════════════════════════════════════════════════════════════════════

    // @DisplayName : 테스트 결과 화면에 보이는 이름입니다.
    @DisplayName("addArticle 메소드 : 블로그 글 추가에 성공하는지 테스트")
    @Test
    public void addArticle() throws Exception{

        // ───────────── 1단계  given1 : 블로그 글 추가요청에 필요한 요청 주소와 값 준비 ─────────────
        //url : 블로그 글 추가(등록,INSERT) 요청 주소 만들기
        //title, content : 블로그 글 추가 시 사용할 글제목, 글내용 만들기
        final String url = "/api/articles"; // (BlogApiController.addArticle() 의 @PostMapping 주소와 같습니다)
        final String title = "title";
        final String content = "content";

        // ───────────── 2단계  given2 : 블로그 글 추가요청에 필요한 요청 데이터 Dto역할을 하는 AddArticleRequest객체로 만들기 ─────────────
        //   AddArticleRequest 의 명시적 생성자(String title, String content)를 호출합니다. ( dto/AddArticleRequest.java)
        final AddArticleRequest  userRequest = new AddArticleRequest(title, content);

        // ───────────── 3단계 given3 : 블로그 글 추가요청에 필요한 DTO역할을 하는 AddArticleRequest객체를 JSON 문자열로 바꾸기 ─────────────
        //   Jackson.jar에 포함된 ObjectMapper클래스의 writeValueAsString메소드로 Dto역할을 하는 userRequest객체 의 getter 를 읽어서 JSON 형태의  문자열로 만듭니다.
        //  => '{ "title":"title",   "content":"content" }' <= 새글 등록시 사용할 요청데이터를  JSON 문자열 로 바꿔서 반환
        final String requestBody =  objectMapper.writeValueAsString(userRequest);

        // ───────────── 4단계 When : 블로그 글 추가 API (BlogApiController.java에 만들어 놓은 addArticle()메소드)에 새글 추가 요청 보내기 ─────────────
        // 호출하는 줄 : mockMvc.perform(post(url).contentType(...).content(requestBody))
        //   post(url)             : POST 방식 요청을 만듭니다. 요청 주소는 /api/articles 입니다.
        //   .contentType(JSON)    : 요청 메세지 본문영역에 담아서 요청 보낼 데이터가  JSON 문자열 로 (Content-Type)속성에 설정 합니다.
        //   .content(requestBody) : 요청 메세지 본문영역에 새글 추가시 만들었던 JSON형태의 문자열을  HttpRequest 메세지에 담습니다.
        //
        //   이 줄 안에서 일어나는 일:
        //     (1) MockMvc 가 요청을 DispatcherServlet 객체에 넘깁니다.
        //     (2) DispatcherServlet 이 주소 /api/articles 와 POST 를 보고 BlogApiController.addArticle 을 고릅니다.
        //     (3) 본문의 JSON 을 AddArticleRequest 객체로 바꿔 addArticle 의 매개변수 @RequestBody 에 넣습니다.
        //     (4) addArticle 은 로그인 사용자 Principal 도 함께 받습니다.
        //   돌려주는 값 result : 응답 결과 상자(ResultActions)입니다.
        //
        // [11.5 변경] 기존 코드는 Principal 없이 요청해서 addArticle() 안의 principal.getName() 에서
        //   NullPointerException 이 났습니다. Mockito 로 가짜 Principal 을 만들고 .principal(principal) 로 넣습니다.
        //   principal.getName() 을 부르면 "username" 을 돌려주도록 설정합니다.
        Principal principal = Mockito.mock(Principal.class);
        Mockito.when(principal.getName()).thenReturn("username");

        ResultActions result = mockMvc.perform(post(url)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .principal(principal)          // [11.5 추가]
                                                .content(requestBody));

        // ───────────── 5단계  Then1 : 블로그 글 등록(새글 추가)에 성공했다면 응답 상태 코드가 201(Created)인지 검사 확인 ─────────────
        // status().isCreated() : 응답 상태 코드가 201(Created)인지 검사합니다.
        result.andExpect(status().isCreated());

        // ───────────── 6단계  Then2 : article 테이블에 실제로 저장(INSERT)된 데이터와 사용자가 요청한 새글 데이터가 같은지  직접 확인 하기 위해 전체 조회 ───────────
        // 호출하는 줄 : blogRepository.findAll() → SQL: select ... from article
        List<Article> articles = blogRepository.findAll();

        // article 테이블에 실제로 저장(INSERT)된 데이터와 사용자가 요청한 새글 데이터가 같은지  직접 확인
        //방법  :  assertThat(실제값).isEqualTo(기대값) : 같으면 통과, 다르면 실패합니다. (AssertJ 라이브러리)
        assertThat(articles.size()).isEqualTo(1);           // article테이블에 추가(INSERT)한 블로그 새글 레코드 갯수가 1개이면 테스트 통과
        assertThat(articles.get(0).getTitle()).isEqualTo(title);    // article테이블에 추가(INSERT)한 블로그 새글의 제목과 요청한 새글 제목과 같으면? 테스트 통과
        assertThat(articles.get(0).getContent()).isEqualTo(content);// article테이블에 추가(INSERT)한 블로그 새글의 내용과 요청한 새글 내용과 같으면? 테스트 통과
        assertThat(articles.get(0).getAuthor()).isEqualTo("username"); // [11.5 추가] 글쓴이가 Principal 이름으로 저장됐는지 확인

    }

    // ═════════════════════════════════════════════════════════════════════════════════════════════════
    // 테스트 2 : findAllArticles() — 글 목록 조회 (GET /api/articles)
    // ═════════════════════════════════════════════════════════════════════════════════════════════════

    @DisplayName("findAllArticles() :  블로그 글 목록 조회에 성공하는지 테스트 ")
    @Test
    public void findAllArticles() throws Exception{

        // ───────────── 1단계  given1 : 글 목록 조회 테스트를 위해 저장(INSERT) 할 블로그 글  데이터 준비 ─────────────
        final String url = "/api/articles";  //글목록 조회 요청 URL 준비
        final String title = "title";        //글목록 조회 테스트를 위해 저장할 새글 제목 데이터 준비
        final String content = "content";    //글목록 조회 테스트를 위해 저장할 새글 내용 데이터 준비

        // ───────────── 2단계  given2 :  글 목록 조회 테스트를 위해 블로그 글 저장(INSERT)  ─────────────
        // 호출하는 줄 : blogRepository.save(Article.builder().title(title).content(content).build())
        //   Article.builder() : Article 의 빌더로 객체를 만듭니다. id 는 DB 가 채웁니다.
        //   save : 객체를 article 테이블에 저장합니다. (insert)
        // [11.5 변경] author(nullable=false) 없이 저장하면 DataIntegrityViolationException 이 납니다.
        //   글쓴이까지 채워 저장하는 createDefaultArticle() 로 바꿨습니다. (기존: blogRepository.save(Article.builder().title(title).content(content).build()))
        Article savedArticle = createDefaultArticle();

        // ───────────── 3단계  when :  블로그 글 목록 조회 API 요청 ─────────────
        // 호출하는 줄 : mockMvc.perform(get(url).accept(JSON))
        //   accept(JSON) : 응답을 JSON 으로 달라는 머리글(Accept)을 붙입니다.
        //   이 줄 안에서 DispatcherServlet 이 BlogApiController.findAllArticles()메소드 를 실행합니다.
        //   findAllArticles()안에서는 BlogService 를 거쳐 글 목록을 조회해서 가져와 ArticleResponse 목록으로 바꾸고,
        //   응답 상태 코드 200 과 함께 돌려줍니다.
        final ResultActions resultActions = mockMvc.perform(get(url).accept(MediaType.APPLICATION_JSON));

        // ───────────── 4단계 then :  블로그 조회한 글 목록을 응답받아 확인 ─────────────
        //   status().isOk()                         : 블로그 글 목록을 조회한 응답 상태 코드가 200 인지 확인합니다.
        //   jsonPath("$[0].content").value(content) : 블로그 글 목록을 조회한 응답 JSON 배열의  첫 번째(0번) 항목의 content 값 확인
        //   jsonPath("$[0].title").value(title)     : 블로그 글 목록을 조회한 응답 JSON 배열의  첫 번째(0번) 항목의 title 값 확인
        //     $     :  응답받은 조회한  JSONArray 전체
        //     [0]   :  JSONArray배열의 첫 번째 글 레코드
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value(savedArticle.getContent()))  // [11.5 변경] content → savedArticle.getContent()
                .andExpect(jsonPath("$[0].title").value(savedArticle.getTitle()));     // [11.5 변경] title → savedArticle.getTitle()


    }

    // ═════════════════════════════════════════════════════════════════════════════════════════════════
    // 테스트 3 : findArticle — 글 한 개 조회 (GET /api/articles/{id})
    // ═════════════════════════════════════════════════════════════════════════════════════════════════
    @DisplayName("findArticle() : 블로그 글 한개 조회에 성공하는지 테스트")
    @Test
    public void findArticle() throws Exception{

        // ───────────── 1단계  given1 : GET /api/articles/{id} 글 한개 조회 테스트를 위해 저장(INSERT) 할 블로그 글  데이터 준비 ─────────────
        final String url = "/api/articles/{id}"; //글 한개 조회 요청할 URL 준비
        final String title = "title";            //글 한개 조회 요청 테스트 하기 위해 글 저장(INSERT)할 글제목 데이터 준비
        final String content = "content";        //글 한개 조회 요청 테스트 하기 위해 글 저장(INSERT)할 글내용 데이터 준비

        // ───────────── 2단계 given2 : 글 한개 조회 테스트하기 위해  글 저장 ─────────────
        // [11.5 변경] author 가 필수라서 createDefaultArticle() 로 저장합니다. (기존: Article.builder().title(title).content(content).build())
        Article savedArticle = createDefaultArticle(); //INSERT

        // ───────────── 3단계 when : 글한 개 조회 요청 ─────────────
        // 호출하는 줄 : get(url, savedArticle.getId())
        //   {id} 자리에 savedArticle.getId() 값을 넣습니다. 예) id 가 1 이면 /api/articles/1
        //   DispatcherServlet 이 BlogApiController.findArticle() 을 고릅니다.
        //   findArticle() 은 @PathVariable 로 주소의 id를 받아 BlogService 에 넘깁니다.
        final  ResultActions resultActions = mockMvc.perform( get(url, savedArticle.getId())  );

        // ───────────── 4단계 then : 조회한 응답메세지 확인(검증) ─────────────
        //   status().isOk()        : 응답 상태 코드 200 확인
        //   jsonPath("$.content")  : 응답 JSONObject { }객체의 content 키값 확인 (배열이 아니므로 [0] 이 없습니다)
        //   jsonPath("$.title")    : title 값 확인
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(savedArticle.getContent()))  // [11.5 변경] content → savedArticle.getContent()
                .andExpect(jsonPath("$.title").value(savedArticle.getTitle()));     // [11.5 변경] title → savedArticle.getTitle()
    }

    // ═════════════════════════════════════════════════════════════════════════════════════════════════
    // 테스트 4 : deleteArticle() — 글 삭제 (DELETE /api/articles/{id})
    //   연결된 컨트롤러 메소드 : BlogApiController.deleteArticle(@PathVariable long id)
    // ═════════════════════════════════════════════════════════════════════════════════════════════════
    @DisplayName("deleteArticle(): 블로그 글 삭제에 성공하는지 테스트")
    @Test
    public void deleteArticle() throws Exception {

        // ───────────── 1단계  given1 : 삭제 요청 주소와 더미 글 값 준비 ─────────────
        final String url = "/api/articles/{id}"; //삭제 요청할 URL 준비
        final String title = "title";            //더미 글의 제목입니다. 삭제만 확인하면 되므로 값은 아무거나 괞찬습니다.
        final String content = "content";        //더미 글의 내용입니다.

        // ───────────── 2단계  given2 : 삭제할 더미 글을 DB 에 저장(INSERT) ─────────────
        // 저장(INSERT)된 객체 savedArticle 에 DB 가 채운 id 가 들어 있습니다. 이 id 로 삭제 요청을 보냅니다.
        // [11.5 변경] author 가 필수이고, 삭제는 "글쓴이 본인"만 가능하므로
        //   SecurityContext 의 사용자(user)를 글쓴이로 넣어 주는 createDefaultArticle() 로 저장합니다.
        Article savedArticle = createDefaultArticle(); //INSERT
        // ───────────── 3단계  when : 글 한 개 삭제 요청 ─────────────
        // 호출하는 줄 : mockMvc.perform(delete(url, savedArticle.getId())).andExpect(status().isOk())
        //   delete(url, id) : DELETE 방식 요청을 만들고, {id} 자리에 savedArticle.getId() 를 넣습니다.
        //   .andExpect(status().isOk()) : 응답 상태 코드가 200 인지 바로 확인합니다.
        //
        //   이 줄 안에서 일어나는 일 (순서대로):
        //     (1) DispatcherServlet 객체가 BlogApiController.deleteArticle(long id) 를 실행합니다.
        //     (2) BlogApiController.deleteArticle(long id) 는 blogService.delete(id) 를 호출합니다.
        //     [11.5 변경] 11장 BlogService.delete() 기준으로 설명을 고쳤습니다.
        //     (3) BlogService.delete(long id) 는 blogRepository.findById(id) 로 글을 읽습니다.
        //         → SQL: select ... from article where id=?
        //         없으면 IllegalArgumentException("not found : id") 를 던집니다.
        //     (4) authorizeArticleAuthor(article) 가 SecurityContextHolder 의 사용자 이름("user@gmail.com")과
        //         글의 author 가 같은지 확인합니다. 다르면 IllegalArgumentException("not authorized").
        //     (5) 같으면 blogRepository.delete(article) 로 삭제합니다.
        //         → SQL: delete from article where id=?
        //     (6) deleteArticle 이 ResponseEntity.ok().build() 로 본문 없이 200 을 돌려줍니다.
        mockMvc.perform(delete(url, savedArticle.getId()))
                .andExpect(status().isOk());

        // ───────────── 4단계  then : 삭제된 글이 DB 에 없는지 확인 ─────────────
        // 호출하는 줄 : blogRepository.findAll()
        //   새 읽기 전용 트랜잭션에서 조회합니다. → SQL: select ... from article
        //   DELETE 가 실제로 DB 에 반영되었다면, article 테이블에는 행이 하나도 남아 있지 않아야 합니다.
        //   돌려주는 값 articles : 지금 article 테이블에 있는 행 전체를 담은 목록입니다.
        List<Article> articles = blogRepository.findAll();

        // isEmpty() : 목록에 Article객체가 하나도 없으면 통과합니다. 한 개라도 남아 있으면 테스트가 실패합니다.
        assertThat(articles).isEmpty();

        //전체 SQL 실행 순서
        //  더미 insert → select(where id=?) → delete(where id=?) → select     // [11.5 변경]
    }

    // ═════════════════════════════════════════════════════════════════════════════════════════════════
    // 테스트 5 : updateArticle — 블로그 글 수정 (PUT /api/articles/{id})
    //   연결된 컨트롤러 메소드 : BlogApiController.updateArticle(@PathVariable long id, @RequestBody UpdateArticleRequest request)
    // ═════════════════════════════════════════════════════════════════════════════════════════════════
    @DisplayName("updateArticle(): 블로그 글 수정에 성공하는지 테스트.")
    @Test
    public void updateArticle() throws Exception{

        // ───────────── 1단계  given1 : 수정 요청 주소와 처음 저장할 값 준비 ─────────────
        final  String url = "/api/articles/{id}"; //수정 요청 주소 URL 준비
        final  String title = "title";            //수정 요청 테스트를 위해 DB에 INSERT 할 더미 글 제목 값 준비
        final  String content = "content";        //수정 요청 테스트를 위해 DB에 INSERT 할 더미 글 내용 값 준비

        // ───────────── 2단계  given2 : 수정요청 테스트를 위해  더미 글을 DB 에 저장(INSERT) ─────────────
        // [11.5 변경] 수정도 "글쓴이 본인"만 가능하므로 createDefaultArticle() 로 저장합니다.
        Article savedArticle = createDefaultArticle(); //INSERT

        // ───────────── 3단계  given3 : 수정할 새 제목과 새 내용 준비 ─────────────
        final String newTitle = "new Title";        //수정할 새 제목 준비
        final String newContent = "new Content";    //수정할 새 내용 준비

        //Dto역할을 하는 UpdateArticleRequest 클래스(dto/UpdateArticleRequest.java) 위의
        // @AllArgsConstructor 가 만든 생성자 UpdateArticleRequest(String title, String content) 를 호출합니다.
        UpdateArticleRequest  request = new UpdateArticleRequest(newTitle, newContent); //<- DB에 수정할 새 제목과 새내용이 담긴 DTO


        // ───────────── 4단계  When : 글 수정 요청 보내기 ─────────────
        // 호출하는 줄 : mockMvc.perform(put(url, id).contentType(JSON).content(JSON 문자열))
        //   put(url, id) : PUT 방식 요청을 만들고, {id} 자리에 savedArticle.getId() 를 넣습니다.
        //   .contentType(MediaType.APPLICATION_JSON_VALUE) : 본문이 JSON 이라는 머리글을 붙입니다.
        //   .content(objectMapper.writeValueAsString(request)) : 수정 객체를 JSON 문자열로 바꿔서 본문에 담습니다.
        //     => '{"title":"new title","content":"new content"}'
        //
        //   이 줄 안에서 일어나는 일 (순서대로):
        //     (1) DispatcherServlet 객체가 BlogApiController.updateArticle(long id, UpdateArticleRequest request) 를 실행합니다.
        //     (2) @PathVariable 이 수정 요청주소의 번호를 id 에 넣고, @RequestBody 가 본문 JSON 을 UpdateArticleRequest 객체로 바꿔서 request 에 넣습니다.
        //     (3) updateArticle() 이 blogService.update(id, request) 를 호출합니다.
        //     (4) BlogService.update(long id, UpdateArticleRequest request) 는 @Transactional 메소드라서 트랜잭션을 시작합니다.
        //     (5) blogRepository.findById(id) 로 글을 조회합니다. → SQL: select ... where id=?
        //         조회한 Article 객체는 이 트랜잭션의 1차 캐시에 들어가 영속 상태입니다. JPA 는 처음 읽은 값(title, content)을 기억해 둡니다.
        //         글이 없으면 IllegalArgumentException("not found : id") 를 던집니다.                       // [11.5 변경]
        //         이어서 authorizeArticleAuthor(article) 로 글쓴이 본인인지 확인합니다.                        // [11.5 추가]
        //     (6) article.update(request.getTitle(), request.getContent()) 를 호출합니다.
        //         Article 의 update 메소드가 title, content 필드 값을 바꿉니다. 이 줄에서는 SQL 이 나가지 않습니다.
        //         [메모리 상태]  1차 캐시 : { Article { id=<번호>, title="new title", content="new content" } : 영속 }
        //     (7) update 메소드가 끝나면 트랜잭션을 커밋합니다. 커밋 직전 flush 에서
        //         JPA 가 처음 읽은 값과 지금 값을 비교(변경 감지)해서 다른 값이 있으므로 UPDATE SQL 을 보냅니다.
        //         → SQL: update article set content=?, title=? where id=?
        //     (8) updateArticle 이 ResponseEntity.ok().body(updatedArticle) 로 200 과 수정된 Article 객체를 돌려줍니다.
        ResultActions result = mockMvc.perform(put(url, savedArticle.getId())
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content( objectMapper.writeValueAsString(request))
                                               );

        // ───────────── 5단계  Then1 : 수정한 응답 상태 코드가 200 인지 확인 ─────────────
        // status().isOk() : 수정 응답 상태 코드가 200(OK) 인지 검사합니다.
        result.andExpect(status().isOk());

        // ───────────── 6단계  Then2 : DB 에서 다시 조회해서 가져와  수정 글의 데이터가 바뀌었는지 확인 ─────────────
        // 호출하는 줄 : blogRepository.findById(savedArticle.getId()).get()
        //   findById(id) : 새 읽기 전용 트랜잭션에서 em.find(Article.class, id) 를 호출합니다.
        //                  새 트랜잭션이라 1차 캐시가 비어 있으므로, 캐시가 아니라 DB 에서 다시 읽습니다. → SQL: select ... where id=?
        //   돌려주는 값은 Optional 상자입니다. .get() 으로 상자 안의 Article 객체를 꺼냅니다.
        //   이 객체는 2단계의 savedArticle 과 다른 새 객체이고, 값은 DB 에 실제로 저장된 값입니다.
        //   (savedArticle 은 준영속 객체라서 지금도 title="title" 그대로입니다. 그래서 savedArticle 이 아니라 새로 조회한 Article 객체로 검사합니다)
        Article article = blogRepository.findById(savedArticle.getId()).get();

        // DB 에서 수정 후 조회된 글제목이 수정요청시 사용한 새 제목("new title")과 같은지 확인합니다.
        assertThat(article.getTitle()).isEqualTo(newTitle);

        // DB 에서 수정 후 조회된 글내용이 수정요청시 사용한 새 내용("new Content")과 같은지 확인합니다.
        assertThat(article.getContent()).isEqualTo(newContent);

    }

    // ═════════════════════════════════════════════════════════════════════════════════════════════════
    // [수정·삭제 완성 추가] 테스트 6 : 남의 글은 수정할 수 없다 (403)
    //   BlogService.authorizeArticleAuthor() 가 글쓴이가 아니면 ResponseStatusException(403) 을 던집니다.
    // ═════════════════════════════════════════════════════════════════════════════════════════════════
    @DisplayName("updateArticle(): 다른 사람이 쓴 글은 수정할 수 없다 (403).")
    @Test
    public void updateArticleByOtherUser() throws Exception {
        // given : 글쓴이가 "other@gmail.com" 인 글을 저장합니다.
        //   지금 로그인한 사람(setSecurityContext 의 user)은 "user@gmail.com" 이므로 글쓴이가 아닙니다.
        Article othersArticle = blogRepository.save(Article.builder()
                .title("title")
                .content("content")
                .author("other@gmail.com")
                .build());
        String body = objectMapper.writeValueAsString(new UpdateArticleRequest("new title", "new content"));

        // when & then : 수정 요청 → 403(FORBIDDEN) 이어야 합니다.
        mockMvc.perform(put("/api/articles/{id}", othersArticle.getId())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(body))
                .andExpect(status().isForbidden());

        // then : DB 의 글이 바뀌지 않았는지 확인합니다. (@Transactional 메소드에서 예외 → 롤백)
        Article article = blogRepository.findById(othersArticle.getId()).get();
        assertThat(article.getTitle()).isEqualTo("title");
        assertThat(article.getContent()).isEqualTo("content");
    }

    // ═════════════════════════════════════════════════════════════════════════════════════════════════
    // [수정·삭제 완성 추가] 테스트 7 : 남의 글은 삭제할 수 없다 (403)
    // ═════════════════════════════════════════════════════════════════════════════════════════════════
    @DisplayName("deleteArticle(): 다른 사람이 쓴 글은 삭제할 수 없다 (403).")
    @Test
    public void deleteArticleByOtherUser() throws Exception {
        // given : 다른 사람이 쓴 글 저장
        Article othersArticle = blogRepository.save(Article.builder()
                .title("title")
                .content("content")
                .author("other@gmail.com")
                .build());

        // when & then : 삭제 요청 → 403
        mockMvc.perform(delete("/api/articles/{id}", othersArticle.getId()))
                .andExpect(status().isForbidden());

        // then : 글이 그대로 남아 있어야 합니다.
        assertThat(blogRepository.findById(othersArticle.getId())).isPresent();
    }

    // ═════════════════════════════════════════════════════════════════════════════════════════════════
    // [11.5 추가] 공통 도우미 : 테스트용 글 한 개를 글쓴이(author)까지 채워 저장합니다.
    //   author = user.getUsername() = "user@gmail.com" (setSecurityContext() 에서 로그인시킨 사용자)
    //   그래서 수정/삭제 테스트에서 authorizeArticleAuthor() 검사를 통과합니다.
    // ═════════════════════════════════════════════════════════════════════════════════════════════════
    private Article createDefaultArticle() {
        return blogRepository.save(Article.builder()
                .title("title")
                .author(user.getUsername())
                .content("content")
                .build());
    }




}









