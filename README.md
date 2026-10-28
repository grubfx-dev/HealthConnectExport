# Health Connect Exports

Export and own your personal health data

> Fork of [angeloanan/HealthConnectExports](https://github.com/angeloanan/HealthConnectExports),
> developed further as part of the HeCoExp stack. See the
> [HeCoExp main repository](https://github.com/grubfx-dev/hecoexp) for the full picture.

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

Releases are built automatically by GitHub Actions. The version comes from the git tag, the tag format is
unprefixed (`1.0.0`, not `v1.0.0`), and the same convention is used across the HeCoExp repos. The release
workflow derives the version from the Conventional Commits since the last tag, builds the APK, signs it with
the release key and creates the GitHub Release.

The version is also injected into the APK, so the installed app reports the same version as its release.

### Manual Build

```bash
./gradlew assembleRelease
```

The APK will be at `app/build/outputs/apk/release/app-release.apk`. It is unsigned unless the signing
environment variables are set — see the CI/CD documentation of the main repository.