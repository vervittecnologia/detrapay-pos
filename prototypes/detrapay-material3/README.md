# Detrapay — Proposta 02 / Material 3 / GPOS780

Abra `board.html` para comparar as telas e `index.html` para navegar pelos fluxos. O `index.html` é autônomo: Material 3, Roboto, estilos e lógica ficam incorporados no próprio arquivo para funcionar em navegadores que restringem recursos externos em URLs `file://`.

## Proposta 03 — Action POS

Abra `concept3-board.html` para ver as dez telas e `concept3.html` para navegar. Esta proposta começa por uma grade de ações de balcão e destaca somente um atendimento em andamento. A lista completa de pedidos fica em uma área separada com navegação inferior.

Referências pesquisadas em fontes oficiais:

- Square: checkout simplificado, navegação inferior e acesso rápido a ferramentas frequentes.
- Shopify POS: grade configurável de ações e meios de pagamento selecionados depois do carrinho.
- Lightspeed Retail: venda em andamento, cliente e pagamento preservados no mesmo contexto.

A proposta adapta esses padrões ao Detrapay sem copiar identidade visual, estrutura comercial ou componentes proprietários.

## Dispositivo medido

Medição via ADB em 22/09/2026, device conectado `0123abcd`:

- Modelo GPOS780, Android 11, orientação retrato.
- Tela física: 720 × 1280 px.
- Densidade física declarada: 320 dpi; densidade configurada e efetiva: 272 dpi.
- Escala de fonte: 1,0. Conversão do layout: 1 dp = 1,7 px físicos.
- Área estável do app: [0,41]–[720,1198] px, ou 423,53 × 680,59 dp.
- Status bar: 41 px / 24,12 dp. Navegação Android: 82 px / 48,24 dp.

O protótipo reproduz a tela inteira em 423,53 × 752,94 unidades de layout. A área do app desconta exatamente as barras medidas. A moldura é ilustrativa. O protótipo navegável usa zoom visual de 85%; a prancha usa 70%. Esses zooms só facilitam a visualização no computador, sem mudar as proporções dos componentes.

## Material 3

Biblioteca oficial `@material/web` 2.5.0, Roboto local, tokens de cor Material 3. Botões, campos, listas, chips, seletores, checkboxes, rádios e indicadores são componentes da biblioteca, não imitações em CSS. A estrutura de tela, top app bar e rodapé são layouts compostos com esses componentes e tokens. No Android, a correspondência de implementação é Jetpack Compose Material 3; este trabalho entrega apenas protótipos web.

Referências oficiais: [Material Web](https://material-web.dev/about/intro/), [Buttons](https://material-web.dev/components/button/), [Text fields](https://material-web.dev/components/text-field/), [Lists](https://material-web.dev/components/list/).

## Referência observada no PagVendas

PagVendas 2.75.0 instalado (`br.com.uol.ps.pagvendas.debug`). Foram visualizadas venda, perfil e solicitação de login ao abrir pedidos. A área de pedidos autenticada não foi revisada.

A tela de venda privilegia o valor, teclado amplo e botão de cobrar fixo. Os contêineres das teclas medidos eram de aproximadamente 237 × 148 px (139 × 87 dp); o botão de cobrar, 546 × 87 px (321 × 51 dp). A proposta usa teclas de 72 dp e botão principal de 56 dp, acomodando também a identificação do pedido e seu saldo. A referência é funcional; a identidade visual continua sendo do Detrapay com Material 3.

## Interações

- Busca e filtros de pedidos; detalhe acompanha o pedido escolhido.
- Crédito, débito, Pix e registros manuais; valor parcial; seleção de parcelas; revisão.
- Resultados controlados fora da tela do app. Aprovar atualiza o saldo apenas em memória.
- Criação de pedido em cliente, veículo e revisão; formulário preservado ao voltar.
- Simulação, documentação com falha de envio, relatório e perfil.

Todos os dados e taxas são fictícios. Métodos de pagamento reais dependem do contrato e da configuração recebidos pelo app. Não há autenticação, chamada ao backend, impressão, câmera, instalação ou cobrança real. O código Android não foi alterado por esta proposta.

## Verificação

`npm run check` verifica a sintaxe e a consistência dos fluxos de pagamento locais. `npm run build` recompila os componentes oficiais, copia as fontes e gera as propostas 02 e 03 como HTML autônomo. Ambas foram renderizadas no Chrome; a proposta 03 também foi conferida em uma prancha com as telas principais.
