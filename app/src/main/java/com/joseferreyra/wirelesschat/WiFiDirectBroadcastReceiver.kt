package com.joseferreyra.wirelesschat

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

class WiFiDirectBroadcastReceiver(
        private val mManager: WifiP2pManager?, private val mChannel: WifiP2pManager.Channel,
        private val mActivity: MainActivity
) : BroadcastReceiver() {
    private val LOG_TAG = this.toString()

    // Handle wifi statuses here
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        // Wifi enabled or disabled?
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION == action) {
            val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Log.i(LOG_TAG, "Wifi Direct is enabled")
            } else {
                Log.i(LOG_TAG, "Wifi Direct is not enabled")
            }
            // Handle device status changes (a new device is available, an existing device is no longer available etc.)
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == action) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Dexter.withContext(context)
                        .withPermissions(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_WIFI_STATE,
                                Manifest.permission.CHANGE_WIFI_STATE,
                                Manifest.permission.ACCESS_NETWORK_STATE,
                                Manifest.permission.CHANGE_NETWORK_STATE,
                        ).withListener(object : MultiplePermissionsListener {
                            @SuppressLint("MissingPermission")
                            override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                                mManager?.requestPeers(mChannel, mActivity)
                            }

                            @SuppressLint("MissingPermission")
                            override fun onPermissionRationaleShouldBeShown(permissions: List<PermissionRequest>, token: PermissionToken) {
                                mManager?.requestPeers(mChannel, mActivity)
                            }
                        }).check()
            } else {
                mManager?.requestPeers(mChannel, mActivity)
            }
            // Handle connection status changes (connecting to a new device, losing an existing connection etc.)
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == action) {
            mManager?.let { requestConnectionInfo(intent, it,context) }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION == action) {
            Toast.makeText(context, "Respond to this device's wifi state changing", Toast.LENGTH_SHORT).show()
            // Respond to this device's wifi state changing
        }
    }

    private fun requestConnectionInfo(intent: Intent, mManager: WifiP2pManager, context: Context) {
        val networkInfo = intent.getParcelableExtra<Parcelable>(WifiP2pManager.EXTRA_NETWORK_INFO) as NetworkInfo?
        if (networkInfo?.isConnected == true) {
            // we are connected with the other device, request connection
            // info to find group owner IP
            mManager.requestConnectionInfo(mChannel, mActivity)
        } else {
            Toast.makeText(context, "ALREADY DISCONNECTED", Toast.LENGTH_SHORT).show()
        }
    }
}