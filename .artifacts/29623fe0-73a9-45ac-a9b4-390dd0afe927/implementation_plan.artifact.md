# Plano de Implementação - Refinamento Estético da Tela de Calendário (Full UI)

Este plano descreve os ajustes necessários para tornar a tela de calendário **exatamente** igual à imagem, incluindo a Toolbar, Bottom Navigation e detalhes refinados no Fragment.

## Mudanças Propostas

### 1. Temas e Cores

#### [MODIFY] [colors.xml](file:///C:/Users/rosicleia60022026/AndroidStudioProjects/mobile_SIGEC/app/src/main/res/values/colors.xml)
- Ajustar `sigec_dark_blue` para o tom exato do SENAC (#002F6C ou similar).
- Ajustar `sigec_orange` para o tom exato (#FF8C00).
- Adicionar `sigec_light_blue` para o texto da Toolbar (#7B92A8).

#### [MODIFY] [themes.xml](file:///C:/Users/rosicleia60022026/AndroidStudioProjects/mobile_SIGEC/app/src/main/res/values/themes.xml)
- Alterar `colorPrimary` para `sigec_white` (para Toolbar branca).
- Configurar o estilo da `BottomNavigationView` para usar as cores institucionais.

### 2. Barra Superior (Toolbar)

#### [NEW] [layout_custom_toolbar.xml](file:///C:/Users/rosicleia60022026/AndroidStudioProjects/mobile_SIGEC/app/src/main/res/layout/layout_custom_toolbar.xml)
Criar um layout customizado para a Toolbar contendo:
- "SENAC SIGEC" (Texto em azul escuro à esquerda).
- "Painel do Instrutor - Calendário" (Texto em laranja à direita, duas linhas).
- Ícone de menu vertical (três pontos) à extrema direita.

#### [MODIFY] [app_bar_main.xml](file:///C:/Users/rosicleia60022026/AndroidStudioProjects/mobile_SIGEC/app/src/main/res/layout/app_bar_main.xml)
- Configurar a Toolbar para remover o título padrão e incluir o layout customizado.
- Remover o FAB (Floating Action Button) que não aparece na imagem.

### 3. Navegação Inferior (Bottom Navigation)

#### [NEW] Ícones (Drawables)
- `ic_home_nav`: Ícone de casa (outline).
- `ic_calendar_nav`: Ícone de calendário (preenchido em laranja).
- `ic_book_nav`: Ícone de livro (outline).

#### [MODIFY] [bottom_navigation.xml](file:///C:/Users/rosicleia60022026/AndroidStudioProjects/mobile_SIGEC/app/src/main/res/menu/bottom_navigation.xml)
- Atualizar os IDs e ícones dos itens para corresponder à Home, Calendário e Livro.

#### [MODIFY] [content_main.xml](file:///C:/Users/rosicleia60022026/AndroidStudioProjects/mobile_SIGEC/app/src/main/res/layout/content_main.xml)
- Estilizar a `BottomNavigationView` com fundo azul escuro e seletores de cores adequados.

### 4. Refinamentos no Fragment de Calendário

#### [MODIFY] [fragment_calendario_blank.xml](file:///C:/Users/rosicleia60022026/AndroidStudioProjects/mobile_SIGEC/app/src/main/res/layout/fragment_calendario_blank.xml)
- **Borda Laranja**: Adicionar uma View fina na lateral esquerda do CardView principal para simular a borda laranja vista na imagem.
- **Marca D'água**: Adicionar um TextView grande ao fundo ("SIGEC") com opacidade baixa e cor azulada.
- **Fundo da Tela**: Garantir que o fundo do layout tenha o degradê ou a cor correta conforme a imagem.

## Plano de Verificação

### Testes Manuais
- Verificar o alinhamento dos textos na Toolbar.
- Validar se a cor do item selecionado na Bottom Navigation é laranja.
- Confirmar se a marca d'água e a borda lateral do CardView aparecem corretamente.
