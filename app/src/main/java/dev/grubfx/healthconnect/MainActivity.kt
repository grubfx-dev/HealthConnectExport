package dev.grubfx.healthconnect

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import dev.grubfx.healthconnect.ui.theme.HealthConnectExportsTheme
import dev.grubfx.healthconnect.ui.viewmodels.ExporterBackgroundWorkViewModel
import dev.grubfx.healthconnect.ui.viewmodels.WORK_NAME_ONCE
import dev.grubfx.healthconnect.ui.components.HealthConnectProblemsBanner

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val EXPORT_DESTINATION_URI = stringPreferencesKey("export_destination_uri")

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val colorScheme = dynamicDarkColorScheme(applicationContext)
        super.onCreate(savedInstanceState)

        setContent {
            HealthConnectExportsTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = colorScheme.background
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = colorScheme.surface,
                                scrolledContainerColor = colorScheme.surface,
                                navigationIconContentColor = colorScheme.onSurface,
                                actionIconContentColor = colorScheme.onSurface,
                                titleContentColor = colorScheme.onSurface
                            ), title = {
                                Text(
                                    text = "Health Connect Exporter",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            })
                        },
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HealthConnectProblemsBanner()

                            Text(
                                text = "Permissions",
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                            )

                            // The permission buttons are stacked and each one
                            // disappears as soon as its permission is granted.
                            // When nothing is missing anymore, a short label
                            // confirms that instead of an empty section.
                            PermissionsSection()

                            Text(
                                text = "Data Export",
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                            )

                            ExportDestinationInputField()
                            RunDataExportButton()
                            ScheduleSwitch()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleSwitch(
    viewModel: ExporterBackgroundWorkViewModel = viewModel()
) {
    val (checked, setChecked) = remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(true) {
        setChecked(viewModel.isWorkScheduled())
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text("Schedule Data Export")
        Switch(
            enabled = checked != null,
            checked = checked ?: false,
            modifier = Modifier.padding(start = 16.dp),
            onCheckedChange = {
                setChecked(it)
                if (it) {
                    viewModel.scheduleWork()
                } else {
                    viewModel.cancelWork()
                }
            },
        )
    }
}

@Composable
fun ExportDestinationInputField() {
    val context = LocalContext.current
    val (fieldValue, setFieldValue) = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(true) {
        setFieldValue(context.dataStore.data.map { it[EXPORT_DESTINATION_URI] }.first() ?: "")
    }

    LaunchedEffect(fieldValue) {
        if (fieldValue == null) return@LaunchedEffect
        context.dataStore.edit { it[EXPORT_DESTINATION_URI] = fieldValue }
    }

    // The full URL including the scheme, so that https:// can be used too.
    TextField(enabled = fieldValue != null,
        placeholder = { Text("http://host:3001/api/ingest") },
        value = fieldValue ?: "Loading...",
        maxLines = 1,
        keyboardOptions = KeyboardOptions(
            KeyboardCapitalization.None,
            autoCorrect = false,
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Go
        ),
        onValueChange = { setFieldValue(it) })
}

@Composable
fun RunDataExportButton() {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)

    Button(onClick = {
        Log.d("MainActivity", "Enqueuing DataExport work")
        workManager.enqueueUniqueWork(
            WORK_NAME_ONCE,
            androidx.work.ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequest.from(DataExporterScheduleWorker::class.java)
        )
    }) {
        Text(text = "Run Data Export")
    }
}

val requiredPermissions = arrayOf(
    android.Manifest.permission.POST_NOTIFICATIONS,
)

/**
 * The permissions that allow reading data older than 30 days and reading
 * data while the app is in the background. Both are granted by Health
 * Connect, not by the Android runtime, so they are requested through the
 * Health Connect permission contract.
 */
val backgroundAndHistoryPermissions = setOf(
    HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND,
    HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY,
)

/**
 * The permissions the app asks for, grouped by the way they are granted.
 * The runtime permissions come from the Android permission dialog, the
 * Health Connect permissions from the Health Connect contract.
 */
private val runtimePermissionLabels = listOf(
    Manifest.permission.POST_NOTIFICATIONS to "Notification Permission",
    Manifest.permission.ACCESS_LOCAL_NETWORK to "Local Network Permission",
)

/**
 * The key under which the state of the Health Connect permissions is kept.
 * The runtime permissions use their permission string as the key.
 */
private const val HEALTH_CONNECT_KEY = "health-connect"

/**
 * Shows the buttons for the permissions that are still missing and a short
 * confirmation once every permission is granted. The buttons are stacked,
 * each one disappears as soon as its permission is granted.
 */
@Composable
private fun PermissionsSection() {
    // The granted state of every permission. The buttons write their state
    // into it, so the section knows whether anything is still missing.
    val grantedStates = remember { mutableStateMapOf<String, Boolean>() }

    // The permissions that are asked for on this Android version. The local
    // network permission only exists from Android 14 on.
    val expectedKeys = buildList {
        add(HEALTH_CONNECT_KEY)
        runtimePermissionLabels.forEach { (permission, _) ->
            if (permission != Manifest.permission.ACCESS_LOCAL_NETWORK ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN
            ) {
                add(permission)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RequestHealthConnectPermissionButton(grantedStates)
        runtimePermissionLabels.forEach { (permission, label) ->
            if (permission == Manifest.permission.ACCESS_LOCAL_NETWORK &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN
            ) {
                return@forEach
            }
            RuntimePermissionButton(
                permission = permission,
                label = label,
                grantedStates = grantedStates,
            )
        }

        // The confirmation replaces the buttons once every permission is
        // granted. The state is only complete after the buttons reported
        // theirs, so the check waits for all of them.
        if (expectedKeys.all { grantedStates[it] == true }) {
            Text(
                text = "All required permissions granted",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * A button that requests a runtime permission and disappears once the
 * permission is granted. The state is refreshed when the screen comes back
 * into the foreground, because the permission can also be revoked in the
 * system settings.
 */
@Composable
private fun RuntimePermissionButton(
    permission: String,
    label: String,
    grantedStates: MutableMap<String, Boolean>,
) {
    val context = LocalContext.current
    var isGranted by remember { mutableStateOf(false) }

    // The permission state is read on every composition, so the button
    // reappears as soon as the user revokes the permission in the settings.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGranted = ActivityCompat.checkSelfPermission(
                    context, permission
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d("MainActivity", "$permission granted: $granted")
        isGranted = granted
    }

    // The state is reported to the section, so it knows if anything is
    // still missing.
    SideEffect {
        grantedStates[permission] = isGranted
    }

    if (!isGranted) {
        Button(
            onClick = {
                Log.d("MainActivity", "Requesting $permission")
                permissionLauncher.launch(permission)
            },
        ) {
            Text(text = label)
        }
    }
}

/**
 * Requests the Health Connect permissions for all data types, plus the
 * background and history permissions. The button disappears once everything
 * is granted.
 */
@Composable
fun RequestHealthConnectPermissionButton(
    grantedStates: MutableMap<String, Boolean>,
) {
    val context = LocalContext.current
    val healthConnectClient = remember { HealthConnectClient.getOrCreate(context) }
    val scope = rememberCoroutineScope()
    var isGranted by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    isGranted = checkHealthConnectPermissions(healthConnectClient)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            Log.d("MainActivity", "Health Connect granted permissions: $granted")
            scope.launch {
                isGranted = checkHealthConnectPermissions(healthConnectClient)
            }
        }

    // The state is reported to the section, so it knows if anything is
    // still missing.
    SideEffect {
        grantedStates[HEALTH_CONNECT_KEY] = isGranted
    }

    if (!isGranted) {
        Button(
            onClick = {
                permissionLauncher.launch(
                    readonlyHealthConnectPermissions + backgroundAndHistoryPermissions
                )
            },
        ) {
            Text(text = "Health Connect Permission")
        }
    }
}

/**
 * Checks whether every Health Connect permission the exporter needs has
 * been granted. The background and history permissions are only relevant
 * for the features that use them, so they are checked separately.
 */
private suspend fun checkHealthConnectPermissions(
    healthConnectClient: HealthConnectClient,
): Boolean {
    val granted = healthConnectClient.permissionController.getGrantedPermissions()
    val missing = readonlyHealthConnectPermissions.filter { it !in granted }
    Log.d("MainActivity", "Missing Health Connect permissions: $missing")
    return missing.isEmpty()
}
