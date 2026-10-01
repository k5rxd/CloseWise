# CloseWise architecture

CloseWise is deliberately small and local. The application has no backend and no network permission.

## Components

### `MainActivity`

Builds the dashboard, reports permission readiness, displays recent activity, applies the user/system filter, and starts a close session only after Usage Access and accessibility are ready.

### `UsageInsights`

Reads foreground activity events from the previous 15 minutes. Results are ordered newest-first and exclude CloseWise, Settings, and System UI.

### `AppScanner`

Classifies applications, intersects candidates with recent activity, applies the selected filter and user exclusions, and protects critical packages.

### `ClosingSession`

Persists the queue and independent total, checked, and successfully stopped counters so Android can recreate the accessibility service without losing context.

### `AppClosingAccessibilityService`

Runs a watchdog-driven state machine against Android Settings. It resolves OEM controls, performs gesture and accessibility-click fallbacks, retries delayed navigation, skips disabled controls, and enforces a per-app timeout.

### `ProgressOverlay`

Uses Android's trusted accessibility-overlay window type. It is drawn before App info opens and stays visible until CloseWise returns. A separate interactive Stop control remains available while the visual layer stays touch-through for automation gestures.

## Safety boundaries

- Accessibility events are restricted to `com.android.settings`.
- Settings, System UI, the launcher, enabled keyboards, enabled accessibility services, and CloseWise are protected.
- User exclusions are applied independently of system protections.
- A timeout advances the queue instead of retrying indefinitely.
- Cancellation stops future work and returns an explicit partial result.

## Platform constraint

Android does not offer ordinary apps a public `forceStopPackage` capability. CloseWise automates the official Settings control with informed user consent; it does not bypass Android security, use root, or invoke hidden privileged APIs.
