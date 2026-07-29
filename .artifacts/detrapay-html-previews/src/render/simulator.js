import {
  brl,
  fullHeader,
  outlineButton,
  screenTitle,
  solidButton,
  statePanel,
  tabBar,
} from "./primitives.js";

const resultRows = `
  <div class="fig-simulator-results">
    <label><input type="radio" name="sim"><span><b>3x de ${brl(1763.33)}</b><small>Total ${brl(5290)}</small></span></label>
    <label class="selected"><input type="radio" checked name="sim"><span><b>6x de ${brl(915)}</b><small>Total ${brl(5490)}</small></span></label>
    <label><input type="radio" name="sim"><span><b>10x de ${brl(579)}</b><small>Total ${brl(5790)}</small></span></label>
  </div>`;

export const simulatorRenderers = {
  simulator: ({ state }) => `
    ${fullHeader("Meus pedidos")}
    <main class="fig-content muted-background"></main>${tabBar("pedidos")}
    <div class="fig-simulator-layer">
      <section class="fig-simulator">
        ${screenTitle("Simulador de parcelas", "close")}
        <div class="fig-simulator-body">${
          state === "default"
            ? `<label class="fig-simulator-value"><span>Valor da simulação</span><strong>${brl(5000)}</strong></label>
               <p>Consulte as condições sem alterar o pedido.</p>
               ${solidButton("Consultar parcelas", "consult")}`
            : state === "loading"
              ? statePanel("loading", "Consultando parcelas", "Buscando as taxas mais recentes.", "loaded", "Ver resultado")
              : state === "loaded"
                ? `<label class="fig-simulator-value"><span>Valor simulado</span><strong>${brl(5000)}</strong></label>
                   ${resultRows}
                   <div class="fig-simulator-actions">
                     ${outlineButton("Copiar", "copy")}
                     ${outlineButton("Compartilhar", "share")}
                     ${solidButton("Fechar", "close")}
                   </div>`
                : state === "empty"
                  ? statePanel("empty", "Nenhuma parcela disponível", "Tente outro valor.", "retry")
                  : statePanel("error", "Erro ao consultar parcelas", "Verifique a conexão e tente novamente.", "retry")
        }</div>
      </section>
    </div>`,
};
