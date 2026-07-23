# Migração da Home e Dashboard para Compose

Identificamos que o aplicativo ainda exibe telas em XML porque o Dashboard inicial (`RegistrationFragment`) ainda não foi migrado e havia um bug na persistência do modo de operação (`appMode`) no login.

## Mudanças Propostas

### 1. Correção da Persistência de Modo (Concluído)

#### [MODIFY] [AuthRepository.kt](file:///C:/Vervit/detray-android/app/src/main/java/com/detrapay/data/repositories/AuthRepository.kt)
- Corrigido `saveLoginSession` para priorizar o campo `appMode` da raiz da resposta da API, garantindo que o modo `direct_checkout` seja salvo corretamente.

---

### 2. Migração do Dashboard (RegistrationFragment) para Compose (Concluído)

#### [NEW] [HomeScreen.kt](file:///C:/Vervit/detray-android/app/src/main/java/com/detrapay/ui/home/registration/HomeScreen.kt)
- Implementado Dashboard moderno com suporte a temas e carregamento de imagens via Coil.

#### [MODIFY] [RegistrationFragment.kt](file:///C:/Vervit/detray-android/app/src/main/java/com/detrapay/ui/home/registration/RegistrationFragment.kt)
- Migrado para Compose, integrando os ViewModels existentes.

#### [MODIFY] [build.gradle.kts](file:///C:/Vervit/detray-android/app/build.gradle.kts) e [libs.versions.toml](file:///C:/Vervit/detray-android/gradle/libs.versions.toml)
- Adicionada dependência `coil-compose` para suporte a imagens.

---

## User Review Required

> [!IMPORTANT]
> Para aplicar a correção do modo `direct_checkout` no dispositivo atual, será necessário fazer **Logout** e **Login** novamente após as alterações.

## Plano de Verificação

### Testes Manuais
1. Compilar e rodar o app no emulador.
2. Fazer Logout e Login novamente (usando a conta de Direct Checkout).
3. Verificar se o Dashboard agora tem o visual moderno (Compose).
4. Clicar em "Novo Registro" e verificar se a transição ocorre.
