
package com.minicourse.game

import android.app.Activity
import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.Button
import android.widget.LinearLayout
import kotlin.math.max
import kotlin.math.min

class MainActivity : Activity() {
    private lateinit var game: RacingView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        game = RacingView()
        setContentView(game)
    }

    override fun onResume() {
        super.onResume()
        if (::game.isInitialized) game.startGame()
    }

    override fun onPause() {
        if (::game.isInitialized) game.stopGame()
        super.onPause()
    }
}

class RacingView : View {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val road = Path()
    private var playerX = 0f
    private var speed = 0f
    private var distance = 0f
    private var steering = 0
    private var accelerating = false
    private var paused = false
    private var lastTime = 0L
    private var running = false

    private val sky = Color.rgb(100, 190, 240)
    private val grass = Color.rgb(30, 135, 55)
    private val asphalt = Color.rgb(55, 58, 64)
    private val white = Color.WHITE
    private val red = Color.rgb(240, 40, 45)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        paint.color = sky
        canvas.drawRect(0f, 0f, w, h, paint)

        paint.color = Color.rgb(70, 170, 220)
        canvas.drawCircle(w * 0.8f, h * 0.14f, h * 0.05f, paint)

        paint.color = grass
        canvas.drawRect(0f, h * 0.36f, w, h, paint)

        val horizon = h * 0.34f
        val topLeft = w * 0.47f
        val topRight = w * 0.53f
        val bottomLeft = w * 0.08f
        val bottomRight = w * 0.92f

        road.reset()
        road.moveTo(topLeft, horizon)
        road.lineTo(topRight, horizon)
        road.lineTo(bottomRight, h)
        road.lineTo(bottomLeft, h)
        road.close()
        paint.color = asphalt
        canvas.drawPath(road, paint)

        paint.color = Color.rgb(235, 235, 235)
        canvas.drawLine(topLeft, horizon, bottomLeft, h, paint)
        canvas.drawLine(topRight, horizon, bottomRight, h, paint)

        for (i in 0..11) {
            val t = ((i / 12f + distance * 0.015f) % 1f)
            val y = horizon + (h - horizon) * t * t
            val roadWidth = (bottomRight - bottomLeft) * t
            val center = w / 2f
            val lineHeight = max(2f, h * 0.008f * t)
            paint.color = if (i % 2 == 0) white else Color.rgb(255, 210, 30)
            canvas.drawRect(
                center - roadWidth * 0.025f,
                y,
                center + roadWidth * 0.025f,
                y + lineHeight,
                paint
            )
        }

        drawCar(canvas, w * (0.5f + playerX), h * 0.79f, w * 0.14f, h * 0.17f, red)

        paint.color = Color.argb(200, 0, 0, 0)
        canvas.drawRoundRect(20f, 20f, 270f, 115f, 18f, 18f, paint)
        paint.color = white
        paint.textSize = 27f
        canvas.drawText("MINI COURSE 3D", 35f, 55f, paint)
        paint.textSize = 22f
        canvas.drawText("Vitesse : ${speed.toInt()} km/h", 35f, 88f, paint)

        drawButton(canvas, 90f, h - 100f, 150f, "◀")
        drawButton(canvas, 250f, h - 100f, 150f, "▶")
        drawButton(canvas, w - 200f, h - 100f, 160f, "GAZ")
    }

    private fun drawCar(canvas: Canvas, cx: Float, cy: Float, cw: Float, ch: Float, color: Int) {
        paint.color = Color.rgb(25, 25, 28)
        canvas.drawRoundRect(cx - cw * 0.44f, cy - ch * 0.36f, cx + cw * 0.44f, cy + ch * 0.42f, 18f, 18f, paint)
        paint.color = color
        canvas.drawRoundRect(cx - cw * 0.36f, cy - ch * 0.5f, cx + cw * 0.36f, cy + ch * 0.5f, 22f, 22f, paint)
        paint.color = Color.rgb(70, 180, 220)
        canvas.drawRoundRect(cx - cw * 0.24f, cy - ch * 0.28f, cx + cw * 0.24f, cy - ch * 0.03f, 10f, 10f, paint)
        paint.color = Color.YELLOW
        canvas.drawCircle(cx - cw * 0.23f, cy + ch * 0.4f, cw * 0.055f, paint)
        canvas.drawCircle(cx + cw * 0.23f, cy + ch * 0.4f, cw * 0.055f, paint)
    }

    private fun drawButton(canvas: Canvas, cx: Float, cy: Float, width: Float, text: String) {
        paint.color = Color.argb(175, 0, 0, 0)
        canvas.drawRoundRect(cx - width / 2f, cy - 40f, cx + width / 2f, cy + 40f, 20f, 20f, paint)
        paint.color = white
        paint.textSize = 30f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, cx, cy + 10f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val w = width.toFloat()
        val h = height.toFloat()

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                steering = 0
                accelerating = false
                if (y > h - 180f) {
                    if (x < w * 0.25f) steering = -1
                    else if (x < w * 0.48f) steering = 1
                    else accelerating = true
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                steering = 0
                accelerating = false
                return true
            }
        }
        return true
    }

    fun startGame() {
        running = true
        lastTime = System.currentTimeMillis()
        postInvalidateOnAnimation()
        post(updateRunnable)
    }

    fun stopGame() {
        running = false
        removeCallbacks(updateRunnable)
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            val now = System.currentTimeMillis()
            val dt = min(0.05f, (now - lastTime) / 1000f)
            lastTime = now

            if (accelerating) speed = min(220f, speed + 100f * dt)
            else speed = max(0f, speed - 55f * dt)

            playerX = (playerX + steering * dt * speed / 150f).coerceIn(-0.38f, 0.38f)
            distance += speed * dt / 10f

            invalidate()
            postDelayed(this, 16L)
        }
    }
}
