# GitHub Actions Workflows

## Tag and Release Workflow

**File:** `.github/workflows/tag-and-release.yml`

### Overview
This workflow automatically builds and releases the Android APK when a new version tag is pushed.

### Trigger
- **Automatic:** Push to tags matching `v*` (e.g., `v1.0.0`, `v1.2.3`)
- **Manual:** Via `workflow_dispatch`

### Features
✅ Builds release APK  
✅ Signs APK with keystore  
✅ Runs unit tests  
✅ Creates GitHub Release  
✅ Uploads APK as artifact  
✅ Generates release notes  

### Setup

#### 1. Create Keystore (if you don't have one)
```bash
keytool -genkeypair -v \
  -keystore keystore.jks \
  -storepass your_store_password \
  -alias your_key_alias \
  -keypass your_key_password \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=Your Name, OU=Org, O=Org, L=City, ST=State, C=US"
```

#### 2. Encode Keystore to Base64
```bash
base64 -i keystore.jks | pbcopy  # macOS
base64 -i keystore.jks | xclip   # Linux
```

#### 3. Add Secrets to GitHub Repository
Go to **Settings → Secrets and variables → Actions** and add:

| Secret Name | Description | Example |
|------------|-------------|---------|
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded keystore | (from step 2) |
| `KEYSTORE_PASSWORD` | Keystore password | `your_store_password` |
| `KEY_ALIAS` | Key alias | `your_key_alias` |
| `KEY_PASSWORD` | Key password | `your_key_password` |

#### 4. Create a Release

```bash
# Create and push tag (triggers GitHub Action)
git tag v1.0.0
git push origin development --tags
```

The workflow will automatically:
1. Build the release APK
2. Sign it with your keystore
3. Run unit tests
4. Create a GitHub Release
5. Attach the APK

### Workflow Steps

1. **Checkout** - Get the code
2. **Setup Java** - JDK 17 (Temurin)
3. **Setup Gradle** - With caching
4. **Decode Keystore** - From base64 secret
5. **Build APK** - Release variant
6. **Run Tests** - Unit tests
7. **Sign APK** - Using r0adkll/sign-android-release
8. **Create Release** - GitHub Release with notes
9. **Upload Artifact** - Backup copy

### Notes

- The workflow uses **Temurin JDK 17**
- Gradle wrapper is used for consistency
- APK is signed with the provided keystore
- Release notes include changelog link
- Artifacts are retained for 30 days

### Troubleshooting

**Build fails:**
- Check Gradle version in `gradle-wrapper.properties`
- Verify all dependencies are available
- Review build logs in Actions tab

**Signing fails:**
- Verify keystore password is correct
- Check key alias matches
- Ensure keystore is valid

**Release not created:**
- Verify tag format matches `v*`
- Check GitHub token permissions
- Review workflow run logs

### Related Files

- `gradle/libs.versions.toml` - Version catalog
- `app/build.gradle.kts` - Android build config
- `gradle.properties` - Gradle properties

### See Also

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android Signing Docs](https://developer.android.com/studio/publish/app-signing)
- [Release Management](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)