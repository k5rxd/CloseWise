package com.closewise.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextView serviceTitle;
    private TextView serviceDetail;
    private TextView eligibleCount;
    private TextView excludedCount;
    private TextView recentCount;
    private TextView recentDetail;
    private LinearLayout recentIcons;
    private Button primaryButton;
    private AppScanner.Filter selectedFilter = AppScanner.Filter.USER;
    private final int userFilterId = View.generateViewId();
    private final int systemFilterId = View.generateViewId();
    private final int allFilterId = View.generateViewId();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(buildContent());
        if (getIntent().getBooleanExtra("session_complete", false)) {
            showCompletionDialog(getIntent().getIntExtra("checked_count", 0),
                    getIntent().getIntExtra("closed_count", 0), false);
        } else if (getIntent().getBooleanExtra("session_cancelled", false)) {
            showCompletionDialog(getIntent().getIntExtra("checked_count", 0),
                    getIntent().getIntExtra("closed_count", 0), true);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        refreshServiceState();
        refreshRecentActivity();
        refreshExcludedCount();
        refreshEligibleCount();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private View buildContent() {
        int pad = dp(24);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, dp(18), pad, dp(18));
        android.graphics.drawable.GradientDrawable page = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF040A14, 0xFF07182A, 0xFF050B16});
        root.setBackground(page);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(10), 0, dp(16));

        TextView brand = text("CLOSEWISE", 13, getColor(R.color.mint));
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        brand.setLetterSpacing(.18f);
        content.addView(brand);

        TextView title = text(getString(R.string.title), 34, getColor(R.color.text_primary));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(18), 0, dp(10));
        content.addView(title);

        TextView subtitle = text(getString(R.string.subtitle), 16, getColor(R.color.text_secondary));
        subtitle.setLineSpacing(0, 1.25f);
        content.addView(subtitle);

        content.addView(space(28));
        content.addView(buildServiceCard());
        content.addView(space(14));
        content.addView(buildActivityCard());
        content.addView(space(14));
        content.addView(buildFilterCard());
        content.addView(space(14));
        content.addView(buildExclusionsCard());
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        TextView privacy = text(getString(R.string.privacy_note), 12, getColor(R.color.text_secondary));
        privacy.setGravity(Gravity.CENTER);
        privacy.setPadding(dp(8), dp(8), dp(8), dp(3));
        root.addView(privacy);

        TextView developer = text(getString(R.string.developer_credit), 11,
                getColor(R.color.text_secondary));
        developer.setGravity(Gravity.CENTER);
        developer.setAlpha(.78f);
        developer.setPadding(dp(8), 0, dp(8), dp(10));
        developer.setOnClickListener(v -> startActivity(
                new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_url)))));
        root.addView(developer);

        primaryButton = new Button(this);
        primaryButton.setText(R.string.close_apps);
        primaryButton.setTextSize(17);
        primaryButton.setTypeface(Typeface.DEFAULT_BOLD);
        primaryButton.setTextColor(getColor(R.color.navy));
        primaryButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.mint)));
        primaryButton.setMinHeight(dp(58));
        primaryButton.setOnClickListener(v -> startClosing());
        root.addView(primaryButton, new LinearLayout.LayoutParams(-1, dp(60)));
        return root;
    }

    private View buildServiceCard() {
        LinearLayout card = card();
        serviceTitle = text("", 17, getColor(R.color.text_primary));
        serviceTitle.setTypeface(Typeface.DEFAULT_BOLD);
        serviceDetail = text("", 13, getColor(R.color.text_secondary));
        serviceDetail.setPadding(0, dp(5), 0, 0);
        card.addView(serviceTitle);
        card.addView(serviceDetail);
        card.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        return card;
    }

    private View buildFilterCard() {
        LinearLayout card = card();
        TextView heading = text(getString(R.string.choose_apps), 16, getColor(R.color.text_primary));
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(heading);

        RadioGroup filters = new RadioGroup(this);
        filters.setOrientation(RadioGroup.HORIZONTAL);
        filters.setPadding(0, dp(13), 0, dp(10));
        filters.addView(filterButton(R.string.user_apps, userFilterId), weighted());
        filters.addView(filterButton(R.string.system_apps, systemFilterId), weighted());
        filters.addView(filterButton(R.string.all_apps, allFilterId), weighted());
        filters.check(userFilterId);
        filters.setOnCheckedChangeListener((group, checkedId) -> {
            selectedFilter = checkedId == systemFilterId ? AppScanner.Filter.SYSTEM :
                    checkedId == allFilterId ? AppScanner.Filter.ALL : AppScanner.Filter.USER;
            refreshEligibleCount();
        });
        card.addView(filters);

        eligibleCount = text("", 13, getColor(R.color.mint));
        eligibleCount.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(eligibleCount);
        TextView detail = text(getString(R.string.filter_detail), 13,
                getColor(R.color.text_secondary));
        detail.setPadding(0, dp(5), 0, 0);
        card.addView(detail);
        return card;
    }

    private View buildActivityCard() {
        LinearLayout card = card();
        TextView heading = text(getString(R.string.recent_activity), 16, getColor(R.color.text_primary));
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(heading);
        recentCount = text("", 15, getColor(R.color.cyan));
        recentCount.setTypeface(Typeface.DEFAULT_BOLD);
        recentCount.setPadding(0, dp(7), 0, 0);
        card.addView(recentCount);
        recentDetail = text("", 13, getColor(R.color.text_secondary));
        recentDetail.setPadding(0, dp(4), 0, 0);
        card.addView(recentDetail);
        recentIcons = new LinearLayout(this);
        recentIcons.setOrientation(LinearLayout.HORIZONTAL);
        recentIcons.setGravity(Gravity.CENTER_VERTICAL);
        recentIcons.setPadding(0, dp(12), 0, 0);
        card.addView(recentIcons);
        card.setOnClickListener(v -> {
            if (!UsageInsights.hasAccess(this)) {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
            } else {
                refreshRecentActivity();
            }
        });
        return card;
    }

    private RadioButton filterButton(int label, int id) {
        RadioButton button = new RadioButton(this);
        button.setId(id);
        button.setText(label);
        button.setTextSize(13);
        button.setGravity(Gravity.CENTER);
        button.setButtonDrawable(null);
        button.setTextColor(getColorStateList(R.color.segment_text));
        button.setBackgroundResource(R.drawable.segment_background);
        button.setPadding(dp(8), dp(10), dp(8), dp(10));
        return button;
    }

    private View buildExclusionsCard() {
        LinearLayout card = card();
        TextView heading = text(getString(R.string.excluded_apps), 16, getColor(R.color.text_primary));
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(heading);
        excludedCount = text("", 13, getColor(R.color.mint));
        excludedCount.setTypeface(Typeface.DEFAULT_BOLD);
        excludedCount.setPadding(0, dp(6), 0, 0);
        card.addView(excludedCount);
        TextView detail = text(getString(R.string.excluded_apps_detail), 13,
                getColor(R.color.text_secondary));
        detail.setPadding(0, dp(5), 0, 0);
        card.addView(detail);
        card.setOnClickListener(v -> showExclusionPicker());
        return card;
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1f);
        params.setMarginEnd(dp(5));
        return params;
    }

    private void refreshServiceState() {
        boolean enabled = AppScanner.isServiceEnabled(this);
        serviceTitle.setText(enabled ? R.string.service_ready : R.string.service_needed);
        serviceDetail.setText(enabled ? R.string.service_ready_detail : R.string.service_needed_detail);
        serviceTitle.setTextColor(getColor(enabled ? R.color.mint : R.color.danger));
    }

    private void refreshEligibleCount() {
        if (eligibleCount == null) return;
        AppScanner.Filter filter = selectedFilter;
        eligibleCount.setText(R.string.scanning);
        executor.execute(() -> {
            int count = UsageInsights.hasAccess(this) ?
                    AppScanner.activeEligiblePackages(this, filter).size() : 0;
            runOnUiThread(() -> eligibleCount.setText(getResources().getQuantityString(
                    R.plurals.eligible_count, count, count)));
        });
    }

    private void refreshExcludedCount() {
        if (excludedCount == null) return;
        int count = ExclusionStore.get(this).size();
        excludedCount.setText(count == 0 ? getString(R.string.none_excluded) :
                getResources().getQuantityString(R.plurals.excluded_count, count, count));
    }

    private void refreshRecentActivity() {
        if (recentCount == null) return;
        if (!UsageInsights.hasAccess(this)) {
            recentCount.setText(R.string.usage_access_needed);
            recentDetail.setText(R.string.usage_access_detail);
            recentIcons.removeAllViews();
            return;
        }
        recentCount.setText(R.string.scanning);
        executor.execute(() -> {
            List<String> packages = UsageInsights.recentlyActive(this);
            runOnUiThread(() -> {
                int count = packages.size();
                recentCount.setText(count == 0 ? getString(R.string.no_recent_activity) :
                        getResources().getQuantityString(R.plurals.recent_count, count, count));
                recentDetail.setText(R.string.usage_access_detail);
                recentIcons.removeAllViews();
                int limit = Math.min(6, count);
                for (int i = 0; i < limit; i++) {
                    try {
                        ImageView icon = new ImageView(this);
                        icon.setImageDrawable(getPackageManager().getApplicationIcon(packages.get(i)));
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(38), dp(38));
                        params.setMarginEnd(dp(9));
                        recentIcons.addView(icon, params);
                    } catch (PackageManager.NameNotFoundException ignored) {}
                }
            });
        });
    }

    private void showExclusionPicker() {
        ExclusionPickerDialog.show(this, () -> {
            refreshExcludedCount();
            refreshEligibleCount();
        });
    }

    private void showCompletionDialog(int checked, int closed, boolean cancelled) {
        Dialog dialog = new Dialog(this);
        LinearLayout panel = card();
        panel.setPadding(dp(24), dp(24), dp(24), dp(20));

        TextView mark = text("\u2713", 28, getColor(R.color.navy));
        mark.setGravity(Gravity.CENTER);
        mark.setTypeface(Typeface.DEFAULT_BOLD);
        android.graphics.drawable.GradientDrawable markBg = new android.graphics.drawable.GradientDrawable();
        markBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        markBg.setColor(getColor(R.color.mint));
        mark.setBackground(markBg);
        panel.addView(mark, new LinearLayout.LayoutParams(dp(54), dp(54)));

        TextView title = text(getString(cancelled ? R.string.cleanup_stopped : R.string.cleanup_complete),
                23, getColor(R.color.text_primary));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(18), 0, dp(7));
        panel.addView(title);
        TextView detail = text(getString(R.string.cleanup_result, checked, closed), 14,
                getColor(R.color.text_secondary));
        detail.setLineSpacing(0, 1.2f);
        panel.addView(detail);

        Button done = new Button(this);
        done.setText(R.string.done);
        done.setTypeface(Typeface.DEFAULT_BOLD);
        done.setTextColor(getColor(R.color.navy));
        done.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.mint)));
        done.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(-1, dp(52));
        doneParams.setMargins(0, dp(22), 0, 0);
        panel.addView(done, doneParams);

        dialog.setContentView(panel);
        dialog.setOnShowListener(ignored -> {
            android.view.Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawableResource(android.R.color.transparent);
                window.setDimAmount(.78f);
                window.setLayout(-1, -2);
            }
        });
        dialog.show();
    }

    private void startClosing() {
        if (!AppScanner.isServiceEnabled(this)) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            return;
        }
        if (!UsageInsights.hasAccess(this)) {
            Toast.makeText(this, R.string.usage_access_required, Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
            return;
        }
        primaryButton.setEnabled(false);
        primaryButton.setText(R.string.scanning);
        AppScanner.Filter filter = selectedFilter;
        executor.execute(() -> {
            List<String> packages = AppScanner.activeEligiblePackages(this, filter);
            runOnUiThread(() -> {
                primaryButton.setEnabled(true);
                primaryButton.setText(R.string.close_apps);
                if (packages.isEmpty()) {
                    Toast.makeText(this, R.string.no_apps, Toast.LENGTH_SHORT).show();
                } else {
                    if (!AppClosingAccessibilityService.startClosing(packages)) {
                        Toast.makeText(this, R.string.service_reconnecting, Toast.LENGTH_LONG).show();
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    }
                }
            });
        });
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(17), dp(18), dp(17));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColors(new int[]{getColor(R.color.surface), 0xFF0D1D33});
        bg.setOrientation(android.graphics.drawable.GradientDrawable.Orientation.TL_BR);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), getColor(R.color.surface_alt));
        card.setBackground(bg);
        return card;
    }

    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        return view;
    }

    private Space space(int dp) {
        Space view = new Space(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(1, dp(dp)));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

}
