const API_BASE = "/api/products";
const MATERIAL_API = "/api/materials";
const toast = document.getElementById("toast");

const form = document.getElementById("product-form");
const formTitle = document.getElementById("form-title");
const submitBtn = document.getElementById("submit-btn");
const cancelEditBtn = document.getElementById("cancel-edit-btn");
const idInput = document.getElementById("product-id");
const tableBody = document.getElementById("product-table-body");
const emptyState = document.getElementById("empty-state");

let allMaterials = []; // 재료 검색용 캐시 (id, name, unit, unitCost 등)
const UNIT_LABELS = { KG: "kg", G: "g", L: "L", ML: "ml", EA: "개" };

document.addEventListener("DOMContentLoaded", async () => {
    await loadMaterials();
    await loadProducts();
});

// ===================== 원재료 캐시 (검색 가능한 datalist용) =====================
async function loadMaterials() {
    try {
        const response = await fetch(MATERIAL_API);
        if (!response.ok) throw new Error("원재료 목록을 불러오지 못했습니다.");
        allMaterials = await response.json();

        const datalist = document.getElementById("materials-datalist");
        datalist.innerHTML = allMaterials
            .map((m) => `<option value="${escapeHtml(m.name)}" data-id="${m.id}"></option>`)
            .join("");
    } catch (err) {
        showToast(err.message, true);
    }
}

function findMaterialByName(name) {
    return allMaterials.find((m) => m.name === name);
}

// ===================== 완제품 목록 (원가/원가율/최소판매가 포함) =====================
async function loadProducts() {
    try {
        const response = await fetch(API_BASE);
        if (!response.ok) throw new Error("완제품 목록을 불러오지 못했습니다.");
        const products = await response.json();

        // 각 완제품의 원가 정보도 같이 불러옴 (목록에서 바로 원가/원가율/최소판매가 확인하려고)
        const withCost = await Promise.all(products.map(async (p) => {
            try {
                const costRes = await fetch(`${API_BASE}/${p.id}/cost`);
                const cost = costRes.ok ? await costRes.json() : null;
                return { ...p, cost };
            } catch {
                return { ...p, cost: null };
            }
        }));

        renderTable(withCost);
    } catch (err) {
        showToast(err.message, true);
    }
}

function renderTable(products) {
    tableBody.innerHTML = "";

    if (products.length === 0) {
        emptyState.style.display = "block";
        return;
    }
    emptyState.style.display = "none";

    for (const p of products) {
        const tr = document.createElement("tr");

        const rawCostText = p.cost ? `${Math.round(p.cost.rawCost).toLocaleString()}원` : "-";
        const ratioText = p.cost && p.cost.costRatio != null ? `${(p.cost.costRatio * 100).toFixed(1)}%` : "-";
        const minPriceText = p.cost ? `${Math.round(p.cost.minPrice).toLocaleString()}원` : "-";

        tr.innerHTML = `
            <td><a href="#" onclick="openDetailDialog(${p.id}); return false;" style="color:var(--accent); font-weight:500;">${escapeHtml(p.name)}</a></td>
            <td>${escapeHtml(p.category ?? "-")}</td>
            <td>${p.price.toLocaleString()}원</td>
            <td>${rawCostText}</td>
            <td>${ratioText}</td>
            <td>${minPriceText}</td>
            <td>${p.currentStock}개</td>
            <td class="actions-cell">
                <button class="btn-ghost btn-sm" onclick="openProductionDialog(${p.id}, '${escapeHtml(p.name)}')">생산 등록</button>
                <button class="btn-ghost btn-sm" onclick="openDetailDialog(${p.id})">상세보기</button>
                <button class="btn-ghost btn-sm" onclick="startEdit(${p.id})">수정</button>
                <button class="btn-danger-text" onclick="deleteProduct(${p.id})">삭제</button>
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

// ===================== 등록 / 수정 (기본 정보) =====================
form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const payload = {
        storeId: 1,
        name: document.getElementById("name").value,
        category: document.getElementById("category").value || null,
        price: Number(document.getElementById("price").value || 0),
        yieldCount: Number(document.getElementById("yieldCount").value || 1),
        overheadRate: Number(document.getElementById("overheadRate").value || 0) / 100,
        targetCostRatio: Number(document.getElementById("targetCostRatio").value || 40) / 100,
    };

    const editingId = idInput.value;

    try {
        const response = editingId
            ? await fetch(`${API_BASE}/${editingId}`, {
                method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload),
            })
            : await fetch(API_BASE, {
                method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload),
            });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message);
        }

        showToast(editingId ? "수정됐어요." : "등록됐어요.");
        resetForm();
        loadProducts();
    } catch (err) {
        showToast(err.message, true);
    }
});

async function startEdit(id) {
    try {
        const response = await fetch(`${API_BASE}/${id}`);
        if (!response.ok) throw new Error("완제품 정보를 불러오지 못했습니다.");
        const p = await response.json();

        idInput.value = p.id;
        document.getElementById("name").value = p.name;
        document.getElementById("category").value = p.category ?? "";
        document.getElementById("price").value = p.price;
        document.getElementById("yieldCount").value = p.yieldCount;
        document.getElementById("overheadRate").value = (p.overheadRate * 100).toFixed(1);
        document.getElementById("targetCostRatio").value = (p.targetCostRatio * 100).toFixed(1);

        formTitle.textContent = "완제품 수정";
        submitBtn.textContent = "수정 완료";
        cancelEditBtn.style.display = "inline-block";
        document.getElementById("product-form-card").classList.add("editing");
        window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (err) {
        showToast(err.message, true);
    }
}

cancelEditBtn.addEventListener("click", resetForm);

function resetForm() {
    form.reset();
    idInput.value = "";
    formTitle.textContent = "완제품 등록";
    submitBtn.textContent = "등록";
    cancelEditBtn.style.display = "none";
    document.getElementById("product-form-card").classList.remove("editing");
}

async function deleteProduct(id) {
    if (!confirm("정말 삭제할까요? 레시피도 함께 삭제되고 되돌릴 수 없어요.")) return;
    try {
        const response = await fetch(`${API_BASE}/${id}`, { method: "DELETE" });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message);
        }
        showToast("삭제됐어요.");
        loadProducts();
    } catch (err) {
        showToast(err.message, true);
    }
}

// ===================== 상세 (레시피 + 원가) 모달 =====================
const recipeDialog = document.getElementById("recipe-dialog");
const recipeRowsContainer = document.getElementById("recipe-rows");
let currentProduct = null; // name/category 등 저장할 때 그대로 유지해야 하는 값들 보관
let recipeRowIdCounter = 0;

async function openDetailDialog(productId) {
    try {
        const [productRes, recipeRes] = await Promise.all([
            fetch(`${API_BASE}/${productId}`),
            fetch(`${API_BASE}/${productId}/recipe`),
        ]);
        if (!productRes.ok || !recipeRes.ok) throw new Error("정보를 불러오지 못했습니다.");

        currentProduct = await productRes.json();
        const recipe = await recipeRes.json();

        document.getElementById("recipe-dialog-title").textContent = currentProduct.name;
        document.getElementById("rd-price").value = currentProduct.price;
        document.getElementById("rd-yield").value = currentProduct.yieldCount;
        document.getElementById("rd-overhead").value = (currentProduct.overheadRate * 100).toFixed(1);
        document.getElementById("rd-target").value = (currentProduct.targetCostRatio * 100).toFixed(1);

        recipeRowsContainer.innerHTML = "";
        if (recipe.length === 0) {
            addRecipeRow();
        } else {
            for (const item of recipe) {
                addRecipeRow(item.materialId, item.materialName, item.unit, item.batchQuantity,
                        item.perUnitQuantity, item.costContribution);
            }
        }

        await refreshCostSummary(productId);
        recipeDialog.showModal();
    } catch (err) {
        showToast(err.message, true);
    }
}

document.getElementById("recipe-dialog-close-btn").addEventListener("click", () => recipeDialog.close());
document.getElementById("add-recipe-row-btn").addEventListener("click", () => addRecipeRow());

// perUnitQuantity/costContribution은 서버가 마지막으로 계산해준 값 - 저장 전까지는 참고용으로만 보여주고,
// 저장하면 최신 값으로 다시 채워짐 (실시간 재계산은 안 하고, "저장 시점 기준"으로만 정확하게 보여줌)
function addRecipeRow(materialId = "", materialName = "", unit = "", batchQuantity = "",
                       perUnitQuantity = null, costContribution = null) {
    const rowId = `recipe-row-${recipeRowIdCounter++}`;
    const row = document.createElement("div");
    row.className = "recipe-row";
    row.id = rowId;

    const unitLabel = unit ? (UNIT_LABELS[unit] ?? unit) : "";
    const perUnitText = perUnitQuantity != null ? `${perUnitQuantity} ${unitLabel}` : "-";
    const costText = costContribution != null ? `${Math.round(costContribution).toLocaleString()}원` : "-";

    row.innerHTML = `
        <input type="text" list="materials-datalist" class="recipe-material-input"
               value="${escapeHtml(materialName)}" placeholder="재료명 검색">
        <input type="hidden" class="recipe-material-id" value="${materialId}">
        <input type="number" step="0.001" class="recipe-quantity-input" placeholder="배치 필요량" value="${batchQuantity}">
        <span class="recipe-per-unit" style="flex:1; font-size:13px; color:var(--text-secondary);">${perUnitText}</span>
        <span class="recipe-cost" style="flex:1; font-size:13px;">${costText}</span>
        <button type="button" class="btn-danger-text" style="width:40px;" onclick="document.getElementById('${rowId}').remove()">삭제</button>
    `;
    recipeRowsContainer.appendChild(row);

    // 재료명 입력 시, datalist에서 고른 이름과 매칭되는 원재료를 찾아 숨은 id 필드에 채움
    const nameInput = row.querySelector(".recipe-material-input");
    const idInputEl = row.querySelector(".recipe-material-id");
    nameInput.addEventListener("input", () => {
        const matched = findMaterialByName(nameInput.value);
        idInputEl.value = matched ? matched.id : "";
    });
}

document.getElementById("save-recipe-btn").addEventListener("click", async () => {
    const rows = recipeRowsContainer.querySelectorAll(".recipe-row");
    const items = [];

    for (const row of rows) {
        const materialId = row.querySelector(".recipe-material-id").value;
        const batchQuantity = row.querySelector(".recipe-quantity-input").value;
        if (!materialId || !batchQuantity) continue;
        items.push({ materialId: Number(materialId), batchQuantity: Number(batchQuantity) });
    }

    if (items.length === 0) {
        showToast("재료를 하나 이상 입력해주세요 (재료명은 목록에서 검색해서 선택하세요).", true);
        return;
    }

    try {
        // 1. 완제품 재무 정보(판매가, 나오는 개수, 기타경비율, 목표원가율) 저장
        //    name/category/ 이 모달에서 안 건드리니 기존 값을 그대로 유지
        const productPayload = {
            name: currentProduct.name,
            category: currentProduct.category,
            price: Number(document.getElementById("rd-price").value || 0),
            yieldCount: Number(document.getElementById("rd-yield").value || 1),
            overheadRate: Number(document.getElementById("rd-overhead").value || 0) / 100,
            targetCostRatio: Number(document.getElementById("rd-target").value || 40) / 100,
        };
        const productRes = await fetch(`${API_BASE}/${currentProduct.id}`, {
            method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify(productPayload),
        });
        if (!productRes.ok) throw new Error((await productRes.json()).message);

        // 2. 레시피 저장
        const recipeRes = await fetch(`${API_BASE}/${currentProduct.id}/recipe`, {
            method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ items }),
        });
        if (!recipeRes.ok) throw new Error((await recipeRes.json()).message);
        const savedRecipe = await recipeRes.json();

        // 3. 저장된 최신 값으로 행별 원가 다시 채우기 + 요약 갱신
        recipeRowsContainer.innerHTML = "";
        for (const item of savedRecipe) {
            addRecipeRow(item.materialId, item.materialName, item.unit, item.batchQuantity,
                    item.perUnitQuantity, item.costContribution);
        }
        await refreshCostSummary(currentProduct.id);

        showToast("저장됐어요.");
        loadProducts(); // 목록의 원가/원가율/최소판매가도 갱신
    } catch (err) {
        showToast(err.message, true);
    }
});

async function refreshCostSummary(productId) {
    const response = await fetch(`${API_BASE}/${productId}/cost`);
    if (!response.ok) throw new Error("원가 계산에 실패했습니다.");
    const cost = await response.json();

    document.getElementById("cost-raw").textContent = `${Math.round(cost.rawCost).toLocaleString()}원`;
    document.getElementById("cost-overhead").textContent = `${Math.round(cost.overhead).toLocaleString()}원`;
    document.getElementById("cost-total").textContent = `${Math.round(cost.totalCost).toLocaleString()}원`;
    document.getElementById("cost-ratio").textContent =
        cost.costRatio != null ? `${(cost.costRatio * 100).toFixed(1)}%` : "판매가 미입력";
    document.getElementById("cost-min-price").textContent = `${Math.round(cost.minPrice).toLocaleString()}원`;

    const marginEl = document.getElementById("cost-margin");
    marginEl.textContent = `${Math.round(cost.margin).toLocaleString()}원`;
    marginEl.className = cost.margin >= 0 ? "cost-positive" : "cost-negative";
}

// ===================== 생산 등록 & 이력 모달 =====================
const PRODUCTION_API = "/api/productions";
const productionDialog = document.getElementById("production-dialog");
const productionForm = document.getElementById("production-form");
const productionProductId = document.getElementById("production-product-id");
const productionDateInput = document.getElementById("production-date");

function todayString() {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}

async function openProductionDialog(productId, productName) {
    productionProductId.value = productId;
    productionForm.reset();
    productionDateInput.value = todayString();
    document.getElementById("production-dialog-title").textContent = `${productName} - 생산 등록`;

    await loadProductionHistory(productId);
    productionDialog.showModal();
}

document.getElementById("production-dialog-close-btn").addEventListener("click", () => productionDialog.close());

async function loadProductionHistory(productId) {
    const container = document.getElementById("production-history-list");
    try {
        const response = await fetch(`${PRODUCTION_API}?productId=${productId}`);
        if (!response.ok) throw new Error("생산 이력을 불러오지 못했습니다.");
        const logs = await response.json();

        if (logs.length === 0) {
            container.innerHTML = `<p style="font-size:13px; color:var(--text-secondary);">생산 이력이 없어요.</p>`;
            return;
        }

        container.innerHTML = logs.map((log) => `
            <div style="display:flex; justify-content:space-between; align-items:center; padding:8px 0; border-bottom:1px solid var(--border); font-size:13px;">
                <span>
                    ${log.productionDate} · ${log.producedQuantity}개
                    ${log.note ? `<span style="color:var(--text-secondary);"> · ${escapeHtml(log.note)}</span>` : ""}
                    ${log.cancelled ? `<span class="badge badge-neutral" style="margin-left:6px;">취소됨</span>` : ""}
                </span>
                ${log.cancelled ? "" : `<button type="button" class="btn-danger-text" onclick="cancelProduction(${log.id}, ${productId})">취소</button>`}
            </div>
        `).join("");
    } catch (err) {
        showToast(err.message, true);
    }
}

productionForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const payload = {
        productId: Number(productionProductId.value),
        producedQuantity: Number(document.getElementById("production-quantity").value),
        productionDate: productionDateInput.value,
        note: document.getElementById("production-note").value || null,
    };

    try {
        const response = await fetch(PRODUCTION_API, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message);
        }

        showToast("생산이 등록되고 원재료가 자동 차감됐어요.");
        productionForm.reset();
        productionDateInput.value = todayString();
        await loadProductionHistory(payload.productId);
        loadProducts(); // 완제품 재고, 원가 등 갱신
    } catch (err) {
        showToast(err.message, true);
    }
});

async function cancelProduction(logId, productId) {
    if (!confirm("이 생산 기록을 취소할까요? 재고(완제품 감소, 원재료 복원)가 원상복구돼요.")) return;
    try {
        const response = await fetch(`${PRODUCTION_API}/${logId}/cancel`, { method: "POST" });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message);
        }
        showToast("생산 기록이 취소됐어요.");
        await loadProductionHistory(productId);
        loadProducts();
    } catch (err) {
        showToast(err.message, true);
    }
}

// ===================== 토스트 =====================
let toastTimer;
function showToast(message, isError = false) {
    clearTimeout(toastTimer);
    toast.textContent = message;
    toast.classList.toggle("error", isError);
    toast.classList.add("show");
    toastTimer = setTimeout(() => toast.classList.remove("show"), 2500);
}