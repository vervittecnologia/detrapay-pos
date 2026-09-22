# Integridade de pagamentos, isolamento de sessao e seguranca de transporte

## Objetivo

Concluir o fluxo atomico de pagamentos entre o Detrapay POS e o Detrapay Manager, impedir cobrancas duplicadas ou aprovacoes perdidas e garantir que dados de uma sessao nunca sejam reutilizados por outra. A entrega deve preservar o fluxo visual e as regras financeiras atuais, corrigindo o contrato incompleto entre os dois repositorios.

O sucesso significa que uma aprovacao PagBank sobrevive a falha de rede, erro de persistencia local, recriacao do processo e repeticao da requisicao sem abrir uma segunda cobranca; que o backend registra cada transacao externa no maximo uma vez; e que logout/login nao expõe pedidos, detalhes ou vendedores da sessao anterior.

## Estado atual confirmado

- O POS prepara pagamentos em `POST /orders/{orderId}/payment-attempts` e declara chamadas para `POST /payment-attempts/{id}/complete` e `POST /orders/{orderId}/manual-payments`.
- O Manager implementa apenas a preparacao da tentativa. As duas rotas de conclusao ainda nao estao registradas no roteador da Edge Function.
- `payment_attempts` aparece nos tipos gerados do Supabase, mas sua criacao e suas garantias de unicidade nao estao representadas por migration versionada no repositorio.
- O POS mantem a conclusao aprovada apenas em memoria. Um encerramento do processo perde essa recuperacao.
- O log Room e gravado antes da pendencia aprovada. Se esse insert falhar, uma nova tentativa pode reabrir o terminal.
- A reconciliacao de falha ambigua identifica a operacao por pedido, valor e tipo. Duas cobrancas iguais no mesmo pedido podem corresponder a transacao errada.
- O cancelamento libera o estado do fluxo antes de o SDK terminar; a operacao original pode concluir enquanto outra cobranca ja foi iniciada.
- Os caches singleton de pedidos, detalhes e vendedores nao pertencem explicitamente a uma identidade de sessao.
- Downloads por URL absoluta usam o cliente autenticado, permitindo envio de cabecalhos internos para um host externo.

## Escopo

### Manager

- Versionar a estrutura de `payment_attempts` e suas restricoes em uma migration nova, sem reescrever migrations anteriores.
- Implementar conclusao idempotente de tentativa online.
- Implementar registro idempotente de pagamento manual.
- Executar criacao da parcela/recebivel, atualizacao do pedido e fechamento da tentativa em uma unica transacao de banco.
- Impedir que o mesmo identificador de transacao externa conclua tentativas diferentes.
- Validar identidade, acesso ao pedido, estado, expiracao, meio de pagamento e dados preparados antes da mutacao.
- Retornar uma referencia curta e exclusiva da tentativa para uso no terminal.

### POS

- Persistir aprovacoes pendentes antes de qualquer atividade auxiliar que possa falhar.
- Recuperar e concluir aprovacoes pendentes apos recriacao do processo, sem executar nova cobranca.
- Reconciliar resposta ambigua do terminal por referencia exclusiva da tentativa.
- Serializar iniciar, cancelar, reconciliar e concluir pagamento em uma unica maquina de estados.
- Isolar caches pela identidade da sessao e limpa-los explicitamente no logout.
- Separar downloads publicos do cliente HTTP autenticado.
- Remover corpos sensiveis dos logs HTTP e logs manuais.

## Fora de escopo

- Alterar taxas, juros, parcelamento, simulacao ou a fonte do valor final ja definida pelo fluxo atual.
- Redesenhar telas de pedidos ou pagamento.
- Implementar a paginacao, fotos/documentos, compatibilidade de API minima ou otimizacoes de UI dos lotes seguintes.
- Aplicar migration ou publicar Edge Function em producao.
- Instalar APK no dispositivo.
- Rotacionar o keystore de assinatura, reescrever historico Git ou revogar credenciais. Essas operacoes exigem um procedimento operacional e autorizacao separados.

## Arquitetura escolhida

A entrega sera incremental e compativel com o contrato ja iniciado. O Manager sera a autoridade final da operacao financeira; o POS manterá um registro duravel apenas para permitir a retomada segura entre a aprovacao do terminal e a confirmacao do servidor.

O fluxo sera dividido em quatro componentes de responsabilidade unica:

1. `PaymentAttempt` no Manager prepara e identifica uma operacao financeira.
2. Uma funcao SQL transacional conclui a tentativa ou registra um pagamento manual de forma idempotente.
3. Um registro Room de conclusao pendente conserva a aprovacao PagBank ate o aceite definitivo do backend.
4. Uma maquina de estados no POS coordena terminal, persistencia, reconciliacao, cancelamento e conclusao.

Nao sera criado fallback que grave diretamente um recebivel quando o contrato atomico falhar.

## Contrato do Manager

### Preparar pagamento online

`POST /orders/{orderId}/payment-attempts` permanece idempotente por `idempotency_key` e passa a retornar tambem:

```json
{
  "data": {
    "id": "uuid",
    "status": "prepared",
    "terminal_reference": "A1B2C3D4E5"
  }
}
```

`terminal_reference` deve ser estavel para a tentativa, ter no maximo 10 caracteres, usar somente caracteres aceitos pelo PlugPag e possuir restricao de unicidade. Repetir a mesma `idempotency_key` devolve a mesma tentativa e a mesma referencia.

### Concluir pagamento online

`POST /payment-attempts/{attemptId}/complete` recebe:

```json
{
  "transaction_id": "identificador externo obrigatorio",
  "authorization_code": "opcional",
  "card_brand": "opcional",
  "card_last4": "opcional",
  "card_holder": "opcional",
  "transaction_log": {}
}
```

Regras:

- A tentativa deve pertencer a um pedido acessivel ao usuario autenticado.
- O meio deve ser online, a tentativa deve estar preparada e nao expirada na primeira conclusao.
- O valor, as parcelas e o meio usados para criar o recebivel vêm exclusivamente da tentativa preparada.
- `transaction_id` e obrigatorio e unico entre tentativas.
- A primeira requisicao valida cria o recebivel, atualiza o saldo/estado do pedido, grava `external_transaction_id`, `completed_receivable_id`, `completed_at` e muda o estado para `completed` na mesma transacao.
- Repetir a conclusao da mesma tentativa com o mesmo `transaction_id` devolve HTTP 200 e o mesmo resultado, com `created: false`.
- Usar o mesmo `transaction_id` em outra tentativa devolve HTTP 409 e nao altera dados.
- Repetir a mesma tentativa com outro `transaction_id` devolve HTTP 409.

A resposta preserva o formato que o POS ja declara:

```json
{
  "data": {
    "receivable_id": 123,
    "order_id": 456,
    "created": true
  },
  "updatedOrder": {}
}
```

### Registrar pagamento manual

`POST /orders/{orderId}/manual-payments` usa `idempotency_key` para criar no maximo um recebivel manual. O backend valida que o meio nao e online e executa a criacao e atualizacao do pedido na mesma transacao. Repeticoes devolvem o mesmo recebivel com `created: false`.

### Migration e acesso

A migration deve criar ou reconciliar `payment_attempts` com:

- chave primaria UUID;
- chaves estrangeiras para pedido, empresa, dispatcher, metodo e recebivel concluido;
- unicidade de `idempotency_key`, `terminal_reference` e `external_transaction_id` quando nao nulo;
- estados documentados `prepared`, `completed` e `expired`;
- timestamps de criacao, atualizacao, expiracao e conclusao;
- indice para busca por pedido e estado;
- politicas que nao permitam mutacao direta pelo cliente. As Edge Functions usam o service role somente depois de validar o token e o contexto organizacional.

Se a tabela ja existir no ambiente remoto, a migration deve ser aditiva e segura para esse estado, sem apagar tentativas existentes.

## Persistencia e retomada no POS

O Room ganha uma entidade de conclusao pendente com, no minimo:

- `attemptId` como chave;
- `idempotencyKey`;
- `terminalReference`;
- `orderId` e identidade da sessao;
- `transactionId`;
- dados necessarios para o payload de conclusao;
- valor original/final e meio apenas para auditoria e apresentacao;
- estado local e timestamps.

Ao receber aprovacao do PlugPag, o POS deve:

1. Validar que existe `transaction_id`.
2. Persistir a conclusao pendente de forma duravel.
3. Gravar o log local da transacao como atividade auxiliar e tolerante a falha.
4. Chamar a conclusao atomica no Manager.
5. Remover a pendencia somente depois de resposta idempotente de sucesso.

Se a persistencia da pendencia falhar, o app nao deve informar conclusao nem iniciar automaticamente outra cobranca. A UI orienta a conferir a venda e tentar a recuperacao; o resultado aprovado continua sendo tratado como operacao incerta.

Ao iniciar o fluxo de pedidos e ao abrir a tela de pagamento, o app procura pendencias da sessao atual e tenta somente a etapa de conclusao HTTP. Uma pendencia nunca chama `doPayment` novamente.

Pendencias de outra identidade permanecem isoladas e nao sao exibidas ou processadas. O logout nao apaga uma aprovacao financeira ainda nao sincronizada; ela continua vinculada a identidade original para recuperacao no proximo login correspondente.

## Maquina de estados do pagamento

Os estados operacionais serao:

- `Idle`
- `Preparing`
- `TerminalActive`
- `AbortRequested`
- `ApprovedPendingPersistence`
- `ApprovedPendingServer`
- `Completed`
- `Failed`

Somente `Idle`, `Completed` e uma falha definitivamente recusada podem iniciar uma nova cobranca. `AbortRequested` nao libera a operacao imediatamente: o fluxo aguarda a resposta final do SDK. Se uma aprovacao chegar depois do pedido de cancelamento, ela segue obrigatoriamente para persistencia e conclusao.

Chamadas concorrentes sao protegidas por `Mutex` e por um identificador da operacao ativa. Eventos atrasados de outra operacao sao ignorados para a UI, mas uma aprovacao valida nunca e descartada.

## Reconciliacao de falha ambigua

O POS envia `terminal_reference` no `userReference` do PlugPag. Para recuperar uma falha A011 ou equivalente, `getLastApprovedTransaction()` so e aceito quando todos os criterios coincidirem:

- resultado aprovado;
- `transaction_id` presente;
- `userReference` igual a `terminal_reference`;
- valor em centavos igual ao valor preparado;
- tipo de pagamento compativel, quando retornado pelo SDK.

Uma correspondencia aprovada e persistida antes da chamada ao Manager. Ausencia ou divergencia nao abre automaticamente uma segunda cobranca e orienta conferencia no PagBank.

## Isolamento de sessao e cache

Os caches de pedidos, detalhes e vendedores carregam uma chave de escopo derivada da identidade autenticada. O repositorio resolve a identidade antes de consultar o cache. Uma entrada so pode ser reutilizada quando a chave atual for identica.

Sera criado um coordenador de limpeza de sessao, injetando os repositorios que mantêm estado em memoria. O logout executa, nesta ordem:

1. bloqueia novas leituras autenticadas;
2. limpa caches em memoria;
3. preserva pendencias financeiras vinculadas a identidade;
4. remove tokens e usuario local;
5. conclui a navegacao para login.

Logout disparado manualmente e por sessao expirada usa o mesmo caminho.

## Transporte, midia e logs

- O cliente autenticado atende somente a API Detrapay.
- Downloads por URL absoluta usam um cliente sem `Authorization`, refresh automático ou cabecalhos de dispositivo.
- URLs de midia devem usar HTTPS. Esquemas inseguros ou desconhecidos sao rejeitados.
- A resposta de imagem e limitada por tamanho e por tipo de conteudo antes da decodificacao.
- O interceptor de logging nao registra corpos de login, refresh ou respostas com tokens, mesmo em debug.
- Logs manuais nao incluem payloads completos, tokens, CPF/CNPJ ou o JSON bruto da transacao. Identificadores tecnicos podem ser parcialmente mascarados.
- O log de auditoria do Manager deve sanitizar campos sensiveis de forma recursiva e limitar o tamanho do corpo persistido.

## Tratamento de erros

- Falha antes de abrir o terminal: encerra como erro seguro e permite nova tentativa.
- Recusa definitiva do terminal: registra telemetria sanitizada e permite nova tentativa.
- Falha ambigua: tenta reconciliacao exata; sem correspondencia, bloqueia repeticao automatica e orienta conferencia.
- Aprovacao com falha no log auxiliar: continua a conclusao, pois o log nao e a fonte financeira de verdade.
- Aprovacao com falha HTTP: mantem pendencia duravel e oferece nova tentativa de sincronizacao sem cobrar novamente.
- HTTP 409 por transacao usada em outra tentativa: mantem a pendencia, apresenta erro de integridade e exige intervencao; nao cria recebivel alternativo.
- Sessao expirada durante conclusao: renova a sessao uma vez e repete a mesma requisicao idempotente.
- Dados ou endpoints ausentes no Manager: o POS nao mascara o problema com gravacao no endpoint legado.

## Testes

### Manager

- Preparacao repetida retorna a mesma tentativa e referencia.
- Conclusao cria um unico recebivel e atualiza o pedido atomicamente.
- Repeticao da mesma conclusao retorna o mesmo resultado.
- Mesmo `transaction_id` em tentativa diferente retorna 409 sem mutacao.
- Tentativa concluida com outro `transaction_id` retorna 409.
- Tentativa expirada, meio offline e pedido inacessivel sao rejeitados.
- Pagamento manual repetido por `idempotency_key` cria um unico recebivel.
- Falha intermediaria faz rollback completo.
- Sanitizacao recursiva remove segredos e limita o corpo de auditoria.

### POS

- Falha ao salvar o log depois da aprovacao nao abre nova cobranca.
- Recriacao do ViewModel/processo recupera a pendencia e chama apenas `complete`.
- Repetir conclusao bem-sucedida nao chama o terminal.
- A011 aceita somente a referencia exclusiva da tentativa.
- Cancelamento concorrente com aprovacao persiste e conclui a aprovacao.
- Nova cobranca fica bloqueada enquanto a operacao anterior nao tem resultado definitivo.
- Usuario B nao reutiliza caches do usuario A.
- Logout manual e expiracao limpam os mesmos caches.
- Pendencia do usuario A nao e processada pelo usuario B.
- Download externo nunca recebe `Authorization` ou token de refresh.
- Logs de debug nao contêm senha, token ou corpo bruto da transacao.

## Verificacao e entrega

1. Executar testes focados do Manager e do POS durante o desenvolvimento.
2. Executar `npm run lint`, `npm run test` e `npm run build` no Manager.
3. Executar em um unico lote final os testes Gradle aplicaveis, `lintDebug` e `assembleDebug`, preferindo `--offline`.
4. Inspecionar os diffs para garantir que mudancas locais preexistentes nao foram sobrescritas.
5. Nao publicar nem aplicar migration remotamente sem nova autorizacao explicita.
6. Se houver autorizacao posterior para instalar o APK, limpar o log, instalar o artefato existente, abrir `com.detrapay/.ui.splash.SplashActivity` e inspecionar `adb logcat` filtrado por `com.detrapay`, `AndroidRuntime` e `FATAL EXCEPTION`.

## Criterios de aceite

- As tres rotas do fluxo atomico existem, sao autenticadas e possuem testes de contrato.
- Uma transacao externa pode produzir no maximo um recebivel.
- Uma tentativa pode ser concluida no maximo uma vez, com repeticoes idempotentes.
- Nenhuma falha posterior a aprovacao chama o PlugPag novamente para a mesma tentativa.
- Aprovacoes pendentes sobrevivem a recriacao do processo e ficam isoladas por identidade.
- Cancelar e aprovar simultaneamente nao libera uma segunda cobranca.
- Caches nunca atravessam sessoes.
- Downloads externos nao recebem credenciais Detrapay.
- Logs nao expõem senha, token, CPF/CNPJ ou JSON bruto da transacao.
- Testes, lint e builds definidos para os dois repositorios passam, ou falhas preexistentes e nao relacionadas sao registradas separadamente com evidencia.
- Nenhuma alteracao de producao e realizada como parte desta implementacao local.
