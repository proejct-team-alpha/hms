document.write(`
<header class="bg-white border-b border-slate-200 h-16 flex items-center px-8 justify-between shrink-0 sticky top-0 z-10">
  <h2 class="text-lg font-semibold text-slate-800" id="header-title">
    대시보드
  </h2>
  <div class="flex items-center gap-4">
    <div class="w-8 h-8 rounded-full bg-indigo-100 flex items-center justify-center text-indigo-700 font-bold text-sm">
      의
    </div>
  </div>
</header>
`);

document.addEventListener("DOMContentLoaded", () => {
  const path = window.location.pathname;
  const titleEl = document.getElementById("header-title");
  if (path.includes("dashboard.html")) titleEl.textContent = "대시보드";
  else if (path.includes("treatment-list.html")) titleEl.textContent = "진료 목록";
  else if (path.includes("treatment-detail.html")) titleEl.textContent = "진료실";
  else if (path.includes("completed-list.html")) titleEl.textContent = "진료 완료 목록";
});
