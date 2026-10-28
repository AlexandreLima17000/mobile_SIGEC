# Adicionar Cards de Receitas por Dia da Semana na Segunda Página (Calendário)

Add weekday accordion cards (matching the first photo / Home Page implementation) to the second page (`CalendarioFragment`), displaying recipes according to the day of the week for the current week only (excluding past weeks).

## User Review Required

> [!IMPORTANT]
> This change updates `CalendarioFragment` (the second tab in bottom navigation) to feature the weekly accordion recipe cards grouped by weekday (Monday to Friday), filtering out past weeks and showing only recipes for the current week, matching the styling and behavior seen on the Home page.

## Proposed Changes

### Calendario Component

#### [MODIFY] [fragment_calendario.xml](file:///C:/Users/ana57393876/StudioProjects/mobile_SIGEC/app/src/main/res/layout/fragment_calendario.xml)
- Update layout to include the "Receitas da Semana" section with a container (`container_receitas`) for weekday accordion cards, similar to `fragment_home_page.xml`.

#### [MODIFY] [CalendarioFragment.java](file:///C:/Users/ana57393876/StudioProjects/mobile_SIGEC/app/src/main/java/com/example/mobilesigec/calendario/CalendarioFragment.java)
- Adapt recipe loading and week filtering logic (from `HomePageFragment.java`) to load recipes for the selected turma for the current week (Monday to Friday), filter out past weeks, and populate the weekday accordion cards (`item_dia_semana.xml` and `item_receita_semana.xml`).

## Verification Plan

### Automated Tests
- Build project using Gradle to ensure no compilation errors.

### Manual Verification
- Deploy app to Android device/emulator.
- Navigate to the second page (Calendário).
- Verify that weekday accordion cards (Segunda a Sexta) appear with recipes for the current week only, excluding past weeks.
