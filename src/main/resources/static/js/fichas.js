// Tela "Fichas": indicadores, filtros e lista paginada.

const PAGE_SIZE = 15;
let page = 0;
let totalPages = 1;
let incidentTypes = [];

const CLIP_ICON = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="m21.4 11.1-9.2 9.2a6 6 0 0 1-8.5-8.5l9.2-9.2a4 4 0 0 1 5.7 5.7l-9.2 9.2a2 2 0 0 1-2.8-2.8l8.5-8.5"/></svg>';

function buildQuery(params) {
    const q = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== "") q.append(k, v);
    });
    return q.toString();
}

// Quantidade de fichas para um filtro (usa só o totalElements, pedindo 1 item)
async function countNotifications(params) {
    const data = await fetchJSON("/api/notifications?" + buildQuery({ ...params, size: 1 }));
    return data ? data.totalElements : 0;
}

async function loadStats() {
    const today = todayISO();
    const monthStart = today.slice(0, 8) + "01";
    const pharmaco = incidentTypes.find(t => t.code === "PHARMACOVIGILANCE");
    const techno = incidentTypes.find(t => t.code === "TECHNOVIGILANCE");

    const monthName = new Date().toLocaleDateString("pt-BR", { timeZone: TIMEZONE, month: "long" });
    document.getElementById("stat-month-label").textContent = "Em " + monthName;

    try {
        const [dayCount, weekCount, monthCount, pharmacoCount, technoCount] = await Promise.all([
            countNotifications({ from: today, to: today }),
            countNotifications({ from: addDaysISO(today, -6), to: today }),
            countNotifications({ from: monthStart, to: today }),
            pharmaco ? countNotifications({ from: monthStart, to: today, incidentTypeId: pharmaco.id }) : 0,
            techno ? countNotifications({ from: monthStart, to: today, incidentTypeId: techno.id }) : 0
        ]);
        document.getElementById("stat-today").textContent = dayCount;
        document.getElementById("stat-week").textContent = weekCount;
        document.getElementById("stat-month").textContent = monthCount;
        // Uma ficha não pode ter as duas vigilâncias ao mesmo tempo, então a soma não repete fichas
        document.getElementById("stat-vigilance").textContent = pharmacoCount + technoCount;
    } catch (err) {
        showNotification(err.message, "error");
    }
}

async function loadFilterOptions() {
    const [sectors, types] = await Promise.all([
        fetchJSON("/api/sectors?includeInactive=true"),
        fetchJSON("/api/incident-types")
    ]);
    incidentTypes = types || [];

    const sectorSelect = document.getElementById("filter-sector");
    (sectors || []).forEach(s => {
        const option = document.createElement("option");
        option.value = s.id;
        option.textContent = s.isActive ? s.name : s.name + " (inativo)";
        sectorSelect.appendChild(option);
    });

    const typeSelect = document.getElementById("filter-type");
    incidentTypes.forEach(t => {
        const option = document.createElement("option");
        option.value = t.id;
        option.textContent = t.name;
        typeSelect.appendChild(option);
    });
}

function currentFilters() {
    return {
        sectorId: document.getElementById("filter-sector").value,
        incidentTypeId: document.getElementById("filter-type").value,
        from: document.getElementById("filter-from").value,
        to: document.getElementById("filter-to").value
    };
}

async function loadPage() {
    const body = document.getElementById("fichas-body");
    const query = buildQuery({ ...currentFilters(), page, size: PAGE_SIZE, sort: "notificationDate,desc" });

    let data;
    try {
        data = await fetchJSON("/api/notifications?" + query);
    } catch (err) {
        body.innerHTML = `<tr><td colspan="7" class="table-empty">${escapeHtml(err.message)}</td></tr>`;
        return;
    }
    if (!data) return;

    totalPages = Math.max(1, data.totalPages);

    if (data.content.length === 0) {
        body.innerHTML = '<tr><td colspan="7" class="table-empty">Nenhuma ficha encontrada para os filtros selecionados.</td></tr>';
    } else {
        body.innerHTML = data.content.map(renderRow).join("");
        body.querySelectorAll("tr[data-id]").forEach(row => {
            row.addEventListener("click", () => {
                window.location.href = "/fichas/detalhe?id=" + row.dataset.id;
            });
        });
    }

    const first = data.totalElements === 0 ? 0 : data.page * data.size + 1;
    const last = data.page * data.size + data.content.length;
    document.getElementById("page-summary").textContent =
        data.totalElements === 0 ? "" : `Mostrando ${first}–${last} de ${data.totalElements} fichas`;
    document.getElementById("page-info").textContent = `Página ${page + 1} de ${totalPages}`;
    document.getElementById("prev-page").disabled = page <= 0;
    document.getElementById("next-page").disabled = page >= totalPages - 1;
}

function renderRow(n) {
    // O resumo traz só os nomes dos tipos; a cor vem do catálogo, pelo nome
    const chips = n.incidentTypes.map(name => {
        const type = incidentTypes.find(t => t.name === name);
        return `<span class="chip ${chipClass(type && type.code)}">${escapeHtml(name)}</span>`;
    });
    const visible = chips.slice(0, 2).join("");
    const more = chips.length > 2 ? `<span class="chip more">+${chips.length - 2}</span>` : "";

    return `<tr class="clickable" data-id="${n.id}">
        <td class="cell-num">${n.id}</td>
        <td><div class="cell-strong">${escapeHtml(n.patientName)}</div></td>
        <td>${escapeHtml(n.sector.name)}</td>
        <td><div class="chips">${visible}${more}</div></td>
        <td><div class="cell-strong">${formatDate(n.eventDate)}</div><div class="cell-sub">${formatTime(n.eventDate)}</div></td>
        <td>${escapeHtml(n.notifier.name)}</td>
        <td>${n.attachmentCount > 0 ? `<span class="clip" title="${n.attachmentCount} anexo(s)">${CLIP_ICON}${n.attachmentCount}</span>` : ""}</td>
    </tr>`;
}

function reload() {
    page = 0;
    loadPage();
}

document.getElementById("filters-form").addEventListener("change", reload);
document.getElementById("filters-form").addEventListener("submit", e => { e.preventDefault(); reload(); });
document.getElementById("filters-clear").addEventListener("click", () => {
    document.getElementById("filters-form").reset();
    reload();
});
document.getElementById("prev-page").addEventListener("click", () => {
    if (page > 0) { page--; loadPage(); }
});
document.getElementById("next-page").addEventListener("click", () => {
    if (page < totalPages - 1) { page++; loadPage(); }
});

(async function init() {
    const me = await initSession();
    if (!me) return;

    document.getElementById("page-subtitle").textContent =
        (isAdmin() ? "Todas as fichas registradas" : "Fichas registradas por você") + " — " + formatLongToday();

    try {
        await loadFilterOptions();
    } catch (err) {
        showNotification(err.message, "error");
    }
    loadStats();
    loadPage();
})();
