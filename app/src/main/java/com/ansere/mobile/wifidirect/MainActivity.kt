package com.ansere.mobile.wifidirect

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.ansere.mobile.wifidirect.java.DynamicMatrix
import com.ansere.mobile.wifidirect.java.DynamicMatrix.MatrixCell
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import org.json.JSONObject


class MainActivity : FragmentActivity(), DeviceListFragment.OnListFragmentInteractionListener {

	private var webSocket: WebSocket? = null
	private var connectedDevice: WifiP2pDevice? = null
	private val intentFilter = IntentFilter()
	private lateinit var wifiP2pManager: WifiP2pManager
	private lateinit var wifiP2pChannel: WifiP2pManager.Channel
	private lateinit var wifiP2pBroadcastReceiver: WifiP2pBroadcastReceiver
	private lateinit var wifiP2pDevice: WifiP2pDevice
	private lateinit var wifiP2pConnectionManager: WifiP2pConnectionManager
	private lateinit var sendButton: Button
	var matrix: DynamicMatrix? = null


	companion object {
		val TAG_DEVICE_LIST_FRAGMENT: String = "device_list_fragment"
		val TAG_BUTTON_FRAGMENT: String = "button_fragment"
		private const val REQUEST_ENABLE_LOCATION = 600
		val WS_PATH = "ws://192.168.4.1:8888/ws/chat/"
	}

	private val peerListListener = WifiP2pManager.PeerListListener {
		peerDeviceList ->
		val fragment = supportFragmentManager.findFragmentByTag(TAG_DEVICE_LIST_FRAGMENT) as DeviceListFragment?
		fragment?.updateDevices(peerDeviceList.deviceList.toList())
	}

	override fun onListFragmentInteraction(device: WifiP2pDevice) {
		val config = WifiP2pConfig()
		config.deviceAddress = device.deviceAddress
		config.wps.setup = WpsInfo.PBC

		wifiP2pManager.connect(wifiP2pChannel, config, object : WifiP2pManager.ActionListener {
			private lateinit var error: String
			override fun onFailure(p0: Int) {
				when (p0) {
					WifiP2pManager.P2P_UNSUPPORTED -> error = getString(R.string.error_connection_p2p_unsupported)
					WifiP2pManager.ERROR -> error = getString(R.string.error_connection_general)
					WifiP2pManager.BUSY -> error = getString(R.string.error_connection_busy)
				}
                //showDevicesList(true)
				Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
			}

			override fun onSuccess() {
				connectedDevice = device
				Toast.makeText(this@MainActivity, "Devices Connected", Toast.LENGTH_SHORT).show()
			}
		})
	}


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		sendButton = findViewById(R.id.button_send)
		sendButton.setOnClickListener{
			//wifiP2pConnectionManager.sendData(1)
	//		val msgJson = JSONObject()
	//		msgJson.put("message","UP")
	//		webSocket!!.send(msgJson.toString())
			showBlueDot()
		}

		//  Indicates a change in the Wi-Fi P2P status.
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
		// Indicates a change in the list of available peers.
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
		// Indicates the state of Wi-Fi P2P connectivity has changed.
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
		// Indicates this device's details have changed.
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

		wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
		wifiP2pChannel = wifiP2pManager.initialize(this, mainLooper, null)

		requestLocationPermission()

		supportFragmentManager
				.beginTransaction()
				.replace(R.id.container_fragment, DeviceListFragment.newInstance(), TAG_DEVICE_LIST_FRAGMENT)
				.commit()

		//matrix = findViewById(R.id.matrix)
		//(matrix as DynamicMatrix).setOnUseListener(DynamicMatrixListener())

		wifiP2pConnectionManager = WifiP2pConnectionManager(object : WifiP2pConnectionManager.OnDataListener {
			override fun onClientBound(success: Boolean) {
				Toast.makeText(this@MainActivity, "${wifiP2pDevice.deviceName} bound client $success", Toast.LENGTH_SHORT).show()
				wifiP2pConnectionManager.receiveData()
			}

			override fun onServerBound(success: Boolean) {
				Toast.makeText(this@MainActivity, "${wifiP2pDevice.deviceName} bound server $success", Toast.LENGTH_SHORT).show()
				if (success) {
                 	showBlueDot()
                }
			}

			override fun onDataSend(data: Int, success: Boolean) {
				Toast.makeText(this@MainActivity, "${wifiP2pDevice.deviceName} send $data $success", Toast.LENGTH_SHORT).show()
			}

			override fun onDataReceived(data: Int) {
				Toast.makeText(this@MainActivity, "${wifiP2pDevice.deviceName} received $data", Toast.LENGTH_SHORT).show()
			}
		})
	}

	override fun onResume() {
		super.onResume()
		wifiP2pBroadcastReceiver = WifiP2pBroadcastReceiver()
		registerReceiver(wifiP2pBroadcastReceiver, intentFilter)
		discoverPeers()
	}

	override fun onPause() {
		super.onPause()
		wifiP2pConnectionManager.receiveData(true)
		wifiP2pConnectionManager.deleteSocket()
		unregisterReceiver(wifiP2pBroadcastReceiver)
		wifiP2pManager.stopPeerDiscovery(wifiP2pChannel, null)
	}

	private fun requestLocationPermission(){
		if(!isLocationPermissionGranted()){
			val perm = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
			requestPermissions(perm, REQUEST_ENABLE_LOCATION)
		}
		else {
			discoverPeers()
		}
	}

	private fun isLocationPermissionGranted(): Boolean  {
		val permission = Manifest.permission.ACCESS_FINE_LOCATION
		return PackageManager.PERMISSION_GRANTED == checkSelfPermission(permission)
	}


	private fun discoverPeers() {
		wifiP2pManager.discoverPeers(wifiP2pChannel, object : WifiP2pManager.ActionListener {
			override fun onSuccess() {
				Log.i("TAG", "success")
			}

			override fun onFailure(p0: Int) {
				var message: String = ""
				when (p0) {
					WifiP2pManager.P2P_UNSUPPORTED -> message = "unsupported"
					WifiP2pManager.BUSY -> message = "busy"
					WifiP2pManager.ERROR -> message = "error"
				}
				Log.i("TAG", "failure: " + message)
				Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
			}
		})
	}


	private inner class WifiP2pBroadcastReceiver : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			val action = intent?.action
			if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION == action) {
				val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
				val enabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
				if (!enabled) {
					reset()
				}
			} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == action) {
				wifiP2pManager.requestPeers(wifiP2pChannel, peerListListener)
			} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == action) {
				val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
				if (networkInfo!!.isConnected) {
					wifiP2pManager.requestConnectionInfo(wifiP2pChannel) {
						wifiP2pInfo ->
						val groupOwner = wifiP2pInfo.isGroupOwner
						val groupOwnerAddress = wifiP2pInfo.groupOwnerAddress
						val groupFormed = wifiP2pInfo.groupFormed
						Log.d("TAG_WIFI_INFO", groupOwner.toString() + " " + groupOwnerAddress.toString() + " " + groupFormed.toString())

						if (groupFormed && groupOwner) {
							wifiP2pConnectionManager.bindClient()
						} else if (groupFormed) {
							wifiP2pConnectionManager.bindServer(groupOwnerAddress)
							sendButton.visibility = View.VISIBLE
						}
					}
				} else {
					reset()
				}
			} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION == action) {
				wifiP2pDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
			}
		}
	}

	private fun reset() {
		// sendButton.visibility = View.GONE
		wifiP2pConnectionManager.receiveData(true)
		connectedDevice = null
		val fragment = supportFragmentManager.findFragmentByTag(TAG_DEVICE_LIST_FRAGMENT) as DeviceListFragment?
		fragment?.updateDevices(emptyList())
		discoverPeers()
	}

	fun showBlueDot(){
		val deviceListFragment = supportFragmentManager.findFragmentByTag(TAG_DEVICE_LIST_FRAGMENT)
		supportFragmentManager
				.beginTransaction()
				.replace(deviceListFragment!!.id, ButtonFragment.newInstance(), TAG_BUTTON_FRAGMENT)
				.addToBackStack(null)
				.commit()
    //createWebSocket()
       // val fragment = supportFragmentManager.findFragmentByTag(TAG_BUTTON_FRAGMENT) as ButtonFragment?
       // fragment?.createWebSocket()
	}

}
