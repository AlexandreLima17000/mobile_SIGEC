# Walkthrough - Ajustes nas Receitas da Semana (Home Page) e Sincronização de Turmas

Implementado o filtro restrito para exibir na Home Page apenas as receitas agendadas especificamente para a **semana atual** (segunda a sexta), descartando receitas de semanas passadas, futuras ou sem data agendada. Além disso, foi sincronizada a seleção de turmas entre a Home e o Calendário.

## Changes

### Home Page Component

#### [HomePageFragment.java](file:///C:/Users/ana57393876/StudioProjects/mobile_SIGEC/app/src/main/java/com/example/mobilesigec/home/HomePageFragment.java)
- Atualizada a lógica em `carregarReceitasDaTurma` para garantir que apenas receitas com data pertencente à semana atual (`pertenceASemanaAtual == true` e `temData == true`) sejam exibidas nos cards dos dias da semana. Receitas de semanas passadas, futuras ou sem data são ignoradas.
- Sincronizada a seleção do Spinner de turmas utilizando `SharedPreferences` para manter consistência com o Calendário.

## Verification Results

### Automated Tests
- Executado `gradle_build(app:assembleDebug)` com sucesso (`Build finished successfully`).
