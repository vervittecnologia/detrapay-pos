import test from "node:test";
import assert from "node:assert/strict";
import { SCREENS } from "../src/catalog.js";
import { renderGallery, renderScreen } from "../src/render.js";

test("every registered screen renders a single named device", () => {
  for (const screen of SCREENS) {
    const html = renderScreen(screen);
    assert.match(html, /class="device"/);
    assert.match(html, new RegExp(`data-screen-id="${screen.id}"`));
    assert.doesNotMatch(html, /undefined|null/);
  }
});

test("gallery contains each screen and its review label", () => {
  const html = renderGallery(SCREENS);
  assert.equal((html.match(/class="preview-card"/g) ?? []).length, SCREENS.length);
  for (const screen of SCREENS) {
    assert.match(html, new RegExp(screen.title));
  }
});

test("happy-path screens expose meaningful actions", () => {
  const actionable = [
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
  ];
  for (const id of actionable) {
    const screen = SCREENS.find((candidate) => candidate.id === id);
    assert.match(renderScreen(screen), /data-action="[^"]+"/);
  }
});
