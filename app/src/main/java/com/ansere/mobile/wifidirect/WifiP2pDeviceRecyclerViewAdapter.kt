package com.ansere.mobile.wifidirect

import android.net.wifi.p2p.WifiP2pDevice
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.ansere.mobile.wifidirect.DeviceListFragment.OnListFragmentInteractionListener
import com.ansere.mobile.wifidirect.R

class WifiP2pDeviceRecyclerViewAdapter(private val listener: OnListFragmentInteractionListener?) : RecyclerView.Adapter<WifiP2pDeviceRecyclerViewAdapter.ViewHolder>() {
	var values: List<WifiP2pDevice>? = null
		get() = field
		set(values) {
			field = values
			notifyDataSetChanged()
		}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
				.inflate(R.layout.item_device, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.bindItem(values?.get(position))

		holder.view.setOnClickListener {
			holder.item?.let { it1 -> listener?.onListFragmentInteraction(it1) }
		}
	}

	override fun getItemCount(): Int {
		return if (values == null) return 0 else { values!!.size }
	}

	inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
		private val deviceNameTextView: TextView = view.findViewById<View>(R.id.id) as TextView
		private val deviceAddressTextView: TextView = view.findViewById<View>(R.id.content) as TextView

		var item: WifiP2pDevice? = null

		fun bindItem(wifiP2pDevice: WifiP2pDevice?) {
			item = wifiP2pDevice
			deviceNameTextView.text = wifiP2pDevice?.deviceName
			deviceAddressTextView.text = wifiP2pDevice?.deviceAddress
		}
	}
}
