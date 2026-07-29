# Detrapay Current Flow Gallery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the prototype-linear HTML gallery with a Figma-styled gallery that navigates the two reachable Android journeys and the installment simulator.

**Architecture:** Keep the catalog as the source of truth, but replace the single happy-path array with named journeys and per-action destinations. Split renderers by journey so access, orders, registration, payment, and simulator can evolve independently while sharing the same Figma component primitives.

**Tech Stack:** Vanilla HTML, CSS, JavaScript ES modules, Node.js built-in test runner, Playwright with installed Chrome.

## Global Constraints

- Figma `4569:1291` defines appearance and components.
- The current Android code defines sequence, content, actions, and reachable states.
- Every preview is exactly `390 × 844`.
- Login goes directly to Orders; employee selection is removed.
- New order creates without requiring payment during registration.
- Existing-order checkout follows Method → Amount → conditional Installments → Review → Waiting/result.
- Simulator is an overlay launched from Orders and returns without mutating checkout state.
- No file under `app/` may be modified, built, installed, or executed.
- Do not invent missing backend contracts; use representative preview data only.

---

### Task 1: Replace the linear catalog with reachable journeys

**Files:**
- Modify: `.artifacts/detrapay-html-previews/src/catalog.js`
- Modify: `.artifacts/detrapay-html-previews/tests/catalog.test.mjs`

**Interfaces:**
- Produces: `SCREENS: readonly Screen[]`, `JOURNEYS: Readonly<Record<string, Journey>>`, `getScreen(id: string): Screen`, `getScreensByFlow(flow: string): Screen[]`, and `resolveAction(screenId: string, action: string): string`.
- `Screen` shape: `{ id, flow, state, title, eyebrow, renderer, actions }`.
- `Journey` shape: `{ id, label, start, steps }`.

- [ ] **Step 1: Write failing catalog tests**

```js
const expectedIds = [
  "splash", "login-default", "login-loading", "login-error",
  "orders-loading", "orders-loaded", "orders-empty", "orders-error",
  "order-data", "registration-loading", "registration-resume",
  "order-creating", "order-created-detail",
  "order-detail", "payment-method", "payment-amount",
  "payment-fees-loading", "payment-installments", "payment-review",
  "payment-waiting", "payment-approved", "payment-failed",
  "simulator-empty", "simulator-loading", "simulator-loaded",
  "simulator-empty-result", "simulator-error",
  "discount-dialog", "leave-dialog",
];

assert.deepEqual(SCREENS.map(({ id }) => id), expectedIds);
assert.equal(resolveAction("login-default", "submit"), "orders-loading");
assert.equal(resolveAction("orders-loaded", "new-order"), "order-data");
assert.equal(resolveAction("orders-loaded", "open-detail"), "order-detail");
assert.equal(resolveAction("orders-loaded", "open-simulator"), "simulator-empty");
assert.equal(resolveAction("registration-resume", "create"), "order-creating");
assert.equal(resolveAction("payment-method", "select-credit"), "payment-amount");
assert.equal(resolveAction("payment-amount", "continue-credit"), "payment-fees-loading");
assert.equal(resolveAction("payment-amount", "continue-direct"), "payment-review");
assert.equal(resolveAction("simulator-loaded", "close"), "orders-loaded");
assert.throws(() => resolveAction("orders-loaded", "missing"), /Unknown action/);
```

- [ ] **Step 2: Run the tests and verify the old linear model fails**

Run:

```powershell
node --test .artifacts/detrapay-html-previews/tests/catalog.test.mjs
```

Expected: FAIL because `JOURNEYS`, screen action maps, and the current-flow identifiers do not exist.

- [ ] **Step 3: Implement the new catalog model**

```js
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

export const JOURNEYS = Object.freeze({
  registration: Object.freeze({
    id: "registration",
    label: "Criar novo pedido",
    start: "orders-loaded",
    steps: Object.freeze([
      "orders-loaded", "order-data", "registration-loading",
      "registration-resume", "order-creating", "order-created-detail",
      "orders-loaded",
    ]),
  }),
  payment: Object.freeze({
    id: "payment",
    label: "Pagar pedido existente",
    start: "orders-loaded",
    steps: Object.freeze([
      "orders-loaded", "order-detail", "payment-method", "payment-amount",
      "payment-fees-loading", "payment-installments", "payment-review",
      "payment-waiting", "payment-approved", "orders-loaded",
    ]),
  }),
  simulator: Object.freeze({
    id: "simulator",
    label: "Simular parcelas",
    start: "orders-loaded",
    steps: Object.freeze([
      "orders-loaded", "simulator-empty", "simulator-loading",
      "simulator-loaded", "orders-loaded",
    ]),
  }),
});

export function resolveAction(screenId, action) {
  const target = getScreen(screenId).actions[action];
  if (!target) throw new Error(`Unknown action "${action}" for screen "${screenId}"`);
  getScreen(target);
  return target;
}
```

Define the catalog with these exact action maps:

```js
export const SCREENS = Object.freeze([
  define("splash", "access", "default", "Splash", "Acesso", "splash", {
    login: "login-default", authenticated: "orders-loading",
  }),
  define("login-default", "access", "default", "Login", "Acesso", "login", {
    submit: "orders-loading", fail: "login-error",
  }),
  define("login-loading", "access", "loading", "Entrando", "Acesso", "loginLoading", {
    success: "orders-loading", fail: "login-error",
  }),
  define("login-error", "access", "error", "Falha no login", "Acesso", "loginError", {
    retry: "login-default",
  }),
  define("orders-loading", "orders", "loading", "Pedidos carregando", "Pedidos", "ordersState", {
    loaded: "orders-loaded", empty: "orders-empty", fail: "orders-error",
  }),
  define("orders-loaded", "orders", "loaded", "Pedidos", "Pedidos", "orders", {
    "new-order": "order-data", "open-detail": "order-detail",
    "open-simulator": "simulator-empty",
  }),
  define("orders-empty", "orders", "empty", "Nenhum pedido", "Pedidos", "ordersState", {
    reload: "orders-loading", "new-order": "order-data",
  }),
  define("orders-error", "orders", "error", "Erro em pedidos", "Pedidos", "ordersState", {
    retry: "orders-loading",
  }),
  define("order-data", "registration", "default", "Dados do pedido", "Novo pedido", "orderData", {
    simulate: "registration-loading", close: "leave-dialog",
  }),
  define("registration-loading", "registration", "loading", "Calculando valores", "Novo pedido", "registrationLoading", {
    loaded: "registration-resume", fail: "order-data",
  }),
  define("registration-resume", "registration", "loaded", "Resumo do pedido", "Novo pedido", "registrationResume", {
    discount: "discount-dialog", create: "order-creating", close: "leave-dialog",
  }),
  define("order-creating", "registration", "loading", "Criando pedido", "Novo pedido", "orderCreating", {
    created: "order-created-detail", fail: "registration-resume",
  }),
  define("order-created-detail", "registration", "success", "Pedido criado", "Novo pedido", "createdDetail", {
    finish: "orders-loaded",
  }),
  define("order-detail", "payment", "loaded", "Detalhes do pedido", "Pagamento", "orderDetail", {
    pay: "payment-method", back: "orders-loaded",
  }),
  define("payment-method", "payment", "default", "Método de pagamento", "Pagamento", "paymentMethod", {
    "select-credit": "payment-amount", "select-direct": "payment-amount",
    back: "order-detail",
  }),
  define("payment-amount", "payment", "default", "Valor da cobrança", "Pagamento", "paymentAmount", {
    "continue-credit": "payment-fees-loading",
    "continue-direct": "payment-review", back: "payment-method",
  }),
  define("payment-fees-loading", "payment", "loading", "Consultando taxas", "Pagamento", "feesLoading", {
    "fees-loaded": "payment-installments", fail: "payment-amount",
  }),
  define("payment-installments", "payment", "loaded", "Parcelamento", "Pagamento", "installments", {
    review: "payment-review", back: "payment-amount",
  }),
  define("payment-review", "payment", "default", "Revisar pagamento", "Pagamento", "paymentReview", {
    confirm: "payment-waiting", back: "payment-installments",
  }),
  define("payment-waiting", "payment", "loading", "Aguardando pagamento", "Pagamento", "paymentWaiting", {
    approved: "payment-approved", fail: "payment-failed", back: "payment-review",
  }),
  define("payment-approved", "payment", "success", "Pagamento aprovado", "Pagamento", "paymentApproved", {
    finish: "orders-loaded",
  }),
  define("payment-failed", "payment", "error", "Falha no pagamento", "Pagamento", "paymentFailed", {
    retry: "payment-waiting", review: "payment-review",
  }),
  define("simulator-empty", "simulator", "default", "Simulador", "Simulador", "simulator", {
    consult: "simulator-loading", close: "orders-loaded",
  }),
  define("simulator-loading", "simulator", "loading", "Consultando parcelas", "Simulador", "simulator", {
    loaded: "simulator-loaded", empty: "simulator-empty-result",
    fail: "simulator-error", close: "orders-loaded",
  }),
  define("simulator-loaded", "simulator", "loaded", "Parcelas simuladas", "Simulador", "simulator", {
    copy: "simulator-loaded", share: "simulator-loaded", close: "orders-loaded",
  }),
  define("simulator-empty-result", "simulator", "empty", "Sem parcelas", "Simulador", "simulator", {
    retry: "simulator-empty", close: "orders-loaded",
  }),
  define("simulator-error", "simulator", "error", "Erro no simulador", "Simulador", "simulator", {
    retry: "simulator-empty", close: "orders-loaded",
  }),
  define("discount-dialog", "dialogs", "default", "Adicionar desconto", "Diálogo", "discountDialog", {
    cancel: "registration-resume", apply: "registration-resume",
  }),
  define("leave-dialog", "dialogs", "warning", "Sair sem salvar", "Diálogo", "leaveDialog", {
    cancel: "order-data", leave: "orders-loaded",
  }),
]);
```

Remove `store-select`, `payment-breakdown`, `payment-mismatch`, `payment-reversed`, `notifications`, and the linear `HAPPY_PATH`.

- [ ] **Step 4: Run catalog tests**

Run:

```powershell
node --test .artifacts/detrapay-html-previews/tests/catalog.test.mjs
```

Expected: PASS.

- [ ] **Step 5: Commit the catalog**

```powershell
git add .artifacts/detrapay-html-previews/src/catalog.js .artifacts/detrapay-html-previews/tests/catalog.test.mjs
git commit -m "test: model current Detrapay journeys"
```

---

### Task 2: Implement action-based gallery navigation

**Files:**
- Modify: `.artifacts/detrapay-html-previews/src/navigation.js`
- Modify: `.artifacts/detrapay-html-previews/src/app.js`
- Modify: `.artifacts/detrapay-html-previews/index.html`
- Modify: `.artifacts/detrapay-html-previews/tests/navigation.test.mjs`

**Interfaces:**
- Consumes: `JOURNEYS`, `getScreen`, and `resolveAction` from Task 1.
- Produces: `createNavigationState(initial)`, `transition(state, event)`, and URL state with `view`, `flow`, `journey`, and `screen`.

- [ ] **Step 1: Write failing navigation tests**

```js
const start = createNavigationState({
  view: "focused",
  flow: "all",
  journey: "payment",
  screen: "orders-loaded",
});

const detail = transition(start, { type: "action", name: "open-detail" });
assert.equal(detail.screen, "order-detail");

const method = transition(detail, { type: "action", name: "pay" });
assert.equal(method.screen, "payment-method");

const amount = transition(method, { type: "action", name: "select-credit" });
assert.equal(amount.screen, "payment-amount");

const gallery = transition(amount, { type: "view", value: "gallery" });
assert.equal(gallery.view, "gallery");
assert.equal(gallery.screen, "payment-amount");

const simulator = transition(start, { type: "action", name: "open-simulator" });
assert.equal(simulator.screen, "simulator-empty");
assert.equal(transition(simulator, { type: "action", name: "close" }).screen, "orders-loaded");
```

- [ ] **Step 2: Run navigation tests and verify failure**

Run:

```powershell
node --test .artifacts/detrapay-html-previews/tests/navigation.test.mjs
```

Expected: FAIL because the old helpers only move through `HAPPY_PATH`.

- [ ] **Step 3: Implement transitions**

```js
import { JOURNEYS, getScreen, resolveAction } from "./catalog.js";

export function createNavigationState(initial = {}) {
  const journey = JOURNEYS[initial.journey] ? initial.journey : "payment";
  const screen = initial.screen || JOURNEYS[journey].start;
  getScreen(screen);
  return Object.freeze({
    view: initial.view === "focused" ? "focused" : "gallery",
    flow: initial.flow || "all",
    journey,
    screen,
  });
}

export function transition(state, event) {
  if (event.type === "action") {
    return Object.freeze({ ...state, view: "focused", screen: resolveAction(state.screen, event.name) });
  }
  if (event.type === "view") {
    return Object.freeze({ ...state, view: event.value === "focused" ? "focused" : "gallery" });
  }
  if (event.type === "journey") {
    const journey = JOURNEYS[event.value] || JOURNEYS.payment;
    return Object.freeze({ ...state, journey: journey.id, screen: journey.start });
  }
  if (event.type === "screen") {
    getScreen(event.value);
    return Object.freeze({ ...state, screen: event.value });
  }
  return state;
}
```

Update `app.js` to dispatch events using `data-action`, not a global next step:

```js
document.addEventListener("click", (event) => {
  const action = event.target.closest("[data-action]")?.dataset.action;
  if (action) {
    state = transition(state, { type: "action", name: action });
    render();
    return;
  }

  const journey = event.target.closest("[data-journey]")?.dataset.journey;
  if (journey) {
    state = transition(state, { type: "journey", value: journey });
    state = transition(state, { type: "view", value: "focused" });
    render();
  }
});
```

Add a journey selector with `registration`, `payment`, and `simulator` buttons to focused mode. Update the flow filter options to `all`, `access`, `orders`, `registration`, `payment`, `simulator`, and `dialogs`.

- [ ] **Step 4: Run navigation and catalog tests**

Run:

```powershell
node --test .artifacts/detrapay-html-previews/tests/catalog.test.mjs .artifacts/detrapay-html-previews/tests/navigation.test.mjs
```

Expected: PASS.

- [ ] **Step 5: Commit navigation**

```powershell
git add .artifacts/detrapay-html-previews/src/navigation.js .artifacts/detrapay-html-previews/src/app.js .artifacts/detrapay-html-previews/index.html .artifacts/detrapay-html-previews/tests/navigation.test.mjs
git commit -m "feat: navigate real Detrapay journeys"
```

---

### Task 3: Split renderers and build access, orders, and registration

**Files:**
- Create: `.artifacts/detrapay-html-previews/src/render/primitives.js`
- Create: `.artifacts/detrapay-html-previews/src/render/access.js`
- Create: `.artifacts/detrapay-html-previews/src/render/orders.js`
- Create: `.artifacts/detrapay-html-previews/src/render/registration.js`
- Modify: `.artifacts/detrapay-html-previews/src/render.js`
- Modify: `.artifacts/detrapay-html-previews/styles.css`
- Modify: `.artifacts/detrapay-html-previews/tests/render.test.mjs`

**Interfaces:**
- Produces from `primitives.js`: `statusBar`, `fullHeader`, `screenTitle`, `field`, `solidButton`, `outlineButton`, `tabBar`, `statePanel`, `detailLine`, and `orderCard`.
- Produces from each journey module: `Record<string, (screen: Screen) => string>`.
- `render.js` merges renderer maps and exports `renderScreen(screen)` and `renderGallery(screens)`.

- [ ] **Step 1: Write failing renderer tests**

```js
for (const id of [
  "splash", "login-default", "login-loading", "login-error",
  "orders-loading", "orders-loaded", "orders-empty", "orders-error",
  "order-data", "registration-loading", "registration-resume",
  "order-creating", "order-created-detail",
]) {
  const html = renderScreen(getScreen(id));
  assert.match(html, new RegExp(`data-screen-id="${id}"`));
  assert.doesNotMatch(html, /undefined|null/);
}

assert.match(renderScreen(getScreen("login-default")), /data-action="submit"/);
assert.match(renderScreen(getScreen("orders-loaded")), /data-action="new-order"/);
assert.match(renderScreen(getScreen("orders-loaded")), /data-action="open-detail"/);
assert.match(renderScreen(getScreen("orders-loaded")), /data-action="open-simulator"/);
assert.match(renderScreen(getScreen("registration-resume")), /data-action="create"/);
assert.doesNotMatch(renderGallery(SCREENS), /Seleção de vendedor|store-select/);
```

- [ ] **Step 2: Run renderer tests and verify failure**

Run:

```powershell
node --test .artifacts/detrapay-html-previews/tests/render.test.mjs
```

Expected: FAIL for the new renderer keys and actions.

- [ ] **Step 3: Move shared Figma primitives**

Move the existing status bar, blue user header, yellow divider, underlined field, rectangular buttons, pale-blue order cards, detail rows, and dialogs into `render/primitives.js`. Keep their current class names so existing Figma-aligned CSS remains valid.

Export a button primitive that carries the real action:

```js
export const solidButton = (label, action, extraClass = "") =>
  `<button class="fig-button fig-button-primary ${extraClass}" type="button" data-action="${action}">${label}</button>`;
```

- [ ] **Step 4: Implement reachable access and order states**

`access.js` must render Splash and the three Login states. `orders.js` must render loading, loaded, empty, error, existing-order detail, and created-order detail. The loaded list exposes three distinct actions:

```html
<button data-action="new-order">Novo pedido</button>
<button data-action="open-simulator">Simular parcelas</button>
<button data-action="open-detail">Ver detalhes</button>
```

Do not render employee selection.

- [ ] **Step 5: Implement registration states**

`registration.js` must render:

- the current order data form;
- simulation loading without navigating prematurely;
- the itemized registration resume;
- order creation loading;
- created-order detail with `data-action="finish"`;
- discount and leave dialogs.

The resume primary action text is `Criar pedido`, not `Avançar para pagamento`.

- [ ] **Step 6: Run renderer tests**

Run:

```powershell
node --test .artifacts/detrapay-html-previews/tests/render.test.mjs
```

Expected: PASS for access, orders, and registration.

- [ ] **Step 7: Commit the first renderer group**

```powershell
git add .artifacts/detrapay-html-previews/src/render .artifacts/detrapay-html-previews/src/render.js .artifacts/detrapay-html-previews/styles.css .artifacts/detrapay-html-previews/tests/render.test.mjs
git commit -m "feat: render current access and registration flows"
```

---

### Task 4: Build payment and simulator branches

**Files:**
- Create: `.artifacts/detrapay-html-previews/src/render/payment.js`
- Create: `.artifacts/detrapay-html-previews/src/render/simulator.js`
- Modify: `.artifacts/detrapay-html-previews/src/render.js`
- Modify: `.artifacts/detrapay-html-previews/styles.css`
- Modify: `.artifacts/detrapay-html-previews/tests/render.test.mjs`
- Modify: `.artifacts/detrapay-html-previews/tests/navigation.test.mjs`

**Interfaces:**
- Consumes shared primitives from Task 3.
- Produces renderer keys `paymentMethod`, `paymentAmount`, `feesLoading`, `installments`, `paymentReview`, `paymentWaiting`, `paymentApproved`, `paymentFailed`, and simulator state renderers.

- [ ] **Step 1: Add failing payment branch tests**

```js
assert.match(renderScreen(getScreen("payment-method")), /data-action="select-credit"/);
assert.match(renderScreen(getScreen("payment-method")), /data-action="select-direct"/);
assert.match(renderScreen(getScreen("payment-amount")), /data-action="continue-credit"/);
assert.match(renderScreen(getScreen("payment-installments")), /data-action="review"/);
assert.match(renderScreen(getScreen("payment-review")), /data-action="confirm"/);
assert.match(renderScreen(getScreen("payment-failed")), /data-action="retry"/);
assert.match(renderScreen(getScreen("payment-approved")), /data-action="finish"/);

let payment = createNavigationState({ view: "focused", journey: "payment", screen: "orders-loaded" });
for (const action of [
  "open-detail", "pay", "select-credit", "continue-credit",
  "fees-loaded", "review", "confirm", "approved", "finish",
]) {
  payment = transition(payment, { type: "action", name: action });
}
assert.equal(payment.screen, "orders-loaded");
```

- [ ] **Step 2: Add failing simulator tests**

```js
for (const id of [
  "simulator-empty", "simulator-loading", "simulator-loaded",
  "simulator-empty-result", "simulator-error",
]) {
  assert.match(renderScreen(getScreen(id)), /fig-simulator/);
}

assert.match(renderScreen(getScreen("simulator-empty")), /data-action="consult"/);
assert.match(renderScreen(getScreen("simulator-loaded")), /data-action="copy"/);
assert.match(renderScreen(getScreen("simulator-loaded")), /data-action="share"/);
assert.match(renderScreen(getScreen("simulator-loaded")), /data-action="close"/);
```

- [ ] **Step 3: Run tests and verify failure**

Run:

```powershell
node --test .artifacts/detrapay-html-previews/tests/render.test.mjs .artifacts/detrapay-html-previews/tests/navigation.test.mjs
```

Expected: FAIL for missing payment and simulator renderers/actions.

- [ ] **Step 4: Implement payment screens**

Build the current sequence:

- Method: online and record-only sections.
- Amount: amount display, pending-balance shortcut, numeric keypad.
- Fees loading: same Amount layout with disabled keypad and progress.
- Installments: comparable rows with original amount, fee, total, and installment value.
- Review: method, original amount, fee, final amount, installments, and confirm action.
- Waiting: distinct Pix/card messaging with retry and copy code where applicable.
- Approved and failed: finish/retry actions returning through the catalog action map.

All screens reuse the blue Figma header, underlined values, pale-blue surfaces, and rectangular buttons.

- [ ] **Step 5: Implement simulator overlay states**

Render simulator screens with class `fig-simulator`, a close action, amount entry, and the four outcomes. The loaded state includes:

```html
<button type="button" data-action="copy">Copiar simulação</button>
<button type="button" data-action="share">Compartilhar</button>
<button type="button" data-action="close">Fechar</button>
```

Map `copy` and `share` to the same loaded screen so the preview remains visible after the representative side effect.

- [ ] **Step 6: Run model and renderer tests**

Run:

```powershell
node --test .artifacts/detrapay-html-previews/tests/catalog.test.mjs .artifacts/detrapay-html-previews/tests/navigation.test.mjs .artifacts/detrapay-html-previews/tests/render.test.mjs
```

Expected: PASS.

- [ ] **Step 7: Commit payment and simulator**

```powershell
git add .artifacts/detrapay-html-previews/src/render/payment.js .artifacts/detrapay-html-previews/src/render/simulator.js .artifacts/detrapay-html-previews/src/render.js .artifacts/detrapay-html-previews/styles.css .artifacts/detrapay-html-previews/tests
git commit -m "feat: add current payment and simulator previews"
```

---

### Task 5: Validate the complete browser gallery

**Files:**
- Modify: `.artifacts/detrapay-html-previews/tests/browser.test.mjs`
- Modify: `.artifacts/detrapay-html-previews/scripts/capture.mjs`
- Replace: `.artifacts/detrapay-html-previews/screenshots/*.png`

**Interfaces:**
- Consumes the complete catalog, action navigation, and journey renderers.
- Produces one screenshot per catalog screen plus `screenshots/overview.png`.

- [ ] **Step 1: Write failing browser journey tests**

```js
const journeys = {
  registration: ["new-order", "simulate", "loaded", "create", "created", "finish"],
  payment: [
    "open-detail", "pay", "select-credit", "continue-credit",
    "fees-loaded", "review", "confirm", "approved", "finish",
  ],
  simulator: ["open-simulator", "consult", "loaded", "close"],
};

for (const [journey, actions] of Object.entries(journeys)) {
  await page.goto(`http://127.0.0.1:4173/?view=focused&journey=${journey}&screen=orders-loaded`);
  for (const action of actions) {
    await page.locator(`.device [data-action="${action}"]`).first().click();
  }
  assert.equal(await page.locator(".device").getAttribute("data-screen-id"), "orders-loaded");
}
```

Keep the existing checks for catalog count and exact `390 × 844` dimensions. Query `.screen` and each primary content region to verify `scrollWidth <= clientWidth` and `scrollHeight <= clientHeight`.

- [ ] **Step 2: Run browser tests and verify failure**

Run:

```powershell
$env:CODEX_PLAYWRIGHT_PATH='file:///C:/Users/gerbs/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/index.mjs'
$env:CODEX_CHROME_PATH='C:\Program Files\Google\Chrome\Application\chrome.exe'
node --test .artifacts/detrapay-html-previews/tests/browser.test.mjs
```

Expected: FAIL until URL journey state and all action buttons are wired.

- [ ] **Step 3: Complete browser wiring and responsive fixes**

Fix only gallery HTML/CSS/JS issues exposed by the tests. Do not add scrolling inside a device merely to hide overflow; reduce spacing or make the intended content region scrollable only where the Android screen itself scrolls.

- [ ] **Step 4: Run all tests**

Run:

```powershell
$env:CODEX_PLAYWRIGHT_PATH='file:///C:/Users/gerbs/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/index.mjs'
$env:CODEX_CHROME_PATH='C:\Program Files\Google\Chrome\Application\chrome.exe'
node --test .artifacts/detrapay-html-previews/tests/catalog.test.mjs .artifacts/detrapay-html-previews/tests/navigation.test.mjs .artifacts/detrapay-html-previews/tests/render.test.mjs .artifacts/detrapay-html-previews/tests/browser.test.mjs
```

Expected: all tests PASS.

- [ ] **Step 5: Regenerate screenshots**

Run:

```powershell
$env:CODEX_PLAYWRIGHT_PATH='file:///C:/Users/gerbs/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/index.mjs'
$env:CODEX_CHROME_PATH='C:\Program Files\Google\Chrome\Application\chrome.exe'
node .artifacts/detrapay-html-previews/scripts/capture.mjs
```

Expected: one image for every `SCREENS` item plus `overview.png`.

- [ ] **Step 6: Perform visual and repository verification**

Inspect:

- `screenshots/overview.png`
- `screenshots/orders-loaded.png`
- `screenshots/registration-resume.png`
- `screenshots/payment-method.png`
- `screenshots/payment-amount.png`
- `screenshots/payment-installments.png`
- `screenshots/payment-review.png`
- `screenshots/payment-waiting.png`
- `screenshots/simulator-loaded.png`

Then run:

```powershell
git diff --check
git status --short
git diff --name-only HEAD -- app
```

Expected: no whitespace errors, only gallery/docs changes, and no output for `app`.

- [ ] **Step 7: Commit verified gallery**

```powershell
git add .artifacts/detrapay-html-previews
git commit -m "fix: match gallery to current Android flows"
```
