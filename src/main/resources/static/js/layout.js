// Menu lateral no celular/tablet (abre e fecha por cima da tela)
(function () {
    const toggle = document.getElementById("mobile-nav-toggle");
    const sidebar = document.querySelector(".sidebar");
    const backdrop = document.getElementById("sidebar-backdrop");

    if (!toggle || !sidebar || !backdrop) return;

    function openSidebar() {
        sidebar.classList.add("open");
        backdrop.classList.add("open");
    }

    function closeSidebar() {
        sidebar.classList.remove("open");
        backdrop.classList.remove("open");
    }

    toggle.addEventListener("click", function () {
        if (sidebar.classList.contains("open")) {
            closeSidebar();
        } else {
            openSidebar();
        }
    });

    backdrop.addEventListener("click", closeSidebar);

    sidebar.querySelectorAll(".nav-item").forEach(function (link) {
        link.addEventListener("click", closeSidebar);
    });

    window.addEventListener("resize", function () {
        if (window.innerWidth > 900) {
            closeSidebar();
        }
    });
})();
