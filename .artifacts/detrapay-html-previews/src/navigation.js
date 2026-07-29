import {
  JOURNEYS,
  getScreen,
  resolveAction,
} from "./catalog.js";

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
    return Object.freeze({
      ...state,
      view: "focused",
      screen: resolveAction(state.screen, event.name),
    });
  }
  if (event.type === "view") {
    return Object.freeze({
      ...state,
      view: event.value === "focused" ? "focused" : "gallery",
    });
  }
  if (event.type === "journey") {
    const journey = JOURNEYS[event.value] || JOURNEYS.payment;
    return Object.freeze({
      ...state,
      journey: journey.id,
      screen: journey.start,
    });
  }
  if (event.type === "screen") {
    getScreen(event.value);
    return Object.freeze({ ...state, screen: event.value });
  }
  if (event.type === "flow") {
    return Object.freeze({ ...state, flow: event.value || "all", view: "gallery" });
  }
  return state;
}
