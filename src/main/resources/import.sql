-- 파일 위치: src/main/resources/import.sql
-- 서버가 켜질 때 Hibernate 가 표를 만든 뒤 이 파일의 SQL 을 한 줄씩 실행해 예시 글 3개를 넣는다.
-- (한 줄이 SQL 한 문장이다. -- 로 시작하는 줄은 주석이라 실행되지 않는다.)
--
-- [11장 변경] Article 에 author(글쓴이, nullable = false) 열이 생겨서 author 값을 함께 넣는다.
--   author 를 빼면 "NULL 을 넣을 수 없다"는 오류가 나서 글이 들어가지 않는다.
-- ⚠️ 'user1', 'user2', 'user3' 은 실제 회원 이메일이 아니다.
--   구글 로그인 사용자의 이름(이메일)과 다르므로, 이 3개 글은 누구도 수정·삭제할 수 없다. (BlogService.authorizeArticleAuthor)
INSERT INTO article (title, content, author, image_url, created_at, updated_at) VALUES ('제목 1', '내용 1', 'user1', NULL, NOW(), NOW())

INSERT INTO article (title, content, author, image_url, created_at, updated_at) VALUES ('제목 2', '내용 2', 'user2', NULL, NOW(), NOW())

INSERT INTO article (title, content, author, image_url, created_at, updated_at) VALUES ('제목 3', '내용 3', 'user3', NULL, NOW(), NOW())