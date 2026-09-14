# Implementation Plan - Upgrade Project to Material 3

The user wants to use Material 3 styles in the project. The current `InflateException` in `LoginActivity` is caused by using Material 3 styles in the layout while the project theme still inherits from Material 2. Upgrading the theme to Material 3 will resolve the crash and allow the use of modern Material 3 components.

## User Review Required

> [!IMPORTANT]
> This change will update the entire application's base theme from Material 2 (`Theme.MaterialComponents`) to Material 3 (`Theme.Material3`).
> - Some default colors, shapes, and typography might change slightly to adhere to Material 3 guidelines.
> - Material 3 uses a different set of theme attributes (e.g., `colorSecondary` vs `colorSecondaryContainer`). I will update the basic color mappings to ensure the app still looks good.

## Proposed Changes

### [Component Name] Themes and Styles

#### [MODIFY] [themes.xml](file:///C:/Users/lucas59694586/AndroidStudioProjects/mobile_SIGEC/app/src/main/res/values/themes.xml)
- Change parent of `Theme.MobileSIGEC` from `Theme.MaterialComponents.DayNight.DarkActionBar` to `Theme.Material3.DayNight.NoActionBar` (or with ActionBar if preferred, but usually M3 apps use `NoActionBar` + `Toolbar`).
- Update `Theme.MobileSIGEC.NoActionBar` to inherit from `Theme.Material3.DayNight.NoActionBar`.
- Map legacy color attributes to M3 attributes if needed (e.g., `colorPrimary` to `colorPrimary`, but also ensure `colorOnPrimary` is set).

#### [MODIFY] [themes.xml](file:///C:/Users/lucas59694586/AndroidStudioProjects/mobile_SIGEC/app/src/main/res/values-night/themes.xml)
- Perform similar parent updates for the night theme.

### [Component Name] Android Manifest

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/lucas59694586/AndroidStudioProjects/mobile_SIGEC/app/src/main/AndroidManifest.xml)
- Verify theme applications to ensure they match the new M3 definitions.

## Verification Plan

### Automated Tests
- Run `gradle build` to check for compilation errors.

### Manual Verification
- Deploy to an emulator/device.
- Confirm `LoginActivity` and `MainActivity` open without crashing.
- Check that `TextInputLayout` and `MaterialButton` render with Material 3 styling.
