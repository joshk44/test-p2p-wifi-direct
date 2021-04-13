package com.joseferreyra.wirelesschat

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.util.*


class MainActivity : AppCompatActivity(), PeerListListener, ConnectionInfoListener, WifiP2pManager.ChannelListener {
    private val TAG = this.javaClass.toString()
    private lateinit var mManager: WifiP2pManager
    private lateinit var mChannel: WifiP2pManager.Channel
    var mReceiver: BroadcastReceiver? = null
    var mIntentFilter: IntentFilter? = null
    private lateinit var deviceList: ArrayList<WifiP2pDevice>
    private lateinit var deviceNames: ArrayList<String>
    var deviceListView: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        deviceNames = ArrayList<String>()
        deviceListView = findViewById<View>(R.id.device_list) as ListView
        mManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        mChannel = mManager.initialize(this, mainLooper, this)
        mReceiver = WiFiDirectBroadcastReceiver(mManager, mChannel, this)
        mIntentFilter = IntentFilter()

        mIntentFilter!!.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        mIntentFilter!!.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        mIntentFilter!!.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        mIntentFilter!!.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        val btn_discover = findViewById<View>(R.id.btn_discover) as Button

        // Check if we can scan for devices
        btn_discover.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Dexter.withContext(this)
                        .withPermissions(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_WIFI_STATE,
                                Manifest.permission.CHANGE_WIFI_STATE,
                                Manifest.permission.ACCESS_NETWORK_STATE,
                                Manifest.permission.CHANGE_NETWORK_STATE,
                        ).withListener(object : MultiplePermissionsListener {
                            override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                                discoverPeers()
                            }
                            override fun onPermissionRationaleShouldBeShown(permissions: List<PermissionRequest>, token: PermissionToken) {
                                discoverPeers()
                            }
                        }).check()
            } else {
                discoverPeers()
            }
        }

        // Connect to selected device
        deviceListView!!.onItemClickListener = OnItemClickListener { adapterView, view, i, l -> connectToDevice(i) }
    }

    @SuppressLint("MissingPermission")
    private fun discoverPeers() {
        mManager.discoverPeers(mChannel, object : WifiP2pManager.ActionListener {
            // Yep, we can scan for devices
            override fun onSuccess() {
                deviceListView?.adapter = null
                Toast.makeText(getApplicationContext(), "Discovery is a success.", Toast.LENGTH_SHORT).show();
                //startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            }

            // Nope, can't scan for devices
            override fun onFailure(reasonCode: Int) {
                Toast.makeText(applicationContext, "Discovery is a failure $reasonCode", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mReceiver, mIntentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //disconnect()
    }


    // Get device names after the scan and list them on the screen
    override fun onPeersAvailable(wifiP2pDeviceList: WifiP2pDeviceList) {
        if (wifiP2pDeviceList.deviceList.isNotEmpty()) {
            deviceList = ArrayList<WifiP2pDevice>(wifiP2pDeviceList.deviceList)
            for (device in deviceList!!) {
                if (!deviceNames.contains(device.deviceName)) {
                    deviceNames.add(device.deviceName)
                }
            }
            val adapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNames)
            deviceListView!!.adapter = adapter
        }
    }

    // Connect to the selected device
    private fun connectToDevice(position: Int) {
        val toConnect = deviceList[position]
        val config = WifiP2pConfig().apply {
            wps.setup = WpsInfo.PBC
        }
        config.deviceAddress = toConnect.deviceAddress
        config.groupOwnerIntent = 15
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Dexter.withContext(this)
                    .withPermissions(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.CHANGE_NETWORK_STATE,
                    ).withListener(object : MultiplePermissionsListener {
                        override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                            connectManager(config, toConnect)
                        }
                        override fun onPermissionRationaleShouldBeShown(permissions: List<PermissionRequest>, token: PermissionToken) { /* ... */
                        }
                    }).check()
        } else {
            connectManager(config, toConnect)
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectManager(config: WifiP2pConfig, toConnect: WifiP2pDevice) {
        mManager.connect(mChannel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "Successfully connectted to " + toConnect.deviceName)
            }

            override fun onFailure(reason: Int) {
                Log.i(TAG, "Error while connecting to device. Reason code: $reason")
            }
        })
    }

    override fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo) {
        // move owner info from main screen to the chat screen
        if (wifiP2pInfo.isGroupOwner) {
            Toast.makeText(applicationContext, "Yay I am the owner!!", Toast.LENGTH_LONG).show()
            val chatIntent = Intent(this@MainActivity, ChatActivity::class.java)
            chatIntent.putExtra("Owner?", true)
            this@MainActivity.startActivityForResult(chatIntent, 100)
        } else {
            Toast.makeText(applicationContext, "The owner is: " +
                    wifiP2pInfo.groupOwnerAddress.hostAddress, Toast.LENGTH_LONG).show()
            val chatIntent = Intent(this@MainActivity, ChatActivity::class.java)
            chatIntent.putExtra("Owner?", false)
            chatIntent.putExtra("Owner Address", wifiP2pInfo.groupOwnerAddress.hostAddress)
            this@MainActivity.startActivityForResult(chatIntent, 100)
        }
    }

    override fun onChannelDisconnected() {
        Toast.makeText(this, "Channel : ${mChannel.toString()}", Toast.LENGTH_SHORT).show()
        this.finish()
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        if (mManager != null && mChannel != null) {
            mManager.requestGroupInfo(mChannel) { group ->
                if (group != null && mManager != null && mChannel != null) {
                    mManager.removeGroup(mChannel, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            Log.d(TAG, "removeGroup onSuccess -")
                        }

                        override fun onFailure(reason: Int) {
                            Log.d(TAG, "removeGroup onFailure -$reason")
                        }
                    })
                }
            }
        }
    }
}