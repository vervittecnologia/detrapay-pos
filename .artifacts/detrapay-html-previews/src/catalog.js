const define = (id, flow, state, title, eyebrow, renderer) =>
  Object.freeze({ id, flow, state, title, eyebrow, renderer });

export const SCREENS = Object.freeze([
  define("splash", "access", "default", "Splash", "Acesso", "splash"),
  define("login-default", "access", "default", "Login", "Acesso", "login"),
  define("store-select", "access", "default", "Seleção de vendedor", "Loja", "store"),
  define("order-data", "registration", "default", "Dados do pedido", "Emplacamento", "orderData"),
  define("payment-breakdown", "registration", "default", "Detalhamento do pagamento", "Emplacamento", "breakdown"),
  define("payment-method", "registration", "default", "Forma de pagamento", "Emplacamento", "paymentMethod"),
  define("payment-mismatch", "registration", "error", "Soma incorreta", "Alerta", "paymentMismatch"),
  define("order-created", "registration", "success", "Pedido criado", "Pedido", "orderCreated"),
  define("payment-approved", "payment", "success", "Pagamento aprovado", "Pagamento", "approved"),
  define("payment-reversed", "payment", "warning", "Pagamento estornado", "Pagamento", "reversed"),
  define("orders-loaded", "orders", "loaded", "Meus pedidos", "Pedidos", "orders"),
  define("orders-loading", "orders", "loading", "Pedidos carregando", "Estado", "ordersState"),
  define("orders-empty", "orders", "empty", "Nenhum pedido", "Estado", "ordersState"),
  define("orders-error", "orders", "error", "Erro em pedidos", "Estado", "ordersState"),
  define("order-detail", "orders", "loaded", "Detalhes do pedido", "Pedidos", "orderDetail"),
  define("notifications", "dialogs", "default", "Notificações", "Pedidos", "notifications"),
  define("discount-dialog", "dialogs", "default", "Adicionar desconto", "Diálogo", "discountDialog"),
  define("leave-dialog", "dialogs", "warning", "Sair sem salvar", "Diálogo", "leaveDialog"),
]);

export const HAPPY_PATH = Object.freeze([
  "splash",
  "login-default",
  "store-select",
  "order-data",
  "payment-breakdown",
  "payment-method",
  "order-created",
  "payment-approved",
  "orders-loaded",
  "order-detail",
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
