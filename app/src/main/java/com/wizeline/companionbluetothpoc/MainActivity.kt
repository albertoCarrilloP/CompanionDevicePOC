package com.wizeline.companionbluetothpoc

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothDevice
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.MacAddress
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.wizeline.companionbluetothpoc.ui.theme.CompanionBluetothPOCTheme
import java.util.concurrent.Executor
import java.util.regex.Pattern


const val SELECT_DEVICE_REQUEST_CODE = 0

class MainActivity : ComponentActivity() {
    private lateinit var launcher: ActivityResultLauncher<IntentSenderRequest>
    private val deviceManager: CompanionDeviceManager by lazy {
        (this.getSystemService(COMPANION_DEVICE_SERVICE) as CompanionDeviceManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompanionBluetothPOCTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Button(
                        modifier = Modifier
                            .wrapContentHeight(),
                        onClick = { addBluetoothDeviceFilters() }) {
                        Text(text = "Start bluetooth device filters")
                    }
                }
            }
        }
        checkAndRequestPermissions()
        createNotificationChannel()
        launcher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val deviceToPair: BluetoothDevice? =
                        result.data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
                    deviceToPair?.createBond()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        deviceToPair?.address?.let { deviceManager.startObservingDevicePresence(it) }
                    }
                    Log.d("TAG", "onCreate: activity result OK")
                } else {
                    Log.d("TAG", "onCreate: activity result false")
                }
            }
    }

    private fun addBluetoothDeviceFilters() {
        val deviceFilter: BluetoothDeviceFilter = BluetoothDeviceFilter.Builder()
            // Match only Bluetooth devices whose name matches the pattern.
            // TODO add any pattern or uuid to start matching you device
            .setNamePattern(Pattern.compile("WH-1000XM4"))
            // Match only Bluetooth devices whose service UUID matches this pattern.
            //.addServiceUuid(ParcelUuid(UUID(0x123abcL, -1L)), null)
            .build()

        val pairingRequest: AssociationRequest = AssociationRequest.Builder()
            // Find only devices that match this request filter.
            .addDeviceFilter(deviceFilter)
            // Stop scanning as soon as one device matching the filter is found.
            .setSingleDevice(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            startAssociation(pairingRequest)
        } else {
            startAssociationLegacy(pairingRequest)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun startAssociation(pairingRequest: AssociationRequest) {
        val executor = Executor { it.run() }

        deviceManager.associate(
            pairingRequest,
            executor,
            object : CompanionDeviceManager.Callback() {
                // Called when a device is found. Launch the IntentSender so the user
                // can select the device they want to pair with.
                override fun onAssociationPending(intentSender: IntentSender) {
                    Log.d("TAG", "onAssociationPending: ")
                    try {
                        val request = IntentSenderRequest.Builder(intentSender).build()
                        launcher.launch(request)
                    } catch (e: IntentSender.SendIntentException) {
                        e.printStackTrace()
                    }
                }

                override fun onAssociationCreated(associationInfo: AssociationInfo) {
                    // The association is created.
                    Log.d("TAG", "onAssociationCreated: ")
                    var associationId = associationInfo.id
                    var macAddress: MacAddress? = associationInfo.deviceMacAddress
                    Log.d("TAG", "onAssociationCreated: $associationId $macAddress")
                }

                override fun onFailure(errorMessage: CharSequence?) {
                    // Handle the failure.
                    Log.d("TAG", "onFailure: ")
                }
            })
    }

    private fun startAssociationLegacy(pairingRequest: AssociationRequest) {
        val deviceManager: CompanionDeviceManager =
            (this.getSystemService(COMPANION_DEVICE_SERVICE) as CompanionDeviceManager)

        deviceManager.associate(
            pairingRequest,
            object : CompanionDeviceManager.Callback() {
                // Called when a device is found. Launch the IntentSender so the user
                // can select the device they want to pair with.
                override fun onDeviceFound(chooserLauncher: IntentSender) {
                    Log.d("TAG", "onDeviceFound: ")
                    startIntentSenderForResult(
                        chooserLauncher,
                        SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0
                    )
                }

                override fun onFailure(error: CharSequence?) {
                    Log.d("TAG", "onFailure: ")
                    // Handle the failure.
                }
            }, null
        )
    }

    // function to try to pair with companion with an already paired device
    private fun pairWithCompanionDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            deviceManager.myAssociations.forEach {
                it.deviceMacAddress
            }
        }
    }

    private fun createNotificationChannel() {
        val name = "ServiceNotification"
        val descriptionText = "no description needed"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("ForegroundServiceChannel", name, importance).apply {
            description = descriptionText
        }
        channel.setShowBadge(true)
        channel.setSound(
            Settings.System.DEFAULT_NOTIFICATION_URI,
            Notification.AUDIO_ATTRIBUTES_DEFAULT
        )
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun Activity.checkAndRequestPermissions() {
        val permissions = arrayOf(
            android.Manifest.permission.BLUETOOTH_CONNECT,
        )
        val permissionToRequest = mutableListOf<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionToRequest.add(permission)
            }
        }

        if (permissionToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionToRequest.toTypedArray(), 1)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CompanionBluetothPOCTheme {
        Greeting("Android")
    }
}