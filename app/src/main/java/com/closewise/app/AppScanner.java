package com.closewise.app;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class AppScanner {
    enum Filter { USER, SYSTEM, ALL }

    private AppScanner() {}

    static List<String> eligiblePackages(Context context, Filter filter) {
        return scan(context, filter, true);
    }

    static List<String> selectablePackages(Context context) {
        return scan(context, Filter.ALL, false);
    }

    static List<String> activeEligiblePackages(Context context, Filter filter) {
        Set<String> eligible = new HashSet<>(eligiblePackages(context, filter));
        List<String> result = new ArrayList<>();
        for (String packageName : UsageInsights.recentlyActive(context)) {
            if (eligible.contains(packageName)) result.add(packageName);
        }
        return result;
    }

    private static List<String> scan(Context context, Filter filter, boolean applyUserExclusions) {
        PackageManager pm = context.getPackageManager();
        Set<String> protectedPackages = protectedPackages(context);
        Set<String> excludedPackages = applyUserExclusions ? ExclusionStore.get(context) :
                java.util.Collections.emptySet();
        List<String> result = new ArrayList<>();

        for (ApplicationInfo app : pm.getInstalledApplications(PackageManager.MATCH_ALL)) {
            if (!app.enabled || protectedPackages.contains(app.packageName) ||
                    excludedPackages.contains(app.packageName)) continue;
            boolean system = (app.flags & (ApplicationInfo.FLAG_SYSTEM |
                    ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
            if (filter == Filter.USER && system) continue;
            if (filter == Filter.SYSTEM && !system) continue;
            if (!system && pm.getLaunchIntentForPackage(app.packageName) == null) continue;
            result.add(app.packageName);
        }
        return result;
    }

    static boolean isServiceEnabled(Context context) {
        String enabledSetting = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        String expected = new ComponentName(context, AppClosingAccessibilityService.class)
                .flattenToString();
        if (enabledSetting != null) {
            for (String component : enabledSetting.split(":")) {
                if (expected.equalsIgnoreCase(component)) return true;
            }
        }
        AccessibilityManager manager = (AccessibilityManager)
                context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        for (AccessibilityServiceInfo info : manager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            if (info.getResolveInfo().serviceInfo.packageName.equals(context.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> protectedPackages(Context context) {
        Set<String> packages = new HashSet<>();
        packages.add(context.getPackageName());
        packages.add("android");
        packages.add("com.android.settings");
        packages.add("com.android.systemui");
        packages.add("com.android.permissioncontroller");
        packages.add("com.google.android.permissioncontroller");

        PackageManager pm = context.getPackageManager();
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo launcher = pm.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        if (launcher != null && launcher.activityInfo != null) {
            packages.add(launcher.activityInfo.packageName);
        }

        InputMethodManager imm = (InputMethodManager)
                context.getSystemService(Context.INPUT_METHOD_SERVICE);
        for (InputMethodInfo ime : imm.getEnabledInputMethodList()) {
            packages.add(ime.getPackageName());
        }

        AccessibilityManager am = (AccessibilityManager)
                context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        for (AccessibilityServiceInfo service : am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            packages.add(service.getResolveInfo().serviceInfo.packageName);
        }
        return packages;
    }
}
