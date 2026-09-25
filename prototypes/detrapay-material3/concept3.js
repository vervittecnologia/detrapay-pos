(() => {
const D=window.Detrapay;
let state=D.createModel();
state.screen='home';
const app=document.getElementById('app');
const esc=value=>String(value??'').replace(/[&<>"']/g,char=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
const paths={
  home:'M3 11l9-8 9 8v10h-6v-6H9v6H3V11z',
  orders:'M6 2h12v2h3v18H3V4h3V2zm2 2h8V3H8v1zm-3 3v13h14V7H5zm3 2h8v2H8V9zm0 4h8v2H8v-2zm0 4h5v2H8v-2z',
  person:'M12 12a4 4 0 100-8 4 4 0 000 8zm0 2c-4.42 0-8 1.79-8 4v2h16v-2c0-2.21-3.58-4-8-4z',
  add:'M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z',
  card:'M20 4H4a2 2 0 00-2 2v12a2 2 0 002 2h16a2 2 0 002-2V6a2 2 0 00-2-2zm0 14H4v-6h16v6zm0-10H4V6h16v2z',
  pix:'M12 2l5 5-3.5 3.5a2.12 2.12 0 01-3 0L7 7l5-5zm-6.4 6.4l3.5 3.5a4.1 4.1 0 005.8 0l3.5-3.5L22 12l-3.6 3.6-3.5-3.5a4.1 4.1 0 00-5.8 0l-3.5 3.5L2 12l3.6-3.6zM7 17l3.5-3.5a2.12 2.12 0 013 0L17 17l-5 5-5-5z',
  cash:'M2 5h20v14H2V5zm2 2v10h16V7H4zm8 1a4 4 0 100 8 4 4 0 000-8z',
  store:'M4 4h16l1 6v2h-1v8h-7v-6H7v6H4v-8H3v-2l1-6zm2 2l-.7 4h13.4L18 6H6z',
  search:'M9.5 3a6.5 6.5 0 104.1 11.55L19 20l1-1-5.45-5.4A6.5 6.5 0 009.5 3zm0 2a4.5 4.5 0 110 9 4.5 4.5 0 010-9z',
  camera:'M9 3L7.2 5H4a2 2 0 00-2 2v12a2 2 0 002 2h16a2 2 0 002-2V7a2 2 0 00-2-2h-3.2L15 3H9zm3 15a5 5 0 110-10 5 5 0 010 10z',
  next:'M9 6l-1.4 1.4L12.2 12l-4.6 4.6L9 18l6-6-6-6z',
  back:'M20 11H7.8l5.6-5.6L12 4l-8 8 8 8 1.4-1.4L7.8 13H20v-2z',
  check:'M9 16.2L4.8 12 3.4 13.4 9 19 21 7l-1.4-1.4L9 16.2z',
  receipt:'M5 2l2 2 2-2 3 2 3-2 2 2 2-2v20l-2-2-2 2-3-2-3 2-2-2-2 2V2zm3 6v2h8V8H8zm0 4v2h8v-2H8zm0 4v2h6v-2H8z',
  clock:'M12 2a10 10 0 100 20 10 10 0 000-20zm1 5v5.4l3.6 2.1-1 1.7-4.6-2.7V7h2z',
  backspace:'M22 3H7L0 12l7 9h15a2 2 0 002-2V5a2 2 0 00-2-2zm-3 12.6L17.6 17 14 13.4 10.4 17 9 15.6l3.6-3.6L9 8.4 10.4 7l3.6 3.6L17.6 7 19 8.4 15.4 12l3.6 3.6z'
};
const icon=(name,slot='')=>`<svg ${slot?`slot="${slot}"`:''} viewBox="0 0 24 24" aria-hidden="true"><path d="${paths[name]||paths.receipt}"/></svg>`;
const pages=[
  ['Balcão','home','Início','A ação vem primeiro.','Atalhos grandes e um único atendimento em destaque reduzem procura e rolagem.','FilledTonalButton, FilledButton, Navigation actions'],
  ['Balcão','orders','Pedidos','Lista compacta.','Busca e filtros preservam contexto sem transformar cada pedido em um painel.','OutlinedTextField, FilterChip, ListItem'],
  ['Balcão','detail','Conta do pedido','Formato de conta.','O pedido vira uma conta: itens, cliente, recebido e saldo na mesma leitura.','AssistChip, LinearProgress, ListItem'],
  ['Cobrança','methods','Receber','Meios em grade.','As opções de pagamento ficam visíveis de uma vez, como em terminais de checkout.','FilledTonalButton, FilledButton'],
  ['Cobrança','amount','Valor','Teclado imediato.','Valor e teclado ocupam a área central com confirmação fixa.','FilledTonalButton, TextButton'],
  ['Cobrança','review','Confirmar','Uma decisão final.','Cliente, método e total aparecem juntos antes de acionar o terminal.','AssistChip, FilledButton'],
  ['Cobrança','processing','No terminal','A tela vira orientação.','Durante a captura, desaparecem ações concorrentes e permanece apenas o que o cliente deve fazer.','CircularProgress, Surface'],
  ['Cobrança','success','Concluído','Encerre e siga.','Confirmação, saldo e comprovante ficam disponíveis sem sobrecarregar o operador.','FilledButton, OutlinedButton'],
  ['Atendimento','new-order','Novo pedido','Cadastro essencial.','O pedido começa com cliente e valor; dados adicionais podem ser completados depois.','OutlinedTextField, FilledButton'],
  ['Atendimento','profile','Terminal','Status operacional.','Operador, estabelecimento, conexão e hardware ficam em uma tela de suporte.','ListItem, AssistChip']
];
const page=id=>pages.find(item=>item[1]===id);
const button=(label,action,variant='filled',symbol='',attrs='')=>`<md-${variant}-button data-action="${action}" ${attrs}>${symbol?icon(symbol,'icon'):''}${label}</md-${variant}-button>`;
const iconButton=(name,action,label)=>`<md-icon-button data-action="${action}" aria-label="${label}">${icon(name)}</md-icon-button>`;
const order=()=>D.order(state);
const balance=()=>D.balance(state);
const percent=()=>order().base?Math.min(100,Math.round(order().paid/order().base*100)):0;
const header=(title,back=false,eyebrow='Auto Central')=>`<header class="c3-top">${back?iconButton('back','back','Voltar'):''}<div><span>${eyebrow}</span><h2>${title}</h2></div>${!back?`<md-assist-chip label="ONLINE" class="c3-online"></md-assist-chip>`:''}</header>`;
const nav=active=>`<nav class="c3-nav" aria-label="Navegação principal">${[['home','Início','home'],['orders','Pedidos','orders'],['profile','Terminal','person']].map(([id,label,symbol])=>`<md-icon-button data-action="${id}" class="${active===id?'active':''}" aria-label="${label}">${icon(symbol)}<span>${label}</span></md-icon-button>`).join('')}</nav>`;
const footer=(label,action,symbol='')=>`<footer class="c3-footer">${button(label,action,'filled',symbol)}</footer>`;
const moneyInput=()=>D.money(state.amount);
const paymentName=()=>D.method(state).name;
const actionTile=(title,subtitle,action,symbol,tone='')=>`<md-filled-tonal-button class="c3-action ${tone}" data-action="${action}"><span class="c3-action-content"><span class="c3-action-icon">${icon(symbol)}</span><strong>${title}</strong><small>${subtitle}</small></span></md-filled-tonal-button>`;
const compactOrder=o=>{const remaining=Math.max(0,o.base-o.paid);const paid=remaining===0;return `<md-list-item type="button" data-action="select-order" data-id="${o.id}" class="c3-order-row"><div slot="headline"><span class="c3-order-id">#${o.id} · ${o.date}</span><strong>${esc(o.name).toLocaleUpperCase('pt-BR')}</strong></div><div slot="supporting-text"><span>${paid?'Quitado':'Falta '+D.money(remaining)}</span><small>Total ${D.money(o.base)}</small></div><md-assist-chip slot="end" label="${paid?'PAGO':'ABERTO'}" class="${paid?'paid':''}"></md-assist-chip></md-list-item>`};
const views={
  home:()=>`${header('Balcão')}<div class="c3-scroll c3-home"><section class="c3-shift"><div><span>HOJE · 22 SET</span><strong>R$ 2.783,80</strong><small>2 pedidos em aberto</small></div><div class="c3-shift-ring"><b>4</b><span>atendimentos</span></div></section><h3 class="c3-section-title">Ações rápidas</h3><div class="c3-action-grid">${actionTile('Novo pedido','Iniciar atendimento','new-order','add','blue')}${actionTile('Receber','Pedido existente','orders','card','green')}${actionTile('Consultar','Buscar pedido','orders','search')}${actionTile('Documentos','Fotos do pedido','detail','camera','purple')}</div><h3 class="c3-section-title">Retomar atendimento</h3><section class="c3-active-ticket"><div class="c3-ticket-top"><span>EM ANDAMENTO</span><small>Pedido #${order().id}</small></div><strong>${esc(order().name).toLocaleUpperCase('pt-BR')}</strong><div class="c3-ticket-balance"><div><span>Falta receber</span><b>${D.money(balance())}</b></div><md-linear-progress value="${percent()/100}"></md-linear-progress><small>${percent()}% recebido</small></div>${button('Continuar','detail','filled','next','class="full-width"')}</section></div>${nav('home')}`,
  orders:()=>`${header('Pedidos',true)}<div class="c3-search"><md-outlined-text-field id="c3-search" label="Buscar pedido ou cliente" type="search"></md-outlined-text-field><md-chip-set>${[['open','Em aberto'],['paid','Quitados'],['all','Todos']].map(([id,label],i)=>`<md-filter-chip label="${label}" ${i===0?'selected':''}></md-filter-chip>`).join('')}</md-chip-set></div><div class="c3-scroll c3-order-list"><md-list>${state.orders.map(compactOrder).join('')}</md-list></div>${nav('orders')}`,
  detail:()=>`${header('Conta #'+order().id,true,'Pedido em andamento')}<div class="c3-scroll"><section class="c3-account"><div class="c3-account-client"><span>CLIENTE</span><strong>${esc(order().name).toLocaleUpperCase('pt-BR')}</strong><small>${esc(order().document)}</small></div><md-divider></md-divider><div class="c3-account-line"><span>Regularização veicular</span><strong>${D.money(order().base)}</strong></div><div class="c3-account-line subtle"><span>Recebido</span><strong>− ${D.money(order().paid)}</strong></div><md-divider></md-divider><div class="c3-account-total"><span>SALDO DA CONTA</span><strong>${D.money(balance())}</strong></div><md-linear-progress value="${percent()/100}"></md-linear-progress><small>${percent()}% do pedido recebido</small></section><section class="c3-detail-actions">${button('Dados','new-order','outlined','person')}${button('Fotos','detail','outlined','camera')}${button('Recibo','review','outlined','receipt')}</section><section class="c3-activity"><header><h3>Atividade</h3><span>Mais recente primeiro</span></header><div class="c3-timeline"><i></i><div><strong>Pagamento em crédito</strong><span>R$ 1.000,00 · 21 ago, 10:32</span></div><md-assist-chip label="PAGO" class="paid"></md-assist-chip></div><div class="c3-timeline muted"><i></i><div><strong>Pedido criado</strong><span>21 ago, 10:14 · Ana Oliveira</span></div></div></section></div>${footer('Receber '+D.money(balance()),'methods','card')}`,
  methods:()=>`${header('Como receber?',true,'Saldo '+D.money(balance()))}<div class="c3-scroll c3-method-screen"><div class="c3-big-value"><span>VALOR DISPONÍVEL</span><strong>${D.money(balance())}</strong></div><div class="c3-method-grid">${[['credit','Crédito','Parcelado','card'],['debit','Débito','À vista','card'],['pix','Pix','No terminal','pix'],['cash','Dinheiro','Registrar','cash'],['store','Crédito loja','Registrar','store']].map(([id,title,sub,symbol])=>`<md-filled-tonal-button data-action="choose-method" data-id="${id}"><span>${icon(symbol)}<strong>${title}</strong><small>${sub}</small></span></md-filled-tonal-button>`).join('')}</div></div>`,
  amount:()=>`${header('Digite o valor',true,paymentName())}<div class="c3-amount"><div class="c3-amount-value"><span>VALOR A RECEBER</span><strong id="c3-amount-value">${moneyInput()}</strong><small>Saldo ${D.money(balance())}</small></div><div class="c3-keypad">${['1','2','3','4','5','6','7','8','9','clear','0','back'].map(key=>button(key==='clear'?'Limpar':key==='back'?icon('backspace'):key,'key','filled-tonal','',`data-key="${key}" class="${['clear','back'].includes(key)?'utility':''}"`)).join('')}</div></div>${footer('Continuar','review','next')}`,
  review:()=>{const q=D.paymentQuote(state);return `${header('Confirmar cobrança',true)}<div class="c3-scroll"><section class="c3-review-card"><md-assist-chip label="${paymentName().toLocaleUpperCase('pt-BR')}" class="c3-method-chip"></md-assist-chip><span>TOTAL A COBRAR</span><strong>${D.money(q.total)}</strong><small>${state.method==='credit'?state.installments+'x de '+D.money(q.each):'Pagamento à vista'}</small></section><section class="c3-review-data"><div><span>Cliente</span><strong>${esc(order().name)}</strong></div><div><span>Pedido</span><strong>#${order().id}</strong></div><div><span>Valor original</span><strong>${D.money(q.base)}</strong></div><div><span>Taxas e juros</span><strong>${D.money(q.fee)}</strong></div></section><div class="c3-safe-note">${icon('check')}<span>Confira o valor com o cliente antes de acionar o terminal.</span></div></div>${footer('Cobrar '+D.money(q.total),'processing','card')}`},
  processing:()=>`${header('Pagamento',false,'Terminal conectado')}<div class="c3-process"><div class="c3-tap">${icon(state.method==='pix'?'pix':'card')}</div><h3>${state.method==='pix'?'Mostre o QR Code':'Aproxime ou insira o cartão'}</h3><p>Peça ao cliente para seguir as instruções do terminal.</p><strong>${D.money(D.paymentQuote(state).total)}</strong><md-circular-progress indeterminate></md-circular-progress><small>Não inicie outra cobrança.</small></div>`,
  success:()=>`${header('Concluído',false,'Pagamento confirmado')}<div class="c3-scroll"><section class="c3-success"><div>${icon('check')}</div><span>PAGAMENTO APROVADO</span><strong>${D.money((state.result||D.paymentQuote(state)).total)}</strong><small>${paymentName()} · Pedido #${order().id}</small></section><section class="c3-success-actions">${button('Imprimir recibo','receipt-demo','outlined','receipt')}${button('Enviar comprovante','receipt-demo','outlined','next')}</section><section class="c3-new-balance"><span>Saldo restante do pedido</span><strong>${D.money(balance())}</strong></section></div>${footer('Novo atendimento','home','add')}`,
  'new-order':()=>`${header('Novo pedido',true,'Cadastro rápido')}<div class="c3-scroll"><section class="c3-form-intro"><span>1 DE 2</span><h3>Quem será atendido?</h3><p>Cadastre o essencial agora. Veículo e documentação podem ser completados no pedido.</p></section><div class="c3-fields"><md-outlined-text-field label="CPF ou CNPJ" value="${esc(state.orderDraft.document)}"></md-outlined-text-field><md-outlined-text-field label="Nome do cliente" value="${esc(state.orderDraft.name)}"></md-outlined-text-field><md-outlined-text-field label="WhatsApp" value="${esc(state.orderDraft.phone)}"></md-outlined-text-field><md-outlined-text-field label="Valor do pedido" value="R$ 2.531,68"></md-outlined-text-field></div></div>${footer('Criar e continuar','detail','next')}`,
  profile:()=>`${header('Terminal',false)}<div class="c3-scroll"><section class="c3-terminal-card"><div class="c3-terminal-avatar">AO</div><div><strong>Ana Oliveira</strong><span>Auto Central</span></div><md-assist-chip label="ONLINE" class="c3-online"></md-assist-chip></section><md-list class="c3-settings"><md-list-item>${icon('store','start')}<span slot="headline">Estabelecimento</span><span slot="supporting-text">12.345.678/0001-90</span></md-list-item><md-list-item>${icon('card','start')}<span slot="headline">GPOS780</span><span slot="supporting-text">Terminal pronto para cobrar</span><md-assist-chip slot="end" label="CONECTADO" class="paid"></md-assist-chip></md-list-item><md-list-item>${icon('receipt','start')}<span slot="headline">Fechamento do dia</span><span slot="supporting-text">4 atendimentos · R$ 2.783,80</span></md-list-item></md-list></div>${nav('profile')}`
};
function render(){
  const item=page(state.screen)||page('home');
  app.dataset.screen='c3-'+state.screen;
  app.innerHTML=views[state.screen]();
  document.getElementById('screen-title').textContent=item[2];
  document.getElementById('note-title').textContent=item[3];
  document.getElementById('note-body').textContent=item[4];
  document.getElementById('component-list').textContent=item[5]+'. Layout calibrado para 424 × 681 dp.';
  const groups=[...new Set(pages.map(p=>p[0]))];
  document.getElementById('catalog').innerHTML=groups.map(group=>`<div class="catalog-group"><span>${group}</span>${pages.filter(p=>p[0]===group).map(p=>`<button data-page="${p[1]}" class="${state.screen===p[1]?'active':''}">${p[2]}</button>`).join('')}</div>`).join('');
  document.getElementById('demo-controls').innerHTML=state.screen==='processing'?'<div class="demo-box"><strong>Resultado da demonstração</strong><button data-demo="approve">Aprovar pagamento</button></div>':'';
}
function go(id){if(!views[id])return;state.screen=id;render()}
function handleAction(event){
  const target=event.composedPath().find(node=>node?.dataset?.action);if(!target)return;
  const id=target.dataset.action;
  if(id==='back'){go(['methods','amount','review'].includes(state.screen)?'detail':state.screen==='detail'?'home':'home');return}
  if(id==='select-order'){D.selectOrder(state,Number(target.dataset.id));go('detail');return}
  if(id==='choose-method'){D.setMethod(state,target.dataset.id);go('amount');return}
  if(id==='key'){D.key(state,target.dataset.key);const el=document.getElementById('c3-amount-value');if(el)el.textContent=D.money(state.amount);return}
  if(id==='processing'){if(!D.validAmount(state))state.amount=balance();if(!state.attempt)D.begin(state);go('processing');return}
  if(id==='receipt-demo'){go('success');return}
  go(id);
}
app.addEventListener('click',handleAction);
document.addEventListener('click',event=>{
  const catalog=event.target.closest('[data-page]');if(catalog){go(catalog.dataset.page);return}
  const demo=event.target.closest('[data-demo]');if(demo?.dataset.demo==='approve'){if(!state.attempt){state.amount=balance();D.begin(state)}if(!state.result)D.approve(state);go('success')}
});
document.getElementById('restart').addEventListener('click',()=>{state=D.createModel();state.screen='home';render()});
document.getElementById('terminal-status').innerHTML='● 86%';
const params=new URLSearchParams(location.search);
if(params.get('embed')==='1')document.documentElement.classList.add('embedded');
if(page(params.get('screen')))state.screen=params.get('screen');
if(state.screen==='processing'){state.amount=balance();D.begin(state)}
if(state.screen==='success'){state.amount=balance();D.begin(state);D.approve(state)}
globalThis.DetrapayConcept3={pages,views};
render();
})();
