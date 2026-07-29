const define = (id, flow, state, title, eyebrow, renderer) =>
  Object.freeze({ id, flow, state, title, eyebrow, renderer });

export const SCREENS = Object.freeze([
  define("login-default", "access", "default", "Login", "Acesso", "login"),
  define("orders-loaded", "orders", "loaded", "Pedidos", "Principal", "orders"),
  define("orders-loading", "orders", "loading", "Pedidos carregando", "Estado", "ordersState"),
  define("orders-empty", "orders", "empty", "Nenhum pedido", "Estado", "ordersState"),
  define("orders-error", "orders", "error", "Erro em pedidos", "Estado", "ordersState"),
  define("order-detail", "orders", "loaded", "Detalhes do pedido", "Pagamento", "detail"),
  define("amount-default", "payment", "default", "Valor do pagamento", "Pagamento", "amount"),
  define("amount-loading", "payment", "loading", "Consultando condições", "Estado", "amountState"),
  define("amount-error", "payment", "error", "Erro ao consultar", "Estado", "amountState"),
  define("method-selection", "payment", "default", "Forma de pagamento", "Pagamento", "method"),
  define("installments-selection", "payment", "default", "Parcelamento", "Pagamento", "installments"),
  define("review-payment", "payment", "default", "Revisar pagamento", "Pagamento", "review"),
  define("payment-processing", "payment", "processing", "Processando", "Pagamento", "processing"),
  define("payment-approved", "payment", "success", "Pagamento aprovado", "Resultado", "approved"),
  define("simulator-loaded", "simulator", "loaded", "Simulação", "Parcelas", "simulator"),
  define("simulator-empty", "simulator", "empty", "Simulação vazia", "Estado", "simulatorState"),
  define("simulator-loading", "simulator", "loading", "Simulação carregando", "Estado", "simulatorState"),
  define("simulator-error", "simulator", "error", "Erro na simulação", "Estado", "simulatorState"),
]);

export const HAPPY_PATH = Object.freeze([
  "login-default",
  "orders-loaded",
  "order-detail",
  "amount-default",
  "method-selection",
  "installments-selection",
  "review-payment",
  "payment-processing",
  "payment-approved",
]);

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
