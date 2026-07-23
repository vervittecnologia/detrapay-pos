# Migração do Dashboard e Unificação da Home

Concluí a migração do Dashboard inicial e da listagem de pagamentos para Jetpack Compose, unificando a identidade visual do aplicativo.

## Alterações Realizadas

### Dashboard (Tela Inicial)
- **[HomeScreen.kt](file:///C:/Vervit/detray-android/app/src/main/java/com/detrapay/ui/home/registration/HomeScreen.kt)**: Nova implementação 100% Compose do Dashboard.
    - Cabeçalho moderno com logo e logout.
    - Cards de serviços com visual atualizado.
    - Seção de "Pedidos Recentes" integrada.
- **[RegistrationFragment.kt](file:///C:/Vervit/detray-android/app/src/main/java/com/detrapay/ui/home/registration/RegistrationFragment.kt)**: Agora atua apenas como um host para a `HomeScreen` em Compose.

### Pagamentos (Listagem Simplificada)
- **[SimplifiedReceivableScreen.kt](file:///C:/Vervit/detray-android/app/src/main/java/com/detrapay/ui/home/simplified/SimplifiedReceivableScreen.kt)**: Implementação da listagem de pagamentos pendentes em Compose.
- **[SimplifiedReceivableListFragment.kt](file:///C:/Vervit/detray-android/app/src/main/java/com/detrapay/ui/home/simplified/SimplifiedReceivableListFragment.kt)**: Migrado para Compose.

### Infraestrutura e Roteamento
- **[AuthRepository.kt](file:///C:/Vervit/detray-android/app/src/main/java/com/detrapay/data/repositories/AuthRepository.kt)**: Corrigido bug onde o `appMode` retornado pela API não era priorizado no login, impedindo que usuários em modo `direct_checkout` vissem as telas corretas.
- **[HomeActivity.kt](file:///C:/Vervit/detray-android/app/src/main/java/com/detrapay/ui/home/HomeActivity.kt)**: Melhorada a lógica de roteamento automático e visibilidade da barra de navegação inferior.
- **[build.gradle.kts](file:///C:/Vervit/detray-android/app/build.gradle.kts)**: Adicionada biblioteca `coil-compose` para carregamento eficiente de imagens.

## Verificação

### Evidências Visuais
![Novo Dashboard em Compose](/C:/Vervit/detray-android/.artifacts/e143e3c5-87a5-4e2c-bd74-8560817b06b5/scratch/dashboard_compose.png)

### Testes Realizados
1. Validei que o Dashboard agora exibe as informações do Operador e Pedidos Recentes usando Compose.
2. Verifiquei os logs e confirmei que a `HomeActivity` está identificando o `App Mode` corretamente.
3. Testei a navegação entre as abas e a persistência do estado.

> [!IMPORTANT]
> Se o aplicativo ainda estiver exibindo as telas antigas, por favor, realize **LOGOUT** e **LOGIN** novamente. Isso garantirá que a nova lógica de identificação de modo (`direct_checkout` vs `complete`) seja aplicada ao seu perfil de usuário.
