# Wizard de pagamento com escolha do meio primeiro

## Objetivo

Reorganizar o pagamento de pedidos para que o usuário escolha primeiro o meio de pagamento, informe o valor em uma tela própria, selecione parcelas em uma tela separada quando usar crédito e revise o valor final antes de qualquer registro ou abertura do PagBank.

Esta especificação substitui, para o fluxo novo, a decisão registrada em `2026-07-28-backend-prepared-payment-value-design.md` de usar o `amount_final` da preparação como valor do terminal. O requisito atual torna o total confirmado na interface a fonte do valor enviado ao PagBank e exige que o backend produza o mesmo total por construção.

## Escopo

- Inverter a ordem atual de valor e meio de pagamento.
- Criar uma tela própria de valor após a escolha do meio.
- Manter parcelamento de crédito em uma tela própria do wizard.
- Criar uma tela de revisão para todos os meios de pagamento.
- Mostrar na revisão o valor informado, a taxa, as parcelas quando aplicáveis e o total final.
- Enviar ao PagBank exatamente o total final exibido e confirmado pelo usuário.
- Não substituir nem bloquear esse valor quando a preparação do backend devolver outro total.
- Revisar e corrigir o cálculo do backend para que cotação, tentativa e registro usem a mesma regra.
- Validar o fluxo completo de todos os meios disponíveis no device.

Não faz parte do escopo adicionar novos meios de pagamento, alterar a identidade visual geral da aplicação ou criar um indicador numérico de etapas.

## Fluxo do wizard

### Etapas comuns

1. O usuário toca em `Pagar` em um pedido.
2. A aplicação carrega os meios disponíveis e abre `Forma de pagamento`.
3. O usuário seleciona um meio.
4. A aplicação abre `Valor do pagamento`, iniciando em zero e oferecendo a ação `Usar valor pendente`.
5. O usuário informa um valor positivo e toca em `Continuar`.
6. A aplicação obtém a cotação aplicável.
7. A aplicação abre `Revisar pagamento` com um snapshot imutável da escolha.
8. Somente após `Confirmar` a aplicação registra o pagamento manual ou prepara e abre o PagBank.

Cada tela terá título e botão de voltar, sem texto como `Etapa 2 de 4`.

### Crédito

Após o valor, a aplicação consulta as parcelas no backend e abre a tela `Parcelamento`. O usuário seleciona uma opção e avança para a revisão. A revisão apresenta:

- meio de pagamento;
- valor informado;
- quantidade e valor das parcelas;
- juros/taxa;
- total final confirmado.

Voltar e alterar o valor invalida a cotação e a parcela anteriores. Voltar da revisão para o parcelamento mantém a cotação enquanto o valor e o meio não forem alterados.

### Débito, Pix e transferência Pix

Após o valor, a aplicação consulta a cotação de uma parcela e abre diretamente a revisão. A revisão apresenta valor informado, taxa e total final.

### Dinheiro e crédito loja

Esses meios não consultam taxas remotas. O total final é igual ao valor informado e a aplicação abre diretamente a revisão.

## Estado e navegação

O estado local do fluxo armazenará separadamente:

- pedido selecionado;
- meio de pagamento selecionado;
- dígitos do valor;
- opções de cotação;
- parcela selecionada;
- snapshot confirmado com valor original, taxa, total final e parcelas;
- requisição ativa e chave de idempotência.

Selecionar outro meio limpa valor, cotação, parcela e confirmação. Alterar o valor limpa cotação, parcela e confirmação. Cancelar a revisão não registra nada, não cria cobrança no terminal e retorna à última tela configurável.

## Fonte do valor cobrado

O total exibido na revisão é a fonte do valor monetário enviado ao PagBank:

- meios com cotação usam o `totalValue` da opção selecionada;
- meios diretos usam o próprio valor informado;
- o valor é arredondado uma única vez para centavos antes do SDK;
- o número de parcelas enviado é exatamente o selecionado no wizard;
- o log local e `PaymentData.amountFinal` usam o mesmo total confirmado.

Ao criar `POST /orders/{orderId}/payment-attempts`, o aplicativo continuará enviando valor original, meio, parcelas e chave de idempotência. O `amount_final` devolvido pela tentativa será preservado para diagnóstico, mas não substituirá o total confirmado e uma divergência não bloqueará o PagBank. A divergência será registrada em log sem expor dados sensíveis.

## Revisão do backend

### Problema encontrado

O endpoint `/calculate-fees` e a criação de `/payment-attempts` possuem cálculos duplicados:

- `/calculate-fees` aplica `interest_tax` e `payment_method_surcharges`;
- `/payment-attempts` usa `calculateReceivables`, que aplica `interest_tax`, mas atualmente não inclui `payment_method_surcharges` no total do cliente.

Essa diferença permite que o total exibido na cotação seja diferente do `amount_final` preparado e posteriormente registrado.

### Correção

O backend terá uma regra compartilhada para calcular o total cobrado do cliente. Essa regra receberá valor original, meio, parcelas, plano e adicionais e produzirá, com arredondamento monetário único:

- juros;
- adicionais;
- valor final;
- valor de cada parcela;
- indicação de pagamento sem juros.

Tanto `/calculate-fees` quanto `calculateReceivables`, usado por tentativas online e registros manuais, consumirão essa regra. As rotinas de split e taxa transacional continuarão usando o recebível calculado, sem alterar o total já confirmado pelo cliente.

## Tratamento de erros

- Falha ao carregar meios: permanecer na escolha e permitir nova tentativa.
- Valor zero ou inválido: manter `Continuar` desabilitado.
- Falha de cotação: permanecer na tela de valor ou parcelamento e permitir tentar novamente.
- Cotação vazia: informar que não há condição disponível para o meio selecionado.
- Parcela não selecionada no crédito: manter a revisão indisponível.
- Cancelamento da revisão: não executar efeitos externos.
- Falha na preparação online: não abrir o PagBank e permitir nova tentativa com a mesma chave idempotente.
- Divergência entre cotação confirmada e tentativa preparada: registrar diagnóstico e continuar usando o total confirmado.
- Aprovação no PagBank seguida de falha de registro: manter a retentativa idempotente sem realizar nova cobrança.

## Testes automatizados

### Aplicativo Android

- `Pagar` abre a escolha do meio antes do teclado de valor.
- Selecionar um meio abre a tela de valor.
- Alterar o meio ou o valor invalida cotação, parcela e confirmação dependentes.
- Crédito abre parcelamento em tela própria e exige seleção.
- Débito, Pix e transferência Pix usam a cotação de uma parcela.
- Dinheiro e crédito loja usam total igual ao valor original.
- A revisão mostra método, valor original, taxa, parcelas e total corretos.
- Cancelar a revisão não chama PagBank nem registra pagamento.
- Confirmar um meio online envia ao PagBank o total revisado e as parcelas selecionadas.
- Um `amount_final` diferente na tentativa não substitui o total revisado e não bloqueia o PagBank.
- Confirmar um meio manual registra somente depois da revisão.
- Voltar segue a sequência correta para cada tipo de pagamento.

### Backend

- A regra compartilhada cobre `seller`, `buyer` e `none`.
- Juros percentuais e adicionais fixos/percentuais são arredondados de forma consistente.
- Para cada meio ativo, a opção de `/calculate-fees` e o recebível de `calculateReceivables` produzem o mesmo total com a mesma parcela.
- Tentativas online persistem o mesmo `amount_final` retornado pela cotação equivalente.
- Registros manuais persistem o mesmo total da cotação equivalente.
- Idempotência e conclusão de tentativas permanecem inalteradas.

## Validação no device

A validação final deve exercitar todos os meios disponíveis no aparelho:

- crédito;
- débito;
- Pix online;
- transferência Pix;
- dinheiro;
- crédito loja.

Para cada meio serão verificados ordem das telas, navegação de volta, valor original, taxa, total final, confirmação e efeito posterior. Para crédito também serão verificadas parcelas. Para meios online será conferido o valor efetivamente entregue ao PlugPag pelos logs. Depois da instalação, o app será aberto e o `adb logcat` será monitorado para `com.detrapay`, `AndroidRuntime` e `FATAL EXCEPTION`.

## Critérios de aceite

- A primeira tela do pagamento é a escolha do meio.
- O valor é informado somente na tela seguinte.
- Crédito possui uma tela separada de parcelamento.
- Todos os meios possuem revisão antes de qualquer efeito externo.
- A revisão não apresenta indicador numérico de etapas.
- O PagBank recebe exatamente o total final visto e confirmado pelo usuário e as parcelas selecionadas.
- Uma divergência na preparação não bloqueia nem altera o valor enviado ao PagBank.
- Cotação, tentativa e registro do backend usam a mesma regra de cálculo.
- Todos os meios são validados no device.
- As suítes relacionadas do app e do backend passam sem regressões.
