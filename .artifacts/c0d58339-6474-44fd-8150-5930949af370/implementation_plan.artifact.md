# Plano de Implementação - Envio de E-mail de Recuperação com JavaMail

Integrar a biblioteca JavaMail no projeto para que, ao solicitar a recuperação de senha, o aplicativo gere um código de verificação de 6 dígitos e envie um e-mail real utilizando a conta do Gmail `sigec.teste@gmail.com` com a senha de aplicativo `heplqzyxmmklopbh`.

## Revisão Necessária pelo Usuário

> [!IMPORTANT]
> - Adicionar dependências do JavaMail (`android-mail` e `android-activation`) em `build.gradle.kts`.
> - Configurar o envio SMTP do Gmail com a conta `sigec.teste@gmail.com` e a senha de aplicativo fornecida (`heplqzyxmmklopbh`).
> - Gerar código aleatório de 6 dígitos e enviá-lo por e-mail para o usuário.

## Questões em Aberto

- Nenhuma. Credenciais e remetente definidos (`sigec.teste@gmail.com` / `heplqzyxmmklopbh`).

## Alterações Propostas

### app

#### [MODIFY] [build.gradle.kts](file:///C:/Users/carlos57656326/AndroidStudioProjects/mobileSIGEC/app/build.gradle.kts)
- Adicionar dependências do JavaMail:
  ```kotlin
  implementation("com.sun.mail:android-mail:1.6.7")
  implementation("com.sun.mail:android-activation:1.6.7")
  ```

---

### app/src/main/java/com/example/mobilesigec

#### [MODIFY] [ForgotPasswordActivity.java](file:///C:/Users/carlos57656326/AndroidStudioProjects/mobileSIGEC/app/src/main/java/com/example/mobilesigec/ForgotPasswordActivity.java)
- Ao validar que o e-mail existe na base de dados:
  - Gerar um código numérico aleatório de 6 dígitos (ex: `123456`).
  - Conectar ao servidor SMTP do Gmail (`smtp.gmail.com:465` com SSL/TLS).
  - Autenticar com `sigec.teste@gmail.com` e `heplqzyxmmklopbh`.
  - Enviar mensagem contendo o código de recuperação para o e-mail do usuário.
  - Exibir mensagem de sucesso ("Código enviado para o seu e-mail").

---

### app/src/main/res/values

#### [MODIFY] [strings.xml](file:///C:/Users/carlos57656326/AndroidStudioProjects/mobileSIGEC/app/src/main/res/values/strings.xml)
- Adicionar strings auxiliares para mensagens de sucesso de envio de e-mail e erros de envio.

## Plano de Verificação

### Testes Automatizados
- Executar sincronização do Gradle e compilar projeto (`app:assembleDebug`).

### Verificação Manual
- Executar o aplicativo.
- Ir em "Esqueci minha senha".
- Digitar um e-mail válido cadastrado no banco de dados.
- Verificar se o e-mail real é enviado pela conta `sigec.teste@gmail.com` contendo o código de recuperação.
