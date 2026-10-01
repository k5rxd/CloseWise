package com.closewise.app;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Process;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

final class UsageInsights {
    private static final long WINDOW_MS = 15L * 60L * 1000L;

    private UsageInsights() {}

    static boolean hasAccess(Context context) {
        AppOpsManager ops = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    static List<String> recentlyActive(Context context) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        UsageStatsManager manager = (UsageStatsManager)
                context.getSystemService(Context.USAGE_STATS_SERVICE);
        long end = System.currentTimeMillis();
        UsageEvents events = manager.queryEvents(end - WINDOW_MS, end);
        UsageEvents.Event event = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            int type = event.getEventType();
            if (type == UsageEvents.Event.ACTIVITY_RESUMED ||
                    type == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                String packageName = event.getPackageName();
                if (packageName != null && !packageName.equals(context.getPackageName()) &&
                        !packageName.equals("com.android.settings") &&
                        !packageName.equals("com.android.systemui")) {
                    ordered.remove(packageName);
                    ordered.add(packageName);
                }
            }
        }
        ArrayList<String> newestFirst = new ArrayList<>(ordered);
        java.util.Collections.reverse(newestFirst);
        return newestFirst;
    }
}
