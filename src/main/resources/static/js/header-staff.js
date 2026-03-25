document.write(`
<header class="bg-white border-b border-slate-200 h-16 flex items-center px-8 justify-between shrink-0 sticky top-0 z-10">
  <h2 class="text-lg font-semibold text-slate-800" id="header-title">
    대시보드
  </h2>
  <div class="flex items-center gap-4">
    <div class="w-8 h-8 rounded-full bg-indigo-100 flex items-center justify-center text-indigo-700 font-bold text-sm">
      원
    </div>
  </div>
</header>
`);

document.addEventListener("DOMContentLoaded", () => {
  const path = window.location.pathname;
  const titleEl = document.getElementById("header-title");
  if (path.includes("dashboard.html")) titleEl.textContent = "대시보드";
  else if (path.includes("reception-list.html")) titleEl.textContent = "접수 목록";
  else if (path.includes("reception-detail.html")) titleEl.textContent = "예약 환자 접수";
  else if (path.includes("phone-reservation.html")) titleEl.textContent = "전화 예약";
  else if (path.includes("walkin-reception.html")) titleEl.textContent = "방문 접수";
});
