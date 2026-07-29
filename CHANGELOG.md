# Changelog

All notable changes to Health Connect Export will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial release with basic functionality
- Health Connect data export (steps, calories, sleep)
- Background sync via WorkManager (24-hour interval)
- InfluxDB 3 integration for data storage
- Incremental sync with last timestamp tracking

### Changed
- Updated to Gradle 9.6.1
- Updated to AGP 8.8.0
- Updated to Kotlin 2.0.21
- compileSdk 36

### Fixed
- Cleartext HTTP for local development
- Network security configuration
- Permission bypass for testing

## [1.0.0] - 2026-07-29

### Added
- First functional release
- Data export from Health Connect to InfluxDB
- Automatic background synchronization
- Support for aggregated health metrics
- WorkManager-based scheduling

### Features
- Steps count export
- Active calories export
- Total calories export
- Sleep duration export
- Device ID tracking
- Timestamp-based incremental sync

### Technical
- Kotlin 2.0.21
- Android Gradle Plugin 8.8.0
- Gradle 9.6.1
- compileSdk 36
- Health Connect SDK
- Ktor HTTP client
- Kotlin Coroutines
- DataStore for preferences

### Configuration
- Network security config for cleartext HTTP (local dev)
- 24-hour sync interval
- Battery-friendly WorkManager constraints

### Known Limitations
- Only aggregated data (per-record export coming in v1.1)
- No heart rate per-sample data yet
- No SpO2 export yet
- No exercise session export yet
- Cleartext HTTP (HTTPS coming in production)

## [Unreleased] - Future

### Planned
- [ ] Per-record heart rate export
- [ ] SpO2 export
- [ ] Exercise session export
- [ ] HTTPS support
- [ ] API key authentication
- [ ] Offline queue for failed syncs
- [ ] Manual sync trigger
- [ ] Configurable sync interval
- [ ] Export history in app
- [ ] Data visualization

### Improvements
- [ ] Reduce sync interval to 15 min
- [ ] Add foreground service option for frequent sync
- [ ] Better error handling and user feedback
- [ ] Export progress indicator
- [ ] Historical backfill feature

### Technical Debt
- [ ] Remove permission bypass for production
- [ ] Add proper certificate pinning
- [ ] Implement request retry with exponential backoff
- [ ] Add analytics/telemetry (opt-in)
- [ ] Write comprehensive tests

---

## Versioning

We use [Semantic Versioning](https://semver.org/spec/v2.0.0.html):

- **MAJOR** version for incompatible API changes
- **MINOR** version for new functionality
- **PATCH** version for bug fixes

## References

- [GitHub Releases](https://github.com/grubfx-dev/HealthConnectExport/releases)
- [Project Board](https://github.com/users/grubfx-dev/projects/6)
- [Documentation](https://github.com/grubfx-dev/hecoexp)