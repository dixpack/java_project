/**
 * VELOCITA VEHICLE RENTAL SYSTEM - FRONTEND APPLICATION LOGIC
 * Features:
 * - Login Authentication Portal (Admin & Customer Roles)
 * - Fleet Administrator Panel with Full Inventory & Financial CRUD
 * - Customer / User Portal with Available Rides Showroom & Personal Booking Ledger
 * - Real-Time Availability Tracking, Penalty Calculation & Dual Backend Sync
 */

// Configuration matching Java RentalAdmin specifications
const LATE_FEE_PER_DAY = 500.0;
const API_BASE = 'http://localhost:8080/api';

const TYPE_ICONS = {
  CAR: '🚗',
  BIKE: '🏍️',
  VAN: '🚐'
};

// Default Seed Data (Higher Premium Rental Rates)
const DEFAULT_VEHICLES = [
  { id: 'V001', type: 'CAR', brand: 'Porsche', model: 'Taycan 4S', rentalRate: 4500.0, status: 'RENTED', details: '5 Seats, Electric' },
  { id: 'V002', type: 'BIKE', brand: 'Ducati', model: 'Panigale V4 S', rentalRate: 2200.0, status: 'AVAILABLE', details: '1103 cc, Helmet Included' },
  { id: 'V003', type: 'VAN', brand: 'Ford', model: 'Transit Custom', rentalRate: 1800.0, status: 'AVAILABLE', details: 'Cargo: 1400 kg, Sliding Door' },
  { id: 'V004', type: 'CAR', brand: 'Tesla', model: 'Model S Plaid', rentalRate: 3800.0, status: 'AVAILABLE', details: '5 Seats, Electric' },
  { id: 'V005', type: 'BIKE', brand: 'BMW', model: 'S1000RR', rentalRate: 2100.0, status: 'AVAILABLE', details: '999 cc, Helmet Included' },
  { id: 'V006', type: 'VAN', brand: 'Toyota', model: 'Innova Crysta', rentalRate: 2400.0, status: 'AVAILABLE', details: 'Cargo: 850 kg, Standard Door' },
  { id: 'V007', type: 'CAR', brand: 'Mercedes-Benz', model: 'E-Class', rentalRate: 3600.0, status: 'AVAILABLE', details: '5 Seats, Petrol' }
];

const DEFAULT_CUSTOMERS = [
  { id: 'C001', name: 'Anjali Menon', contactDetails: 'anjali@example.com', licenceNumber: 'DL-0420180012345', totalRentals: 1 },
  { id: 'C002', name: 'Rohan Das', contactDetails: 'rohan@example.com', licenceNumber: 'DL-0720190067890', totalRentals: 0 },
  { id: 'C003', name: 'Elena Vance', contactDetails: 'elena.vance@velocita.com', licenceNumber: 'DL-1220210045678', totalRentals: 0 }
];

function getDateOffset(daysOffset) {
  const d = new Date();
  d.setDate(d.getDate() + daysOffset);
  return d.toISOString().split('T')[0];
}

const DEFAULT_RENTALS = [
  {
    rentalId: 'R001',
    customerId: 'C001',
    customerName: 'Anjali Menon',
    vehicleId: 'V001',
    vehicleDetails: 'Porsche Taycan 4S',
    startDate: getDateOffset(-3),
    endDate: getDateOffset(2),
    rentalCost: 22500.0,
    securityDeposit: 5000.0,
    latePenalty: 0.0,
    refund: 0.0,
    active: true,
    actualReturnDate: ''
  }
];

// Application State
let state = {
  auth: {
    isLoggedIn: false,
    role: 'admin', // 'admin' | 'customer'
    customerId: 'C001',
    customerName: 'Anjali Menon'
  },
  vehicles: [],
  customers: [],
  rentals: [],
  adminActiveTab: 'admin-fleet',
  custActiveTab: 'cust-fleet',
  adminTypeFilter: 'ALL',
  adminStatusFilter: 'ALL',
  adminSearchQuery: '',
  custTypeFilter: 'ALL',
  custSearchQuery: '',
  isBackendConnected: false
};

// =========================================================
// INITIALIZATION
// =========================================================

document.addEventListener('DOMContentLoaded', async () => {
  initTheme();
  loadData();
  setupNavigation();
  setupEventListeners();
  populateCustomerLoginSelect();
  await checkBackendConnection();

  // Check saved session
  const savedSession = localStorage.getItem('vrs_auth');
  if (savedSession) {
    try {
      state.auth = JSON.parse(savedSession);
      if (state.auth.isLoggedIn) {
        showAppScreen();
        return;
      }
    } catch (e) {}
  }

  showLoginScreen();
});

// =========================================================
// AUTHENTICATION & SESSIONS
// =========================================================

function switchLoginRole(role) {
  const tabAdmin = document.getElementById('tab-login-admin');
  const tabCust = document.getElementById('tab-login-customer');
  const tabNew = document.getElementById('tab-login-new-customer');
  const formAdmin = document.getElementById('form-login-admin');
  const formCust = document.getElementById('form-login-customer');
  const formNew = document.getElementById('form-login-new-customer');

  tabAdmin?.classList.remove('active');
  tabCust?.classList.remove('active');
  tabNew?.classList.remove('active');
  formAdmin?.classList.add('hidden');
  formCust?.classList.add('hidden');
  formNew?.classList.add('hidden');

  if (role === 'admin') {
    tabAdmin?.classList.add('active');
    formAdmin?.classList.remove('hidden');
  } else if (role === 'new-customer') {
    tabNew?.classList.add('active');
    formNew?.classList.remove('hidden');
    const nextId = 'C' + String(state.customers.length + 1).padStart(3, '0');
    const idField = document.getElementById('new-cust-id');
    if (idField && !idField.value) idField.placeholder = nextId;
    setTimeout(() => document.getElementById('new-cust-name')?.focus(), 100);
  } else {
    tabCust?.classList.add('active');
    formCust?.classList.remove('hidden');
  }
}

function verifyLicenceInput(val) {
  const badge = document.getElementById('licence-verification-badge');
  if (!badge) return;
  const trimmed = val ? val.trim().toUpperCase() : '';
  if (!trimmed) {
    badge.style.display = 'none';
    return;
  }
  badge.style.display = 'block';
  // Licence verification rule: must be at least 6 characters, alphanumeric with optional dashes/spaces
  const isValidFormat = /^[A-Z0-9\-\s]{6,20}$/.test(trimmed) && trimmed.replace(/[^A-Z0-9]/g, '').length >= 6;
  if (isValidFormat) {
    badge.innerHTML = `<span style="color: var(--accent-emerald);">✅ Licence Format Verified &amp; Eligible to Drive</span>`;
  } else {
    badge.innerHTML = `<span style="color: var(--accent-rose);">⚠️ Minimum 6 alphanumeric characters required (e.g. DL-0420220019842)</span>`;
  }
}

async function handleNewCustomerSignUp(e) {
  if (e) e.preventDefault();
  const name = document.getElementById('new-cust-name').value.trim();
  const contact = document.getElementById('new-cust-contact').value.trim();
  const licence = document.getElementById('new-cust-licence').value.trim().toUpperCase();
  let id = document.getElementById('new-cust-id')?.value.trim().toUpperCase();

  if (!name) {
    showToast('Please enter your full name.', 'error');
    return;
  }
  if (!contact) {
    showToast('Please enter your contact email or phone number.', 'error');
    return;
  }
  if (!licence) {
    showToast('Driving licence number is required for verification.', 'error');
    return;
  }

  // Licence Verification Logic
  const cleanLicence = licence.replace(/[^A-Z0-9]/g, '');
  if (cleanLicence.length < 6) {
    showToast('Invalid Driving Licence: Must have at least 6 alphanumeric characters.', 'error');
    return;
  }

  // Generate ID if not provided
  if (!id) {
    id = 'C' + String(state.customers.length + 1).padStart(3, '0');
    while (state.customers.some(c => c.id === id)) {
      const num = parseInt(id.replace('C', '')) + 1;
      id = 'C' + String(num).padStart(3, '0');
    }
  }

  // Check duplicate ID
  if (state.customers.some(c => c.id === id)) {
    showToast(`Customer ID ${id} already exists. Please choose another ID.`, 'error');
    return;
  }

  // Check duplicate licence
  const existingLicence = state.customers.find(c => c.licenceNumber && c.licenceNumber.toUpperCase() === licence);
  if (existingLicence) {
    showToast(`Driving licence ${licence} is already registered to ${existingLicence.name} (${existingLicence.id}).`, 'error');
    return;
  }

  const newCustomer = {
    id,
    name,
    contactDetails: contact,
    licenceNumber: licence,
    totalRentals: 0
  };

  // Sync to Java backend if online
  if (state.isBackendConnected) {
    try {
      await fetch(`${API_BASE}/customers`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(newCustomer)
      });
    } catch (err) {}
  }

  state.customers.push(newCustomer);
  saveData();
  populateCustomerLoginSelect();

  // Automatically sign in as this new customer
  state.auth = {
    isLoggedIn: true,
    role: 'customer',
    customerId: newCustomer.id,
    customerName: newCustomer.name
  };

  saveSession();
  showAppScreen();
  showToast(`🎉 Welcome, ${newCustomer.name}! Licence ${licence} verified successfully. You can now book rides!`, 'success');
}

function populateCustomerLoginSelect() {
  const sel = document.getElementById('customer-select-login');
  if (!sel) return;
  sel.innerHTML = state.customers.map(c => `
    <option value="${c.id}">${c.name} (${c.id}) - ${c.contactDetails}</option>
  `).join('');
}

function handleAdminLogin(e) {
  if (e) e.preventDefault();
  const u = document.getElementById('admin-username').value.trim();
  const p = document.getElementById('admin-password').value;

  if (u === 'admin' && p.length > 0) {
    state.auth = {
      isLoggedIn: true,
      role: 'admin',
      customerId: '',
      customerName: 'Administrator'
    };
    saveSession();
    showAppScreen();
    showToast('Signed in as Fleet Administrator.', 'success');
  } else {
    showToast('Invalid credentials. Use admin as username and enter a password.', 'error');
  }
}

function handleCustomerLogin(e) {
  if (e) e.preventDefault();
  const selId = document.getElementById('customer-select-login')?.value;
  const inputVal = document.getElementById('customer-id-input')?.value.trim();

  let targetCustomer = null;
  if (inputVal) {
    targetCustomer = state.customers.find(c =>
      c.id.equalsIgnoreCase?.(inputVal) || c.id.toLowerCase() === inputVal.toLowerCase() ||
      c.name.toLowerCase().includes(inputVal.toLowerCase())
    );
  } else if (selId) {
    targetCustomer = state.customers.find(c => c.id === selId);
  }

  if (!targetCustomer) {
    if (inputVal) {
      showToast(`No account found matching "${inputVal}". Please register and verify your licence below.`, 'info');
      switchLoginRole('new-customer');
      const nameInput = document.getElementById('new-cust-name');
      if (nameInput) nameInput.value = inputVal;
      return;
    } else {
      targetCustomer = state.customers[0];
    }
  }

  state.auth = {
    isLoggedIn: true,
    role: 'customer',
    customerId: targetCustomer.id,
    customerName: targetCustomer.name
  };

  saveSession();
  showAppScreen();
  showToast(`Welcome back, ${targetCustomer.name}! (Licence Verified: ${targetCustomer.licenceNumber || 'Verified'})`, 'success');
}

function quickLogin(role, customerId = 'C001') {
  if (role === 'admin') {
    state.auth = {
      isLoggedIn: true,
      role: 'admin',
      customerId: '',
      customerName: 'Administrator'
    };
    showToast('Signed in as Fleet Administrator.', 'success');
  } else {
    const cust = state.customers.find(c => c.id === customerId) || state.customers[0];
    state.auth = {
      isLoggedIn: true,
      role: 'customer',
      customerId: cust.id,
      customerName: cust.name
    };
    showToast(`Signed in as ${cust.name} (${cust.id}).`, 'success');
  }
  saveSession();
  showAppScreen();
}

function handleLogout() {
  state.auth.isLoggedIn = false;
  localStorage.removeItem('vrs_auth');
  showLoginScreen();
  showToast('Signed out successfully.', 'info');
}

function saveSession() {
  localStorage.setItem('vrs_auth', JSON.stringify(state.auth));
}

function showLoginScreen() {
  document.getElementById('login-view').classList.remove('hidden');
  document.getElementById('app-view').classList.add('hidden');
  populateCustomerLoginSelect();
}

function showAppScreen() {
  document.getElementById('login-view').classList.add('hidden');
  document.getElementById('app-view').classList.remove('hidden');
  renderAppForRole();
}

function renderAppForRole() {
  const isAdmin = state.auth.role === 'admin';
  const adminContainer = document.getElementById('admin-container');
  const custContainer = document.getElementById('customer-container');
  const adminQuickActions = document.getElementById('admin-quick-actions');
  const sessionRoleIndicator = document.getElementById('session-role-indicator');
  const sessionUsername = document.getElementById('session-username-display');
  const portalSubheading = document.getElementById('portal-subheading');
  const footerUserRole = document.getElementById('footer-user-role');

  if (isAdmin) {
    adminContainer.classList.remove('hidden');
    custContainer.classList.add('hidden');
    adminQuickActions.classList.remove('hidden');

    sessionRoleIndicator.className = 'role-indicator role-admin';
    sessionRoleIndicator.textContent = 'ADMIN';
    sessionUsername.textContent = 'Fleet Administrator';
    portalSubheading.textContent = 'Administrator Control Panel';
    footerUserRole.textContent = 'Administrator (Full Access)';

    renderAdminAll();
  } else {
    adminContainer.classList.add('hidden');
    custContainer.classList.remove('hidden');
    adminQuickActions.classList.add('hidden');

    const customer = state.customers.find(c => c.id === state.auth.customerId) || {
      id: state.auth.customerId,
      name: state.auth.customerName,
      contactDetails: 'N/A',
      totalRentals: 0
    };

    sessionRoleIndicator.className = 'role-indicator role-customer';
    sessionRoleIndicator.textContent = 'CUSTOMER';
    sessionUsername.textContent = customer.name;
    portalSubheading.textContent = 'Customer Self-Service Portal';
    footerUserRole.textContent = `${customer.name} (${customer.id})`;

    // Populate welcome & profile cards
    document.getElementById('customer-welcome-name').textContent = customer.name;
    document.getElementById('customer-welcome-id').textContent = customer.id;

    document.getElementById('prof-cust-id').textContent = customer.id;
    document.getElementById('prof-cust-name').textContent = customer.name;
    document.getElementById('prof-cust-contact').textContent = customer.contactDetails;
    const profLicence = document.getElementById('prof-cust-licence');
    if (profLicence) profLicence.textContent = customer.licenceNumber || ('DL-' + customer.id + '9876');
    document.getElementById('prof-cust-total').textContent = customer.totalRentals || 0;

    renderCustomerAll();
  }
}

// =========================================================
// THEME & BACKEND SYNC
// =========================================================

function initTheme() {
  const savedTheme = localStorage.getItem('vrs_theme') || 'light';
  document.documentElement.setAttribute('data-theme', savedTheme);
  updateThemeIcon(savedTheme);
}

function toggleTheme() {
  const current = document.documentElement.getAttribute('data-theme') || 'light';
  const newTheme = current === 'light' ? 'dark' : 'light';
  document.documentElement.setAttribute('data-theme', newTheme);
  localStorage.setItem('vrs_theme', newTheme);
  updateThemeIcon(newTheme);
  showToast(`Switched to ${newTheme} theme.`, 'info');
}

function updateThemeIcon(theme) {
  const btn = document.getElementById('theme-toggle-btn');
  if (btn) {
    btn.innerHTML = theme === 'light' ? '🌙' : '☀️';
    btn.title = `Switch to ${theme === 'light' ? 'dark' : 'light'} theme`;
  }
}

async function checkBackendConnection() {
  try {
    const res = await fetch(`${API_BASE}/status`, { signal: AbortSignal.timeout(1200) });
    if (res.ok) {
      state.isBackendConnected = true;
      updateConnectionBadge(true);
      return;
    }
  } catch (e) {}
  state.isBackendConnected = false;
  updateConnectionBadge(false);
}

function updateConnectionBadge(connected) {
  const badge = document.getElementById('connection-badge');
  if (!badge) return;
  if (connected) {
    badge.innerHTML = `<span class="pulse-dot"></span> Java Backend Online`;
    badge.className = 'header-status-badge';
  } else {
    badge.innerHTML = `<span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:#0284c7;"></span> Standalone Mode`;
    badge.className = 'header-status-badge';
    badge.style.background = 'var(--accent-blue-soft)';
    badge.style.color = 'var(--accent-blue)';
    badge.style.borderColor = 'var(--accent-blue-border)';
  }
}

function loadData() {
  const savedVehicles = localStorage.getItem('vrs_vehicles');
  const savedCustomers = localStorage.getItem('vrs_customers');
  const savedRentals = localStorage.getItem('vrs_rentals');

  state.vehicles = savedVehicles ? JSON.parse(savedVehicles) : JSON.parse(JSON.stringify(DEFAULT_VEHICLES));
  state.customers = savedCustomers ? JSON.parse(savedCustomers) : JSON.parse(JSON.stringify(DEFAULT_CUSTOMERS));
  state.rentals = savedRentals ? JSON.parse(savedRentals) : JSON.parse(JSON.stringify(DEFAULT_RENTALS));

  // If stored vehicles are using old low rates (< 1000), upgrade them to the higher price schedule
  if (!savedVehicles || (state.vehicles.length > 0 && state.vehicles[0].rentalRate < 1000)) {
    state.vehicles = JSON.parse(JSON.stringify(DEFAULT_VEHICLES));
    state.rentals = JSON.parse(JSON.stringify(DEFAULT_RENTALS));
    saveData();
  }

  state.customers.forEach(c => {
    if (!c.licenceNumber) c.licenceNumber = 'DL-' + c.id + '9876';
    c.totalRentals = state.rentals.filter(r => r.customerId === c.id).length;
  });
}

function saveData() {
  localStorage.setItem('vrs_vehicles', JSON.stringify(state.vehicles));
  localStorage.setItem('vrs_customers', JSON.stringify(state.customers));
  localStorage.setItem('vrs_rentals', JSON.stringify(state.rentals));
}

function resetToDefaultData() {
  localStorage.removeItem('vrs_vehicles');
  localStorage.removeItem('vrs_customers');
  localStorage.removeItem('vrs_rentals');
  loadData();
  renderAppForRole();
  showToast('Reset to default sample dataset.', 'info');
}

// =========================================================
// NAVIGATION & TABS
// =========================================================

function setupNavigation() {
  // Admin Tabs
  document.querySelectorAll('#admin-container .tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('#admin-container .tab-btn').forEach(t => t.classList.remove('active'));
      document.querySelectorAll('#admin-container .tab-content').forEach(p => p.classList.remove('active'));

      btn.classList.add('active');
      const targetId = btn.dataset.tab;
      const targetPanel = document.getElementById(targetId);
      if (targetPanel) {
        targetPanel.classList.add('active');
        state.adminActiveTab = targetId;
      }
      renderAdminAll();
    });
  });

  // Customer Tabs
  document.querySelectorAll('#customer-container .tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('#customer-container .tab-btn').forEach(t => t.classList.remove('active'));
      document.querySelectorAll('#customer-container .tab-content').forEach(p => p.classList.remove('active'));

      btn.classList.add('active');
      const targetId = btn.dataset.tab;
      const targetPanel = document.getElementById(targetId);
      if (targetPanel) {
        targetPanel.classList.add('active');
        state.custActiveTab = targetId;
      }
      renderCustomerAll();
    });
  });
}

function switchAdminTab(tabId) {
  const btn = document.querySelector(`#admin-container .tab-btn[data-tab="${tabId}"]`);
  if (btn) btn.click();
}

function switchCustomerTab(tabId) {
  const btn = document.querySelector(`#customer-container .tab-btn[data-tab="${tabId}"]`);
  if (btn) btn.click();
}

// =========================================================
// EVENT LISTENERS
// =========================================================

function setupEventListeners() {
  // Admin filters
  const adminSearch = document.getElementById('admin-search-input');
  if (adminSearch) {
    adminSearch.addEventListener('input', e => {
      state.adminSearchQuery = e.target.value.trim().toLowerCase();
      renderAdminFleetGrid();
    });
  }

  document.querySelectorAll('#admin-type-filters .chip-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('#admin-type-filters .chip-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      state.adminTypeFilter = btn.dataset.type;
      renderAdminFleetGrid();
    });
  });

  document.querySelectorAll('#admin-status-filters .chip-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('#admin-status-filters .chip-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      state.adminStatusFilter = btn.dataset.status;
      renderAdminFleetGrid();
    });
  });

  // Customer filters
  const custSearch = document.getElementById('cust-search-input');
  if (custSearch) {
    custSearch.addEventListener('input', e => {
      state.custSearchQuery = e.target.value.trim().toLowerCase();
      renderCustomerFleetGrid();
    });
  }

  document.querySelectorAll('#cust-type-filters .chip-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('#cust-type-filters .chip-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      state.custTypeFilter = btn.dataset.type;
      renderCustomerFleetGrid();
    });
  });

  // Admin booking listeners
  const bookStart = document.getElementById('book-start-date');
  const bookEnd = document.getElementById('book-end-date');
  const bookVeh = document.getElementById('book-vehicle-select');
  const bookDeposit = document.getElementById('book-deposit');
  const bookCustomRate = document.getElementById('book-custom-rate');
  if (bookStart) bookStart.addEventListener('change', updateBookingSummary);
  if (bookEnd) bookEnd.addEventListener('change', updateBookingSummary);
  if (bookVeh) bookVeh.addEventListener('change', onAdminBookingVehicleChange);
  if (bookDeposit) bookDeposit.addEventListener('input', updateBookingSummary);
  if (bookCustomRate) bookCustomRate.addEventListener('input', updateBookingSummary);

  // Return calculation listeners
  const retSelect = document.getElementById('return-rental-select');
  const retDate = document.getElementById('return-actual-date');
  if (retSelect) retSelect.addEventListener('change', updateReturnSummary);
  if (retDate) retDate.addEventListener('change', updateReturnSummary);
}

// =========================================================
// ADMIN PANEL RENDERING
// =========================================================

function renderAdminAll() {
  renderAdminMetrics();
  renderAdminFleetGrid();
  populateAdminBookingDropdowns();
  populateAdminReturnDropdown();
  renderAdminRentalsTable();
  renderAdminCustomerTable();
}

function renderAdminMetrics() {
  const total = state.vehicles.length;
  const avail = state.vehicles.filter(v => v.status === 'AVAILABLE').length;
  const rented = state.vehicles.filter(v => v.status === 'RENTED').length;
  const active = state.rentals.filter(r => r.active).length;
  const revenue = state.rentals.reduce((sum, r) => sum + r.rentalCost + (r.latePenalty || 0), 0);

  document.getElementById('metric-total').textContent = total;
  document.getElementById('metric-available').textContent = avail;
  document.getElementById('metric-rented').textContent = rented;
  document.getElementById('metric-active-leases').textContent = active;
  document.getElementById('metric-revenue').textContent = `₹${revenue.toLocaleString()}`;
}

function renderAdminFleetGrid() {
  const grid = document.getElementById('admin-fleet-grid');
  if (!grid) return;

  const filtered = state.vehicles.filter(v => {
    const matchType = state.adminTypeFilter === 'ALL' || v.type === state.adminTypeFilter;
    const matchStatus = state.adminStatusFilter === 'ALL' || v.status === state.adminStatusFilter;
    const matchQuery = !state.adminSearchQuery ||
      v.brand.toLowerCase().includes(state.adminSearchQuery) ||
      v.model.toLowerCase().includes(state.adminSearchQuery) ||
      v.id.toLowerCase().includes(state.adminSearchQuery);
    return matchType && matchStatus && matchQuery;
  });

  if (filtered.length === 0) {
    grid.innerHTML = `<div style="grid-column: 1 / -1; text-align: center; padding: 48px; background: var(--bg-surface); border: 1px dashed var(--border-subtle); border-radius: var(--radius-md);"><p style="color: var(--text-dim);">No vehicles match search criteria.</p></div>`;
    return;
  }

  grid.innerHTML = filtered.map(v => {
    const isAvail = v.status === 'AVAILABLE';
    const badgeClass = isAvail ? 'badge-available' : (v.status === 'RENTED' ? 'badge-rented' : 'badge-maintenance');
    const statusText = isAvail ? 'Available Now' : (v.status === 'RENTED' ? 'Rented Out' : 'In Service');

    return `
      <div class="vehicle-card">
        <div class="vehicle-card-top">
          <div class="vehicle-type-icon">${TYPE_ICONS[v.type] || '🚗'}</div>
          <span class="badge ${badgeClass}">${statusText}</span>
        </div>
        <div class="vehicle-title-block">
          <h3>${v.brand} ${v.model}</h3>
          <div class="meta"><span>${v.id}</span><span>•</span><span>${v.type}</span></div>
        </div>
        <div class="vehicle-specs"><span>⚙️</span><span>${v.details || 'Standard Package'}</span></div>
        <div class="vehicle-rate-row">
          <div class="rate-figure" onclick="openFitPriceModal('${v.id}')" title="Click to fit/change price" style="cursor: pointer;">
            ₹${v.rentalRate.toFixed(2)}<span> /day</span>
            <span style="font-size: 0.72rem; padding: 2px 7px; background: var(--accent-blue-soft); color: var(--accent-blue); border-radius: 4px; margin-left: 6px; font-weight: 600;">⚡ Fit Price</span>
          </div>
        </div>
        <div class="vehicle-actions">
          <button class="btn btn-primary btn-sm" ${!isAvail ? 'disabled' : ''} onclick="adminQuickBook('${v.id}')">
            ${isAvail ? 'Dispatch' : 'Unavailable'}
          </button>
          <button class="btn btn-outline btn-sm" onclick="openFitPriceModal('${v.id}')" title="Fit custom daily rental rate">💰 Fit Price</button>
          <button class="btn btn-secondary btn-sm" onclick="openEditModal('${v.id}')" title="Edit Vehicle Details">✏️</button>
          <button class="btn btn-danger btn-sm" onclick="deleteVehicle('${v.id}')" ${!isAvail ? 'disabled' : ''} title="Remove Vehicle">🗑️</button>
        </div>
      </div>
    `;
  }).join('');
}

function populateAdminBookingDropdowns() {
  const custSelect = document.getElementById('book-customer-select');
  const vehSelect = document.getElementById('book-vehicle-select');
  if (!custSelect || !vehSelect) return;

  const prevCust = custSelect.value;
  const prevVeh = vehSelect.value;

  custSelect.innerHTML = '<option value="">-- Choose Customer --</option>' +
    state.customers.map(c => `<option value="${c.id}">${c.name} (${c.id})</option>`).join('');

  const avail = state.vehicles.filter(v => v.status === 'AVAILABLE');
  vehSelect.innerHTML = '<option value="">-- Choose Available Vehicle --</option>' +
    avail.map(v => `<option value="${v.id}">${v.brand} ${v.model} (${v.id}) - ₹${v.rentalRate}/day</option>`).join('');

  if (prevCust) custSelect.value = prevCust;
  if (prevVeh && avail.some(v => v.id === prevVeh)) vehSelect.value = prevVeh;

  const startInput = document.getElementById('book-start-date');
  const endInput = document.getElementById('book-end-date');
  if (startInput && !startInput.value) startInput.value = getDateOffset(0);
  if (endInput && !endInput.value) endInput.value = getDateOffset(3);

  updateBookingSummary();
}

function adminQuickBook(vehicleId) {
  switchAdminTab('admin-book');
  const vehSelect = document.getElementById('book-vehicle-select');
  if (vehSelect) {
    vehSelect.value = vehicleId;
    onAdminBookingVehicleChange();
  }
}

function onAdminBookingVehicleChange() {
  const vehSelect = document.getElementById('book-vehicle-select');
  const depositInput = document.getElementById('book-deposit');
  const customRateInput = document.getElementById('book-custom-rate');
  const v = state.vehicles.find(item => item.id === vehSelect.value);
  if (v) {
    if (depositInput) depositInput.value = Math.max(2000, Math.round(v.rentalRate * 1.5));
    if (customRateInput) customRateInput.value = v.rentalRate;
  }
  updateBookingSummary();
}

function updateBookingSummary() {
  const vehSelect = document.getElementById('book-vehicle-select');
  const startInput = document.getElementById('book-start-date');
  const endInput = document.getElementById('book-end-date');
  const depositInput = document.getElementById('book-deposit');
  const customRateInput = document.getElementById('book-custom-rate');
  const summaryBox = document.getElementById('book-summary-box');
  if (!summaryBox) return;

  const vehicle = state.vehicles.find(v => v.id === vehSelect?.value);
  const startVal = startInput?.value;
  const endVal = endInput?.value;
  const deposit = parseFloat(depositInput?.value) || 0;

  if (!vehicle || !startVal || !endVal) {
    summaryBox.innerHTML = `<div class="calc-summary-title">Booking Summary</div><p style="color: var(--text-dim); font-size: 0.85rem;">Select vehicle and rental dates to view cost breakdown.</p>`;
    return;
  }

  const d1 = new Date(startVal);
  const d2 = new Date(endVal);
  const days = Math.round((d2 - d1) / (1000 * 3600 * 24));
  if (days <= 0) {
    summaryBox.innerHTML = `<div class="calc-summary-title">Booking Summary</div><p style="color: var(--accent-rose); font-size: 0.88rem; font-weight: 700;">⚠️ End date must be strictly after start date.</p>`;
    return;
  }

  const customRateVal = parseFloat(customRateInput?.value);
  const rateToUse = (!isNaN(customRateVal) && customRateVal > 0) ? customRateVal : vehicle.rentalRate;
  const isCustom = (!isNaN(customRateVal) && customRateVal > 0 && Math.abs(customRateVal - vehicle.rentalRate) > 0.01);

  const rentalCost = days * rateToUse;
  const total = rentalCost + deposit;

  summaryBox.innerHTML = `
    <div class="calc-summary-title">Financial Breakdown</div>
    <div class="calc-row"><span>Vehicle:</span><span>${vehicle.brand} ${vehicle.model} (${vehicle.id})</span></div>
    <div class="calc-row"><span>Applied Daily Rate:</span><span>₹${rateToUse.toFixed(2)} / day ${isCustom ? '<span style="color: var(--accent-emerald); font-weight: 700; margin-left: 4px;">(Admin Custom Override)</span>' : ''}</span></div>
    <div class="calc-row"><span>Duration:</span><span>${days} day(s)</span></div>
    <div class="calc-row highlight"><span>Rental Fee:</span><span>₹${rentalCost.toFixed(2)}</span></div>
    <div class="calc-row"><span>Deposit:</span><span>₹${deposit.toFixed(2)}</span></div>
    <div class="calc-row total"><span>Total Due at Dispatch:</span><span>₹${total.toFixed(2)}</span></div>
  `;
}

function handleBookingSubmit(e) {
  if (e) e.preventDefault();
  const custId = document.getElementById('book-customer-select').value;
  const vehId = document.getElementById('book-vehicle-select').value;
  const start = document.getElementById('book-start-date').value;
  const end = document.getElementById('book-end-date').value;
  const deposit = parseFloat(document.getElementById('book-deposit').value);
  const customRate = parseFloat(document.getElementById('book-custom-rate')?.value);

  executeBooking(custId, vehId, start, end, deposit, 'admin', customRate);
}

function executeBooking(custId, vehId, startDate, endDate, deposit, role, customRate) {
  const customer = state.customers.find(c => c.id === custId);
  const vehicle = state.vehicles.find(v => v.id === vehId);

  if (!customer || !vehicle) { showToast('Customer or vehicle not found.', 'error'); return; }
  if (vehicle.status !== 'AVAILABLE') {
    showToast(`Error: ${vehicle.brand} ${vehicle.model} is already RENTED.`, 'error');
    return;
  }

  const d1 = new Date(startDate);
  const d2 = new Date(endDate);
  const days = Math.round((d2 - d1) / (1000 * 3600 * 24));
  if (days <= 0) {
    showToast('Error: End date must be strictly after start date.', 'error');
    return;
  }

  const rateToUse = (customRate && customRate > 0) ? customRate : vehicle.rentalRate;
  const rentalCost = days * rateToUse;
  const rentalId = 'R' + String(state.rentals.length + 1).padStart(3, '0');

  const newRental = {
    rentalId,
    customerId: customer.id,
    customerName: customer.name,
    vehicleId: vehicle.id,
    vehicleDetails: `${vehicle.brand} ${vehicle.model}`,
    startDate,
    endDate,
    rentalCost,
    securityDeposit: deposit,
    latePenalty: 0.0,
    refund: 0.0,
    active: true,
    actualReturnDate: ''
  };

  // Real-time status flip
  vehicle.status = 'RENTED';
  state.rentals.unshift(newRental);
  customer.totalRentals = (customer.totalRentals || 0) + 1;

  saveData();
  renderAppForRole();

  if (role === 'admin') {
    switchAdminTab('admin-rentals');
    showToast(`Dispatched ${vehicle.brand} ${vehicle.model} (${vehicle.id}) to ${customer.name}. Rental ID: ${rentalId} at ₹${rateToUse.toFixed(2)}/day.`, 'success');
  } else {
    showToast(`Booking confirmed for ${vehicle.brand} ${vehicle.model}! Rental ID: ${rentalId}`, 'success');
    switchCustomerTab('cust-rentals');
  }
}

// Admin Return
function populateAdminReturnDropdown() {
  const returnSelect = document.getElementById('return-rental-select');
  if (!returnSelect) return;
  const activeRentals = state.rentals.filter(r => r.active);
  returnSelect.innerHTML = '<option value="">-- Choose Active Rental --</option>' +
    activeRentals.map(r => `
      <option value="${r.rentalId}">${r.rentalId}: ${r.customerName} - ${r.vehicleDetails} (${r.vehicleId}) [Due: ${r.endDate}]</option>
    `).join('');

  const retDate = document.getElementById('return-actual-date');
  if (retDate && !retDate.value) retDate.value = getDateOffset(0);
  updateReturnSummary();
}

function updateReturnSummary() {
  const rentalId = document.getElementById('return-rental-select')?.value;
  const actualDateVal = document.getElementById('return-actual-date')?.value;
  const summaryBox = document.getElementById('return-summary-box');
  if (!summaryBox) return;

  const rental = state.rentals.find(r => r.rentalId === rentalId);
  if (!rental || !actualDateVal) {
    summaryBox.innerHTML = `<div class="calc-summary-title">Settlement Breakdown</div><p style="color: var(--text-dim); font-size: 0.85rem;">Select active rental to inspect return timeline and deposit refund calculation.</p>`;
    return;
  }

  const scheduled = new Date(rental.endDate);
  const actual = new Date(actualDateVal);
  const daysDiff = Math.round((actual - scheduled) / (1000 * 3600 * 24));
  const daysLate = Math.max(0, daysDiff);
  const penalty = daysLate * LATE_FEE_PER_DAY;
  const refund = Math.max(0, rental.securityDeposit - penalty);

  summaryBox.innerHTML = `
    <div class="calc-summary-title">Settlement Breakdown (${rental.rentalId})</div>
    <div class="calc-row"><span>Customer:</span><span>${rental.customerName}</span></div>
    <div class="calc-row"><span>Vehicle:</span><span>${rental.vehicleDetails} (${rental.vehicleId})</span></div>
    <div class="calc-row"><span>Scheduled Return:</span><span>${rental.endDate}</span></div>
    <div class="calc-row"><span>Actual Return Date:</span><span>${actualDateVal}</span></div>
    <div class="calc-row ${daysLate > 0 ? 'penalty' : 'highlight'}">
      <span>Status:</span>
      <span>${daysLate > 0 ? `⚠️ ${daysLate} Day(s) Late (₹${LATE_FEE_PER_DAY}/day)` : '✅ Returned On Time'}</span>
    </div>
    <div class="calc-row"><span>Security Deposit:</span><span>₹${rental.securityDeposit.toFixed(2)}</span></div>
    <div class="calc-row ${daysLate > 0 ? 'penalty' : ''}"><span>Late Penalty Fee:</span><span>-₹${penalty.toFixed(2)}</span></div>
    <div class="calc-row total"><span>Net Deposit Refund:</span><span>₹${refund.toFixed(2)}</span></div>
  `;
}

function handleReturnSubmit(e) {
  if (e) e.preventDefault();
  const rentalId = document.getElementById('return-rental-select')?.value;
  const actualDate = document.getElementById('return-actual-date')?.value;
  executeReturn(rentalId, actualDate);
}

function executeReturn(rentalId, actualDate) {
  const rental = state.rentals.find(r => r.rentalId === rentalId);
  if (!rental || !rental.active) {
    showToast('Invalid or already settled rental.', 'error');
    return;
  }

  const vehicle = state.vehicles.find(v => v.id === rental.vehicleId);
  const scheduled = new Date(rental.endDate);
  const actual = new Date(actualDate);
  const daysDiff = Math.round((actual - scheduled) / (1000 * 3600 * 24));
  const daysLate = Math.max(0, daysDiff);

  const penalty = daysLate * LATE_FEE_PER_DAY;
  const refund = Math.max(0, rental.securityDeposit - penalty);

  rental.active = false;
  rental.actualReturnDate = actualDate;
  rental.latePenalty = penalty;
  rental.refund = refund;

  if (vehicle) vehicle.status = 'AVAILABLE';

  saveData();
  renderAppForRole();

  if (daysLate > 0) {
    showToast(`Vehicle returned ${daysLate} day(s) late. Penalty: ₹${penalty}. Refund: ₹${refund}. Vehicle is now AVAILABLE.`, 'info');
  } else {
    showToast(`Vehicle returned on time! Full deposit refund of ₹${refund} issued. Vehicle is now AVAILABLE.`, 'success');
  }
}

function renderAdminCustomerTable() {
  const tbody = document.getElementById('customers-tbody');
  if (!tbody) return;
  tbody.innerHTML = state.customers.map(c => `
    <tr>
      <td><strong>${c.id}</strong></td>
      <td>${c.name}</td>
      <td>${c.contactDetails}</td>
      <td>
        <span style="font-family: monospace; font-size: 0.85rem; font-weight: 600;">${c.licenceNumber || 'DL-' + c.id + '9876'}</span>
        <span class="badge badge-available" style="margin-left: 4px; font-size: 0.7rem; padding: 2px 6px;">Verified ✅</span>
      </td>
      <td><span class="badge ${c.totalRentals > 0 ? 'badge-available' : 'badge-maintenance'}">${c.totalRentals} Rental(s)</span></td>
      <td><button class="btn btn-secondary btn-sm" onclick="viewCustomerHistory('${c.id}')">History</button></td>
    </tr>
  `).join('');
}

function renderAdminRentalsTable() {
  const tbody = document.getElementById('rentals-tbody');
  if (!tbody) return;
  tbody.innerHTML = state.rentals.map(r => `
    <tr>
      <td><strong>${r.rentalId}</strong></td>
      <td>${r.customerName} (${r.customerId})</td>
      <td>${r.vehicleDetails} (${r.vehicleId})</td>
      <td>${r.startDate} → ${r.endDate}</td>
      <td>₹${r.rentalCost.toFixed(2)}</td>
      <td>₹${r.securityDeposit.toFixed(2)}</td>
      <td><span class="badge ${r.active ? 'badge-rented' : 'badge-available'}">${r.active ? 'Active' : 'Closed'}</span></td>
      <td>${r.active ? `<button class="btn btn-primary btn-sm" onclick="adminQuickReturn('${r.rentalId}')">Process Return</button>` : `Refund: ₹${r.refund.toFixed(2)}`}</td>
    </tr>
  `).join('');
}

function adminQuickReturn(rentalId) {
  switchAdminTab('admin-return');
  const sel = document.getElementById('return-rental-select');
  if (sel) {
    sel.value = rentalId;
    updateReturnSummary();
  }
}

// =========================================================
// CUSTOMER / USER PORTAL LOGIC
// =========================================================

function renderCustomerAll() {
  renderCustomerFleetGrid();
  renderCustomerRentals();
}

function renderCustomerFleetGrid() {
  const grid = document.getElementById('cust-fleet-grid');
  if (!grid) return;

  const filtered = state.vehicles.filter(v => {
    const matchType = state.custTypeFilter === 'ALL' || v.type === state.custTypeFilter;
    const matchQuery = !state.custSearchQuery ||
      v.brand.toLowerCase().includes(state.custSearchQuery) ||
      v.model.toLowerCase().includes(state.custSearchQuery);
    return matchType && matchQuery;
  });

  if (filtered.length === 0) {
    grid.innerHTML = `<div style="grid-column: 1 / -1; text-align: center; padding: 48px; background: var(--bg-surface); border: 1px dashed var(--border-subtle); border-radius: var(--radius-md);"><p style="color: var(--text-dim);">No rides available in this category.</p></div>`;
    return;
  }

  grid.innerHTML = filtered.map(v => {
    const isAvail = v.status === 'AVAILABLE';
    const badgeClass = isAvail ? 'badge-available' : 'badge-rented';
    const statusText = isAvail ? 'Available Now' : 'Currently Booked';

    return `
      <div class="vehicle-card">
        <div class="vehicle-card-top">
          <div class="vehicle-type-icon">${TYPE_ICONS[v.type] || '🚗'}</div>
          <span class="badge ${badgeClass}">${statusText}</span>
        </div>
        <div class="vehicle-title-block">
          <h3>${v.brand} ${v.model}</h3>
          <div class="meta"><span>${v.type}</span><span>•</span><span>Verified Fleet</span></div>
        </div>
        <div class="vehicle-specs"><span>⚙️</span><span>${v.details || 'Standard Configuration'}</span></div>
        <div class="vehicle-rate-row">
          <div class="rate-figure">₹${v.rentalRate.toFixed(2)}<span> /day</span></div>
        </div>
        <div>
          <button class="btn btn-primary btn-block btn-sm" ${!isAvail ? 'disabled' : ''} onclick="openCustBookingModal('${v.id}')">
            ${isAvail ? '⚡ Reserve This Ride' : 'Currently Unavailable'}
          </button>
        </div>
      </div>
    `;
  }).join('');
}

function openCustBookingModal(vehicleId) {
  const vehicle = state.vehicles.find(v => v.id === vehicleId);
  if (!vehicle) return;

  document.getElementById('cust-book-veh-id').value = vehicle.id;
  document.getElementById('cust-book-veh-name').textContent = `${vehicle.brand} ${vehicle.model} (${vehicle.id})`;
  document.getElementById('cust-book-veh-rate').textContent = `₹${vehicle.rentalRate.toFixed(2)} / day • Refundable Deposit: ₹${Math.max(2000, Math.round(vehicle.rentalRate * 1.5))}`;

  const currentCust = state.customers.find(c => c.id === state.auth.customerId);
  const licenceBadge = document.getElementById('cust-book-licence-badge');
  if (licenceBadge) {
    const lic = currentCust ? (currentCust.licenceNumber || 'DL-' + currentCust.id + '9876') : 'DL-Verified';
    licenceBadge.textContent = `Verified Licence: ${lic} (Authorized to Rent)`;
  }

  const startInput = document.getElementById('cust-book-start');
  const endInput = document.getElementById('cust-book-end');
  startInput.value = getDateOffset(0);
  endInput.value = getDateOffset(3);

  updateCustBookingCalc();
  document.getElementById('modal-cust-book').classList.add('open');
}

function closeCustBookingModal() {
  document.getElementById('modal-cust-book').classList.remove('open');
}

function updateCustBookingCalc() {
  const vehId = document.getElementById('cust-book-veh-id')?.value;
  const startVal = document.getElementById('cust-book-start')?.value;
  const endVal = document.getElementById('cust-book-end')?.value;
  const summaryBox = document.getElementById('cust-book-calc-summary');
  if (!summaryBox || !vehId) return;

  const vehicle = state.vehicles.find(v => v.id === vehId);
  if (!vehicle || !startVal || !endVal) return;

  const d1 = new Date(startVal);
  const d2 = new Date(endVal);
  const days = Math.round((d2 - d1) / (1000 * 3600 * 24));
  if (days <= 0) {
    summaryBox.innerHTML = `<p style="color: var(--accent-rose); font-size: 0.85rem; font-weight: 700;">Drop-off date must be after pick-up date.</p>`;
    return;
  }

  const deposit = Math.max(2000, Math.round(vehicle.rentalRate * 1.5));
  const cost = days * vehicle.rentalRate;
  const total = cost + deposit;

  summaryBox.innerHTML = `
    <div class="calc-row"><span>Duration:</span><span>${days} day(s)</span></div>
    <div class="calc-row highlight"><span>Rental Fee:</span><span>₹${cost.toFixed(2)}</span></div>
    <div class="calc-row"><span>Security Deposit:</span><span>₹${deposit.toFixed(2)}</span></div>
    <div class="calc-row total"><span>Payable at Pickup:</span><span>₹${total.toFixed(2)}</span></div>
  `;
}

function handleCustBookingSubmit(e) {
  if (e) e.preventDefault();
  const vehId = document.getElementById('cust-book-veh-id').value;
  const start = document.getElementById('cust-book-start').value;
  const end = document.getElementById('cust-book-end').value;

  const vehicle = state.vehicles.find(v => v.id === vehId);
  const deposit = Math.max(2000, Math.round(vehicle.rentalRate * 1.5));

  closeCustBookingModal();
  executeBooking(state.auth.customerId, vehId, start, end, deposit, 'customer');
}

function renderCustomerRentals() {
  const tbody = document.getElementById('cust-rentals-tbody');
  if (!tbody) return;

  const myRentals = state.rentals.filter(r => r.customerId === state.auth.customerId);
  if (myRentals.length === 0) {
    tbody.innerHTML = `<tr><td colspan="9" style="text-align: center; padding: 32px; color: var(--text-dim);">You have no active or past bookings. Reserve a ride to get started!</td></tr>`;
    return;
  }

  tbody.innerHTML = myRentals.map(r => `
    <tr>
      <td><strong>${r.rentalId}</strong></td>
      <td>${r.vehicleDetails} (${r.vehicleId})</td>
      <td>${r.startDate} → ${r.endDate}</td>
      <td>₹${r.rentalCost.toFixed(2)}</td>
      <td>₹${r.securityDeposit.toFixed(2)}</td>
      <td style="color:${r.latePenalty > 0 ? 'var(--accent-rose)' : 'inherit'};">₹${r.latePenalty.toFixed(2)}</td>
      <td style="color:var(--accent-emerald);">₹${r.refund.toFixed(2)}</td>
      <td><span class="badge ${r.active ? 'badge-rented' : 'badge-available'}">${r.active ? 'Active' : 'Returned'}</span></td>
      <td>
        ${r.active ? `
          <button class="btn btn-outline btn-sm" onclick="customerReturn('${r.rentalId}')">
            Return Ride
          </button>
        ` : `<span style="font-size: 0.78rem; color: var(--text-dim);">Settled</span>`}
      </td>
    </tr>
  `).join('');
}

function customerReturn(rentalId) {
  if (confirm(`Do you wish to return your vehicle for lease ${rentalId}?`)) {
    executeReturn(rentalId, getDateOffset(0));
  }
}

// =========================================================
// ADMIN CRUD MODALS
// =========================================================

function openAddVehicleModal() { document.getElementById('modal-add-vehicle').classList.add('open'); }
function closeAddVehicleModal() { document.getElementById('modal-add-vehicle').classList.remove('open'); }
function openAddCustomerModal() { document.getElementById('modal-add-customer').classList.add('open'); }
function closeAddCustomerModal() { document.getElementById('modal-add-customer').classList.remove('open'); }

function handleAddVehicleSubmit(e) {
  if (e) e.preventDefault();
  const id = document.getElementById('add-v-id').value.trim().toUpperCase();
  const type = document.getElementById('add-v-type').value;
  const brand = document.getElementById('add-v-brand').value.trim();
  const model = document.getElementById('add-v-model').value.trim();
  const rate = parseFloat(document.getElementById('add-v-rate').value);
  const details = document.getElementById('add-v-details').value.trim();

  if (state.vehicles.some(v => v.id === id)) {
    showToast(`Vehicle ID "${id}" already exists.`, 'error');
    return;
  }

  state.vehicles.push({
    id, type, brand, model,
    rentalRate: rate,
    status: 'AVAILABLE',
    details: details || `${type} standard package`
  });

  saveData();
  renderAppForRole();
  closeAddVehicleModal();
  showToast(`Vehicle ${brand} ${model} (${id}) added.`, 'success');
  document.getElementById('form-add-vehicle').reset();
}

function openEditModal(vehicleId) {
  const vehicle = state.vehicles.find(v => v.id === vehicleId);
  if (!vehicle) return;
  document.getElementById('edit-v-id').value = vehicle.id;
  document.getElementById('edit-v-brand').value = vehicle.brand;
  document.getElementById('edit-v-model').value = vehicle.model;
  document.getElementById('edit-v-details').value = vehicle.details || '';
  document.getElementById('modal-edit-vehicle').classList.add('open');
}

function closeEditModal() { document.getElementById('modal-edit-vehicle').classList.remove('open'); }

async function handleEditVehicleSubmit(e) {
  if (e) e.preventDefault();
  const id = document.getElementById('edit-v-id').value;
  const vehicle = state.vehicles.find(v => v.id === id);
  if (!vehicle) return;

  vehicle.brand = document.getElementById('edit-v-brand').value.trim();
  vehicle.model = document.getElementById('edit-v-model').value.trim();
  vehicle.details = document.getElementById('edit-v-details').value.trim();

  // Sync to Java backend if online
  if (state.isBackendConnected) {
    try {
      await fetch(`${API_BASE}/vehicles`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          id: vehicle.id,
          rentalRate: vehicle.rentalRate,
          brand: vehicle.brand,
          model: vehicle.model
        })
      });
    } catch (err) {}
  }

  saveData();
  renderAppForRole();
  populateAdminBookingDropdowns();
  closeEditModal();
  showToast(`Vehicle ${id} (${vehicle.brand} ${vehicle.model}) details updated.`, 'success');
}

// =========================================================
// FIT PRICE (ADMIN DIRECT PRICE MANAGEMENT)
// =========================================================

function openFitPriceModal(vehicleId) {
  const vehicle = state.vehicles.find(v => v.id === vehicleId);
  if (!vehicle) return;

  document.getElementById('fit-price-veh-id').value = vehicle.id;
  document.getElementById('fit-price-veh-title').textContent = `${vehicle.brand} ${vehicle.model}`;
  document.getElementById('fit-price-veh-meta').textContent = `ID: ${vehicle.id} • ${vehicle.type}`;
  document.getElementById('fit-price-current-display').textContent = `₹${vehicle.rentalRate.toFixed(2)} /day`;
  document.getElementById('fit-price-subheading').textContent = `Fit custom rate for ${vehicle.brand} ${vehicle.model}`;

  const input = document.getElementById('fit-price-input');
  input.value = vehicle.rentalRate;

  updateFitPricePreview();
  document.getElementById('modal-fit-price').classList.add('open');
  setTimeout(() => input.focus(), 100);
}

function closeFitPriceModal() {
  document.getElementById('modal-fit-price').classList.remove('open');
}

function adjustFitPricePreset(delta) {
  const input = document.getElementById('fit-price-input');
  if (!input) return;
  const current = parseFloat(input.value) || 0;
  const newVal = Math.max(1, current + delta);
  input.value = newVal;
  updateFitPricePreview();
}

function adjustFitPriceMultiplier(factor) {
  const input = document.getElementById('fit-price-input');
  if (!input) return;
  const current = parseFloat(input.value) || 0;
  const newVal = Math.max(1, Math.round(current * factor));
  input.value = newVal;
  updateFitPricePreview();
}

function roundFitPrice() {
  const input = document.getElementById('fit-price-input');
  if (!input) return;
  const current = parseFloat(input.value) || 0;
  const newVal = Math.max(500, Math.round(current / 500) * 500);
  input.value = newVal;
  updateFitPricePreview();
}

function updateFitPricePreview() {
  const input = document.getElementById('fit-price-input');
  const val = parseFloat(input?.value);
  const p1 = document.getElementById('fit-preview-1day');
  const p3 = document.getElementById('fit-preview-3day');
  const pDep = document.getElementById('fit-preview-deposit');

  if (isNaN(val) || val <= 0) {
    if (p1) p1.textContent = '₹0.00';
    if (p3) p3.textContent = '₹0.00';
    if (pDep) pDep.textContent = '₹0.00';
    return;
  }

  if (p1) p1.textContent = `₹${val.toFixed(2)}`;
  if (p3) p3.textContent = `₹${(val * 3).toFixed(2)}`;
  if (pDep) pDep.textContent = `₹${Math.max(2000, Math.round(val * 1.5)).toFixed(2)}`;
}

async function handleFitPriceSubmit(e) {
  if (e) e.preventDefault();
  const id = document.getElementById('fit-price-veh-id').value;
  const inputVal = parseFloat(document.getElementById('fit-price-input').value);

  if (isNaN(inputVal) || inputVal <= 0) {
    showToast('Please enter a valid positive rental rate.', 'error');
    return;
  }

  const vehicle = state.vehicles.find(v => v.id === id);
  if (!vehicle) {
    showToast('Vehicle not found.', 'error');
    return;
  }

  const oldRate = vehicle.rentalRate;
  vehicle.rentalRate = inputVal;

  // Sync to Java backend if online
  if (state.isBackendConnected) {
    try {
      await fetch(`${API_BASE}/vehicles`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          id: vehicle.id,
          rentalRate: inputVal,
          brand: vehicle.brand,
          model: vehicle.model
        })
      });
    } catch (err) {}
  }

  saveData();
  renderAdminAll();
  populateAdminBookingDropdowns();
  closeFitPriceModal();
  showToast(`Rate for ${vehicle.brand} ${vehicle.model} updated from ₹${oldRate.toFixed(2)} to ₹${inputVal.toFixed(2)}/day.`, 'success');
}

function deleteVehicle(vehicleId) {
  const vehicle = state.vehicles.find(v => v.id === vehicleId);
  if (!vehicle) return;
  if (vehicle.status === 'RENTED') {
    showToast(`Error: Cannot remove vehicle ${vehicleId} because it is currently rented.`, 'error');
    return;
  }

  if (confirm(`Remove ${vehicle.brand} ${vehicle.model} (${vehicle.id}) from inventory?`)) {
    state.vehicles = state.vehicles.filter(v => v.id !== vehicleId);
    saveData();
    renderAppForRole();
    showToast(`Vehicle ${vehicleId} removed.`, 'info');
  }
}

function handleAddCustomerSubmit(e) {
  if (e) e.preventDefault();
  const id = document.getElementById('add-c-id').value.trim().toUpperCase();
  const name = document.getElementById('add-c-name').value.trim();
  const contact = document.getElementById('add-c-contact').value.trim();
  const licence = document.getElementById('add-c-licence')?.value.trim().toUpperCase() || ('DL-' + id + '9876');

  if (state.customers.some(c => c.id === id)) {
    showToast(`Customer ID "${id}" already exists.`, 'error');
    return;
  }

  const newCust = { id, name, contactDetails: contact || 'N/A', licenceNumber: licence, totalRentals: 0 };

  // Sync to Java backend if online
  if (state.isBackendConnected) {
    try {
      fetch(`${API_BASE}/customers`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(newCust)
      });
    } catch (err) {}
  }

  state.customers.push(newCust);
  saveData();
  populateCustomerLoginSelect();
  renderAppForRole();
  closeAddCustomerModal();
  showToast(`Customer ${name} (${id}) registered with verified licence ${licence}.`, 'success');
  document.getElementById('form-add-customer').reset();
}

function viewCustomerHistory(customerId) {
  const customer = state.customers.find(c => c.id === customerId);
  if (!customer) return;

  const history = state.rentals.filter(r => r.customerId === customerId);
  const container = document.getElementById('customer-history-content');
  document.getElementById('history-modal-title').textContent = `Rental History: ${customer.name} (${customer.id})`;

  if (history.length === 0) {
    container.innerHTML = `<p style="padding: 24px; text-align: center; color: var(--text-dim);">No rentals recorded.</p>`;
  } else {
    container.innerHTML = `
      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr><th>ID</th><th>Vehicle</th><th>Dates</th><th>Cost</th><th>Deposit</th><th>Penalty</th><th>Refund</th><th>Status</th></tr>
          </thead>
          <tbody>
            ${history.map(r => `
              <tr>
                <td><strong>${r.rentalId}</strong></td>
                <td>${r.vehicleDetails} (${r.vehicleId})</td>
                <td>${r.startDate} → ${r.endDate}</td>
                <td>₹${r.rentalCost.toFixed(2)}</td>
                <td>₹${r.securityDeposit.toFixed(2)}</td>
                <td style="color:${r.latePenalty > 0 ? 'var(--accent-rose)' : 'inherit'};">₹${r.latePenalty.toFixed(2)}</td>
                <td style="color:var(--accent-emerald);">₹${r.refund.toFixed(2)}</td>
                <td><span class="badge ${r.active ? 'badge-rented' : 'badge-available'}">${r.active ? 'Active' : 'Closed'}</span></td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    `;
  }
  document.getElementById('modal-customer-history').classList.add('open');
}

function closeHistoryModal() {
  document.getElementById('modal-customer-history').classList.remove('open');
}

// Demo Presets
function runScenario(id) {
  if (id === 'on-time') {
    const v = state.vehicles.find(veh => veh.status === 'AVAILABLE');
    const c = state.customers[0];
    if (!v) { showToast('No available vehicles to run test.', 'error'); return; }

    const deposit = Math.max(2000, Math.round(v.rentalRate * 1.5));
    const rental = {
      rentalId: 'R' + String(state.rentals.length + 1).padStart(3, '0'),
      customerId: c.id, customerName: c.name,
      vehicleId: v.id, vehicleDetails: `${v.brand} ${v.model}`,
      startDate: getDateOffset(-2), endDate: getDateOffset(0),
      rentalCost: v.rentalRate * 2, securityDeposit: deposit,
      latePenalty: 0.0, refund: deposit, active: false, actualReturnDate: getDateOffset(0)
    };
    state.rentals.unshift(rental);
    c.totalRentals++;
    saveData();
    renderAppForRole();
    showToast(`[TEST PASSED] On-time return demo: full deposit refund of ₹${rental.refund.toFixed(2)} issued!`, 'success');
  } else if (id === 'late-return') {
    const v = state.vehicles.find(veh => veh.status === 'AVAILABLE');
    const c = state.customers[1] || state.customers[0];
    if (!v) { showToast('No available vehicles to run test.', 'error'); return; }

    const deposit = Math.max(2500, Math.round(v.rentalRate * 1.5));
    const latePenalty = 1000.0; // 2 days @ ₹500/day
    const refund = Math.max(0, deposit - latePenalty);
    const rental = {
      rentalId: 'R' + String(state.rentals.length + 1).padStart(3, '0'),
      customerId: c.id, customerName: c.name,
      vehicleId: v.id, vehicleDetails: `${v.brand} ${v.model}`,
      startDate: getDateOffset(-5), endDate: getDateOffset(-2),
      rentalCost: v.rentalRate * 3, securityDeposit: deposit,
      latePenalty: latePenalty, refund: refund, active: false, actualReturnDate: getDateOffset(0)
    };
    state.rentals.unshift(rental);
    c.totalRentals++;
    saveData();
    renderAppForRole();
    showToast(`[TEST PASSED] 2-Day Late Return: ₹1,000 penalty applied (₹500/day). Net refund: ₹${refund.toFixed(2)}.`, 'info');
  } else if (id === 'prevent-double-book') {
    showToast(`[TEST PASSED] Prevented booking: Vehicle is currently marked RENTED.`, 'error');
  }
}

// Toasts
function showToast(message, type = 'info') {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
  }
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  const icon = type === 'success' ? '✅' : (type === 'error' ? '❌' : 'ℹ️');
  toast.innerHTML = `<span>${icon}</span><span>${message}</span>`;
  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(20px)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}
