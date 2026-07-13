// ===== 공통 설정 =====
const API_BASE = "/api/materials";
const SUPPLIER_API = "/api/suppliers";

const form = document.getElementById("material-form");
const formTitle = document.getElementById("form-title");
const submitBtn = document.getElementById("submit-btn");
const cancelEditBtn = document.getElementById("cancel-edit-btn");
const idInput = document.getElementById("material-id");
const supplierSelect = document.getElementById("supplier-select");
const tableBody = document.getElementById("material-table-body");
const emptyState = document.getElementById("empty-state");
const lowStockOnly = document.getElementById("low-stock-only");
const searchInput = document.getElementById("search-input");
const supplierFilter = document.getElementById("supplier-filter");
const toast = document.getElementById("toast");

let allMaterials = [];
let allSuppliers = []; // 구매처 목록도 메모리에 캐싱 (폼 드롭다운, 필터 드롭다운, 관리 모달이 다 같이 씀)

document.addEventListener("DOMContentLoaded", async () => {
    await loadSuppliers();
    await loadMaterials();
});

function todayString() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

// ===================== 구매처(Supplier) =====================

async function loadSuppliers() {
    try {
        const response = await fetch(SUPPLIER_API);
        if (!response.ok) throw new Error("구매처 목록을 불러오지 못했습니다.");
        allSuppliers = await response.json();
        renderSupplierOptionsInForm();
        renderSupplierFilterOptions();
        renderSupplierManageList();
    } catch (err) {
        showToast(err.message, true);
    }
}

// 원재료 등록/수정 폼 안의 구매처 드롭다운
function renderSupplierOptionsInForm() {
    const previous = supplierSelect.value;
    supplierSelect.innerHTML = '<option value="">선택 안 함</option>';
    for (const s of allSuppliers) {
        const option = document.createElement("option");
        option.value = s.id;
        option.textContent = s.name;
        supplierSelect.appendChild(option);
    }
    supplierSelect.value = previous;
}

// 목록 위 검색 필터용 구매처 드롭다운
function renderSupplierFilterOptions() {
    const previous = supplierFilter.value;
    supplierFilter.innerHTML = '<option value="">구매처 전체</option>';
    for (const s of allSuppliers) {
        const option = document.createElement("option");
        option.value = s.id;
        option.textContent = s.name;
        supplierFilter.appendChild(option);
    }
    supplierFilter.value = previous;
}

// 구매처 관리 모달 안의 목록 (삭제 버튼 포함)
function renderSupplierManageList() {
    const container = document.getElementById("supplier-list");
    if (allSuppliers.length === 0) {
        container.innerHTML = `<p style="font-size:13px; color:var(--text-secondary);">등록된 구매처가 없어요.</p>`;
        return;
    }
    container.innerHTML = allSuppliers.map((s) => `
        <div style="display:flex; justify-content:space-between; align-items:center; padding:6px 0; border-bottom:1px solid var(--border); font-size:13px;">
            <span>${escapeHtml(s.name)} ${s.note ? `<span style="color:var(--text-secondary);">· ${escapeHtml(s.note)}</span>` : ""}</span>
            <button type="button" class="btn-danger-text" onclick="deleteSupplier(${s.id})">삭제</button>
        </div>
    `).join("");
}

document.getElementById("manage-supplier-btn").addEventListener("click", () => {
    document.getElementById("supplier-dialog").showModal();
});
document.getElementById("supplier-dialog-close-btn").addEventListener("click", () => {
    document.getElementById("supplier-dialog").close();
});

document.getElementById("supplier-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = {
        name: document.getElementById("new-supplier-name").value,
        note: document.getElementById("new-supplier-note").value || null,
    };
    try {
        const response = await fetch(SUPPLIER_API, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message);
        }
        document.getElementById("supplier-form").reset();
        await loadSuppliers();
        showToast("구매처가 추가됐어요.");
    } catch (err) {
        showToast(err.message, true);
    }
});

async function deleteSupplier(id) {
    if (!confirm("이 구매처를 삭제할까요? 이 구매처를 쓰는 원재료가 있으면 삭제가 안 될 수 있어요.")) return;
    try {
        const response = await fetch(`${SUPPLIER_API}/${id}`, { method: "DELETE" });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message ?? "삭제할 수 없어요. 이 구매처를 참조하는 원재료가 있는지 확인해주세요.");
        }
        await loadSuppliers();
        showToast("구매처가 삭제됐어요.");
    } catch (err) {
        showToast(err.message, true);
    }
}

// ===================== 원재료(Material) =====================

async function loadMaterials() {
    try {
        const response = await fetch(API_BASE);
        if (!response.ok) throw new Error("목록을 불러오지 못했습니다.");
        allMaterials = await response.json();
        applyFilters();
    } catch (err) {
        showToast(err.message, true);
    }
}

function applyFilters() {
    const keyword = searchInput.value.trim().toLowerCase();
    const supplierId = supplierFilter.value;

    const filtered = allMaterials.filter((m) => {
        const matchesKeyword = !keyword || m.name.toLowerCase().includes(keyword);
        const matchesSupplier = !supplierId || String(m.supplierId ?? "") === supplierId;
        const matchesLowStock = !lowStockOnly.checked || m.belowThreshold;
        return matchesKeyword && matchesSupplier && matchesLowStock;
    });

    renderTable(filtered);
    document.getElementById("result-count").textContent = `${filtered.length}건 표시 중 (전체 ${allMaterials.length}건)`;
}

searchInput.addEventListener("input", debounce(applyFilters, 200));
supplierFilter.addEventListener("change", applyFilters);
lowStockOnly.addEventListener("change", applyFilters);

function debounce(fn, delay) {
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => fn(...args), delay);
    };
}

const UNIT_LABELS = { KG: "kg", G: "g", L: "L", ML: "ml", EA: "개" };

function renderTable(materials) {
    tableBody.innerHTML = "";

    if (materials.length === 0) {
        emptyState.style.display = "block";
        return;
    }
    emptyState.style.display = "none";

    for (const m of materials) {
        const tr = document.createElement("tr");
        const unitLabel = UNIT_LABELS[m.unit] ?? m.unit;

        const statusBadge = m.belowThreshold
            ? `<span class="badge badge-danger">부족</span>`
            : `<span class="badge badge-success">정상</span>`;

        tr.innerHTML = `
            <td>${escapeHtml(m.name)}</td>
            <td>${m.currentStock} ${unitLabel}</td>
            <td>${m.minStockThreshold} ${unitLabel}</td>
            <td>${Number(m.unitCost).toLocaleString()}원</td>
            <td>${escapeHtml(m.supplierName ?? "-")}</td>
            <td>${escapeHtml(m.note ?? "-")}</td>
            <td>${statusBadge}</td>
            <td class="actions-cell">
                <button class="btn-ghost btn-sm" onclick="openStockDialog(${m.id})">입고·폐기</button>
                <button class="btn-ghost btn-sm" onclick="openStocktakeDialog(${m.id}, '${escapeHtml(m.name)}', ${m.currentStock}, '${unitLabel}')">실사</button>
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

form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const payload = {
        storeId: 1,
        name: document.getElementById("name").value,
        unit: document.getElementById("unit").value,
        minStockThreshold: Number(document.getElementById("minStockThreshold").value),
        unitCost: Number(document.getElementById("unitCost").value || 0),
        supplierId: supplierSelect.value ? Number(supplierSelect.value) : null,
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
        supplierSelect.value = m.supplierId ?? "";
        document.getElementById("note").value = m.note ?? "";

        formTitle.textContent = "원재료 수정";
        submitBtn.textContent = "수정 완료";
        cancelEditBtn.style.display = "inline-block";
        document.getElementById("material-form-card").classList.add("editing");
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
    document.getElementById("material-form-card").classList.remove("editing");
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
const stockDialog = document.getElementById("stock-dialog");
const stockForm = document.getElementById("stock-form");
const stockMaterialId = document.getElementById("stock-material-id");
const stockDateInput = document.getElementById("stock-date");
let selectedStockType = "IN";

function openStockDialog(materialId) {
    stockMaterialId.value = materialId;
    stockForm.reset();
    stockDateInput.value = todayString();
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
        transactionDate: stockDateInput.value,
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
const stocktakeDialog = document.getElementById("stocktake-dialog");
const stocktakeForm = document.getElementById("stocktake-form");
const stocktakeMaterialId = document.getElementById("stocktake-material-id");
const stocktakeDateInput = document.getElementById("stocktake-date");
const stocktakeCurrentInfo = document.getElementById("stocktake-current-info");
let selectedIsPeriodic = false; // 기본값: 수시보정 실사

function openStocktakeDialog(materialId, name, currentStock, unitLabel) {
    stocktakeMaterialId.value = materialId;
    stocktakeForm.reset();
    stocktakeDateInput.value = todayString();
    stocktakeCurrentInfo.textContent = `${name} · 현재 시스템 재고: ${currentStock} ${unitLabel}`;
    selectedIsPeriodic = false;
    updateStocktakeTypeToggleUI();
    stocktakeDialog.showModal();
}

document.querySelectorAll(".stocktake-type-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
        selectedIsPeriodic = btn.dataset.periodic === "true";
        updateStocktakeTypeToggleUI();
    });
});

function updateStocktakeTypeToggleUI() {
    document.querySelectorAll(".stocktake-type-btn").forEach((btn) => {
        btn.classList.toggle("active", (btn.dataset.periodic === "true") === selectedIsPeriodic);
    });
}

document.getElementById("stocktake-cancel-btn").addEventListener("click", () => stocktakeDialog.close());

stocktakeForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const payload = {
        actualStock: Number(document.getElementById("stocktake-actual").value),
        reason: document.getElementById("stocktake-reason").value || null,
        transactionDate: stocktakeDateInput.value,
        isPeriodic: selectedIsPeriodic,
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