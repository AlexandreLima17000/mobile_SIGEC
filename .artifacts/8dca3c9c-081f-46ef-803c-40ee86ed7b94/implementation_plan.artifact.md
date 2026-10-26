# Implementation Plan - Fix Login and Implement Navigation

The goal is to fix the `NullPointerException` during login, implement the requested navigation between Login, Home, Calendar, and Profile, and ensure the application logic runs smoothly by offloading database operations from the main thread.

## User Review Required

> [!IMPORTANT]
> The database connection is currently performed on the main thread using `StrictMode`. I will refactor this to use background threads for a smoother user experience and to avoid ANRs.

> [!WARNING]
> I will be creating a new `PerfilFragment` and its corresponding layout. Please let me know if there are specific UI requirements for this screen.

## Proposed Changes

### 1. Fix Login Crash and Smooth Logic

#### [MODIFY] [LoginActivity.java](file:///C:/Users/ana57393876/StudioProjects/mobile_SIGEC/app/src/main/java/com/example/mobilesigec/LoginActivity.java)
- Add null checks for the `Connection` object to prevent `NullPointerException`.
- Use an `ExecutorService` to run the authentication query in the background.
- Show a `ProgressBar` or disable the login button while connecting.
- Display appropriate error messages to the user if the connection fails or credentials are wrong.

#### [MODIFY] [ConexaoMySQL.java](file:///C:/Users/ana57393876/StudioProjects/mobile_SIGEC/app/src/main/java/com/example/mobilesigec/ConexaoMySQL.java)
- (Optional) Verify if the password `""` is correct for the production/development database if issues persist.

### 2. Implement Navigation (Home to Calendar and Profile)

#### [NEW] [PerfilFragment.java](file:///C:/Users/ana57393876/StudioProjects/mobile_SIGEC/app/src/main/java/com/example/mobilesigec/ui/perfil/PerfilFragment.java)
- Create a new fragment class for the Profile screen.

#### [NEW] [fragment_perfil.xml](file:///C:/Users/ana57393876/StudioProjects/mobile_SIGEC/app/src/main/res/layout/fragment_perfil.xml)
- Create the layout for the Profile screen.

#### [MODIFY] [mobile_navigation.xml](file:///C:/Users/ana57393876/StudioProjects/mobile_SIGEC/app/src/main/res/navigation/mobile_navigation.xml)
- Add the `nav_perfil` destination.
- Ensure all fragments are correctly mapped.

#### [MODIFY] [bottom_navigation.xml](file:///C:/Users/ana57393876/StudioProjects/mobile_SIGEC/app/src/main/res/menu/bottom_navigation.xml)
- Add the "Perfil" item to the bottom navigation bar using `@drawable/ic_person`.

#### [MODIFY] [strings.xml](file:///C:/Users/ana57393876/StudioProjects/mobile_SIGEC/app/src/main/res/values/strings.xml)
- Add missing strings for navigation labels (e.g., `menu_perfil`).

### 3. Refactor Home Page Logic

#### [MODIFY] [HomePageFragment.java](file:///C:/Users/ana57393876/StudioProjects/mobile_SIGEC/app/src/main/java/com/example/mobilesigec/home/HomePageFragment.java)
- Refactor `carregarTurmasNoSpinner` and `carregarReceitasDaTurma` to use background threads.
- Handle `null` connection scenarios gracefully with user feedback.

## Verification Plan

### Automated Tests
- I will verify the code compiles successfully after changes.
- I will check for any obvious logic errors using `analyze_file`.

### Manual Verification
1.  **Login**: Verify that the app no longer crashes if the database is unreachable.
2.  **Navigation**: Test navigating between Home, Calendar, and Profile using the Bottom Navigation bar.
3.  **UI Performance**: Observe the "smoothness" of the application during database loading states.
