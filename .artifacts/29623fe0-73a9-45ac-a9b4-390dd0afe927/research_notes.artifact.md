# Análise de Erros de Design e Requisitos Funcionais

Após revisar a implementação atual comparada com a imagem de referência e as melhores práticas de desenvolvimento Android, identifiquei os seguintes pontos:

## 🎨 Erros de Design (Visual)

1.  **Estática da Seleção**: O dia 2 está permanentemente destacado com a borda laranja e o canto diagonal. Em um app funcional, esse destaque deveria mudar conforme o usuário toca em diferentes dias.
2.  **Marca d'Água (Watermark)**: A marca d'água "SIGEC" ao fundo está centralizada na parte inferior. Na imagem original, ela parece ser um pouco mais sutil e possivelmente integrada ao fundo do container do calendário, não apenas sobreposta.
3.  **Densidade de Informação**: O `GridLayout` usa `60dp` fixos para a altura dos dias. Dependendo do tamanho da tela do dispositivo, isso pode comprimir o calendário ou deixar muito espaço vazio. O ideal seria usar pesos (`layout_rowWeight`) se o container tivesse altura definida.
4.  **Acessibilidade**: Os textos nos cabeçalhos dos dias (DOM, SEG...) estão muito pequenos (`10sp`). O recomendado para legibilidade mínima é `12sp`.

## ⚙️ Erros de Requisitos Funcionais

1.  **Calendário "Congelado"**: A lógica em `populateCalendar` está "chumbada" para Setembro de 2026. As setas de navegação (próximo/anterior) não funcionam para mudar o mês.
2.  **Falta de Interatividade**:
    - Clicar em um dia não atualiza a seção inferior ("FICHAS ALOCADAS").
    - Clicar em um dia não muda o destaque visual (o dia selecionado).
3.  **Dados Hardcoded**: Os indicadores de aula (pontos laranja) estão inseridos manualmente no loop do Fragment. Em um cenário real, esses dados deveriam vir de um Banco de Dados ou API.
4.  **Barra de Busca Inativa**: O campo de busca é apenas um elemento visual; não filtra os eventos ou dias do calendário.
5.  **Rodapé (Bottom Navigation)**: Embora os ícones estejam corretos, a navegação entre as abas (Home, Calendário, Fichas) precisa ser configurada no `NavController` para garantir que o estado de cada fragmento seja preservado.

---

> [!TIP]
> **Próximos Passos Sugeridos**:
> - Implementar a lógica de clique nos dias para atualizar a data e o conteúdo inferior.
> - Tornar a navegação de meses funcional usando a classe `Calendar` do Java.
> - Criar um modelo de dados simples para "Aulas" para remover o código hardcoded.
