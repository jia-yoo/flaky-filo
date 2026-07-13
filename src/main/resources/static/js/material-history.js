const API_BASE = "/api/materials";
const toast = document.getElementById("toast");

// URL 쿼리스트링에서 ?id=5 같은 값을 꺼냄 (materials.html에서 링크로 넘어올 때 이 값을 실어서 옴)
const params = new URLSearchParams(window.location.search);
const materialId = params.get("id");

const UNIT_LABELS = { KG: "kg", G: "g", L: "L", ML: "ml", EA: "개" };

const fromDateInput = document.getElementById("from-date");
const toDateInput = document.getElementById("to-date");

function dateToString(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

document.addEventListener("DOMContentLoaded", () => {
    if (!materialId) {
        showToast("잘못된 접근이에요. 원재료 목록에서 다시 들어와주세요.", true);
        return;
    }

    // 기본값: 이번 달 1일 ~ 오늘
    const today = new Date();
    const firstDayOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);
    fromDateInput.value = dateToString(firstDayOfMonth);
    toDateInput.value = dateToString(today);

    loadMaterialSummary();
    loadHistory();
});

// 기간을 바꾸면 서버에 다시 요청해서 그 기간만큼만 받아온다 (전체를 다 받아서 자르지 않음 - 이력은 계속 쌓이는 데이터라서)
fromDateInput.addEventListener("change", loadHistory);
toDateInput.addEventListener("change", loadHistory);

// 상단에 "어떤 원재료의 이력인지 + 지금 재고" 요약 표시
async function loadMaterialSummary() {
    try {
        const response = await fetch(`${API_BASE}/${materialId}`);
        if (!response.ok) throw new Error("원재료 정보를 불러오지 못했습니다.");
        const material = await response.json();
        const unitLabel = UNIT_LABELS[material.unit] ?? material.unit;

        document.getElementById("material-name").textContent = `${material.name} 재고 변동 이력`;
        document.getElementById("material-summary").textContent =
            `현재 재고 ${material.currentStock} ${unitLabel} · 기준 ${material.minStockThreshold} ${unitLabel}`;
    } catch (err) {
        showToast(err.message, true);
    }
}

// 실제 이력 목록
async function loadHistory() {
    const tableBody = document.getElementById("history-table-body");
    const emptyState = document.getElementById("empty-state");

    if (fromDateInput.value > toDateInput.value) {
        showToast("시작일이 종료일보다 늦을 수 없어요.", true);
        return;
    }

    try {
        const response = await fetch(
            `${API_BASE}/${materialId}/transactions?from=${fromDateInput.value}&to=${toDateInput.value}`);
        if (!response.ok) throw new Error("이력을 불러오지 못했습니다.");
        const transactions = await response.json();

        tableBody.innerHTML = "";

        if (transactions.length === 0) {
            emptyState.style.display = "block";
            return;
        }
        emptyState.style.display = "none";

        for (const t of transactions) {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${formatDate(t.transactionDate)}</td>
                <td>${reasonBadge(t.reasonCode, t.reasonLabel)}</td>
                <td class="${isIncrease(t.type) ? "qty-plus" : "qty-minus"}">
                    ${isIncrease(t.type) ? "+" : "-"}${t.quantity}
                </td>
                <td>${escapeHtml(t.reason ?? "-")}</td>
            `;
            tableBody.appendChild(tr);
        }
    } catch (err) {
        showToast(err.message, true);
    }
}

// 타입별로 늘어난 건지 줄어든 건지 판단 (화면 표시용 +/- 부호 결정)
function isIncrease(type) {
    return type === "IN" || type === "ADJUST_UP";
}

// reasonCode 기준으로 배지 표시 - type(방향)보다 훨씬 구체적으로 "왜" 변동됐는지 보여줌
// 텍스트(라벨)는 서버가 이미 계산해서 내려줌(t.reasonLabel) - 여기선 색상(className)만 매핑
// (색상/CSS는 프론트엔드 고유의 관심사라 여기 남겨둠. 텍스트까지 여기서 또 매핑하면 서버와 중복됨)
function reasonBadge(reasonCode, reasonLabel) {
    const classNames = {
        PURCHASE: "badge-success",
        PRODUCTION_CONSUMPTION: "badge-gold",
        DISPOSAL: "badge-danger",
        PERIODIC_STOCKTAKE: "badge-neutral",
        AD_HOC_CORRECTION: "badge-neutral",
    };
    const className = classNames[reasonCode] ?? "badge-neutral";
    return `<span class="badge ${className}">${escapeHtml(reasonLabel)}</span>`;
}

function formatDate(isoDateString) {
    const date = new Date(isoDateString);
    return date.toLocaleDateString("ko-KR", {
        year: "numeric", month: "2-digit", day: "2-digit",
    });
}

function escapeHtml(str) {
    const div = document.createElement("div");
    div.textContent = str ?? "";
    return div.innerHTML;
}

let toastTimer;
function showToast(message, isError = false) {
    clearTimeout(toastTimer);
    toast.textContent = message;
    toast.classList.toggle("error", isError);
    toast.classList.add("show");
    toastTimer = setTimeout(() => toast.classList.remove("show"), 2500);
}