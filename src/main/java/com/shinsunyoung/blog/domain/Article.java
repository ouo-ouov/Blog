// ============================================================
// [파일 설명] 블로그 "글 1개"를 나타내는 엔티티(Entity)입니다.
// 엔티티 = 데이터베이스 테이블의 한 줄(row)을 자바 객체로 옮겨 놓은 것입니다.
// 이 클래스는 article 테이블과 1:1로 연결됩니다.
//
// [호출 흐름]
//   BlogService.save()  : new Article(...) 대신 AddArticleRequest.toEntity() 가 Article.builder() 로 만들고,
//                         BlogRepository.save() 가 article 테이블에 INSERT 합니다.
//   BlogService.update(): 조회한 Article 객체의 update() 메소드로 값을 바꾸면,
//                         @Transactional 덕분에 트랜잭션이 끝날 때 UPDATE 가 자동 실행됩니다.
// ============================================================
package com.shinsunyoung.blog.domain;

// jakarta.persistence.* : JPA(자바 데이터 저장 규격)의 어노테이션들을 한꺼번에 가져옵니다.
//   @Entity, @Id, @GeneratedValue, @Column, @EntityListeners 등이 여기에 들어 있습니다.
import jakarta.persistence.*;

// Lombok 어노테이션들: 반복되는 코드를 자동으로 만들어 줍니다.
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 생성 시간, 수정 시간을 표시하는 어노테이션입니다.
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

// AuditingEntityListener : @CreatedDate, @LastModifiedDate 값을 저장 시점에 자동으로 채워 주는 감시 도구입니다.
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// LocalDateTime : 날짜와 시간(예: 2026-10-30T14:05:00)을 담는 자바 자료형입니다.
import java.time.LocalDateTime;

// [@EntityListeners(AuditingEntityListener.class)]
//   이 엔티티가 저장·수정될 때 AuditingEntityListener 가 실행되도록 연결합니다.
//   그래서 createdAt, updatedAt 값이 자동으로 채워집니다. (SpringBootDeveloperApplication 의 @EnableJpaAuditing 과 함께 동작)
@EntityListeners(AuditingEntityListener.class)

// [@Entity] 이 클래스가 DB 테이블과 연결되는 JPA 엔티티라는 표시입니다.
//   테이블 이름을 따로 적지 않았으므로 클래스 이름 Article 과 같은 article 테이블을 사용합니다.
@Entity

// [@Getter] 모든 필드의 값을 읽는 getXxx() 메소드를 자동으로 만들어 줍니다. (예: getTitle())
@Getter

// [@NoArgsConstructor(access = PROTECTED)]
//   값을 받지 않는 기본 생성자를 만듭니다.
//   JPA 는 DB에서 꺼낸 값을 객체에 담을 때 이 생성자를 사용합니다.
//   access = PROTECTED : 같은 패키지나 자식 클래스에서만 쓸 수 있고, 밖에서 new Article() 로 빈 객체를 만드는 것은 막습니다.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Article {

    // [@Id] 이 필드가 기본키(Primary Key)라는 표시입니다. 같은 글을 찾을 때 쓰는 고유 번호입니다.
    @Id

    // [@GeneratedValue(strategy = IDENTITY)] id 값을 개발자가 넣지 않고 DB가 1, 2, 3... 으로 자동 증가시키라는 뜻입니다.
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    // [@Column(name = "id", updatable = false)]
    //   DB 컬럼 이름을 id 로 지정하고, 한 번 저장된 뒤에는 수정하지 못하게 막습니다.
    @Column(name = "id", updatable = false)
    private Long id;

    // [@Column(name = "title", nullable = false)] 제목 컬럼입니다. nullable = false 는 "비어 있으면 저장 불가"라는 뜻입니다.
    @Column(name = "title", nullable = false)
    private String title;

    // 본문 컬럼입니다. 역시 비어 있으면 저장할 수 없습니다.
    @Column(name = "content", nullable = false)
    private String content;

    // 썸네일 이미지 주소 컬럼입니다. nullable 옵션이 없으므로 비어 있어도 됩니다. (예: /uploads/abc.png)
    @Column(name = "image_url")
    private String imageUrl;

    // [@CreatedDate] 이 글이 처음 저장될 때 현재 시간이 자동으로 들어갑니다.
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // [@LastModifiedDate] 글이 저장되거나 수정될 때마다 현재 시간이 자동으로 갱신됩니다.
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // [11장 추가] 글쓴이
    // 값: 토큰의 sub(이메일). 예) "gildong@gmail.com"
    // nullable = false : 글쓴이 없이 글을 저장할 수 없습니다.
    // ※ update() 메서드는 author 를 바꾸지 않습니다. 글쓴이는 처음 저장할 때 한 번만 정해집니다.
    @Column(name = "author", nullable = false)
    private String author;


    // 빌더로 글을 만들 때 쓰는 생성자입니다.
    // 예) Article.builder().author("gildong@gmail.com").title("안녕").content("첫 글").build()
    // [11장] 첫 번째 값으로 author 가 추가되었습니다.
    @Builder
    public Article(String author,  String title, String content, String imageUrl) {
        this.author = author;        //글쓴이 
        this.title = title;         // 매개변수로 받은 제목을 이 객체의 title 필드에 넣습니다.
        this.content = content;     // 본문을 content 필드에 넣습니다.
        this.imageUrl = imageUrl;   // 이미지 주소를 imageUrl 필드에 넣습니다.
    }

    // update : 수정 요청이 왔을 때 기존 글의 제목, 본문, 이미지 주소를 새 값으로 바꾸는 메소드입니다.
    //   호출하는 곳 : BlogService.update() 메소드
    //   이 메소드로 바꾼 값은 @Transactional 이 끝나는 시점에 DB 에 UPDATE 로 저장됩니다.
    public void update(String title, String content, String imageUrl) {
        this.title = title;         // 새 제목으로 바꿉니다.
        this.content = content;     // 새 본문으로 바꿉니다.
        this.imageUrl = imageUrl;   // 새 이미지 주소로 바꿉니다.
    }
}
