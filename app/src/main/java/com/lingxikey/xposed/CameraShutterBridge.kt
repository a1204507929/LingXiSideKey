package com.lingxikey.custom.xposed

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.lingxikey.custom.onXposedFailure

/**
 * Runs inside the OPPO/ColorOS camera process.
 *
 * The camera exposes no public API for still capture and ignores injected
 * KEYCODE_CAMERA / volume keys, so we cannot drive it from system_server directly.
 * Instead this bridge locates the real on-screen shutter button and feeds it a
 * genuine touch sequence, which is exactly what a finger does.
 *
 * Why not call the internal listener / performClick()?
 *   The shipped camera (v6.106.965) registers its capture handler on the plain
 *   `com.oplus.camera:id/shutter_button` ImageView. Calling
 *   `ShutterButton.performClick()` on the custom view returns false and does nothing,
 *   while the synthetic touch used previously landed off-centre and hit the
 *   neighbouring `switch_camera_button`, which is why a "shutter press" flipped the
 *   lens instead of taking a photo. Dispatching real touches at the button's own
 *   absolute coordinates is both simpler and immune to internal renames.
 *
 * Semantics (the camera decides by its active mode):
 *  - [ACTION_PRESS] -> tap: photo mode captures, video mode starts/pauses recording
 *
 * Long-press semantics were removed on the user's request (OPPO's native long press is a
 * burst mode, not focus-and-capture, which was confusing); only the tap path remains.
 */
internal object CameraShutterBridge {

    private const val TAG = "LingXiKeyCustom"

    const val ACTION_PRESS = "com.lingxikey.custom.action.CAMERA_PRESS"
    const val ACTION_PING = "com.lingxikey.custom.action.CAMERA_PING"

    private const val CAMERA_PACKAGE = "com.oplus.camera"

    /** The real, on-screen shutter button id (a plain ImageView in the shipped APK). */
    private const val SHUTTER_ID_NAME = "shutter_button"

    /** The lens flip button; we must never land on it by accident. */
    private const val SWITCH_ID_NAME = "switch_camera_button"

    /** How long the synthetic DOWN is held before the matching UP. Short enough to stay a tap. */
    private const val TAP_HOLD_MS = 60L

    @Volatile
    private var shutterView: View? = null

    @Volatile
    private var installed = false

    private val handler = Handler(Looper.getMainLooper())

    /** Registers hooks inside the camera process. Safe to call only from the camera process. */
    fun install(context: Context, classLoader: ClassLoader) {
        if (installed) return
        installed = true

        registerReceiver(context)

        // The camera Activity may inflate its UI after us (or have been restored while the
        // screen was locked), so retry the lookup a few times instead of scanning once.
        listOf(500L, 1500L, 4000L, 9000L).forEach { delay ->
            handler.postDelayed({ refreshShutterView() }, delay)
        }
        XposedBridge.log("$TAG: installed camera touch bridge")
    }

    /** Locates the live shutter button by walking the current activity's view tree. */
    private fun refreshShutterView() {
        val found = findViewById(SHUTTER_ID_NAME) ?: return
        if (shutterView !== found) {
            shutterView = found
            XposedBridge.log("$TAG: camera shutter view resolved -> $found")
        }
    }

    /** Resolves a view by its local resource name across the current activity's decor views. */
    private fun findViewById(name: String): View? {
        val direct = runCatching {
            for (decor in collectDecorViews()) {
                val id = decor.resources.getIdentifier(name, "id", CAMERA_PACKAGE)
                if (id != 0) {
                    decor.findViewById<View>(id)?.let { return@runCatching it }
                }
            }
            null
        }.onXposedFailure("find camera view by id $name").getOrNull()
        if (direct != null) return direct

        // Fall back to a structural scan when the id cannot be resolved.
        return runCatching {
            for (decor in collectDecorViews()) {
                var hit: View? = null
                forEachChild(decor) { child ->
                    if (hit != null) return@forEachChild
                    val childId = child.id
                    if (childId != 0) {
                        val entry = runCatching { child.resources.getResourceEntryName(childId) }.getOrNull()
                        if (entry == name) hit = child
                    }
                }
                if (hit != null) return@runCatching hit
            }
            null
        }.onXposedFailure("find camera view by scan $name").getOrNull()
    }

    /** Every decor view belonging to a live Activity in this process. */
    private fun collectDecorViews(): List<View> {
        val result = mutableListOf<View>()
        runCatching {
            val threadClass = Class.forName("android.app.ActivityThread")
            val thread = threadClass.getMethod("currentActivityThread").invoke(null) ?: return@runCatching
            val activitiesField = threadClass.getDeclaredField("mActivities").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            val activities = activitiesField.get(thread) as? Map<Any, Any> ?: return@runCatching
            for (record in activities.values) {
                val activity = runCatching {
                    record.javaClass.getDeclaredField("activity")
                        .apply { isAccessible = true }
                        .get(record) as? android.app.Activity
                }.getOrNull() ?: continue
                activity.window?.decorView?.let { result.add(it) }
            }
            Unit
        }.onXposedFailure("collect camera decor views")
        return result
    }

    private fun forEachChild(view: View, visit: (View) -> Unit) {
        visit(view)
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) forEachChild(view.getChildAt(i), visit)
        }
    }

    private fun registerReceiver(context: Context) {
        runCatching {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    when (intent?.action) {
                        ACTION_PRESS -> pressShutter()
                        ACTION_PING -> XposedBridge.log("$TAG: camera ping received (module active)")
                    }
                }
            }
            val filter = IntentFilter().apply {
                addAction(ACTION_PRESS)
                addAction(ACTION_PING)
            }
            // Exported so the broadcast can come from system_server (a different uid).
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            XposedBridge.log("$TAG: camera shutter broadcast receiver registered")
        }.onFailure {
            XposedBridge.log("$TAG: camera receiver registration failed: ${it.message}")
        }
    }

    private fun pressShutter() {
        handler.post {
            val view = ensureShutter() ?: return@post
            sendTouch(view)
        }
    }

    /** Returns the shutter view, re-resolving it if the activity was recreated. */
    private fun ensureShutter(): View? {
        shutterView?.let { if (it.isAttachedToWindow) return it }
        shutterView = null
        refreshShutterView()
        val view = shutterView
        if (view == null) {
            XposedBridge.log("$TAG: shutter view not available yet")
        }
        return view
    }

    /**
     * Feeds a real DOWN then (shortly after) an UP into the view's touch pipeline.
     *
     * Both events are dispatched on the decor view using absolute screen coordinates:
     *  - dispatching on the decor (not the button) is what the framework does for a finger;
     *  - absolute coordinates matter because the previous implementation used view-local
     *    coordinates, which is exactly why the press landed on the neighbour button.
     *
     * The UP is scheduled with [Handler.postDelayed] instead of sleeping, since View touch
     * handling must stay on the main thread and blocking it would freeze the shutter.
     */
    private fun sendTouch(view: View) {
        val decor = rootDecorOf(view) ?: view
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val x = location[0] + view.width / 2f
        val y = location[1] + view.height / 2f

        // Guard: never let the computed point fall inside the lens-switch button. Landing
        // there is exactly the failure mode we are fixing (a "shutter press" that flips
        // the camera instead of capturing).
        if (isInsideSwitchButton(decor, x, y)) {
            XposedBridge.log("$TAG: refusing camera touch at ($x, $y): inside switch_camera_button")
            return
        }

        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        decor.dispatchTouchEvent(down)
        down.recycle()

        XposedBridge.log("$TAG: camera touch down at (${x.toInt()}, ${y.toInt()})")

        handler.postDelayed({
            runCatching {
                val upTime = SystemClock.uptimeMillis()
                val up = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, x, y, 0)
                decor.dispatchTouchEvent(up)
                up.recycle()
                XposedBridge.log("$TAG: camera touch up sent")
            }.onXposedFailure("camera touch up")
        }, TAP_HOLD_MS)
    }

    /** True when (x, y) lies within the lens-flip button's on-screen bounds. */
    private fun isInsideSwitchButton(decor: View, x: Float, y: Float): Boolean = runCatching {
        val id = decor.resources.getIdentifier(SWITCH_ID_NAME, "id", CAMERA_PACKAGE)
        val button = if (id != 0) decor.findViewById<View>(id) else null
        if (button == null || !button.isShown) return@runCatching false
        val loc = IntArray(2)
        button.getLocationOnScreen(loc)
        x >= loc[0] && x <= loc[0] + button.width && y >= loc[1] && y <= loc[1] + button.height
    }.onXposedFailure("camera switch-button bounds").getOrDefault(false)

    /** Walks up until the top-most decor view so the event goes through the normal pipeline. */
    private fun rootDecorOf(view: View): View? {
        var current: View? = view
        while (true) {
            val parent = (current?.parent as? View) ?: return current
            current = parent
        }
    }
}
