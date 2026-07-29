import {
  HAPPY_PATH,
  getScreen,
  getScreensByFlow,
} from "./catalog.js";
import {
  nextHappyPathScreen,
  previousHappyPathScreen,
} from "./navigation.js";
import { renderGallery, renderScreen } from "./render.js";

const params = new URLSearchParams(location.search);
const state = {
  view: params.get("view") === "focused" ? "focused" : "gallery",
  flow: ["all", "access", "registration", "payment", "orders", "dialogs"].includes(params.get("flow"))
    ? params.get("flow")
    : "all",
  screen: params.get("screen") || HAPPY_PATH[0],
};

try {
  getScreen(state.screen);
} catch {
  state.screen = HAPPY_PATH[0];
}

const gallery = document.querySelector("#gallery");
const focused = document.querySelector("#focused-preview");
const flowFilter = document.querySelector("#flow-filter");

function writeUrl() {
  const nextParams = state.view === "gallery"
    ? new URLSearchParams({ view: "gallery", flow: state.flow })
    : new URLSearchParams({ view: "focused", screen: state.screen });
  history.replaceState(null, "", `?${nextParams}`);
}

function focusedMarkup() {
  const screen = getScreen(state.screen);
  const onPath = HAPPY_PATH.includes(screen.id);
  const previous = previousHappyPathScreen(screen.id);
  const next = nextHappyPathScreen(screen.id);
  return `
    <div class="focus-shell">
      <aside class="focus-meta">
        <span>${screen.eyebrow}</span><h2>${screen.title}</h2><p>Estado: ${screen.state}</p>
      </aside>
      ${renderScreen(screen)}
      <nav class="focus-nav" aria-label="Navegação da preview">
        <button type="button" data-nav="previous" ${!onPath || previous === screen.id ? "disabled" : ""}>← Anterior</button>
        <button type="button" data-nav="gallery">Todas as telas</button>
        <button type="button" data-nav="next" ${!onPath || next === screen.id ? "disabled" : ""}>Próxima →</button>
      </nav>
    </div>`;
}

function render() {
  const isGallery = state.view === "gallery";
  gallery.hidden = !isGallery;
  focused.hidden = isGallery;
  if (isGallery) gallery.innerHTML = renderGallery(getScreensByFlow(state.flow));
  else focused.innerHTML = focusedMarkup();
  flowFilter.value = state.flow;
  document.querySelectorAll("[data-view]").forEach((button) => {
    button.setAttribute("aria-pressed", String(button.dataset.view === state.view));
  });
  writeUrl();
}

document.addEventListener("click", (event) => {
  const viewButton = event.target.closest("[data-view]");
  if (viewButton) {
    state.view = viewButton.dataset.view;
    render();
    return;
  }

  const openCard = event.target.closest("[data-open-screen]");
  if (openCard && !event.target.closest("[data-action]")) {
    state.screen = openCard.dataset.openScreen;
    state.view = "focused";
    render();
    return;
  }

  const navigation = event.target.closest("[data-nav]")?.dataset.nav;
  if (navigation === "gallery") {
    state.view = "gallery";
  } else if (navigation === "next") {
    state.screen = nextHappyPathScreen(state.screen);
  } else if (navigation === "previous") {
    state.screen = previousHappyPathScreen(state.screen);
  }
  if (navigation) {
    render();
    return;
  }

  const action = event.target.closest("[data-action]");
  if (action) {
    state.view = "focused";
    state.screen = nextHappyPathScreen(state.screen);
    render();
  }
});

document.addEventListener("keydown", (event) => {
  const card = event.target.closest("[data-open-screen]");
  if (card && (event.key === "Enter" || event.key === " ")) {
    event.preventDefault();
    state.screen = card.dataset.openScreen;
    state.view = "focused";
    render();
  }
});

flowFilter.addEventListener("change", (event) => {
  state.flow = event.target.value;
  state.view = "gallery";
  render();
});

document.querySelector("#toggle-reference").addEventListener("click", (event) => {
  const strip = document.querySelector("#reference-strip");
  strip.hidden = !strip.hidden;
  event.currentTarget.textContent = strip.hidden ? "Ver referências" : "Ocultar referências";
});

render();
