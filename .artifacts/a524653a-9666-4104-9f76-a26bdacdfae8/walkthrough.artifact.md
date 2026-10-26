# Walkthrough - Fixing ActivityNotFoundException

I have fixed the crash that occurred after a successful login. The issue was caused by trying to start a `Fragment` as if it were an `Activity`.

## Changes Made

### Login Navigation
I updated the `LoginActivity` to navigate to `MainActivity` instead of `HomePageFragment`. `MainActivity` is the proper entry point for the app's main flow and correctly hosts the `HomePageFragment` as its start destination.

#### [LoginActivity.java](file:///C:/Users/ana57393876/StudioProjects/mobile_SIGEC/app/src/main/java/com/example/mobilesigec/LoginActivity.java)

```diff
-import com.example.mobilesigec.home.HomePageFragment;
...
-startActivity(new Intent(LoginActivity.this, HomePageFragment.class));
+startActivity(new Intent(LoginActivity.this, MainActivity.class));
```

## Verification Results

### Automated Tests
- Ran `./gradlew app:assembleDebug` to ensure the project still compiles correctly after the changes.
- **Result:** Build finished successfully.

### Manual Verification
- The user should now be able to log in and reach the home screen without encountering the `ActivityNotFoundException`.
