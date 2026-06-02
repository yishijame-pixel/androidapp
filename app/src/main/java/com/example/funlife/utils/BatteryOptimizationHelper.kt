package com.example.funlife.utils

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast

/**
 * 🔋 电池优化引导工具
 *
 * 解决问题：MIUI/EMUI/ColorOS 等国产 ROM 默认会限制后台 App，
 * 导致 AlarmManager / WorkManager 闹钟在 App 被杀后无法触发。
 *
 * 解决方案：
 *   1) 系统级"忽略电池优化"对话框（Android 原生 API）
 *   2) 各 OEM 自启动管理页面跳转（MIUI 自启动、华为电池优化等）
 */
object BatteryOptimizationHelper {

    private const val TAG = "BatteryOpt"

    /** 检查是否已经忽略电池优化 */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pm.isIgnoringBatteryOptimizations(context.packageName)
            } else true
        } catch (e: Exception) {
            false
        }
    }

    /** 弹出系统对话框，请求忽略电池优化 */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "请求忽略电池优化失败", e)
            // 降级：打开电池优化设置页面
            openBatteryOptimizationSettings(context)
        }
    }

    /**
     * 打开本应用的【通知设置】页（MIUI/原生都支持）
     * 用户在这里可以把「通知重要性」从"默认"改为"重要/紧急"，让 Heads-up 真正弹出
     */
    fun openAppNotificationSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
            Toast.makeText(
                context,
                "请把【通知重要性】设为「重要」或「紧急」，并打开「悬浮通知」",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "打开通知设置失败", e)
        }
    }

    /** 打开系统电池优化列表页面 */
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "打开电池优化设置失败", e)
        }
    }

    /**
     * 跳转到 MIUI 自启动管理页面（如失败则降级到应用详情）
     */
    fun openAutoStartSettings(context: Context) {
        val intents = listOf(
            // 小米 MIUI 自启动管理
            Intent().setComponent(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            ),
            // 华为 EMUI / HarmonyOS
            Intent().setComponent(
                ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                )
            ),
            // OPPO ColorOS
            Intent().setComponent(
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            ),
            // vivo FuntouchOS
            Intent().setComponent(
                ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            ),
            // 三星
            Intent().setComponent(
                ComponentName(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                )
            )
        )
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // 试下一个
            }
        }
        // 全部失败 → 打开应用详情页
        try {
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        } catch (e: Exception) {
            Toast.makeText(context, "请手动打开：设置→应用→FunLife→自启动 / 省电", Toast.LENGTH_LONG).show()
        }
    }
}
