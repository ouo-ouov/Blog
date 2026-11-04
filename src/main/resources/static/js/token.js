// 파일 위치: src/main/resources/static/js/token.js   (브라우저 주소: /js/token.js)
//
// ============================================================
// 이 파일이 하는 일  [11장 추가]
// ============================================================
// 구글 로그인이 끝나면 OAuth2SuccessHandler 가 브라우저를 이 주소로 보냅니다.
//     /articles?token=eyJhbGciOiJIUzI1NiJ9.eyJpc3Mi….xxxx
// 이 파일은 주소창의 token 값을 꺼내 브라우저의 localStorage 에 "access_token" 이라는 이름으로 저장합니다.
// 그 뒤 article.js 가 API 를 부를 때마다 이 값을 꺼내 Authorization: Bearer … 헤더에 붙입니다.
//
//   OAuth2SuccessHandler ──302──▶ /articles?token=액세스토큰
//                                   └ articleList.html 이 열리며 <script src="/js/token.js"> 실행
//                                       └ localStorage.setItem("access_token", 액세스토큰)
//
// ============================================================
// 꼭 알아둘 낱말
// ============================================================
// localStorage     : 브라우저 안의 작은 저장소입니다. 창을 닫아도 남아 있고, 같은 사이트(주소·포트)에서만 읽을 수 있습니다.
//                    개발자 도구(F12) → Application → Local Storage 에서 직접 볼 수 있습니다.
// URLSearchParams  : 주소의 ?이름=값&이름=값 부분을 쉽게 읽게 해 주는 브라우저 기본 도구입니다.
// location.search  : 현재 주소의 ? 부터 끝까지입니다. 예) "?token=eyJ…"
//
// ============================================================
// 꼭 조심할 점
// ============================================================
// 1. 이 파일은 articleList.html 에만 들어 있습니다. (로그인 성공 후 이동하는 화면이 /articles 이기 때문)
// 2. token 값이 없는 주소(/articles)로 들어오면 아무것도 하지 않습니다. 이미 저장된 값은 그대로 둡니다.
// 3. ⚠️ 저장한 뒤에도 주소창에는 ?token=… 이 그대로 남습니다. 화면을 캡처하거나 주소를 복사해 공유할 때 조심하세요.
// 4. ⚠️ localStorage 는 그 페이지의 모든 자바스크립트가 읽을 수 있어서, 악성 스크립트가 섞이면 토큰이 새어 나갈 수 있습니다.
// ============================================================

// 주소창에서 token 값을 꺼냅니다. 없으면 null 입니다.
// ※ 아래 searchParam 함수는 이 줄보다 뒤에 적혀 있지만, function 으로 만든 함수는 미리 준비되므로(호이스팅) 먼저 불러도 됩니다.
const token = searchParam('token')

// token 이 있으면(null 이 아니면) localStorage 에 저장합니다.
if (token) {
    localStorage.setItem("access_token", token)
}

// 주소의 ?key=값 에서 key 에 해당하는 값을 돌려줍니다.
// 예) 주소가 /articles?token=abc 이면 searchParam('token') → "abc"
function searchParam(key) {
    return new URLSearchParams(location.search).get(key);
}