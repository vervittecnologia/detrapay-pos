import test from "node:test";
import assert from "node:assert/strict";
import {
  SCREENS,
  HAPPY_PATH,
  getScreen,
  getScreensByFlow,
} from "../src/catalog.js";
import {
  nextHappyPathScreen,
  previousHappyPathScreen,
} from "../src/navigation.js";

test("catalog contains every approved screen and state", () => {
  assert.deepEqual(
    SCREENS.map(({ id }) => id),
    [
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
    ],
  );
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

test("happy path navigation advances and stops at its boundaries", () => {
  assert.equal(nextHappyPathScreen("login-default"), "orders-loaded");
  assert.equal(nextHappyPathScreen("review-payment"), "payment-processing");
  assert.equal(nextHappyPathScreen("payment-approved"), "payment-approved");
  assert.equal(previousHappyPathScreen("login-default"), "login-default");
  assert.equal(previousHappyPathScreen("payment-approved"), "payment-processing");
});
