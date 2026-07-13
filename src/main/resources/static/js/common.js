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
    </nav>
</header>
`;

document.getElementById("topbar-placeholder").innerHTML = TOPBAR_HTML;
