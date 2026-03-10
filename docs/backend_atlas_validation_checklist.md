# Backend Atlas Validation Checklist

## Objetivo

Validar no ambiente real se o app Android esta consumindo corretamente as melhorias implementadas no backend Atlas, com foco em:

- lista de pedidos resumida
- detalhe de pedido enxuto
- mutacoes retornando pedido atualizado
- catalogos com contrato novo
- fluxo de cadastro e simulacao

## Pre-condicoes

- app compilado com as adaptacoes mais recentes do cliente Android
- usuario com empresa, despachante e vendedores configurados
- pelo menos 1 pedido pendente e 1 pedido pago no backend
- pelo menos 1 metodo de pagamento PIX e 1 de cartao ativos
- terminal SmartPOS autenticado para testes de pagamento fisico

## Fluxo 1. Login e bootstrap inicial

Passos:

1. abrir o app
2. fazer login com um usuario valido
3. entrar na home

Esperado:

- login conclui sem erro
- empresa e despachante aparecem na home
- lista inicial carrega sem travamento perceptivel
- logout continua limpando sessao, mas a tela de login mostra o ultimo CNPJ usado

## Fluxo 2. Lista de pedidos resumida

Passos:

1. abrir a tela de pedidos
2. observar o carregamento inicial
3. abrir alguns pedidos de status diferentes
4. voltar para a lista

Esperado:

- lista carrega normalmente usando o endpoint resumido
- cada card mostra pelo menos:
  - id do pedido
  - nome do cliente
  - CPF/CNPJ
  - vendedor
  - valor atual
  - status
  - data
- nenhum card depende de itens completos ou recebiveis detalhados para renderizar
- ao voltar do detalhe, a lista continua consistente

Sinais de falha:

- lista vazia com pedidos existentes no backend
- crash ao abrir card
- status ou datas vazias em massa

## Fluxo 3. Detalhe de pedido

Passos:

1. abrir um pedido da lista
2. validar cabecalho, cliente, itens e recebiveis
3. repetir com pedido pago e pendente

Esperado:

- detalhe abre sem depender de `populate=deep,3`
- nome do cliente, documento, veiculo, vendedor, itens e recebiveis aparecem corretamente
- valores de pagamento, bandeira, autorizacao e `pixTxIdCode` aparecem quando existirem

Sinais de falha:

- detalhe abre com campos zerados indevidamente
- itens ou recebiveis somem em pedidos que deveriam ter esses dados
- crash por campo nulo

## Fluxo 4. Troca de vendedor

Passos:

1. abrir detalhe de um pedido
2. trocar o vendedor
3. confirmar a operacao

Esperado:

- alteracao persiste
- tela volta com o pedido atualizado sem precisar de um segundo refresh manual
- ao sair e entrar novamente no detalhe, o vendedor permanece correto

Sinais de falha:

- sucesso visual sem persistencia real
- detalhe volta com vendedor antigo
- erro de parsing apos a alteracao

## Fluxo 5. Confirmacao de pagamento

Passos:

1. abrir um pedido com recebivel pendente
2. pagar com cartao
3. aguardar retorno do backend

Esperado:

- maquininha processa normalmente
- backend confirma o pagamento
- app atualiza o pedido a partir da propria resposta da mutacao
- status do recebivel muda para pago
- dados como autorizacao, bandeira, titular e ultimos 4 digitos aparecem se enviados pelo backend

Sinais de falha:

- pagamento aprovado na maquininha, mas pedido nao atualiza
- tela fecha com erro de parsing
- necessidade de reabrir manualmente para ver status atualizado

## Fluxo 6. Geracao de PIX

Passos:

1. abrir um pedido com recebivel PIX
2. iniciar o pagamento

Esperado:

- QR Code ou codigo copia e cola sao gerados
- `txId` aparece quando fornecido
- nenhum erro por identificacao do tipo de pagamento

Observacao:

- a geracao de PIX ainda depende do endpoint especifico de geracao, separado da confirmacao final

## Fluxo 7. Estorno

Passos:

1. abrir um pedido pago
2. executar estorno de um recebivel
3. confirmar a operacao

Esperado:

- backend retorna o pedido atualizado
- status do recebivel muda para cancelado ou estornado, conforme regra do backend
- detalhe reflete a mudanca imediatamente

Sinais de falha:

- retorno 200 sem atualizacao visual
- erro de parsing no retorno do estorno

## Fluxo 8. Cadastro de pedido

Passos:

1. iniciar novo cadastro
2. carregar tipos de veiculo
3. carregar vendedores
4. carregar metodos de pagamento
5. simular
6. concluir criacao do pedido

Esperado:

- tipos de veiculo carregam no formato enxuto
- vendedores carregam no formato enxuto
- metodos de pagamento carregam no formato enxuto
- simulacao continua funcionando
- pedido criado retorna dados suficientes para navegacao e resumo

Sinais de falha:

- dropdowns vazios
- erro de parse em catalogos
- simulacao retorna mas UI nao monta os dados

## Fluxo 9. Reabertura e consistencia

Passos:

1. criar ou alterar um pedido
2. fechar a tela
3. voltar para lista e detalhe

Esperado:

- cache local nao mascara resposta desatualizada
- lista e detalhe mostram o estado mais recente

## Validacao tecnica recomendada

Se houver proxy, log do backend ou Android Studio Network Inspector disponivel, confirmar:

- lista usa `GET /orders/summary`
- detalhe usa `GET /sales-orders/{id}`
- troca de vendedor nao depende de `GET` extra obrigatorio para atualizar a tela
- confirmacao de pagamento nao depende de `GET` extra obrigatorio para atualizar a tela
- estorno nao depende de `GET` extra obrigatorio para atualizar a tela
- `GET /vehicle-types`, `GET /payment-methods` e `GET /salespeople` retornam o contrato enxuto esperado

## Resultado esperado

O backend estara considerado alinhado quando:

- nenhum fluxo critico quebrar por diferenca de contrato
- lista e detalhe renderizarem com os DTOs novos
- mutacoes atualizarem a UI com a propria resposta
- catalogos novos funcionarem sem adaptacao manual adicional
