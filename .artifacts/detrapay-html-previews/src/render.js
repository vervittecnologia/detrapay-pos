import { accessRenderers } from "./render/access.js";
import { orderRenderers } from "./render/orders.js";
import { paymentRenderers } from "./render/payment.js";
import { registrationRenderers } from "./render/registration.js";
import { simulatorRenderers } from "./render/simulator.js";

const renderers = {
  ...accessRenderers,
  ...orderRenderers,
  ...registrationRenderers,
  ...paymentRenderers,
  ...simulatorRenderers,
};

export function renderScreen(screen) {
  const renderer = renderers[screen.renderer];
  if (!renderer) throw new Error(`Unknown renderer: ${screen.renderer}`);
  return `<div class="device" data-screen-id="${screen.id}"><article class="screen">${renderer(screen)}</article></div>`;
}

export function renderGallery(screens) {
  return screens.map((screen, index) => `
    <article class="preview-card" tabindex="0" data-open-screen="${screen.id}">
      <header>
        <span>${String(index + 1).padStart(2, "0")} · ${screen.eyebrow}</span>
        <h2>${screen.title}</h2>
        <mark>${screen.state}</mark>
      </header>
      ${renderScreen(screen)}
    </article>`).join("");
}
