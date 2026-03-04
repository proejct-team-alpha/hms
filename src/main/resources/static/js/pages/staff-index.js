document.addEventListener('DOMContentLoaded', function () {
    var iframe = document.querySelector('iframe[name="contentFrame"]');
    var navLinks = document.querySelectorAll('.nav-link');
    var pageTitle = document.getElementById('pageTitle');

    iframe.addEventListener('load', function () {
        try {
            var iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
            var title = iframeDoc.title;
            if (title) {
                pageTitle.textContent = title;
            }

            var currentUrl = iframe.contentWindow.location.href;
            navLinks.forEach(function (link) {
                link.classList.remove('bg-indigo-600', 'text-white');
                link.classList.add('text-slate-300');
                if (currentUrl.includes(link.getAttribute('href'))) {
                    link.classList.remove('text-slate-300', 'hover:bg-slate-800');
                    link.classList.add('bg-indigo-600', 'text-white');
                }
            });
        } catch (e) {
            console.log('Cross-origin iframe access denied');
        }
    });

    navLinks[0].classList.remove('text-slate-300', 'hover:bg-slate-800');
    navLinks[0].classList.add('bg-indigo-600', 'text-white');
});
