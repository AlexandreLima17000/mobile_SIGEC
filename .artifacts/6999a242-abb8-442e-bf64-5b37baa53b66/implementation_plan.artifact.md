# Plano de Implementação - Tela de Splash com Vídeo

Este plano detalha os passos para implementar uma tela de Splash animada utilizando o vídeo `splash_sigec.mp4` localizado na pasta drawable, configurando-a como a tela inicial do projeto antes de redirecionar para a `LoginActivity`.

## Alterações Propostas

### 1. Organização de Recursos
- Mover ou referenciar o arquivo `splash_sigec.mp4` de forma adequada. No Android, vídeos devem ser colocados na pasta `res/raw/` para que possam ser reproduzidos corretamente pelo `VideoView`. Portanto, criaremos a pasta `app/src/main/res/raw/` e moveremos o vídeo para lá (ou usaremos a referência correta).

### 2. Nova Activity de Splash
- **[NEW] [SplashActivity.java](file:///C:/Users/carlos57656326/AndroidStudioProjects/mobileSIGEC/app/src/main/java/com/example/mobilesigec/SplashActivity.java)**: Criar a classe que conterá o `VideoView`, configurará o caminho do vídeo e tratará o redirecionamento para a `LoginActivity` assim que o vídeo terminar de rodar.
- **[NEW] [activity_splash.xml](file:///C:/Users/carlos57656326/AndroidStudioProjects/mobileSIGEC/app/src/main/res/layout/activity_splash.xml)**: Criar o layout da tela de Splash contendo um `VideoView` em tela cheia.

### 3. Configuração do Manifesto
- **[MODIFY] [AndroidManifest.xml](file:///C:/Users/carlos57656326/AndroidStudioProjects/mobileSIGEC/app/src/main/AndroidManifest.xml)**:
    - Registrar a `SplashActivity`.
    - Mover o `intent-filter` (LAUNCHER) da `LoginActivity` para a `SplashActivity`, tornando-a a nova tela inicial do aplicativo.

## Plano de Verificação

### Verificação Manual
- Inicializar o aplicativo e verificar se o vídeo de Splash é exibido em tela cheia imediatamente.
- Garantir que, ao encerrar o vídeo, o aplicativo mude automaticamente para a tela de Login (`LoginActivity`).
