package com.closewise.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class ExclusionPickerDialog {
    private ExclusionPickerDialog() {}

    static void show(Activity activity, Runnable onSaved) {
        Dialog loading = loadingDialog(activity);
        loading.show();
        new Thread(() -> {
            List<AppItem> items = loadItems(activity);
            Set<String> selected = ExclusionStore.get(activity);
            activity.runOnUiThread(() -> {
                loading.dismiss();
                showPicker(activity, items, selected, onSaved);
            });
        }, "closewise-app-loader").start();
    }

    private static void showPicker(Activity activity, List<AppItem> items,
                                   Set<String> selected, Runnable onSaved) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 22), dp(activity, 20), dp(activity, 22), dp(activity, 16));
        root.setBackground(rounded(activity, R.color.surface, 24, R.color.surface_alt, 1));

        TextView title = text(activity, activity.getString(R.string.exclusion_manager), 22,
                activity.getColor(R.color.text_primary), true);
        root.addView(title);
        TextView subtitle = text(activity, activity.getString(R.string.exclusion_manager_detail), 13,
                activity.getColor(R.color.text_secondary), false);
        subtitle.setPadding(0, dp(activity, 5), 0, dp(activity, 14));
        root.addView(subtitle);

        EditText search = new EditText(activity);
        search.setSingleLine(true);
        search.setHint(R.string.search_apps);
        search.setTextColor(activity.getColor(R.color.text_primary));
        search.setHintTextColor(activity.getColor(R.color.text_secondary));
        search.setTextSize(15);
        search.setPadding(dp(activity, 16), 0, dp(activity, 16), 0);
        search.setBackground(rounded(activity, R.color.surface_alt, 14, R.color.surface_alt, 0));
        root.addView(search, new LinearLayout.LayoutParams(-1, dp(activity, 52)));

        int userId = View.generateViewId();
        int systemId = View.generateViewId();
        int allId = View.generateViewId();
        RadioGroup filters = new RadioGroup(activity);
        filters.setOrientation(RadioGroup.HORIZONTAL);
        filters.setPadding(0, dp(activity, 12), 0, dp(activity, 10));
        filters.addView(filter(activity, R.string.user_apps, userId), weighted(activity));
        filters.addView(filter(activity, R.string.system_apps, systemId), weighted(activity));
        filters.addView(filter(activity, R.string.all_apps, allId), weighted(activity));
        filters.check(userId);
        root.addView(filters);

        TextView summary = text(activity, "", 13, activity.getColor(R.color.mint), true);
        summary.setPadding(dp(activity, 2), 0, 0, dp(activity, 8));
        root.addView(summary);

        ListView list = new ListView(activity);
        list.setDividerHeight(dp(activity, 1));
        list.setDivider(new android.graphics.drawable.ColorDrawable(activity.getColor(R.color.surface_alt)));
        list.setSelector(android.R.color.transparent);
        AppAdapter adapter = new AppAdapter(activity, items, selected, summary);
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout actions = new LinearLayout(activity);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        actions.setPadding(0, dp(activity, 12), 0, 0);
        Button cancel = actionButton(activity, R.string.cancel, false);
        cancel.setOnClickListener(v -> dialog.dismiss());
        Button save = actionButton(activity, R.string.save, true);
        save.setOnClickListener(v -> {
            ExclusionStore.save(activity, selected);
            dialog.dismiss();
            onSaved.run();
        });
        actions.addView(cancel, new LinearLayout.LayoutParams(0, dp(activity, 50), 1f));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, dp(activity, 50), 1f);
        saveParams.setMarginStart(dp(activity, 10));
        actions.addView(save, saveParams);
        root.addView(actions);

        filters.setOnCheckedChangeListener((group, checkedId) -> {
            adapter.mode = checkedId == systemId ? Mode.SYSTEM :
                    checkedId == allId ? Mode.ALL : Mode.USER;
            adapter.applyFilter();
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.query = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                adapter.applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        dialog.setContentView(root);
        dialog.setOnShowListener(ignored -> configureWindow(dialog));
        dialog.show();
    }

    private static List<AppItem> loadItems(Activity activity) {
        PackageManager pm = activity.getPackageManager();
        List<AppItem> result = new ArrayList<>();
        for (String packageName : AppScanner.selectablePackages(activity)) {
            try {
                ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
                boolean system = (info.flags & (ApplicationInfo.FLAG_SYSTEM |
                        ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
                result.add(new AppItem(pm.getApplicationLabel(info).toString(), packageName,
                        system, pm.getApplicationIcon(info)));
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        result.sort(Comparator.comparing(item -> item.label.toLowerCase(Locale.ROOT)));
        return result;
    }

    private static Dialog loadingDialog(Activity activity) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        TextView loading = text(activity, activity.getString(R.string.loading_apps), 16,
                activity.getColor(R.color.text_primary), true);
        loading.setGravity(Gravity.CENTER);
        loading.setPadding(dp(activity, 36), dp(activity, 28), dp(activity, 36), dp(activity, 28));
        loading.setBackground(rounded(activity, R.color.surface, 20, R.color.surface_alt, 1));
        dialog.setContentView(loading);
        dialog.setOnShowListener(ignored -> {
            Window window = dialog.getWindow();
            if (window != null) window.setDimAmount(.72f);
        });
        return dialog;
    }

    private static void configureWindow(Dialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) return;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.copyFrom(window.getAttributes());
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = (int) (dialog.getContext().getResources().getDisplayMetrics().heightPixels * .88f);
        params.gravity = Gravity.BOTTOM;
        params.dimAmount = .78f;
        window.setAttributes(params);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setBackgroundDrawableResource(android.R.color.transparent);
    }

    private static RadioButton filter(Activity activity, int text, int id) {
        RadioButton button = new RadioButton(activity);
        button.setId(id);
        button.setText(text);
        button.setTextSize(13);
        button.setGravity(Gravity.CENTER);
        button.setButtonDrawable(null);
        button.setTextColor(activity.getColorStateList(R.color.segment_text));
        button.setBackgroundResource(R.drawable.segment_background);
        return button;
    }

    private static LinearLayout.LayoutParams weighted(Activity activity) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(activity, 42), 1f);
        params.setMarginEnd(dp(activity, 5));
        return params;
    }

    private static Button actionButton(Activity activity, int text, boolean primary) {
        Button button = new Button(activity);
        button.setText(text);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(activity.getColor(primary ? R.color.navy : R.color.text_primary));
        button.setBackground(rounded(activity, primary ? R.color.mint : R.color.surface_alt,
                14, primary ? R.color.mint : R.color.surface_alt, 0));
        return button;
    }

    private static TextView text(Activity activity, String text, int size, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }

    private static GradientDrawable rounded(Activity activity, int fill, int radius, int stroke, int width) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(activity.getColor(fill));
        drawable.setCornerRadius(dp(activity, radius));
        if (width > 0) drawable.setStroke(dp(activity, width), activity.getColor(stroke));
        return drawable;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private enum Mode { USER, SYSTEM, ALL }

    private static final class AppItem {
        final String label;
        final String packageName;
        final boolean system;
        final Drawable icon;

        AppItem(String label, String packageName, boolean system, Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.system = system;
            this.icon = icon;
        }
    }

    private static final class AppAdapter extends BaseAdapter {
        private final Activity activity;
        private final List<AppItem> all;
        private final List<AppItem> shown = new ArrayList<>();
        private final Set<String> selected;
        private final TextView summary;
        Mode mode = Mode.USER;
        String query = "";

        AppAdapter(Activity activity, List<AppItem> all, Set<String> selected, TextView summary) {
            this.activity = activity;
            this.all = all;
            this.selected = selected;
            this.summary = summary;
            applyFilter();
        }

        void applyFilter() {
            shown.clear();
            for (AppItem item : all) {
                if (mode == Mode.USER && item.system) continue;
                if (mode == Mode.SYSTEM && !item.system) continue;
                String haystack = (item.label + " " + item.packageName).toLowerCase(Locale.ROOT);
                if (!query.isEmpty() && !haystack.contains(query)) continue;
                shown.add(item);
            }
            updateSummary();
            notifyDataSetChanged();
        }

        private void updateSummary() {
            int count = selected.size();
            summary.setText(count == 0 ? activity.getString(R.string.none_selected) :
                    activity.getResources().getQuantityString(R.plurals.selected_count, count, count));
        }

        @Override public int getCount() { return shown.size(); }
        @Override public AppItem getItem(int position) { return shown.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override public View getView(int position, View recycled, ViewGroup parent) {
            AppItem item = getItem(position);
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(activity, 4), dp(activity, 10), dp(activity, 2), dp(activity, 10));

            ImageView icon = new ImageView(activity);
            icon.setImageDrawable(item.icon);
            row.addView(icon, new LinearLayout.LayoutParams(dp(activity, 44), dp(activity, 44)));

            LinearLayout labels = new LinearLayout(activity);
            labels.setOrientation(LinearLayout.VERTICAL);
            labels.setPadding(dp(activity, 13), 0, dp(activity, 8), 0);
            TextView name = text(activity, item.label, 15, activity.getColor(R.color.text_primary), true);
            labels.addView(name);
            TextView meta = text(activity,
                    (item.system ? activity.getString(R.string.system_badge) : activity.getString(R.string.user_badge)) +
                            "  ·  " + item.packageName,
                    11, activity.getColor(R.color.text_secondary), false);
            meta.setMaxLines(1);
            meta.setEllipsize(android.text.TextUtils.TruncateAt.END);
            labels.addView(meta);
            row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));

            CheckBox check = new CheckBox(activity);
            check.setChecked(selected.contains(item.packageName));
            check.setButtonTintList(android.content.res.ColorStateList.valueOf(activity.getColor(R.color.mint)));
            View.OnClickListener toggle = v -> {
                boolean checked = !selected.contains(item.packageName);
                if (checked) selected.add(item.packageName); else selected.remove(item.packageName);
                check.setChecked(checked);
                updateSummary();
            };
            row.setOnClickListener(toggle);
            check.setOnClickListener(v -> {
                if (check.isChecked()) selected.add(item.packageName); else selected.remove(item.packageName);
                updateSummary();
            });
            row.addView(check, new LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48)));
            return row;
        }
    }
}
