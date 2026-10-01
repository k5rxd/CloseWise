package com.closewise.app;

import android.annotation.SuppressLint;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.Arrays;
import java.util.List;

public final class AppClosingAccessibilityService extends AccessibilityService {
    private static final String SETTINGS_PACKAGE = "com.android.settings";
    private static final long APP_TIMEOUT_MS = 9000L;
    private static volatile AppClosingAccessibilityService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable heartbeat = this::tick;
    private ProgressOverlay overlay;
    private long appStartedAt;
    private long forceControlFoundAt;
    private boolean advancing;
    private boolean actionInFlight;
    private boolean reopenedCurrent;

    static boolean startClosing(List<String> packages) {
        AppClosingAccessibilityService service = instance;
        if (service == null || packages == null || packages.isEmpty()) return false;
        service.handler.post(() -> service.begin(packages));
        return true;
    }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        overlay = new ProgressOverlay(this, this::cancelSession);
        if (ClosingSession.isActive(this)) {
            appStartedAt = SystemClock.elapsedRealtime();
            overlay.show();
            updateOverlay();
            scheduleTick(0L);
        }
    }

    private void begin(List<String> packages) {
        ClosingSession.begin(this, packages);
        appStartedAt = SystemClock.elapsedRealtime();
        forceControlFoundAt = 0L;
        advancing = false;
        actionInFlight = false;
        overlay.show();
        updateOverlay();
        handler.postDelayed(() -> {
            ClosingSession.openCurrent(this);
            scheduleTick(180L);
        }, 280L);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (ClosingSession.isActive(this)) scheduleTick(0L);
    }

    @Override public void onInterrupt() {}

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (overlay != null) overlay.hide();
        if (instance == this) instance = null;
        super.onDestroy();
    }

    private void tick() {
        handler.removeCallbacks(heartbeat);
        if (!ClosingSession.isActive(this)) {
            handler.postDelayed(overlay::hide, 380L);
            return;
        }
        updateOverlay();
        processCurrentWindow();
        if (ClosingSession.isActive(this)) scheduleTick(250L);
    }

    private void scheduleTick(long delay) {
        handler.removeCallbacks(heartbeat);
        handler.postDelayed(heartbeat, delay);
    }

    private void processCurrentWindow() {
        if (advancing || actionInFlight) return;
        if (SystemClock.elapsedRealtime() - appStartedAt >= APP_TIMEOUT_MS) {
            finishCurrent(false);
            return;
        }
        AccessibilityNodeInfo root = settingsRoot();
        if (root == null) {
            if (!reopenedCurrent && SystemClock.elapsedRealtime() - appStartedAt >= 2500L) {
                reopenedCurrent = true;
                ClosingSession.openCurrent(this);
            }
            return;
        }
        AccessibilityNodeInfo confirmation = findConfirmation(root);
        if (confirmation != null && confirmation.isEnabled()) {
            if (clickOrTap(confirmation)) handler.postDelayed(() -> finishCurrent(true), 450L);
            return;
        }
        AccessibilityNodeInfo forceStop = findForceStop(root);
        if (forceStop == null) return;
        if (!forceStop.isEnabled()) {
            finishCurrent(false);
            return;
        }
        if (forceControlFoundAt == 0L) {
            forceControlFoundAt = SystemClock.elapsedRealtime();
            return;
        }
        if (SystemClock.elapsedRealtime() - forceControlFoundAt >= 350L) clickOrTap(forceStop);
    }

    private AccessibilityNodeInfo settingsRoot() {
        for (AccessibilityWindowInfo window : getWindows()) {
            AccessibilityNodeInfo root = window.getRoot();
            if (root != null && root.getPackageName() != null &&
                    SETTINGS_PACKAGE.contentEquals(root.getPackageName())) return root;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        return root != null && root.getPackageName() != null &&
                SETTINGS_PACKAGE.contentEquals(root.getPackageName()) ? root : null;
    }

    private void finishCurrent(boolean closed) {
        if (advancing) return;
        advancing = true;
        actionInFlight = false;
        overlay.completeStep(ClosingSession.done(this) + 1, ClosingSession.total(this));
        handler.postDelayed(() -> {
            ClosingSession.advance(this, closed);
            advancing = false;
            appStartedAt = SystemClock.elapsedRealtime();
            forceControlFoundAt = 0L;
            reopenedCurrent = false;
            if (ClosingSession.isActive(this)) updateOverlay(); else overlay.hide();
        }, 300L);
    }

    private void cancelSession() {
        handler.removeCallbacksAndMessages(null);
        overlay.hide();
        ClosingSession.cancel(this);
    }

    private void updateOverlay() {
        String packageName = ClosingSession.current(this);
        if (packageName == null) return;
        String label = packageName;
        android.graphics.drawable.Drawable icon = null;
        try {
            android.content.pm.ApplicationInfo info = getPackageManager().getApplicationInfo(packageName, 0);
            label = getPackageManager().getApplicationLabel(info).toString();
            icon = getPackageManager().getApplicationIcon(info);
        } catch (android.content.pm.PackageManager.NameNotFoundException ignored) {}
        float withinCurrent = Math.min(.92f,
                (SystemClock.elapsedRealtime() - appStartedAt) / 1400f * .92f);
        overlay.update(label, icon, ClosingSession.done(this), ClosingSession.total(this), withinCurrent);
    }

    private AccessibilityNodeInfo findForceStop(AccessibilityNodeInfo root) {
        for (String id : Arrays.asList("com.android.settings:id/button_force_stop",
                "com.android.settings:id/force_stop_button", "com.android.settings:id/button3")) {
            AccessibilityNodeInfo node = first(root.findAccessibilityNodeInfosByViewId(id));
            if (node != null) return clickableAncestor(node);
        }
        for (String label : localizedSettingsStrings("force_stop", "force_stop_dlg_title")) {
            AccessibilityNodeInfo node = findByText(root, label);
            if (node != null) return clickableAncestor(node);
        }
        return null;
    }

    private AccessibilityNodeInfo findConfirmation(AccessibilityNodeInfo root) {
        for (String id : Arrays.asList("android:id/button1", "com.android.settings:id/button1")) {
            AccessibilityNodeInfo node = first(root.findAccessibilityNodeInfosByViewId(id));
            if (node != null) return clickableAncestor(node);
        }
        for (String label : Arrays.asList(getString(android.R.string.ok),
                getString(android.R.string.yes), "OK")) {
            AccessibilityNodeInfo node = findByText(root, label);
            if (node != null) return clickableAncestor(node);
        }
        return null;
    }

    @SuppressLint("DiscouragedApi")
    private List<String> localizedSettingsStrings(String... names) {
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        labels.add("Force stop");
        try {
            Context settings = createPackageContext(SETTINGS_PACKAGE, 0);
            Resources resources = settings.getResources();
            for (String name : names) {
                int id = resources.getIdentifier(name, "string", SETTINGS_PACKAGE);
                if (id != 0) {
                    String value = resources.getString(id);
                    if (!labels.contains(value)) labels.add(value);
                }
            }
        } catch (Exception ignored) {}
        return labels;
    }

    private static AccessibilityNodeInfo first(List<AccessibilityNodeInfo> nodes) {
        return nodes == null || nodes.isEmpty() ? null : nodes.get(0);
    }

    private static AccessibilityNodeInfo clickableAncestor(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        while (current != null) {
            if (current.isClickable()) return current;
            current = current.getParent();
        }
        return node;
    }

    private static AccessibilityNodeInfo findByText(AccessibilityNodeInfo node, String target) {
        if (node == null || target == null || target.isEmpty()) return null;
        CharSequence text = node.getText();
        if (text != null && target.equalsIgnoreCase(text.toString().trim())) return node;
        CharSequence description = node.getContentDescription();
        if (description != null && target.equalsIgnoreCase(description.toString().trim())) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo match = findByText(node.getChild(i), target);
            if (match != null) return match;
        }
        return null;
    }

    private boolean clickOrTap(AccessibilityNodeInfo node) {
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (!bounds.isEmpty()) {
            overlay.pauseStopControl();
            Path path = new Path();
            path.moveTo(bounds.exactCenterX(), bounds.exactCenterY());
            boolean dispatched = dispatchGesture(new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(path, 0L, 80L)).build(),
                    null, null);
            if (dispatched) {
                beginActionCooldown();
                return true;
            }
            overlay.resumeStopControl();
        }
        AccessibilityNodeInfo target = node;
        while (target != null) {
            if (target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                beginActionCooldown();
                return true;
            }
            target = target.getParent();
        }
        return false;
    }

    private void beginActionCooldown() {
        actionInFlight = true;
        handler.postDelayed(() -> {
            actionInFlight = false;
            overlay.resumeStopControl();
            scheduleTick(0L);
        }, 420L);
    }
}
