const PRODUCT_API = "/api/products";
const ORDER_API = "/api/orders";

let allProducts = []; // 완제품 검색용 캐시
let itemRowCounter = 0;
let subRowCounter = 0;

const CHANNEL_LABELS = { STORE: "매장방문", NAVER: "네이버", COUPANG: "쿠팡", BAEMIN: "배민" };

document.addEventListener("DOMContentLoaded", async () => {
    setDefaultDateRange();
    await loadProducts();
    addOrderItemRow(); // 처음엔 빈 항목 하나
    await loadOrders();
});

function todayString() {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}

function setDefaultDateRange() {
    const today = new Date();
    const firstDayOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);
    document.getElementById("order-date").value = todayString();
    document.getElementById("order-from-date").value =
        `${firstDayOfMonth.getFullYear()}-${String(firstDayOfMonth.getMonth() + 1).padStart(2, "0")}-01`;
    document.getElementById("order-to-date").value = todayString();
}

document.getElementById("order-from-date").addEventListener("change", loadOrders);
document.getElementById("order-to-date").addEventListener("change", loadOrders);

function escapeHtml(str) {
    const div = document.createElement("div");
    div.textContent = str ?? "";
    return div.innerHTML;
}

async function loadProducts() {
    try {
        const response = await fetch(`${PRODUCT_API}/active`);
        if (!response.ok) throw new Error("완제품 목록을 불러오지 못했습니다.");
        allProducts = await response.json();

        const datalist = document.getElementById("order-products-datalist");
        datalist.innerHTML = allProducts.map((p) => `<option value="${escapeHtml(p.name)}" data-id="${p.id}"></option>`).join("");
    } catch (err) {
        showToast(err.message, true);
    }
}

function findProductByName(name) {
    return allProducts.find((p) => p.name === name);
}

// ===================== 주문 항목 입력 (행 확장 방식) =====================
document.getElementById("add-order-item-btn").addEventListener("click", () => addOrderItemRow());

function addOrderItemRow() {
    const rowId = `order-item-${itemRowCounter++}`;
    const row = document.createElement("div");
    row.id = rowId;
    row.className = "recipe-row";
    row.style.cssText = "display:flex; flex-direction:column; gap:6px; padding:10px 0; border-bottom:1px solid var(--border);";
    row.innerHTML = `
        <div style="display:flex; align-items:center; gap:8px;">
            <input type="text" list="order-products-datalist" class="order-product-input" placeholder="완제품명 검색" style="flex:2;">
            <input type="hidden" class="order-product-id">
            <input type="number" min="1" class="order-quantity-input" placeholder="수량" style="flex:1;">
            <button type="button" class="btn-ghost btn-sm add-substitution-btn">+ 대체 추가</button>
            <button type="button" class="btn-danger-text" onclick="document.getElementById('${rowId}').remove()">삭제</button>
        </div>
        <div class="substitution-rows" style="margin-left:20px;"></div>
    `;
    document.getElementById("order-item-rows").appendChild(row);

    const nameInput = row.querySelector(".order-product-input");
    const idInput = row.querySelector(".order-product-id");
    nameInput.addEventListener("input", () => {
        const matched = findProductByName(nameInput.value);
        idInput.value = matched ? matched.id : "";
    });

    row.querySelector(".add-substitution-btn").addEventListener("click", () => {
        addSubstitutionRow(row.querySelector(".substitution-rows"));
    });
}

function addSubstitutionRow(container) {
    const rowId = `sub-row-${subRowCounter++}`;
    const div = document.createElement("div");
    div.id = rowId;
    div.style.cssText = "display:flex; align-items:center; gap:8px; margin-top:6px; font-size:13px;";
    div.innerHTML = `
        <span style="color:var(--text-secondary); flex:0 0 auto;">ㄴ 대체:</span>
        <input type="text" list="order-products-datalist" class="sub-product-input" placeholder="대체 완제품명 검색" style="flex:2;">
        <input type="hidden" class="sub-product-id">
        <input type="number" min="1" class="sub-quantity-input" placeholder="수량" style="flex:1;">
        <button type="button" class="btn-danger-text" onclick="document.getElementById('${rowId}').remove()">삭제</button>
    `;
    container.appendChild(div);

    const nameInput = div.querySelector(".sub-product-input");
    const idInput = div.querySelector(".sub-product-id");
    nameInput.addEventListener("input", () => {
        const matched = findProductByName(nameInput.value);
        idInput.value = matched ? matched.id : "";
    });
}

document.getElementById("order-form").addEventListener("submit", async (e) => {
    e.preventDefault();

    const channel = document.getElementById("order-channel").value;
    const orderDate = document.getElementById("order-date").value;
    const itemRows = document.querySelectorAll("#order-item-rows > div");
    const items = [];

    for (const row of itemRows) {
        const productId = row.querySelector(".order-product-id").value;
        const quantity = Number(row.querySelector(".order-quantity-input").value || 0);
        if (!productId || quantity <= 0) continue;

        const substitutions = [];
        const subRows = row.querySelectorAll(".substitution-rows > div");
        for (const subRow of subRows) {
            const subProductId = subRow.querySelector(".sub-product-id").value;
            const subQuantity = Number(subRow.querySelector(".sub-quantity-input").value || 0);
            if (subProductId && subQuantity > 0) {
                substitutions.push({ substituteProductId: Number(subProductId), quantity: subQuantity });
            }
        }

        items.push({ productId: Number(productId), quantity, substitutions });
    }

    if (items.length === 0) {
        showToast("주문 항목을 하나 이상 입력해주세요 (완제품명은 목록에서 검색해서 선택하세요).", true);
        return;
    }

    try {
        const response = await fetch(ORDER_API, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ channel, orderDate, items }),
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message);
        }

        showToast("주문이 등록됐어요.");
        document.getElementById("order-item-rows").innerHTML = "";
        addOrderItemRow();
        await loadOrders();
    } catch (err) {
        showToast(err.message, true);
    }
});

// ===================== 주문 목록 =====================
async function loadOrders() {
    const from = document.getElementById("order-from-date").value;
    const to = document.getElementById("order-to-date").value;
    const tbody = document.getElementById("order-table-body");
    const emptyState = document.getElementById("order-empty-state");

    try {
        const response = await fetch(`${ORDER_API}?from=${from}&to=${to}`);
        if (!response.ok) throw new Error("주문 목록을 불러오지 못했습니다.");
        const orders = await response.json();

        if (orders.length === 0) {
            tbody.innerHTML = "";
            emptyState.style.display = "block";
            return;
        }
        emptyState.style.display = "none";

        tbody.innerHTML = orders.map((order) => {
            const itemsSummary = order.items.map((item) => {
                const subText = item.substitutions.length > 0
                    ? ` (대체: ${item.substitutions.map((s) => `${escapeHtml(s.substituteProductName)} ${s.quantity}개`).join(", ")})`
                    : "";
                return `${escapeHtml(item.productName)} ${item.quantity}개${subText}`;
            }).join(" · ");

            const statusBadge = order.cancelled
                ? '<span class="badge badge-neutral">취소됨</span>'
                : '<span class="badge badge-success">정상</span>';

            return `
                <tr>
                    <td>${order.orderDate}</td>
                    <td>${CHANNEL_LABELS[order.channel] ?? order.channel}</td>
                    <td>${escapeHtml(order.orderNumber)}</td>
                    <td>${order.totalAmount.toLocaleString()}원</td>
                    <td style="font-size:13px;">${itemsSummary}</td>
                    <td>${statusBadge}</td>
                    <td>${order.cancelled ? "" : `<button type="button" class="btn-danger-text" onclick="cancelOrder(${order.id})">취소</button>`}</td>
                </tr>
            `;
        }).join("");
    } catch (err) {
        showToast(err.message, true);
    }
}

async function cancelOrder(orderId) {
    if (!confirm("이 주문을 취소할까요? 재고에 영향을 줬던 항목이면 재고가 원상복구돼요.")) return;
    try {
        const response = await fetch(`${ORDER_API}/${orderId}/cancel`, { method: "POST" });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message);
        }
        showToast("주문이 취소됐어요.");
        await loadOrders();
    } catch (err) {
        showToast(err.message, true);
    }
}
