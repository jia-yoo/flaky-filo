// 모든 화면(index, materials, products 등)이 공유하는 상단 네비게이션.
// HTML엔 <div id="topbar-placeholder"></div> 한 줄만 넣고 이 스크립트를 불러오면,
// 여기 내용만 고치면 모든 화면에 한 번에 반영된다.
// (Thymeleaf 같은 서버 템플릿이 아니라 순수 정적 HTML이라 "include" 문법이 없어서 JS로 대신함)

const CURRENT_PATH = location.pathname.split("/").pop() || "index.html";

const TOPBAR_HTML = `
<header class="topbar">
    <a class="brand" href="index.html">
        <img alt="Flaky Filo" class="brand-logo" src="img/logo-icon.png">
        <span class="brand-sub">FLAKY FILO</span>
    </a>
    <nav class="nav-links">
        <a href="index.html" class="${CURRENT_PATH === "index.html" ? "active" : ""}">대시보드</a>
        <a href="materials.html" class="${CURRENT_PATH.startsWith("material") ? "active" : ""}">원재료</a>
        <a href="products.html" class="${CURRENT_PATH === "products.html" ? "active" : ""}">완제품</a>
        <a href="daily-operations.html" class="${CURRENT_PATH === "daily-operations.html" ? "active" : ""}">일일 운영</a>
    </nav>
</header>
`;

document.getElementById("topbar-placeholder").innerHTML = TOPBAR_HTML;

// 모든 <dialog>에 공통 적용: 모달 안(내용) 말고 바깥(배경) 클릭하면 자동으로 닫힘.
// <dialog>는 showModal()로 열리면 배경(::backdrop)까지 자기 자신이 채우는데,
// 배경을 클릭하면 이벤트의 target이 dialog 엘리먼트 자기 자신이 된다 (내용 클릭 시엔 안쪽 자식이 target).
// 그래서 "target이 dialog 자신이면" = "배경을 클릭한 것"으로 판단해서 닫아준다.
//
// 주의: 이 스크립트는 <body> 맨 위(topbar-placeholder 바로 뒤)에서 즉시 실행되는데,
// 그 시점엔 아래쪽에 있는 <dialog>들이 아직 브라우저에 파싱되기 전이라 못 찾는다.
// 그래서 DOMContentLoaded(페이지 전체 파싱 완료 시점)까지 기다렸다가 찾는다.
document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("dialog").forEach((dialog) => {
        dialog.addEventListener("click", (e) => {
            if (e.target === dialog) {
                dialog.close();
            }
        });
    });

    toastEl = document.getElementById("toast");
});

// ===== 공용 토스트 알림 =====
// material.js/products.js/material-history.js 등 여러 화면이 똑같은 로직을 중복해서
// 갖고 있던 걸 여기 하나로 통합 - 화면마다 <div class="toast" id="toast"></div>만 있으면 됨.
let toastEl;
let toastTimer;

function showToast(message, isError = false) {
    if (!toastEl) return; // 혹시 이 화면에 toast 요소가 없으면 조용히 무시

    clearTimeout(toastTimer);

    // 지금 열려있는 <dialog>가 있으면 그 안으로 토스트를 옮겨서 모달 위에 보이게 함
    // (dialog는 브라우저 top-layer에 그려져서, 모달 바깥의 고정 요소는 z-index와 상관없이 가려짐)
    const openDialog = document.querySelector("dialog[open]");
    (openDialog || document.body).appendChild(toastEl);

    toastEl.textContent = message;
    toastEl.classList.toggle("error", isError);
    toastEl.classList.add("show");
    toastTimer = setTimeout(() => toastEl.classList.remove("show"), 2500);
}