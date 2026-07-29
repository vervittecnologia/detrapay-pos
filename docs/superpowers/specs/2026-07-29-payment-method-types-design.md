# Tipos de meios de pagamento

## Objetivo

Simplificar a tela “Novo pagamento” para que o vendedor escolha um tipo de pagamento, e não uma configuração específica cadastrada pelo backend.

## Comportamento

- Agrupar os meios recebidos pelo valor normalizado de `paymentType`.
- Exibir somente uma opção para cada tipo disponível: Crédito, Débito, Pix, Transferência Pix, Dinheiro ou Crédito loja.
- Manter os grupos “Pagamentos na maquininha” e “Registrar pagamento”.
- Exibir `Pix` em “Pagamentos na maquininha”; esse tipo inicia a cobrança Pix no terminal.
- Exibir `Transferência Pix` em “Registrar pagamento”; esse tipo representa uma transferência já recebida fora da maquininha e apenas registra o pagamento no pedido.
- Na etapa de valor, tipos de registro externo devem exibir “Registro manual” e orientar o vendedor a informar o valor recebido; “Teclado da maquininha” fica restrito aos pagamentos processados no terminal.
- Usar um meio representativo do tipo apenas para iniciar o fluxo.
- Para Crédito, resolver o meio exato depois que o usuário escolher o parcelamento, preservando o comportamento atual de `resolvePaymentMethod`.
- Trocar “Saldo disponível” por “Valor pendente”.
- Manter o valor pendente destacado e calculado pelo resumo financeiro atual.

## Apresentação

- Título principal: “Escolha a forma de pagamento”.
- Crédito: “À vista ou parcelado”.
- Débito: “Pagamento na maquininha”.
- Pix: “Pix pela maquininha”.
- Transferência Pix: “Registrar transferência já recebida”.
- Dinheiro: “Registrar valor recebido”.
- Crédito loja: “Registrar crédito concedido”.

## Testes

- A lista agrupada contém somente um item por tipo normalizado.
- O representante escolhido é determinístico.
- Tipos online e manuais continuam nas seções corretas.
- `pix` e `pix_manual` permanecem como escolhas distintas e não são agrupados entre si.
- A tela usa “Valor pendente” e não contém “Saldo disponível”.
