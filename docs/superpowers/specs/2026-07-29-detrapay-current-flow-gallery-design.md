# Galeria HTML Detrapay — fluxos atuais

## Objetivo

Corrigir a galeria HTML para aplicar a identidade visual do Figma `4569:1291` aos fluxos realmente alcançáveis no aplicativo Android atual. A galeria serve como etapa de aprovação visual antes de qualquer alteração no Android.

## Fontes de verdade

- Aparência e componentes: Figma Detrapay Copy, página `4569:1291` e protótipo full-screen `4569:1604`.
- Navegação e comportamento: código Android atual, especialmente `home_navigation.xml`, `registration_navigation.xml`, `OrdersScreen.kt`, `OrdersRoute.kt`, `OrderFlowContract.kt` e `OrderFlowReducer.kt`.
- Dimensão de cada preview: `390 × 844`.

Quando Figma e Android divergirem, o Figma define a aparência e o Android define sequência, conteúdo, ações e estados.

## Escopo

### Jornada compartilhada

1. Splash
2. Login padrão
3. Login carregando
4. Login com erro
5. Pedidos carregando
6. Pedidos carregados
7. Pedidos vazios
8. Erro ao carregar pedidos

Usuário autenticado pode sair da Splash diretamente para Pedidos. Usuário não autenticado passa por Login. A antiga seleção de vendedor não integra a navegação atual e será removida da galeria.

### Jornada 1 — criar novo pedido

1. Pedidos carregados
2. Dados do pedido
3. Simulação dos valores carregando
4. Resumo/detalhamento
5. Diálogo de desconto
6. Criação do pedido carregando
7. Detalhes do pedido criado
8. Retorno para Pedidos

O caminho principal cria o pedido sem pagamento a partir do resumo. As telas legadas de seleção de pagamentos dentro do cadastro não são alcançadas pelo fluxo atual e não entram na jornada principal.

O diálogo “Sair sem salvar” será mantido como estado alternativo de Dados do pedido e Resumo.

### Jornada 2 — pagar pedido existente

1. Pedidos carregados
2. Detalhes do pedido
3. Seleção do método
4. Digitação do valor
5. Consulta de taxas carregando
6. Seleção de parcelas, somente para crédito
7. Revisão do pagamento
8. Processamento/aguardando confirmação
9. Pagamento aprovado
10. Falha de pagamento com opção de tentar novamente
11. Retorno para Pedidos e atualização da lista

Pix, débito e métodos sem taxa seguem de Valor diretamente para Revisão. Crédito passa por Parcelas. Métodos online usam a tela de espera; pagamentos apenas registrados retornam após a confirmação da revisão.

### Jornada 3 — simulador lateral

1. Pedidos carregados
2. Simulador sem valor
3. Consulta carregando
4. Parcelas carregadas
5. Nenhuma parcela disponível
6. Erro de consulta
7. Seleção de parcela
8. Ações de copiar e compartilhar
9. Fechar e retornar a Pedidos

O simulador é uma sobreposição do fluxo de Pedidos e não altera o pedido selecionado nem o checkout em andamento.

## Organização da galeria

A galeria terá dois modos:

- **Todas as telas:** catálogo completo, filtrável por Acesso, Pedidos, Novo pedido, Pagamento, Simulador e Diálogos.
- **Fluxo navegável:** seletor de jornada e navegação pelas ações primárias reais.

Telas compartilhadas aparecem uma vez no catálogo. Cada jornada referencia essas telas pelo identificador, evitando cópias visualmente divergentes.

## Componentes visuais

Serão preservados:

- azul Detrapay e faixa amarela;
- tipografia Inter do arquivo;
- campos sublinhados;
- cartões azul-claro;
- botões retangulares;
- cabeçalho com usuário e concessionária;
- estados verde, vermelho, cinza e amarelo equivalentes aos componentes do Figma;
- ilustrações e logotipo extraídos das referências do próprio Figma.

Componentes novos necessários ao fluxo atual — teclado numérico, escolha de método, parcelas, revisão, espera e simulador — usarão os mesmos tokens, espaçamentos e hierarquia visual.

## Modelo de navegação HTML

O catálogo declarará:

- identificador estável;
- jornada;
- estado;
- título e rótulo de revisão;
- renderer;
- destinos por ação.

A navegação deixará de depender de uma única lista linear. Cada tela poderá ter ações nomeadas, por exemplo `new-order`, `open-detail`, `pay`, `select-credit`, `continue`, `retry`, `finish` e `close-simulator`. Isso permite representar bifurcações reais sem duplicar telas.

## Estados e erros

- Loading, vazio e erro serão telas ou estados explícitos, nunca apenas mensagens escondidas.
- Erros de consulta de taxas mantêm o usuário em Valor ou no Simulador.
- Falha de pagamento mantém a opção de tentar novamente ou voltar à revisão.
- Fechar o cadastro apresenta confirmação antes de abandonar os dados.
- Nenhum contrato inexistente do backend será inventado; a galeria usará apenas dados representativos.

## Validação

- Testes de catálogo verificam todos os identificadores, jornadas e destinos.
- Testes de navegação percorrem integralmente as duas jornadas e o simulador.
- Testes no Chrome confirmam `390 × 844`, ausência de corte e funcionamento dos filtros.
- Capturas serão regeneradas para cada tela e para a visão geral.
- Uma revisão visual independente comparará componentes compartilhados com as referências do Figma.
- Nenhum arquivo em `app/` será modificado ou compilado nesta etapa.

## Critérios de aceite

- Login leva diretamente a Pedidos.
- Seleção de vendedor não aparece no fluxo.
- Novo pedido termina em Detalhes sem obrigar pagamento durante o cadastro.
- Pagamento de pedido existente respeita Método → Valor → Parcelas condicionais → Revisão → Espera/resultado.
- Simulador é acessível a partir de Pedidos e retorna sem alterar o checkout.
- Todos os estados relevantes podem ser vistos e filtrados.
- A aparência permanece consistente com o Figma aprovado.
