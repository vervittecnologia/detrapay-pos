# Revisão de interface · DetraPay POS · 24/09/2026

## Escopo e evidência

Foram lidos os fluxos ativos de Splash, login, pedidos, detalhe, documentos, escolha de pagamento, valor, parcelas, revisão, processamento, cadastro, simulador, perfil e relatório no checkout atual. No GPOS780 conectado, foram observadas a lista de pedidos, o detalhe do pedido #589 e a tela apresentada após tocar em **Receber pagamento**. O protótipo é uma proposta de UX; ele não substitui o APK.

O terminal está em retrato, Android 11, 720 × 1280 px e 272 dpi configurados. A área útil do app medida foi [0,41]–[720,1198] px, aproximadamente 424 × 681 dp. Esta dimensão limita quantos cartões, campos e ações cabem sem rolagem.

## Achados e ajustes propostos

| Prioridade | Observação | Ajuste proposto |
| --- | --- | --- |
| Alta | O usuário aprovou o formato atual dos cartões de pedido e prefere o texto em caixa alta. | Preservar número, data, situação, cliente e três métricas; retirar o rótulo **PAGAMENTO** e mostrar uma barra de recebido ÷ total abaixo do nome. O status permanece no topo. |
| Média | A busca na lista fica oculta até tocar no ícone. | Preservar o acionamento atual da busca; avaliar mudanças de busca em uma tarefa separada. |
| Alta | No detalhe instalado, o valor total domina a primeira área visual, embora a próxima ação seja receber o saldo. | Mostrar o saldo a receber como número principal, mantendo total e recebido juntos logo abaixo. |
| Alta | Ao tocar em **Receber pagamento** no APK instalado, apareceu **Dados do pedido** com campos preenchidos. No checkout, `OrderPay` chama `OrderFlowReducer.startPayment`, que vai a `Method`. O APK informa versão 1.5.1/code 7, iguais aos valores do build atual; essa numeração não comprova que contenha as alterações locais. | Antes de implementar a proposta no app, validar uma build identificável e o caminho real a partir desse botão. Não inferir a causa apenas pela versão. |
| Média | O fluxo de pagamento já distingue meios no terminal de registros manuais. O processamento já usa estados específicos para cartão e Pix. | Preservar essa distinção. Na revisão, explicitar o efeito da confirmação e, no resultado, diferenciar aprovação da maquininha, registro manual e ausência de confirmação. |
| Média | `RegistrationActivity` usa `imePadding()` no Compose, enquanto o manifesto declara `windowSoftInputMode="adjustPan"`. Em formulário longo, a combinação merece verificação no GPOS780 quando o teclado abre. | Testar todos os campos com teclado visível e ajustar insets somente se houver conteúdo ou botão encoberto. |
| Média | Há uma fonte e esquema Material 3 centrais, mas várias telas definem paletas, fontes e estilos locais. Isso dificulta consistência de espaçamento, botões, foco e estados. | Consolidar tokens visuais no tema atual ao implementar as telas aprovadas. |
| Baixa | O relatório continua como Activity separada, e a tela de seleção de funcionário não consta no manifesto atual. | Confirmar acessibilidade real do relatório e remover ou reintegrar código legado em uma tarefa própria. |

## Decisões do protótipo

- O fluxo principal permanece orientado a pedidos; os cartões da lista mantêm o formato atual, conforme preferência do usuário.
- O botão principal fica no rodapé de etapas com decisão. As teclas de valor e alvos de toque têm pelo menos cerca de 48 dp.
- Cobrança e cadastro mostram uma barra de progresso por etapa; enquanto o terminal responde, a barra é indeterminada, pois o fluxo não fornece porcentagem real.
- Condições de parcelas e pagamentos simulados são explícitos. Saldo local muda apenas após aprovação fictícia ou registro manual fictício; recusa e resultado desconhecido mantêm o saldo.
- O cadastro preserva rascunho ao voltar. Fotos, impressão e login são apenas visuais no protótipo.

## Limites

O APK observado e o checkout atual são estados distintos. Não houve instalação, build Android nem alteração de código do app. O protótipo foi validado por execução de fluxos no DOM local; não houve conferência visual automatizada em navegador, porque a política do navegador bloqueou a abertura de `file://` neste ambiente.
