package com.dylgosling.bluetoothshark

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class BluetoothWatchService : Service() {
    private var targetAddress: String? = null
    private var targetName: String = "Bluetooth device"

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val device = if (Build.VERSION.SDK_INT >= 33) {
                intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            } ?: return

            if (device.address != targetAddress) return

            when (intent?.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> showStatus("$targetName connected")
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> showStatus("$targetName disconnected — reconnect it in Bluetooth settings")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(receiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        targetAddress = intent?.getStringExtra("device_address")
        targetName = intent?.getStringExtra("device_name") ?: "Bluetooth device"
        startForeground(1001, notification("Watching $targetName"))
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                "bluetooth_watch",
                "Bluetooth Watch",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notification(text: String) =
        NotificationCompat.Builder(this, "bluetooth_watch")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("Bluetooth Shark")
            .setContentText(text)
            .setOngoing(true)
            .build()

    private fun showStatus(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(1001, notification(text))
    }
}
