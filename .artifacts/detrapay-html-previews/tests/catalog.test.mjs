import test from "node:test";
import assert from "node:assert/strict";
import {
  JOURNEYS,
  SCREENS,
  getScreen,
  getScreensByFlow,
  resolveAction,
} from "../src/catalog.js";

const expectedIds = [
  "splash",
  "login-default",
  "login-loading",
  "login-error",
  "orders-loading",
  "orders-loaded",
  "orders-empty",
  "orders-error",
  "order-data",
  "registration-loading",
  "registration-resume",
  "order-creating",
  "order-created-detail",
  "order-detail",
  "payment-method",
  "payment-amount",
  "payment-fees-loading",
  "payment-installments",
  "payment-review",
  "payment-waiting",
  "payment-approved",
  "payment-failed",
  "simulator-empty",
  "simulator-loading",
  "simulator-loaded",
  "simulator-empty-result",
  "simulator-error",
  "discount-dialog",
  "leave-data-dialog",
  "leave-resume-dialog",
];

test("catalog contains only reachable app screens and approved states", () => {
  assert.deepEqual(SCREENS.map(({ id }) => id), expectedIds);
  assert.equal(SCREENS.some(({ id }) => id === "store-select"), false);
});

test("every action target resolves to a registered screen", () => {
  for (const screen of SCREENS) {
    for (const [action, target] of Object.entries(screen.actions)) {
      assert.equal(resolveAction(screen.id, action), target);
      assert.equal(getScreen(target).id, target);
    }
  }
  assert.throws(
    () => resolveAction("orders-loaded", "missing"),
    /Unknown action "missing"/,
  );
});

test("access, registration, payment, and simulator follow current app routes", () => {
  assert.equal(resolveAction("splash", "login"), "login-default");
  assert.equal(resolveAction("login-default", "submit"), "login-loading");
  assert.equal(resolveAction("login-loading", "success"), "orders-loading");
  assert.equal(resolveAction("orders-loaded", "new-order"), "order-data");
  assert.equal(resolveAction("registration-resume", "create"), "order-creating");
  assert.equal(resolveAction("order-created-detail", "finish"), "orders-loaded");
  assert.equal(resolveAction("orders-loaded", "open-detail"), "order-detail");
  assert.equal(resolveAction("order-detail", "pay"), "payment-method");
  assert.equal(resolveAction("payment-method", "select-credit"), "payment-amount");
  assert.equal(resolveAction("payment-amount", "continue-credit"), "payment-fees-loading");
  assert.equal(resolveAction("payment-amount", "continue-direct"), "payment-review");
  assert.equal(resolveAction("simulator-loaded", "close"), "orders-loaded");
});

test("named journeys contain their full reachable happy paths", () => {
  assert.deepEqual(JOURNEYS.registration.steps, [
    "orders-loaded",
    "order-data",
    "registration-loading",
    "registration-resume",
    "order-creating",
    "order-created-detail",
    "orders-loaded",
  ]);
  assert.deepEqual(JOURNEYS.payment.steps, [
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
  ]);
  assert.deepEqual(JOURNEYS.simulator.steps, [
    "orders-loaded",
    "simulator-empty",
    "simulator-loading",
    "simulator-loaded",
    "orders-loaded",
  ]);
});

test("flow filtering preserves catalog order", () => {
  assert.deepEqual(
    getScreensByFlow("payment").map(({ id }) => id),
    [
      "order-detail",
      "payment-method",
      "payment-amount",
      "payment-fees-loading",
      "payment-installments",
      "payment-review",
      "payment-waiting",
      "payment-approved",
      "payment-failed",
    ],
  );
});
