# Implementation Plan - Fix Login Activity Warnings and Errors

The goal is to address hardcoded strings, inconsistent IDs, typos, and missing accessibility/autofill hints in `activity_login.xml`.

## Proposed Changes

### [app](file:///C:/Users/lucas59694586/AndroidStudioProjects/mobile_SIGEC/app)

#### [MODIFY] [strings.xml](file:///C:/Users/lucas59694586/AndroidStudioProjects/mobile_SIGEC/app/src/main/res/values/strings.xml)
- Add strings for the login screen:
    - `login_system_name`: "Sistema de Gerenciamento de Estoque da Cozinha"
    - `login_portal_instructor`: "PORTAL DO INSTRUTOR"
    - `login_welcome`: "Bem-vindo(a)"
    - `login_access_account`: "Acesse sua conta"
    - `login_email_hint`: "E-MAIL"
    - `login_password_hint`: "SENHA"
    - `login_forgot_password`: "Esqueci minha senha"
    - `login_button_enter`: "Entrar"
    - `login_logo_description`: "Logo SIGEC"

#### [MODIFY] [activity_login.xml](file:///C:/Users/lucas59694586/AndroidStudioProjects/mobile_SIGEC/app/src/main/res/layout/activity_login.xml)
- Replace hardcoded strings with resource references.
- Rename `@+id/et_cpf` to `@+id/et_email` to match the "E-MAIL" hint.
- Fix typo in `contentDescription` ("SICG" -> "SIGEC").
- Add `android:autofillHints="emailAddress"` to the email field.
- Add `android:autofillHints="password"` to the password field.

## Verification Plan

### Manual Verification
- Deploy the app and verify the login screen UI.
- Verify that the hints and text are correctly displayed from the resources.
- Check if autofill works for email and password fields.
