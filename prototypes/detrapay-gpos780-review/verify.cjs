const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { Window } = require('../detrapay-material3/node_modules/happy-dom');

const root = __dirname;
const window = new Window({ url: 'http://localhost/prototype' });
window.document.write(fs.readFileSync(path.join(root, 'index.html'), 'utf8'));
window.eval(fs.readFileSync(path.join(root, 'app.js'), 'utf8'));
const doc = window.document;

function click(selector) {
  const element = doc.querySelector(selector);
  assert.ok(element, `Elemento ausente: ${selector}`);
  element.click();
}
function page(text) { assert.ok(doc.querySelector('#app').textContent.includes(text), `Texto ausente: ${text}`); }
function input(selector, value) {
  const element = doc.querySelector(selector);
  assert.ok(element, `Campo ausente: ${selector}`);
  element.value = value;
  element.dispatchEvent(new window.Event('input', { bubbles: true }));
}

page('Pedidos');
assert.equal(doc.querySelectorAll('[data-order]').length, 5);
assert.equal(doc.querySelectorAll('.order-metrics').length, 5);
assert.equal(doc.querySelectorAll('.order-top').length, 5);
assert.equal(doc.querySelectorAll('.order-payment').length, 5);
assert.equal(doc.querySelectorAll('.order-payment .payment-progress').length, 5);
assert.equal(doc.querySelectorAll('.order-payment span').length, 0);
assert.equal(doc.querySelector('[data-order="589"] .payment-progress').getAttribute('aria-valuenow'), '0');
assert.equal(doc.querySelector('[data-order="588"] .payment-progress').getAttribute('aria-valuenow'), '21');
assert.equal(doc.querySelector('[data-order="568"] .payment-progress').getAttribute('aria-valuenow'), '100');
assert.ok(!doc.querySelector('#order-list').textContent.includes('Pendente · falta'));
assert.match(fs.readFileSync(path.join(root, 'styles.css'), 'utf8'), /\.order-row\{[^}]*text-transform:uppercase/);
click('[data-action="toggle-search"]');
input('[data-field="search"]', 'Marina');
assert.equal(doc.querySelectorAll('[data-order]').length, 1);
click('[data-order="589"]');
page('FALTA');
page('Fotos anexadas ao pedido');
click('[data-action="view-photos"]');
click('[data-action="pay"]');
page('NA MAQUININHA');
assert.equal(doc.querySelector('[aria-label="Progresso do pagamento"]').getAttribute('aria-valuenow'), '1');
click('[data-method="credito"]');
assert.equal(doc.querySelector('[aria-label="Progresso do pagamento"]').getAttribute('aria-valuenow'), '2');
click('[data-action="fill-balance"]');
page('1.236,08');
click('[data-action="amount-next"]');
page('Escolha as parcelas');
assert.equal(doc.querySelector('[aria-label="Progresso do pagamento"]').getAttribute('aria-valuenow'), '3');
click('[data-installments="2"]');
click('[data-action="review"]');
page('Acréscimo simulado');
assert.equal(doc.querySelector('[aria-label="Progresso do pagamento"]').getAttribute('aria-valuenow'), '4');
click('[data-action="confirm"]');
page('Operação em andamento');
assert.equal(doc.querySelector('[aria-label="Aguardando resultado do terminal"]').getAttribute('aria-valuenow'), null);
click('[data-simulate="unknown"]');
page('Sem confirmação');
click('[data-action="detail"]');
page('1.236,08');
click('[data-action="pay"]');
click('[data-method="debito"]');
click('[data-action="fill-balance"]');
click('[data-action="amount-next"]');
click('[data-action="confirm"]');
click('[data-simulate="approved"]');
page('Pagamento aprovado');
click('[data-action="detail"]');
page('R$ 0,00');
click('[data-action="back"]');
assert.equal(doc.querySelector('[data-order="589"] .payment-progress').getAttribute('aria-valuenow'), '100');

click('#reset');
click('[data-action="new-order"]');
assert.equal(doc.querySelector('[aria-label="Progresso do cadastro"]').getAttribute('aria-valuenow'), '1');
input('[data-field="document"]', '111.222.333-44');
input('[data-field="name"]', 'Cliente de teste');
input('[data-field="vehicle"]', 'R$ 1.250,00');
click('[data-action="new-review"]');
page('Cliente de teste');
assert.equal(doc.querySelector('[aria-label="Progresso do cadastro"]').getAttribute('aria-valuenow'), '2');
click('[data-action="back"]');
assert.equal(doc.querySelector('[data-field="name"]').value, 'Cliente de teste');
click('[data-action="new-review"]');
click('[data-action="create-order"]');
page('Pedido #590');

assert.ok(fs.readFileSync(path.join(root, 'styles.css'), 'utf8').includes('423.53px'));
console.log('OK: busca, detalhe, cobrança simulada, resultado sem confirmação, aprovação e rascunho.');
window.close();
