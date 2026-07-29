export const brl = (value) =>
  new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);

export const statusBar = (blue = true) => `
  <div class="fig-status ${blue ? "fig-status-blue" : ""}">
    <strong>23:59</strong><span>▮▮▮ &nbsp; Wi-Fi &nbsp; 100</span>
  </div>`;

export const userBar = (section = "") => `
  <header class="fig-userbar">
    <span class="fig-user"><i>RM</i><strong>Rodrigo Maia</strong><b>⌄</b></span>
    <span class="fig-location">⌖ Concessionária Mito</span>
    ${section ? `<h1>${section}</h1>` : ""}
  </header>`;

export const fullHeader = (section = "") => `${statusBar()}${userBar(section)}`;

export const tabBar = (active = "pedidos") => `
  <nav class="fig-tabs" aria-label="Navegação principal">
    <button class="${active === "emplacamento" ? "active" : ""}" type="button"><b>▣</b><span>Novo pedido</span></button>
    <button class="${active === "pedidos" ? "active" : ""}" type="button"><b>▤</b><span>Pedidos</span></button>
  </nav>`;

export const solidButton = (label, action, extraClass = "") =>
  `<button class="fig-button fig-button-primary ${extraClass}" type="button" data-action="${action}">${label}</button>`;

export const outlineButton = (label, action, extraClass = "") =>
  `<button class="fig-button fig-button-outline ${extraClass}" type="button" data-action="${action}">${label}</button>`;

export const screenTitle = (title, backAction = "", menu = false) => `
  <header class="fig-screen-title">
    ${backAction
      ? `<button type="button" data-action="${backAction}" aria-label="Voltar">‹</button>`
      : "<span></span>"}
    <h1>${title}</h1>
    ${menu ? `<button type="button" aria-label="Mais opções">•••</button>` : "<span></span>"}
  </header>`;

export const field = (label, value = "", required = false, search = false) => `
  <label class="fig-field"><span>${label}${required ? "*" : ""}</span>
    <span class="fig-field-value ${value ? "" : "placeholder"}">${value || " "}${search ? "<b>⌕</b>" : ""}</span>
  </label>`;

export const statePanel = (kind, title, detail, action = "", actionLabel = "") => `
  <section class="fig-state">
    <span class="fig-state-icon ${kind}">${kind === "error" ? "!" : kind === "loading" ? "◌" : "○"}</span>
    <h2>${title}</h2>
    <p>${detail}</p>
    ${action ? outlineButton(actionLabel || "Tentar novamente", action) : ""}
  </section>`;

export const detailLine = (label, value, emphasized = false) => `
  <div class="fig-detail-line ${emphasized ? "emphasized" : ""}">
    <span>${label}</span><strong>${value}</strong>
  </div>`;

export const orderCard = (name, status, date, action = "") => `
  <button class="fig-order-card" type="button" ${action ? `data-action="${action}"` : ""}>
    <span><strong>${name}</strong><small>Pedido de emplacamento</small></span>
    <span><mark class="status-${status.toLowerCase()}">${status}</mark><small>${date}</small></span>
  </button>`;

export const dialogShell = (title, body, actions) => `
  ${fullHeader()}<main class="fig-content muted-background">
    <section class="fig-placeholder-page">
      ${field("CPF/CNPJ do cliente", "006.000.000-00", true)}
      ${field("Nome do cliente", "João Pedro da Silva")}
    </section>
  </main>
  <div class="fig-dialog-layer">
    <section class="fig-dialog">
      <header><strong>${title}</strong><span>×</span></header>
      ${body}
      <footer>${actions}</footer>
    </section>
  </div>`;
