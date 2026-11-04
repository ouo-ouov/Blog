// ============================================================
// [파일 설명] Article(글) 테이블에 접근하는 리포지토리(저장소) 인터페이스입니다.
// 직접 SQL 을 쓰지 않아도 저장, 조회, 수정, 삭제 기능을 사용할 수 있습니다.
//
// [호출하는 곳]
//   BlogService : save(), findAll(), findById(), deleteById(), update() 등에서 사용
// ============================================================
package com.shinsunyoung.blog.repository;

// Article : 리포지토리가 다루는 엔티티(글 1개) 클래스입니다.
import com.shinsunyoung.blog.domain.Article;

// JpaRepository : 저장·조회·삭제 기능이 이미 만들어져 있는 스프링 데이터 JPA 의 부모 인터페이스입니다.
import org.springframework.data.jpa.repository.JpaRepository;

// [interface] 인터페이스라서 메소드의 몸체(실행 코드)를 직접 쓰지 않습니다.
//   스프링 데이터 JPA 가 실행 시점에 이 인터페이스를 구현한 객체를 자동으로 만들어 줍니다.
// extends JpaRepository<Article, Long>
//   Article : 다룰 엔티티 타입
//   Long    : 엔티티의 기본키(id) 타입
//   이 한 줄만으로 save(), findAll(), findById(), deleteById(), deleteAll() 등을 쓸 수 있습니다.
public interface BlogRepository extends JpaRepository<Article, Long> {
    // 추가로 만든 메소드가 없으므로 본문이 비어 있습니다.
}
