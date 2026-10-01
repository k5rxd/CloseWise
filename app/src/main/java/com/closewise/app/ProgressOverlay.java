package com.closewise.app;

import android.accessibilityservice.AccessibilityService;
import android.animation.ObjectAnimator;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

final class ProgressOverlay {
    private final AccessibilityService service;
    private final WindowManager windows;
    private final Runnable onStop;
    private View visual;
    private Button stop;
    private ProgressRingView ring;
    private TextView appName;
    private TextView position;
    private ImageView icon;
    private ProgressBar progress;
    private boolean shown;
    private boolean stopAttached;

    ProgressOverlay(AccessibilityService service, Runnable onStop) {
        this.service = service;
        this.onStop = onStop;
        windows = (WindowManager) service.getSystemService(AccessibilityService.WINDOW_SERVICE);
    }

    void show() {
        if (shown) return;
        build();
        windows.addView(visual, visualParams());
        windows.addView(stop, stopParams());
        shown = true;
        stopAttached = true;
    }

    void update(String label, Drawable appIcon, int completed, int total, float withinCurrent) {
        if (!shown) show();
        if (!shown) return;
        int value = total == 0 ? 0 : Math.min(99,
                Math.round((completed + withinCurrent) * 100f / total));
        appName.setText(label);
        icon.setImageDrawable(appIcon);
        position.setText(service.getString(R.string.overlay_progress,
                Math.min(completed + 1, total), total));
        ObjectAnimator animator = ObjectAnimator.ofInt(progress, "progress", progress.getProgress(), value);
        animator.setDuration(260L);
        animator.start();
        ring.animateTo(value);
    }

    void completeStep(int completed, int total) {
        if (!shown || total <= 0) return;
        int value = Math.min(100, Math.round(completed * 100f / total));
        ObjectAnimator animator = ObjectAnimator.ofInt(progress, "progress", progress.getProgress(), value);
        animator.setDuration(220L);
        animator.start();
        ring.animateTo(value);
    }

    void pauseStopControl() {
        if (stopAttached) {
            try { windows.removeViewImmediate(stop); } catch (RuntimeException ignored) {}
            stopAttached = false;
        }
    }

    void resumeStopControl() {
        if (shown && !stopAttached) {
            try {
                windows.addView(stop, stopParams());
                stopAttached = true;
            } catch (RuntimeException ignored) {}
        }
    }

    void hide() {
        if (!shown) return;
        pauseStopControl();
        try { windows.removeViewImmediate(visual); } catch (RuntimeException ignored) {}
        shown = false;
    }

    private void build() {
        LinearLayout root = new LinearLayout(service);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(34), dp(40), dp(34), dp(120));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF040A14, 0xFF07182A, 0xFF050B16}));
        TextView brand = text("CLOSEWISE", 13, 0xFF5EF2BE, true);
        brand.setLetterSpacing(.18f);
        root.addView(brand);
        TextView title = text(service.getString(R.string.overlay_title), 29, 0xFFF8FBFF, true);
        title.setPadding(0, dp(20), 0, dp(28));
        root.addView(title);
        ring = new ProgressRingView(service);
        root.addView(ring, new LinearLayout.LayoutParams(dp(230), dp(230)));
        icon = new ImageView(service);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(74), dp(74));
        iconParams.setMargins(0, dp(28), 0, 0);
        root.addView(icon, iconParams);
        appName = text(service.getString(R.string.overlay_preparing), 19, 0xFFF8FBFF, true);
        appName.setGravity(Gravity.CENTER);
        appName.setPadding(0, dp(18), 0, dp(7));
        root.addView(appName);
        position = text("", 14, 0xFF9FB5D2, false);
        root.addView(position);
        progress = new ProgressBar(service, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF5EF2BE));
        progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF142641));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(12));
        progressParams.setMargins(0, dp(25), 0, 0);
        root.addView(progress, progressParams);
        visual = root;

        stop = new Button(service);
        stop.setText(R.string.overlay_stop);
        stop.setTextSize(15);
        stop.setTypeface(Typeface.DEFAULT_BOLD);
        stop.setTextColor(0xFFF8FBFF);
        GradientDrawable stopBg = new GradientDrawable();
        stopBg.setColor(0xFF142641);
        stopBg.setCornerRadius(dp(16));
        stopBg.setStroke(dp(1), 0xFF2A4263);
        stop.setBackground(stopBg);
        stop.setOnClickListener(v -> onStop.run());
    }

    private WindowManager.LayoutParams visualParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(-1, -1,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        return params;
    }

    private WindowManager.LayoutParams stopParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(dp(220), dp(58),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                android.graphics.PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.y = dp(42);
        return params;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(service);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * service.getResources().getDisplayMetrics().density);
    }
}
