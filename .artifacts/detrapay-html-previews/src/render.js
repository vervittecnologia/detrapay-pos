const brl = (value) =>
  new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);

const statusBar = (blue = true) => `
  <div class="fig-status ${blue ? "fig-status-blue" : ""}">
    <strong>23:59</strong><span>▮▮▮  Wi-Fi  100</span>
  </div>`;

const userBar = (section = "") => `
  <header class="fig-userbar">
    <span class="fig-user"><i>RM</i><strong>Rodrigo Maia</strong><b>⌄</b></span>
    <span class="fig-location">⌖ Concessionária Mito</span>
    ${section ? `<h1>${section}</h1>` : ""}
  </header>`;

const fullHeader = (section = "") => `${statusBar()}${userBar(section)}`;

const tabBar = (active = "emplacamento") => `
  <nav class="fig-tabs" aria-label="Navegação principal">
    <button class="${active === "emplacamento" ? "active" : ""}" type="button"><b>▣</b><span>Emplacamento</span></button>
    <button class="${active === "pedidos" ? "active" : ""}" type="button"><b>▤</b><span>Pedidos</span></button>
  </nav>`;

const solidButton = (label, action) =>
  `<button class="fig-button fig-button-primary" type="button" data-action="${action}">${label}</button>`;

const outlineButton = (label, action = "") =>
  `<button class="fig-button fig-button-outline" type="button" ${action ? `data-action="${action}"` : ""}>${label}</button>`;

const screenTitle = (title, back = true, menu = false) => `
  <header class="fig-screen-title">
    ${back ? `<button type="button" aria-label="Voltar">‹</button>` : "<span></span>"}
    <h1>${title}</h1>
    ${menu ? `<button type="button" aria-label="Mais opções">•••</button>` : "<span></span>"}
  </header>`;

const field = (label, value = "", required = false, search = false) => `
  <label class="fig-field"><span>${label}${required ? "*" : ""}</span>
    <span class="fig-field-value ${value ? "" : "placeholder"}">${value || " "}${search ? "<b>⌕</b>" : ""}</span>
  </label>`;

const statePanel = (kind, title, detail, action = "") => `
  <section class="fig-state">
    <span class="fig-state-icon ${kind}">${kind === "error" ? "!" : kind === "loading" ? "◌" : "○"}</span>
    <h2>${title}</h2><p>${detail}</p>${action ? outlineButton(action, "retry") : ""}
  </section>`;

const orderCard = (name, status, date, kind = "Primeiro Emplacamento", action = "") => `
  <button class="fig-order-card" type="button" ${action ? `data-action="${action}"` : ""}>
    <span><strong>${name}</strong><small>${kind}</small></span>
    <span><mark class="status-${status.toLowerCase()}">${status}</mark><small>${date}</small></span>
  </button>`;

const detailLine = (label, value, emphasized = false) => `
  <div class="fig-detail-line ${emphasized ? "emphasized" : ""}"><span>${label}</span><strong>${value}</strong></div>`;

const dialogShell = (title, body, actions) => `
  ${fullHeader()}<main class="fig-content muted-background">
    <section class="fig-placeholder-page">${field("CPF/CNPJ do cliente", "006.000.000-00", true)}${field("Nome do cliente", "João Pedro da Silva")}</section>
  </main>
  <div class="fig-dialog-layer"><section class="fig-dialog"><header><strong>${title}</strong><button type="button">×</button></header>${body}<footer>${actions}</footer></section></div>`;

const renderers = {
  splash: () => `
    <main class="fig-splash">
      <div class="fig-logo-lockup"><span>✣</span><strong>Detrapay</strong></div>
      <small>Documentação veicular fácil e rápida</small>
    </main>${solidButton("Continuar", "next")}`,
  login: () => `
    <div class="fig-login-hero">
      <div class="fig-logo-lockup"><span>✣</span><strong>Detrapay</strong></div>
      <div class="fig-document-art"><i>⌕</i><b>▤</b><em>▱</em></div>
    </div>
    <main class="fig-login-panel"><h1>Login</h1>
      ${field("CNPJ", "15.945.405/0001-70")}
      ${field("Senha", "**************")}
      ${solidButton("Fazer login", "next")}
    </main>`,
  store: () => `
    ${statusBar()}<main class="fig-store">
      <h1>Bem-vindo ao Detrapay!</h1>
      <p>Você está na <strong>Concessionária Mito.</strong> Toque no seu nome para continuar.</p>
      <button type="button" data-action="next">Rodrigo Maia</button>
      <button type="button">Luan Lemos</button>
      <button type="button">Roberto Silva</button>
      <div class="fig-store-art"><span>⌕</span><i></i><b></b></div>
    </main>`,
  orderData: () => `
    ${fullHeader()}<main class="fig-content">
      <h1 class="fig-form-heading">Dados do pedido</h1>
      ${field("CPF/CNPJ do cliente", "000.000.000-00", true)}
      ${field("Nome do cliente")}
      ${field("Whatsapp", "(85) 99000-0865", true)}
      ${field("Data de faturamento", "05/02/2025", true)}
      ${field("Valor do veículo", "R$ 350.000,00", true)}
      ${field("Tipo de veículo", "Selecione o tipo de veículo", true, true)}
      <label class="fig-check"><i></i>Veículo com alienação</label>
      <label class="fig-check checked"><i>✓</i>Placa especial</label>
    </main><footer class="fig-footer">${solidButton("Avançar", "next")}</footer>${tabBar("emplacamento")}`,
  breakdown: () => `
    ${fullHeader()}${screenTitle("Detalhamento do pagamento", true, true)}
    <main class="fig-content"><section class="fig-breakdown">
      ${detailLine("IPVA 2024", brl(522.05))}
      ${detailLine("IPVA 2025", brl(5162.53))}
      ${detailLine("Taxas do 1º emplacamento", brl(518.60))}
      ${detailLine("Taxa de alienação", brl(90.45))}
      ${detailLine("Cartório", brl(40))}
      ${detailLine("Placa Mercosul", brl(370))}
      ${detailLine("Serviço do despachante", brl(300))}
      ${detailLine("", brl(7003.63), true)}
    </section></main><footer class="fig-footer stacked">${solidButton("Avançar", "next")}${outlineButton("▧  Imprimir")}</footer>${tabBar("emplacamento")}`,
  paymentMethod: () => `
    ${fullHeader()}${screenTitle("Forma de pagamento")}
    <main class="fig-content payment-method-content">
      <section class="fig-total-field"><span>Valor total</span><strong>${brl(7003.63)}</strong></section>
      <p>Selecione a forma de pagamento</p>
      <section class="fig-payment-line"><label>Forma de pgto.<b>Dinheiro⌄</b></label><label>Valor<b>${brl(1003.63)}</b></label><button>⊕</button></section>
      <section class="fig-payment-line"><label>Forma de pgto.<b>Pix⌄</b></label><label>Valor<b>${brl(1000)}</b></label><button class="remove">▣</button></section>
      <section class="fig-payment-line with-installments"><label>Forma de pgto.<b>Cartão⌄</b></label><label>Valor<b>${brl(5000)}</b></label><button class="remove">▣</button><small>Parcelamento<br><strong>Em 10x sem juros de R$500,00 (R$5.000)</strong>⌄</small></section>
    </main><footer class="fig-footer">${solidButton("Criar pedido", "next")}</footer>${tabBar("emplacamento")}`,
  paymentMismatch: () => `
    ${fullHeader()}${screenTitle("Forma de pagamento")}
    <main class="fig-content">${statePanel("error", "A soma dos valores está incorreta", "Por favor, revise e tente novamente.", "Revisar pagamentos")}</main>
    <div class="fig-toast-error"><b>!</b><span>A soma dos valores está incorreta<br><small>Por favor, revise e tente novamente.</small></span></div>${tabBar("emplacamento")}`,
  orderCreated: () => `
    ${fullHeader()}<main class="fig-content">
      <header class="fig-order-heading"><h1>Pedido 12345</h1><button>Editar dados⌄</button></header>
      ${detailLine("CPF/CNPJ do cliente", "007.877.765-09")}
      ${detailLine("Nome do cliente", "João Pedro da Silva")}
      ${detailLine("Chassi", "MTX123456789")}
      ${detailLine("Tipo de veículo", "Motocicleta de 300cc")}
      <section class="fig-order-total"><span>Valor total</span><strong>${brl(7003.63)}</strong></section>
      <button class="fig-service-row"><span>▣ Cartão de crédito <mark>Pendente</mark><small>${brl(5000)} · 10x de R$500,00</small></span><b>›</b></button>
      <button class="fig-service-row"><span>◆ Pix <mark>Pago</mark><small>${brl(1000)}</small></span><b>›</b></button>
    </main><footer class="fig-footer">${solidButton("Concluir atendimento", "next")}</footer>${tabBar("emplacamento")}`,
  approved: () => `
    <header class="fig-success-header">Detalhe da transação</header>
    <main class="fig-approved"><span class="fig-approved-icon">✓</span><h1>Pagamento realizado<br>com sucesso!</h1><small>20/07/2020 às 10:00:05</small>
      <div><span>Valor do pagamento</span><strong>${brl(5000)}</strong></div>
      <p>Para<br><strong>Rodrigo Teles Oliveira</strong><small>CPF: 948.456.789-**</small></p>
      <button type="button">Ver comprovante</button>
      <section><span>O que você deseja fazer agora?</span>${solidButton("Ir para pedidos", "next")}</section>
    </main>`,
  reversed: () => `
    ${fullHeader()}${screenTitle("Pagamento estornado")}
    <main class="fig-content">${statePanel("warning", "Pagamento estornado", "A transação de R$ 5.000,00 foi cancelada e o pedido permanece pendente.", "Voltar ao pedido")}</main>${tabBar("pedidos")}`,
  orders: () => `
    ${fullHeader("Meus pedidos")}<main class="fig-content">
      <label class="fig-search">Nome ou CPF do cliente <b>⌕</b></label>
      ${orderCard("João Pedro da Silva", "Autorizado", "01/02/2025", "Primeiro Emplacamento", "next")}
      ${orderCard("Roberto Olaerde Lima da Silva", "Pendente", "29/01/2025")}
      ${orderCard("Gustavo Cavalcante Gomes", "Negado", "28/01/2025")}
    </main>${tabBar("pedidos")}`,
  ordersState: ({ state }) => `
    ${fullHeader("Meus pedidos")}<main class="fig-content fig-state-page">${
      state === "loading"
        ? statePanel("loading", "Carregando pedidos", "Buscando os atendimentos mais recentes.")
        : state === "empty"
          ? statePanel("empty", "Nenhum pedido encontrado", "Os novos atendimentos aparecerão aqui.", "Atualizar")
          : statePanel("error", "Não foi possível carregar", "Verifique a conexão e tente novamente.", "Tentar novamente")
    }</main>${tabBar("pedidos")}`,
  orderDetail: () => `
    ${fullHeader()}${screenTitle("Detalhes do pedido", true, true)}
    <main class="fig-content">
      <section class="fig-order-summary"><h1>João Pedro da Silva</h1><mark class="status-autorizado">Autorizado</mark><small>Primeiro Emplacamento · Pedido 12345</small></section>
      ${detailLine("CPF/CNPJ", "007.877.765-09")}
      ${detailLine("Veículo", "Motocicleta de 300cc")}
      ${detailLine("Data", "01/02/2025")}
      ${detailLine("Valor total", brl(7003.63), true)}
      <h2 class="fig-section-label">Pagamentos</h2>
      <button class="fig-service-row"><span>▣ Cartão de crédito <mark>Pago</mark><small>${brl(5000)} · 10 parcelas</small></span><b>›</b></button>
      <button class="fig-service-row"><span>◆ Pix <mark>Pago</mark><small>${brl(2003.63)}</small></span><b>›</b></button>
      ${outlineButton("Voltar aos pedidos", "next")}
    </main>${tabBar("pedidos")}`,
  notifications: () => `
    ${fullHeader()}${screenTitle("Notificações", true)}
    <main class="fig-content"><section class="fig-notification unread"><b>Pagamento autorizado</b><span>O pedido de João Pedro foi atualizado.</span><small>Agora</small></section>
      <section class="fig-notification"><b>Novo pedido criado</b><span>O pedido 12345 está aguardando pagamento.</span><small>Há 12 min</small></section>
    </main>${tabBar("pedidos")}`,
  discountDialog: () => dialogShell("Adicionar desconto",
    `<label class="fig-select-dialog">Selecione o item do desconto<b>Serviço do despachante⌄</b></label>${field("Insira o valor do desconto", "R$ 100,00")}`,
    `<button type="button">Cancelar</button><button type="button">Adicionar</button>`),
  leaveDialog: () => dialogShell("Deseja sair sem salvar?",
    `<p>Tem certeza que deseja sair sem salvar? As informações serão perdidas.</p>`,
    `<button type="button">Cancelar</button><button class="danger" type="button">Sair</button>`),
};

export function renderScreen(screen) {
  const renderer = renderers[screen.renderer];
  if (!renderer) throw new Error(`Unknown renderer: ${screen.renderer}`);
  return `<div class="device" data-screen-id="${screen.id}"><article class="screen">${renderer(screen)}</article></div>`;
}

export function renderGallery(screens) {
  return screens.map((screen, index) => `
    <article class="preview-card" tabindex="0" data-open-screen="${screen.id}">
      <header><span>${String(index + 1).padStart(2, "0")} · ${screen.eyebrow}</span><h2>${screen.title}</h2><mark>${screen.state}</mark></header>
      ${renderScreen(screen)}
    </article>`).join("");
}
