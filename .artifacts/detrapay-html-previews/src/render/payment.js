import {
  brl,
  detailLine,
  fullHeader,
  outlineButton,
  screenTitle,
  solidButton,
  statePanel,
  tabBar,
} from "./primitives.js";

const methodRow = (icon, title, detail, action) => `
  <button class="fig-method-row" type="button" data-action="${action}">
    <i>${icon}</i><span><strong>${title}</strong><small>${detail}</small></span><b>›</b>
  </button>`;

const keypad = (loading = false) => `
  <section class="fig-amount-card">
    <span>Valor da cobrança</span>
    <strong>${brl(5000)}</strong>
    <small>Saldo restante: ${brl(5000)}</small>
  </section>
  <button class="fig-balance-shortcut" type="button">Usar saldo restante</button>
  <div class="fig-keypad ${loading ? "is-disabled" : ""}">
    ${["1", "2", "3", "4", "5", "6", "7", "8", "9", "00", "0", "⌫"].map((key) => `<button type="button">${key}</button>`).join("")}
  </div>`;

export const paymentRenderers = {
  paymentMethod: () => `
    ${fullHeader()}${screenTitle("Método de pagamento", "back")}
    <main class="fig-content">
      <h2 class="fig-section-label">Pagamentos online</h2>
      ${methodRow("Cr", "Crédito", "À vista ou parcelado", "select-credit")}
      ${methodRow("Db", "Débito", "Pagamento imediato", "select-direct")}
      ${methodRow("Px", "Pix", "QR Code na maquininha", "select-direct")}
      <h2 class="fig-section-label">Registrar sem TEF</h2>
      ${methodRow("R$", "Dinheiro", "Registro manual", "select-direct")}
      ${methodRow("Lj", "Crédito da loja", "Registro manual", "select-direct")}
    </main>${tabBar("pedidos")}`,
  paymentAmount: () => `
    ${fullHeader()}${screenTitle("Valor da cobrança", "back")}
    <main class="fig-content fig-payment-content">${keypad(false)}</main>
    <footer class="fig-footer split">
      ${outlineButton("Continuar sem taxas", "continue-direct")}
      ${solidButton("Consultar crédito", "continue-credit")}
    </footer>${tabBar("pedidos")}`,
  feesLoading: () => `
    ${fullHeader()}${screenTitle("Valor da cobrança")}
    <main class="fig-content fig-payment-content">
      ${keypad(true)}
      <div class="fig-loading-band"><i>◌</i><span>Consultando taxas e parcelas...</span></div>
    </main>
    <footer class="fig-footer">${solidButton("Ver parcelas", "fees-loaded")}</footer>
    ${tabBar("pedidos")}`,
  installments: () => `
    ${fullHeader()}${screenTitle("Parcelamento", "back")}
    <main class="fig-content">
      <section class="fig-total-field"><span>Valor original</span><strong>${brl(5000)}</strong></section>
      <div class="fig-installment-list">
        <label><input type="radio" name="installment"><span><b>1x de ${brl(5140)}</b><small>Taxa ${brl(140)} · Total ${brl(5140)}</small></span></label>
        <label class="selected"><input type="radio" checked name="installment"><span><b>3x de ${brl(1763.33)}</b><small>Taxa ${brl(290)} · Total ${brl(5290)}</small></span></label>
        <label><input type="radio" name="installment"><span><b>6x de ${brl(915)}</b><small>Taxa ${brl(490)} · Total ${brl(5490)}</small></span></label>
      </div>
    </main>
    <footer class="fig-footer">${solidButton("Revisar pagamento", "review")}</footer>
    ${tabBar("pedidos")}`,
  paymentReview: () => `
    ${fullHeader()}${screenTitle("Revisar pagamento", "back")}
    <main class="fig-content">
      <section class="fig-review-hero"><span>Total a cobrar</span><strong>${brl(5290)}</strong><small>Crédito · 3 parcelas</small></section>
      ${detailLine("Valor original", brl(5000))}
      ${detailLine("Taxa", brl(290))}
      ${detailLine("Parcelamento", `3x de ${brl(1763.33)}`)}
      ${detailLine("Total final", brl(5290), true)}
      <div class="fig-info-note">Confira os dados antes de iniciar a cobrança na maquininha.</div>
    </main>
    <footer class="fig-footer stacked">
      ${solidButton("Confirmar e cobrar", "confirm")}
      ${outlineButton("Registrar sem TEF", "confirm-record")}
    </footer>
    ${tabBar("pedidos")}`,
  paymentWaiting: () => `
    ${fullHeader()}${screenTitle("Aguardando pagamento", "back")}
    <main class="fig-content fig-state-page">
      <section class="fig-waiting">
        <span class="fig-terminal-icon">▣</span>
        <h2>Aguardando confirmação</h2>
        <p>Insira, aproxime o cartão ou conclua a operação na maquininha.</p>
        <strong>${brl(5290)}</strong>
        <div class="fig-waiting-actions">
          ${solidButton("Simular aprovação", "approved")}
          ${outlineButton("Simular falha", "fail")}
        </div>
      </section>
    </main>${tabBar("pedidos")}`,
  paymentApproved: () => `
    <header class="fig-success-header">Detalhe da transação</header>
    <main class="fig-approved">
      <span class="fig-approved-icon">✓</span>
      <h1>Pagamento realizado<br>com sucesso!</h1>
      <small>Hoje às 14:32</small>
      <div><span>Valor do pagamento</span><strong>${brl(5290)}</strong></div>
      <p>Pedido<br><strong>#12345 · João Pedro da Silva</strong></p>
      <section>${solidButton("Voltar aos pedidos", "finish")}</section>
    </main>`,
  paymentFailed: () => `
    ${fullHeader()}${screenTitle("Falha no pagamento")}
    <main class="fig-content fig-state-page">
      ${statePanel("error", "Pagamento não concluído", "A maquininha recusou ou interrompeu a operação.")}
      <div class="fig-state-actions">
        ${solidButton("Tentar novamente", "retry")}
        ${outlineButton("Voltar à revisão", "review")}
      </div>
    </main>${tabBar("pedidos")}`,
};
