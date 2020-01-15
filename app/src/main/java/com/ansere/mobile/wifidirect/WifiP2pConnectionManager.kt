package com.ansere.mobile.wifidirect

import android.os.AsyncTask
import android.util.Log
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class WifiP2pConnectionManager(private val listener: OnDataListener) {
	private var receiveDataTask: AsyncTask<Void, Void, Int?>? = null
	private var serverSocket: Socket? = null
	// send date to this socket
	private var clientSocket: Socket? = null
	// receive messages with this input stream
	private var inputStream: InputStream? = null

	interface OnDataListener {
		fun onServerBound(success: Boolean)
		fun onClientBound(success: Boolean)
		fun onDataSend(data: Int, success: Boolean)
		fun onDataReceived(data: Int)
	}

	fun bindServer(inetAddress: InetAddress?) {
		inetAddress?.let { BindServerTask(WeakReference(this), it).execute() }
	}

	fun sendData(message: Int) {
		SendDataTask(WeakReference(this)).execute(message)
	}

	// if socket is not deleted, 'Broken pipe' error is returned on send datas
	fun deleteSocket() {
		serverSocket = null
	}

	fun bindClient() {
		BindClientTask(WeakReference(this)).execute()
	}

	fun receiveData(stop: Boolean = false) {
		if (receiveDataTask == null) {
			receiveDataTask = ReceiveDataTask(WeakReference(this)).execute()
		} else if (stop) {
			receiveDataTask!!.cancel(true)
			receiveDataTask = null
		}
	}


	class BindServerTask(private val manager: WeakReference<WifiP2pConnectionManager>, private val inetAddress: InetAddress) : AsyncTask<Void, Void, Boolean?>() {
		override fun doInBackground(vararg p0: Void): Boolean? {
			try {
				if (manager.get()?.serverSocket != null && manager.get()?.serverSocket!!.isBound) {
					return null
				}
				val socketAddress = InetSocketAddress(inetAddress, 8888)
				val socket = Socket()
				socket.bind(null)
				manager.get()?.serverSocket = socket
				manager.get()?.serverSocket!!.connect(socketAddress, 500)
				return true
			} catch (e: IOException) {
				Log.e("TAG_BIND_SERVER", e.message)
				return false
			}
		}

		override fun onPostExecute(result: Boolean?) {
			if (result != null) {
				manager.get()?.listener?.onServerBound(result)
			}
		}
	}


	class BindClientTask(private val manager: WeakReference<WifiP2pConnectionManager>) : AsyncTask<Void, Void, Boolean>() {
		override fun doInBackground(vararg p0: Void): Boolean {
			try {
				val serverSocket = ServerSocket(8888)
				manager.get()?.clientSocket = serverSocket.accept()
				// receive messages and log
				manager.get()?.inputStream = manager.get()?.clientSocket?.getInputStream()
				return true
			} catch (e: IOException) {
				Log.e("TAG_RECEIVE_DATA", e.message)
				return false
			}
		}

		override fun onPostExecute(result: Boolean) {
			manager.get()?.listener?.onClientBound(result)
		}
	}

	class SendDataTask(private val manager: WeakReference<WifiP2pConnectionManager>) : AsyncTask<Int, Void, Boolean>() {
		private var message: Int? = null

		override fun doInBackground(vararg p0: Int?): Boolean {
			if (p0[0] == null) {
				return false
			} else {
				message = p0[0]
			}

			try {
				val serverSocket = manager.get()?.serverSocket
				if (serverSocket == null || !serverSocket.isConnected) {
					return false
				}
				val outputStream = serverSocket.getOutputStream() ?: return false
				val dataOutputStream = DataOutputStream(outputStream)
				p0[0]?.let { dataOutputStream.writeInt(it) }
				return true
			} catch (e: IOException) {
				Log.e("TAG_SEND_DATA", e.message)
				return false
			}
		}

		override fun onPostExecute(result: Boolean?) {
			manager.get()?.listener?.onDataSend(message ?: 0, result ?: false)
		}
	}

	class ReceiveDataTask(private val manager: WeakReference<WifiP2pConnectionManager>) : AsyncTask<Void, Void, Int?>() {
		override fun doInBackground(vararg p0: Void): Int? {
			if (manager.get()?.inputStream != null) {
				val dataInputStream = DataInputStream(manager.get()?.inputStream)
				return dataInputStream.readInt()
			}
			return null
		}

		override fun onPostExecute(result: Int?) {
			if (result != null) {
				manager.get()?.listener?.onDataReceived(result)
				Log.d("TAG_RECEIVE_DATA", result.toString())
				val wifiP2pConnectionManager = manager.get() ?: return
				wifiP2pConnectionManager.receiveDataTask = ReceiveDataTask(WeakReference(wifiP2pConnectionManager))
				wifiP2pConnectionManager.receiveDataTask!!.execute()
			}
		}
	}
}