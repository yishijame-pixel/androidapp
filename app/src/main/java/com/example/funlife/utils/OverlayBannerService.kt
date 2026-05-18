package com.example.funlife.utils

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.funlife.MainActivity

/**
 * 🎀 全局悬浮窗服务 - 在其他 App / 桌面之上显示纪念日提醒
 * 需要 SYSTEM_ALERT_WINDOW 权限（用户需在系统设置允许"在其他应用上层显示"）
 */
class OverlayBannerService : Service() {

    private var windowManager: WindowManager? = null
    private var bannerView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val count = intent?.getIntExtra(EXTRA_COUNT, 1) ?: 1
        showBanner(count)
        return START_NOT_STICKY
    }

    private fun showBanner(count: Int) {
        if (!hasOverlayPermission(this)) {
            stopSelf()
            return
        }
        try {
            removeBanner()
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val view = buildView(count)
            bannerView = view
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP
                y = dp(28)
            }
            windowManager?.addView(view, params)
        } catch (e: Exception) {
            android.util.Log.e("OverlayBanner", "show failed", e)
            stopSelf()
        }
    }

    private fun buildView(count: Int): View {
        val ctx = this
        // 渐变粉色背景
        val bg = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                Color.parseColor("#FFCAD4"),
                Color.parseColor("#FF80AB"),
                Color.parseColor("#EC407A"),
                Color.parseColor("#D81B60")
            )
        ).apply {
            cornerRadius = dp(50).toFloat()
        }
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            background = bg
            setPadding(dp(16), dp(12), dp(12), dp(12))
            elevation = dp(12).toFloat()
            // 留两侧边距
            val marginLp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dp(10); rightMargin = dp(10)
            }
            layoutParams = marginLp
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            // 点击 → 跳转 App
            setOnClickListener {
                AnniversaryReminderManager.dismissInAppBanner()
                AnniversaryReminderManager.stopAlarm(ctx)
                val launchIntent = Intent(ctx, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(launchIntent)
                stopSelf()
            }
        }

        // 铃铛圆
        val bell = TextView(ctx).apply {
            text = "🔔"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            val bellBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#4DFFFFFF"))
            }
            background = bellBg
            val lp = LinearLayout.LayoutParams(dp(40), dp(40)).apply { rightMargin = dp(10) }
            layoutParams = lp
            gravity = Gravity.CENTER
        }
        container.addView(bell)

        val textCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        val title = TextView(ctx).apply {
            text = "今日纪念日提醒 🎀"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val subtitle = TextView(ctx).apply {
            text = "✨ 你有 $count 个值得庆祝的日子！点击进入 💗"
            setTextColor(Color.parseColor("#F0FFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        textCol.addView(title)
        textCol.addView(subtitle)
        container.addView(textCol)

        // 关闭按钮
        val closeBtn = TextView(ctx).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            val closeBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#4DFFFFFF"))
            }
            background = closeBg
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(dp(28), dp(28)).apply { leftMargin = dp(8) }
            layoutParams = lp
            setOnClickListener {
                stopSelf()
            }
        }
        container.addView(closeBtn)

        // 用 FrameLayout 包一下让 margin 生效
        val outer = FrameLayout(ctx)
        outer.addView(container)
        return outer
    }

    private fun removeBanner() {
        try {
            bannerView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {}
        bannerView = null
    }

    override fun onDestroy() {
        super.onDestroy()
        removeBanner()
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()

    companion object {
        private const val EXTRA_COUNT = "count"

        fun hasOverlayPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        }

        fun start(context: Context, count: Int) {
            if (!hasOverlayPermission(context)) return
            val intent = Intent(context, OverlayBannerService::class.java).apply {
                putExtra(EXTRA_COUNT, count)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, OverlayBannerService::class.java))
            } catch (_: Exception) {}
        }

        fun requestOverlayPermission(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:${context.packageName}")
                ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                try { context.startActivity(intent) } catch (_: Exception) {}
            }
        }
    }
}
