// ===== 공통 설정 =====
const API_BASE = "/api/materials";

const form = document.getElementById("material-form");
const formTitle = document.getElementById("form-title");
const submitBtn = document.getElementById("submit-btn");
const cancelEditBtn = document.getElementById("cancel-edit-btn");
const idInput = document.getElementById("material-id");
const tableBody = document.getElementById("material-table-body");
const emptyState = document.getElementById("empty-state");
const lowStockOnly = document.getElementById("low-stock-only");
const toast = document.getElementById("toast");

// 입고/폐기 모달 관련 요소
const stockDialog = document.getElementById("stock-dialog");
const stockForm = document.getElementById("stock-form");
const stockMaterialId = document.getElementById("stock-material-id");
const stockDateInput = document.getElementById("stock-date");
let selectedStockType = "IN";

// 실사 모달 관련 요소
const stocktakeDialog = document.getElementById("stocktake-dialog");
const stocktakeForm = document.getElementById("stocktake-form");
const stocktakeMaterialId = document.getElementById("stocktake-material-id");
const stocktakeDateInput = document.getElementById("stocktake-date");
const stocktakeCurrentInfo = document.getElementById("stocktake-current-info");

document.addEventListener("DOMContentLoaded", loadMaterials);

// ===== 오늘 날짜를 <input type="date"> 기본값으로 넣어주는 헬퍼 =====
// input[type=date]는 "YYYY-MM-DD" 형식 문자열을 값으로 받는다.
function todayString() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

// ===== 목록 조회 및 렌더링 =====
async function loadMaterials() {
    const url = lowStockOnly.checked ? `${API_BASE}/low-stock` : API_BASE;
    try {
        const response = await fetch(url);
        if (!response.ok) throw new Error("목록을 불러오지 못했습니다.");
        const materials = await response.json();
        renderTable(materials);
    } catch (err) {
        showToast(err.message, true);
    }
}

lowStockOnly.addEventListener("change", loadMaterials);

function renderTable(materials) {
    tableBody.innerHTML = "";

    if (materials.length === 0) {
        emptyState.style.display = "block";
        return;
    }
    emptyState.style.display = "none";

    for (const m of materials) {
        const tr = document.createElement("tr");

        const statusBadge = m.belowThreshold
            ? `<span class="badge badge-danger">부족</span>`
            : `<span class="badge badge-success">정상</span>`;

        tr.innerHTML = `
            <td>${escapeHtml(m.name)}</td>
            <td>${m.currentStock} ${escapeHtml(m.unit)}</td>
            <td>${m.minStockThreshold} ${escapeHtml(m.unit)}</td>
            <td>${Number(m.unitCost).toLocaleString()}원</td>
            <td>${escapeHtml(m.supplier ?? "-")}</td>
            <td>${statusBadge}</td>
            <td class="actions-cell">
                <button class="btn-ghost btn-sm" onclick="openStockDialog(${m.id})">입고·폐기</button>
                <button class="btn-ghost btn-sm" onclick="openStocktakeDialog(${m.id}, '${escapeHtml(m.name)}', ${m.currentStock}, '${escapeHtml(m.unit)}')">실사</button>
                <button class="btn-ghost btn-sm" onclick="location.href='material-history.html?id=${m.id}'">이력</button>
                <button class="btn-ghost btn-sm" onclick="startEdit(${m.id})">수정</button>
                <button class="btn-danger-text" onclick="deleteMaterial(${m.id})">삭제</button>
            </td>
        `;
        tableBody.appendChild(tr);
    }
}

function escapeHtml(str) {
    const div = document.createElement("div");
    div.textContent = str ?? "";
    return div.innerHTML;
}

// ===== 등록 / 수정 폼 제출 =====
form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const payload = {
        storeId: 1,
        name: document.getElementById("name").value,
        unit: document.getElementById("unit").value,
        minStockThreshold: Number(document.getElementById("minStockThreshold").value),
        unitCost: Number(document.getElementById("unitCost").value || 0),
        supplier: document.getElementById("supplier").value || null,
        note: document.getElementById("note").value || null,
    };

    const editingId = idInput.value;

    try {
        let response;
        if (editingId) {
            response = await fetch(`${API_BASE}/${editingId}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload),
            });
        } else {
            response = await fetch(API_BASE, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload),
            });
        }

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message);
        }

        showToast(editingId ? "수정됐어요." : "등록됐어요.");
        resetForm();
        loadMaterials();
    } catch (err) {
        showToast(err.message, true);
    }
});

async function startEdit(id) {
    try {
        const response = await fetch(`${API_BASE}/${id}`);
        if (!response.ok) throw new Error("원재료 정보를 불러오지 못했습니다.");
        const m = await response.json();

        idInput.value = m.id;
        document.getElementById("name").value = m.name;
        document.getElementById("unit").value = m.unit;
        document.getElementById("minStockThreshold").value = m.minStockThreshold;
        document.getElementById("unitCost").value = m.unitCost;
        document.getElementById("supplier").value = m.supplier ?? "";
        document.getElementById("note").value = m.note ?? "";

        formTitle.textContent = "원재료 수정";
        submitBtn.textContent = "수정 완료";
        cancelEditBtn.style.display = "inline-block";
        window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (err) {
        showToast(err.message, true);
    }
}

cancelEditBtn.addEventListener("click", resetForm);

function resetForm() {
    form.reset();
    idInput.value = "";
    formTitle.textContent = "원재료 등록";
    submitBtn.textContent = "등록";
    cancelEditBtn.style.display = "none";
}

async function deleteMaterial(id) {
    if (!confirm("정말 삭제할까요? 되돌릴 수 없어요.")) return;
    try {
        const response = await fetch(`${API_BASE}/${id}`, { method: "DELETE" });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message);
        }
        showToast("삭제됐어요.");
        loadMaterials();
    } catch (err) {
        showToast(err.message, true);
    }
}

// ===== 입고 / 폐기 모달 =====
function openStockDialog(materialId) {
    stockMaterialId.value = materialId;
    stockForm.reset();
    stockDateInput.value = todayString(); // 기본값: 오늘. 클릭하면 브라우저 기본 달력 UI가 뜸
    selectedStockType = "IN";
    updateTypeToggleUI();
    stockDialog.showModal();
}

document.getElementById("stock-cancel-btn").addEventListener("click", () => stockDialog.close());

document.querySelectorAll(".type-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
        selectedStockType = btn.dataset.type;
        updateTypeToggleUI();
    });
});

function updateTypeToggleUI() {
    document.querySelectorAll(".type-btn").forEach((btn) => {
        btn.classList.toggle("active", btn.dataset.type === selectedStockType);
    });
}

stockForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const payload = {
        type: selectedStockType,
        quantity: Number(document.getElementById("stock-quantity").value),
        reason: document.getElementById("stock-reason").value || null,
        transactionDate: stockDateInput.value, // "YYYY-MM-DD" 문자열 그대로 보내면 서버 LocalDate가 파싱함
    };

    try {
        const response = await fetch(`${API_BASE}/${stockMaterialId.value}/stock`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message);
        }

        showToast("재고가 반영됐어요.");
        stockDialog.close();
        loadMaterials();
    } catch (err) {
        showToast(err.message, true);
    }
});

// ===== 실사 모달 =====
function openStocktakeDialog(materialId, name, currentStock, unit) {
    stocktakeMaterialId.value = materialId;
    stocktakeForm.reset();
    stocktakeDateInput.value = todayString();
    stocktakeCurrentInfo.textContent = `${name} · 현재 시스템 재고: ${currentStock} ${unit}`;
    stocktakeDialog.showModal();
}

document.getElementById("stocktake-cancel-btn").addEventListener("click", () => stocktakeDialog.close());

stocktakeForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const payload = {
        actualStock: Number(document.getElementById("stocktake-actual").value),
        reason: document.getElementById("stocktake-reason").value || null,
        transactionDate: stocktakeDateInput.value,
    };

    try {
        const response = await fetch(`${API_BASE}/${stocktakeMaterialId.value}/stocktake`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message);
        }

        showToast("실사 결과가 반영됐어요.");
        stocktakeDialog.close();
        loadMaterials();
    } catch (err) {
        showToast(err.message, true);
    }
});

// ===== 토스트 알림 =====
let toastTimer;
function showToast(message, isError = false) {
    clearTimeout(toastTimer);
    toast.textContent = message;
    toast.classList.toggle("error", isError);
    toast.classList.add("show");
    toastTimer = setTimeout(() => toast.classList.remove("show"), 2500);
}