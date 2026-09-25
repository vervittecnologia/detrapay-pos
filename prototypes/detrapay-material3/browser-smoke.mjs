import assert from 'node:assert/strict';
import fs from 'node:fs';
import { Window } from 'happy-dom';

const root = new URL('./', import.meta.url);
const html = fs.readFileSync(new URL('index.html', root), 'utf8')
  .replace(/<script[^>]*>[\s\S]*?<\/script>/gi, '')
  .replace(/<script[^>]*><\/script>/gi, '');
const window = new Window({ url: new URL('index.html', root).href });
window.document.write(html);
// happy-dom does not implement form-associated custom elements yet.
window.HTMLElement.prototype.attachInternals ??= function attachInternals() {
  return {
    form: null,
    labels: [],
    validity: {},
    validationMessage: '',
    willValidate: false,
    setFormValue() {},
    setValidity() {},
    checkValidity() { return true; },
    reportValidity() { return true; },
  };
};
const errors = [];
window.addEventListener('error', event => errors.push(event.error ?? event.message));
for (const file of ['model.js', 'app.js']) {
  window.eval(`${fs.readFileSync(new URL(file, root), 'utf8')}\n//# sourceURL=${file}`);
}
await window.happyDOM.waitUntilComplete();
await new Promise(resolve => window.setTimeout(resolve, 20));
assert.deepEqual(errors, [], `Erros no navegador: ${errors.join('; ')}`);
assert.match(window.document.getElementById('app').textContent, /Pedidos/);
assert.match(window.document.getElementById('app').textContent, /Marina Costa/);
assert.ok(window.document.getElementById('app').children.length > 2);
assert.match(fs.readFileSync(new URL('assets/material.bundle.js', root), 'utf8'), /md-filled-button/);
const standalone = fs.readFileSync(new URL('index.html', root), 'utf8');
assert.doesNotMatch(standalone, /<script\s+src=/i);
assert.doesNotMatch(standalone, /<link\s+rel="stylesheet"/i);
assert.match(standalone, /data:font\/woff2;base64,/);
console.log('OK: tela inicial renderizada; pacote Material 3 presente.');
await window.happyDOM.close();
