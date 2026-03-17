// FunLifeApplication.kt - 应用程序类
package com.example.funlife

import android.app.Application
import com.example.funlife.utils.AuditLogger

class FunLifeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化审计日志系统
        AuditLogger.initialize(this)
    }
}
