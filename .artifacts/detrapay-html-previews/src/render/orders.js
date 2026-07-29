import {
  brl,
  detailLine,
  fullHeader,
  orderCard,
  outlineButton,
  screenTitle,
  solidButton,
  statePanel,
  tabBar,
} from "./primitives.js";

const detailBase = (created = false) => `
  ${fullHeader()}${screenTitle(created ? "Pedido criado" : "Detalhes do pedido", "back", true)}
  <main class="fig-content fig-detail-content">
    <section class="fig-order-summary">
      <h1>João Pedro da Silva</h1>
      <mark class="${created ? "status-pendente" : "status-autorizado"}">${created ? "Criado" : "Pendente"}</mark>
      <small>Pedido 12345 · Primeiro emplacamento</small>
    </section>
    ${detailLine("Valor original", brl(7003.63))}
    ${detailLine("Já registrado", brl(created ? 0 : 2003.63))}
    ${detailLine("Saldo restante", brl(created ? 7003.63 : 5000), true)}
    <h2 class="fig-section-label">${created ? "Nenhum pagamento registrado" : "Pagamentos registrados"}</h2>
    ${created ? "" : `<button class="fig-service-row"><span>◆ Pix <mark>Pago</mark><small>${brl(2003.63)}</small></span><b>›</b></button>`}
    ${created
      ? solidButton("Voltar aos pedidos", "finish")
      : solidButton("Adicionar pagamento", "pay")}
  </main>${tabBar("pedidos")}`;

export const orderRenderers = {
  orders: () => `
    ${fullHeader("Meus pedidos")}
    <main class="fig-content fig-orders-content">
      <section class="fig-order-actions">
        <button type="button" data-action="new-order"><b>＋</b><span>Novo pedido</span></button>
        <button type="button" data-action="open-simulator"><b>%</b><span>Simular parcelas</span></button>
      </section>
      <label class="fig-search">Nome, CPF ou número do pedido <b>⌕</b></label>
      ${orderCard("João Pedro da Silva", "Pendente", "01/02/2025", "open-detail")}
      ${orderCard("Roberto Olaerde Lima", "Autorizado", "29/01/2025")}
      ${orderCard("Gustavo Cavalcante", "Negado", "28/01/2025")}
    </main>${tabBar("pedidos")}`,
  ordersState: ({ state }) => `
    ${fullHeader("Meus pedidos")}
    <main class="fig-content fig-state-page">${
      state === "loading"
        ? statePanel("loading", "Carregando pedidos", "Buscando os atendimentos mais recentes.", "loaded", "Ver pedidos")
        : state === "empty"
          ? `${statePanel("empty", "Nenhum pedido encontrado", "Os novos atendimentos aparecerão aqui.", "reload", "Atualizar")}
             ${solidButton("Criar novo pedido", "new-order")}`
          : statePanel("error", "Não foi possível carregar", "Verifique a conexão e tente novamente.", "retry")
    }</main>${tabBar("pedidos")}`,
  orderDetail: () => detailBase(false),
  createdDetail: () => detailBase(true),
};
