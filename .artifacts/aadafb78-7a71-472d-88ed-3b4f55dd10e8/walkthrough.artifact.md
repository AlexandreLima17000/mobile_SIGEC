# Walkthrough - Otimização Assíncrona e Controle de Concorrência no Calendário

Implementação de carregamento assíncrono e controle avançado de concorrência (`ExecutorService` + `AtomicInteger`) no `CalendarioFragment` para evitar travamentos (ANRs) ao alternar entre turmas e proteger contra respostas fora de ordem em cliques rápidos.

## Alterações Realizadas

### `CalendarioFragment.java`
- **Atributos de Thread & Concorrência**:
  - Adicionado `executorCalendario` (`ExecutorService`) para execuções em segundo plano.
  - Adicionado `idRequisicaoAtual` (`AtomicInteger`) para controle de versões de requisição.
- **Carregamento Assíncrono (`carregarCalendarioPorTurmaAsync`)**:
  - Consulta ao banco de dados MySQL realizada de forma assíncrona com feedback visual imediato ("Carregando calendário da turma...").
  - Verificação de validade da requisição atual (`requisicaoId == idRequisicaoAtual.get()`) para ignorar respostas obsoletas causadas por cliques rápidos em diferentes turmas.
- **Gerenciamento de Ciclo de Vida (`onDestroyView`)**:
  - Invalidação automática de requisições pendentes e encerramento seguro do executor (`executorCalendario.shutdownNow()`) para evitar vazamentos de memória (memory leaks).

## Verificação e Testes

### Testes Automatizados
- Executado o build do projeto (`app:assembleDebug`) via Gradle.
- **Resultado**: Build concluído com sucesso (`BUILD SUCCESSFUL`).
