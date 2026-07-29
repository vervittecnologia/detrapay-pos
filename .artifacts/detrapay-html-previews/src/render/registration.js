import {
  brl,
  detailLine,
  dialogShell,
  field,
  fullHeader,
  outlineButton,
  screenTitle,
  solidButton,
  statePanel,
  tabBar,
} from "./primitives.js";

const resume = () => `
  ${fullHeader()}${screenTitle("Resumo do pedido", "close", true)}
  <main class="fig-content">
    <section class="fig-breakdown">
      ${detailLine("IPVA 2025", brl(5162.53))}
      ${detailLine("Taxas do emplacamento", brl(518.60))}
      ${detailLine("Taxa de alienação", brl(90.45))}
      ${detailLine("Cartório", brl(40))}
      ${detailLine("Placa Mercosul", brl(370))}
      ${detailLine("Serviço do despachante", brl(300))}
      ${detailLine("Valor total", brl(6481.58), true)}
    </section>
    <button class="fig-text-action" type="button" data-action="discount">＋ Adicionar desconto</button>
  </main>
  <footer class="fig-footer stacked">
    ${solidButton("Criar pedido", "create")}
    ${outlineButton("Imprimir resumo", "create")}
  </footer>${tabBar("emplacamento")}`;

export const registrationRenderers = {
  orderData: () => `
    ${fullHeader()}${screenTitle("Novo pedido", "close")}
    <main class="fig-content fig-form-content">
      <h1 class="fig-form-heading">Dados do pedido</h1>
      ${field("CPF/CNPJ do cliente", "000.000.000-00", true)}
      ${field("Nome do cliente", "João Pedro da Silva")}
      ${field("Whatsapp", "(85) 99000-0865", true)}
      ${field("Data de faturamento", "05/02/2025", true)}
      ${field("Valor do veículo", "R$ 350.000,00", true)}
      ${field("Tipo de veículo", "Selecione o tipo de veículo", true, true)}
      <label class="fig-check"><i></i>Veículo com alienação</label>
      <label class="fig-check checked"><i>✓</i>Placa especial</label>
    </main>
    <footer class="fig-footer">${solidButton("Calcular valores", "simulate")}</footer>
    ${tabBar("emplacamento")}`,
  registrationLoading: () => `
    ${fullHeader()}${screenTitle("Novo pedido", "fail")}
    <main class="fig-content fig-state-page">
      ${statePanel("loading", "Calculando valores", "Consultando taxas e serviços do emplacamento.", "loaded", "Ver resumo")}
    </main>${tabBar("emplacamento")}`,
  registrationResume: resume,
  orderCreating: () => `
    ${fullHeader()}${screenTitle("Criando pedido")}
    <main class="fig-content fig-state-page">
      ${statePanel("loading", "Criando pedido", "Registrando os dados do atendimento.", "created", "Ver pedido criado")}
    </main>${tabBar("emplacamento")}`,
  discountDialog: () => dialogShell(
    "Adicionar desconto",
    `<label class="fig-select-dialog">Item do desconto<b>Serviço do despachante⌄</b></label>
     ${field("Valor do desconto", "R$ 100,00")}`,
    `${outlineButton("Cancelar", "cancel")}${solidButton("Adicionar", "apply")}`,
  ),
  leaveDialog: ({ id }) => dialogShell(
    "Deseja sair sem salvar?",
    "<p>As informações preenchidas serão perdidas.</p>",
    `${outlineButton("Cancelar", "cancel")}${solidButton("Sair", "leave", id === "leave-resume-dialog" ? "danger" : "")}`,
  ),
};
