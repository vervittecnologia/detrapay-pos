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
      "splash",
      "login-default",
      "store-select",
      "order-data",
      "payment-breakdown",
      "payment-method",
      "payment-mismatch",
      "order-created",
      "payment-approved",
      "payment-reversed",
      "orders-loaded",
      "orders-loading",
      "orders-empty",
      "orders-error",
      "order-detail",
      "notifications",
      "discount-dialog",
      "leave-dialog",
    ],
  );
});

test("happy path uses only registered screens in functional order", () => {
  assert.deepEqual(HAPPY_PATH, [
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
  for (const id of HAPPY_PATH) assert.equal(getScreen(id).id, id);
});

test("flow filtering returns stable catalog order", () => {
  assert.deepEqual(
    getScreensByFlow("orders").map(({ id }) => id),
    [
      "orders-loaded",
      "orders-loading",
      "orders-empty",
      "orders-error",
      "order-detail",
    ],
  );
});

test("happy path navigation advances and stops at its boundaries", () => {
  assert.equal(nextHappyPathScreen("splash"), "login-default");
  assert.equal(nextHappyPathScreen("payment-method"), "order-created");
  assert.equal(nextHappyPathScreen("order-detail"), "order-detail");
  assert.equal(previousHappyPathScreen("splash"), "splash");
  assert.equal(previousHappyPathScreen("payment-approved"), "order-created");
});
