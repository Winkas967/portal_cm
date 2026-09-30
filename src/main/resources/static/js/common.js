// Funções usadas por todas as telas logadas: chamadas à API, sessão e formatação.

const TIMEZONE = "America/Sao_Paulo";

function redirectToLogin() {
    queueNotification("Sua sessão expirou. Faça login novamente.", "info");
    window.location.href = "/login";
}

// GET que devolve JSON. Em 401 manda para o login; em erro lança exceção com a mensagem da API.
async function fetchJSON(url) {
    const response = await fetch(url, { headers: { "Accept": "application/json" } });

    if (response.status === 401) {
        redirectToLogin();
        return null;
    }

    const data = await readBody(response);
    if (!response.ok) {
        throw new Error(apiErrorMessage(data, "Não foi possível carregar os dados."));
    }
    return data;
}

// POST/PUT/PATCH com corpo JSON (ou FormData, para envio de arquivos).
async function sendRequest(url, method, body) {
    const options = { method, headers: { "Accept": "application/json" } };

    if (body instanceof FormData) {
        options.body = body; // o navegador define o Content-Type multipart sozinho
    } else if (body !== undefined) {
        options.headers["Content-Type"] = "application/json";
        options.body = JSON.stringify(body);
    }

    const response = await fetch(url, options);

    if (response.status === 401) {
        redirectToLogin();
        return { ok: false, status: 401, data: null };
    }

    return { ok: response.ok, status: response.status, data: await readBody(response) };
}

async function readBody(response) {
    if (response.status === 204) return null;
    const text = await response.text();
    if (!text) return null;
    try {
        return JSON.parse(text);
    } catch (err) {
        return null;
    }
}

// Junta a mensagem da API (ApiError) com os erros de campo, se houver.
function apiErrorMessage(data, fallback) {
    if (!data) return fallback;
    if (data.fields && Object.keys(data.fields).length > 0) {
        return Object.values(data.fields).join(" ");
    }
    return data.message || fallback;
}

// Escapa texto antes de montar HTML (evita que um nome com "<" quebre a página).
function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

// ----- Datas (a API devolve ISO com fuso, ex.: 2026-09-30T08:40:00-03:00) -----

function formatDate(iso) {
    if (!iso) return "—";
    // Datas sem hora (yyyy-MM-dd) não passam por fuso, para não "voltar" um dia
    if (/^\d{4}-\d{2}-\d{2}$/.test(iso)) {
        const [y, m, d] = iso.split("-");
        return `${d}/${m}/${y}`;
    }
    return new Date(iso).toLocaleDateString("pt-BR", { timeZone: TIMEZONE });
}

function formatTime(iso) {
    if (!iso) return "";
    return new Date(iso).toLocaleTimeString("pt-BR", { timeZone: TIMEZONE, hour: "2-digit", minute: "2-digit" });
}

function formatDateTime(iso) {
    if (!iso) return "—";
    return formatDate(iso) + " " + formatTime(iso);
}

// Data de hoje no fuso do hospital, no formato yyyy-MM-dd
function todayISO() {
    return new Date().toLocaleDateString("en-CA", { timeZone: TIMEZONE });
}

function addDaysISO(isoDate, days) {
    const d = new Date(isoDate + "T12:00:00");
    d.setDate(d.getDate() + days);
    return d.toLocaleDateString("en-CA");
}

function formatLongToday() {
    const text = new Date().toLocaleDateString("pt-BR", {
        timeZone: TIMEZONE, weekday: "long", day: "numeric", month: "long", year: "numeric"
    });
    return text.charAt(0).toUpperCase() + text.slice(1);
}

function formatFileSize(bytes) {
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return Math.round(bytes / 1024) + " KB";
    return (bytes / (1024 * 1024)).toFixed(1).replace(".", ",") + " MB";
}

function fileKind(contentTypeOrName) {
    const v = String(contentTypeOrName || "").toLowerCase();
    return v.includes("pdf") ? "pdf" : "img";
}

function fileLabel(contentTypeOrName) {
    const v = String(contentTypeOrName || "").toLowerCase();
    if (v.includes("pdf")) return "PDF";
    if (v.includes("png")) return "PNG";
    return "JPG";
}

// Cor da etiqueta de cada tipo de incidente, pelo código
function chipClass(code) {
    if (code === "PHARMACOVIGILANCE") return "farmaco";
    if (code === "TECHNOVIGILANCE") return "tecno";
    if (code === "MEDICATION_ERROR") return "med";
    return "";
}

// ----- Sessão -----

let currentUser = null;

function isAdmin() {
    return !!currentUser && Array.isArray(currentUser.role) && currentUser.role.includes("ADMIN");
}

// Confere o login, preenche o usuário no menu e esconde o que é só de ADMIN.
// Cada tela chama: const me = await initSession();
async function initSession(options = {}) {
    let me;
    try {
        me = await fetchJSON("/api/auth/me");
    } catch (err) {
        showNotification("Não foi possível verificar sua sessão.", "error");
        return null;
    }
    if (!me) return null;

    currentUser = me;

    if (options.adminOnly && !isAdmin()) {
        queueNotification("Você não possui permissão para acessar esta página.", "error");
        window.location.href = "/fichas";
        return null;
    }

    const nameEl = document.getElementById("user-mini-name");
    const roleEl = document.getElementById("user-mini-role");
    const avatarEl = document.getElementById("user-avatar");
    if (nameEl) nameEl.textContent = me.username;
    if (roleEl) roleEl.textContent = me.displayRole;
    if (avatarEl) avatarEl.textContent = (me.username || "?").charAt(0).toUpperCase();

    if (!isAdmin()) {
        document.querySelectorAll(".admin-only").forEach(el => el.classList.add("hidden"));
    }

    const layout = document.querySelector(".app-layout");
    if (layout) layout.classList.remove("auth-checking");

    return me;
}

document.addEventListener("DOMContentLoaded", function () {
    const logout = document.getElementById("logout-link");
    if (!logout) return;
    logout.addEventListener("click", async function (event) {
        event.preventDefault();
        try {
            await fetch("/api/auth/logout", { method: "POST" });
        } finally {
            window.location.href = "/login";
        }
    });
});
