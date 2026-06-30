// OpenSourceCreditsScreen.kt — 开源许可与 SuperTux 致谢
package com.example.funlife.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceCreditsScreen(onNavigateBack: () -> Unit) {
    val appName = LocalContext.current.getString(R.string.app_name)
    val scroll = rememberScrollState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("开源许可") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF7F8FA)),
            )
        },
        containerColor = Color(0xFFF7F8FA),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            CreditSection(
                title = "SuperTux 游戏素材",
                body = """
                    横版冒险「SuperTux 改编模式」关卡、贴图、音效与角色素材来源于 SuperTux 项目。

                    • 项目：https://github.com/SuperTux/supertux
                    • 许可：GPL v2+ 与 CC-BY-SA 3.0（双选，见上游 data/AUTHOR)
                    • 改编：$appName 离线转换为 platformer_supertux 资源包，非 1:1 引擎复刻
                """.trimIndent(),
            )
            CreditSection(
                title = "$appName 改编资源",
                body = """
                    • platformer_supertux — 107 关（World1/2/Bonus/Redmond）
                    • platformer_sfx — 横版音效精选
                    • 每个 zip 内含 LICENSE.txt 与 ATTRIBUTION.json
                """.trimIndent(),
            )
            CreditSection(
                title = "其他开源组件",
                body = """
                    $appName 还使用 Android Jetpack、Compose、Gson、PocketBase 客户端等开源库。
                    完整依赖列表见 Gradle 构建配置与各库官方许可。
                """.trimIndent(),
            )
            Text(
                "如有遗漏或授权疑问，请联系应用开发者。",
                fontSize = 13.sp,
                color = Color.Gray,
            )
        }
    }
}

@Composable
private fun CreditSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Text(body, fontSize = 14.sp, lineHeight = 22.sp, color = Color(0xFF424242))
    }
}
