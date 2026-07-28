# Payment Flow UX Hardening Design

## Context

O Detrapay é operado por vendedores e atendentes em uma SmartPOS diante do cliente. O fluxo atual já separa método, valor, parcelamento, revisão e processamento, além de mostrar valor original, juros, parcelas e total. A revisão UX identificou cinco grupos de problemas: baixa densidade na lista, sinais visuais ambíguos, entrada monetária inconsistente, interrupção transacional insegura e lacunas de acessibilidade.

Esta evolução preserva a arquitetura Compose, o wizard atual, os contratos de backend e a integração PlugPag. Não haverá reconstrução da navegação nem alteração do cálculo financeiro do backend.

## Goals

- Aumentar a quantidade de pedidos legíveis por viewport sem perder hierarquia.
- Tornar ações e estados compreensíveis sem depender de ícones ou cor.
- Garantir entrada monetária consistente em centavos e adequada à SmartPOS.
- Evitar saída ambígua durante uma transação em andamento.
- Exibir um resultado transacional persistente antes do retorno à lista.
- Atender WCAG AA nos textos e estados críticos.
- Isolar corretamente o simulador como superfície modal.

## Non-goals

- Alterar endpoints, regras de juros ou o contrato de parcelas.
- Alterar o valor final enviado ao PlugPag.
- Redesenhar cadastro, autenticação ou telas fora de pedidos e pagamentos.
- Adicionar bibliotecas externas de UI ou navegação.
- Criar recibo fiscal, impressão ou compartilhamento de comprovante.

## Chosen Approach

Será usada uma evolução incremental segura. O estado e os reducers existentes continuam sendo a fonte de verdade. Mudanças de apresentação ficam nos composables; normalização e validação monetária ficam em funções puras testáveis; transições de processamento e resultado ficam no reducer/route.

## UX Design

### 1. Lista de pedidos

Cada pedido deve priorizar apenas:

1. Cliente e número do pedido.
2. Total e saldo pendente ou estado quitado.
3. Ação contextual de pagar ou visualizar.

“Quitado” não será repetido em badge, métrica e progresso ao mesmo tempo. Cartões terão paddings e tipografia reduzidos, mantendo alvos de toque efetivos de pelo menos 48 dp. A ação primária “Novo pedido” ficará rotulada. “Simular parcelas” continuará disponível como ação secundária, sem competir visualmente com a criação de pedido.

### 2. Seleção do método

Nenhum método deve parecer selecionado antes do toque. Crédito não terá borda especial por estar disponível. Métodos indisponíveis serão ocultados quando não houver informação útil; quando precisarem permanecer visíveis, mostrarão texto curto “Indisponível” e motivo acessível.

Os grupos usarão linguagem operacional:

- “Cobrar na maquininha” para Crédito, Débito e Pix online.
- “Apenas registrar no pedido” para Dinheiro, Crédito Loja e Transferência Pix.

O texto do débito não prometerá “com taxa” sem explicar o impacto. A revisão financeira continuará sendo a fonte definitiva do total.

### 3. Entrada monetária

O modelo de entrada será sempre uma sequência de dígitos representando centavos:

- `""` representa R$ 0,00.
- `"1"` representa R$ 0,01.
- `"1234"` representa R$ 12,34.
- A tecla de separador decimal será removida do teclado interno.
- Delete removerá um dígito.
- O limite permanecerá em dez dígitos.

O simulador filtrará caracteres não numéricos e usará `KeyboardType.Number`. O prefixo `R$` será apresentado uma única vez pelo valor formatado, sem `leadingIcon` duplicado. Ao consultar parcelas, o foco será limpo e o teclado ocultado.

Antes de avançar, o valor deve ser maior que zero e não pode exceder o saldo pendente. Se exceder, a tela mostrará mensagem inline: “O valor não pode ser maior que o saldo pendente de R$ X.”

### 4. Simulador de parcelas

O simulador será uma superfície modal de tela cheia com semântica própria, bloqueando foco e interação com a lista subjacente. Haverá apenas uma ação de saída no topo.

Cada opção continuará exibindo:

- Valor original.
- Total com juros.
- Valor e quantidade das parcelas com juros.

A seleção de uma parcela terá consequência explícita: Copiar e WhatsApp compartilharão apenas a opção selecionada. Antes da seleção, essas ações ficarão indisponíveis e a tela instruirá “Selecione uma opção para compartilhar”. Não haverá pré-seleção automática da última parcela.

### 5. Processamento e resultado

Durante `UIState.Loading`, o retorno comum não poderá abortar silenciosamente. Ao tocar Voltar, o usuário verá confirmação:

- Título: “Cancelar pagamento?”
- Corpo: “A cobrança pode estar em andamento na maquininha. Cancele somente se o atendimento não puder continuar.”
- Primária destrutiva: “Cancelar pagamento”.
- Secundária: “Continuar aguardando”.

Quando o SDK não puder garantir cancelamento imediato, a cópia não afirmará que a cobrança foi cancelada; o estado retornará como erro/pendência conforme o resultado real.

Após sucesso online, o fluxo não voltará imediatamente para a lista. Uma tela persistente exibirá:

- “Pagamento aprovado”.
- Valor total cobrado.
- Método e parcelamento.
- Identificador da transação quando disponível.
- Ação “Voltar para pedidos”.

Pix pendente continuará exibindo QR Code e código copia e cola, mas a saída será rotulada “Voltar e acompanhar nos pedidos”.

### 6. Acessibilidade

- Texto normal deve atingir contraste mínimo 4,5:1; texto grande, 3:1.
- `Faint` não será usado em textos pequenos informativos.
- Branco sobre `WhatsappGreen` será substituído por combinação AA.
- Seleções declararão `Role.RadioButton` ou semântica equivalente, `selected` e descrição completa.
- Loading, erro e sucesso usarão regiões de anúncio apropriadas.
- O modal do simulador ocultará os descendentes da tela de fundo da árvore de acessibilidade.
- Ícones decorativos continuarão sem descrição; QR Code terá descrição funcional.
- A animação de espera será removida ou substituída por feedback estático, evitando movimento decorativo contínuo.
- Alvos efetivos permanecerão com pelo menos 48 dp.

## Data and State Flow

### Monetary input

`OrderPresentation` continuará concentrando conversão e formatação. `nextPaymentDigits` aceitará somente `"0"` a `"9"` e `"DEL"`. O reducer limpará cotações, seleção e erro quando o valor mudar.

### Simulator selection

`simulatorLoaded` deixará `simulatorSelectedInstallment = null`. A apresentação de compartilhamento receberá apenas a parcela selecionada. O reducer continuará invalidando resultados quando o valor mudar.

### Payment processing

O estado local ganhará controle explícito para a confirmação de cancelamento e para o resultado aprovado. A rota não chamará `abortPayment()` diretamente no primeiro toque em Voltar durante loading. Sucesso preservará os dados necessários até o usuário confirmar o retorno à lista.

## Error Handling

- Entrada vazia ou zero: “Informe um valor maior que zero.”
- Acima do saldo: mensagem inline com o saldo formatado.
- Falha de consulta de parcelas: preservar valor e permitir nova consulta.
- Método indisponível: mensagem acentuada e ação de retorno ao método.
- Falha no pagamento: preservar pedido, método, valor e parcela para nova tentativa segura.
- Cancelamento inconclusivo: não apresentar sucesso de cancelamento; orientar conferência na maquininha.

Nenhum workaround será criado para dados ausentes do backend. Se identificador ou estado confiável não forem fornecidos, a UI omitirá o campo e a necessidade de contrato será documentada.

## Testing

### Unit tests

- Entrada de centavos aceita apenas dígitos e delete.
- Separadores e caracteres alfabéticos são ignorados.
- Valor acima do saldo é rejeitado com mensagem formatada.
- Simulador não pré-seleciona parcela.
- Compartilhamento usa somente a parcela selecionada.
- Voltar durante loading abre confirmação sem chamar abort imediatamente.
- Sucesso mantém tela de resultado até ação do usuário.

### Compose/UI tests

- Crédito não começa selecionado.
- Simulador tem somente uma saída.
- Ações de compartilhar ficam desabilitadas sem seleção.
- Tela de revisão mantém original, juros, parcela e total.
- Resultado aprovado apresenta total e ação de retorno.
- Controles críticos expõem labels e estado de seleção.

### Device verification

No SmartPOS 720 × 1280:

- Medir densidade da lista e legibilidade.
- Confirmar teclado numérico e ausência de `R$` duplicado.
- Confirmar ocultação do teclado após consultar.
- Percorrer método, valor, parcelas, revisão, processamento, erro e resultado.
- Conferir árvore de acessibilidade com simulador aberto.
- Instalar, abrir o app e monitorar logs filtrados para `com.detrapay`, `AndroidRuntime` e `FATAL EXCEPTION`.

## Success Criteria

- Pelo menos três pedidos típicos visíveis por viewport no device alvo.
- Nenhuma opção parece selecionada antes da interação.
- Nenhum caminho aceita vírgula literal no modelo de centavos.
- Simulador usa teclado numérico e não expõe controles da tela de fundo.
- Voltar durante processamento exige decisão explícita.
- Aprovação permanece visível até ação do vendedor.
- Todos os pares de texto críticos atingem WCAG AA.
- Testes unitários, instrumentados aplicáveis e build debug passam.
- App instalado, aberto e sem `FATAL EXCEPTION` nos logs da execução atual.
