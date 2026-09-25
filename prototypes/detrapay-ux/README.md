# Protótipo de UX · Detrapay SmartPOS

Abra [index.html](index.html) no Chrome para navegar pelas telas. O arquivo funciona localmente e usa apenas HTML, CSS, JavaScript e a fonte Inter já incluída no projeto.

## Escopo

- Acesso e erro de login.
- Pedidos, busca, filtros, vazio, erro de carregamento, detalhe, documentação, relatório e perfil.
- Novo pedido: dados, revisão e confirmação de criação.
- Cobrança: método, valor, parcelas, revisão, processamento, aprovação e falha.
- Simulador de parcelas, separado da cobrança.

Os clientes, pedidos, referências, valores e taxas são **fictícios**. O protótipo não faz requisições, não autentica, não registra pedidos e não inicia cobranças. As ações mudam apenas o estado da página. Para testar a UX de pagamento, percorra **Pedidos → Pedido #568 → Cobrar saldo → Crédito → Valor → Parcelas → Revisão → Processamento → Resultado**.

## Direção proposta

1. Mostrar o saldo antes de iniciar uma cobrança.
2. Dar uma decisão principal a cada etapa.
3. Exibir valor original, juros, parcelas e total juntos antes da confirmação.
4. Diferenciar operação em andamento, aprovação confirmada e ausência de confirmação.
5. Preservar contexto e oferecer uma próxima ação clara em estados vazios ou de erro.

## Relação com o app

O fluxo foi mapeado a partir do código Compose atual e de uma captura da SmartPOS conectada em 22/09/2026. Este diretório não altera o APK, o backend ou os contratos de pagamento. A validação visual no Chrome precisa ser feita na máquina do usuário: a automação disponível bloqueou a abertura de arquivos `file://`.
