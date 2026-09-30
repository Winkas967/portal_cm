// Tela "Administração": usuários, perfis (roles) e setores. Só para ADMIN.

const TABS = ["users", "roles", "sectors"];
const NEW_LABELS = { users: "+ Novo usuário", roles: "+ Novo perfil", sectors: "+ Novo setor" };

let currentTab = TABS.includes(new URLSearchParams(window.location.search).get("tab"))
    ? new URLSearchParams(window.location.search).get("tab") : "users";
let users = [];
let roles = [];
let sectors = [];
let editingUser = null;
let editingRole = null;
let editingSector = null;

const $ = id => document.getElementById(id);

// Ativos primeiro, depois por nome
function byActiveThenName(a, b) {
    if (a.isActive !== b.isActive) return a.isActive ? -1 : 1;
    return a.name.localeCompare(b.name, "pt-BR");
}

function statusBadge(active) {
    return `<span class="badge ${active ? "ativo" : "inativo"}">${active ? "Ativo" : "Inativo"}</span>`;
}

function emptyRow(cols, text) {
    return `<tr><td colspan="${cols}" class="table-empty">${text}</td></tr>`;
}

// ---------- Abas ----------

function setTab(tab) {
    currentTab = tab;
    document.querySelectorAll(".tab").forEach(t => t.classList.toggle("active", t.dataset.tab === tab));
    document.querySelectorAll("[data-panel]").forEach(p => p.classList.toggle("hidden", p.dataset.panel !== tab));
    $("new-button").textContent = NEW_LABELS[tab];

    // Mantém o menu lateral e a URL coerentes com a aba
    document.querySelectorAll(".sidebar .nav-item").forEach(link => {
        const href = link.getAttribute("href") || "";
        link.classList.toggle("active", href === "/admin?tab=" + tab);
    });
    history.replaceState(null, "", "/admin?tab=" + tab);
}

// ---------- Carregamento ----------

async function loadAll() {
    try {
        [users, roles, sectors] = await Promise.all([
            fetchJSON("/api/users"),
            fetchJSON("/api/roles"),
            fetchJSON("/api/sectors?includeInactive=true")
        ]);
        renderUsers();
        renderRoles();
        renderSectors();
    } catch (err) {
        showNotification(err.message, "error");
    }
}

function renderUsers() {
    const body = $("users-body");
    if (!users || users.length === 0) {
        body.innerHTML = emptyRow(4, "Nenhum usuário cadastrado.");
        return;
    }
    body.innerHTML = [...users].sort(byActiveThenName).map(u => {
        const isMe = currentUser && u.name.toLowerCase() === currentUser.username.toLowerCase();
        const roleLabel = (u.roleValue || "").toUpperCase() === "ADMIN"
            ? `<span class="badge admin">${escapeHtml(u.roleName)}</span>`
            : escapeHtml(u.roleName || "Sem perfil");
        return `<tr>
            <td class="cell-strong">${escapeHtml(u.name)}${isMe ? ' <span class="cell-sub">(você)</span>' : ""}</td>
            <td>${roleLabel}</td>
            <td>${statusBadge(u.isActive)}</td>
            <td><div class="row-actions">
                <button type="button" class="row-action edit" data-user-edit="${u.id}" ${u.isActive ? "" : "disabled"}>Editar</button>
                <button type="button" class="row-action danger" data-user-toggle="${u.id}" ${isMe ? "disabled title=\"Você não pode desativar o seu próprio usuário\"" : ""}>${u.isActive ? "Desativar" : "Reativar"}</button>
            </div></td>
        </tr>`;
    }).join("");

    body.querySelectorAll("[data-user-edit]").forEach(b =>
        b.addEventListener("click", () => openUserModal(users.find(u => u.id === Number(b.dataset.userEdit)))));
    body.querySelectorAll("[data-user-toggle]").forEach(b =>
        b.addEventListener("click", () => toggleUser(users.find(u => u.id === Number(b.dataset.userToggle)))));
}

function renderRoles() {
    const body = $("roles-body");
    if (!roles || roles.length === 0) {
        body.innerHTML = emptyRow(5, "Nenhum perfil cadastrado.");
        return;
    }
    body.innerHTML = [...roles].sort(byActiveThenName).map(r => {
        const isAdminRole = (r.role || "").toUpperCase() === "ADMIN";
        return `<tr>
            <td class="cell-strong">${escapeHtml(r.name)}</td>
            <td>${isAdminRole ? '<span class="badge admin">ADMIN</span>' : escapeHtml(r.role)}</td>
            <td>${r.userCount}</td>
            <td>${statusBadge(r.isActive)}</td>
            <td><div class="row-actions">
                <button type="button" class="row-action edit" data-role-edit="${r.id}" ${r.isActive ? "" : "disabled"}>Editar</button>
                ${isAdminRole ? "" : `<button type="button" class="row-action danger" data-role-toggle="${r.id}">${r.isActive ? "Desativar" : "Reativar"}</button>`}
            </div></td>
        </tr>`;
    }).join("");

    body.querySelectorAll("[data-role-edit]").forEach(b =>
        b.addEventListener("click", () => openRoleModal(roles.find(r => r.id === Number(b.dataset.roleEdit)))));
    body.querySelectorAll("[data-role-toggle]").forEach(b =>
        b.addEventListener("click", () => toggleRole(roles.find(r => r.id === Number(b.dataset.roleToggle)))));
}

function renderSectors() {
    const body = $("sectors-body");
    if (!sectors || sectors.length === 0) {
        body.innerHTML = emptyRow(3, "Nenhum setor cadastrado. Cadastre os setores para liberar o formulário de notificação.");
        return;
    }
    body.innerHTML = [...sectors].sort(byActiveThenName).map(s => `<tr>
            <td class="cell-strong">${escapeHtml(s.name)}</td>
            <td>${statusBadge(s.isActive)}</td>
            <td><div class="row-actions">
                <button type="button" class="row-action edit" data-sector-edit="${s.id}" ${s.isActive ? "" : "disabled"}>Editar</button>
                <button type="button" class="row-action danger" data-sector-toggle="${s.id}">${s.isActive ? "Desativar" : "Reativar"}</button>
            </div></td>
        </tr>`).join("");

    body.querySelectorAll("[data-sector-edit]").forEach(b =>
        b.addEventListener("click", () => openSectorModal(sectors.find(s => s.id === Number(b.dataset.sectorEdit)))));
    body.querySelectorAll("[data-sector-toggle]").forEach(b =>
        b.addEventListener("click", () => toggleSector(sectors.find(s => s.id === Number(b.dataset.sectorToggle)))));
}

// ---------- Modais ----------

function openModal(id) { $(id).classList.remove("hidden"); }
function closeModal(id) { $(id).classList.add("hidden"); }

document.querySelectorAll(".modal-overlay").forEach(overlay => {
    overlay.querySelectorAll("[data-close]").forEach(b => b.addEventListener("click", () => closeModal(overlay.id)));
    overlay.addEventListener("click", e => { if (e.target === overlay) closeModal(overlay.id); });
});
document.addEventListener("keydown", e => {
    if (e.key === "Escape") document.querySelectorAll(".modal-overlay").forEach(o => o.classList.add("hidden"));
});

// Usuário
function openUserModal(user) {
    editingUser = user || null;
    $("user-modal-title").textContent = user ? "Editar usuário" : "Cadastrar usuário";
    $("user-name").value = user ? user.name : "";
    $("user-password").value = "";
    $("user-password-label").innerHTML = user ? "Nova senha" : 'Senha<span class="req">*</span>';
    $("user-password-hint").classList.toggle("hidden", !user);
    $("user-error").textContent = "";

    const select = $("user-role");
    select.innerHTML = '<option value="">Selecione um perfil</option>' + roles
        .filter(r => r.isActive)
        .sort((a, b) => a.name.localeCompare(b.name, "pt-BR"))
        .map(r => `<option value="${r.id}">${escapeHtml(r.name)}</option>`).join("");
    select.value = user && user.roleId ? String(user.roleId) : "";

    openModal("user-modal");
    $("user-name").focus();
}

$("user-form").addEventListener("submit", async e => {
    e.preventDefault();
    const name = $("user-name").value.trim();
    const password = $("user-password").value;
    const roleId = $("user-role").value ? Number($("user-role").value) : null;
    const errorEl = $("user-error");

    if (!name) { errorEl.textContent = "Preencha o nome do usuário."; return; }
    if (!editingUser && password.length < 6) { errorEl.textContent = "A senha deve ter no mínimo 6 caracteres."; return; }
    if (editingUser && password && password.length < 6) { errorEl.textContent = "A nova senha deve ter no mínimo 6 caracteres."; return; }
    if (!roleId) { errorEl.textContent = "Selecione o perfil do usuário."; return; }

    const body = editingUser
        ? { name, roleId, ...(password ? { password } : {}) }
        : { name, password, roleId };
    const { ok, data } = editingUser
        ? await sendRequest("/api/users/" + editingUser.id, "PUT", body)
        : await sendRequest("/api/users", "POST", body);

    if (!ok) { errorEl.textContent = apiErrorMessage(data, "Não foi possível salvar o usuário."); return; }
    closeModal("user-modal");
    showNotification(editingUser ? "Usuário atualizado." : "Usuário cadastrado.", "success");
    loadAll();
});

async function toggleUser(user) {
    const action = user.isActive ? "deactivate" : "reactivate";
    if (user.isActive && !confirm(`Desativar o usuário "${user.name}"? Ele não conseguirá mais entrar no sistema.`)) return;
    const { ok, data } = await sendRequest(`/api/users/${user.id}/${action}`, "PATCH");
    if (!ok) { showNotification(apiErrorMessage(data, "Não foi possível alterar o usuário."), "error"); return; }
    showNotification(user.isActive ? "Usuário desativado." : "Usuário reativado.", "success");
    loadAll();
}

// Perfil
function openRoleModal(role) {
    editingRole = role || null;
    $("role-modal-title").textContent = role ? "Editar perfil" : "Cadastrar perfil";
    $("role-name").value = role ? role.name : "";
    $("role-code").value = role ? role.role : "";
    $("role-code").readOnly = !!role && (role.role || "").toUpperCase() === "ADMIN"; // o código ADMIN não pode mudar
    $("role-error").textContent = "";
    openModal("role-modal");
    $("role-name").focus();
}

$("role-form").addEventListener("submit", async e => {
    e.preventDefault();
    const name = $("role-name").value.trim();
    const role = $("role-code").value.trim().toUpperCase();
    const errorEl = $("role-error");

    if (!name) { errorEl.textContent = "Preencha o nome do perfil."; return; }
    if (!role) { errorEl.textContent = "Preencha o código do perfil."; return; }

    const { ok, data } = editingRole
        ? await sendRequest("/api/roles/" + editingRole.id, "PUT", { name, role })
        : await sendRequest("/api/roles", "POST", { name, role });

    if (!ok) { errorEl.textContent = apiErrorMessage(data, "Não foi possível salvar o perfil."); return; }
    closeModal("role-modal");
    showNotification(editingRole ? "Perfil atualizado." : "Perfil cadastrado.", "success");
    loadAll();
});

async function toggleRole(role) {
    const action = role.isActive ? "deactivate" : "reactivate";
    const { ok, data } = await sendRequest(`/api/roles/${role.id}/${action}`, "PATCH");
    if (!ok) { showNotification(apiErrorMessage(data, "Não foi possível alterar o perfil."), "error"); return; }
    showNotification(role.isActive ? "Perfil desativado." : "Perfil reativado.", "success");
    loadAll();
}

// Setor
function openSectorModal(sector) {
    editingSector = sector || null;
    $("sector-modal-title").textContent = sector ? "Editar setor" : "Cadastrar setor";
    $("sector-name").value = sector ? sector.name : "";
    $("sector-error").textContent = "";
    openModal("sector-modal");
    $("sector-name").focus();
}

$("sector-form").addEventListener("submit", async e => {
    e.preventDefault();
    const name = $("sector-name").value.trim();
    const errorEl = $("sector-error");
    if (!name) { errorEl.textContent = "Preencha o nome do setor."; return; }

    const { ok, data } = editingSector
        ? await sendRequest("/api/sectors/" + editingSector.id, "PUT", { name })
        : await sendRequest("/api/sectors", "POST", { name });

    if (!ok) { errorEl.textContent = apiErrorMessage(data, "Não foi possível salvar o setor."); return; }
    closeModal("sector-modal");
    showNotification(editingSector ? "Setor atualizado." : "Setor cadastrado.", "success");
    loadAll();
});

async function toggleSector(sector) {
    const action = sector.isActive ? "deactivate" : "reactivate";
    const { ok, data } = await sendRequest(`/api/sectors/${sector.id}/${action}`, "PATCH");
    if (!ok) { showNotification(apiErrorMessage(data, "Não foi possível alterar o setor."), "error"); return; }
    showNotification(sector.isActive ? "Setor desativado." : "Setor reativado.", "success");
    loadAll();
}

// ---------- Início ----------

document.querySelectorAll(".tab").forEach(t => t.addEventListener("click", () => setTab(t.dataset.tab)));
$("new-button").addEventListener("click", () => {
    if (currentTab === "users") openUserModal(null);
    else if (currentTab === "roles") openRoleModal(null);
    else openSectorModal(null);
});

(async function init() {
    const me = await initSession({ adminOnly: true });
    if (!me) return;
    setTab(currentTab);
    loadAll();
})();
