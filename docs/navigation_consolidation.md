# Consolidação de Navegação — Plano

Objetivo: reduzir complexidade do back stack e inconsistência entre `Activity` e `Fragment` destinations, adotando preferencialmente um fluxo Single-Activity/Fragments usando Navigation Component.

Resumo das ações propostas
- Migrar Activities que apenas hospedam Fragments para destinos `fragment` dentro dos nav graphs existentes.
- Uniformizar uso do `NavController` (preferir `findNavController()` em fragments) e remover intents internas que simulam navegação local.
- Consolidar nav graphs por domínio (ex.: `home_navigation.xml`, `registration_navigation.xml`) e evitar declarar `activity` destinations nesses graphs salvo casos explicitamente necessários.
- Padronizar animações e argumentos de navegação via `safe-args` (adicionar plugin se não estiver presente).

Mapeamento inicial (arquivos para revisão)
- Home flow
  - `HomeActivity` -> hospeda `NavHostFragment` e `BottomNavigation` ([app/src/main/java/com/detrapay/ui/home/HomeActivity.kt](app/src/main/java/com/detrapay/ui/home/HomeActivity.kt#L1-L200))
  - `home_navigation.xml` -> manter como nav graph principal ([app/src/main/res/navigation/home_navigation.xml](app/src/main/res/navigation/home_navigation.xml#L1-L120))
- Registration flow
  - `RegistrationActivity` -> atualmente um stepper que hospeda fragments; migrar para ser apenas um container `NavHostFragment` dentro do fluxo de `home_navigation` ou manter `registration_navigation.xml` como nested graph ([app/src/main/java/com/detrapay/ui/registration/RegistrationActivity.kt](app/src/main/java/com/detrapay/ui/registration/RegistrationActivity.kt#L1-L220))
  - `registration_navigation.xml` ([app/src/main/res/navigation/registration_navigation.xml](app/src/main/res/navigation/registration_navigation.xml#L1-L140))

Regras de decisão (quando migrar)
- Se a Activity apenas contém um `NavHostFragment` e toolbars/bottom nav, transformar a Activity em host único (single-activity) ou mantê-la como container do nav graph aninhado; preferir fragment destinations.
- Manter Activities separadas somente quando o fluxo exigir isolamento de processo/permissions/launchMode diferentes, ou integração com terceiros que exigem Activity (ex.: intents externas, SDKs que pedem Activity callbacks).

Passo-a-passo para uma migração segura (exemplo: `RegistrationActivity` → fragments)
1. Criar um branch/PR por fluxo (ex.: `feat/nav/registration-to-fragment`).
2. Identificar todos os fragments atualmente gerenciados por `RegistrationActivity` e listar: `RegistrationStartFragment`, `RegistrationPaymentMethodFragment`, `RegistrationResumeFragment`, etc.
3. Garantir que o `registration_navigation.xml` declare todos esses destinos como `fragment` e que os actions contenham argumentos tipados (`safe-args`).
4. Alterar `RegistrationActivity` para apenas hospedar `NavHostFragment` e transferir toda navegação para `NavController` invocado por fragments.
5. Atualizar chamadas que usavam `startActivity()` para navegar entre telas internas convertendo-as em `findNavController().navigate(...)`.
6. Atualizar e rodar testes manuais: iniciar fluxo de registro via Home → Registro → passos e validar back button e deep link behavior.
7. Criar PR com descrição clara das mudanças e screenshots/gif do fluxo.

PR Checklist (mínimo)
- [ ] Branch limitado ao fluxo e mudanças pequenas por PR
- [ ] Atualizar nav graph e remover `activity` destinations convertidos
- [ ] Atualizar `AndroidManifest.xml` removendo `intent-filter`/`activity` entries se Activity não for mais usada
- [ ] Verificar injeção Hilt em fragments (`@AndroidEntryPoint` quando necessário)
- [ ] Substituir `Activity`-scoped `ViewModels` por `navGraphViewModels` ou `hiltViewModel` conforme o escopo desejado
- [ ] Validar comportamento de `up`/back em toolbar e sistema
- [ ] Atualizar README/Docs se pontos de entrada mudarem

Considerações sobre ViewModel e escopo
- Para estados compartilhados entre fragments do mesmo fluxo, usar `navGraphViewModels` (KTX) ou `ViewModel` compartilhado no `NavHostFragment` como escopo.

Recomendações extras
- Habilitar `safe-args` plugin se não estiver presente para evitar erros de argumentos manuais.
- Remover destinos `activity` dos nav graphs e usar `navigation` nesting para flows isolados.
- Adicionar testes de UI básicos (Espresso) para fluxos críticos (checkout, pagamento).

Bloqueadores potenciais
- Uso de SDKs que exigem callbacks de `Activity` (ex.: alguns SDKs de pagamento)—esses casos podem forçar Activity separada.
- Dependências de AndroidManifest (launchMode, intent filters) que exigem Activity única.

Próximos passos propostos (curto prazo)
1. Revisar `HomeActivity` e `RegistrationActivity` e decidir migração para sample flow (eu posso abrir PR de exemplo se autorizar).
2. Conferir `AndroidManifest.xml` e listar Activities que podem ser removidas.
3. Criar PR piloto que converte `RegistrationActivity` em nav-graph-hosted fragments e atualiza `registration_navigation.xml`.

---
Gerado pelo plano de padronização de UI — referência rápida para os PRs de consolidação.
