import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const root = new URL("../", import.meta.url);
const css = await readFile(new URL("styles.css", root), "utf8");
const index = await readFile(new URL("index.html", root), "utf8");

test("gallery loads the exact Inter family used by Figma", () => {
  assert.match(index, /fonts\.googleapis\.com\/css2\?family=Inter:wght@400;500;600;700/);
  assert.match(css, /font-family:\s*Inter,\s*"Segoe UI"/);
});

test("primary controls use the Figma button scale", () => {
  assert.match(css, /\.fig-button\s*\{[^}]*min-height:\s*48px/s);
  assert.match(css, /\.fig-button\s*\{[^}]*font-size:\s*14px/s);
  assert.match(css, /\.fig-button\s*\{[^}]*font-weight:\s*600/s);
  assert.match(css, /\.fig-button\s*\{[^}]*border-radius:\s*4px/s);
});

test("screen typography and spacing follow the shared Figma rhythm", () => {
  assert.match(css, /\.fig-content\s*\{[^}]*padding:\s*24px 20px 20px/s);
  assert.match(css, /\.fig-field\s*\{[^}]*gap:\s*8px/s);
  assert.match(css, /\.fig-field\s*\{[^}]*font-size:\s*12px/s);
  assert.match(css, /\.fig-field-value\s*\{[^}]*font-size:\s*14px/s);
  assert.match(css, /\.fig-footer\s*\{[^}]*gap:\s*12px/s);
});
