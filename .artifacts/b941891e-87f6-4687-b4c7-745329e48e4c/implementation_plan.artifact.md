# Fix TextInputLayout Inflation Error in LoginActivity

The application crashes on startup because `TextInputLayout` fails to inflate in `activity_login.xml`. This is typically caused by a theme mismatch or missing Material Components attributes in the applied theme.

## User Review Required

> [!IMPORTANT]
> The layout `activity_login.xml` uses Material 3 styles (`@style/Widget.Material3.TextInputLayout.OutlinedBox`), but the application theme (`Theme.MobileSIGEC`) currently inherits from Material Components (Material 2) (`Theme.MaterialComponents.DayNight.DarkActionBar`).
>
> I will update the theme to Material 3 to ensure compatibility with Material 3 components and styles.

## Proposed Changes

### [Component Name] Themes and Styles

#### [MODIFY] [themes.xml](file:///C:/Users/lucas59694586/AndroidStudioProjects/mobile_SIGEC/app/src/main/res/values/themes.xml)
- Update `Theme.MobileSIGEC` to inherit from `Theme.Material3.DayNight.NoActionBar` (or similar M3 theme).
- Explicitly set `parent` for `Theme.MobileSIGEC.NoActionBar` to ensure it correctly inherits Material attributes.
- Update color attributes to match Material 3 naming conventions if necessary, though Material 3 themes are somewhat backward compatible with legacy color names.

#### [MODIFY] [themes.xml](file:///C:/Users/lucas59694586/AndroidStudioProjects/mobile_SIGEC/app/src/main/res/values-night/themes.xml)
- Update the night theme to match the M3 inheritance.

### [Component Name] Layouts

#### [MODIFY] [activity_login.xml](file:///C:/Users/lucas59694586/AndroidStudioProjects/mobile_SIGEC/app/src/main/res/layout/activity_login.xml)
- Verify and potentially adjust `TextInputLayout` attributes to be fully compatible with the updated theme.

## Verification Plan

### Automated Tests
- Run `gradlew assembleDebug` to ensure the project still builds.

### Manual Verification
- Deploy the app to a device/emulator and verify that `LoginActivity` starts without crashing.
- Check that the `TextInputLayout` (E-mail and Password fields) are rendered correctly with the outlined style.
