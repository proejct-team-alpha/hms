document.write(`
<header class="bg-white border-b border-slate-200 h-16 flex items-center px-8 justify-between shrink-0 sticky top-0 z-10">
  <h2 class="text-lg font-semibold text-slate-800" id="header-title">
    대시보드
  </h2>
  <div class="flex items-center gap-4">
    <div class="w-8 h-8 rounded-full bg-indigo-100 flex items-center justify-center text-indigo-700 font-bold text-sm">
      관
    </div>
  </div>
</header>
`);

document.addEventListener("DOMContentLoaded", () => {
  const path = window.location.pathname;
  const titleEl = document.getElementById("header-title");
  if (path.includes("dashboard.html")) titleEl.textContent = "관리자 대시보드";
  else if (path.includes("reservation-list.html")) titleEl.textContent = "예약 취소 관리";
  else if (path.includes("department-list.html")) titleEl.textContent = "진료과 관리";
  else if (path.includes("rule-list.html") || path.includes("rule-new.html")) titleEl.textContent = "병원 규칙 관리";
  else if (path.includes("staff-list.html") || path.includes("staff-form.html")) titleEl.textContent = "직원 관리";
  else if (path.includes("item-list.html") || path.includes("item-form.html")) titleEl.textContent = "물품 관리";
});
