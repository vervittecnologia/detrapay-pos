# Detrapay HTML Screen Previews Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a standalone, navigable HTML gallery that renders the current Detrapay flows with Figma-faithful components before any Android implementation.

**Architecture:** A dependency-light static site will keep screen data, reusable renderers, navigation state, and styling in separate files. Vanilla ES modules will render both a gallery and a focused `390 × 844` device preview from one screen catalog; Node tests will verify the catalog and Playwright will verify browser navigation, viewport integrity, and screenshot generation.

**Tech Stack:** HTML5, CSS custom properties, vanilla JavaScript ES modules, Node.js built-in test runner, Playwright from the Codex workspace runtime.

## Global Constraints

- Create the site only under `.artifacts/detrapay-html-previews/`.
- Do not modify Android production or test sources.
- Use Figma as the source of truth for visual components and the current Android app as the source of truth for flows and content.
- Render focused previews at exactly `390 × 844`.
- Do not perform network requests, authentication, payment operations, or persistent storage.
- Do not invent backend fields, states, or contracts.
- Reuse exact extracted Figma assets; do not redraw available brand or illustration assets.
- Include loaded, loading, empty, validation-error, service-error, processing, and success states where the app exposes them.

## File Structure

- `.artifacts/detrapay-html-previews/index.html` — single entry point and gallery shell.
- `.artifacts/detrapay-html-previews/styles.css` — tokens, reusable components, gallery, and screen layouts.
- `.artifacts/detrapay-html-previews/src/catalog.js` — screen definitions, grouping, states, and happy-path order.
- `.artifacts/detrapay-html-previews/src/navigation.js` — pure happy-path navigation rules.
- `.artifacts/detrapay-html-previews/src/render.js` — reusable component and screen renderers.
- `.artifacts/detrapay-html-previews/src/app.js` — URL state, filters, navigation, and click handling.
- `.artifacts/detrapay-html-previews/assets/` — exact copied Figma raster assets.
- `.artifacts/detrapay-html-previews/tests/catalog.test.mjs` — catalog contract tests.
- `.artifacts/detrapay-html-previews/tests/preview.spec.mjs` — browser behavior and layout tests.
- `.artifacts/detrapay-html-previews/scripts/capture.mjs` — deterministic screenshots and contact sheet capture.
- `.artifacts/detrapay-html-previews/playwright.config.mjs` — local static-server and browser configuration.
- `.artifacts/detrapay-html-previews/package.json` — local verification commands.
- `.artifacts/detrapay-html-previews/screenshots/` — generated focused previews and overview.

---

### Task 1: Define the Screen Catalog and Prototype Contract

**Files:**
- Create: `.artifacts/detrapay-html-previews/package.json`
- Create: `.artifacts/detrapay-html-previews/src/catalog.js`
- Create: `.artifacts/detrapay-html-previews/tests/catalog.test.mjs`

**Interfaces:**
- Produces: `SCREENS: readonly ScreenDefinition[]`
- Produces: `HAPPY_PATH: readonly string[]`
- Produces: `getScreen(id: string): ScreenDefinition`
- Produces: `getScreensByFlow(flow: string): ScreenDefinition[]`
- `ScreenDefinition` fields: `id`, `flow`, `state`, `title`, `eyebrow`, `renderer`

- [ ] **Step 1: Write the failing catalog contract test**

```js
// tests/catalog.test.mjs
import test from "node:test";
import assert from "node:assert/strict";
import {
  SCREENS,
  HAPPY_PATH,
  getScreen,
  getScreensByFlow,
} from "../src/catalog.js";

test("catalog contains every approved screen and state", () => {
  const ids = SCREENS.map(({ id }) => id);
  assert.deepEqual(ids, [
    "login-default",
    "orders-loaded",
    "orders-loading",
    "orders-empty",
    "orders-error",
    "order-detail",
    "amount-default",
    "amount-loading",
    "amount-error",
    "method-selection",
    "installments-selection",
    "review-payment",
    "payment-processing",
    "payment-approved",
    "simulator-loaded",
    "simulator-empty",
    "simulator-loading",
    "simulator-error",
  ]);
});

test("happy path uses only registered screens in functional order", () => {
  assert.deepEqual(HAPPY_PATH, [
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
  for (const id of HAPPY_PATH) assert.equal(getScreen(id).id, id);
});

test("flow filtering returns stable catalog order", () => {
  assert.deepEqual(
    getScreensByFlow("simulator").map(({ id }) => id),
    [
      "simulator-loaded",
      "simulator-empty",
      "simulator-loading",
      "simulator-error",
    ],
  );
});
```

- [ ] **Step 2: Run the test and verify it fails**

Run:

```powershell
& 'C:\Users\gerbs\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test .artifacts\detrapay-html-previews\tests\catalog.test.mjs
```

Expected: FAIL with `ERR_MODULE_NOT_FOUND` for `src/catalog.js`.

- [ ] **Step 3: Implement the catalog**

```js
// src/catalog.js
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
```

- [ ] **Step 4: Add the local package contract**

```json
{
  "name": "detrapay-html-previews",
  "private": true,
  "type": "module",
  "scripts": {
    "test:model": "node --test tests/catalog.test.mjs",
    "test:browser": "playwright test",
    "capture": "node scripts/capture.mjs"
  }
}
```

- [ ] **Step 5: Run the model test and commit**

Expected: 3 tests pass.

```powershell
git add .artifacts/detrapay-html-previews/package.json .artifacts/detrapay-html-previews/src/catalog.js .artifacts/detrapay-html-previews/tests/catalog.test.mjs
git commit -m "test: define Detrapay preview catalog"
```

### Task 2: Build the Figma-Faithful Visual System and Screen Renderers

**Files:**
- Create: `.artifacts/detrapay-html-previews/index.html`
- Create: `.artifacts/detrapay-html-previews/styles.css`
- Create: `.artifacts/detrapay-html-previews/src/render.js`
- Create: `.artifacts/detrapay-html-previews/assets/figma-login.png`
- Create: `.artifacts/detrapay-html-previews/assets/figma-home.png`
- Create: `.artifacts/detrapay-html-previews/assets/figma-pix-success.png`

**Interfaces:**
- Consumes: `SCREENS`, `getScreen()` from `src/catalog.js`
- Produces: `renderScreen(screen: ScreenDefinition): string`
- Produces: `renderGallery(screens: ScreenDefinition[]): string`
- Produces DOM hooks: `[data-screen-id]`, `[data-action]`, `#gallery`, `#focused-preview`

- [ ] **Step 1: Copy only the exact extracted Figma assets**

```powershell
New-Item -ItemType Directory -Force '.artifacts\detrapay-html-previews\assets'
Copy-Item -LiteralPath '.artifacts\app-preview-board-proposal-2\assets\figma-login.png' -Destination '.artifacts\detrapay-html-previews\assets\figma-login.png'
Copy-Item -LiteralPath '.artifacts\app-preview-board-proposal-2\assets\figma-home.png' -Destination '.artifacts\detrapay-html-previews\assets\figma-home.png'
Copy-Item -LiteralPath '.artifacts\app-preview-board-proposal-2\assets\figma-pix-success.png' -Destination '.artifacts\detrapay-html-previews\assets\figma-pix-success.png'
```

Expected: three non-empty PNG files exist in the new `assets` directory.

- [ ] **Step 2: Create the single-page shell**

```html
<!doctype html>
<html lang="pt-BR">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Detrapay — Previews</title>
    <link rel="stylesheet" href="./styles.css">
  </head>
  <body>
    <header class="review-header">
      <div>
        <p class="eyebrow">Design review</p>
        <h1>Detrapay — previews de telas</h1>
        <p>Visual do Figma, fluxos atuais do Android.</p>
      </div>
      <nav class="view-switcher" aria-label="Modo de visualização">
        <button data-view="gallery" aria-pressed="true">Todas</button>
        <button data-view="focused" aria-pressed="false">Navegável</button>
      </nav>
    </header>
    <main>
      <section class="review-controls" aria-label="Filtros">
        <label>Fluxo
          <select id="flow-filter">
            <option value="all">Todos</option>
            <option value="access">Acesso</option>
            <option value="orders">Pedidos</option>
            <option value="payment">Pagamento</option>
            <option value="simulator">Simulador</option>
          </select>
        </label>
      </section>
      <section id="gallery" class="gallery" aria-live="polite"></section>
      <section id="focused-preview" class="focused" hidden></section>
    </main>
    <script type="module" src="./src/app.js"></script>
  </body>
</html>
```

- [ ] **Step 3: Implement the visual tokens and reusable components**

Add these exact foundations to `styles.css`, then add focused rules for the
named classes emitted by `render.js`:

```css
:root {
  --blue-700: #0756b8;
  --blue-600: #1265bd;
  --blue-100: #e8f2fc;
  --canvas: #f6f5f8;
  --surface: #ffffff;
  --ink: #111827;
  --muted: #64748b;
  --line: #dbe4ef;
  --success: #2f9e44;
  --success-soft: #e7f6e8;
  --warning: #ed8b00;
  --warning-soft: #fff4d7;
  --danger: #c62828;
  --danger-soft: #fdecec;
  --radius-sm: 12px;
  --radius-md: 20px;
  --radius-lg: 30px;
  --shadow-card: 0 2px 8px rgb(15 23 42 / 8%);
  font-family: Inter, "Segoe UI", Arial, sans-serif;
}

* { box-sizing: border-box; }
body { margin: 0; color: var(--ink); background: var(--canvas); }
.device {
  width: 390px;
  height: 844px;
  overflow: hidden;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 36px;
  box-shadow: 0 20px 60px rgb(15 23 42 / 14%);
}
.screen { height: 100%; display: flex; flex-direction: column; }
.screen-content { flex: 1; overflow: hidden; padding: 24px 20px; }
.card { border-radius: var(--radius-md); background: var(--surface); box-shadow: var(--shadow-card); }
.button-primary {
  min-height: 52px;
  border: 0;
  border-radius: var(--radius-sm);
  color: white;
  background: var(--blue-600);
  font: inherit;
  font-weight: 700;
}
.status-success { color: var(--success); background: var(--success-soft); }
.status-warning { color: var(--warning); background: var(--warning-soft); }
.status-error { color: var(--danger); background: var(--danger-soft); }
```

- [ ] **Step 4: Implement semantic component and screen renderers**

`render.js` must define small reusable functions named `appBar`, `button`,
`orderCard`, `statusPanel`, `paymentOption`, `installmentRow`, and
`bottomNavigation`. Map every catalog `renderer` value to a renderer and reject
unknown values:

```js
const money = (value) =>
  new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(value);

const button = (label, action, kind = "primary") =>
  `<button class="button-${kind}" data-action="${action}">${label}</button>`;

const appBar = (title) =>
  `<header class="app-bar"><button aria-label="Voltar">←</button><strong>${title}</strong><span></span></header>`;

const orderCard = (name, document, value, status) => `
  <button class="card order-card" data-action="select-order">
    <span class="order-icon">🏍</span>
    <span><strong>${name}</strong><small>${document}</small></span>
    <span><em class="status-${status === "Finalizado" ? "success" : "warning"}">${status}</em><strong>${money(value)}</strong></span>
  </button>`;

const paymentOption = (icon, label, detail) => `
  <button class="card payment-option" data-action="select-credit">
    <span>${icon}</span><span><strong>${label}</strong><small>${detail}</small></span><b>›</b>
  </button>`;

const installmentRow = (count, total, selected = false) => `
  <button class="installment-row ${selected ? "is-selected" : ""}" data-action="continue-installments">
    <span><strong>${count}x de ${money(total / count)}</strong><small>Total ${money(total)}</small></span>
    <span aria-hidden="true">${selected ? "●" : "○"}</span>
  </button>`;

const statusPanel = (kind, title, detail) => `
  <section class="status-panel status-${kind}">
    <strong>${title}</strong>
    <span>${detail}</span>
  </section>`;

const bottomNavigation = () => `
  <nav class="bottom-nav"><b>⌂<small>Início</small></b><span>◆<small>Vendas</small></span><span>●<small>Perfil</small></span></nav>`;

const renderers = {
  login: () => `
    <div class="login-hero"><img src="./assets/figma-login.png" alt=""><strong>Detrapay</strong></div>
    <main class="screen-content login-panel"><h1>Login</h1><label>CPF<input value="072.990.879-00"></label>
    <label>Senha<input type="password" value="123456789"></label>${button("Fazer login", "login")}
    <button class="link-button">Esqueci minha senha</button></main>`,
  orders: () => `
    <header class="brand-bar"><strong>Detrapay</strong><button aria-label="Notificações">●</button></header>
    <main class="screen-content"><section class="card operator"><strong>ID Operador</strong><span>Detrapay Motors</span></section>
    <h2>Pedidos recentes</h2>${orderCard("Ricardo S. Almeida", "123.456.789-00", 18490, "Finalizado")}
    ${orderCard("Fernanda Lima", "987.654.321-00", 24100, "Pendente")}</main>${bottomNavigation()}`,
  ordersState: ({ state }) => `
    ${appBar("Pedidos")}<main class="screen-content state-page">${
      state === "loading" ? statusPanel("info", "Carregando pedidos", "Aguarde um instante.")
      : state === "empty" ? statusPanel("info", "Nenhum pedido encontrado", "Novos pedidos aparecerão aqui.")
      : statusPanel("error", "Falha ao carregar pedidos", "Verifique a conexão e tente novamente.")
    }</main>${bottomNavigation()}`,
  detail: () => `
    ${appBar("Detalhes do pedido")}<main class="screen-content"><p class="eyebrow">Pedido #1048</p>
    <h1>Ricardo S. Almeida</h1><section class="card detail-card"><dl><dt>Documento</dt><dd>123.456.789-00</dd>
    <dt>Valor do pedido</dt><dd>${money(18490)}</dd><dt>Vendedor</dt><dd>Marco Aurélio</dd></dl></section>
    ${button("Receber pagamento", "continue-detail")}</main>`,
  amount: () => `
    ${appBar("Valor do pagamento")}<main class="screen-content amount-page"><p>Informe quanto será recebido agora</p>
    <strong class="amount-value">${money(125)}</strong><div class="keypad"><button>1</button><button>2</button><button>3</button>
    <button>4</button><button>5</button><button>6</button><button>7</button><button>8</button><button>9</button>
    <button>00</button><button>0</button><button>⌫</button></div>${button("Continuar", "continue-amount")}</main>`,
  amountState: ({ state }) => `
    ${appBar("Valor do pagamento")}<main class="screen-content state-page">${
      state === "loading" ? statusPanel("info", "Consultando condições", "Buscando taxas e parcelas disponíveis.")
      : statusPanel("error", "Não foi possível consultar parcelas", "Tente novamente antes de continuar.")
    }</main>`,
  method: () => `
    ${appBar("Forma de pagamento")}<main class="screen-content"><h1>Como deseja receber?</h1>
    ${paymentOption("◆", "Pix", "Aprovação imediata")}${paymentOption("▣", "Crédito", "Parcelamento disponível")}
    ${paymentOption("▤", "Débito", "Pagamento à vista")}</main>`,
  installments: () => `
    ${appBar("Parcelamento")}<main class="screen-content"><h1>Escolha as parcelas</h1>
    ${installmentRow(1, 125)}${installmentRow(2, 131)}${installmentRow(3, 135, true)}
    ${installmentRow(4, 139)}${button("Continuar", "continue-installments")}</main>`,
  review: () => `
    ${appBar("Revisar pagamento")}<main class="screen-content"><h1>Confira os dados</h1>
    <section class="card summary-card"><dl><dt>Pedido</dt><dd>#1048</dd><dt>Valor solicitado</dt><dd>${money(125)}</dd>
    <dt>Taxas</dt><dd>${money(10)}</dd><dt>Total</dt><dd>${money(135)}</dd>
    <dt>Parcelamento</dt><dd>3x de ${money(45)}</dd></dl></section>${button("Confirmar pagamento", "confirm-payment")}</main>`,
  processing: () => `
    ${appBar("Pagamento")}<main class="screen-content state-page"><div class="spinner" aria-label="Processando"></div>
    <h1>Processando pagamento</h1><p>Não feche o aplicativo.</p>${button("Continuar", "finish")}</main>`,
  approved: () => `
    <header class="success-bar">Detalhe da transação</header><main class="screen-content result-page">
    <span class="success-mark">✓</span><h1>Pagamento realizado com sucesso!</h1><p>Valor do pagamento</p>
    <strong class="amount-value">${money(135)}</strong><p>Para<br><strong>Ricardo S. Almeida</strong></p>
    <button class="link-button">Ver comprovante</button>${button("Ir para o início", "finish")}</main>`,
  simulator: () => `
    ${appBar("Simular parcelamento")}<main class="screen-content"><label>Valor<input value="R$ 500,00"></label>
    <h2>Opções disponíveis</h2>${installmentRow(1, 500)}${installmentRow(2, 518)}
    ${installmentRow(3, 533, true)}${installmentRow(4, 548)}</main>`,
  simulatorState: ({ state }) => `
    ${appBar("Simular parcelamento")}<main class="screen-content state-page">${
      state === "loading" ? statusPanel("info", "Calculando parcelas", "Consultando as condições disponíveis.")
      : state === "empty" ? statusPanel("info", "Informe um valor", "As opções aparecerão após a simulação.")
      : statusPanel("error", "Não foi possível simular", "Revise o valor e tente novamente.")
    }</main>`,
};

export function renderScreen(screen) {
  const renderer = renderers[screen.renderer];
  if (!renderer) throw new Error(`Unknown renderer: ${screen.renderer}`);
  return `<div class="device" data-screen-id="${screen.id}">
    <article class="screen">${renderer(screen)}</article>
  </div>`;
}

export function renderGallery(screens) {
  return screens.map((screen) => `
    <article class="preview-card">
      <header><span>${screen.eyebrow}</span><h2>${screen.title}</h2></header>
      ${renderScreen(screen)}
    </article>`).join("");
}
```

Keep the current app copy (`Detrapay Motors`, order totals, payment methods,
installment values, loading and error messages) and retain the happy-path
actions `login`, `select-order`, `continue-detail`, `continue-amount`,
`select-credit`, `continue-installments`, `confirm-payment`, and `finish`.

- [ ] **Step 5: Perform a static render smoke check and commit**

Run:

```powershell
rg -n "\.\.\.|TODO|TBD" .artifacts\detrapay-html-previews
```

Expected: no matches.

Open `index.html` through a local server and confirm all 18 `.device` elements
are present after Task 3 wires the app. Commit the visual foundation:

```powershell
git add .artifacts/detrapay-html-previews/index.html .artifacts/detrapay-html-previews/styles.css .artifacts/detrapay-html-previews/src/render.js .artifacts/detrapay-html-previews/assets
git commit -m "feat: build Detrapay preview visual system"
```

### Task 3: Add Gallery Filters and Happy-Path Navigation

**Files:**
- Create: `.artifacts/detrapay-html-previews/src/navigation.js`
- Create: `.artifacts/detrapay-html-previews/src/app.js`
- Modify: `.artifacts/detrapay-html-previews/styles.css`
- Test: `.artifacts/detrapay-html-previews/tests/catalog.test.mjs`

**Interfaces:**
- Consumes: `SCREENS`, `HAPPY_PATH`, `getScreen()`, `getScreensByFlow()`
- Consumes: `renderScreen()`, `renderGallery()`
- Produces URL contract: `?view=gallery&flow=all` and `?view=focused&screen=<id>`
- Produces action-to-screen transitions in `ACTION_TARGETS`

- [ ] **Step 1: Extend the model test with navigation assertions**

```js
import { nextHappyPathScreen } from "../src/navigation.js";

test("happy path advances and stops at the approved result", () => {
  assert.equal(nextHappyPathScreen("login-default"), "orders-loaded");
  assert.equal(nextHappyPathScreen("review-payment"), "payment-processing");
  assert.equal(nextHappyPathScreen("payment-approved"), "payment-approved");
});
```

- [ ] **Step 2: Run the test and verify it fails**

Expected: FAIL because `src/navigation.js` does not exist.

- [ ] **Step 3: Implement deterministic URL and interaction state**

```js
// src/navigation.js
import { HAPPY_PATH } from "./catalog.js";

export function nextHappyPathScreen(id) {
  const index = HAPPY_PATH.indexOf(id);
  if (index < 0 || index === HAPPY_PATH.length - 1) return id;
  return HAPPY_PATH[index + 1];
}
```

Create `src/app.js` with DOM state kept separate from the pure navigation rule:

```js
import { HAPPY_PATH, getScreen, getScreensByFlow } from "./catalog.js";
import { nextHappyPathScreen } from "./navigation.js";
import { renderGallery, renderScreen } from "./render.js";

const state = {
  view: new URLSearchParams(location.search).get("view") || "gallery",
  flow: new URLSearchParams(location.search).get("flow") || "all",
  screen: new URLSearchParams(location.search).get("screen") || HAPPY_PATH[0],
};

function writeUrl() {
  const params = state.view === "gallery"
    ? new URLSearchParams({ view: state.view, flow: state.flow })
    : new URLSearchParams({ view: state.view, screen: state.screen });
  history.replaceState(null, "", `?${params}`);
}

function render() {
  document.querySelector("#gallery").hidden = state.view !== "gallery";
  document.querySelector("#focused-preview").hidden = state.view !== "focused";
  document.querySelector("#gallery").innerHTML =
    renderGallery(getScreensByFlow(state.flow));
  document.querySelector("#focused-preview").innerHTML =
    renderScreen(getScreen(state.screen));
  document.querySelector("#flow-filter").value = state.flow;
  document.querySelectorAll("[data-view]").forEach((button) => {
    button.setAttribute("aria-pressed", String(button.dataset.view === state.view));
  });
  writeUrl();
}

document.addEventListener("click", (event) => {
  const view = event.target.closest("[data-view]")?.dataset.view;
  if (view) state.view = view;

  const screen = event.target.closest("[data-screen-id]")?.dataset.screenId;
  if (event.target.closest(".preview-card") && screen) {
    state.view = "focused";
    state.screen = screen;
  }

  if (event.target.closest("[data-action]")) {
    state.view = "focused";
    state.screen = nextHappyPathScreen(state.screen);
  }
  render();
});

document.querySelector("#flow-filter").addEventListener("change", (event) => {
  state.flow = event.target.value;
  render();
});

render();
```

- [ ] **Step 4: Add focused navigation controls**

Add previous, next, and “Todas as telas” controls around the focused device.
Disable previous at `login-default` and next at `payment-approved`. Update the
CSS so the focused viewport remains exactly `390 × 844` while controls remain
outside it.

- [ ] **Step 5: Run the model test and commit**

Expected: all 4 model tests pass.

```powershell
git add .artifacts/detrapay-html-previews/src/navigation.js .artifacts/detrapay-html-previews/src/app.js .artifacts/detrapay-html-previews/styles.css .artifacts/detrapay-html-previews/tests/catalog.test.mjs
git commit -m "feat: add preview gallery navigation"
```

### Task 4: Verify Every Screen in a Real Browser and Generate Review Images

**Files:**
- Create: `.artifacts/detrapay-html-previews/playwright.config.mjs`
- Create: `.artifacts/detrapay-html-previews/tests/preview.spec.mjs`
- Create: `.artifacts/detrapay-html-previews/scripts/capture.mjs`
- Create: `.artifacts/detrapay-html-previews/screenshots/overview.png`
- Create: `.artifacts/detrapay-html-previews/screenshots/<screen-id>.png`

**Interfaces:**
- Consumes: URL contracts from `src/app.js`
- Produces: browser verification for all 18 registered screens
- Produces: one overview screenshot and one focused PNG per screen

- [ ] **Step 1: Add the Playwright configuration**

```js
// playwright.config.mjs
import { defineConfig } from "playwright/test";

export default defineConfig({
  testDir: "./tests",
  use: {
    baseURL: "http://127.0.0.1:4173",
    viewport: { width: 1440, height: 1000 },
  },
  webServer: {
    command: "python -m http.server 4173 --bind 127.0.0.1",
    cwd: ".",
    port: 4173,
    reuseExistingServer: true,
  },
});
```

- [ ] **Step 2: Write the failing browser contract**

```js
// tests/preview.spec.mjs
import { test, expect } from "playwright/test";
import { SCREENS, HAPPY_PATH } from "../src/catalog.js";

test("gallery renders every registered preview", async ({ page }) => {
  await page.goto("/?view=gallery&flow=all");
  await expect(page.locator(".device")).toHaveCount(SCREENS.length);
});

for (const screen of SCREENS) {
  test(`${screen.id} fits the Android viewport`, async ({ page }) => {
    await page.goto(`/?view=focused&screen=${screen.id}`);
    const device = page.locator(".device");
    await expect(device).toHaveCSS("width", "390px");
    await expect(device).toHaveCSS("height", "844px");
    const overflow = await device.evaluate((node) => ({
      x: node.scrollWidth > node.clientWidth,
      y: node.scrollHeight > node.clientHeight,
    }));
    expect(overflow).toEqual({ x: false, y: false });
  });
}

test("primary actions traverse the complete happy path", async ({ page }) => {
  await page.goto(`/?view=focused&screen=${HAPPY_PATH[0]}`);
  for (const expected of HAPPY_PATH.slice(1)) {
    await page.locator("[data-action]").first().click();
    await expect(page.locator(`[data-screen-id="${expected}"]`)).toBeVisible();
  }
});
```

- [ ] **Step 3: Run the browser test and fix only observed defects**

Run using the bundled module directory:

```powershell
$env:NODE_PATH='C:\Users\gerbs\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules'
& 'C:\Users\gerbs\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' 'C:\Users\gerbs\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules\playwright\cli.js' test --config .artifacts\detrapay-html-previews\playwright.config.mjs
```

Expected: all 20 browser checks pass. If a screen overflows, change only the
screen/component rule responsible, then rerun the full suite.

- [ ] **Step 4: Implement deterministic screenshot capture**

```js
// scripts/capture.mjs
import { chromium } from "playwright";
import { mkdir } from "node:fs/promises";
import { SCREENS } from "../src/catalog.js";

const output = new URL("../screenshots/", import.meta.url);
await mkdir(output, { recursive: true });
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
await page.goto("http://127.0.0.1:4173/?view=gallery&flow=all");
await page.screenshot({ path: new URL("overview.png", output), fullPage: true });

for (const screen of SCREENS) {
  await page.goto(`http://127.0.0.1:4173/?view=focused&screen=${screen.id}`);
  await page.locator(".device").screenshot({
    path: new URL(`${screen.id}.png`, output),
  });
}
await browser.close();
```

- [ ] **Step 5: Capture, inspect, and commit the complete review artifact**

Start a local server, run `scripts/capture.mjs`, and inspect
`screenshots/overview.png` plus every focused screenshot. Check typography,
alignment, spacing, clipping, icons, reused components, and state consistency.

Run:

```powershell
rg -n "\.\.\.|TODO|TBD|FIXME" .artifacts\detrapay-html-previews
git diff --check
```

Expected: no placeholder matches and no whitespace errors.

```powershell
git add .artifacts/detrapay-html-previews
git commit -m "test: verify Detrapay HTML previews"
```

### Task 5: Final Scope and Regression Verification

**Files:**
- Verify only: `.artifacts/detrapay-html-previews/`
- Verify unchanged: `app/src/main/`
- Verify unchanged: `app/src/test/`

**Interfaces:**
- Produces: final local preview URL and screenshot handoff

- [ ] **Step 1: Run all model and browser checks**

Expected: model tests and all browser tests pass.

- [ ] **Step 2: Verify Android sources were not changed by this feature**

```powershell
git diff --name-only HEAD~4..HEAD -- app/src/main app/src/test app/src/androidTest
```

Expected: no output.

- [ ] **Step 3: Verify all required screen IDs exist in the overview**

Compare `SCREENS` with the filenames in `screenshots/`. Expected: one
`<screen-id>.png` per catalog entry plus `overview.png`.

- [ ] **Step 4: Open the gallery for user review**

Serve `.artifacts/detrapay-html-previews/` on `127.0.0.1:4173` and open:

```text
http://127.0.0.1:4173/?view=gallery&flow=all
```

Keep the Android implementation unchanged until the user explicitly approves
the previews.
