// 파일 위치: src/main/resources/static/js/article.js   (브라우저 주소: /js/article.js)
//
// ============================================================
// 이 파일이 하는 일
// ============================================================
// 글 화면(articleList.html, article.html, newArticle.html)의 버튼 동작을 맡는 자바스크립트입니다.
//   이미지 업로드 · 이미지 제거 · AI 썸네일 · AI 글쓰기 도우미 · 글 등록 · 글 수정 · 글 삭제 · 로그아웃
//
// ============================================================
// 10장 → 11장, 무엇이 바뀌었나  (토큰을 들고 API 를 부르도록)
// ============================================================
//   1. 모든 /api/** 요청에 헤더를 붙입니다.
//        Authorization: Bearer <localStorage 의 access_token>
//      11장 서버(WebOAuthSecurityConfig)는 /api/** 를 authenticated 로 막아 두었기 때문입니다.
//   2. 등록·수정·삭제는 fetch 를 직접 쓰지 않고 공통 함수 httpRequest() 를 씁니다.
//        httpRequest() 는 401(토큰 없음·만료)을 받으면
//          → refresh_token 쿠키로 POST /api/token → 새 액세스 토큰 저장 → 같은 요청을 다시 보냅니다.
//   3. 성공·실패 알림을 나눴습니다. (success / fail 함수)
//   4. 새 함수: 로그아웃 버튼(logout-btn), getCookie(), deleteCookie(), httpRequest()
//
// ============================================================
// 토큰 재발급 흐름 (httpRequest 안에서 일어나는 일)
// ============================================================
//   ① fetch(원래 요청 + Bearer 액세스토큰)
//        ├─ 200 또는 201 → success()                                        끝
//        ├─ 401 이고 refresh_token 쿠키가 있음
//        │     ② fetch POST /api/token  { refreshToken: 쿠키값 }
//        │          ├─ 성공(201) → { accessToken: 새 토큰 }
//        │          │     ③ localStorage 의 access_token 을 새 토큰으로 바꿈
//        │          │     ④ httpRequest(같은 요청) 다시 호출 → ①부터 반복 (이번엔 새 토큰)
//        │          └─ 실패 → catch → fail()
//        └─ 그 밖의 상태(403, 404, 500 …) 또는 쿠키 없음 → fail()          끝
//
// ============================================================
// 꼭 알아둘 낱말
// ============================================================
// fetch        : 브라우저에서 서버로 HTTP 요청을 보내는 기본 함수입니다. 결과는 Promise(나중에 오는 값)입니다.
// .then(함수)  : 앞의 일이 끝나면 그 결과로 다음 일을 하라는 뜻입니다.
// .catch(함수) : 앞의 일들 중 오류가 나면 실행할 일입니다.
// localStorage : 브라우저 저장소. token.js 가 access_token 을 여기에 저장해 둡니다.
// document.cookie : 이 페이지가 읽을 수 있는 쿠키 전부를 "이름=값; 이름=값" 한 줄 글자로 돌려줍니다.
// 콜백(callback)  : 다른 함수에 넘겨 두었다가 "때가 되면" 불리는 함수입니다. 여기서는 success, fail 입니다.
//
// ============================================================
// 꼭 조심할 점
// ============================================================
// 1. ✅ [로그아웃 완성으로 해결] 로그아웃 기능(logout-btn)
//    - articleList.html 상단에 <a id="logout-btn">로그아웃</a> 링크를 추가했습니다. (로그인 상태일 때만 보임)
//    - DELETE /api/refresh-token 을 받는 TokenApiController.deleteRefreshToken() 을 추가했습니다.
//      → DB 리프레시 토큰 삭제(RefreshTokenService.delete) + 쿠키 삭제 → 브라우저 토큰 삭제 → /login 이동
// 2. ✅ [해결] articleList.html 의 "글 등록" 버튼 id 를 create-btn → new-article-btn 으로 바꿨습니다.
//    예전에는 목록 화면에도 id="create-btn" 이 있어서, 아래 "생성 기능"의 클릭 처리기가 붙었고
//    그 화면에는 title 입력칸이 없어서 오류(null 의 value)가 났습니다. (화면 이동만 되고 콘솔에 오류)
// 3. ✅ [해결] 수정·생성 기능의 body = … 를 const body = … 로 바꿔 전역 변수가 생기지 않게 했습니다.
// 4. AI 글쓰기 도우미(get-suggestion-btn)는 httpRequest 를 쓰지 않으므로, 토큰이 만료되어 401 이 와도 재발급하지 않습니다.
//    이미지 업로드·AI 썸네일도 마찬가지입니다.
// ============================================================

// ------------------------------------------------------------
// 화면이 다 읽히면: 이미 이미지 주소가 있는 글(수정 화면)은 미리보기를 보여 줍니다.
// ?. (옵셔널 체이닝) : image-url 칸이 없는 화면이면 오류 없이 undefined 가 됩니다.
// ------------------------------------------------------------
window.addEventListener('DOMContentLoaded', () => {
    const imageUrl = document.getElementById('image-url')?.value;
    if (imageUrl) {
        displayImagePreview(imageUrl);
    }
});

// 이미지 업로드 버튼
// 파일을 고르면(change) → POST /api/upload (multipart) → 응답의 imageUrl 을 숨은 칸에 넣고 미리보기
const imageUpload = document.getElementById('image-upload');
if (imageUpload) {
    imageUpload.addEventListener('change', async (event) => {
        const file = event.target.files[0]; // 고른 첫 번째 파일
        if (!file) return;

        // 그림 파일(image/png, image/jpeg …)이 아니면 멈춥니다.
        if (!file.type.startsWith('image/')) {
            alert('이미지 파일만 업로드 가능합니다.');
            return;
        }

        // 파일을 보내는 양식(FormData)을 만듭니다. 'file' 이름은 UploadController 의 @RequestParam("file") 과 같아야 합니다.
        const formData = new FormData();
        formData.append('file', file);

        // [11장] /api/upload 도 /api/** 이므로 Authorization 헤더를 붙입니다.
        // ※ Content-Type 은 일부러 적지 않습니다. FormData 를 보내면 브라우저가 multipart/form-data 와 경계 문자열을 알아서 넣습니다.
        fetch('/api/upload', {
            method: 'POST',
            body: formData,
            headers: {
                Authorization: 'Bearer ' + localStorage.getItem('access_token'),
            },
        }).then((response) => {
            if (!response.ok) {
                alert('이미지 업로드에 실패했습니다.');
                throw new Error();
            }
            return response.json();
        })
            .then((data) => {
                document.getElementById('image-url').value = data.imageUrl; // 예) /uploads/3f2a….png
                displayImagePreview(data.imageUrl);
            })
            .catch((e) => console.error(e));
    });
}

// 이미지 미리보기를 보여 주는 함수입니다. 미리보기 칸이 없는 화면에서는 아무것도 하지 않습니다.
function displayImagePreview(imageUrl) {
    const preview = document.getElementById('image-preview');
    const previewImg = document.getElementById('preview-img');

    if (preview && previewImg && imageUrl) {
        previewImg.src = imageUrl;
        preview.style.display = 'block';
    }
}

// 이미지 제거 버튼: 이미지 주소·선택한 파일·미리보기를 모두 비웁니다. (서버의 파일은 지우지 않음)
const removeImageButton = document.getElementById('remove-image-btn');
if (removeImageButton) {
    removeImageButton.addEventListener('click', () => {
        document.getElementById('image-url').value = '';
        document.getElementById('image-upload').value = '';
        document.getElementById('image-preview').style.display = 'none';
    });
}

// AI 썸네일 생성 버튼
// 제목·본문 → POST /api/ai-thumbnails → AI 가 만든 그림을 서버가 저장 → 그 주소로 미리보기
const aiThumbnailButton = document.getElementById('ai-thumbnail-btn');
if (aiThumbnailButton) {
    aiThumbnailButton.addEventListener('click', async () => {
        const title = document.getElementById('title').value;
        const content = document.getElementById('content').value;

        // 제목과 본문이 둘 다 비어 있으면 멈춥니다. (trim: 앞뒤 빈칸 제거)
        if (!title.trim() && !content.trim()) {
            alert('제목이나 내용을 먼저 입력해주세요.');
            return;
        }

        // 기다리는 동안 "생성 중" 표시를 켜고 버튼을 잠급니다. (여러 번 누르기 방지)
        const loadingDiv = document.getElementById('ai-thumbnail-loading');
        loadingDiv.style.display = 'block';
        aiThumbnailButton.disabled = true;

        // [11장] Authorization 헤더를 붙여 보냅니다.
        fetch('/api/ai-thumbnails', {
            method: 'POST',
            body: JSON.stringify({
                title: title,
                content: content
            }),
            headers: {
                Authorization: 'Bearer ' + localStorage.getItem('access_token'),
                'Content-Type': 'application/json',
            },
        }).then((response) => {
            if (!response.ok) {
                alert('썸네일 생성에 실패했습니다.');
                throw new Error();
            }
            return response.json();
        }).then((data) => {
            document.getElementById('image-url').value = data.imageUrl;
            displayImagePreview(data.imageUrl);
        }).finally(() => {
            // 성공하든 실패하든 "생성 중" 표시를 끄고 버튼을 다시 엽니다.
            loadingDiv.style.display = 'none';
            aiThumbnailButton.disabled = false;
        });
    });
}

// AI 도움 받기 버튼: 질문을 적는 창(모달)을 엽니다. ($ 는 jQuery, modal 은 부트스트랩 기능)
const aiAssistButton = document.getElementById('ai-assist-btn');

if (aiAssistButton) {
    aiAssistButton.addEventListener('click', event => {
        $('#aiAssistModal').modal('show');
        document.getElementById('ai-suggestion').style.display = 'none';
        document.getElementById('ai-question').value = '';
    });
}

// "제안 받기" 버튼: 제목·본문·질문 → POST /api/ai-suggestions → 제안 목록을 화면에 그립니다.
const getSuggestionButton = document.getElementById('get-suggestion-btn');

if (getSuggestionButton) {
    getSuggestionButton.addEventListener('click', event => {
        const title = document.getElementById('title').value;
        const content = document.getElementById('content').value;
        const question = document.getElementById('ai-question').value;

        if (!question.trim()) {
            alert('고민되는 내용을 입력해주세요.');
            return;
        }

        // 로딩 표시
        document.getElementById('ai-loading').style.display = 'block';
        document.getElementById('ai-suggestion').style.display = 'none';

        const body = JSON.stringify({
            title: title,
            content: content,
            question: question
        });

        // [11장] Authorization 헤더를 붙여 보냅니다. (401 이 와도 재발급은 하지 않음 — 위 "조심할 점" 4번)
        fetch('/api/ai-suggestions', {
            method: 'POST',
            headers: {
                Authorization: 'Bearer ' + localStorage.getItem('access_token'),
                'Content-Type': 'application/json',
            },
            body: body
        })
            .then(response => {
                return response.json();
            })
            .then(data => {
                document.getElementById('ai-loading').style.display = 'none';

                const suggestionContent = document.getElementById('ai-suggestion-content');

                // 응답 예) { "suggestions": ["첫 제안", "두 번째 제안", …] }
                // 제안마다 클릭할 수 있는 목록 줄(li)을 글자로 이어 붙여 만듭니다.
                let html = '';
                if (data.suggestions && data.suggestions.length > 0) {
                    html += '<ul class="list-group">';
                    data.suggestions.forEach((suggestion, index) => {
                        html += `<li class="list-group-item suggestion-item" style="cursor: pointer;" data-suggestion="${suggestion.replace(/"/g, '&quot;')}" title="클릭하면 본문에 추가됩니다">
                                ${suggestion}
                                <small class="text-muted float-right">클릭하여 추가</small>
                             </li>`;
                    });
                    html += '</ul>';
                }

                suggestionContent.innerHTML = html;
                document.getElementById('ai-suggestion').style.display = 'block';
            })
    });
}

// 제안 목록을 클릭하면: 그 제안 글을 본문 끝에 덧붙이고 창을 닫습니다.
// 목록 줄은 나중에 만들어지므로, 바깥 상자에 클릭 처리기를 하나만 달고 closest() 로 어느 줄인지 찾습니다. (이벤트 위임)
const suggestionContent = document.getElementById('ai-suggestion-content');
if (suggestionContent) {
    suggestionContent.addEventListener('click', function(e) {
        const suggestionItem = e.target.closest('.suggestion-item');
        if (suggestionItem) {
            const suggestion = suggestionItem.getAttribute('data-suggestion');
            const contentTextarea = document.getElementById('content');

            // 본문이 있고 줄바꿈으로 끝나지 않으면 빈 줄을 하나 넣고 이어 붙입니다.
            const currentContent = contentTextarea.value;
            const separator = currentContent && !currentContent.endsWith('\n') ? '\n\n' : '';
            contentTextarea.value = currentContent + separator + suggestion;

            $('#aiAssistModal').modal('hide');

            contentTextarea.focus();
        }
    });
}

// ------------------------------------------------------------
// 삭제 기능 (article.html 의 "삭제" 버튼)
// DELETE /api/articles/{id} → 성공: 목록으로 / 실패(남의 글 등): "삭제 실패했습니다."
// ------------------------------------------------------------
const deleteButton = document.getElementById('delete-btn');

if (deleteButton) {
    deleteButton.addEventListener('click', event => {
        let id = document.getElementById('article-id').value; // article.html 의 숨은 칸에 들어 있는 글 번호
        // 성공했을 때 할 일
        function success() {
            alert('삭제가 완료되었습니다.');
            location.replace('/articles');
        }

        // 실패했을 때 할 일
        // [수정·삭제 완성 - 변경] fail() → fail(response)
        //   글쓴이가 아니면 서버(BlogService.authorizeArticleAuthor)가 403 을 돌려줍니다. (예전에는 500)
        //   response?.status : 응답이 있으면 상태 코드, 없으면(재발급 실패 등) undefined
        //   403 이면 이유를 알려 주고 글 상세 화면에 그대로 머뭅니다. (목록으로 보내면 어느 글이었는지 헷갈리므로)
        function fail(response) {
            if (response?.status === 403) {
                alert('본인이 작성한 글만 삭제할 수 있습니다.');
                return;
            }
            alert('삭제 실패했습니다.');
            location.replace('/articles');
        }

        // 본문 없이(null) DELETE 요청. 토큰 처리·재발급은 httpRequest 가 알아서 합니다.
        httpRequest('DELETE',`/api/articles/${id}`, null, success, fail);
    });
}

// ------------------------------------------------------------
// 수정 기능 (newArticle.html 의 "수정" 버튼, 주소: /new-article?id=3)
// PUT /api/articles/{id} + JSON 본문
// ------------------------------------------------------------
const modifyButton = document.getElementById('modify-btn');

if (modifyButton) {
    modifyButton.addEventListener('click', event => {
        let params = new URLSearchParams(location.search); // 주소의 ?id=3
        let id = params.get('id');                         // → "3"

        // 보낼 JSON 본문. 예) {"title":"새 제목","imageUrl":"","content":"새 본문"}
        // [수정] body = … → const body = … : let/const 없이 쓰면 window.body 라는 전역 변수가 생깁니다.
        //   const 로 이 클릭 처리 안에서만 쓰는 변수로 만들었습니다.
        const body = JSON.stringify({
            title: document.getElementById('title').value,
            imageUrl: document.getElementById('image-url').value,
            content: document.getElementById('content').value
        })

        function success() {
            alert('수정 완료되었습니다.');
            location.replace(`/articles/${id}`);
        }

        // [수정·삭제 완성 - 변경] fail() → fail(response) : 403(남의 글)이면 이유를 알려 줍니다.
        //   어느 경우든 글 상세 화면(/articles/{id})으로 돌아가 수정되지 않은 원래 글을 보여 줍니다.
        function fail(response) {
            if (response?.status === 403) {
                alert('본인이 작성한 글만 수정할 수 있습니다.');
            } else {
                alert('수정 실패했습니다.');
            }
            location.replace(`/articles/${id}`);
        }

        httpRequest('PUT',`/api/articles/${id}`, body, success, fail);
    });
}

// ------------------------------------------------------------
// 생성 기능 (newArticle.html 의 "등록" 버튼)
// POST /api/articles + JSON 본문 → 서버가 토큰 주인(이메일)을 글쓴이로 저장
// ------------------------------------------------------------
const createButton = document.getElementById('create-btn');

if (createButton) {
    createButton.addEventListener('click', event => {
        // ※ 글쓴이(author)는 보내지 않습니다. 서버가 토큰에서 꺼내 정합니다.
        // [수정] body = … → const body = … (전역 변수가 생기지 않도록)
        const body = JSON.stringify({
            title: document.getElementById('title').value,
            imageUrl: document.getElementById('image-url').value,
            content: document.getElementById('content').value
        });
        function success() {
            alert('등록 완료되었습니다.');
            location.replace('/articles');
        };
        function fail() {
            alert('등록 실패했습니다.');
            location.replace('/articles');
        };

        httpRequest('POST','/api/articles', body, success, fail)
    });
}

// ------------------------------------------------------------
// [11장 추가 → 로그아웃 완성] 로그아웃 기능 (id="logout-btn" 링크가 있는 articleList.html 에서만 동작)
// ------------------------------------------------------------
// [변경] 예전 주석의 "logout-btn 도, DELETE /api/refresh-token 컨트롤러도 없습니다" 는 이제 해결됐습니다.
//   - 링크   : articleList.html 상단 <a id="logout-btn">로그아웃</a>
//   - 서버   : TokenApiController.deleteRefreshToken()  (DELETE /api/refresh-token)
//
// 전체 흐름:
//   ① 화면이 열리면 → 로그인 상태(토큰 있음)인지 보고 [로그아웃] 링크를 보이거나 숨깁니다.
//   ② 링크 클릭    → DELETE /api/refresh-token (Authorization: Bearer <액세스 토큰>)
//                     · 액세스 토큰이 만료돼 401 이 오면 httpRequest 가 refresh_token 쿠키로 새 토큰을 받아 다시 요청합니다.
//   ③ 서버         → DB 의 리프레시 토큰 삭제 + refresh_token 쿠키 삭제(Set-Cookie Max-Age=0) → 200
//   ④ success()    → localStorage 의 access_token 삭제, 쿠키 한 번 더 삭제, /login 으로 이동
const logoutButton = document.getElementById('logout-btn');

if (logoutButton) {
    // ── ① 로그인 상태일 때만 [로그아웃] 링크 보이기 ──
    // token.js 가 이 파일보다 먼저 실행되어(articleList.html 의 <script> 순서),
    // 주소의 ?token= 값을 이미 localStorage 에 저장해 둔 상태입니다.
    //   access_token(localStorage) 이 있거나, refresh_token(쿠키) 이 있으면 → 로그인 상태로 봅니다.
    //   (액세스 토큰이 지워졌어도 리프레시 토큰이 있으면 httpRequest 가 재발급할 수 있으므로 로그인 상태입니다)
    const isLoggedIn = localStorage.getItem('access_token') || getCookie('refresh_token');
    // style.display : '' 로 두면 원래 모양(인라인 버튼)대로 보이고, 'none' 이면 숨겨집니다.
    logoutButton.style.display = isLoggedIn ? '' : 'none';

    // ── ② 클릭했을 때 로그아웃 처리 ──
    logoutButton.addEventListener('click', event => {
        // <a href="#"> 의 기본 동작(주소 끝에 # 를 붙이고 화면 맨 위로 이동)을 막습니다.
        event.preventDefault();

        // 브라우저에 남은 토큰을 모두 지우는 함수 (성공·실패 양쪽에서 씁니다)
        function clearTokens() {
            // 로컬 스토리지에 저장된 액세스 토큰을 삭제
            localStorage.removeItem('access_token');
            // 쿠키에 저장된 리프레시 토큰을 삭제 (서버도 지우지만, 한 번 더 확실히 지웁니다)
            deleteCookie('refresh_token');
        }

        // 성공(200) : 서버의 리프레시 토큰까지 지워졌습니다. 브라우저 토큰도 지우고 로그인 화면으로 갑니다.
        function success() {
            clearTokens();
            alert('로그아웃 되었습니다.');
            // location.replace : 뒤로 가기 기록에 남기지 않고 이동합니다. (뒤로 가기로 글 목록에 돌아오지 않게)
            location.replace('/login');
        }

        // 실패 : 토큰이 이미 만료·삭제되어 서버가 누구인지 알 수 없는 경우입니다. (재발급도 실패)
        //   이미 서버 입장에서는 로그인 상태가 아니므로, 브라우저에 남은 토큰만 지우고 로그인 화면으로 보냅니다.
        //   [변경] 예전 코드는 알림만 띄우고 토큰을 그대로 두어서, 링크를 다시 눌러도 계속 실패했습니다.
        function fail() {
            clearTokens();
            alert('로그인 정보가 만료되어 로그아웃 처리했습니다.');
            location.replace('/login');
        }

        // 본문 없이(null) DELETE 요청. 액세스 토큰 첨부·401 재발급은 httpRequest 가 처리합니다.
        httpRequest('DELETE','/api/refresh-token', null, success, fail);
    });
}

// ------------------------------------------------------------
// [11장 추가] 쿠키를 가져오는 함수
// ------------------------------------------------------------
// document.cookie 예) "refresh_token=eyJ…; other=1"
//   1. ';' 로 나눠 ["refresh_token=eyJ…", " other=1"]
//   2. 각 조각의 첫 빈칸을 지우고, '=' 로 나눠 [이름, 값]
//   3. 이름이 key 와 같으면 값을 결과로 정하고 true 를 돌려 반복을 멈춤 (some 은 true 가 나오면 멈춤)
// 없으면 null 을 돌려줍니다.
// ※ refresh_token 쿠키에 HttpOnly 가 없어서 자바스크립트로 읽을 수 있는 것입니다. (CookieUtil 참고)
// ※ split('=') 은 값 안의 '=' 에서도 나뉘는데, JWT 는 Base64URL 이라 '=' 이 들어가지 않아 문제가 없습니다.
function getCookie(key) {
    var result = null;
    var cookie = document.cookie.split(';');
    cookie.some(function (item) {
        // 공백을 제거
        item = item.replace(' ', '');

        var dic = item.split('=');

        if (key === dic[0]) {
            result = dic[1];
            return true;
        }
    });

    return result;
}

// ------------------------------------------------------------
// [11장 추가] 쿠키를 삭제하는 함수
// ------------------------------------------------------------
// 만료 날짜를 과거(1970년)로 정해 다시 쓰면 브라우저가 그 쿠키를 지웁니다.
// [로그아웃 수정] 예전 코드는 path 를 적지 않아서, 현재 페이지 경로(예: /articles/3)에 따라
//   서버가 Path=/ 로 만든 refresh_token 쿠키가 지워지지 않을 수 있었습니다.
//   쿠키는 "이름 + path" 가 같아야 같은 쿠키로 보므로, CookieUtil.addCookie 와 똑같이 path=/ 를 붙였습니다.
function deleteCookie(name) {
    document.cookie = name + '=; expires=Thu, 01 Jan 1970 00:00:01 GMT; path=/;';
}

// ------------------------------------------------------------
// [11장 추가] HTTP 요청을 보내는 공통 함수 (토큰 첨부 + 401 이면 재발급 후 재시도)
// ------------------------------------------------------------
// 받는 값:
//   method  : 'GET', 'POST', 'PUT', 'DELETE'
//   url     : 요청 주소. 예) '/api/articles/3'
//   body    : 보낼 JSON 문자열. 없으면 null
//   success : 성공(200, 201)했을 때 부를 함수
//   fail    : 실패했을 때 부를 함수
// 흐름은 파일 맨 위 "토큰 재발급 흐름" 그림을 보세요.
function httpRequest(method, url, body, success, fail) {
    // ① 원래 요청 보내기 (Bearer 액세스 토큰 첨부)
    fetch(url, {
        method: method,
        headers: { // 로컬 스토리지에서 액세스 토큰 값을 가져와 헤더에 추가
            Authorization: 'Bearer ' + localStorage.getItem('access_token'),
            'Content-Type': 'application/json',
        },
        body: body,
    }).then(response => {
        // 200(OK) 또는 201(Created) 이면 성공
        if (response.status === 200 || response.status === 201) {
            return success();
        }
        // 실패했으면 리프레시 토큰 쿠키가 있는지 봅니다.
        const refresh_token = getCookie('refresh_token');
        // 401(인증 없음 = 액세스 토큰 없음·만료·가짜) 이고 리프레시 토큰이 있으면 재발급을 시도합니다.
        if (response.status === 401 && refresh_token) {
            // ② 새 액세스 토큰 요청. /api/token 은 permitAll 이라 토큰 없이도 통과합니다.
            //    (헤더의 만료된 액세스 토큰은 서버에서 무시됩니다)
            fetch('/api/token', {
                method: 'POST',
                headers: {
                    Authorization: 'Bearer ' + localStorage.getItem('access_token'),
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ // 본문 예) {"refreshToken":"eyJ…"} → CreateAccessTokenRequest 로 들어감
                    refreshToken: getCookie('refresh_token'),
                }),
            })
                .then(res => {
                    // 성공(201)이면 JSON 을 꺼냅니다. 실패면 아무것도 돌려주지 않아 다음 단계의 result 가 undefined 가 됩니다.
                    if (res.ok) {
                        return res.json();
                    }
                })
                .then(result => { // 재발급이 성공하면 로컬 스토리지값을 새로운 액세스 토큰으로 교체
                    // ③ 새 토큰 저장 (result 가 undefined 면 여기서 오류 → 아래 catch → fail)
                    localStorage.setItem('access_token', result.accessToken);
                    // ④ 같은 요청을 새 토큰으로 다시 보냅니다.
                    httpRequest(method, url, body, success, fail);
                })
                .catch(error => fail());
        } else {
            // 401 이 아닌 실패(403, 500 등)이거나 리프레시 토큰이 없으면 바로 실패 처리
            // [수정·삭제 완성 - 변경] fail() → fail(response)
            //   실패한 응답 객체를 넘겨서, 부르는 쪽(수정·삭제)이 response.status 로 실패 이유를 나눌 수 있게 했습니다.
            //   예) 403 이면 "본인이 작성한 글만 수정할 수 있습니다." / 그 밖이면 "수정 실패했습니다."
            //   등록·로그아웃의 fail() 처럼 매개변수를 받지 않는 함수에 넘겨도 그냥 무시되므로 문제없습니다.
            return fail(response);
        }
    });
}