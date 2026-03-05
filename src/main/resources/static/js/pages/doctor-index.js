function setActive(element, title) {
    document.querySelectorAll('.nav-link').forEach(function (link) {
        link.classList.remove('bg-indigo-50', 'text-indigo-700');
        link.classList.add('text-slate-600', 'hover:bg-slate-100', 'hover:text-slate-900');
    });

    element.classList.remove('text-slate-600', 'hover:bg-slate-100', 'hover:text-slate-900');
    element.classList.add('bg-indigo-50', 'text-indigo-700');

    document.getElementById('headerTitle').textContent = title;
}
