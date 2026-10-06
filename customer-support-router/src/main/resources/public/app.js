'use strict';

const state = { user: null, tickets: [], selectedId: null, agents: [] };

const $ = (id) => document.getElementById(id);

async function api(method, path, body) {
  const response = await fetch(path, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined,
    credentials: 'same-origin',
  });
  if (response.status === 204) return null;
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    if (response.status === 401 && path !== '/api/login') showLogin();
    throw new Error(data.error || `Request failed (${response.status})`);
  }
  return data;
}

function can(permission) {
  return state.user && state.user.permissions.includes(permission);
}

function el(tag, options = {}, children = []) {
  const node = document.createElement(tag);
  if (options.text !== undefined) node.textContent = options.text;
  if (options.className) node.className = options.className;
  for (const [key, value] of Object.entries(options.attrs || {})) node.setAttribute(key, value);
  for (const child of children) node.append(child);
  return node;
}

function flash(message, ok = true) {
  const box = $('flash');
  box.textContent = message;
  box.className = `flash ${ok ? 'ok' : 'bad'}`;
  box.hidden = false;
  clearTimeout(flash.timer);
  flash.timer = setTimeout(() => { box.hidden = true; }, 5000);
}

function showLogin() {
  state.user = null;
  $('flash').hidden = true;
  $('app-view').hidden = true;
  $('login-view').hidden = false;
  $('username').focus();
}

async function showApp(user) {
  state.user = user;
  state.selectedId = null;
  $('login-view').hidden = true;
  $('app-view').hidden = false;
  $('user-name').textContent = user.name;
  $('user-role').textContent = user.role;
  $('new-ticket').hidden = !can('CREATE_TICKET');
  $('customer-fields').hidden = !can('VIEW_ALL_TICKETS');
  $('create-form').hidden = true;
  $('tickets-title').textContent = {
    CUSTOMER: 'My tickets',
    AGENT: 'Tickets assigned to me',
    SUPERVISOR: 'All tickets',
  }[user.role] || 'Tickets';
  state.agents = can('ASSIGN_TICKET') ? await api('GET', '/api/agents') : [];
  renderDetailPlaceholder();
  await loadTickets();
}

async function loadTickets() {
  state.tickets = await api('GET', '/api/tickets');
  renderTickets();
}

function renderTickets() {
  const rows = $('ticket-rows');
  rows.replaceChildren();
  $('no-tickets').hidden = state.tickets.length > 0;
  for (const ticket of state.tickets) {
    const row = el('tr', { className: ticket.id === state.selectedId ? 'active' : '' }, [
      el('td', { text: ticket.id }),
      el('td', { text: ticket.category }),
      el('td', {}, [el('span', { text: ticket.priority, className: `badge priority-${ticket.priority}` })]),
      el('td', {}, [el('span', { text: ticket.status, className: `badge status-${ticket.status}` })]),
      el('td', { text: ticket.assignedAgent || '—' }),
      el('td', { text: ticket.customerName }),
    ]);
    row.addEventListener('click', () => selectTicket(ticket.id));
    rows.append(row);
  }
}

function renderDetailPlaceholder() {
  $('detail').replaceChildren(el('p', { className: 'muted empty', text: 'Select a ticket to see its details.' }));
}

async function selectTicket(id) {
  state.selectedId = id;
  renderTickets();
  try {
    const detail = await api('GET', `/api/tickets/${encodeURIComponent(id)}`);
    renderDetail(detail);
  } catch (error) {
    flash(error.message, false);
  }
}

function renderDetail({ ticket, history }) {
  const fields = el('dl', { className: 'fields' });
  const rows = [
    ['Customer', `${ticket.customerName} <${ticket.customerEmail}>`],
    ['Category', ticket.category],
    ['Priority', ticket.priority],
    ['Team', ticket.assignedTeam],
    ['Agent', ticket.assignedAgent || 'Not assigned'],
    ['Status', ticket.status],
  ];
  for (const [label, value] of rows) {
    fields.append(el('dt', { text: label }), el('dd', { text: value }));
  }

  const historyList = el('ul', { className: 'history' });
  if (history.length === 0) {
    historyList.append(el('li', { className: 'muted', text: 'No status changes yet.' }));
  }
  for (const change of history) {
    historyList.append(el('li', {}, [
      el('time', { text: change.changedAt }),
      `${change.oldStatus} → ${change.newStatus}`,
    ]));
  }

  const children = [
    el('h2', { text: `Ticket ${ticket.id}` }),
    fields,
    el('p', { className: 'message', text: ticket.message }),
    el('h3', { text: 'Status history' }),
    historyList,
  ];

  const actions = renderActions(ticket);
  if (actions) children.push(el('h3', { text: 'Actions' }), actions);

  $('detail').replaceChildren(...children);
}

function renderActions(ticket) {
  const box = el('div', { className: 'actions' });

  if (can('ASSIGN_TICKET')) {
    const select = el('select', { attrs: { 'aria-label': 'Agent' } });
    for (const agent of state.agents) {
      select.append(el('option', { text: `${agent.name} (${agent.username})`, attrs: { value: agent.username } }));
    }
    const assign = el('button', { text: 'Assign', className: 'primary', attrs: { type: 'button' } });
    assign.addEventListener('click', () => runAction(
      () => api('POST', `/api/tickets/${encodeURIComponent(ticket.id)}/assign`, { agent: select.value }),
      `Ticket ${ticket.id} assigned to ${select.value}.`));
    box.append(select, assign);
  }

  const statusActions = [
    ['START', 'START_TICKET', 'Start work', 'is now in progress'],
    ['RESOLVE', 'RESOLVE_TICKET', 'Resolve', 'is resolved'],
    ['CLOSE', 'CLOSE_TICKET', 'Close', 'is closed'],
  ];
  for (const [action, permission, label, outcome] of statusActions) {
    if (!can(permission)) continue;
    const button = el('button', { text: label, attrs: { type: 'button' } });
    button.addEventListener('click', () => runAction(
      () => api('POST', `/api/tickets/${encodeURIComponent(ticket.id)}/status`, { action }),
      `Ticket ${ticket.id} ${outcome}.`));
    box.append(button);
  }

  return box.childElementCount > 0 ? box : null;
}

async function runAction(request, successMessage) {
  const buttons = $('detail').querySelectorAll('button');
  buttons.forEach((button) => { button.disabled = true; });
  try {
    await request();
    flash(successMessage);
    await loadTickets();
    if (state.tickets.some((t) => t.id === state.selectedId)) {
      await selectTicket(state.selectedId);
    } else {
      state.selectedId = null;
      renderDetailPlaceholder();
    }
  } catch (error) {
    flash(error.message, false);
    buttons.forEach((button) => { button.disabled = false; });
  }
}

$('login-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  $('login-error').hidden = true;
  try {
    const user = await api('POST', '/api/login', { username: $('username').value.trim() });
    $('username').value = '';
    await showApp(user);
  } catch (error) {
    $('login-error').textContent = error.message;
    $('login-error').hidden = false;
  }
});

document.querySelectorAll('.chip').forEach((chip) => {
  chip.addEventListener('click', () => {
    $('username').value = chip.dataset.user;
    $('login-form').requestSubmit();
  });
});

$('logout').addEventListener('click', async () => {
  await api('POST', '/api/logout').catch(() => {});
  showLogin();
});

$('new-ticket').addEventListener('click', () => {
  $('create-form').hidden = false;
  $('message').focus();
});

$('cancel-create').addEventListener('click', () => {
  $('create-form').reset();
  $('create-form').hidden = true;
});

$('create-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const body = { message: $('message').value };
  if (can('VIEW_ALL_TICKETS')) {
    body.customerName = $('customer-name').value;
    body.customerEmail = $('customer-email').value;
    body.customerId = $('customer-id').value;
  }
  try {
    const ticket = await api('POST', '/api/tickets', body);
    $('create-form').reset();
    $('create-form').hidden = true;
    flash(`Ticket ${ticket.id} created: ${ticket.category}, ${ticket.priority} priority, routed to ${ticket.assignedTeam}.`);
    await loadTickets();
    await selectTicket(ticket.id);
  } catch (error) {
    flash(error.message, false);
  }
});

(async () => {
  try {
    await showApp(await api('GET', '/api/me'));
  } catch {
    showLogin();
  }
})();
