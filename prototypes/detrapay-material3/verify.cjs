const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const D=require('./model.js');
let s=D.createModel();
D.setMethod(s,'credit');s.amount=50000;s.installments=3;
D.begin(s);D.approve(s);
assert.equal(D.balance(s),103168,'Aprovação parcial reduz apenas o valor base');
D.approve(s);assert.equal(D.balance(s),103168,'Mesmo resultado não duplica recebimento');
D.setMethod(s,'cash');D.begin(s);D.approve(s);
assert.equal(D.balance(s),0,'Dinheiro completa o saldo');assert.equal(s.result.online,false);
assert.equal(D.validAmount(s),false,'Pedido quitado não aceita outra cobrança');
s=D.createModel();s.amount=153169;assert.throws(()=>D.begin(s));
s.amount=0;assert.throws(()=>D.begin(s));
D.selectOrder(s,565);assert.equal(D.balance(s),125212);
for(const n of [1,2,3,6]){const q=D.quote(153168,n);assert.equal(q.each*q.n,q.total);assert.equal(q.total-q.base,q.fee)}
s.orderDraft.name='Cliente Exemplo';s.orderDraft.discount=10000;D.createOrder(s);
assert.equal(D.order(s).name,'Cliente Exemplo');assert.equal(D.balance(s),243168);
// Generate each preview without a browser. This does not claim visual validation.
const nodes=new Map();const stub=()=>({innerHTML:'',textContent:'',dataset:{},addEventListener(){},classList:{add(){}},querySelector(){return null}});
const document={getElementById(id){if(!nodes.has(id))nodes.set(id,stub());return nodes.get(id)},addEventListener(){},documentElement:stub()};
const ctx=vm.createContext({window:{Detrapay:D},document,location:{search:''},URLSearchParams,console});
vm.runInContext(fs.readFileSync(__dirname+'/app.js','utf8'),ctx);
for(const row of ctx.DetrapayPreview.pages){const markup=ctx.DetrapayPreview.views[row[1]]();assert.ok(markup.length>200,row[1]);assert.ok(!markup.includes('undefined'),row[1])}
const css=fs.readFileSync(__dirname+'/styles.css','utf8');assert.match(css,/--screen-width:423\.53px/);assert.match(css,/--screen-height:752\.94px/);assert.match(css,/#app\{height:680\.58px/);
assert.ok(fs.existsSync(__dirname+'/assets/material.bundle.js'));
console.log(`OK: ${ctx.DetrapayPreview.pages.length} telas geradas, fluxo financeiro local consistente e dimensões do device configuradas. Sem validação visual de navegador.`);
