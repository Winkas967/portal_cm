// Tela "Nova notificação": monta o formulário FOR-NSP-001 e envia como multipart
// (parte "data" com o JSON da ficha + parte "files" com os anexos).

const MAX_FILES = 5;
const MAX_FILE_SIZE = 10 * 1024 * 1024;
const ALLOWED_EXTENSIONS = ["jpg", "jpeg", "png", "pdf"];

// Etiqueta mostrada nos tipos que pedem informação extra
const TYPE_TAGS = {
    MEDICATION_ERROR: "+ fase",
    PHLEBITIS: "+ tipo",
    OTHER: "+ descrição",
    PHARMACOVIGILANCE: "+ seção 2.1",
    TECHNOVIGILANCE: "+ seção 2.1"
};

let incidentTypes = [];
let selectedFiles = [];

const $ = id => document.getElementById(id);
const value = id => $(id).value.trim();
const orNull = v => (v === "" ? null : v);

// ---------- Tipos de incidente ----------

function renderTypes() {
    const grid = $("type-grid");
    grid.innerHTML = incidentTypes.map(t => `
        <label class="type-option" data-code="${t.code || ""}">
            <input type="checkbox" value="${t.id}">
            <span>${escapeHtml(t.name)}</span>
            ${t.code && TYPE_TAGS[t.code] ? `<span class="tag">${TYPE_TAGS[t.code]}</span>` : ""}
        </label>`).join("");
    grid.addEventListener("change", onTypesChanged);
}

function markedCodes() {
    const codes = new Set();
    document.querySelectorAll("#type-grid .type-option").forEach(el => {
        if (el.querySelector("input").checked && el.dataset.code) codes.add(el.dataset.code);
    });
    return codes;
}

function onTypesChanged() {
    document.querySelectorAll("#type-grid .type-option").forEach(el => {
        el.classList.toggle("checked", el.querySelector("input").checked);
    });
    const codes = markedCodes();
    $("sub-medication").classList.toggle("hidden", !codes.has("MEDICATION_ERROR"));
    $("sub-phlebitis").classList.toggle("hidden", !codes.has("PHLEBITIS"));
    $("sub-other").classList.toggle("hidden", !codes.has("OTHER"));
    $("section-pharmaco").classList.toggle("hidden", !codes.has("PHARMACOVIGILANCE"));
    $("section-techno").classList.toggle("hidden", !codes.has("TECHNOVIGILANCE"));
    updateChecklist();
}

// ---------- Anexos ----------

function addFiles(fileList) {
    for (const file of fileList) {
        const ext = file.name.split(".").pop().toLowerCase();
        if (!ALLOWED_EXTENSIONS.includes(ext)) {
            showNotification(`"${file.name}" não é JPG, PNG ou PDF.`, "error");
            continue;
        }
        if (file.size > MAX_FILE_SIZE) {
            showNotification(`"${file.name}" é maior que 10 MB.`, "error");
            continue;
        }
        if (selectedFiles.length >= MAX_FILES) {
            showNotification(`Envie no máximo ${MAX_FILES} arquivos por ficha.`, "error");
            break;
        }
        selectedFiles.push(file);
    }
    renderFiles();
}

function renderFiles() {
    const list = $("file-list");
    list.innerHTML = selectedFiles.map((f, i) => `
        <div class="file-item">
            <div class="file-icon ${fileKind(f.type || f.name)}">${fileLabel(f.type || f.name)}</div>
            <div><div class="file-name">${escapeHtml(f.name)}</div><div class="file-size">${formatFileSize(f.size)}</div></div>
            <button type="button" class="btn-link danger" data-index="${i}">Remover</button>
        </div>`).join("");
    list.querySelectorAll("button[data-index]").forEach(btn => {
        btn.addEventListener("click", () => {
            selectedFiles.splice(Number(btn.dataset.index), 1);
            renderFiles();
        });
    });
    updateChecklist();
}

function setupDropzone() {
    const zone = $("dropzone");
    const input = $("file-input");

    input.addEventListener("change", () => {
        addFiles(input.files);
        input.value = ""; // permite escolher o mesmo arquivo de novo depois de remover
    });
    ["dragenter", "dragover"].forEach(evt => zone.addEventListener(evt, e => {
        e.preventDefault();
        zone.classList.add("dragover");
    }));
    ["dragleave", "drop"].forEach(evt => zone.addEventListener(evt, e => {
        e.preventDefault();
        zone.classList.remove("dragover");
    }));
    zone.addEventListener("drop", e => addFiles(e.dataTransfer.files));
}

// ---------- Checklist lateral ----------

function checklistItems() {
    const codes = markedCodes();
    const items = [
        { label: "Setor e data do evento", ok: value("sector") !== "" && value("event-date") !== "" },
        { label: "Tipo de incidente", ok: codes.size > 0 || document.querySelector("#type-grid input:checked") !== null }
    ];
    if (codes.has("MEDICATION_ERROR")) {
        items.push({ label: "Fase do erro de medicação", ok: !!document.querySelector('input[name="medication-stage"]:checked') });
    }
    if (codes.has("PHLEBITIS")) {
        items.push({ label: "Tipo de flebite", ok: !!document.querySelector('input[name="phlebitis-type"]:checked') });
    }
    if (codes.has("OTHER")) {
        items.push({ label: "Descrição de \"Outros\"", ok: value("other-description") !== "" });
    }
    if (codes.has("PHARMACOVIGILANCE")) {
        items.push({ label: "Dados do medicamento", ok: ["product-name", "manufacturer", "batch", "expiry-date"].every(id => value(id) !== "") });
    }
    if (codes.has("TECHNOVIGILANCE")) {
        items.push({ label: "Dados do equipamento", ok: value("equipment") !== "" });
    }
    items.push({ label: "Nome do paciente", ok: value("patient-name") !== "" });
    items.push({ label: "Descrição do evento", ok: value("event-description") !== "" });
    return items;
}

function updateChecklist() {
    const items = checklistItems();
    const done = items.filter(i => i.ok).length;
    $("progress-bar").style.width = Math.round((done / items.length) * 100) + "%";
    $("checklist").innerHTML = items.map(i =>
        `<div class="check-item ${i.ok ? "ok" : ""}"><div class="check-dot"></div>${escapeHtml(i.label)}</div>`
    ).join("") + `<div class="check-item ${selectedFiles.length ? "ok" : ""}"><div class="check-dot"></div>Anexos (opcional)${selectedFiles.length ? " — " + selectedFiles.length : ""}</div>`;
}

// ---------- Envio ----------

function buildRequest() {
    const codes = markedCodes();
    const typeIds = Array.from(document.querySelectorAll("#type-grid input:checked")).map(i => Number(i.value));
    const stage = document.querySelector('input[name="medication-stage"]:checked');
    const phlebitis = document.querySelector('input[name="phlebitis-type"]:checked');

    let productDefect = null;
    if (codes.has("PHARMACOVIGILANCE")) {
        productDefect = {
            productName: orNull(value("product-name")),
            manufacturer: orNull(value("manufacturer")),
            batch: orNull(value("batch")),
            expiryDate: orNull(value("expiry-date"))
        };
    } else if (codes.has("TECHNOVIGILANCE")) {
        productDefect = {
            equipment: orNull(value("equipment")),
            serialNumber: orNull(value("serial-number")),
            assetNumber: orNull(value("asset-number")),
            lastPreventiveMaintenance: orNull(value("maintenance-date"))
        };
    }

    return {
        sectorId: value("sector") ? Number(value("sector")) : null,
        eventDate: orNull(value("event-date")),
        incidentTypeIds: typeIds,
        medicationErrorStage: codes.has("MEDICATION_ERROR") && stage ? stage.value : null,
        phlebitisType: codes.has("PHLEBITIS") && phlebitis ? phlebitis.value : null,
        otherIncidentDescription: codes.has("OTHER") ? orNull(value("other-description")) : null,
        productDefect,
        nonconformity: orNull(value("nonconformity")),
        patientName: orNull(value("patient-name")),
        patientBirthDate: orNull(value("birth-date")),
        patientColor: orNull(value("patient-color")),
        visitReason: orNull(value("visit-reason")),
        medicalRecordNumber: orNull(value("medical-record")),
        attendanceNumber: orNull(value("attendance")),
        wardBed: orNull(value("ward-bed")),
        eventDescription: orNull(value("event-description")),
        immediateActions: orNull(value("immediate-actions"))
    };
}

// Mesmas regras do servidor, para avisar antes de enviar
function validate(req) {
    const codes = markedCodes();
    if (!req.sectorId) return "Selecione o setor da ocorrência.";
    if (!req.eventDate) return "Informe a data e hora do evento.";
    if (new Date(req.eventDate) > new Date()) return "A data do evento não pode ser futura.";
    if (req.incidentTypeIds.length === 0) return "Marque pelo menos um tipo de incidente.";
    if (codes.has("PHARMACOVIGILANCE") && codes.has("TECHNOVIGILANCE")) {
        return "Farmacovigilância e tecnovigilância devem ser notificadas em fichas separadas.";
    }
    if (codes.has("MEDICATION_ERROR") && !req.medicationErrorStage) return "Informe a fase do erro de medicação.";
    if (codes.has("PHLEBITIS") && !req.phlebitisType) return "Informe o tipo de flebite.";
    if (codes.has("OTHER") && !req.otherIncidentDescription) return "Descreva o incidente marcado como \"Outros\".";
    if (codes.has("PHARMACOVIGILANCE")) {
        const d = req.productDefect;
        if (!d.productName || !d.manufacturer || !d.batch || !d.expiryDate) {
            return "Preencha nome, fabricante, lote e validade do medicamento.";
        }
    }
    if (codes.has("TECHNOVIGILANCE") && !req.productDefect.equipment) return "Informe o equipamento.";
    if (!req.patientName) return "Preencha o nome do paciente.";
    if (req.patientBirthDate && req.patientBirthDate > req.eventDate.slice(0, 10)) {
        return "A data de nascimento não pode ser posterior à data do evento.";
    }
    if (!req.eventDescription) return "Descreva o evento.";
    return null;
}

async function onSubmit(event) {
    event.preventDefault();
    const errorEl = $("form-error");
    const button = $("submit-button");
    errorEl.textContent = "";

    const req = buildRequest();
    const problem = validate(req);
    if (problem) {
        errorEl.textContent = problem;
        showNotification(problem, "error");
        return;
    }

    const formData = new FormData();
    formData.append("data", new Blob([JSON.stringify(req)], { type: "application/json" }));
    selectedFiles.forEach(f => formData.append("files", f));

    button.disabled = true;
    try {
        const { ok, status, data } = await sendRequest("/api/notifications", "POST", formData);
        if (status === 401) return;
        if (!ok) {
            const message = apiErrorMessage(data, "Não foi possível enviar a notificação.");
            errorEl.textContent = message;
            showNotification(message, "error");
            return;
        }
        queueNotification(`Ficha nº ${data.id} registrada com sucesso.`, "success");
        window.location.href = "/fichas/detalhe?id=" + data.id;
    } catch (err) {
        const message = "Não foi possível conectar ao servidor.";
        errorEl.textContent = message;
        showNotification(message, "error");
    } finally {
        button.disabled = false;
    }
}

// ---------- Início ----------

(async function init() {
    const me = await initSession();
    if (!me) return;

    $("notifier").value = me.username + " (você)";

    // Não deixa escolher data/hora futura
    const now = new Date();
    const localNow = new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
    $("event-date").max = localNow;
    $("birth-date").max = localNow.slice(0, 10);

    try {
        const [sectors, types] = await Promise.all([
            fetchJSON("/api/sectors"),
            fetchJSON("/api/incident-types")
        ]);
        incidentTypes = types || [];

        const select = $("sector");
        (sectors || []).forEach(s => {
            const option = document.createElement("option");
            option.value = s.id;
            option.textContent = s.name;
            select.appendChild(option);
        });
        if (!sectors || sectors.length === 0) {
            showNotification("Nenhum setor cadastrado. Peça ao administrador para cadastrar os setores.", "info");
        }
        renderTypes();
    } catch (err) {
        showNotification(err.message, "error");
    }

    setupDropzone();
    $("notification-form").addEventListener("input", updateChecklist);
    $("notification-form").addEventListener("change", updateChecklist);
    $("notification-form").addEventListener("submit", onSubmit);
    updateChecklist();
})();
