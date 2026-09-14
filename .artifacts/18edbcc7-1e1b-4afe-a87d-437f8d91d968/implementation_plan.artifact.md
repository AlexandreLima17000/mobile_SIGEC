# Fix TextInputLayout Inflation Error

The application crashes with an `InflateException` when starting `LoginActivity`. This is caused by a mismatch between the activity theme and the component styles used in `activity_login.xml`. Specifically, `TextInputLayout` is using a Material3 style (`Widget.Material3.TextInputLayout.OutlinedBox`) while the theme inherits from a Material Components (Material2) theme. Additionally, the `Theme.MobileSIGEC.NoActionBar` style used by the activity does not have a parent, causing it to lack essential Material attributes.

## User Review Required

> [!IMPORTANT]
> I am proposing to upgrade the project theme to **Material3**. This is consistent with the styles already being used in the layout and follows modern Android development standards. This will change the default appearance of some components to align with Material3 guidelines.

## Proposed Changes

### Theme Updates

#### [MODIFY] [themes.xml](file:///C:/Users/lucas59694586/AndroidStudioProjects/mobile_SIGEC/app/src/main/res/values/themes.xml)
- Change parent of `Theme.MobileSIGEC` to `Theme.Material3.DayNight`.
- Ensure `Theme.MobileSIGEC.NoActionBar` inherits from `Theme.MobileSIGEC`.
- Update overlay styles to Material3 versions.

#### [MODIFY] [themes.xml](file:///C:/Users/lucas59694586/AndroidStudioProjects/mobile_SIGEC/app/src/main/res/values-night/themes.xml)
- Change parent of `Theme.MobileSIGEC` to `Theme.Material3.DayNight`.

### Layout Adjustments (Optional but recommended)

#### [MODIFY] [activity_login.xml](file:///C:/Users/lucas59694586/AndroidStudioProjects/mobile_SIGEC/app/src/main/res/layout/activity_login.xml)
- Verify that all Material components are compatible with the new theme. (Already using Material3 styles, so this should be fine).

## Verification Plan

### Automated Tests
- Run `gradle_build` to ensure the project compiles with the theme changes.

### Manual Verification
- Deploy the app to a device/emulator and verify that `LoginActivity` starts without crashing.
- Inspect the UI to ensure the `TextInputLayout` and `MaterialButton` are rendered correctly.
