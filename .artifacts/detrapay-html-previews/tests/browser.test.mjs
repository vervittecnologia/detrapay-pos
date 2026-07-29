import test, { after, before } from "node:test";
import assert from "node:assert/strict";
import { SCREENS, HAPPY_PATH } from "../src/catalog.js";
import { startStaticServer } from "../scripts/static-server.mjs";

const playwrightPath = process.env.CODEX_PLAYWRIGHT_PATH;
if (!playwrightPath) throw new Error("CODEX_PLAYWRIGHT_PATH is required");
const chromePath = process.env.CODEX_CHROME_PATH;
if (!chromePath) throw new Error("CODEX_CHROME_PATH is required");
const { chromium } = await import(playwrightPath);

let server;
let browser;
let page;

before(async () => {
  server = await startStaticServer(new URL("../", import.meta.url), 4173);
  browser = await chromium.launch({ headless: true, executablePath: chromePath });
  page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
});

after(async () => {
  await browser?.close();
  await server?.close();
});

test("gallery renders every registered preview", async () => {
  await page.goto("http://127.0.0.1:4173/?view=gallery&flow=all");
  assert.equal(await page.locator(".device").count(), SCREENS.length);
});

test("every focused screen fits the 390 by 844 device without clipped content", async () => {
  for (const screen of SCREENS) {
    await page.goto(`http://127.0.0.1:4173/?view=focused&screen=${screen.id}`);
    const result = await page.locator(".device").evaluate((device) => {
      const content = device.querySelector(".fig-content, .fig-store, .fig-login-panel, .fig-approved, .fig-splash");
      const rect = device.getBoundingClientRect();
      return {
        width: rect.width,
        height: rect.height,
        clippedX: content ? content.scrollWidth > content.clientWidth : false,
        clippedY: content ? content.scrollHeight > content.clientHeight : false,
      };
    });
    assert.deepEqual(result, {
      width: 390,
      height: 844,
      clippedX: false,
      clippedY: false,
    }, screen.id);
  }
});

test("primary actions traverse the complete happy path", async () => {
  await page.goto(`http://127.0.0.1:4173/?view=focused&screen=${HAPPY_PATH[0]}`);
  for (const expected of HAPPY_PATH.slice(1)) {
    await page.locator(".device [data-action]").first().click();
    assert.equal(await page.locator(".device").getAttribute("data-screen-id"), expected);
  }
});
