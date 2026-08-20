package com.dylgosling.bluetoothshark

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dylgosling.bluetoothshark.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val devices = mutableListOf<Pair<String, String>>()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            loadBondedDevices()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.refreshButton.setOnClickListener { ensurePermissionsAndLoad() }
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }
        binding.lockButton.setOnClickListener {
            if (devices.isEmpty()) {
                binding.statusText.text = "No paired device selected."
                return@setOnClickListener
            }
            val selected = devices[binding.deviceSpinner.selectedItemPosition]
            val intent = Intent(this, BluetoothWatchService::class.java).apply {
                putExtra("device_name", selected.first)
                putExtra("device_address", selected.second)
            }
            ContextCompat.startForegroundService(this, intent)
            binding.statusText.text = "Watching ${selected.first}"
        }

        ensurePermissionsAndLoad()
    }

    private fun ensurePermissionsAndLoad() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            needed += Manifest.permission.BLUETOOTH_CONNECT
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            needed += Manifest.permission.POST_NOTIFICATIONS
        }

        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
        else loadBondedDevices()
    }

    private fun loadBondedDevices() {
        val manager = getSystemService(BluetoothManager::class.java)
        val adapter: BluetoothAdapter? = manager.adapter
        if (adapter == null) {
            binding.statusText.text = "Bluetooth is not supported on this phone."
            return
        }

        try {
            devices.clear()
            adapter.bondedDevices
                .sortedBy { it.name ?: it.address }
                .forEach { d -> devices += Pair(d.name ?: "Unnamed device", d.address) }

            val labels = devices.map { "${it.first}  (${it.second})" }
            binding.deviceSpinner.adapter =
                ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)

            binding.statusText.text =
                if (devices.isEmpty()) "No paired Bluetooth devices found."
                else "Found ${devices.size} paired device(s)."
        } catch (_: SecurityException) {
            binding.statusText.text = "Bluetooth permission is required."
        }
    }
}
