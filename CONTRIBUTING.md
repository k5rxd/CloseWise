# Contributing

Contributions that improve OEM compatibility, accessibility, reliability, privacy, or user experience are welcome.

1. Fork the repository and create a focused branch.
2. Build with JDK 17 and Android SDK Platform 36.
3. Run `./gradlew :app:assembleDebug :app:lintDebug`.
4. Test accessibility changes on a disposable package, never an essential component.
5. Keep signing files, device dumps, APKs, and local SDK paths out of commits.
6. Open a pull request explaining behavior changes and device coverage.

Do not add analytics, advertising, trackers, unnecessary network access, root requirements, or code/assets copied from another application.
