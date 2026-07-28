# Valor preparado pelo backend nos pagamentos

## Objetivo

Corrigir o fluxo de pagamento para que o valor final preparado pelo backend seja o valor efetivamente enviado ao PagBank em todos os meios online. O valor digitado pelo usuário representa o valor original da operação; ele não precisa ser igual ao valor final quando o meio de pagamento acrescenta taxas ou juros.

## Escopo

- Remover a validação no aplicativo que rejeita uma tentativa quando `amount_final` é diferente de `amount_original`.
- Usar `amount_final` retornado por `POST /orders/{orderId}/payment-attempts` em crédito, débito e PIX online.
- Usar o mesmo valor final na chamada ao SDK PagBank, no log local da transação e na confirmação do pagamento.
- Preservar o valor digitado como `amount_original` enviado na preparação.
- Manter dinheiro, crédito da loja e outros meios offline fora do fluxo PagBank, registrando o valor informado.
- Bloquear somente respostas com valor final inválido, sem criar no aplicativo uma regra alternativa de cálculo.

Não faz parte desta correção alterar as regras de simulação, taxas, parcelamento do vendedor, cálculo de saldo do pedido ou endpoints do backend.

## Causa do problema

O fluxo atual converte o valor digitado em uma invariável indevida: ele exige que `amount_original`, `amount_final` e o valor enviado ao PagBank sejam iguais. Quando o backend prepara legitimamente um total diferente, o aplicativo encerra o fluxo com a mensagem “O backend preparou um valor diferente do exibido”.

Além do bloqueio, o aplicativo ignora o `amount_final` retornado e usa `OrderPaymentRequest.amount` na chamada ao PagBank, no log e em `PaymentData.amountFinal`. Isso contradiz o contrato em que a preparação define o total final da transação.

## Fluxo de dados

### Pagamentos online

1. O usuário informa o valor do pagamento.
2. Para crédito, a aplicação apresenta a simulação e o usuário seleciona a quantidade de parcelas.
3. O aplicativo chama `POST /orders/{orderId}/payment-attempts` com o valor digitado em `amount_original`, o meio de pagamento e a quantidade de parcelas.
4. O backend devolve a tentativa com `amount_original` e `amount_final`.
5. O aplicativo valida que `amount_final` é finito e maior que zero.
6. O aplicativo converte `amount_final` para centavos usando arredondamento monetário e envia esse valor ao SDK PagBank.
7. Após aprovação, o aplicativo registra no log e envia na conclusão da tentativa o mesmo `amount_final` efetivamente cobrado.

Esse fluxo se aplica a todo meio com `isOnlinePayment == true`, incluindo crédito, débito e PIX. A escolha do tipo de operação e das parcelas continua sendo determinada pelo meio selecionado; somente a fonte do valor monetário muda.

### Pagamentos offline

Meios com `isOnlinePayment == false`, como dinheiro e crédito da loja, não criam tentativa PagBank. Eles continuam sendo registrados pelo fluxo manual com o valor informado pelo usuário.

## Responsabilidades

### Aplicativo

- Enviar corretamente `amount_original` e parcelas na preparação.
- Tratar `amount_final` da tentativa como fonte do total online.
- Garantir que PagBank, log local e confirmação usem exatamente o mesmo total.
- Rejeitar `amount_final` inválido antes de abrir o terminal.
- Não recalcular taxas nem substituir o valor preparado por uma regra local.

### Backend

- Calcular `amount_final` de acordo com o meio e as parcelas selecionadas.
- Retornar um valor final positivo e finito.
- Manter o identificador da tentativa idempotente.
- Registrar na conclusão o valor correspondente ao que foi preparado e cobrado.

## Tratamento de erros

- Falha de preparação: manter a mensagem devolvida pelo repositório e não abrir o PagBank.
- `amount_final` igual ou menor que zero, `NaN` ou infinito: informar que o backend retornou um valor final inválido e não abrir o PagBank.
- Falha ao configurar a maquininha ou ausência de autenticação: manter o comportamento atual.
- Transação recusada: salvar o log com o valor final preparado e não concluir a tentativa como aprovada.
- Falha de persistência após aprovação: manter a retentativa idempotente existente, sem realizar uma segunda cobrança.

## Testes

- Crédito parcelado: uma preparação com `amount_final` diferente de `amount_original` abre o PagBank e envia o total final em centavos.
- Débito: usa o `amount_final` preparado no PagBank, no log e na confirmação.
- PIX online: usa o `amount_final` preparado no PagBank, no log e na confirmação.
- Arredondamento: um valor final com mais de duas casas é convertido uma única vez para centavos e o valor persistido corresponde ao total cobrado.
- Valor final inválido: não configura a maquininha, não abre o PagBank e não registra aprovação.
- Pagamento offline: continua sem criar tentativa nem chamar o SDK PagBank.
- Regressão: falha de persistência após aprovação continua permitindo retentativa sem nova cobrança.

## Critérios de aceite

- A mensagem de divergência entre valor original e final deixa de bloquear pagamentos.
- Todo meio online cobra no PagBank o `amount_final` retornado pela preparação.
- O valor salvo no log e enviado na conclusão é o mesmo valor cobrado no terminal.
- Crédito preserva a quantidade de parcelas selecionada.
- Débito e PIX online seguem a mesma regra de valor do crédito.
- Meios offline continuam registrando o valor digitado sem acessar o PagBank.
- Valores finais inválidos são interrompidos antes de qualquer cobrança.
- Os testes unitários relacionados e a suíte do app passam.
- Após instalação no device, o app é aberto e os logs filtrados não apresentam `FATAL EXCEPTION`.
