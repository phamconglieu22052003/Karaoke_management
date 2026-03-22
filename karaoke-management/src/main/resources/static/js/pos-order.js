// Đảm bảo các hàm là global (window.*) để onclick gọi được
console.log("POS JS LOADED");
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
  const form = document.getElementById("quickAddForm");
  if (!form) {
    console.warn("quickAddForm not found (có thể đang READ ONLY?)");
    return;
  }

  const pid = document.getElementById("qaProductId");
  const qty = document.getElementById("qaQty");
  const note = document.getElementById("qaNote");

  if (!pid || !qty || !note) {
    console.warn("Hidden inputs not found");
    return;
  }

  pid.value = productId;
  qty.value = String(window.getQty());
  note.value = window.getNote();

  form.submit();
};

window.filterProducts = function () {
  const catEl = document.getElementById("catFilter");
  const qEl = document.getElementById("searchBox");
  const cat = catEl ? catEl.value : "";
  const q = (qEl ? qEl.value : "").trim().toLowerCase();

  const list = document.getElementById("productList");
  if (!list) return;

  list.querySelectorAll(".prod").forEach((c) => {
    const ccat = c.getAttribute("data-cat") || "";
    const name = (c.getAttribute("data-name") || "").toLowerCase();
    const matchCat = !cat || ccat === cat;
    const matchName = !q || name.includes(q);
    c.style.display = matchCat && matchName ? "flex" : "none";
  });
};

window.stepQty = function (itemId, delta) {
  const input = document.getElementById("qty__" + itemId);
  if (!input) return;

  const current = parseInt(input.value || "1", 10);
  const next = Math.max(1, current + delta);
  input.value = String(next);

  const form = document.getElementById("upd__" + itemId);
  if (form) form.submit();
};
