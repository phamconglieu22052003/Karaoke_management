window.getQty = function () {
  const el = document.getElementById("qaQtyInput");
  const v = parseInt(el && el.value ? el.value : "1", 10);
  return isNaN(v) ? 1 : Math.max(1, v);
};

window.getNote = function () {
  const el = document.getElementById("qaNoteInput");
  return (el && el.value ? el.value : "").trim();
};

window.resetQuickAdd = function () {
  const q = document.getElementById("qaQtyInput");
  const n = document.getElementById("qaNoteInput");
  if (q) q.value = "1";
  if (n) n.value = "";
};

window.quickAddById = function (productId) {
  const form = document.getElementById("addForm");
  if (!form) return;

  const pid = document.getElementById("qaProductId");
  const qty = document.getElementById("qaQty");
  const note = document.getElementById("qaNote");

  if (!pid || !qty || !note) return;

  pid.value = String(productId);
  qty.value = String(window.getQty());
  note.value = window.getNote();
  form.submit();
};

window.filterProducts = function () {
  const catEl = document.getElementById("catFilter");
  const qEl = document.getElementById("searchBox");
  const cat = catEl ? String(catEl.value || "") : "";
  const q = (qEl ? qEl.value : "").trim().toLowerCase();

  const list = document.getElementById("productList");
  if (!list) return;

  let visibleCount = 0;
  list.querySelectorAll(".prod").forEach((card) => {
    const ccat = String(card.getAttribute("data-cat") || "");
    const name = String(card.getAttribute("data-name") || "").toLowerCase();
    const matchCat = !cat || ccat === cat;
    const matchName = !q || name.includes(q);
    const show = matchCat && matchName;
    card.style.display = show ? "flex" : "none";
    if (show) visibleCount += 1;
  });

  const head = document.querySelector(".product-list-head b");
  if (head) head.textContent = String(visibleCount);
};

window.addEventListener("DOMContentLoaded", function () {
  window.filterProducts();

  // ===== Live calculator for POS order table =====
  const fmt = new Intl.NumberFormat("vi-VN");

  function toNum(v) {
    const n = typeof v === "number" ? v : parseFloat(String(v || "0"));
    return Number.isFinite(n) ? n : 0;
  }

  function findUnitPriceInRow(tr) {
    if (!tr) return 0;
    const td = tr.querySelector("td[data-unit-price]");
    return toNum(td ? td.getAttribute("data-unit-price") : 0);
  }

  function updateRowAmount(tr) {
    if (!tr) return 0;
    const qtyInput = tr.querySelector("input.input-qty[name='quantity']");
    const qty = Math.max(
      1,
      parseInt(qtyInput && qtyInput.value ? qtyInput.value : "1", 10) || 1,
    );
    if (qtyInput) qtyInput.value = String(qty);
    const unitPrice = findUnitPriceInRow(tr);
    const amount = unitPrice * qty;
    const amountTd = tr.querySelector("td.js-line-amount");
    if (amountTd) amountTd.textContent = fmt.format(Math.round(amount));
    return amount;
  }

  function updateOrderTotal() {
    const totalEl = document.getElementById("js-order-total");
    if (!totalEl) return;
    const table = document.querySelector(".order-table");
    if (!table) return;
    let sum = 0;
    table.querySelectorAll("tbody tr").forEach((tr) => {
      sum += updateRowAmount(tr);
    });
    totalEl.textContent = fmt.format(Math.round(sum));
  }

  function showToast(msg, type) {
    const id = "posToast";
    let toast = document.getElementById(id);
    if (!toast) {
      toast = document.createElement("div");
      toast.id = id;
      toast.style.position = "fixed";
      toast.style.right = "16px";
      toast.style.bottom = "16px";
      toast.style.zIndex = "9999";
      toast.style.padding = "10px 12px";
      toast.style.borderRadius = "10px";
      toast.style.boxShadow = "0 10px 25px rgba(0,0,0,.18)";
      toast.style.fontWeight = "700";
      toast.style.fontSize = "14px";
      toast.style.color = "#0f172a";
      toast.style.background = "#ffffff";
      document.body.appendChild(toast);
    }
    toast.textContent = msg;
    toast.style.border =
      type === "error" ? "1px solid #fecaca" : "1px solid #bbf7d0";
    toast.style.background = type === "error" ? "#fff1f2" : "#f0fdf4";
    toast.style.display = "block";
    clearTimeout(window.__toastTimer);
    window.__toastTimer = setTimeout(() => {
      toast.style.display = "none";
    }, 1500);
  }

  // Live update as user types quantity
  document
    .querySelectorAll("input.input-qty[name='quantity']")
    .forEach((inp) => {
      inp.addEventListener("input", () => {
        const tr = inp.closest("tr");
        updateRowAmount(tr);
        updateOrderTotal();
      });
      inp.addEventListener("change", () => {
        const tr = inp.closest("tr");
        updateRowAmount(tr);
        updateOrderTotal();
      });
    });

  // AJAX save per row (so "Lưu" updates immediately without full page reload)
  document.querySelectorAll("form.inline-form[id^='upd__']").forEach((form) => {
    form.addEventListener("submit", async (e) => {
      e.preventDefault();

      const qtyInput = form.querySelector("input[name='quantity']");
      const qty = qtyInput && qtyInput.value ? qtyInput.value : "1";
      const noteInput = document.querySelector(
        `input[name='note'][form='${form.id}']`,
      );
      const note = noteInput && noteInput.value ? noteInput.value : "";

      const csrf = form.querySelector("input[type='hidden'][name]");
      const params = new URLSearchParams();
      params.set("quantity", qty);
      params.set("note", note);
      if (csrf && csrf.name && csrf.value) params.set(csrf.name, csrf.value);

      try {
        const res = await fetch(form.action, {
          method: "POST",
          headers: { "Content-Type": "application/x-www-form-urlencoded" },
          body: params.toString(),
          redirect: "follow",
        });

        if (!res.ok) {
          showToast("Lưu thất bại (" + res.status + ")", "error");
          return;
        }

        // Update UI immediately
        const tr = form.closest("tr");
        updateRowAmount(tr);
        updateOrderTotal();
        showToast("Đã lưu", "ok");
      } catch (err) {
        console.error(err);
        showToast("Lưu thất bại", "error");
      }
    });
  });

  // Initial render recalculation (in case DB line_amount is stale)
  updateOrderTotal();
});
