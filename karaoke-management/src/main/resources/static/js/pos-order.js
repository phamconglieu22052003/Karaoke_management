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
});
