const API_BASE = "/api/products";
const PRODUCTION_API = "/api/productions";

let activeProducts = []; // 판매중인 완제품 목록 캐시
let todayLogsByProduct = {}; // { productId: [선택된 날짜에 이미 등록된(취소 안 된) 생산 기록들] }
let validSourcesByTarget = {}; // { targetProductId: [{sourceProductId, sourceProductName}] } - 실제 전환 레시피가 등록된 조합만
let conversionRowCounter = 0;

document.addEventListener("DOMContentLoaded", async () => {
    document.getElementById("op-date").value = todayString();
    await refreshAll();
});

document.getElementById("op-date").addEventListener("change", refreshAll);

async function refreshAll() {
    await loadActiveProducts();
    await loadValidConversionSources();
    await loadTodayProductionLogs();
    renderProductionRows();
    renderClosingRows();
}

function todayString() {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}

async function loadActiveProducts() {
    try {
        const response = await fetch(`${API_BASE}/active`);
        if (!response.ok) throw new Error("판매중인 완제품 목록을 불러오지 못했습니다.");
        activeProducts = await response.json();
    } catch (err) {
        showToast(err.message, true);
    }
}

// 각 완제품마다 "실제로 전환 레시피가 등록된 원본"만 걸러서 캐시 - 아무 완제품이나 원본으로 뜨는 문제 방지
async function loadValidConversionSources() {
    validSourcesByTarget = {};
    try {
        await Promise.all(activeProducts.map(async (p) => {
            const response = await fetch(`${API_BASE}/${p.id}/conversion-recipes`);
            if (!response.ok) {
                validSourcesByTarget[p.id] = [];
                return;
            }
            const items = await response.json();
            const uniqueSources = [];
            const seen = new Set();
            for (const item of items) {
                if (!seen.has(item.sourceProductId)) {
                    seen.add(item.sourceProductId);
                    uniqueSources.push({ sourceProductId: item.sourceProductId, sourceProductName: item.sourceProductName });
                }
            }
            validSourcesByTarget[p.id] = uniqueSources;
        }));
    } catch (err) {
        showToast("전환 레시피 정보를 불러오지 못했습니다.", true);
    }
}

// 선택된 날짜에 각 완제품별로 이미 등록된(취소 안 된) 생산 기록을 불러옴
async function loadTodayProductionLogs() {
    const selectedDate = document.getElementById("op-date").value;
    todayLogsByProduct = {};

    try {
        const results = await Promise.all(activeProducts.map(async (p) => {
            const response = await fetch(`${PRODUCTION_API}?productId=${p.id}`);
            if (!response.ok) return { productId: p.id, logs: [] };
            const logs = await response.json();
            const todayLogs = logs.filter((log) => log.productionDate === selectedDate && !log.cancelled);
            return { productId: p.id, logs: todayLogs };
        }));

        for (const result of results) {
            todayLogsByProduct[result.productId] = result.logs;
        }
    } catch (err) {
        showToast("오늘 등록된 생산 내역을 불러오지 못했습니다.", true);
    }
}

function escapeHtml(str) {
    const div = document.createElement("div");
    div.textContent = str ?? "";
    return div.innerHTML;
}

// ===================== 오늘 생산 등록 =====================
function renderProductionRows() {
    const container = document.getElementById("production-rows-daily");

    if (activeProducts.length === 0) {
        container.innerHTML = `<p style="font-size:13px; color:var(--text-secondary);">판매중인 완제품이 없어요. 완제품 관리에서 "지금 판매중인 메뉴"를 체크해주세요.</p>`;
        return;
    }

    container.innerHTML = activeProducts.map((p) => {
        const todayLogs = todayLogsByProduct[p.id] ?? [];
        const normalLogs = todayLogs.filter((l) => l.productionType === "NORMAL");
        const conversionLogs = todayLogs.filter((l) => l.productionType === "CONVERSION");

        const normalQtySum = normalLogs.reduce((sum, l) => sum + l.producedQuantity, 0);
        const normalLogIds = normalLogs.map((l) => l.id).join(",");

        const todayTotal = todayLogs.reduce((sum, l) => sum + l.producedQuantity, 0);
        const carryOver = Math.max(0, p.currentStock - todayTotal); // 오늘 등록분을 뺀 나머지 = 어제까지 이월된 재고 (근사치)

        const sources = validSourcesByTarget[p.id] ?? [];
        const addBtnHtml = sources.length > 0
            ? `<button type="button" class="btn-ghost btn-sm add-conversion-sub-row" style="flex:0 0 auto;">+ 전환분 추가</button>`
            : "";

        return `
            <div class="daily-row" data-product-id="${p.id}" data-normal-log-ids="${normalLogIds}" style="padding:10px 0; border-bottom:1px solid var(--border);">
                <div style="display:flex; align-items:center; gap:10px; flex-wrap:wrap;">
                    <span style="flex:2; font-weight:500;">${escapeHtml(p.name)}</span>
                    <span style="font-size:12px; color:var(--text-secondary);">총재고 ${p.currentStock}개 · 이월 ${carryOver}개 · 보류 ${p.reservedStock ?? 0}개</span>
                    <input type="number" min="0" class="daily-normal-qty" placeholder="오늘 생산량" style="width:110px;" value="${normalQtySum || ""}">
                    ${addBtnHtml}
                </div>
                <div class="conversion-sub-rows" style="margin-left:20px;"></div>
            </div>
        `;
    }).join("");

    // 기존에 등록된 전환분은 자동으로 작은 줄로 미리 채워둠
    container.querySelectorAll(".daily-row").forEach((row) => {
        const productId = Number(row.dataset.productId);
        const todayLogs = (todayLogsByProduct[productId] ?? []).filter((l) => l.productionType === "CONVERSION");
        const subContainer = row.querySelector(".conversion-sub-rows");
        for (const log of todayLogs) {
            addConversionSubRow(subContainer, productId, log.sourceProductId, log.sourceProductName, log.producedQuantity, log.id);
        }
    });

    container.querySelectorAll(".add-conversion-sub-row").forEach((btn) => {
        btn.addEventListener("click", () => {
            const row = btn.closest(".daily-row");
            const productId = Number(row.dataset.productId);
            addConversionSubRow(row.querySelector(".conversion-sub-rows"), productId);
        });
    });
}

// 전환분 한 줄 추가. existingSourceId/existingSourceName/existingQty/existingLogId가 있으면
// "이미 등록된 전환분"을 화면에 보여주는 것 (수정 가능), 없으면 새로 추가하는 빈 줄.
function addConversionSubRow(subRowContainer, targetProductId, existingSourceId = "", existingSourceName = "",
                              existingQty = "", existingLogId = "") {
    const rowId = `conv-sub-${conversionRowCounter++}`;
    const sources = validSourcesByTarget[targetProductId] ?? [];

    const div = document.createElement("div");
    div.id = rowId;
    div.dataset.existingLogId = existingLogId;
    div.style.cssText = "display:flex; align-items:center; gap:8px; margin-top:6px; font-size:13px;";
    div.innerHTML = `
        <span style="color:var(--text-secondary); flex:0 0 auto;">ㄴ 전환 (원본:</span>
        <select class="daily-conversion-source" style="flex:1; max-width:200px;">
            ${sources.map((s) => `<option value="${s.sourceProductId}" ${String(s.sourceProductId) === String(existingSourceId) ? "selected" : ""}>${escapeHtml(s.sourceProductName)}</option>`).join("")}
        </select>
        <span style="color:var(--text-secondary); flex:0 0 auto;">)</span>
        <input type="number" min="0" class="daily-conversion-qty" placeholder="수량" style="width:90px;" value="${existingQty}">
        <button type="button" class="btn-danger-text" onclick="document.getElementById('${rowId}').remove()">삭제</button>
    `;
    subRowContainer.appendChild(div);
}

document.getElementById("save-daily-production-btn").addEventListener("click", async () => {
    const productionDate = document.getElementById("op-date").value;
    const rows = document.querySelectorAll("#production-rows-daily .daily-row");
    const cancelIds = [];
    const registerRequests = [];

    for (const row of rows) {
        const productId = Number(row.dataset.productId);

        // ----- 일반 생산량: 기존 값이랑 다르면 기존 것 취소하고 새로 등록 -----
        const newNormalQty = Number(row.querySelector(".daily-normal-qty").value || 0);
        const existingNormalIds = row.dataset.normalLogIds
            ? row.dataset.normalLogIds.split(",").map(Number)
            : [];
        const existingNormalQty = (todayLogsByProduct[productId] ?? [])
            .filter((l) => l.productionType === "NORMAL")
            .reduce((sum, l) => sum + l.producedQuantity, 0);

        if (newNormalQty !== existingNormalQty) {
            cancelIds.push(...existingNormalIds);
            if (newNormalQty > 0) {
                registerRequests.push({ productId, producedQuantity: newNormalQty, productionDate, productionType: "NORMAL", sourceProductId: null, note: null });
            }
        }

        // ----- 전환분 각 줄: 기존 값이랑 다르면 기존 것 취소하고 새로 등록 -----
        const subRows = row.querySelectorAll(".conversion-sub-rows > div");
        for (const sub of subRows) {
            const sourceId = Number(sub.querySelector(".daily-conversion-source").value);
            const newQty = Number(sub.querySelector(".daily-conversion-qty").value || 0);
            const existingLogId = sub.dataset.existingLogId ? Number(sub.dataset.existingLogId) : null;
            const existingLog = (todayLogsByProduct[productId] ?? []).find((l) => l.id === existingLogId);
            const existingQty = existingLog ? existingLog.producedQuantity : 0;

            if (!sourceId) continue;

            if (newQty !== existingQty) {
                if (existingLogId) cancelIds.push(existingLogId);
                if (newQty > 0) {
                    registerRequests.push({ productId, producedQuantity: newQty, productionDate, productionType: "CONVERSION", sourceProductId: sourceId, note: null });
                }
            }
        }
    }

    if (cancelIds.length === 0 && registerRequests.length === 0) {
        showToast("바뀐 내용이 없어요.");
        return;
    }

    try {
        for (const logId of cancelIds) {
            const response = await fetch(`${PRODUCTION_API}/${logId}/cancel`, { method: "POST" });
            if (!response.ok) {
                const error = await response.json();
                throw new Error(`기존 기록 취소 실패: ${error.message}`);
            }
        }
        for (const payload of registerRequests) {
            const response = await fetch(PRODUCTION_API, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload),
            });
            if (!response.ok) {
                const error = await response.json();
                throw new Error(`${payload.productId}번 완제품 생산 등록 실패: ${error.message}`);
            }
        }
        showToast("생산 등록 내용이 저장됐어요.");
        await refreshAll();
    } catch (err) {
        showToast(err.message, true);
    }
});

// ===================== 마감 처리 =====================
function renderClosingRows() {
    const container = document.getElementById("closing-rows-daily");

    if (activeProducts.length === 0) {
        container.innerHTML = "";
        return;
    }

    container.innerHTML = activeProducts.map((p) => {
        const defaultAction = p.autoDisposeIfUnsold ? "waste" : "carry";
        const defaultQty = defaultAction === "waste" ? p.currentStock : "";
        return `
            <div class="daily-row" data-product-id="${p.id}" data-current-stock="${p.currentStock}" style="padding:10px 0; border-bottom:1px solid var(--border);">
                <div style="display:flex; align-items:center; gap:10px;">
                    <span style="flex:2; font-weight:500;">${escapeHtml(p.name)}</span>
                    <span style="flex:1; font-size:12px; color:var(--text-secondary);">남은 ${p.currentStock}개</span>
                    <select class="closing-main-action" style="flex:1;">
                        <option value="carry" ${defaultAction === "carry" ? "selected" : ""}>이월</option>
                        <option value="waste" ${defaultAction === "waste" ? "selected" : ""}>폐기</option>
                        <option value="reserve">보류</option>
                    </select>
                    <input type="number" min="0" class="closing-main-qty" style="flex:1;"
                           value="${defaultQty}" ${defaultAction === "carry" ? "disabled" : ""}>
                    <button type="button" class="btn-ghost btn-sm add-closing-action" style="flex:0 0 auto;">+ 처리 추가</button>
                </div>
                <div class="closing-action-rows" style="margin-left:20px;"></div>
            </div>
        `;
    }).join("");

    container.querySelectorAll(".closing-main-action").forEach((select) => {
        select.addEventListener("change", () => {
            const row = select.closest(".daily-row");
            const qtyInput = row.querySelector(".closing-main-qty");
            const currentStock = Number(row.dataset.currentStock);

            if (select.value === "carry") {
                qtyInput.value = "";
                qtyInput.disabled = true;
            } else {
                qtyInput.disabled = false;
                if (!qtyInput.value) qtyInput.value = currentStock;
            }
        });
    });

    container.querySelectorAll(".add-closing-action").forEach((btn) => {
        btn.addEventListener("click", () => {
            const row = btn.closest(".daily-row");
            addClosingActionRow(row.querySelector(".closing-action-rows"));
        });
    });
}

let closingRowCounter = 0;

function addClosingActionRow(container, action = "waste", quantity = "") {
    const rowId = `closing-action-${closingRowCounter++}`;
    const div = document.createElement("div");
    div.id = rowId;
    div.style.cssText = "display:flex; align-items:center; gap:8px; margin-top:6px;";
    div.innerHTML = `
        <select class="closing-action-type" style="flex:1;">
            <option value="waste" ${action === "waste" ? "selected" : ""}>폐기</option>
            <option value="reserve" ${action === "reserve" ? "selected" : ""}>보류</option>
        </select>
        <input type="number" min="0" class="closing-action-qty" placeholder="수량" style="flex:1;" value="${quantity}">
        <button type="button" class="btn-danger-text" onclick="document.getElementById('${rowId}').remove()">삭제</button>
    `;
    container.appendChild(div);
}

document.getElementById("save-daily-closing-btn").addEventListener("click", async () => {
    const rows = document.querySelectorAll("#closing-rows-daily .daily-row");
    const actions = [];

    for (const row of rows) {
        const productId = Number(row.dataset.productId);
        const currentStock = Number(row.dataset.currentStock);
        const productName = row.querySelector("span").textContent;

        let usedQuantity = 0;

        const mainAction = row.querySelector(".closing-main-action").value;
        const mainQty = Number(row.querySelector(".closing-main-qty").value || 0);
        if (mainAction !== "carry" && mainQty > 0) {
            usedQuantity += mainQty;
            actions.push({ productId, action: mainAction, quantity: mainQty });
        }

        const extraRows = row.querySelectorAll(".closing-action-rows > div");
        for (const extraRow of extraRows) {
            const action = extraRow.querySelector(".closing-action-type").value;
            const quantity = Number(extraRow.querySelector(".closing-action-qty").value || 0);
            if (quantity <= 0) continue;
            usedQuantity += quantity;
            actions.push({ productId, action, quantity });
        }

        if (usedQuantity > currentStock) {
            showToast(`${productName} - 처리 수량 합계(${usedQuantity}개)가 남은 재고(${currentStock}개)보다 많아요.`, true);
            return;
        }
    }

    if (actions.length === 0) {
        showToast("처리할 마감 항목이 없어요 (전부 이월).");
        return;
    }

    try {
        for (const item of actions) {
            const endpoint = item.action === "waste" ? "dispose-stock" : "reserve-stock";
            const response = await fetch(`${API_BASE}/${item.productId}/${endpoint}`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ quantity: item.quantity }),
            });
            if (!response.ok) {
                const error = await response.json();
                throw new Error(`${item.productId}번 완제품 마감 처리 실패: ${error.message}`);
            }
        }
        showToast(`마감 처리 ${actions.length}건이 완료됐어요.`);
        await refreshAll();
    } catch (err) {
        showToast(err.message, true);
    }
});