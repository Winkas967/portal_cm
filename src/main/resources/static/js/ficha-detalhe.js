// Tela "Detalhe da ficha": mostra a ficha completa (somente leitura) e os anexos.

const STAGE_LABELS = { PRESCRICAO: "Prescrição", DISPENSACAO: "Dispensação", ADMINISTRACAO: "Administração" };
const PHLEBITIS_LABELS = { QUIMICA: "Química", MECANICA: "Mecânica", INFECCIOSA: "Infecciosa" };

const $ = id => document.getElementById(id);

function textOrEmpty(el, text, emptyLabel) {
    if (text) {
        el.className = "text-block";
        el.textContent = text;
    } else {
        el.className = "text-empty";
        el.textContent = emptyLabel || "Não informado";
    }
}

// Monta uma lista <dt>/<dd>, pulando os itens vazios
function renderKv(el, pairs) {
    const rows = pairs.filter(([, v]) => v);
    el.innerHTML = rows.length
        ? rows.map(([k, v]) => `<dt>${escapeHtml(k)}</dt><dd>${escapeHtml(v)}</dd>`).join("")
        : '<dd class="text-empty" style="grid-column: 1 / -1;">Nenhuma informação adicional.</dd>';
}

function ageAt(birthIso, eventIso) {
    if (!birthIso || !eventIso) return null;
    const [by, bm, bd] = birthIso.split("-").map(Number);
    const event = new Date(eventIso);
    let age = event.getFullYear() - by;
    if (event.getMonth() + 1 < bm || (event.getMonth() + 1 === bm && event.getDate() < bd)) age--;
    return age;
}

function render(n) {
    document.title = `Ficha nº ${n.id} — Portal CM`;
    $("crumb").textContent = `Ficha nº ${n.id}`;
    $("patient-title").textContent = n.patientName;
    $("patient-subtitle").textContent = [
        n.medicalRecordNumber && "Prontuário " + n.medicalRecordNumber,
        n.attendanceNumber && "Atendimento " + n.attendanceNumber,
        n.wardBed
    ].filter(Boolean).join(" · ");

    $("detail-number").textContent = "Nº " + n.id;
    $("meta-sector").textContent = n.sector.name;
    $("meta-event").textContent = formatDateTime(n.eventDate);
    $("meta-notified").textContent = formatDateTime(n.notificationDate);
    $("meta-notifier").textContent = n.notifier.name;

    // Tipos, com o complemento de fase/tipo de flebite na própria etiqueta
    $("detail-types").innerHTML = n.incidentTypes.map(t => {
        let label = t.name;
        if (t.code === "MEDICATION_ERROR" && n.medicationErrorStage) label += " — " + STAGE_LABELS[n.medicationErrorStage];
        if (t.code === "PHLEBITIS" && n.phlebitisType) label += " — " + PHLEBITIS_LABELS[n.phlebitisType];
        return `<span class="chip ${chipClass(t.code)}">${escapeHtml(label)}</span>`;
    }).join("");

    if (n.otherIncidentDescription) {
        $("detail-other").classList.remove("hidden");
        $("detail-other-text").textContent = n.otherIncidentDescription;
    }

    textOrEmpty($("detail-description"), n.eventDescription);
    textOrEmpty($("detail-actions"), n.immediateActions);
    textOrEmpty($("detail-nonconformity"), n.nonconformity, "Nenhuma não conformidade informada");

    // Seção 2: farmaco ou tecnovigilância
    const d = n.productDefect;
    if (d) {
        $("defect-panel").classList.remove("hidden");
        if (d.kind === "MEDICAMENTO") {
            $("defect-title").textContent = "Farmacovigilância";
            $("defect-chip").textContent = "Medicamento";
            $("defect-chip").className = "chip farmaco";
            renderKv($("defect-list"), [
                ["Produto", d.productName],
                ["Fabricante", d.manufacturer],
                ["Lote", d.batch],
                ["Validade", d.expiryDate && formatDate(d.expiryDate)]
            ]);
        } else {
            $("defect-title").textContent = "Tecnovigilância";
            $("defect-chip").textContent = "Equipamento";
            $("defect-chip").className = "chip tecno";
            renderKv($("defect-list"), [
                ["Equipamento", d.equipment],
                ["Nº de série", d.serialNumber],
                ["Patrimônio", d.assetNumber],
                ["Manutenção preventiva", d.lastPreventiveMaintenance && formatDate(d.lastPreventiveMaintenance)]
            ]);
        }
    }

    const age = ageAt(n.patientBirthDate, n.eventDate);
    renderKv($("patient-list"), [
        ["Nascimento", n.patientBirthDate && formatDate(n.patientBirthDate) + (age !== null ? ` (${age} anos)` : "")],
        ["Cor", n.patientColor],
        ["Prontuário", n.medicalRecordNumber],
        ["Atendimento", n.attendanceNumber],
        ["Enfermaria / leito", n.wardBed],
        ["Motivo", n.visitReason]
    ]);

    // Anexos: o link passa pela API, que confere a permissão antes de entregar o arquivo
    $("attach-count").textContent = n.attachments.length === 1 ? "1 arquivo" : n.attachments.length + " arquivos";
    $("attach-list").innerHTML = n.attachments.length
        ? n.attachments.map(a => `
            <div class="attach-row">
                <div class="file-icon ${fileKind(a.contentType)}">${fileLabel(a.contentType)}</div>
                <div><div class="file-name">${escapeHtml(a.originalName)}</div><div class="file-size">${formatFileSize(a.sizeBytes)}</div></div>
                <a class="btn-link" href="${a.downloadUrl}" target="_blank" rel="noopener">Abrir</a>
            </div>`).join("")
        : '<div class="text-empty">Nenhum anexo.</div>';

    $("detail").classList.remove("hidden");
}

$("print-button").addEventListener("click", () => window.print());

(async function init() {
    const me = await initSession();
    if (!me) return;

    const id = new URLSearchParams(window.location.search).get("id");
    if (!id || !/^\d+$/.test(id)) {
        queueNotification("Ficha não informada.", "error");
        window.location.href = "/fichas";
        return;
    }

    try {
        const notification = await fetchJSON("/api/notifications/" + id);
        if (notification) render(notification);
    } catch (err) {
        queueNotification(err.message, "error");
        window.location.href = "/fichas";
    }
})();
