document.write(`
<aside class="w-64 bg-white border-r border-slate-200 flex flex-col h-screen fixed left-0 top-0 z-10">
  <div class="p-6 border-b border-slate-200">
    <h1 class="text-xl font-bold text-slate-800 flex items-center gap-2">
      <i data-feather="activity" class="text-indigo-600"></i>
      MediCare+
    </h1>
  </div>
  
  <div class="p-4 border-b border-slate-200 bg-slate-50 flex items-center justify-between">
    <div>
      <div class="text-sm font-bold text-slate-800">물품 담당자</div>
      <div class="text-xs text-slate-500">접속 중</div>
    </div>
    <div class="flex items-center gap-1">
      <a href="#" class="p-2 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors" title="로그아웃">
        <i data-feather="log-out" class="w-5 h-5"></i>
      </a>
    </div>
  </div>

  <nav class="flex-1 overflow-y-auto p-4 space-y-1">
    <a href="dashboard.html" class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors text-slate-600 hover:bg-slate-100 hover:text-slate-900" id="nav-dashboard">
      <i data-feather="layout" class="w-5 h-5"></i>
      대시보드
    </a>
    <a href="item-list.html" class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors text-slate-600 hover:bg-slate-100 hover:text-slate-900" id="nav-item-list">
      <i data-feather="package" class="w-5 h-5"></i>
      물품 관리
    </a>
    <a href="item-history.html" class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors text-slate-600 hover:bg-slate-100 hover:text-slate-900" id="nav-item-history">
      <i data-feather="clock" class="w-5 h-5"></i>
      입출고 내역
    </a>
  </nav>
</aside>
`);

document.addEventListener("DOMContentLoaded", () => {
  const path = window.location.pathname;
  if (path.includes("dashboard.html")) {
    document.getElementById("nav-dashboard").classList.add("bg-indigo-50", "text-indigo-700");
    document.getElementById("nav-dashboard").classList.remove("text-slate-600");
  } else if (path.includes("item-list.html") || path.includes("item-form.html")) {
    document.getElementById("nav-item-list").classList.add("bg-indigo-50", "text-indigo-700");
    document.getElementById("nav-item-list").classList.remove("text-slate-600");
  } else if (path.includes("item-history.html")) {
    document.getElementById("nav-item-history").classList.add("bg-indigo-50", "text-indigo-700");
    document.getElementById("nav-item-history").classList.remove("text-slate-600");
  }
});
