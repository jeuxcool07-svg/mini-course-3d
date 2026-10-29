package com.minicourse.game

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class MainActivity : Activity() {
    private lateinit var game: RacingView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        game = RacingView(this)
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

class RacingView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val road = Path()
    private val density = resources.displayMetrics.density

    // Zones des boutons (calculées selon la taille de l'écran)
    private val btnLeft = RectF()
    private val btnRight = RectF()
    private val btnGas = RectF()

    private var pressLeft = false
    private var pressRight = false
    private var pressGas = false

    private var playerX = 0f
    private var speed = 0f
    private var distance = 0f
    private var steering = 0
    private var accelerating = false
    private var lastTime = 0L
    private var running = false

    private val sky = Color.rgb(100, 190, 240)
    private val grass = Color.rgb(30, 135, 55)
    private val asphalt = Color.rgb(55, 58, 64)
    private val white = Color.WHITE
    private val red = Color.rgb(240, 40, 45)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val bw = 110f * density
        val bh = 60f * density
        val m = 16f * density
        val top = h - bh - m
        val bottom = h - m
        btnLeft.set(m, top, m + bw, bottom)
        btnRight.set(m * 2f + bw, top, m * 2f + bw * 2f, bottom)
        btnGas.set(w - m - bw * 1.4f, top, w - m, bottom)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // Ciel
        paint.color = sky
        canvas.drawRect(0f, 0f, w, h, paint)

        paint.color = Color.rgb(70, 170, 220)
        canvas.drawCircle(w * 0.8f, h * 0.14f, h * 0.05f, paint)

        // Herbe (commence exactement à l'horizon)
        val horizon = h * 0.34f
        paint.color = grass
        canvas.drawRect(0f, horizon, w, h, paint)

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

        // Bords de la route
        paint.color = Color.LTGRAY
        paint.strokeWidth = 3f * density
        canvas.drawLine(topLeft, horizon, bottomLeft, h, paint)
        canvas.drawLine(topRight, horizon, bottomRight, h, paint)

        // Traits au milieu de la route (avec la bonne perspective)
        val topWidth = topRight - topLeft
        val fullWidth = bottomRight - bottomLeft
        for (i in 0..11) {
            val t = (i / 12f + distance * 0.015f) % 1f
            val k = t * t
            val y = horizon + (h - horizon) * k
            val roadWidth = topWidth + (fullWidth - topWidth) * k
            val center = w / 2f
            val lineHeight = max(2f, h * 0.025f * k)

            paint.color = if (i % 2 == 0) {
                white
            } else {
                Color.rgb(255, 210, 30)
            }

            canvas.drawRect(
                center - roadWidth * 0.025f,
                y,
                center + roadWidth * 0.025f,
                y + lineHeight,
                paint
            )
        }

        // Voiture
        drawCar(
            canvas,
            w * (0.5f + playerX),
            h * 0.79f,
            w * 0.14f,
            h * 0.17f,
            red
        )

        // Tableau d'information
        val pad = 12f * density
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.argb(200, 0, 0, 0)
        canvas.drawRoundRect(
            pad,
            pad,
            pad + 190f * density,
            pad + 66f * density,
            12f * density,
            12f * density,
            paint
        )

        paint.color = white
        paint.textSize = 17f * density
        canvas.drawText("MINI COURSE 3D", pad * 1.6f, pad + 26f * density, paint)

        paint.textSize = 14f * density
        canvas.drawText(
            "Vitesse : ${speed.toInt()} km/h",
            pad * 1.6f,
            pad + 50f * density,
            paint
        )

        // Boutons
        drawButton(canvas, btnLeft, "GAUCHE", pressLeft)
        drawButton(canvas, btnRight, "DROITE", pressRight)
        drawButton(canvas, btnGas, "GAZ", pressGas)
    }

    private fun drawCar(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        cw: Float,
        ch: Float,
        color: Int
    ) {
        val radius = cw * 0.12f

        paint.color = Color.rgb(25, 25, 28)
        canvas.drawRoundRect(
            cx - cw * 0.44f,
            cy - ch * 0.36f,
            cx + cw * 0.44f,
            cy + ch * 0.42f,
            radius,
            radius,
            paint
        )

        paint.color = color
        canvas.drawRoundRect(
            cx - cw * 0.36f,
            cy - ch * 0.5f,
            cx + cw * 0.36f,
            cy + ch * 0.5f,
            radius,
            radius,
            paint
        )

        paint.color = Color.rgb(70, 180, 220)
        canvas.drawRoundRect(
            cx - cw * 0.24f,
            cy - ch * 0.28f,
            cx + cw * 0.24f,
            cy - ch * 0.03f,
            radius * 0.6f,
            radius * 0.6f,
            paint
        )

        paint.color = Color.YELLOW
        canvas.drawCircle(
            cx - cw * 0.23f,
            cy + ch * 0.4f,
            cw * 0.055f,
            paint
        )
        canvas.drawCircle(
            cx + cw * 0.23f,
            cy + ch * 0.4f,
            cw * 0.055f,
            paint
        )
    }

    private fun drawButton(
        canvas: Canvas,
        r: RectF,
        text: String,
        pressed: Boolean
    ) {
        paint.color = if (pressed) {
            Color.argb(220, 230, 60, 60)
        } else {
            Color.argb(175, 0, 0, 0)
        }
        canvas.drawRoundRect(r, 16f * density, 16f * density, paint)

        paint.color = white
        paint.textSize = 16f * density
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, r.centerX(), r.centerY() + paint.textSize * 0.35f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun hit(r: RectF, x: Float, y: Float): Boolean {
        val pad = 14f * density
        return x >= r.left - pad && x <= r.right + pad &&
            y >= r.top - pad && y <= r.bottom + pad
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                pressLeft = false
                pressRight = false
                pressGas = false

                val ending = event.actionMasked == MotionEvent.ACTION_UP ||
                    event.actionMasked == MotionEvent.ACTION_CANCEL
                val lifted = if (event.actionMasked == MotionEvent.ACTION_POINTER_UP) {
                    event.actionIndex
                } else {
                    -1
                }

                if (!ending) {
                    // On regarde tous les doigts : on peut tourner ET accélérer
                    for (i in 0 until event.pointerCount) {
                        if (i == lifted) continue
                        val x = event.getX(i)
                        val y = event.getY(i)
                        if (hit(btnLeft, x, y)) pressLeft = true
                        if (hit(btnRight, x, y)) pressRight = true
                        if (hit(btnGas, x, y)) pressGas = true
                    }
                }

                steering = (if (pressRight) 1 else 0) - (if (pressLeft) 1 else 0)
                accelerating = pressGas
                return true
            }
        }

        return true
    }

    fun startGame() {
        if (running) return
        running = true
        lastTime = SystemClock.elapsedRealtime()
        postOnAnimation(updateRunnable)
    }

    fun stopGame() {
        running = false
        removeCallbacks(updateRunnable)
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!running) return

            val now = SystemClock.elapsedRealtime()
            val dt = min(0.05f, (now - lastTime) / 1000f)
            lastTime = now

            if (accelerating) {
                speed = min(220f, speed + 100f * dt)
            } else {
                speed = max(0f, speed - 55f * dt)
            }

            // La voiture reste sur la route
            playerX = (
                playerX + steering * dt * speed / 150f
            ).coerceIn(-0.24f, 0.24f)

            distance += speed * dt / 10f

            invalidate()
            postOnAnimation(this)
        }
    }
}
