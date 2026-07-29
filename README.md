[![wakatime](https://wakatime.com/badge/github/angeloanan/HealthConnectExports.svg)](https://wakatime.com/badge/github/angeloanan/HealthConnectExports)

# Health Connect Exports

Export and own your personal health data

<div style='width:300px'>

![Health Connect Exports](.github/assets/app-screenshot.png)

</div>

A small android app that allows you to export your personal health data from the Health Connect
environment app on your Android phone. The app will group your data export your data into a JSON
format and ping a specified HTTP server with the data.

As of writing this, I'm not planning to make this app "proper", i.e, making better UI / UX choices,
allowing for flexibility, supporting for other Health Connect data points, supporting other data
export method, etc.

Though, if you feel like this would greatly help you, by all means, open an issue and mention what
you want / need from the app and I'll see what I can do.

Contributions are welcome, though not expected and not guaranteed to be merged; this is a personal
project after all, but things might change with time.

## 🚀 Releases

This project uses GitHub Actions for automated builds and releases. When a new version tag is pushed
(e.g., `v1.0.0`), the following happens automatically:

1. ✅ Builds the release APK
2. ✅ Signs with the project keystore
3. ✅ Runs unit tests
4. ✅ Creates a GitHub Release
5. ✅ Attaches the signed APK

### Creating a Release

```bash
# Update version in gradle.properties if needed
# Update CHANGELOG.md

git add -A
git commit -m "Release v1.2.3"
git tag v1.2.3
git push origin main --tags
```

The GitHub Action will automatically build and create the release.

### Manual Build

```bash
./gradlew assembleRelease
```

The APK will be at `app/build/outputs/apk/release/app-release.apk`.