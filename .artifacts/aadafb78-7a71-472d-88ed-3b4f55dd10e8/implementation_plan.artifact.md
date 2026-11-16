# Reorganize Package Structure and Update AndroidManifest

Refactor the project package structure by moving loose classes into appropriate packages (`auth`, `database`, `calendario`, `receita`) and updating references in `AndroidManifest.xml` and Java code.

## User Review Required

> [!IMPORTANT]
> Moving classes into subpackages (`.auth`, `.database`, `.calendario`, `.receita`) requires updating the `package` declarations in the moved Java files, updating `AndroidManifest.xml` activity declarations, and adjusting any explicit `Intent` or fragment references across the app.

## Open Questions

- None. The package structure aligns with the user's instructions.

## Proposed Changes

### Package Structure Refactoring

#### [MODIFY] [ConexaoMySQL.java](file:///C:/Users/carlos57656326/AndroidStudioProjects/mobileSIGEC/app/src/main/java/com/example/mobilesigec/database/ConexaoMySQL.java) (Move to `com.example.mobilesigec.database`)
- Update package declaration to `package com.example.mobilesigec.database;`

#### [MODIFY] [LoginActivity.java](file:///C:/Users/carlos57656326/AndroidStudioProjects/mobileSIGEC/app/src/main/java/com/example/mobilesigec/auth/LoginActivity.java) (Move to `com.example.mobilesigec.auth`)
- Update package declaration to `package com.example.mobilesigec.auth;`
- Update imports (e.g. `ConexaoMySQL`, `MainActivity`, `RecuperarSenhaEmailActivity`)

#### [MODIFY] [ConfirmacaoCodigoActivity.java](file:///C:/Users/carlos57656326/AndroidStudioProjects/mobileSIGEC/app/src/main/java/com/example/mobilesigec/auth/ConfirmacaoCodigoActivity.java) (Move to `com.example.mobilesigec.auth`)
- Update package declaration to `package com.example.mobilesigec.auth;`

#### [MODIFY] [RecuperarSenhaEmailActivity.java](file:///C:/Users/carlos57656326/AndroidStudioProjects/mobileSIGEC/app/src/main/java/com/example/mobilesigec/auth/RecuperarSenhaEmailActivity.java) (Move to `com.example.mobilesigec.auth`)
- Update package declaration to `package com.example.mobilesigec.auth;`

#### [MODIFY] [RedefinirSenhaActivity.java](file:///C:/Users/carlos57656326/AndroidStudioProjects/mobileSIGEC/app/src/main/java/com/example/mobilesigec/auth/RedefinirSenhaActivity.java) (Move to `com.example.mobilesigec.auth`)
- Update package declaration to `package com.example.mobilesigec.auth;`

#### [MODIFY] [CalendarioBlankFragment.java](file:///C:/Users/carlos57656326/AndroidStudioProjects/mobileSIGEC/app/src/main/java/com/example/mobilesigec/calendario/CalendarioBlankFragment.java) (Move to `com.example.mobilesigec.calendario`)
- Update package declaration to `package com.example.mobilesigec.calendario;`

#### [MODIFY] [ReceitasFragment.java](file:///C:/Users/carlos57656326/AndroidStudioProjects/mobileSIGEC/app/src/main/java/com/example/mobilesigec/receita/ReceitasFragment.java) (Move to `com.example.mobilesigec.receita`)
- Update package declaration to `package com.example.mobilesigec.receita;`

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/carlos57656326/AndroidStudioProjects/mobileSIGEC/app/src/main/AndroidManifest.xml)
- Update activity names to include `.auth.` prefix:
  - `.LoginActivity` -> `.auth.LoginActivity`
  - `.RecuperarSenhaEmailActivity` -> `.auth.RecuperarSenhaEmailActivity`
  - `.ConfirmacaoCodigoActivity` -> `.auth.ConfirmacaoCodigoActivity`
  - `.RedefinirSenhaActivity` -> `.auth.RedefinirSenhaActivity`

## Verification Plan

### Automated Tests
- Run Gradle build (`gradle_build("app:assembleDebug")`) to verify compilation and package resolution.

### Manual Verification
- Deploy app to emulator/device or verify all activities and fragments load correctly.
