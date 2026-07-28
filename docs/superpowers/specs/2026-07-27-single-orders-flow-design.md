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
- Atualizar testes e referencias para refletir o fluxo unico.
- Validar compilacao, testes e execucao no device conectado.

### Fora de escopo

- Redesenhar as telas atuais.
- Alterar regras de negocio de pedidos ou pagamentos.
- Migrar toda a navegacao para uma Activity Compose unica.
- Criar compatibilidade visual ou comportamental com as Homes antigas.
- Implementar workaround para eventual ausencia de dados ou contratos do backend.

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

Os dados de pagamento, parcelas e pagamentos pendentes continuam usando os repositorios e ViewModels existentes. Renomeacoes nao alterarao endpoints, payloads ou contratos.

## Tratamento de erros

- Falhas ao carregar pedidos continuam exibindo o estado atual com opcao de recarregar.
- Falhas de simulacao, parcelas e pagamento continuam seguindo os efeitos e mensagens existentes.
- Sessao expirada continua encaminhando o usuario para o fluxo de autenticacao.
- Dados ou contratos ausentes no backend nao serao mascarados no app. O problema devera ser descrito objetivamente para ajuste do endpoint correspondente.
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
                       |      |      +--> Pix / manual
                       |      +---------> Debito
                       +----------------> Credito --> [Parcelas]
                                              |
                                              v
                                      [Aguardando/resultado]
                                              |
                                              +--> volta para Pedidos
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
| Pedido #544           |  |                       |
| Valor pendente:       |  | [ Credito           ] |
| R$ 2.570,18           |  | [ Debito            ] |
| [Usar valor pendente] |  | [ Pix               ] |
| [1] [2] [3]           |  |                       |
| [4] [5] [6]           |  |                       |
| [7] [8] [9]           |  |                       |
| [,] [0] [apagar]      |  | Outras formas        |
|                       |  | [Pix] [Loja] [Dinheiro]|
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
- Novo pedido: abrir o menu `+`, entrar em `RegistrationActivity`, concluir ou cancelar e retornar para a lista canonica.
- Simulacao: abrir o menu `+`, consultar parcelas para `R$ 2.570,18`, selecionar uma opcao e testar copiar/compartilhar.
- Busca: pesquisar por numero, cliente ou CPF/CNPJ e limpar o filtro sem alterar os dados carregados.

## Testes e verificacao

- Testes unitarios das regras de apresentacao de pedidos e roteamento de pagamentos continuarao cobrindo o comportamento atual com nomes atualizados.
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
- Fora desse ajuste explicito de entrada de pagamento, o comportamento visual e as regras de negocio das telas atuais permanecem inalterados.
- Build, testes relevantes, instalacao e verificacao de logs concluem sem falhas introduzidas pela migracao.
