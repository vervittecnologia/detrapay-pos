const money = (value) =>
  new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(value);

const systemBar = () => `
  <div class="system-bar"><strong>9:41</strong><span>▮▮▮  Wi-Fi  100%</span></div>`;

const appBar = (title, subtitle = "") => `
  <header class="app-bar">
    <button class="icon-button" type="button" aria-label="Voltar">‹</button>
    <div><strong>${title}</strong>${subtitle ? `<span>${subtitle}</span>` : ""}</div>
    <button class="icon-button quiet" type="button" aria-label="Ajuda">?</button>
  </header>`;

const stepper = (step) => `
  <div class="stepper" aria-label="Etapa ${step} de 5">
    ${[1, 2, 3, 4, 5].map((item) => `<i class="${item <= step ? "done" : ""}"></i>`).join("")}
  </div>`;

const button = (label, action, kind = "primary") =>
  `<button class="button button-${kind}" type="button" data-action="${action}">${label}</button>`;

const statusPanel = (kind, title, detail, action = "") => `
  <section class="state-panel state-${kind}">
    <span class="state-symbol">${kind === "error" ? "!" : kind === "loading" ? "◌" : "○"}</span>
    <h2>${title}</h2><p>${detail}</p>${action ? button(action, "retry", "secondary") : ""}
  </section>`;

const bottomNav = (active = "inicio") => `
  <nav class="bottom-nav" aria-label="Navegação principal">
    <button class="${active === "inicio" ? "active" : ""}" type="button"><b>⌂</b><span>Início</span></button>
    <button class="${active === "vendas" ? "active" : ""}" type="button"><b>◆</b><span>Vendas</span></button>
    <button type="button"><b>●</b><span>Perfil</span></button>
  </nav>`;

const orderCard = (initials, name, document, value, status, action = "") => `
  <button class="order-card" type="button" ${action ? `data-action="${action}"` : ""}>
    <span class="avatar">${initials}</span>
    <span class="order-copy"><strong>${name}</strong><small>${document}</small><em>Vendedor: Marco Aurélio</em></span>
    <span class="order-meta"><mark class="${status === "Finalizado" ? "success" : "warning"}">${status}</mark><strong>${money(value)}</strong><small>Hoje</small></span>
  </button>`;

const paymentOption = (symbol, label, detail, action = "select-credit") => `
  <button class="payment-option" type="button" data-action="${action}">
    <span class="option-icon">${symbol}</span>
    <span><strong>${label}</strong><small>${detail}</small></span>
    <b>›</b>
  </button>`;

const installmentRow = (count, total, fee, selected = false) => `
  <button class="installment-row ${selected ? "selected" : ""}" type="button" data-action="continue-installments">
    <span class="radio">${selected ? "●" : "○"}</span>
    <span><strong>${count}x de ${money(total / count)}</strong><small>Juros ${money(fee)} · total ${money(total)}</small></span>
  </button>`;

const amountHero = (label, value, meta = "") => `
  <section class="amount-hero"><span>${label}</span><strong>${money(value)}</strong>${meta ? `<small>${meta}</small>` : ""}</section>`;

const renderers = {
  login: () => `
    <div class="login-visual"><img src="./assets/figma-login.png" alt="Ilustração documental Detrapay extraída do Figma"></div>
    <main class="login-sheet">
      <h1>Login</h1>
      <label class="field"><span>CPF</span><input value="072.990.879-00" aria-label="CPF"></label>
      <label class="field"><span>Senha</span><span class="input-with-icon"><input type="password" value="detrapay2026" aria-label="Senha"><b>◉</b></span></label>
      ${button("Fazer login", "login")}
      <button class="text-button" type="button">Esqueci minha senha</button>
    </main>`,
  orders: () => `
    ${systemBar()}<header class="brand-header">
      <span class="dealer-mark">D</span><div><strong>Detrapay</strong><small>DOCUMENTAÇÃO VEICULAR FÁCIL E RÁPIDA</small></div>
      <button class="notification" type="button" aria-label="Notificações">●</button>
    </header>
    <main class="screen-content home-content">
      <section class="operator-card"><span class="operator-icon">●</span><div><strong>ID Operador</strong><small>Detrapay Motors</small></div></section>
      <div class="section-heading"><h2>Pedidos recentes</h2><button type="button">Ver todos</button></div>
      ${orderCard("RA", "Ricardo S. Almeida", "123.456.789-00", 18490, "Finalizado", "select-order")}
      ${orderCard("FL", "Fernanda Lima", "987.654.321-00", 24100, "Pendente")}
    </main>${bottomNav("inicio")}`,
  ordersState: ({ state }) => `
    ${systemBar()}${appBar("Pedidos", "Detrapay Motors")}
    <main class="screen-content state-layout">${
      state === "loading"
        ? statusPanel("loading", "Carregando pedidos", "Sincronizando as vendas mais recentes.")
        : state === "empty"
          ? statusPanel("empty", "Nenhum pedido encontrado", "Novos pedidos aparecerão aqui assim que forem criados.", "Atualizar")
          : statusPanel("error", "Falha ao carregar pedidos", "Verifique a conexão e tente novamente.", "Tentar novamente")
    }</main>${bottomNav("vendas")}`,
  detail: () => `
    ${systemBar()}${appBar("Detalhes do pedido", "#1048 · Finalizado")}
    <main class="screen-content">
      ${amountHero("SALDO DISPONÍVEL PARA RECEBER", 18490, "Pedido total · pagamento ainda não iniciado")}
      <section class="identity-block"><span class="avatar large">RA</span><div><h1>Ricardo S. Almeida</h1><p>123.456.789-00</p></div><mark class="success">ATIVO</mark></section>
      <h2 class="content-title">Resumo do pedido</h2>
      <dl class="data-list"><div><dt>Vendedor</dt><dd>Marco Aurélio</dd></div><div><dt>Data</dt><dd>29 Jul, 2026</dd></div><div><dt>Categoria</dt><dd>Motocicleta</dd></div><div><dt>Número</dt><dd>#1048</dd></div></dl>
      <section class="info-band"><b>i</b><span>Confira o cliente e o valor antes de iniciar a cobrança.</span></section>
    </main><footer class="action-footer">${button("Receber pagamento", "continue-detail")}</footer>`,
  amount: () => `
    ${systemBar()}${appBar("Valor da cobrança", "Pedido #1048")}${stepper(2)}
    <main class="screen-content amount-screen"><p>Digite o valor original que será recebido agora.</p>
      <div class="amount-display"><span>R$</span><strong>125,00</strong><small>Disponível no pedido: ${money(18490)}</small></div>
      <div class="keypad">${["1","2","3","4","5","6","7","8","9","00","0","⌫"].map((key) => `<button type="button">${key}</button>`).join("")}</div>
    </main><footer class="action-footer">${button("Continuar", "continue-amount")}</footer>`,
  amountState: ({ state }) => `
    ${systemBar()}${appBar("Valor da cobrança", "Pedido #1048")}${stepper(2)}
    <main class="screen-content state-layout">${
      state === "loading"
        ? statusPanel("loading", "Consultando condições", "Buscando taxas e parcelas disponíveis.")
        : statusPanel("error", "Não foi possível consultar parcelas", "Mantenha o valor informado e tente a consulta novamente.", "Tentar novamente")
    }</main>`,
  method: () => `
    ${systemBar()}${appBar("Forma de pagamento", `${money(125)} · Pedido #1048`)}${stepper(3)}
    <main class="screen-content"><h1 class="screen-title">Como o cliente vai pagar?</h1><p class="screen-lead">Escolha uma opção para continuar.</p>
      <div class="option-group"><span>PAGAMENTO NA MAQUININHA</span>
        ${paymentOption("Cr", "Crédito", "À vista ou parcelado")}
        ${paymentOption("Db", "Débito", "Pagamento à vista", "select-debit")}
        ${paymentOption("Px", "Pix", "QR Code com aprovação imediata", "select-pix")}
      </div>
      <div class="option-group"><span>SOMENTE REGISTRO</span>${paymentOption("R$", "Dinheiro", "Registrar recebimento", "select-cash")}</div>
    </main>`,
  installments: () => `
    ${systemBar()}${appBar("Parcelamento", `Crédito · ${money(125)}`)}${stepper(4)}
    <main class="screen-content"><h1 class="screen-title">Escolha as parcelas</h1><p class="screen-lead">O cliente verá o valor total antes da cobrança.</p>
      <div class="installment-list">
        ${installmentRow(1, 128.75, 3.75)}
        ${installmentRow(2, 132, 7)}
        ${installmentRow(3, 135, 10, true)}
        ${installmentRow(4, 139, 14)}
        ${installmentRow(6, 146, 21)}
      </div>
    </main><footer class="action-footer">${button("Revisar pagamento", "continue-installments")}</footer>`,
  review: () => `
    ${systemBar()}${appBar("Revisar pagamento", "Última etapa antes de cobrar")}${stepper(5)}
    <main class="screen-content">
      ${amountHero("TOTAL A COBRAR", 135, "3x de R$ 45,00 no crédito")}
      <h2 class="content-title">Resumo transparente</h2>
      <dl class="finance-list"><div><dt>Valor original</dt><dd>${money(125)}</dd></div><div><dt>Juros e taxas</dt><dd>${money(10)}</dd></div><div><dt>Parcelamento</dt><dd>3x de ${money(45)}</dd></div><div class="total"><dt>Total final</dt><dd>${money(135)}</dd></div></dl>
      <section class="customer-mini"><span class="avatar">RA</span><div><small>CLIENTE</small><strong>Ricardo S. Almeida</strong><em>Pedido #1048</em></div></section>
      <section class="info-band warning-band"><b>!</b><span>Após confirmar, mantenha o app aberto até o resultado final.</span></section>
    </main><footer class="action-footer">${button("Confirmar pagamento", "confirm-payment")}</footer>`,
  processing: () => `
    ${systemBar()}${appBar("Pagamento em andamento")}
    <main class="screen-content processing-layout"><div class="processing-orbit"><span>Cr</span></div>
      <h1>Processando pagamento</h1><p>Aguarde a confirmação da maquininha. Não feche o aplicativo.</p>
      <dl class="compact-summary"><div><dt>Total</dt><dd>${money(135)}</dd></div><div><dt>Forma</dt><dd>Crédito · 3x</dd></div></dl>
      ${button("Simular aprovação", "finish", "ghost")}
    </main>`,
  approved: () => `
    ${systemBar()}<header class="success-header">Detalhe da transação</header>
    <main class="screen-content approved-layout"><span class="approved-icon">✓</span>
      <h1>Pagamento realizado<br>com sucesso!</h1><p>29/07/2026 às 09:41</p>
      <div class="approved-value"><small>Valor do pagamento</small><strong>${money(135)}</strong></div>
      <div class="approved-customer"><small>Para</small><strong>Ricardo S. Almeida</strong><span>CPF: 123.456.789-00</span></div>
      <button class="receipt-button" type="button">Ver comprovante</button>
      <section class="approved-actions">${button("Ir para o início", "finish")}${button("Ver pedidos", "finish", "secondary")}</section>
    </main>`,
  simulator: () => `
    ${systemBar()}${appBar("Simular parcelamento", "Sem iniciar uma cobrança")}
    <main class="screen-content"><label class="simulator-value"><span>VALOR ORIGINAL</span><strong>${money(500)}</strong></label>
      <h1 class="screen-title">Condições disponíveis</h1><p class="screen-lead">Compare parcela, juros e total final.</p>
      <div class="installment-list">${installmentRow(1, 515, 15)}${installmentRow(2, 525, 25)}${installmentRow(3, 533, 33, true)}${installmentRow(6, 558, 58)}</div>
      <section class="simulator-total"><span>Opção selecionada</span><strong>3x de ${money(177.67)}</strong><small>Total ${money(533)}</small></section>
    </main>`,
  simulatorState: ({ state }) => `
    ${systemBar()}${appBar("Simular parcelamento", "Sem iniciar uma cobrança")}
    <main class="screen-content state-layout">${
      state === "loading"
        ? statusPanel("loading", "Calculando parcelas", "Consultando as condições disponíveis.")
        : state === "empty"
          ? statusPanel("empty", "Informe um valor", "As opções aparecerão após a simulação.", "Digitar valor")
          : statusPanel("error", "Não foi possível simular", "Revise o valor e tente novamente.", "Tentar novamente")
    }</main>`,
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
