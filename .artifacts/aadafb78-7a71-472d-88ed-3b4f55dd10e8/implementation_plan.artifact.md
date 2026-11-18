# Implement Asynchronous Thread Control in `CalendarioFragment`

Optimize calendar loading in `CalendarioFragment` by introducing dedicated thread control (`ExecutorService`, `Handler`, `AtomicInteger`) to handle database queries asynchronously, prevent ANRs, and protect against rapid selection race conditions.

## User Review Required

> [!IMPORTANT]
> This optimization ensures thread safety and responsiveness when switching classes in the calendar spinner, using an `AtomicInteger` request ID counter to discard outdated background queries.

## Open Questions

- None. The implementation follows the provided blueprint.

## Proposed Changes

### CalendarioFragment Optimization

#### [MODIFY] [CalendarioFragment.java](file:///C:/Users/carlos57656326/AndroidStudioProjects/mobileSIGEC/app/src/main/java/com/example/mobilesigec/ui/calendario/CalendarioFragment.java)
- Add thread control fields:
  - `private final ExecutorService executorCalendario = Executors.newSingleThreadExecutor();`
  - `private final Handler mainHandler = new Handler(Looper.getMainLooper());`
  - `private final AtomicInteger idRequisicaoAtual = new AtomicInteger(0);`
- Update spinner selection listener to trigger `carregarCalendarioPorTurmaAsync` (or integrate async loading logic with request ID validation).
- Implement `onDestroyView()` to shutdown `executorCalendario` and invalidate active requests (`idRequisicaoAtual.incrementAndGet()`).

## Verification Plan

### Automated Tests
- Run Gradle build (`gradle_build("app:assembleDebug")`) to verify syntax correctness and successful compilation.
