# Walkthrough - Renomeação para RecuperarSenhaEmail

Renomeação completa da tela de recuperação de senha para `RecuperarSenhaEmailActivity` e `activity_recuperar_senha_email.xml`.

## Alterações Realizadas

### Layout e Atividade Renomeados
- **[activity_recuperar_senha_email.xml](file:///C:/Users/carlos57656326/AndroidStudioProjects/mobileSIGEC/app/src/main/res/layout/activity_recuperar_senha_email.xml)**: Novo layout renomeado.
- **[RecuperarSenhaEmailActivity.java](file:///C:/Users/carlos57656326/AndroidStudioProjects/mobileSIGEC/app/src/main/java/com/example/mobilesigec/RecuperarSenhaEmailActivity.java)**: Nova Activity renomeada contendo a lógica de envio de e-mail via JavaMail com a conta `sigec.teste@gmail.com`.
- **[LoginActivity.java](file:///C:/Users/carlos57656326/AndroidStudioProjects/mobileSIGEC/app/src/main/java/com/example/mobilesigec/LoginActivity.java)**: Atualizado o Intent para abrir `RecuperarSenhaEmailActivity`.
- **[AndroidManifest.xml](file:///C:/Users/carlos57656326/AndroidStudioProjects/mobileSIGEC/app/src/main/AndroidManifest.xml)**: Registrada a nova activity.
- **Remoção de Arquivos Antigos**: `ForgotPasswordActivity.java` e `activity_forgot_password.xml` foram removidos.

## Resultados da Verificação

### Compilação
- Projeto compilado com sucesso (`app:assembleDebug` -> `BUILD SUCCESSFUL`).
