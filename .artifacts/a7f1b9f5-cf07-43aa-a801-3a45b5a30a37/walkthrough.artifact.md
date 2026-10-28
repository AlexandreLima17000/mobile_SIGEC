# Walkthrough - Fix Recipe Filtering in Checklist Screens

I have updated the recipe loading logic in both checklist fragments (`ChecklistInsumosFragment` and `ChecklistUtensiliosFragment`) to correctly filter recipes according to the requested business criteria.

## Changes

### Checklist Fragments

#### [ChecklistInsumosFragment.java](file:///C:/Users/jankledson59266826/AndroidStudioProjects/mobile_SIGEC/app/src/main/java/com/example/mobilesigec/checkout/ChecklistInsumosFragment.java) and [ChecklistUtensiliosFragment.java](file:///C:/Users/jankledson59266826/AndroidStudioProjects/mobile_SIGEC/app/src/main/java/com/example/mobilesigec/checkout/ChecklistUtensiliosFragment.java)
- Fixed the SharedPreferences key retrieval from `"ID_USUARIO"` to `"id_usuario"` to match what is saved during login (`LoginActivity`).
- Updated the SQL query to join the `agendamento` table (`a`) and applied filters:
  - `a.data = CURDATE()` (strictly today's date).
  - `ut.id_usuario = ?` (associated with the currently logged-in instructor).
  - `a.situacao = 'A'` and `t.situacao = 'A'` (active agendamento and turma).

```java
String sql = "SELECT f.id_ficha, f.nome_ficha, t.nome_turma, l.nome_laboratorio " +
        "FROM ficha f " +
        "INNER JOIN agendamento a ON f.id_ficha = a.id_ficha " +
        "INNER JOIN turma t ON f.id_turma = t.id_turma " +
        "INNER JOIN laboratorio l ON t.id_laboratorio = l.id_laboratorio " +
        "INNER JOIN usuario_turma ut ON t.id_turma = ut.id_turma " +
        "WHERE ut.id_usuario = ? AND t.situacao = 'A' AND a.data = CURDATE() AND a.situacao = 'A'";
```

## Verification Results

### Automated Tests
- Executed Gradle build (`app:assembleDebug`), which finished successfully with status `BUILD SUCCESSFUL`.
