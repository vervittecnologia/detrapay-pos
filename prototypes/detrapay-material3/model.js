(function (root) {
  const money = value => (value / 100).toLocaleString('pt-BR', {style:'currency', currency:'BRL'});
  const methods = [
    {id:'credit',name:'Crédito',description:'À vista ou parcelado',icon:'card',online:true},
    {id:'debit',name:'Débito',description:'Pagamento à vista',icon:'card',online:true},
    {id:'pix',name:'Pix',description:'Pagamento no terminal',icon:'pix',online:true},
    {id:'cash',name:'Dinheiro',description:'Registrar valor recebido',icon:'cash',online:false},
    {id:'store',name:'Crédito da loja',description:'Registrar crédito utilizado',icon:'store',online:false}
  ];
  function createModel() {
    return {screen:'orders',history:[],selected:568,filter:'pending',query:'',method:'credit',amount:153168,fresh:true,installments:3,simAmount:153168,simInstallments:3,result:null,attempt:null,
      orderDraft:{name:'Marina Costa',document:'000.000.000-00',phone:'(11) 99999-0000',vehicle:'Automóvel',value:'42.000,00',date:'22/09/2026',seller:'Ana Oliveira',financed:false,special:false,discount:0},
      login:{cnpj:'12.345.678/0001-90',password:'',error:false},photo:'idle',
      orders:[
        {id:568,name:'Marina Costa',document:'000.000.000-00',phone:'(11) 99999-0000',date:'21 ago',vehicle:'Automóvel',seller:'Ana Oliveira',base:253168,paid:100000,payments:[{method:'Crédito',base:100000,total:100000,installments:1,time:'21 ago · 10:32'}]},
        {id:565,name:'Distribuidora Cidade Ltda.',document:'00.000.000/0000-00',phone:'(11) 99999-0000',date:'17 ago',vehicle:'Automóvel',seller:'Rafael Pereira',base:125212,paid:0,payments:[]},
        {id:554,name:'Roberto Almeida',document:'000.000.000-00',phone:'(11) 99999-0000',date:'13 ago',vehicle:'Motocicleta',seller:'Ana Oliveira',base:257018,paid:257018,payments:[{method:'Pix',base:257018,total:257018,installments:1,time:'13 ago · 14:08'}]}
      ]};
  }
  const order = state => state.orders.find(item => item.id === state.selected) || state.orders[0];
  const balance = state => Math.max(0,order(state).base-order(state).paid);
  const method = state => methods.find(item=>item.id===state.method);
  function quote(base,n=1) {
    const rates={1:0,2:200,3:350,6:750};
    if(!Number.isInteger(base)||base<0||!Object.hasOwn(rates,n))throw new Error('Simulação inválida');
    const each=Math.round(base*(10000+rates[n])/10000/n);
    return {base,n,each,total:each*n,fee:each*n-base};
  }
  const paymentQuote = state => quote(state.amount,state.method==='credit'?state.installments:1);
  function selectOrder(state,id) {if(!state.orders.some(item=>item.id===id))throw new Error('Pedido não encontrado');state.selected=id;state.amount=balance(state);state.fresh=true;state.result=null;state.attempt=null;}
  function setMethod(state,id) {if(!methods.some(item=>item.id===id))throw new Error('Método inválido');state.method=id;state.installments=id==='credit'?3:1;state.amount=balance(state);state.fresh=true;state.result=null;}
  function key(state,digit) {if(digit==='back'){state.amount=state.fresh?0:Math.floor(state.amount/10)}else if(digit==='clear'){state.amount=0}else if(/^\d$/.test(digit)){state.amount=Math.min(99999999,(state.fresh?0:state.amount)*10+Number(digit))}state.fresh=false;}
  const validAmount = state => Number.isInteger(state.amount)&&state.amount>0&&state.amount<=balance(state);
  function begin(state) {if(!validAmount(state))throw new Error('Valor fora do saldo');state.attempt={orderId:state.selected,quote:paymentQuote(state),method:method(state).name,online:method(state).online,applied:false};state.result=null;}
  function approve(state) {const attempt=state.attempt;if(!attempt)throw new Error('Nenhuma operação iniciada');if(attempt.applied)return state.result;const target=state.orders.find(item=>item.id===attempt.orderId);if(target.base-target.paid<attempt.quote.base)throw new Error('Saldo mudou');target.paid+=attempt.quote.base;target.payments.push({method:attempt.method,base:attempt.quote.base,total:attempt.quote.total,installments:attempt.quote.n,time:'22 set · 09:41'});attempt.applied=true;state.result={...attempt.quote,method:attempt.method,orderId:target.id,online:attempt.online};return state.result;}
  function createOrder(state) {const draft=state.orderDraft;if(!draft.name.trim()||!draft.document.trim()||!draft.phone.trim())throw new Error('Preencha os dados do cliente');const id=Math.max(...state.orders.map(item=>item.id))+1;state.orders.unshift({id,name:draft.name,document:draft.document,phone:draft.phone,date:'22 set',vehicle:draft.vehicle,seller:draft.seller,base:253168-draft.discount,paid:0,payments:[]});selectOrder(state,id);return id;}
  const api={money,methods,createModel,order,balance,method,quote,paymentQuote,selectOrder,setMethod,key,validAmount,begin,approve,createOrder};
  if(typeof module!=='undefined'&&module.exports)module.exports=api;else root.Detrapay=api;
})(globalThis);
