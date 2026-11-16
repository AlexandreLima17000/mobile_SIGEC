# Reorganize UI Packages Under `ui`

Refactor the package structure to move UI packages (`auth`, `calendario`, `checkout`, `home`, `receita`, `settings`) under a new parent package `ui` (`com.example.mobilesigec.ui.*`), keeping `adapter`, `database`, `model`, `MainActivity`, and `SplashActivity` at the root level.

## User Review Required

> [!IMPORTANT]
> Moving UI packages under `ui` requires updating package declarations and imports across all moved files, updating `AndroidManifest.xml` for activities, and updating `mobile_navigation.xml` for fragment fully qualified names.

## Open Questions

- None. The target structure matches the requested architecture.

## Proposed Changes

### UI Package Restructuring

#### [NEW / MODIFY] Package Move & Updates
- Move files in `auth`, `calendario`, `checkout`, `home`, `receita`, `settings` to `ui/auth`, `ui/calendario`, `ui/checkout`, `ui/home`, `ui/receita`, `ui/settings`.
- Update package declarations to `package com.example.mobilesigec.ui.<package_name>;`.
- Update imports in these files to reference root packages (`database.ConexaoMySQL`, `model.*`, `adapter.*`, etc.) and each other correctly.

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/carlos57656326/AndroidStudioProjects/mobileSIGEC/app/src/main/AndroidManifest.xml)
- Update activity references to include `.ui.auth.` prefix:
  - `.auth.LoginActivity` -> `.ui.auth.LoginActivity`
  - `.auth.RecuperarSenhaEmailActivity` -> `.ui.auth.RecuperarSenhaEmailActivity`
  - `.auth.ConfirmacaoCodigoActivity` -> `.ui.auth.ConfirmacaoCodigoActivity`
  - `.auth.RedefinirSenhaActivity` -> `.ui.auth.RedefinirSenhaActivity`

#### [MODIFY] [mobile_navigation.xml](file:///C:/Users/carlos57656326/AndroidStudioProjects/mobileSIGEC/app/src/main/res/navigation/mobile_navigation.xml)
- Update fragment references to include `ui.` package prefix:
  - `com.example.mobilesigec.checkout.*` -> `com.example.mobilesigec.ui.checkout.*`
  - `com.example.mobilesigec.receita.*` -> `com.example.mobilesigec.ui.receita.*`
  - `com.example.mobilesigec.home.*` -> `com.example.mobilesigec.ui.home.*`
  - `com.example.mobilesigec.calendario.*` -> `com.example.mobilesigec.ui.calendario.*`
  - `com.example.mobilesigec.settings.*` -> `com.example.mobilesigec.ui.settings.*`

## Verification Plan

### Automated Tests
- Run Gradle build (`gradle_build("app:assembleDebug")`) to verify successful compilation and package resolution.
