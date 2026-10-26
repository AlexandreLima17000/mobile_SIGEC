# Fix ActivityNotFoundException for HomePageFragment

The application crashes with a `android.content.ActivityNotFoundException` when attempting to navigate from `LoginActivity` to `HomePageFragment`. This is because `HomePageFragment` is a `Fragment`, but it is being started as an `Activity` via an `Intent`.

## User Review Required

> [!NOTE]
> The fix involves changing the navigation target in `LoginActivity` from `HomePageFragment` to `MainActivity`. `MainActivity` is the host for the navigation graph that includes `HomePageFragment` as its start destination.

## Proposed Changes

### [app]

#### [MODIFY] [LoginActivity.java](file:///C:/Users/ana57393876/StudioProjects/mobile_SIGEC/app/src/main/java/com/example/mobilesigec/LoginActivity.java)
- Change `startActivity(new Intent(LoginActivity.this, HomePageFragment.class))` to `startActivity(new Intent(LoginActivity.this, MainActivity.class))`.
- Remove the unused import `com.example.mobilesigec.home.HomePageFragment`.

## Verification Plan

### Manual Verification
- Deploy the app to a device/emulator.
- Perform a successful login.
- Verify that the app navigates to `MainActivity` (which displays `HomePageFragment` by default) without crashing.
