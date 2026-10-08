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
        <a href="orders.html" class="${CURRENT_PATH === "orders.html" ? "active" : ""}">주문</a>
    </nav>
</header>
`;

document.getElementById("topbar-placeholder").innerHTML = TOPBAR_HTML;

// 모든 <dialog>에 공통 적용: 모달 안(내용) 말고 바깥(배경) 클릭하면 자동으로 닫힘.
// <dialog>는 showModal()로 열리면 배경(::backdrop)까지 자기 자신이 채우는데,
// 배경을 클릭하면 이벤트의 target이 dialog 엘리먼트 자기 자신이 된다.
// 그런데 모달 안쪽 여백(padding)이나 빈 공간을 클릭해도 target이 dialog 자신이라 구분이 안 된다 -
// 그래서 target 대신 "클릭 좌표가 모달 네모 영역 밖인지"로 판단해서 닫아준다.
// 또 모달 안에서 마우스를 누른 채(텍스트 드래그 등) 바깥에서 떼도 닫히지 않도록,
// 누른 위치(mousedown)와 뗀 위치(click) 둘 다 바깥일 때만 닫는다.
//
// 주의: 이 스크립트는 <body> 맨 위(topbar-placeholder 바로 뒤)에서 즉시 실행되는데,
// 그 시점엔 아래쪽에 있는 <dialog>들이 아직 브라우저에 파싱되기 전이라 못 찾는다.
// 그래서 DOMContentLoaded(페이지 전체 파싱 완료 시점)까지 기다렸다가 찾는다.
document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("dialog").forEach((dialog) => {
        let pressedOutside = false;
        dialog.addEventListener("mousedown", (e) => {
            pressedOutside = isOutsideDialog(dialog, e);
        });
        dialog.addEventListener("click", (e) => {
            if (pressedOutside && isOutsideDialog(dialog, e)) {
                dialog.close();
            }
            pressedOutside = false;
        });
    });

    toastEl = document.getElementById("toast");

    setupInstantMenuWidget();
});

// target이 dialog 자신(= 안쪽 자식 요소가 아님)이면서 좌표가 모달 네모 영역 밖이면 바깥 클릭.
// target 조건도 같이 보는 이유: 키보드(Enter)로 버튼을 누르면 click 좌표가 (0,0)으로 들어와서
// 좌표만 보면 바깥 클릭으로 오해할 수 있음.
function isOutsideDialog(dialog, e) {
    if (e.target !== dialog) return false;
    const rect = dialog.getBoundingClientRect();
    return e.clientX < rect.left || e.clientX > rect.right
        || e.clientY < rect.top || e.clientY > rect.bottom;
}

// ===== 즉석메뉴 빠른 기록 (모든 화면에 떠있는 플로팅 위젯) =====
// 즉석주문생산 메뉴는 일일 생산/마감 사이클 대상이 아니라, 주문 들어오는 그 순간에
// 원재료(또는 보류재고)를 바로 차감해야 정확하다 - 그래서 화면 이동 없이 어디서든 탭 한 번으로 기록.
async function setupInstantMenuWidget() {
    const widgetHtml = `
        <div id="instant-menu-widget">
            <button id="instant-menu-toggle" type="button">⚡</button>
            <div id="instant-menu-panel" style="display:none;">
                <h4>즉석메뉴 빠른 기록</h4>
                <div id="instant-menu-list"><p class="hint">불러오는 중...</p></div>
            </div>
        </div>
    `;
    document.body.insertAdjacentHTML("beforeend", widgetHtml);

    const toggleBtn = document.getElementById("instant-menu-toggle");
    const panel = document.getElementById("instant-menu-panel");

    toggleBtn.addEventListener("click", async () => {
        const isOpen = panel.style.display !== "none";
        if (isOpen) {
            panel.style.display = "none";
        } else {
            panel.style.display = "block";
            await loadInstantMenuList();
        }
    });
}

async function loadInstantMenuList() {
    const listEl = document.getElementById("instant-menu-list");
    try {
        const response = await fetch("/api/products/active/instant");
        if (!response.ok) throw new Error("즉석메뉴 목록을 불러오지 못했습니다.");
        const products = await response.json();

        if (products.length === 0) {
            listEl.innerHTML = `<p class="hint">즉석주문생산으로 등록된 메뉴가 없어요.</p>`;
            return;
        }

        const withSources = await Promise.all(products.map(async (p) => {
            const res = await fetch(`/api/products/${p.id}/conversion-recipes`);
            const sources = res.ok ? await res.json() : [];
            const uniqueSources = [];
            const seen = new Set();
            for (const item of sources) {
                if (!seen.has(item.sourceProductId)) {
                    seen.add(item.sourceProductId);
                    uniqueSources.push({ sourceProductId: item.sourceProductId, sourceProductName: item.sourceProductName });
                }
            }
            return { ...p, sources: uniqueSources };
        }));

        listEl.innerHTML = withSources.map((p) => `
            <div class="instant-menu-row" data-product-id="${p.id}">
                <span class="instant-menu-name">${p.name}</span>
                <div class="instant-menu-actions">
                    <button type="button" class="btn-primary btn-sm instant-normal-btn">일반 +1</button>
                    ${p.sources.length > 0 ? `
                        <select class="instant-source-select">
                            ${p.sources.map((s) => `<option value="${s.sourceProductId}">${s.sourceProductName}</option>`).join("")}
                        </select>
                        <select class="instant-stock-type-select">
                            <option value="RESERVED">보류재고</option>
                            <option value="CURRENT">당일생산분</option>
                        </select>
                        <button type="button" class="btn-ghost btn-sm instant-conversion-btn">전환 +1</button>
                    ` : ""}
                </div>
            </div>
        `).join("");

        listEl.querySelectorAll(".instant-normal-btn").forEach((btn) => {
            btn.addEventListener("click", () => {
                const productId = Number(btn.closest(".instant-menu-row").dataset.productId);
                recordInstantProduction(productId, "NORMAL", null, null);
            });
        });
        listEl.querySelectorAll(".instant-conversion-btn").forEach((btn) => {
            btn.addEventListener("click", () => {
                const row = btn.closest(".instant-menu-row");
                const productId = Number(row.dataset.productId);
                const sourceId = Number(row.querySelector(".instant-source-select").value);
                const stockType = row.querySelector(".instant-stock-type-select").value;
                recordInstantProduction(productId, "CONVERSION", sourceId, stockType);
            });
        });
    } catch (err) {
        listEl.innerHTML = `<p class="hint">불러오기 실패</p>`;
    }
}

async function recordInstantProduction(productId, productionType, sourceProductId, sourceStockType) {
    const today = new Date();
    const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;

    try {
        const response = await fetch("/api/productions/instant", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                productId, producedQuantity: 1, productionDate: todayStr,
                productionType, sourceProductId, sourceStockType, note: "즉석메뉴 빠른 기록",
            }),
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message);
        }
        showToast("기록됐어요.");
    } catch (err) {
        showToast(err.message, true);
    }
}

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
