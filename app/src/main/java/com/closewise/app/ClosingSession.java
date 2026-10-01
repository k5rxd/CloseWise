package com.closewise.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

final class ClosingSession {
    private static final String PREFS = "closing_session";
    private static final String KEY_QUEUE = "queue";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_DONE = "done";
    private static final String KEY_TOTAL = "total";
    private static final String KEY_CLOSED = "closed";

    private ClosingSession() {}

    static void begin(Context context, List<String> packages) {
        ArrayList<String> ordered = new ArrayList<>();
        for (String packageName : packages) {
            if (!ordered.contains(packageName)) ordered.add(packageName);
        }
        prefs(context).edit()
                .putString(KEY_QUEUE, TextUtils.join("\n", ordered))
                .putBoolean(KEY_ACTIVE, !ordered.isEmpty())
                .putInt(KEY_DONE, 0)
                .putInt(KEY_TOTAL, ordered.size())
                .putInt(KEY_CLOSED, 0)
                .apply();
    }

    static boolean isActive(Context context) {
        return prefs(context).getBoolean(KEY_ACTIVE, false);
    }

    static String current(Context context) {
        List<String> values = queue(context);
        return values.isEmpty() ? null : values.get(0);
    }

    static int total(Context context) { return prefs(context).getInt(KEY_TOTAL, 0); }

    static int done(Context context) { return prefs(context).getInt(KEY_DONE, 0); }

    static int closed(Context context) { return prefs(context).getInt(KEY_CLOSED, 0); }

    static void advance(Context context, boolean closed) {
        SharedPreferences preferences = prefs(context);
        List<String> queue = queue(context);
        if (!queue.isEmpty()) queue.remove(0);
        int done = preferences.getInt(KEY_DONE, 0) + 1;
        int closedCount = preferences.getInt(KEY_CLOSED, 0) + (closed ? 1 : 0);
        preferences.edit()
                .putString(KEY_QUEUE, TextUtils.join("\n", queue))
                .putInt(KEY_DONE, done)
                .putInt(KEY_CLOSED, closedCount)
                .putBoolean(KEY_ACTIVE, !queue.isEmpty())
                .apply();

        if (queue.isEmpty()) {
            Intent home = new Intent(context, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra("session_complete", true)
                    .putExtra("checked_count", done)
                    .putExtra("closed_count", closedCount);
            context.startActivity(home);
        } else {
            openCurrent(context);
        }
    }

    static void cancel(Context context) {
        SharedPreferences preferences = prefs(context);
        int done = preferences.getInt(KEY_DONE, 0);
        int closed = preferences.getInt(KEY_CLOSED, 0);
        preferences.edit().putBoolean(KEY_ACTIVE, false).apply();
        Intent home = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("session_cancelled", true)
                .putExtra("checked_count", done)
                .putExtra("closed_count", closed);
        context.startActivity(home);
    }

    static void openCurrent(Context context) {
        String packageName = current(context);
        if (packageName == null) return;
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + packageName));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(intent);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static List<String> queue(Context context) {
        String serialized = prefs(context).getString(KEY_QUEUE, "");
        ArrayList<String> result = new ArrayList<>();
        if (serialized == null || serialized.isEmpty()) return result;
        for (String value : serialized.split("\n")) {
            if (!value.isEmpty()) result.add(value);
        }
        return result;
    }
}
