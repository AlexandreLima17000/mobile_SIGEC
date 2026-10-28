# Implementation Plan - Fix Recipe Filtering in Checklist Screens

## Problem Statement
When navigating to the checklist screens (`ChecklistInsumosFragment` and `ChecklistUtensiliosFragment`) from the home page, the recipes loaded in the spinners are not filtered correctly according to the required business rules:
1. The recipe/agendamento date must be strictly equal to today (`CURDATE()`).
2. The recipe must be associated with the currently logged-in instructor (`id_usuario`).
Additionally, there is a mismatch in the SharedPreferences key used to retrieve the logged-in user ID (`"ID_USUARIO"` vs `"id_usuario"` stored by `LoginActivity`).

## Proposed Changes

### Checklist Fragments

#### [MODIFY] [ChecklistInsumosFragment.java](file:///C:/Users/jankledson59266826/AndroidStudioProjects/mobile_SIGEC/app/src/main/java/com/example/mobilesigec/checkout/ChecklistInsumosFragment.java)
- Correct SharedPreferences key from `"ID_USUARIO"` to `"id_usuario"`.
- Update SQL query in `carregarReceitasNoSpinner()` to join with `agendamento` and filter by today's date (`a.data = CURDATE()`) and logged-in instructor (`ut.id_usuario = ?`).

#### [MODIFY] [ChecklistUtensiliosFragment.java](file:///C:/Users/jankledson59266826/AndroidStudioProjects/mobile_SIGEC/app/src/main/java/com/example/mobilesigec/checkout/ChecklistUtensiliosFragment.java)
- Correct SharedPreferences key from `"ID_USUARIO"` to `"id_usuario"`.
- Update SQL query in `carregarReceitasNoSpinner()` to join with `agendamento` and filter by today's date (`a.data = CURDATE()`) and logged-in instructor (`ut.id_usuario = ?`).

## Verification Plan

### Automated Tests
- Run Gradle build (`app:assembleDebug`) to ensure clean compilation.

### Manual Verification
- Deploy to emulator/device, log in as an instructor, navigate to Insumos and Utensilios checklists, and verify that only today's recipes associated with the logged-in instructor appear in the dropdown.
