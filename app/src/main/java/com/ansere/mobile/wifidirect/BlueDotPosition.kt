package com.ansere.mobile.wifidirect

import android.util.Log
import java.lang.Math.toDegrees
import java.util.EnumSet.range
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class BlueDotPosition(private val _x: Double, private val _y: Double){
    val x: Float
    get() = this.clamped(_x.toFloat())

    val y: Float
    get() = this.clamped(_y.toFloat())


    val angle: Double
        get(){
            return toDegrees(atan2(this.x, this.y).toDouble())
    }
    val distance: Double
        get() = this.clamped(hypot(this.x,this.y)).toDouble()

    private fun clamped(v: Float): Float{
        return max(-1.0, min(1.0,v.toDouble())).toFloat()
    }

    fun pos(): String {
         when{
            (this.distance <= 0.5)  -> return "middle"
            (this.distance > 0.5 ) -> when{
                ((this.angle > -135.0) && (this.angle <= -45.0)) -> return "left"
                (this.angle > -45.0) && (this.angle <= 45.0) -> return "top"
                ((this.angle > 45.0) && (this.angle <= 135.0)) -> return "right"
                ((this.angle > 135.0) || (this.angle <= -135.0)) -> return "bottom"
            }
        }
        return "UNKNOWN 2"
    }

}