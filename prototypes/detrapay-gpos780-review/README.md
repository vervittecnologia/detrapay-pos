# DetraPay POS · proposta 04 para GPOS780

Abra [index.html](index.html) no navegador para percorrer o protótipo. Em computador, o menu lateral permite saltar para qualquer tela; dentro do terminal, os botões seguem os fluxos. Na largura de um telefone, o menu passa a ser uma faixa horizontal e o terminal ocupa a tela.

## Direção

Esta proposta evolui a interface atual. Ela mantém o azul, a tipografia Inter, o fluxo por pedido e a distinção entre cobrança na maquininha e registro manual. **Os cartões de pedidos preservam a disposição atual** de número, data, situação, cliente e três métricas; a tipografia foi refinada e todo o texto visível dos cartões aparece em caixa alta. Uma barra sem rótulo visível, abaixo do nome, mostra quanto do valor total já foi recebido. Os demais ajustes propostos concentram-se no detalhe e na revisão de pagamento.

Cadastro e cobrança exibem barra de progresso por etapa. O processamento no terminal usa barra indeterminada, sem porcentagem inventada.

## Dispositivo usado

Medição no GPOS780 conectado em 24/09/2026: tela física de 720 × 1280 px; densidade configurada em 272 dpi; escala de fonte 1,0; Android 11; retrato. A área de conteúdo observada vai de y=41 a y=1198 px. Assim, a simulação representa aproximadamente 423,53 × 752,94 dp no total, com 24,12 dp de barra superior, 680,58 dp de conteúdo e 48,24 dp de navegação inferior.

## Caminhos principais

1. **Pedidos → Pedido #589 → Receber → Crédito → Usar saldo → Parcelas → Revisar → Iniciar no terminal.** Use os controles **Simular aprovação**, **Simular recusa** ou **Simular ausência de confirmação** à direita do terminal.
2. **Pedidos → Novo pedido → Revisar pedido → Criar pedido fictício.** O rascunho fica preservado ao voltar.
3. **Simular** pelo menu lateral do protótipo. A simulação é independente da cobrança.
4. **Pedido → Documentos.** A câmera e as fotos são representações visuais, sem acesso ao hardware.

Os nomes, pedidos, valores, condições e resultados são fictícios. Nenhuma ação faz login, consulta backend, imprime, captura foto ou inicia transação.

## Verificação

`node --check app.js` verifica a sintaxe. `node verify.cjs` percorre os fluxos principais usando DOM local; o script usa o `happy-dom` já disponível em `../detrapay-material3/node_modules`. O arquivo HTML pode ser aberto sem servidor. A fonte Inter é carregada do recurso já presente no projeto em `app/src/main/res/font/inter.ttf`.

Veja [REVIEW.md](REVIEW.md) para o diagnóstico do app e as recomendações de implementação.
