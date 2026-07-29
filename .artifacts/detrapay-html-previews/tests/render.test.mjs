import test from "node:test";
import assert from "node:assert/strict";
import {
  SCREENS,
  getScreen,
} from "../src/catalog.js";
import {
  renderGallery,
  renderScreen,
} from "../src/render.js";

test("every current-flow screen renders one complete named device", () => {
  for (const screen of SCREENS) {
    const html = renderScreen(screen);
    assert.match(html, /class="device"/);
    assert.match(html, new RegExp(`data-screen-id="${screen.id}"`));
    assert.doesNotMatch(html, /undefined|null/);
  }
});

test("gallery contains each screen and removes unreachable employee selection", () => {
  const html = renderGallery(SCREENS);
  assert.equal((html.match(/class="preview-card"/g) ?? []).length, SCREENS.length);
  assert.doesNotMatch(html, /Seleção de vendedor|store-select/);
  for (const screen of SCREENS) assert.match(html, new RegExp(screen.title));
});

test("shared entry and order screens expose the real actions", () => {
  assert.match(renderScreen(getScreen("login-default")), /data-action="submit"/);
  const orders = renderScreen(getScreen("orders-loaded"));
  assert.match(orders, /data-action="new-order"/);
  assert.match(orders, /data-action="open-detail"/);
  assert.match(orders, /data-action="open-simulator"/);
});

test("new-order screens create without forcing registration payment", () => {
  assert.match(renderScreen(getScreen("order-data")), /data-action="simulate"/);
  const resume = renderScreen(getScreen("registration-resume"));
  assert.match(resume, /data-action="create"/);
  assert.match(resume, />Criar pedido</);
  assert.doesNotMatch(resume, /Avançar para pagamento/);
  const created = renderScreen(getScreen("order-created-detail"));
  assert.match(created, /data-action="finish"/);
  assert.match(created, /Nenhum pagamento registrado/);
  assert.doesNotMatch(created, /Pix <mark>Pago/);
});

test("existing-order checkout exposes the conditional credit branch", () => {
  assert.match(renderScreen(getScreen("order-detail")), /data-action="pay"/);
  assert.match(renderScreen(getScreen("payment-method")), /data-action="select-credit"/);
  assert.match(renderScreen(getScreen("payment-method")), /data-action="select-direct"/);
  assert.match(renderScreen(getScreen("payment-amount")), /data-action="continue-credit"/);
  assert.match(renderScreen(getScreen("payment-installments")), /data-action="review"/);
  const review = renderScreen(getScreen("payment-review"));
  assert.match(review, /data-action="confirm"/);
  assert.match(review, /data-action="confirm-record"/);
  assert.match(renderScreen(getScreen("payment-failed")), /data-action="retry"/);
  assert.match(renderScreen(getScreen("payment-approved")), /data-action="finish"/);
});

test("simulator states remain an overlay with close, copy, and share actions", () => {
  for (const id of [
    "simulator-empty",
    "simulator-loading",
    "simulator-loaded",
    "simulator-empty-result",
    "simulator-error",
  ]) {
    assert.match(renderScreen(getScreen(id)), /fig-simulator/);
  }
  const loaded = renderScreen(getScreen("simulator-loaded"));
  assert.match(loaded, /data-action="copy"/);
  assert.match(loaded, /data-action="share"/);
  assert.match(loaded, /data-action="close"/);
});
