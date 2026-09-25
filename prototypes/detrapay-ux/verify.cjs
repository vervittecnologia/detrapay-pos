const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');

const root = __dirname;
const source = fs.readFileSync(path.join(root, 'app.js'), 'utf8');
const html = fs.readFileSync(path.join(root, 'index.html'), 'utf8');
const css = fs.readFileSync(path.join(root, 'styles.css'), 'utf8');
const nodes = new Map();
const stub = () => ({ innerHTML: '', textContent: '', addEventListener() {}, querySelector() { return null; } });
const document = {
  getElementById(id) { if (!nodes.has(id)) nodes.set(id, stub()); return nodes.get(id); },
  querySelector() { return null; },
};
const context = vm.createContext({ document, console, alert() {} });
vm.runInContext(`${source}\nglobalThis.prototypeForTest = { screens, views, state, navigate, quote, simQuote };`, context);
const prototype = context.prototypeForTest;

assert.match(html, /<html lang="pt-BR">/);
assert.match(html, /id="screen-menu"/);
assert.match(css, /@media\(max-width:760px\)/);
assert.equal(prototype.screens.length, Object.keys(prototype.views).length);
for (const screen of prototype.screens) {
  assert.equal(typeof prototype.views[screen.id], 'function', `${screen.id} sem tela`);
  const markup = prototype.views[screen.id]();
  assert.ok(markup.length > 250, `${screen.id} vazia`);
  assert.ok(!markup.includes('undefined'), `${screen.id} contém undefined`);
  prototype.navigate(screen.id);
  assert.equal(prototype.state.screen, screen.id);
}
prototype.state.method = 'Crédito';
prototype.state.amountCents = 153168;
assert.equal(prototype.quote(1), 153168);
assert.ok(prototype.quote(3) > 153168);
assert.equal(prototype.quote(3) % 3, 0, 'parcelas devem somar o total');
prototype.state.method = 'Pix';
assert.equal(prototype.quote(3), 153168);
assert.ok(prototype.simQuote(3) > 153168);
prototype.state.selectedOrder = 565;
assert.match(prototype.views.detail(), /Pedido #565/);
assert.match(prototype.views.detail(), /R\$ 1\.252,12/);
prototype.state.selectedOrder = 554;
assert.match(prototype.views.detail(), /PEDIDO QUITADO/);
assert.doesNotMatch(prototype.views.detail(), /Cobrar saldo/);
console.log(`OK: ${prototype.screens.length} telas, navegação e cálculos ilustrativos verificados.`);
