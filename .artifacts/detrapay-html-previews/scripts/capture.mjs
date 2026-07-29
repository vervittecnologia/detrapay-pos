import { mkdir } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { SCREENS } from "../src/catalog.js";
import { startStaticServer } from "./static-server.mjs";

const playwrightPath = process.env.CODEX_PLAYWRIGHT_PATH;
const chromePath = process.env.CODEX_CHROME_PATH;
if (!playwrightPath || !chromePath) {
  throw new Error("CODEX_PLAYWRIGHT_PATH and CODEX_CHROME_PATH are required");
}
const { chromium } = await import(playwrightPath);

const outputUrl = new URL("../screenshots/", import.meta.url);
await mkdir(outputUrl, { recursive: true });
const server = await startStaticServer(new URL("../", import.meta.url), 4173);
const browser = await chromium.launch({ headless: true, executablePath: chromePath });
const page = await browser.newPage({ viewport: { width: 1500, height: 1000 }, deviceScaleFactor: 1 });

try {
  await page.goto("http://127.0.0.1:4173/?view=gallery&flow=all");
  await page.screenshot({
    path: fileURLToPath(new URL("overview.png", outputUrl)),
    fullPage: true,
  });

  for (const screen of SCREENS) {
    await page.goto(`http://127.0.0.1:4173/?view=focused&screen=${screen.id}`);
    await page.locator(".device").screenshot({
      path: fileURLToPath(new URL(`${screen.id}.png`, outputUrl)),
    });
  }
} finally {
  await browser.close();
  await server.close();
}

console.log(`Captured ${SCREENS.length + 1} review images.`);
