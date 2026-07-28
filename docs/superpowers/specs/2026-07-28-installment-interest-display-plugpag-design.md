# Exibição de parcelamento com juros e envio à PlugPag

## Objetivo

Exibir de forma consistente, em todas as superfícies de parcelamento, o valor original, o total com juros e o valor de cada parcela com juros. O total confirmado deve ser o mesmo valor enviado à PlugPag, junto com a quantidade de parcelas e o tipo de parcelamento do vendedor.

## Fonte dos valores

O aplicativo usará a cotação retornada pelo backend como fonte dos valores parcelados:

- valor original: valor informado pelo usuário para o pagamento;
- total com juros: `InstallmentFee.totalValue`;
- valor da parcela com juros: `InstallmentFee.installmentValue`;
- quantidade: `InstallmentFee.installmentNumber`.

O app não recalculará juros nem substituirá dados ausentes do backend. Um apresentador compartilhado receberá o valor original e a cotação e produzirá os textos formatados para todas as telas.

## Apresentação

Cada opção com juros exibirá:

- `Valor original: R$ 100,00`;
- `Total com juros: R$ 108,00`;
- `3x de R$ 36,00 com juros`.

Quando `noInterest` for verdadeiro, os textos usarão `Total` e `sem juros`. O padrão será aplicado ao checkout Compose, simulador, conteúdo copiado/compartilhado, adapters e telas legadas de cadastro, revisão/processamento e detalhes de pagamentos parcelados. O valor original poderá permanecer também no cabeçalho.

## PlugPag

O fluxo confirmado manterá o total com juros selecionado em `OrderPaymentRequest.amountFinal`. Ao criar `PlugPagPaymentData`, o aplicativo enviará:

- o total com juros convertido e arredondado para centavos;
- a quantidade de parcelas selecionada;
- `PlugPag.INSTALLMENT_TYPE_PARC_VENDEDOR` para duas ou mais parcelas;
- `PlugPag.INSTALLMENT_TYPE_A_VISTA` para uma parcela.

O parâmetro de construção correspondente a `setInstallmentType` será preenchido explicitamente. Se `POST /orders/{orderId}/payment-attempts` devolver um `amount_final` diferente do total exibido, o fluxo será interrompido antes de chamar a PlugPag e informará uma inconsistência do backend. O app não tentará reconciliar os valores localmente.

## Testes e validação

- Testar o apresentador compartilhado com cotações com e sem juros.
- Testar que checkout, simulador, compartilhamento e adapters recebem os três valores da mesma apresentação.
- Capturar `PlugPagPaymentData` e verificar total em centavos, parcelas e `INSTALLMENT_TYPE_PARC_VENDEDOR`.
- Testar que divergência entre o total exibido e o preparado bloqueia a PlugPag.
- Executar os testes unitários focados e a suíte debug.
- Limpar o log, instalar no device, abrir `com.detrapay/.ui.splash.SplashActivity` e inspecionar `adb logcat` filtrado por `com.detrapay`, `AndroidRuntime` e `FATAL EXCEPTION`.

## Critérios de aceite

- Toda exibição de parcelamento mostra valor original, total e parcela usando os valores da cotação.
- Textos distinguem corretamente operações com e sem juros.
- O total exibido é exatamente o total enviado à PlugPag em centavos.
- Parcelamentos usam a constante de acréscimo vendedor e preservam a quantidade selecionada.
- Divergências do backend não são mascaradas pelo aplicativo.
- Testes passam e o app instalado abre sem falha fatal nos logs filtrados.
