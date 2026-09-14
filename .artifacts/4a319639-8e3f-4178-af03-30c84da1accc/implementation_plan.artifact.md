# Fix "cannot find symbol drawer_layout" by Migrating to New UI

The project is in a transition state where `activity_main.xml` has been updated to a "Google Maps style" UI (using Bottom Sheets and Overlays), but `MainActivity.java` still references the legacy `DrawerLayout` and `NavigationView`.

## Proposed Changes

### [MainActivity.java](file:///C:/Users/abrah/Desktop/LostNet/app/src/main/java/com/example/lostnet/MainActivity.java)

1.  **Remove Legacy UI References**: Delete `DrawerLayout` and `NavigationView` member variables and their initialization.
2.  **Update Filter Logic**: `MainActivity` currently expects a `ChipGroup` with ID `chipGroupFiltros`, but the XML uses a `LinearLayout`. I will update the XML to use `ChipGroup` to maintain the logic, or update the Java code to use the existing `TextView` chips. Given the existing code, updating the XML is cleaner.
3.  **Update Refresh Logic**: Update the click listener for `fabRefresh` to use `btnRefreshMap` which exists in the XML.
4.  **Implement New Navigation**: Connect the `cardAvatar` to open the profile panel.
5.  **Remove Hamburger Menu**: Remove references to `btnMenu`.

### [activity_main.xml](file:///C:/Users/abrah/Desktop/LostNet/app/src/main/res/layout/activity_main.xml)

1.  **Update Chip Container**: Change `LinearLayout` with ID `chipContainer` to `com.google.android.material.chip.ChipGroup` with ID `chipGroupFiltros`.
2.  **Ensure IDs match**: Ensure `fabRefresh` ID is present (rename `btnRefreshMap` or update Java).

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to ensure the project compiles.

### Manual Verification
- Deploy the app and verify the new UI functions as expected (filters work, avatar opens profile, refresh works).
