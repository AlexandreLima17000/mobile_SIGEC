# Walkthrough - Abertura de Detalhes da Receita na Home Page

Adicionado um `onClickListener` aos cards de receitas da semana na Home Page para que, ao clicar em qualquer receita, o aplicativo abra diretamente a tela de detalhes da receita (`nav_detalhes_receita`) passando o `id_ficha` correto.

## Changes

### Home Page Component

#### [HomePageFragment.java](file:///C:/Users/ana57393876/StudioProjects/mobile_SIGEC/app/src/main/java/com/example/mobilesigec/home/HomePageFragment.java)
- Armazenado o `id_ficha` na classe auxiliar `ItemReceitaData`.
- Configurado o `setOnClickListener` em cada card de receita da semana para navegar para `R.id.nav_detalhes_receita` com o ID da ficha correspondente.

## Verification Results

### Automated Tests
- Executado `gradle_build(app:assembleDebug)` com sucesso (`Build finished successfully`).
