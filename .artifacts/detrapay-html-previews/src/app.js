import {
  JOURNEYS,
  getScreen,
  getScreensByFlow,
} from "./catalog.js";
import {
  createNavigationState,
  transition,
} from "./navigation.js";
import {
  renderGallery,
  renderScreen,
} from "./render.js";

const params = new URLSearchParams(location.search);
const allowedFlows = ["all", "access", "orders", "registration", "payment", "simulator", "dialogs"];
let state;

try {
  state = createNavigationState({
    view: params.get("view"),
    flow: allowedFlows.includes(params.get("flow")) ? params.get("flow") : "all",
    journey: params.get("journey"),
    screen: params.get("screen"),
  });
} catch {
  state = createNavigationState();
}

const gallery = document.querySelector("#gallery");
const focused = document.querySelector("#focused-preview");
const flowFilter = document.querySelector("#flow-filter");

function writeUrl() {
  const nextParams = state.view === "gallery"
    ? new URLSearchParams({ view: "gallery", flow: state.flow })
    : new URLSearchParams({
      view: "focused",
      journey: state.journey,
      screen: state.screen,
    });
  history.replaceState(null, "", `?${nextParams}`);
}

function focusedMarkup() {
  const screen = getScreen(state.screen);
  return `
    <div class="journey-switcher" aria-label="Escolher jornada">
      ${Object.values(JOURNEYS).map((journey) => `
        <button type="button" data-journey="${journey.id}" aria-pressed="${journey.id === state.journey}">
          ${journey.label}
        </button>`).join("")}
    </div>
    <div class="focus-shell">
      <aside class="focus-meta">
        <span>${screen.eyebrow}</span>
        <h2>${screen.title}</h2>
        <p>Estado: ${screen.state}</p>
      </aside>
      ${renderScreen(screen)}
      <nav class="focus-nav" aria-label="Navegação da preview">
        <button type="button" data-nav="gallery">Todas as telas</button>
        <button type="button" data-journey="${state.journey}">Reiniciar jornada</button>
      </nav>
    </div>`;
}

function render() {
  const isGallery = state.view === "gallery";
  gallery.hidden = !isGallery;
  focused.hidden = isGallery;
  if (isGallery) {
    gallery.innerHTML = renderGallery(getScreensByFlow(state.flow));
  } else {
    focused.innerHTML = focusedMarkup();
  }
  flowFilter.value = state.flow;
  document.querySelectorAll("[data-view]").forEach((button) => {
    button.setAttribute("aria-pressed", String(button.dataset.view === state.view));
  });
  writeUrl();
}

document.addEventListener("click", (event) => {
  const view = event.target.closest("[data-view]")?.dataset.view;
  if (view) {
    state = transition(state, { type: "view", value: view });
    render();
    return;
  }

  const openCard = event.target.closest("[data-open-screen]");
  if (openCard && !event.target.closest("[data-action]")) {
    state = transition(state, { type: "screen", value: openCard.dataset.openScreen });
    state = transition(state, { type: "view", value: "focused" });
    render();
    return;
  }

  const actionElement = event.target.closest("[data-action]");
  if (actionElement) {
    const deviceScreen = actionElement.closest("[data-screen-id]")?.dataset.screenId;
    if (deviceScreen && deviceScreen !== state.screen) {
      state = transition(state, { type: "screen", value: deviceScreen });
    }
    state = transition(state, { type: "action", name: actionElement.dataset.action });
    render();
    return;
  }

  const journey = event.target.closest("[data-journey]")?.dataset.journey;
  if (journey) {
    state = transition(state, { type: "journey", value: journey });
    state = transition(state, { type: "view", value: "focused" });
    render();
    return;
  }

  if (event.target.closest("[data-nav='gallery']")) {
    state = transition(state, { type: "view", value: "gallery" });
    render();
  }
});

document.addEventListener("keydown", (event) => {
  const card = event.target.closest("[data-open-screen]");
  if (card && (event.key === "Enter" || event.key === " ")) {
    event.preventDefault();
    state = transition(state, { type: "screen", value: card.dataset.openScreen });
    state = transition(state, { type: "view", value: "focused" });
    render();
  }
});

flowFilter.addEventListener("change", (event) => {
  state = transition(state, { type: "flow", value: event.target.value });
  render();
});

document.querySelector("#toggle-reference").addEventListener("click", (event) => {
  const strip = document.querySelector("#reference-strip");
  strip.hidden = !strip.hidden;
  event.currentTarget.textContent = strip.hidden ? "Ver referências" : "Ocultar referências";
});

render();
