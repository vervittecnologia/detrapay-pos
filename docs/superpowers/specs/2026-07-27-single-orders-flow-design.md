# Fluxo unico de pedidos

## Objetivo

Transformar as telas atualmente exibidas no device no unico fluxo suportado pelo aplicativo. O app deixa de reconhecer, selecionar, persistir ou nomear modos como completo, simplificado ou checkout direto.

O fluxo canonico inicia na tela **Pedidos** e preserva as jornadas hoje acessiveis a partir dela: consultar e buscar pedidos, atualizar dados, criar pedido, abrir detalhes, receber pagamentos, simular parcelamento e encerrar a sessao.

## Escopo

### Incluido

- Tornar a tela atual de Pedidos o destino inicial da Home.
- Remover o roteamento condicional por modo.
- Remover modelos, propriedades, persistencia e testes relacionados a modo de aplicativo.
- Renomear o codigo atualmente identificado como `direct_checkout` para nomes canonicos do fluxo de pedidos.
- Renomear componentes ainda identificados como `simplified` quando fizerem parte do fluxo atual.
- Remover destinos, fragments, menus e recursos da Home antiga que nao sejam alcancaveis pelo fluxo atual.
- Preservar as Activities e componentes usados pelas jornadas atuais de cadastro, detalhes, pagamento e relatorio.
- Ajustar a entrada de pagamento para sempre iniciar em `R$ 0,00`, sem preencher automaticamente o saldo pendente.
- Classificar meios de pagamento com uma unica propriedade, `PaymentMethod.isOnlinePayment`, originada de `is_online_payment` no `GET /payment-methods`.
- Garantir que pagamentos online so sejam gravados e exibidos no pedido depois da aprovacao PagBank no device.
- Registrar Credito Loja, Transferencia Pix e Dinheiro imediatamente apos a confirmacao do usuario, sem PagBank e sem tela de espera.
- Atualizar testes e referencias para refletir o fluxo unico.
- Validar compilacao, testes e execucao no device conectado.

### Fora de escopo

- Redesenhar as telas atuais.
- Alterar regras de negocio de pedidos ou pagamentos alem das regras de classificacao e persistencia explicitadas neste documento.
- Migrar toda a navegacao para uma Activity Compose unica.
- Criar compatibilidade visual ou comportamental com as Homes antigas.
- Implementar workaround para eventual ausencia de dados ou contratos do backend.
- Remover ou alterar de forma incompativel campos, modelos ou endpoints do backend ja usados pelas versoes em producao. Este documento exige evolucao aditiva e retrocompativel.

## Arquitetura

`HomeActivity` continua como host Android da navegacao principal, mas deixa de observar estado para escolher uma superficie. O grafo de navegacao passa a iniciar diretamente no fragment canonico de Pedidos.

O fragment de Pedidos continua sendo a fronteira de integracao com APIs Android, dialogs, clipboard, intents e navegacao. A rota Compose observa os ViewModels, monta o estado imutavel e encaminha efeitos. As telas Compose continuam responsaveis apenas pela apresentacao e emissao de acoes do usuario.

O fluxo sera organizado com nomes de dominio, sem qualificadores de modo. Os nomes canonicos serao:

```text
HomeActivity
  -> OrdersFragment
      -> OrdersRoute
          -> OrdersScreen
          -> OrderFlowContract
          -> OrdersViewModel
          -> OrderPresentation
          -> OrderPaymentRouter
```

O pacote sera `com.detrapay.ui.home.orders`. `OrderFlowContract` reunira `OrderFlowState`, `OrderFlowLocalState`, `OrderFlowStep`, `OrderFlowAction`, `OrderFlowEffect` e `OrderFeeRequestTarget`. Os demais arquivos especificos serao `OrderFlowReducer`, `OrderFlowColors`, `OrderFlowBlocks`, `OrderPreviewFixtures` e `OrdersScreenPreview`. As telas internas `CreditScreen`, `DebitScreen`, `DetailScreen`, `InstallmentSimulatorScreen`, `KeypadScreen`, `MethodScreen` e `WaitingScreen` manterao seus nomes dentro de `orders.screens`.

## Navegacao

- O destino inicial do grafo da Home sera Pedidos.
- A navegacao tardia de `HomeActivity` para outro destino sera eliminada.
- A barra inferior e seu menu serao removidos do layout e dos recursos.
- O grafo da Home mantera somente `ordersFragment` como inicio e `registrationActivity` como destino da acao de novo pedido.
- Serao removidos do grafo `registrationFragment`, `orderListFragment`, `profileFragment`, `paymentHistoryFragment` e `notificationsActivity`.
- Serao removidas as implementacoes exclusivas desses destinos: `ui.home.registration`, `ui.home.order_list`, `ui.home.profile`, `ui.home.payment_history` e `ui.notification`, junto de seus layouts e do registro de `NotificationActivity` no manifest.
- A criacao de pedido continuara abrindo `RegistrationActivity` a partir da acao de novo pedido.
- Detalhes continuara exibindo o resumo financeiro e os pagamentos registrados. Quando houver saldo pendente, seu botao de acao sera apenas `Pagar`, sem valor e sem o texto `Pagar saldo`.
- A tela de pagamento exibira o saldo pendente como informacao secundaria e oferecera o atalho `Usar valor pendente`, sem transformar esse saldo no valor principal.
- Simulacao, relatorio e logout manterao o comportamento atual.
- Voltar a partir da raiz de Pedidos continuara oferecendo a confirmacao de logout, conforme o comportamento atual.

## Remocao do conceito de modo

Serao removidos do dominio e da sessao:

- `SellerAppMode`.
- `HomeModeRouter`.
- `LoggedInUser.appMode` e propriedades derivadas.
- `Company.sellerAppMode`, `Company.normalizedSellerAppMode`, `AuthResponse.appMode` e o campo correspondente do DTO de empresa.
- Chaves e metodos de persistencia de modo em `AuthRepository`.
- Selecao ou forca de modo em `LoginRepository` e `HomeViewModel`.
- Testes dedicados a roteamento ou normalizacao de modos.

Os campos antigos de modo serao removidos dos DTOs do app. Caso o backend continue enviando esses campos, eles serao tratados como propriedades JSON extras e ignorados pela desserializacao. O app nao enviara nem persistira um valor de modo.

## Estado e dados

O carregamento da Home continuara fornecendo nome e documento da empresa, dados de operador e vendedores necessarios ao fluxo. O estado deixa de carregar qualquer indicador de modo.

A lista de Pedidos continua vindo do repositorio atual. Atualizacao manual, loading, vazio e erro permanecem representados no estado da rota. A apresentacao de totais, valor pago, saldo e status preserva as regras atuais.

Ao iniciar um pagamento, `paymentDigits` sera vazio e o valor principal exibido sera `R$ 0,00`. O saldo pendente sera calculado separadamente e usado somente na dica formada por `Valor pendente:` mais o saldo formatado e no atalho `Usar valor pendente`. Esse atalho preenchera `paymentDigits` com o saldo atual; ele nunca sera acionado automaticamente. O botao `Pagar` permanecera desabilitado enquanto o valor principal for zero.

Os dados de leitura de pedidos e de simulacao de parcelas continuam usando os repositorios e ViewModels existentes. A escrita de pagamentos da nova versao adotara o contrato atomico descrito abaixo. Esse contrato deve ser aditivo: os endpoints atuais que exigem um recebivel pendente permanecem disponiveis para as versoes do app ja instaladas, mas nao serao usados pelo novo fluxo.

### Classificacao unica dos meios de pagamento

`PaymentMethod` tera uma unica propriedade de classificacao:

```kotlin
val isOnlinePayment: Boolean
```

Ela sera desserializada exclusivamente de `is_online_payment`. O app nao adicionara nem usara `requiresTerminal`, `allowsManualConfirmation`, `paymentGateway` ou outra propriedade paralela para escolher o fluxo. Essa eliminacao vale para o modelo de decisao Android: os campos do contrato backend usados por versoes ja publicadas nao serao removidos de forma destrutiva.

- `isOnlinePayment == true`: Pagamento online. Inclui Credito, Debito e Pix. Exige aprovacao PagBank no device.
- `isOnlinePayment == false`: Pagamento apenas para registro. Inclui Credito Loja (`store_credit`), Transferencia Pix (`pix_manual`) e Dinheiro (`cash`/`dinheiro`).

A tela de meios de pagamento recebera e selecionara o objeto `PaymentMethod` completo. Isso preserva o `id`, o parcelamento e `isOnlinePayment`, e impede que Pix (`pix`) seja confundido com Transferencia Pix (`pix_manual`).

### Persistencia de pagamentos

Para pagamentos online:

1. O usuario confirma valor, meio e parcela no app.
2. O app inicia PagBank sem criar recebivel no pedido.
3. Falha, cancelamento, timeout ou retorno diferente de aprovado encerra a tentativa sem gravar qualquer recebivel.
4. Somente depois da aprovacao PagBank o app envia os dados da transacao para o backend.
5. O backend grava atomicamente o recebivel ja aprovado e devolve o pedido atualizado.

Credito, Debito e Pix nunca podem aparecer com status `PENDING`. Nao sera permitido criar um recebivel pendente e depois tentar remove-lo em caso de falha.

Para pagamentos apenas de registro:

1. O usuario confirma Credito Loja, Transferencia Pix ou Dinheiro.
2. O app nao chama PagBank e nao navega para `WaitingScreen`.
3. O backend grava atomicamente o pagamento confirmado.
4. O app exibe sucesso e atualiza Pedidos.

### Pre-requisito de backend

O contrato atual nao permite cumprir a persistencia acima: `POST /orders/{id}/receivables` cria um recebivel antes da aprovacao; `POST /receivables/{id}/confirm-payment`, `POST /receivables/{id}/generate-pix` e `POST /update-split-config` dependem desse recebivel ja persistido.

Antes da implementacao Android, o backend deve fornecer um fluxo novo, aditivo e retrocompativel que:

- Inicie/configure Credito, Debito ou Pix sem anexar um recebivel pendente ao pedido.
- Receba o resultado PagBank aprovado e crie atomicamente um recebivel final, nunca `PENDING`.
- Registre atomicamente Credito Loja, Transferencia Pix e Dinheiro como pagamentos confirmados.
- Retorne o pedido atualizado depois do registro final.
- Corrija `GET /payment-methods`: todos os parcelamentos de Credito, incluindo 13x a 18x, Debito e Pix devem retornar `is_online_payment=true`; Credito Loja, Transferencia Pix e Dinheiro devem retornar `false`.
- Preserve os campos, modelos e endpoints atuais para que as versoes do app em producao continuem funcionando durante a migracao.
- Valide os consumidores antes de corrigir valores do catalogo compartilhado. Se a correcao puder alterar de forma insegura outro cliente publicado, forneca a semantica correta por contrato versionado ou escopo de cliente, preservando a resposta legada onde necessario.

O rollout deve ocorrer em duas etapas: primeiro publicar e validar o contrato aditivo no backend sem remover o contrato antigo; somente depois liberar a nova versao Android. Sem esse contrato, o app deve parar a implementacao do fluxo online e solicitar o ajuste do backend. Nao sera aceito criar e apagar recebiveis pendentes como compensacao.

## Tratamento de erros

- Falhas ao carregar pedidos continuam exibindo o estado atual com opcao de recarregar.
- Falhas de simulacao, parcelas e pagamento continuam seguindo os efeitos e mensagens existentes.
- Sessao expirada continua encaminhando o usuario para o fluxo de autenticacao.
- Dados ou contratos ausentes no backend nao serao mascarados no app. O problema devera ser descrito objetivamente para ajuste do endpoint correspondente.
- Se `is_online_payment` estiver ausente, o catalogo falhara com erro de configuracao; o app nunca inferira o fluxo pelo nome/tipo nem fara fallback de online para registro manual. A consistencia da matriz de valores sera validada no contrato backend, nao por uma segunda regra de classificacao no app.
- Falha ou cancelamento PagBank nao produz escrita no pedido.
- Falha ao registrar um meio manual permanece na tela de confirmacao com erro e sem abrir `WaitingScreen`.
- A remocao de destinos antigos deve falhar em compilacao ou teste caso ainda exista alguma referencia, impedindo uma remocao parcial silenciosa.

## Estrategia de migracao

1. Criar testes que expressem a Home com destino unico e estado sem modo.
2. Simplificar o dominio e a persistencia de sessao, removendo o conceito de modo.
3. Tornar Pedidos o destino inicial e simplificar `HomeActivity` e seu layout.
4. Renomear o pacote e os componentes do fluxo atual para nomes canonicos.
5. Remover destinos e implementacoes antigas que ficarem sem consumidores.
6. Atualizar testes, recursos e documentacao afetados.
7. Buscar globalmente por referencias residuais aos modos antigos.

As alteracoes serao feitas em passos compilaveis para evitar uma renomeacao ampla sem verificacao intermediaria.

## Fluxo e wireframes ASCII

O plano de implementacao devera repetir este mapa e estes wireframes como referencia de preservacao visual e comportamental. Eles representam as telas atuais; nao autorizam redesign.

### Mapa principal

```text
[Splash / Login]
       |
       v
+------------------+
| PEDIDOS          |
| lista + busca    |
+------------------+
   |       |      |
   |       |      +--------------------> [Logout]
   |       |
   |       +--> [+ Acoes] --> [Novo pedido] --> [RegistrationActivity]
   |                         |                         |
   |                         |                         +--> volta para Pedidos
   |                         |
   |                         +--> [Simular parcelas] --> [Simulador]
   |                                                       |
   |                                                       +--> copiar/compartilhar
   |
   +--> [Cartao do pedido]
             |
             +--> [Detalhes]
                     |
                     +--> [Pagar]
                              |
                              v
                      [Digitar valor]
                              |
                              v
                    [Forma de pagamento]
                       |      |      |
                       |      |      +--> Pix online
                       |      +---------> Debito online
                       +----------------> Credito online --> [Parcelas]
                                              |
                                              v
                                      [PagBank/resultado]
                                              |
                                              +--> volta para Pedidos

[Forma de pagamento]
          |
          +--> Credito Loja / Transferencia Pix / Dinheiro
                         |
                         +--> confirmar registro --> Pedidos
```

### Pedidos

Exemplo baseado na tela observada no device:

```text
+------------------------------------------------+
| Pedidos                         [Sair] [Buscar] |
| CONCESSIONARIA TESTE                           |
| 11.222.333/0001-81                             |
+------------------------------------------------+
| #544   28/07/2026              [ PENDENTE ]    |
| WESLEY DE CASTRO                               |
|------------------------------------------------|
| TOTAL          PAGO             FALTA          |
| R$ 2.570,18    R$ 0,00          R$ 2.570,18    |
| [progresso-----------------------------------] |
+------------------------------------------------+
| #537   23/07/2026              [ PENDENTE ]    |
| alberto de lima                                |
|------------------------------------------------|
| TOTAL          PAGO             FALTA          |
| R$ 2.330,71    R$ 2.330,71      R$ 2.330,71    |
+------------------------------------------------+
|                                          ( + ) |
+------------------------------------------------+
```

Ao tocar no botao flutuante:

```text
                                  +------------------+
                                  | Novo Pedido      |
                                  +------------------+
                                  | Simular Parcelas |
                                  +------------------+
                                             ( x )
```

### Detalhes e inicio do pagamento

```text
+------------------------------------------------+
| [<] Pedido #544                                |
+------------------------------------------------+
| Resumo financeiro                              |
| Total          Pago             Falta          |
| R$ 2.570,18    R$ 0,00          R$ 2.570,18    |
+------------------------------------------------+
| Pagamentos registrados                    0    |
|                                                |
|          Nenhum pagamento registrado          |
|                                                |
+------------------------------------------------+
|                   [ Pagar ]                    |
+------------------------------------------------+
```

### Valor e forma de pagamento

```text
+-----------------------+  +-----------------------+
| [<] PAGAMENTO         |  | [<] R$ 1.000,00       |
|                       |  |                       |
| DIGITE O VALOR        |  | Escolha a forma      |
| R$ 0,00               |  | de pagamento         |
| Pedido #544           |  | Pagamentos online    |
| Valor pendente:       |  | [ Credito           ] |
| R$ 2.570,18           |  | [ Debito            ] |
| [Usar valor pendente] |  | [ Pix               ] |
| [1] [2] [3]           |  | Pagamentos p/ registro|
| [4] [5] [6]           |  | [Transf.Pix][Loja][$]|
| [7] [8] [9]           |  |                       |
| [,] [0] [apagar]      |  |                       |
|                       |  |                       |
| [   Pagar (inativo) ] |  |                       |
+-----------------------+  +-----------------------+
```

Depois que o usuario digitar qualquer valor maior que zero, ou tocar em `Usar valor pendente`, o botao `Pagar` sera habilitado. Digitar e apagar todo o valor deve retornar a `R$ 0,00` e desabilitar o botao novamente.

### Credito, debito e resultado

```text
+-----------------------+  +-----------------------+
| [<] Credito           |  | Pagamento             |
| R$ 1.000,00           |  |                       |
|                       |  |       [status]        |
| Escolha o parcelamento|  | Aguardando pagamento |
| ( ) 1x R$ 1.000,00    |  | R$ 1.000,00          |
| ( ) 2x R$   520,00    |  |                       |
| ( ) 3x R$   353,33    |  | Conectando...        |
|                       |  |                       |
| [Continuar no credito]|  | [Tentar novamente]   |
+-----------------------+  | [Voltar para pedidos]|
                           +-----------------------+

+-----------------------+
| [<] Debito            |
| R$ 1.000,00           |
|                       |
| Resumo do debito      |
| Pagamento imediato    |
|                       |
| [Continuar no debito] |
+-----------------------+
```

Para Pix, o estado de resultado substitui a area de status pelo codigo gerado, com as acoes `Copiar codigo Pix` e `Voltar para pedidos`.

### Simulador de parcelas

```text
+------------------------------------------------+
| [<] Simular parcelas                       [x] |
+------------------------------------------------+
| Credito                                        |
| Simular parcelamento em ate 18x                |
|                                                |
| Valor                                          |
| [ R$ 2.570,18                               ]  |
| [ Consultar Parcelas ]                         |
|                                                |
| Parcelas                                       |
| ( ) 1x de R$ 2.570,18   Total R$ 2.570,18      |
| ( ) 6x de R$   465,00   Total R$ 2.790,00      |
| ( ) 12x de R$  252,00   Total R$ 3.024,00      |
|                                                |
| [ Copiar ]                 [ WhatsApp ]         |
+------------------------------------------------+
```

### Exemplos de verificacao do fluxo

- Pedido pendente: abrir `#544`, confirmar `Resumo financeiro` e o botao `Pagar`, entrar com `R$ 0,00`, digitar `R$ 1.000,00`, escolher credito e selecionar uma parcela; ao concluir, voltar para Pedidos e atualizar os totais.
- Atalho de saldo: abrir `#544`, confirmar a dica `Valor pendente: R$ 2.570,18`, tocar em `Usar valor pendente` e verificar que somente entao o valor principal muda de `R$ 0,00` para `R$ 2.570,18`.
- Pix: informar um valor, escolher Pix, gerar o codigo, copiar e voltar para Pedidos sem perder a navegacao raiz.
- PagBank recusado: tentar Credito, Debito ou Pix e simular falha/cancelamento; confirmar que nenhum pagamento foi salvo ou exibido no pedido.
- Pagamento para registro: confirmar Credito Loja, Transferencia Pix (`pix_manual`) e Dinheiro; confirmar que nenhum deles abre PagBank ou `WaitingScreen` e que o pedido e atualizado logo apos o registro.
- Novo pedido: abrir o menu `+`, entrar em `RegistrationActivity`, concluir ou cancelar e retornar para a lista canonica.
- Simulacao: abrir o menu `+`, consultar parcelas para `R$ 2.570,18`, selecionar uma opcao e testar copiar/compartilhar.
- Busca: pesquisar por numero, cliente ou CPF/CNPJ e limpar o filtro sem alterar os dados carregados.

## Testes e verificacao

- Testes unitarios das regras de apresentacao de pedidos e roteamento de pagamentos continuarao cobrindo o comportamento atual com nomes atualizados.
- Testes de `PaymentMethod` e roteamento comprovarao que somente `isOnlinePayment` decide entre PagBank e registro imediato.
- Testes do fluxo online comprovarao que falha/cancelamento PagBank executa zero chamadas de persistencia e que a persistencia ocorre uma unica vez depois da aprovacao.
- Testes do fluxo manual comprovarao que `store_credit`, `pix_manual` e `cash` nao chamam PagBank nem entram em `WaitingScreen`.
- Testes de repositorio e sessao serao ajustados para comprovar que nenhum modo e armazenado ou devolvido.
- O grafo sera verificado para garantir que Pedidos e o destino inicial e que os destinos removidos nao possuem referencias.
- Uma busca global devera confirmar que nao restam conceitos funcionais de `simplified`, `direct_checkout`, `SellerAppMode`, `HomeModeRouter` ou equivalentes. Referencias historicas em documentos antigos podem permanecer apenas quando claramente identificadas como historico.
- Serao executados os testes unitarios relevantes e o build de debug.
- No device conectado, a sequencia de verificacao sera:
  1. Limpar o log atual.
  2. Instalar o APK debug.
  3. Abrir `com.detrapay/.ui.splash.SplashActivity`.
  4. Confirmar visualmente que a Home abre diretamente em Pedidos.
  5. Inspecionar o `logcat` filtrado por `com.detrapay`, `AndroidRuntime` e `FATAL EXCEPTION`.

## Criterios de aceite

- Todo usuario autenticado abre diretamente a tela atual de Pedidos.
- Nao existe selecao, fallback ou persistencia de modo no app.
- Nao existe Home simplificada, completa, checkout direto ou outra superficie alternativa.
- A barra inferior e as rotas antigas inacessiveis nao fazem parte da Home.
- Novo pedido, detalhes, recebimento, simulacao, atualizacao e logout continuam funcionando a partir de Pedidos.
- Para pedidos com saldo pendente, Detalhes exibe `Resumo financeiro` e um botao `Pagar` sem valor embutido.
- Todo novo pagamento inicia em `R$ 0,00`; o saldo pendente aparece apenas como dica e pode ser aplicado pelo atalho `Usar valor pendente`.
- `Pagar` fica desabilitado em zero e habilita somente para um valor digitado ou aplicado pelo atalho.
- A UI agrupa `isOnlinePayment=true` como `Pagamentos online` e distingue Pix de Transferencia Pix.
- Credito, Debito e Pix so aparecem no pedido depois de aprovados pelo PagBank e nunca possuem status `PENDING`.
- Credito Loja, Transferencia Pix e Dinheiro sao gravados ao confirmar, sem PagBank e sem tela de espera.
- `PaymentMethod.isOnlinePayment` e a unica propriedade de classificacao usada pelo app.
- Fora desse ajuste explicito de entrada de pagamento, o comportamento visual e as regras de negocio das telas atuais permanecem inalterados.
- Build, testes relevantes, instalacao e verificacao de logs concluem sem falhas introduzidas pela migracao.
