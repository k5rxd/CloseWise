# Security policy

## Supported version

Security fixes are applied to the latest published release of CloseWise.

## Reporting a vulnerability

Please do not disclose suspected vulnerabilities in a public issue. Use GitHub private vulnerability reporting when available, or contact the maintainer through [github.com/k5rxd](https://github.com/k5rxd).

Include the CloseWise version, Android version, device manufacturer, reproduction steps, impact, and relevant logs with secrets removed.

## Security design

- No network permission or remote backend
- No root, shell, Shizuku, or hidden privileged API
- Accessibility events restricted to Android Settings
- Critical packages protected independently of user configuration
- Signing material excluded from source control
- Release minification and resource shrinking
- Per-app watchdog timeout and explicit cancellation
