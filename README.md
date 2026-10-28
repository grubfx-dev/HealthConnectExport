# Health Connect Exports

Export and own your personal health data

> Fork of [angeloanan/HealthConnectExports](https://github.com/angeloanan/HealthConnectExports),
> developed further as part of the HeCoExp stack. See the
> [HeCoExp main repository](https://github.com/grubfx-dev/hecoexp) for the full picture.

<div style='width:300px'>

![Health Connect Exports](.github/assets/app-screenshot.png)

</div>

A small Android app that exports your personal health data from the Health Connect
environment app on your phone. It groups the data into a JSON payload and posts it to a
configured HTTP server.

The export path, the payload contract and the phase plan are documented in the main repository:

- [Architecture — Android export path](https://github.com/grubfx-dev/hecoexp/blob/development/docs/architecture.md)
- [Roadmap — current status](https://github.com/grubfx-dev/hecoexp/blob/development/docs/roadmap.md)
- [CI/CD — release pipeline and required secrets](https://github.com/grubfx-dev/hecoexp/blob/development/docs/ci-cd.md)

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