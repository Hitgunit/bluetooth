package com.example.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_BLUETOOTH_PERMISSIONS = 2
    private val deviceList = ArrayList<String>()
    private lateinit var arrayAdapter: ArrayAdapter<String>
    private var isReceiverRegistered = false // <-- Añadido: flag para rastrear el registro
    private var bluetoothSocket: BluetoothSocket? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val listView = findViewById<ListView>(R.id.listView)
        val hostButton = findViewById<Button>(R.id.hostButton)
        val joinButton = findViewById<Button>(R.id.joinButton)

        arrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceList)
        listView.adapter = arrayAdapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val deviceInfo = deviceList[position]
            val deviceAddress = deviceInfo.split("\n")[1]
            connectToDevice(deviceAddress)
        }

        hostButton.setOnClickListener {
            makeDiscoverable()
        }

        joinButton.setOnClickListener {
            if (hasBluetoothPermissions()) {
                discoverDevices()
            } else {
                requestBluetoothPermissions()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(address: String) {
        val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(address)
        val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Este es un UUID estándar para Bluetooth. Puede ser necesario usar uno diferente dependiendo de tu caso.

        try {
            bluetoothSocket = device?.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket?.connect()
            Toast.makeText(this, "Conectado a $address", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al conectar", Toast.LENGTH_SHORT).show()
        }
    }
    private fun makeDiscoverable() {
        val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        }
        startActivity(discoverableIntent)
    }

    @SuppressLint("MissingPermission")
    private fun discoverDevices() {
        if (bluetoothAdapter?.startDiscovery() == true) {
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            registerReceiver(receiver, filter)
            isReceiverRegistered = true // <-- Añadido: marca el receptor como registrado
        }
    }

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action!!
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                    val deviceName = device.name
                    val deviceAddress = device.address // MAC address
                    deviceList.add("$deviceName\n$deviceAddress")
                    arrayAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {  // <-- Añadido: chequea si el receptor está registrado
            unregisterReceiver(receiver)
            isReceiverRegistered = false // <-- Añadido: marca el receptor como desregistrado
        }
        bluetoothSocket?.close()

    }

    private fun hasBluetoothPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestBluetoothPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADMIN
        )

        ActivityCompat.requestPermissions(this, permissions, REQUEST_BLUETOOTH_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            REQUEST_BLUETOOTH_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    discoverDevices()
                } else {
                    Toast.makeText(this, "Permisos necesarios para descubrir dispositivos Bluetooth.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
