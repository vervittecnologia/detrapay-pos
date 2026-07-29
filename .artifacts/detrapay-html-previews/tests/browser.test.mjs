import test, { after, before } from "node:test";
import assert from "node:assert/strict";
import { SCREENS } from "../src/catalog.js";
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

test("gallery renders every current-flow preview and all filters", async () => {
  await page.goto("http://127.0.0.1:4173/?view=gallery&flow=all");
  assert.equal(await page.locator(".device").count(), SCREENS.length);
  assert.deepEqual(
    await page.locator("#flow-filter option").evaluateAll((options) => options.map(({ value }) => value)),
    ["all", "access", "orders", "registration", "payment", "simulator", "dialogs"],
  );
});

test("every focused screen stays inside the 390 by 844 device", async () => {
  for (const screen of SCREENS) {
    await page.goto(`http://127.0.0.1:4173/?view=focused&screen=${screen.id}`);
    const result = await page.locator(".device").evaluate((device) => {
      const rect = device.getBoundingClientRect();
      return {
        width: rect.width,
        height: rect.height,
        clippedX: device.scrollWidth > device.clientWidth,
        clippedY: device.scrollHeight > device.clientHeight,
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

test("new-order journey creates without registration payment", async () => {
  await page.goto("http://127.0.0.1:4173/?view=focused&journey=registration&screen=orders-loaded");
  for (const [action, expected] of [
    ["new-order", "order-data"],
    ["simulate", "registration-loading"],
    ["loaded", "registration-resume"],
    ["create", "order-creating"],
    ["created", "order-created-detail"],
    ["finish", "orders-loaded"],
  ]) {
    await page.locator(`.device [data-action="${action}"]`).first().click();
    assert.equal(await page.locator(".device").getAttribute("data-screen-id"), expected);
  }
});

test("existing-order credit checkout follows every current step", async () => {
  await page.goto("http://127.0.0.1:4173/?view=focused&journey=payment&screen=orders-loaded");
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
    await page.locator(`.device [data-action="${action}"]`).first().click();
    assert.equal(await page.locator(".device").getAttribute("data-screen-id"), expected);
  }
});

test("simulator opens as an overlay and closes back to orders", async () => {
  await page.goto("http://127.0.0.1:4173/?view=focused&journey=simulator&screen=orders-loaded");
  for (const [action, expected] of [
    ["open-simulator", "simulator-empty"],
    ["consult", "simulator-loading"],
    ["loaded", "simulator-loaded"],
    ["close", "orders-loaded"],
  ]) {
    await page.locator(`.device [data-action="${action}"]`).first().click();
    assert.equal(await page.locator(".device").getAttribute("data-screen-id"), expected);
  }
});
