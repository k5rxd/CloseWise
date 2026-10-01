package com.closewise.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

final class ExclusionStore {
    private static final String PREFS = "user_preferences";
    private static final String KEY_EXCLUDED = "excluded_packages";

    private ExclusionStore() {}

    static Set<String> get(Context context) {
        return new HashSet<>(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getStringSet(KEY_EXCLUDED, new HashSet<>()));
    }

    static void save(Context context, Set<String> packages) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putStringSet(KEY_EXCLUDED, new HashSet<>(packages))
                .apply();
    }
}
