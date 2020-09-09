

package com.example.customfancontroller

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.graphics.Typeface
import android.view.accessibility.AccessibilityNodeInfo

import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.example.customfancontroller.R

import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin


private enum class FanSpeed(val label: Int) {
    OFF(R.string.fan_off),
    LOW(R.string.fan_low),
    MEDIUM(R.string.fan_medium),
    HIGH(R.string.fan_high);

    fun next() = when (this) {
        OFF -> LOW
        LOW -> MEDIUM
        MEDIUM -> HIGH
        HIGH -> OFF
    }
}

private const val RADIUS_OFFSET_LABEL = 30          //Offset from dial radius to draw text label
private const val RADIUS_OFFSET_INDICATOR = -35     //Offset from dial radius to draw indicator

class DialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // Paint styles used for rendering are initialized here. This
        // is a performance optimization, since onDraw() is called
        // for every screen refresh.
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = 55.0f
        typeface = Typeface.create("", Typeface.BOLD)
    }

    private var radius = 0.0f                  // Radius of the circle.
    private var fanSpeed = FanSpeed.OFF        // The active selection.
    //Point at which to draw label and indicator circle position. PointF is a point
    //with floating-point coordinates.
    private val pointPosition: PointF = PointF(0.0f, 0.0f)

    private val fanSpeedLowColor:Int
    private val fanSpeedMediumColor:Int
    private val fanSpeedMaxColor:Int

    init {
        isClickable = true

        val typedArray = context.obtainStyledAttributes(attrs,R.styleable.DialView)
        fanSpeedLowColor=typedArray.getColor(R.styleable.DialView_fanColor1,0)
        fanSpeedMediumColor = typedArray.getColor(R.styleable.DialView_fanColor2,0)
        fanSpeedMaxColor = typedArray.getColor(R.styleable.DialView_fanColor3,0)
        typedArray.recycle()

        updateContentDescription()

        // For minsdk >= 21, you can just add a click action. In this app since minSdk is 19,
        // you must add a delegate to handle accessibility.
        ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                val customClick = AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    AccessibilityNodeInfo.ACTION_CLICK,
                    // If the fan speed is OFF, LOW, or MEDIUM, the hint is to change the speed.
                    // If it is HIGH use reset.
                    context.getString(if (fanSpeed !=  FanSpeed.HIGH) R.string.change else R.string.reset)
                )
                info.addAction(customClick)
            }
        })
    }

    override fun performClick(): Boolean {
        // Give default click listeners priority and perform accessibility/autofill events.
        // Also calls onClickListener() to handle further subclass customizations.
        if (super.performClick()) return true

        // Rotates between each of the different selection
        // states on each click.
        fanSpeed = fanSpeed.next()
        updateContentDescription()
        // Redraw the view.
        invalidate()
        return true
    }



    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        // Calculate the radius from the smaller of the width and height.
        radius = (min(width, height) / 2.0 * 0.8).toFloat()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Set dial background color based on the selection.
        paint.color = when (fanSpeed) {
            FanSpeed.OFF -> Color.GRAY
            FanSpeed.LOW -> fanSpeedLowColor
            FanSpeed.MEDIUM -> fanSpeedMediumColor
            FanSpeed.HIGH -> fanSpeedMaxColor
        }
        // Draw the dial.
        canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), radius, paint)
        // Draw the indicator circle.
        val markerRadius = radius + RADIUS_OFFSET_INDICATOR
        pointPosition.computeXYForSpeed(fanSpeed, markerRadius)
        paint.color = Color.BLACK
        canvas.drawCircle(pointPosition.x, pointPosition.y, radius/12, paint)
        // Draw the text labels.
        val labelRadius = radius + RADIUS_OFFSET_LABEL
        for (i in FanSpeed.values()) {
            pointPosition.computeXYForSpeed(i, labelRadius)
            val label = resources.getString(i.label)
            canvas.drawText(label, pointPosition.x, pointPosition.y, paint)
        }
    }


    private fun PointF.computeXYForSpeed(pos: FanSpeed, radius: Float) {
        // Angles are in radians.
        val startAngle = Math.PI * (9 / 8.0)
        val angle = startAngle + pos.ordinal * (Math.PI / 4)
        x = (radius * cos(angle)).toFloat() + width / 2
        y = (radius * sin(angle)).toFloat() + height / 2
    }


    private fun updateContentDescription() {
        contentDescription = resources.getString(fanSpeed.label)
    }
}