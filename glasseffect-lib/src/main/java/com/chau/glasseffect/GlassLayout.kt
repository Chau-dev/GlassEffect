package com.chau.glasseffect

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import com.chau.glasseffect.engine.GlassRenderer

/**
 * GlassLayout: The Primary UI/UX Library Component.
 *
 * This container adds a 3D refractive glass substrate behind its children.
 * It encapsulates the OpenGL ES 3.0 engine, allowing any developer to
 * implement high-fidelity glassmorphism with a single XML tag.
 *
 * Usage (XML):
 *   <com.chau.glasseffect.GlassButton
 *       android:layout_width="match_parent"
 *       android:layout_height="64dp" />
 *
 * Usage (Kotlin):
 *   val glass = GlassButton(context)
 *   glass.button.text = "Click Me"
 *   layout.addView(glass)
 *
 * The engine automatically captures the background behind it every frame,
 * so the refraction stays in sync with scrolling, animations, and any
 * dynamic content changes. No manual setup is required.
 */
open class GlassLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val glassSurfaceView = GlassSurfaceView(context)

    /** Public access to the renderer for fine-tuning optical properties. */
    val renderer: GlassRenderer get() = glassSurfaceView.renderer

    private var isGles3Supported = true
    private var fallbackView: View? = null

    // ── Live Capture Engine ──────────────────────────────────────────────
    // Instead of a one-shot screenshot, we use OnPreDrawListener to
    // re-capture the parent every frame. A reusable bitmap and a
    // throttle interval keep allocations and CPU usage minimal.

    private var captureBitmap: Bitmap? = null
    private var lastCaptureTime: Long = 0L
    /** Minimum interval between captures in ms. 33ms ≈ 30 fps. */
    var captureIntervalMs: Long = 33L

    /**
     * When true (the default), the glass continuously captures the
     * background behind it so the refraction stays correct during
     * scrolling, animations, and layout changes.
     *
     * Set to false only if you are providing a manual bitmap via
     * [setEnvironment] and want full control.
     */
    var autoEnvironment: Boolean = true
        set(value) {
            field = value
            if (value && isAttachedToWindow) {
                startLiveCapture()
            } else {
                stopLiveCapture()
            }
        }

    // ── Lens Geometry ────────────────────────────────────────────────────
    private var explicitWidth: Float? = null
    private var explicitHeight: Float? = null
    private var explicitRadius: Float? = null
    private var explicitThickness: Float? = null

    // ── Initialization ───────────────────────────────────────────────────
    init {
        // 1. Check for GLES 3.0 Support
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        isGles3Supported = activityManager.deviceConfigurationInfo.reqGlEsVersion >= 0x30000

        if (isGles3Supported) {
            addView(glassSurfaceView, 0, LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            ))
        } else {
            setupFallbackUI()
        }

        // 2. Load XML Attributes
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.GlassLayout)

            val ior = a.getFloat(R.styleable.GlassLayout_glass_ior, 1.65f)
            val dispersion = a.getFloat(R.styleable.GlassLayout_glass_dispersion, 0.08f)
            val thickness = a.getFloat(R.styleable.GlassLayout_glass_thickness, 1.2f)
            val radius = a.getFloat(R.styleable.GlassLayout_glass_radius, 0.6f)
            val tiltX = a.getFloat(R.styleable.GlassLayout_glass_tilt_x, 0f)
            val tiltY = a.getFloat(R.styleable.GlassLayout_glass_tilt_y, 0f)

            if (a.hasValue(R.styleable.GlassLayout_glass_width))
                explicitWidth = a.getFloat(R.styleable.GlassLayout_glass_width, 4.2f)
            if (a.hasValue(R.styleable.GlassLayout_glass_height))
                explicitHeight = a.getFloat(R.styleable.GlassLayout_glass_height, 1.2f)
            if (a.hasValue(R.styleable.GlassLayout_glass_radius))
                explicitRadius = radius
            if (a.hasValue(R.styleable.GlassLayout_glass_thickness))
                explicitThickness = thickness

            val offX = a.getFloat(R.styleable.GlassLayout_glass_offset_x, 0f)
            val offY = a.getFloat(R.styleable.GlassLayout_glass_offset_y, 0f)

            glassSurfaceView.renderer.apply {
                offsetX = offX
                offsetY = offY
                updateMeshDimensions(
                    explicitWidth ?: 4.2f,
                    explicitHeight ?: 1.2f,
                    thickness,
                    radius
                )
                setRotation(tiltX, tiltY)
                material.ior = ior
                material.dispersion = dispersion
            }

            // Sync children (text, icons) with lens movement
            glassSurfaceView.onMoveListener = { ox, oy ->
                syncChildrenTranslation(ox, oy)
            }

            // Forward clicks from the glass surface to the layout
            glassSurfaceView.setOnClickListener { this.performClick() }

            // Read autoEnvironment from XML; defaults to true
            autoEnvironment = a.getBoolean(R.styleable.GlassLayout_glass_auto_environment, true)

            a.recycle()
        }
    }

    // ── GLES 3.0 Fallback ────────────────────────────────────────────────
    private fun setupFallbackUI() {
        val fallback = View(context).apply {
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.argb(40, 255, 255, 255))
                setStroke(2, Color.argb(100, 255, 255, 255))
                cornerRadius = 30f
            }
            background = drawable
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        fallbackView = fallback
        addView(fallback, 0)
    }

    // ── Live Capture (OnPreDrawListener) ─────────────────────────────────

    private val preDrawListener = ViewTreeObserver.OnPreDrawListener {
        if (autoEnvironment) {
            val now = SystemClock.uptimeMillis()
            if (now - lastCaptureTime >= captureIntervalMs) {
                lastCaptureTime = now
                captureEnvironmentFast()
            }
        }
        true // allow the draw to proceed
    }

    /**
     * High-performance capture that reuses a single bitmap.
     * Does NOT hide the view (avoids flicker). Instead, it temporarily
     * sets our own drawing to invisible via setWillNotDraw, draws the
     * parent's other children, then restores.
     */
    private fun captureEnvironmentFast() {
        if (!isGles3Supported) return
        val parentView = parent as? ViewGroup ?: return
        if (parentView.width <= 0 || parentView.height <= 0) return

        try {
            // Reuse or create a bitmap sized to the parent
            val bmp = captureBitmap.let { existing ->
                if (existing != null
                    && existing.width == parentView.width
                    && existing.height == parentView.height
                    && !existing.isRecycled
                ) {
                    existing.eraseColor(Color.TRANSPARENT)
                    existing
                } else {
                    existing?.recycle()
                    Bitmap.createBitmap(
                        parentView.width, parentView.height,
                        Bitmap.Config.ARGB_8888
                    )
                }
            }
            captureBitmap = bmp

            val canvas = Canvas(bmp)

            // Draw each sibling except ourselves
            for (i in 0 until parentView.childCount) {
                val child = parentView.getChildAt(i)
                if (child === this) continue
                val saveCount = canvas.save()
                canvas.translate(child.left.toFloat(), child.top.toFloat())
                child.draw(canvas)
                canvas.restoreToCount(saveCount)
            }

            // Also draw the parent's own background
            parentView.background?.draw(canvas)

            setEnvironment(bmp)
        } catch (e: Exception) {
            // Silently handle; don't crash the host app
            android.util.Log.w("GlassLayout", "Live capture failed", e)
        }
    }

    private fun startLiveCapture() {
        viewTreeObserver.addOnPreDrawListener(preDrawListener)
    }

    private fun stopLiveCapture() {
        viewTreeObserver.removeOnPreDrawListener(preDrawListener)
    }

    // ── Children Sync ────────────────────────────────────────────────────

    private fun syncChildrenTranslation(ox: Float, oy: Float) {
        val viewHeight = height.toFloat()
        if (viewHeight <= 0) return

        val pxX = (ox / 4.0f) * viewHeight
        val pxY = -(oy / 4.0f) * viewHeight

        for (i in 1 until childCount) {
            val child = getChildAt(i)
            child.translationX = pxX
            child.translationY = pxY
        }
    }

    // ── Public API ───────────────────────────────────────────────────────

    /**
     * Manually provide a bitmap for the glass to refract.
     * Useful when you want full control or are using a camera feed.
     */
    fun setEnvironment(bitmap: Bitmap) {
        glassSurfaceView.renderer.setBackgroundImage(bitmap)
    }

    /**
     * Force a single environment capture right now.
     * Useful after a major layout change when autoEnvironment is off.
     */
    fun captureEnvironment() {
        captureEnvironmentFast()
    }

    /**
     * Programmatically update the lens mesh dimensions.
     * Thickness should be 2 * radius for a perfect hemispherical shape.
     */
    fun setGlassDimensions(width: Float, height: Float, thickness: Float, radius: Float) {
        explicitWidth = width
        explicitHeight = height
        explicitThickness = thickness
        explicitRadius = radius
        glassSurfaceView.setMeshDimensions(width, height, thickness, radius)
    }

    /**
     * Programmatically move the lens within the 2D plane.
     * Children (text, icons) move in sync.
     */
    fun setLensOffset(x: Float, y: Float) {
        glassSurfaceView.renderer.offsetX = x
        glassSurfaceView.renderer.offsetY = y
        syncChildrenTranslation(x, y)
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (autoEnvironment && isGles3Supported) {
            startLiveCapture()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopLiveCapture()
        captureBitmap?.recycle()
        captureBitmap = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            val scaleFactor = 4.0f
            val aspectRatio = w.toFloat() / h.toFloat()

            val autoHeight = scaleFactor
            val autoWidth = autoHeight * aspectRatio

            val meshHeight = explicitHeight ?: autoHeight
            val meshWidth = explicitWidth ?: autoWidth

            val thickness = explicitThickness ?: 1.2f
            val radius = explicitRadius ?: (meshHeight * 0.4f).coerceAtMost(meshHeight / 2.2f)

            glassSurfaceView.setMeshDimensions(meshWidth, meshHeight, thickness, radius)
            syncChildrenTranslation(glassSurfaceView.renderer.offsetX, glassSurfaceView.renderer.offsetY)
        }
    }
}
