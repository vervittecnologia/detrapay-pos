# Todos os pedidos e taxas PagBank por conta da loja

## Objetivo

Exibir todos os pedidos na tela de Pedidos, inclusive os pagos, concluídos e cancelados, e garantir que qualquer taxa do PagBank seja absorvida pela loja. O cliente deve pagar exatamente o valor total exibido no checkout.

## Escopo

- Remover da listagem o filtro que mantém apenas pedidos com saldo pendente.
- Preservar a ordenação decrescente pelo identificador do pedido.
- Manter as regras atuais de ação: pedidos sem saldo ou em status final podem abrir os detalhes, mas não podem iniciar outro pagamento.
- Configurar compras parceladas no PagBank como parcelamento do vendedor.
- Usar uma única fonte para o total apresentado e cobrado: o valor original informado para o pagamento.
- Bloquear o pagamento antes de abrir o PagBank quando o backend preparar uma tentativa com acréscimo ao cliente.
- Ajustar a apresentação das parcelas do checkout para não exibir taxa ou total superior ao valor que será cobrado.
- Exibir a opção de exclusão, nos detalhes do pedido, somente para pagamentos cujo meio tenha `isOnlinePayment == false` e cujo status ainda admita exclusão.

O simulador isolado de crédito continuará sendo apenas informativo e não inicia nem registra pagamentos. A garantia de igualdade desta mudança se aplica ao checkout que efetivamente abre o PagBank.

## Listagem de pedidos

`OrdersViewModel` deve publicar todos os pedidos retornados por `OrderRepository.getOrders`, ordenados do maior identificador para o menor. Nenhum status será removido: `PENDING`, `PAID`, `AUTHORIZED`, `COMPLETED` e `CANCELLED` devem aparecer.

`OrderPresentation.shouldStartPayment` continuará protegendo os estados finais e os pedidos sem saldo. Assim, ampliar a listagem não amplia indevidamente as operações permitidas.

## Exclusão de pagamentos offline

A elegibilidade será centralizada em `OrderReceivableItem.canBeDeleted()` para que as telas, ViewModels e repositório apliquem a mesma regra. Na tela Compose ativa de detalhes, cada pagamento offline elegível exibirá “Excluir pagamento”; a ação abrirá uma confirmação antes de chamar o endpoint e atualizará o pedido selecionado com a resposta do backend. A tela legada continuará protegida pela mesma regra de domínio.

Um pagamento poderá ser excluído quando `paymentMethod.isOnlinePayment == false` e seu status não for `REFUNDED` nem `CANCELLED`. Isso mantém a possibilidade de corrigir pagamentos manuais já registrados, como dinheiro ou crédito da loja, e impede que pagamentos processados pelo PagBank sejam removidos localmente sem estorno na adquirente.

## Valor único de cobrança

O `OrderPaymentRequest.amount` será o valor total de referência do checkout. Esse mesmo valor deverá:

1. aparecer como valor do pagamento nas telas do fluxo;
2. ser enviado em `amount_original` ao criar o `payment-attempt`;
3. ser enviado, em centavos, ao `PlugPagPaymentData`;
4. ser usado no registro local da transação;
5. ser refletido pelo PagBank no comprovante como consequência do valor enviado ao terminal.

Para pagamentos parcelados, `PlugPagPaymentData` usará `INSTALLMENT_TYPE_PARC_VENDEDOR`. Pagamentos em uma parcela continuarão usando `INSTALLMENT_TYPE_A_VISTA`. O PagBank poderá descontar suas taxas do recebimento da loja, mas não acrescentará essas taxas ao total cobrado do cliente.

A tela de escolha de parcelas do checkout exibirá o total original para todas as opções e identificará que as taxas são por conta da loja. O valor indicativo de cada parcela será calculado a partir do mesmo total original. A invariável contratual é a igualdade entre o total exibido no checkout e o total enviado ao PagBank.

## Contrato do backend e proteção contra inconsistência

Hoje, `POST /orders/{orderId}/payment-attempts` devolve `amount_original` e `amount_final`, e o app envia `amount_final` ao terminal. Para taxas absorvidas pela loja, o contrato esperado é:

- `amount_original` igual ao valor solicitado pelo app;
- `amount_final` e `amount_original` iguais quando ambos são convertidos para centavos com arredondamento monetário;
- taxas da adquirente contabilizadas como custo da loja, sem compor o recebível devido pelo cliente.

O app não deve mascarar uma resposta incompatível. Se o backend devolver `amount_final` diferente do valor solicitado, o PagBank não será aberto e o usuário receberá uma mensagem objetiva informando que o pagamento não pôde ser preparado sem acréscimo ao cliente.

Demanda de backend: ajustar `POST /orders/{orderId}/payment-attempts` para que tentativas PagBank com taxa da loja não adicionem taxa a `amount_final`, e garantir que `POST /payment-attempts/{attemptId}/complete` registre o mesmo total efetivamente cobrado. O impacto atual é risco de a UI mostrar um valor e o terminal cobrar ou registrar outro.

## Tratamento de erros

- Divergência entre os valores em centavos do total solicitado e de `amount_final`: interromper antes de `doPayment`.
- Falha de preparação, configuração da maquininha ou autenticação PagBank: manter o comportamento atual, sem registrar pagamento.
- Falha de persistência após aprovação: manter a retentativa idempotente existente, sem realizar nova cobrança.

## Testes

- Teste de apresentação/listagem com todos os status, comprovando que nenhum pedido é filtrado e que a ordenação é decrescente.
- Teste do `OrdersViewModel` comprovando que ele publica toda a resposta do repositório.
- Teste do pagamento parcelado comprovando `INSTALLMENT_TYPE_PARC_VENDEDOR`.
- Teste comprovando que o valor exibido/de referência é o valor em centavos enviado ao `PlugPagPaymentData`, mesmo quando parcelado.
- Teste comprovando que uma tentativa preparada com acréscimo não chama `doPayment` nem registra pagamento.
- Teste da apresentação das parcelas comprovando que o total mostrado permanece igual ao valor original e sinaliza taxas por conta da loja.
- Testes da regra de exclusão comprovando que pagamentos offline pendentes ou pagos são permitidos e que pagamentos online, estornados e cancelados são rejeitados.
- Execução dos testes unitários relacionados e da suíte de testes unitários do app.
- Instalação no device, abertura automática do app e inspeção de `adb logcat` filtrado por `com.detrapay`, `AndroidRuntime` e `FATAL EXCEPTION`, conforme as instruções do workspace.

## Critérios de aceite

- A tela de Pedidos mostra todos os pedidos retornados pelo backend.
- Pedidos finais aparecem, mas não oferecem ação de pagamento indevida.
- Parcelamento PagBank é sempre do vendedor.
- O total mostrado no checkout é igual ao total enviado ao terminal.
- Nenhuma resposta do backend que acrescente taxa ao cliente consegue iniciar o PagBank.
- Os detalhes do pedido oferecem exclusão para pagamentos offline elegíveis e nunca para pagamentos online.
- Testes automatizados e validação no device não apresentam regressões ou falhas fatais.

Não haverá validação automatizada ou manual específica da impressão do comprovante. O comprovante é um efeito colateral do valor entregue ao SDK com `printReceipt = true`, não um requisito de teste desta mudança.
