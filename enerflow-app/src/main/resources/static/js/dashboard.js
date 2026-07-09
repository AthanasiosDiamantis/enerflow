/* =============================================================
   EnerFlow Dashboard – Vanilla JS
   Responsibilities:
     1. JWT lifecycle (login modal, logout, token storage)
     2. Poll GET /api/dashboard/status every 10 seconds
     3. Update all DOM elements and SVG animations
     4. Handle EnerFlow toggle switch
   ============================================================= */

'use strict';

// ── Constants ─────────────────────────────────────────────────
const API_STATUS_URL   = '/api/dashboard/status';
const API_LOGIN_URL    = '/api/auth/login';
const API_TOGGLE_URL   = '/api/enerflow/toggle';
const API_ADMIN_USERS_URL = '/api/admin/users';
const POLL_INTERVAL_MS = 10_000;
const TOKEN_KEY        = 'enerflow_jwt';
const API_DEVICE_CONFIG_URL = '/api/config/device';
const ROLE_KEY = 'enerflow_role';

// ── State ──────────────────────────────────────────────────────
let pollTimer      = null;
let toggleInFlight = false; // prevents double-click on toggle

// ── DOM references ─────────────────────────────────────────────

// Admin user management (role-gated)
const adminUsersSection    = document.getElementById('adminUsersSection');
const adminUsersTableBody  = document.getElementById('adminUsersTableBody');
const adminNewUsername     = document.getElementById('adminNewUsername');
const adminNewPassword     = document.getElementById('adminNewPassword');
const adminNewRole         = document.getElementById('adminNewRole');
const adminCreateUserBtn   = document.getElementById('adminCreateUserBtn');
const adminUserMsg         = document.getElementById('adminUserMsg');

const loginModal       = new bootstrap.Modal(document.getElementById('loginModal'));
const loginBtn         = document.getElementById('loginBtn');
const logoutBtn        = document.getElementById('logoutBtn');
const loginUsername    = document.getElementById('loginUsername');
const loginPassword    = document.getElementById('loginPassword');
const loginError       = document.getElementById('loginError');

// Metric cards
const elPvW            = document.getElementById('pvProductionW');
const elPvKw           = document.getElementById('pvProductionKw');
const elBatPercent     = document.getElementById('batteryChargePercent');
const elBatStatus      = document.getElementById('batteryStatus');
const elBatCard        = document.getElementById('batteryCard');
const elBatCardLabel   = document.getElementById('batteryCardLabel');
const elConsumptionW   = document.getElementById('consumptionW');
const elGridW          = document.getElementById('gridFeedInW');
const elGridDir        = document.getElementById('gridDirection');
const elGridCard       = document.getElementById('gridCard');
const elGridCardLabel  = document.getElementById('gridCardLabel');

// Heat pump card
const elHpActual       = document.getElementById('hotWaterTempActual');
const elHpSetpoint     = document.getElementById('hotWaterTempSetpoint');
const elHpStatus       = document.getElementById('heatPumpStatus');
const elBoostBadge     = document.getElementById('boostBadge');

// EnerFlow toggle
const elToggle         = document.getElementById('enerflowToggle');
const elToggleLabel    = document.getElementById('enerflowToggleLabel');

// Savings
const elSavedKwh       = document.getElementById('savedKwhToday');
const elSavedEuro      = document.getElementById('savedEuroToday');
const elShowers        = document.getElementById('possibleShowersToday');
const elPrice          = document.getElementById('electricityPrice');
const elPriceInput   = document.getElementById('electricityPriceInput');
const elSavePriceBtn = document.getElementById('savePriceBtn');
const elPriceSaveMsg = document.getElementById('priceSaveMsg');
const elPriceLastSaved = document.getElementById('priceLastSaved');

// Navbar
const elFreshness      = document.getElementById('freshnessIndicator');
const elLastUpdate     = document.getElementById('lastUpdateLabel');

// Manager configuration (role-gated)
const managerConfigSection = document.getElementById('managerConfigSection');
const cfgPvThreshold       = document.getElementById('cfgPvThreshold');
const cfgSocThreshold      = document.getElementById('cfgSocThreshold');
const cfgSetpointElevated  = document.getElementById('cfgSetpointElevated');
const cfgSetpointNormal    = document.getElementById('cfgSetpointNormal');
const cfgTankVolume        = document.getElementById('cfgTankVolume');
const cfgRetentionDays     = document.getElementById('cfgRetentionDays');
const saveConfigBtn        = document.getElementById('saveConfigBtn');
const cfgSaveMsg           = document.getElementById('cfgSaveMsg');

// SVG elements
const elSvgPvW         = document.getElementById('svgPvW');
const elSvgConsW       = document.getElementById('svgConsumptionW');
const elSvgGridW       = document.getElementById('svgGridW');
const elSvgSoc         = document.getElementById('svgSoc');
const elSvgHpTemp      = document.getElementById('svgHpTemp');
const elSvgHpStatus    = document.getElementById('svgHpStatus');
const elHeatpumpBorder = document.getElementById('heatpumpBorder');
const elBatteryBorder  = document.getElementById('batteryBorder');
const arrowPvHouse     = document.getElementById('arrowPvHouse');
const arrowPvHouseHead = document.getElementById('arrowPvHouseHead');
const arrowGrid        = document.getElementById('arrowGrid');
const arrowGridHead    = document.getElementById('arrowGridHead');
const arrowBattery     = document.getElementById('arrowBattery');
const arrowHeatpump    = document.getElementById('arrowHeatpump');
const arrowHeatpumpHead= document.getElementById('arrowHeatpumpHead');

// =============================================================
// 1. JWT LIFECYCLE
// =============================================================

function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

function saveToken(token) {
    localStorage.setItem(TOKEN_KEY, token);
}

function clearToken() {
    localStorage.removeItem(TOKEN_KEY);
}

function getRole() {
    return localStorage.getItem(ROLE_KEY);
}

function saveRole(role) {
    localStorage.setItem(ROLE_KEY, role);
}

function clearRole() {
    localStorage.removeItem(ROLE_KEY);
}

function applyRoleVisibility() {
    const role = getRole();
    const isManagerOrAdmin = role === 'ROLE_MANAGER' || role === 'ROLE_ADMIN';
    const isAdmin = role === 'ROLE_ADMIN';

    managerConfigSection.classList.toggle('d-none', !isManagerOrAdmin);
    adminUsersSection.classList.toggle('d-none', !isAdmin);

    if (isManagerOrAdmin) {
        loadDeviceConfig();
    }
    if (isAdmin) {
        loadUsers();
    }
}

async function loadDeviceConfig() {
    try {
        const resp = await fetch(API_DEVICE_CONFIG_URL, { headers: authHeaders() });
        if (!resp.ok) return;

        const data = await resp.json();
        cfgPvThreshold.value      = data.pvSurplusThresholdWatts;
        cfgSocThreshold.value     = data.batterySocThresholdPercent;
        cfgSetpointElevated.value = data.hotwaterSetpointElevatedCelsius;
        cfgSetpointNormal.value   = data.hotwaterSetpointNormalCelsius;
        cfgTankVolume.value       = data.hotwaterTankVolumeLiters;
        cfgRetentionDays.value    = data.snapshotRetentionDays;
    } catch (e) {
        console.error('Failed to load device config:', e);
    }
}

async function loadUsers() {
    try {
        const resp = await fetch(API_ADMIN_USERS_URL, { headers: authHeaders() });
        if (!resp.ok) return;

        const users = await resp.json();
        renderUsersTable(users);
    } catch (e) {
        console.error('Failed to load users:', e);
    }
}

function renderUsersTable(users) {
    adminUsersTableBody.innerHTML = users.map(u => {
        const statusBadge = !u.enabled
            ? '<span class="badge bg-secondary">Deaktiviert</span>'
            : u.locked
                ? '<span class="badge bg-danger">Gesperrt</span>'
                : '<span class="badge bg-success">Aktiv</span>';

        const created  = u.createdAt ? new Date(u.createdAt).toLocaleDateString('de-DE') : '–';
        const lastLogin = u.lastLogin ? new Date(u.lastLogin).toLocaleString('de-DE') : '– (noch nie)';

        return `
            <tr>
                <td>${escapeHtml(u.username)}</td>
                <td>${formatRole(u.role)}</td>
                <td>${statusBadge}</td>
                <td style="color:#8a94a0; font-size:0.85rem;">${created}</td>
                <td style="color:#8a94a0; font-size:0.85rem;">${lastLogin}</td>
            </tr>
        `;
    }).join('');
}

function formatRole(role) {
    const map = {
        'ROLE_USER':    'Hausbesitzer',
        'ROLE_MANAGER': 'Anlagenverwalter',
        'ROLE_ADMIN':   'Administrator'
    };
    return map[role] || role;
}

// Basic escaping to avoid the username breaking table markup
function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

function showAdminUserMsg(msg, cssClass) {
    adminUserMsg.textContent = msg;
    adminUserMsg.className   = cssClass;
    setTimeout(() => {
        adminUserMsg.textContent = '';
        adminUserMsg.className   = '';
    }, 4000);
}

adminCreateUserBtn.addEventListener('click', async () => {
    const username = adminNewUsername.value.trim();
    const password = adminNewPassword.value;
    const role     = adminNewRole.value;

    if (!username || !password) {
        showAdminUserMsg('Bitte Benutzername und Passwort ausfüllen.', 'text-danger');
        return;
    }

    adminCreateUserBtn.disabled = true;
    try {
        const resp = await fetch(API_ADMIN_USERS_URL, {
            method: 'POST',
            headers: authHeaders(),
            body: JSON.stringify({ username, password, role })
        });

        if (resp.status === 201) {
            showAdminUserMsg('✓ Benutzer angelegt', 'text-success');
            adminNewUsername.value = '';
            adminNewPassword.value = '';
            await loadUsers();
        } else if (resp.status === 409) {
            showAdminUserMsg('Benutzername bereits vergeben.', 'text-danger');
        } else if (resp.status === 400) {
            showAdminUserMsg('Ungültige Eingabe (Passwort min. 8 Zeichen?).', 'text-danger');
        } else {
            showAdminUserMsg('Fehler beim Anlegen.', 'text-danger');
        }
    } catch (e) {
        showAdminUserMsg('Server nicht erreichbar.', 'text-danger');
    } finally {
        adminCreateUserBtn.disabled = false;
    }
});

function showCfgMsg(msg, cssClass) {
    cfgSaveMsg.textContent = msg;
    cfgSaveMsg.className   = cssClass;
    setTimeout(() => {
        cfgSaveMsg.textContent = '';
        cfgSaveMsg.className   = '';
    }, 3000);
}

saveConfigBtn.addEventListener('click', async () => {
    const payload = {
        pvSurplusThresholdWatts: parseInt(cfgPvThreshold.value, 10),
        batterySocThresholdPercent: parseInt(cfgSocThreshold.value, 10),
        hotwaterSetpointElevatedCelsius: parseFloat(cfgSetpointElevated.value),
        hotwaterSetpointNormalCelsius: parseFloat(cfgSetpointNormal.value),
        hotwaterTankVolumeLiters: parseFloat(cfgTankVolume.value),
        snapshotRetentionDays: parseInt(cfgRetentionDays.value, 10)
    };

    saveConfigBtn.disabled = true;
    try {
        const resp = await fetch(API_DEVICE_CONFIG_URL, {
            method: 'PATCH',
            headers: authHeaders(),
            body: JSON.stringify(payload)
        });

        if (resp.ok) {
            showCfgMsg('✓ Gespeichert', 'text-success');
            await loadDeviceConfig();
        } else {
            showCfgMsg('Fehler: Bitte Eingaben prüfen.', 'text-danger');
        }
    } catch (e) {
        showCfgMsg('Server nicht erreichbar.', 'text-danger');
    } finally {
        saveConfigBtn.disabled = false;
    }
});

function authHeaders() {
    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${getToken()}`
    };
}

// ── Login ──────────────────────────────────────────────────────
loginBtn.addEventListener('click', async () => {
    const username = loginUsername.value.trim();
    const password = loginPassword.value.trim();

    if (!username || !password) {
        showLoginError('Bitte Benutzername und Passwort eingeben.');
        return;
    }

    try {
        const resp = await fetch(API_LOGIN_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (!resp.ok) {
            const body = await resp.json().catch(() => ({}));
            showLoginError(body.message || 'Anmeldung fehlgeschlagen.');
            return;
        }

        const data = await resp.json();
        saveToken(data.token);
        saveRole(data.role);
        loginError.classList.add('d-none');
        loginModal.hide();
        startPolling();
        applyRoleVisibility();

    } catch (e) {
        showLoginError('Server nicht erreichbar.');
    }
});

// Allow Enter key in password field
loginPassword.addEventListener('keydown', e => {
    if (e.key === 'Enter') loginBtn.click();
});

function showLoginError(msg) {
    loginError.textContent = msg;
    loginError.classList.remove('d-none');
}

// ── Logout ─────────────────────────────────────────────────────
logoutBtn.addEventListener('click', () => {
    stopPolling();
    clearToken();
    clearRole();
    resetAllDisplays();
    managerConfigSection.classList.add('d-none');
    adminUsersSection.classList.add('d-none');
    loginUsername.value = '';
    loginPassword.value = '';
    loginModal.show();
});

// =============================================================
// 2. POLLING
// =============================================================

function startPolling() {
    fetchAndUpdate();                               // immediate first call
    pollTimer = setInterval(fetchAndUpdate, POLL_INTERVAL_MS);
}

function stopPolling() {
    if (pollTimer) {
        clearInterval(pollTimer);
        pollTimer = null;
    }
}

async function fetchAndUpdate() {
    try {
        const resp = await fetch(API_STATUS_URL, { headers: authHeaders() });

        if (resp.status === 401) {
            stopPolling();
            clearToken();
            clearRole();
            resetAllDisplays();
            managerConfigSection.classList.add('d-none');
            adminUsersSection.classList.add('d-none');
            loginModal.show();
            return;
        }

        if (!resp.ok) {
            console.error('Dashboard API error:', resp.status);
            markStale();
            return;
        }

        const data = await resp.json();
        updateDashboard(data);

    } catch (e) {
        console.error('Fetch failed:', e);
        markStale();
    }
}

// =============================================================
// 3. DOM UPDATE
// =============================================================

function updateDashboard(d) {

    // ── PV ────────────────────────────────────────────────────
    elPvW.textContent  = formatWatts(d.pvProductionW);
    elPvKw.textContent = (d.pvProductionW / 1000).toFixed(2) + ' kW';
    elSvgPvW.textContent = formatWatts(d.pvProductionW);

    // ── Battery ───────────────────────────────────────────────
    const soc = d.batteryChargePercent;
    elBatPercent.textContent = soc + ' %';
    elSvgSoc.textContent     = soc + '%';

    // Color
    ['battery-green', 'battery-yellow', 'battery-red'].forEach(c =>
        elBatCard.classList.remove(c));
    const colorClass = 'battery-' + d.batteryColorCode;
    elBatCard.classList.add(colorClass);
    elBatCardLabel.className = 'small mb-1 ' + colorClass;
    elBatPercent.className   = 'fs-4 fw-bold ' + colorClass;

    // SVG battery border
    elBatteryBorder.setAttribute('stroke',
        d.batteryColorCode === 'green'  ? '#198754' :
            d.batteryColorCode === 'yellow' ? '#ffc107' : '#dc3545');

    // Charging / discharging status text
    elBatStatus.textContent =
        d.batteryCharging    ? '↑ Lädt' :
            d.batteryDischarging ? '↓ Entlädt' : 'Ruhezustand';

    // SVG battery arrow
    arrowBattery.className.baseVal = 'flow-line ' + (
        d.batteryCharging    ? 'flow-line-battery-charge' :
            d.batteryDischarging ? 'flow-line-battery-discharge' : '');

    // ── Consumption ───────────────────────────────────────────
    elConsumptionW.textContent    = formatWatts(d.consumptionW);
    elSvgConsW.textContent        = formatWatts(d.consumptionW);

    // ── Grid ──────────────────────────────────────────────────
    const grid = d.gridFeedInW;
    elGridW.textContent = formatWatts(Math.abs(grid));
    elSvgGridW.textContent = formatWatts(Math.abs(grid));

    elGridCard.classList.remove('grid-feedin', 'grid-drawing');
    if (grid > 0) {
        elGridCard.classList.add('grid-feedin');
        elGridCardLabel.textContent = '⚡ Netz';
        elGridDir.textContent       = '↑ Einspeisung';
        elGridW.className           = 'fs-4 fw-bold text-success';
        arrowGrid.className.baseVal = 'flow-line flow-line-grid-feedin';
        arrowGridHead.setAttribute('fill', '#198754');
    } else {
        elGridCard.classList.add('grid-drawing');
        elGridCardLabel.textContent = '⚡ Netz';
        elGridDir.textContent       = '↓ Netzbezug';
        elGridW.className           = 'fs-4 fw-bold text-danger';
        arrowGrid.className.baseVal = 'flow-line flow-line-grid-drawing';
        arrowGridHead.setAttribute('fill', '#dc3545');
    }

    // PV → House arrow (active when PV produces)
    arrowPvHouse.className.baseVal = 'flow-line ' +
        (d.pvProductionW > 0 ? 'flow-line-active' : '');
    arrowPvHouseHead.setAttribute('fill',
        d.pvProductionW > 0 ? '#ffc107' : '#3a3a3a');

    // ── Heat Pump ─────────────────────────────────────────────
    elHpActual.textContent    = d.hotWaterTempActual.toFixed(1)    + ' °C';
    elHpSetpoint.textContent  = d.hotWaterTempSetpoint.toFixed(1)  + ' °C';
    elSvgHpTemp.textContent   = d.hotWaterTempActual.toFixed(1)    + '°C';

    const hpStatusText = d.heatPumpStatus || 'Standby';
    elHpStatus.textContent    = hpStatusText;
    elSvgHpStatus.textContent = hpStatusText;

    // Heat pump arrow – active when boost is on
    const hpArrowColor = d.boostActive ? '#dc3545' : '#6c757d';
    arrowHeatpump.setAttribute('stroke', hpArrowColor);
    arrowHeatpumpHead.setAttribute('fill', hpArrowColor);

    // Boost badge + SVG pulse (US-01-02)
    elHeatpumpBorder.className.baseVal = '';
    if (d.boostActive) {
        elBoostBadge.textContent  = '🔥 Boost aktiv';
        elBoostBadge.className    = 'badge bg-danger';
        elHeatpumpBorder.classList.add('heatpump-pulse-red');
    } else if (d.enerflowActive) {
        elBoostBadge.textContent  = '✅ EnerFlow aktiv';
        elBoostBadge.className    = 'badge bg-warning text-dark';
        elHeatpumpBorder.classList.add('heatpump-pulse-orange');
    } else {
        elBoostBadge.textContent  = 'Manuell';
        elBoostBadge.className    = 'badge bg-secondary';
    }

    // ── EnerFlow Toggle ───────────────────────────────────────
    if (!toggleInFlight) {
        elToggle.checked          = d.enerflowActive;
        elToggleLabel.textContent = d.enerflowActive ? 'Aktiv' : 'Inaktiv';
        elToggleLabel.className   = 'form-check-label fs-6 ms-2 ' +
            (d.enerflowActive ? 'text-success' : 'text-muted');
    }

    // ── Savings ───────────────────────────────────────────────
    elSavedKwh.textContent  = d.savedKwhToday.toFixed(2);
    elSavedEuro.textContent = d.savedEuroToday.toFixed(2) + ' €';
    elShowers.textContent   = d.possibleShowersToday;
    if (document.activeElement !== elPriceInput) {
        elPriceInput.value = d.electricityPriceCentPerKwh.toFixed(1);
    }

    // ── Freshness & Timestamp ─────────────────────────────────
    const fresh = d.dataFreshness === 'FRESH';
    elFreshness.textContent = fresh ? '●  Live' : '●  Veraltet';
    elFreshness.className   = 'badge ' +
        (fresh ? 'freshness-fresh' : 'freshness-stale');

    const ts = new Date(d.lastSnapshotTime);
    elLastUpdate.textContent = 'Zuletzt: ' + ts.toLocaleTimeString('de-DE');
}

// =============================================================
// 4. ENERFLOW TOGGLE
// =============================================================

elToggle.addEventListener('change', async () => {
    if (toggleInFlight) return;
    toggleInFlight = true;
    elToggle.disabled = true;

    try {
        const resp = await fetch(API_TOGGLE_URL, {
            method: 'PATCH',
            headers: authHeaders()
        });

        if (!resp.ok) {
            console.error('Toggle failed:', resp.status);
            // Revert the visual toggle since the server rejected it
            elToggle.checked = !elToggle.checked;
        }
        // Next poll will sync the correct state from server
    } catch (e) {
        console.error('Toggle error:', e);
        elToggle.checked = !elToggle.checked;
    } finally {
        toggleInFlight  = false;
        elToggle.disabled = false;
    }
});

// =============================================================
// 5. HELPERS
// =============================================================

function formatWatts(w) {
    return w >= 1000
        ? (w / 1000).toFixed(2) + ' kW'
        : w + ' W';
}

function markStale() {
    elFreshness.textContent = '●  Keine Verbindung';
    elFreshness.className   = 'badge freshness-stale';
}

function resetAllDisplays() {
    const fields = [
        elPvW, elPvKw, elBatPercent, elBatStatus,
        elConsumptionW, elGridW, elGridDir,
        elHpActual, elHpSetpoint, elHpStatus,
        elSavedKwh, elSavedEuro, elShowers, elPrice,
        elLastUpdate, elToggleLabel
    ];
    fields.forEach(el => { if (el) el.textContent = '–'; });
    elFreshness.textContent = '●  –';
    elFreshness.className   = 'badge bg-secondary';
}

// =============================================================
// 6. INIT
// =============================================================

(function init() {
    if (getToken()) {
        startPolling();
        loadPriceTimestamp();
        applyRoleVisibility();
    } else {
        loginModal.show();
    }
    // =============================================================
// 7. ELECTRICITY PRICE SAVE (US-01-04)
// =============================================================

    elSavePriceBtn.addEventListener('click', async () => {
        const newPrice = parseFloat(elPriceInput.value);

        if (isNaN(newPrice) || newPrice <= 0) {
            showPriceMsg('Ungültiger Wert.', 'text-danger');
            return;
        }

        elSavePriceBtn.disabled = true;

        try {
            const resp = await fetch(
                `/api/config/electricity-price?price=${newPrice}`,
                { method: 'PATCH', headers: authHeaders() }
            );

            if (resp.ok) {
                showPriceMsg('✓ Gespeichert', 'text-success');
                await loadPriceTimestamp();
            } else {
                showPriceMsg('Fehler beim Speichern.', 'text-danger');
            }
        } catch (e) {
            showPriceMsg('Server nicht erreichbar.', 'text-danger');
        } finally {
            elSavePriceBtn.disabled = false;
        }
    });

    function showPriceMsg(msg, cssClass) {
        elPriceSaveMsg.textContent  = msg;
        elPriceSaveMsg.className    = cssClass;
        // Auto-hide after 3 seconds
        setTimeout(() => {
            elPriceSaveMsg.textContent = '';
            elPriceSaveMsg.className   = '';
        }, 3000);
    }

    async function loadPriceTimestamp() {
        try {
            const resp = await fetch('/api/config/electricity-price',
                { headers: authHeaders() });
            if (!resp.ok) return;

            const data = await resp.json();
            const price = parseFloat(data.priceCentPerKwh).toFixed(2).replace('.', ',');
            const dt    = new Date(data.updatedAt);
            const date  = dt.toLocaleDateString('de-DE',
                { day: '2-digit', month: '2-digit', year: 'numeric' });
            const time  = dt.toLocaleTimeString('de-DE',
                { hour: '2-digit', minute: '2-digit' });

            elPriceLastSaved.textContent = `🕐 ${price} ct/kWh · ${date}, ${time} Uhr`;
        } catch (e) {
            // silently ignore
        }
    }

})();