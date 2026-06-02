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

        // ─── 多层渐变背景：上层柔光 + 下层主色，营造立体感 ───
        val mainGradient = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.parseColor("#FFB6C8"),  // 浅樱粉
                Color.parseColor("#FF7AB6"),  // 玫瑰粉
                Color.parseColor("#E91E63"),  // 主粉
                Color.parseColor("#C2185B")   // 深酒红
            )
        ).apply {
            cornerRadius = dp(28).toFloat()
            setStroke(dp(2), Color.parseColor("#80FFFFFF"))  // 白色描边
        }

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            background = mainGradient
            setPadding(dp(14), dp(14), dp(10), dp(14))
            elevation = dp(20).toFloat()  // 更深阴影
            val marginLp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dp(12); rightMargin = dp(12)
            }
            layoutParams = marginLp
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
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

        // ─── 铃铛：双层背景（外圈白色光晕 + 内圈半透明） ───
        val bellWrap = FrameLayout(ctx).apply {
            val outerLp = LinearLayout.LayoutParams(dp(48), dp(48)).apply { rightMargin = dp(12) }
            layoutParams = outerLp
        }
        val bellOuterRing = View(ctx).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#33FFFFFF"))
                setStroke(dp(1), Color.parseColor("#66FFFFFF"))
            }
            layoutParams = FrameLayout.LayoutParams(dp(48), dp(48))
        }
        val bell = TextView(ctx).apply {
            text = "🔔"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(Color.parseColor("#FFFFFFFF"), Color.parseColor("#FFFFE4EC"))
            }
            gravity = Gravity.CENTER
            val lp = FrameLayout.LayoutParams(dp(38), dp(38)).apply {
                gravity = Gravity.CENTER
            }
            layoutParams = lp
        }
        bellWrap.addView(bellOuterRing)
        bellWrap.addView(bell)
        container.addView(bellWrap)

        // ─── 文字列 ───
        val textCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        // 顶部一行：标题 + NEW 徽章
        val titleRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(ctx).apply {
            text = "今日纪念日提醒"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setShadowLayer(dp(2).toFloat(), 0f, dp(1).toFloat(), Color.parseColor("#80AD1457"))
        }
        val newBadge = TextView(ctx).apply {
            text = "NEW"
            setTextColor(Color.parseColor("#E91E63"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(6), dp(2), dp(6), dp(2))
            background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(Color.WHITE)
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = dp(8) }
            layoutParams = lp
        }
        titleRow.addView(title)
        titleRow.addView(newBadge)
        textCol.addView(titleRow)

        // 副标题
        val subtitle = TextView(ctx).apply {
            text = "✨ 你有 $count 个值得庆祝的日子，点击查看 💗"
            setTextColor(Color.parseColor("#F0FFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, dp(2), 0, 0)
        }
        textCol.addView(subtitle)
        container.addView(textCol)

        // ─── 关闭按钮（更精致：圆形 + 半透明 + 描边） ───
        val closeBtn = TextView(ctx).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#33000000"))
                setStroke(dp(1), Color.parseColor("#66FFFFFF"))
            }
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(dp(30), dp(30)).apply { leftMargin = dp(6) }
            layoutParams = lp
            setOnClickListener { stopSelf() }
        }
        container.addView(closeBtn)

        // 外层 FrameLayout 让 margin 生效 + 加顶部高光条
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
