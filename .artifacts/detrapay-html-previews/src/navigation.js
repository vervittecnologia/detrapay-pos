import { HAPPY_PATH } from "./catalog.js";

export function nextHappyPathScreen(id) {
  const index = HAPPY_PATH.indexOf(id);
  if (index < 0 || index === HAPPY_PATH.length - 1) return id;
  return HAPPY_PATH[index + 1];
}

export function previousHappyPathScreen(id) {
  const index = HAPPY_PATH.indexOf(id);
  if (index <= 0) return id;
  return HAPPY_PATH[index - 1];
}
