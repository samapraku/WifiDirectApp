package com.ansere.mobile.wifidirect

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.ansere.mobile.wifidirect.java.DynamicMatrix
import com.ansere.mobile.wifidirect.java.DynamicMatrix.MatrixCell
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import org.json.JSONObject
import kotlin.math.round

class ButtonFragment : Fragment(R.layout.fragment_button)  {
    private var mConnectedDeviceName: String? = null
    private var mOutStringBuffer: StringBuffer? = null
    private var mInStringBuffer: StringBuffer? = null
    var address: String? = null
    var deviceName: String? = null
    private val progress: ProgressDialog? = null
    private var last_x = 0.0
    private var last_y = 0.0
    private var matrix: DynamicMatrix? = null
    var  webSocket: WebSocket? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createWebSocket()

         matrix = view.findViewById(R.id.matrix)

       /* (matrix as DynamicMatrix).setOnTouchListener(object : DebouncedOnTouchListener(300){
            override fun onDebouncedTouch(motionEvent: MotionEvent?) {
                Log.d("TOUCHEVENT","touched")
                (matrix as DynamicMatrix).dispatchTouchEvent(motionEvent)
            }
        }) */
        // Once connected setup the listener
        (matrix as DynamicMatrix).setOnUseListener(object : DynamicMatrix.DynamicMatrixListener {

            override fun onPress(cell: MatrixCell, pointerId: Int, actual_x: Float, actual_y: Float) {
                val x = calcX(cell, actual_x)
                val y = calcY(cell, actual_y)
                Log.d("TAG_X_Y","X: ${x} Y: ${y}")

                last_x = x
                last_y = y
                val bdot = BlueDotPosition(x,y)
                send(bdot)
            }


            override fun onMove(cell: MatrixCell, pointerId: Int, actual_x: Float, actual_y: Float) {
                val x = calcX(cell, actual_x)
                val y = calcY(cell, actual_y)
                if (x != last_x || y != last_y) {
                  //  send(BlueDotPosition(x,y))
                    last_x = x
                    last_y = y
                }
            }

            override fun onRelease(cell: MatrixCell, pointerId: Int, actual_x: Float, actual_y: Float) {
                val x = calcX(cell, actual_x)
                val y = calcY(cell, actual_y)
           //     send(BlueDotPosition(x,y))
                last_x = x
                last_y = y
            }
        })
    }

    companion object {
        fun newInstance(): Fragment {
            return ButtonFragment()
        }
    }

    private fun calcX(cell: MatrixCell, actual_x: Float): Double {
        var relative_x = actual_x - cell.bounds.left
        relative_x = (relative_x - cell.width / 2) / (cell.width / 2)
        return round(relative_x * 10000.0) / 10000.0
    }

    private fun calcY(cell: MatrixCell, actual_y: Float): Double {
        var relative_y = actual_y - cell.bounds.top
        relative_y = (relative_y - cell.height / 2) / (cell.height / 2) * -1
        return round(relative_y * 10000.0) / 10000.0
    }


    private fun calcX(roundButton: View, event: MotionEvent): Double {
        var x = (event.x - roundButton.width / 2) / (roundButton.width / 2).toDouble()
        x = round(x * 10000.0) / 10000.0
        return x
    }

    private fun calcY(roundButton: View, event: MotionEvent): Double {
        var y = (event.y - roundButton.height / 2) / (roundButton.height / 2) * -1.0
        y = round(y * 10000.0) / 10000.0
        return y
    }

    private fun buildMessage(operation: String, x: Double, y: Double): String {
        return "$operation,$x,$y\n"
    }

    fun send(pos: BlueDotPosition) { // Check that we're actually connected before trying anything
        // Check that there's actually something to send

        val msgJson = JSONObject()
            msgJson.put("message",pos.pos())
            Log.d("BLUEDOTPOSITION",pos.pos())
            webSocket?.send(msgJson.toString())
    }


    private fun msg(message: String) {
        val statusView = view?.findViewById<View>(R.id.status) as TextView
        statusView.text = message
    }

    private fun createWebSocket(){
        val request = Request.Builder().url(MainActivity.WS_PATH).build()
        val wsListener = DisplayWebSocketListener()
        webSocket = OkHttpClient().newWebSocket(request, wsListener)
    }
}