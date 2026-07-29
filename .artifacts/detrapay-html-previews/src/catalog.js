const define = (id, flow, state, title, eyebrow, renderer, actions = {}) =>
  Object.freeze({
    id,
    flow,
    state,
    title,
    eyebrow,
    renderer,
    actions: Object.freeze({ ...actions }),
  });

export const SCREENS = Object.freeze([
  define("splash", "access", "default", "Splash", "Acesso", "splash", {
    login: "login-default",
    authenticated: "orders-loading",
  }),
  define("login-default", "access", "default", "Login", "Acesso", "login", {
    submit: "login-loading",
  }),
  define("login-loading", "access", "loading", "Entrando", "Acesso", "loginLoading", {
    success: "orders-loading",
    fail: "login-error",
  }),
  define("login-error", "access", "error", "Falha no login", "Acesso", "loginError", {
    retry: "login-default",
  }),
  define("orders-loading", "orders", "loading", "Pedidos carregando", "Pedidos", "ordersState", {
    loaded: "orders-loaded",
    empty: "orders-empty",
    fail: "orders-error",
  }),
  define("orders-loaded", "orders", "loaded", "Pedidos", "Pedidos", "orders", {
    "new-order": "order-data",
    "open-detail": "order-detail",
    "open-simulator": "simulator-empty",
  }),
  define("orders-empty", "orders", "empty", "Nenhum pedido", "Pedidos", "ordersState", {
    reload: "orders-loading",
    "new-order": "order-data",
  }),
  define("orders-error", "orders", "error", "Erro em pedidos", "Pedidos", "ordersState", {
    retry: "orders-loading",
  }),
  define("order-data", "registration", "default", "Dados do pedido", "Novo pedido", "orderData", {
    simulate: "registration-loading",
    close: "leave-data-dialog",
  }),
  define("registration-loading", "registration", "loading", "Calculando valores", "Novo pedido", "registrationLoading", {
    loaded: "registration-resume",
    fail: "order-data",
  }),
  define("registration-resume", "registration", "loaded", "Resumo do pedido", "Novo pedido", "registrationResume", {
    discount: "discount-dialog",
    create: "order-creating",
    close: "leave-resume-dialog",
  }),
  define("order-creating", "registration", "loading", "Criando pedido", "Novo pedido", "orderCreating", {
    created: "order-created-detail",
    fail: "registration-resume",
  }),
  define("order-created-detail", "registration", "success", "Pedido criado", "Novo pedido", "createdDetail", {
    finish: "orders-loaded",
  }),
  define("order-detail", "payment", "loaded", "Detalhes do pedido", "Pagamento", "orderDetail", {
    pay: "payment-method",
    back: "orders-loaded",
  }),
  define("payment-method", "payment", "default", "Método de pagamento", "Pagamento", "paymentMethod", {
    "select-credit": "payment-amount",
    "select-direct": "payment-amount",
    back: "order-detail",
  }),
  define("payment-amount", "payment", "default", "Valor da cobrança", "Pagamento", "paymentAmount", {
    "continue-credit": "payment-fees-loading",
    "continue-direct": "payment-review",
    back: "payment-method",
  }),
  define("payment-fees-loading", "payment", "loading", "Consultando taxas", "Pagamento", "feesLoading", {
    "fees-loaded": "payment-installments",
    fail: "payment-amount",
  }),
  define("payment-installments", "payment", "loaded", "Parcelamento", "Pagamento", "installments", {
    review: "payment-review",
    back: "payment-amount",
  }),
  define("payment-review", "payment", "default", "Revisar pagamento", "Pagamento", "paymentReview", {
    confirm: "payment-waiting",
    "confirm-record": "orders-loaded",
    back: "payment-installments",
    "back-direct": "payment-amount",
  }),
  define("payment-waiting", "payment", "loading", "Aguardando pagamento", "Pagamento", "paymentWaiting", {
    approved: "payment-approved",
    fail: "payment-failed",
    back: "payment-review",
  }),
  define("payment-approved", "payment", "success", "Pagamento aprovado", "Pagamento", "paymentApproved", {
    finish: "orders-loaded",
  }),
  define("payment-failed", "payment", "error", "Falha no pagamento", "Pagamento", "paymentFailed", {
    retry: "payment-waiting",
    review: "payment-review",
  }),
  define("simulator-empty", "simulator", "default", "Simulador", "Simulador", "simulator", {
    consult: "simulator-loading",
    close: "orders-loaded",
  }),
  define("simulator-loading", "simulator", "loading", "Consultando parcelas", "Simulador", "simulator", {
    loaded: "simulator-loaded",
    empty: "simulator-empty-result",
    fail: "simulator-error",
    close: "orders-loaded",
  }),
  define("simulator-loaded", "simulator", "loaded", "Parcelas simuladas", "Simulador", "simulator", {
    copy: "simulator-loaded",
    share: "simulator-loaded",
    close: "orders-loaded",
  }),
  define("simulator-empty-result", "simulator", "empty", "Sem parcelas", "Simulador", "simulator", {
    retry: "simulator-empty",
    close: "orders-loaded",
  }),
  define("simulator-error", "simulator", "error", "Erro no simulador", "Simulador", "simulator", {
    retry: "simulator-empty",
    close: "orders-loaded",
  }),
  define("discount-dialog", "dialogs", "default", "Adicionar desconto", "Diálogo", "discountDialog", {
    cancel: "registration-resume",
    apply: "registration-resume",
  }),
  define("leave-data-dialog", "dialogs", "warning", "Sair sem salvar", "Diálogo", "leaveDialog", {
    cancel: "order-data",
    leave: "orders-loaded",
  }),
  define("leave-resume-dialog", "dialogs", "warning", "Sair sem salvar", "Diálogo", "leaveDialog", {
    cancel: "registration-resume",
    leave: "orders-loaded",
  }),
]);

export const JOURNEYS = Object.freeze({
  registration: Object.freeze({
    id: "registration",
    label: "Criar novo pedido",
    start: "orders-loaded",
    steps: Object.freeze([
      "orders-loaded",
      "order-data",
      "registration-loading",
      "registration-resume",
      "order-creating",
      "order-created-detail",
      "orders-loaded",
    ]),
  }),
  payment: Object.freeze({
    id: "payment",
    label: "Pagar pedido existente",
    start: "orders-loaded",
    steps: Object.freeze([
      "orders-loaded",
      "order-detail",
      "payment-method",
      "payment-amount",
      "payment-fees-loading",
      "payment-installments",
      "payment-review",
      "payment-waiting",
      "payment-approved",
      "orders-loaded",
    ]),
  }),
  simulator: Object.freeze({
    id: "simulator",
    label: "Simular parcelas",
    start: "orders-loaded",
    steps: Object.freeze([
      "orders-loaded",
      "simulator-empty",
      "simulator-loading",
      "simulator-loaded",
      "orders-loaded",
    ]),
  }),
});

export function getScreen(id) {
  const screen = SCREENS.find((candidate) => candidate.id === id);
  if (!screen) throw new Error(`Unknown preview screen: ${id}`);
  return screen;
}

export function getScreensByFlow(flow) {
  return flow === "all"
    ? [...SCREENS]
    : SCREENS.filter((screen) => screen.flow === flow);
}

export function resolveAction(screenId, action) {
  const target = getScreen(screenId).actions[action];
  if (!target) throw new Error(`Unknown action "${action}" for screen "${screenId}"`);
  getScreen(target);
  return target;
}
