import test from "node:test";
import assert from "node:assert/strict";
import {
  createNavigationState,
  transition,
} from "../src/navigation.js";

test("payment actions follow the current checkout branch", () => {
  let state = createNavigationState({
    view: "focused",
    journey: "payment",
    screen: "orders-loaded",
  });

  for (const [action, expected] of [
    ["open-detail", "order-detail"],
    ["pay", "payment-method"],
    ["select-credit", "payment-amount"],
    ["continue-credit", "payment-fees-loading"],
    ["fees-loaded", "payment-installments"],
    ["review", "payment-review"],
    ["confirm", "payment-waiting"],
    ["approved", "payment-approved"],
    ["finish", "orders-loaded"],
  ]) {
    state = transition(state, { type: "action", name: action });
    assert.equal(state.screen, expected);
  }
});

test("registration and simulator return to orders", () => {
  let registration = createNavigationState({
    view: "focused",
    journey: "registration",
    screen: "orders-loaded",
  });
  for (const action of ["new-order", "simulate", "loaded", "create", "created", "finish"]) {
    registration = transition(registration, { type: "action", name: action });
  }
  assert.equal(registration.screen, "orders-loaded");

  let simulator = createNavigationState({
    view: "focused",
    journey: "simulator",
    screen: "orders-loaded",
  });
  for (const action of ["open-simulator", "consult", "loaded", "close"]) {
    simulator = transition(simulator, { type: "action", name: action });
  }
  assert.equal(simulator.screen, "orders-loaded");
});

test("view and journey changes preserve valid navigation state", () => {
  const initial = createNavigationState({ screen: "orders-loaded" });
  const focused = transition(initial, { type: "view", value: "focused" });
  assert.equal(focused.view, "focused");
  assert.equal(focused.screen, "orders-loaded");

  const simulator = transition(focused, { type: "journey", value: "simulator" });
  assert.equal(simulator.journey, "simulator");
  assert.equal(simulator.screen, "orders-loaded");

  const selected = transition(simulator, { type: "screen", value: "simulator-error" });
  assert.equal(selected.screen, "simulator-error");
});
