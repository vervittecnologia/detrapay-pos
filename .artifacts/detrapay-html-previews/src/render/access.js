import {
  field,
  outlineButton,
  solidButton,
  statePanel,
} from "./primitives.js";

const loginFrame = (content) => `
  <div class="fig-login-hero">
    <div class="fig-logo-lockup"><span>✣</span><strong>Detrapay</strong></div>
    <div class="fig-document-art"><i>⌕</i><b>▤</b><em>▱</em></div>
  </div>
  <main class="fig-login-panel">${content}</main>`;

export const accessRenderers = {
  splash: () => `
    <main class="fig-splash">
      <div class="fig-logo-lockup"><span>✣</span><strong>Detrapay</strong></div>
      <small>Documentação veicular fácil e rápida</small>
      <div class="fig-splash-actions">
        ${solidButton("Entrar", "login")}
        ${outlineButton("Continuar sessão", "authenticated")}
      </div>
    </main>`,
  login: () => loginFrame(`
    <h1>Login</h1>
    ${field("CNPJ", "15.945.405/0001-70")}
    ${field("Senha", "**************")}
    ${solidButton("Fazer login", "submit")}`),
  loginLoading: () => loginFrame(`
    <h1>Login</h1>
    ${field("CNPJ", "15.945.405/0001-70")}
    ${field("Senha", "**************")}
    <div class="fig-inline-loading"><i>◌</i><span>Entrando...</span></div>
    ${solidButton("Entrando...", "success", "is-disabled")}`),
  loginError: () => loginFrame(`
    <h1>Login</h1>
    <div class="fig-login-error">CNPJ ou senha inválidos. Verifique os dados.</div>
    ${field("CNPJ", "15.945.405/0001-70")}
    ${field("Senha", "**************")}
    ${outlineButton("Tentar novamente", "retry")}
    <div class="visually-hidden">${statePanel("error", "Falha no login", "Verifique os dados.")}</div>`),
};
