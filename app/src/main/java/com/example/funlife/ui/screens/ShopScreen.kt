// ShopScreen.kt - 精美商城页面
// 功能：
// 1. 展示商城商品列表，支持分类筛选
// 2. 每日免费领取金币（每个账号每天只能领取一次）
// 3. 购买商品功能
// 4. 商品稀有度系统（普通、稀有、史诗、传说）
package com.example.funlife.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.funlife.FunLifeApplication
import com.example.funlife.R
import com.example.funlife.data.model.ShopItem
import com.example.funlife.viewmodel.ShopViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

// 商品数据类
data class ShopProduct(
    val id: Int,
    val nameResId: Int,
    val emoji: String,
    val price: Int,
    val descriptionResId: Int,
    val categoryResId: Int,
    val isHot: Boolean = false,
    val discount: Int? = null,
    val isNew: Boolean = false,
    val stock: Int? = null,
    val rarity: ProductRarity = ProductRarity.COMMON,
    // 🔥 新增：支持直接使用字符串（用于数据库商品）
    val nameText: String? = null,
    val descriptionText: String? = null,
    val dbItem: ShopItem? = null
)


// 商品稀有度
enum class ProductRarity(val colorValue: Long, val labelResId: Int) {
    COMMON(0xFF9E9E9E, R.string.shop_rarity_common),
    RARE(0xFF2196F3, R.string.shop_rarity_rare),
    EPIC(0xFF9C27B0, R.string.shop_rarity_epic),
    LEGENDARY(0xFFFF9800, R.string.shop_rarity_legendary);
    
    val color: Color get() = Color(colorValue)
}

// 商品分类
enum class ShopCategory(val displayNameResId: Int, val icon: String) {
    ALL(R.string.shop_category_all, "🛍️"),
    PROPS(R.string.shop_category_props, "🎁"),
    SKINS(R.string.shop_category_skins, "🎨"),
    DECORATIONS(R.string.shop_category_decorations, "✨"),
    EFFECTS(R.string.shop_category_effects, "🎆")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    shopViewModel: ShopViewModel = viewModel(),
    vipViewModel: com.example.funlife.viewmodel.VipViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val database = (context.applicationContext as com.example.funlife.FunLifeApplication).database
    val inventoryDao = remember { database.inventoryDao() }
    
    // 获取VIP状态
    val authViewModel: com.example.funlife.viewmodel.AuthViewModel = viewModel()
    val userSession = authViewModel.getCurrentSession()
    
    LaunchedEffect(userSession) {
        userSession?.userId?.let { userId ->
            vipViewModel.setUserId(userId)
        }
    }
    val userVip by vipViewModel.userVip.collectAsState()
    val vipLevel = userVip?.getCurrentVipLevel() ?: com.example.funlife.data.model.VipLevel.NORMAL
    
    // 价格倍数：VIP用户1倍，普通用户100倍
    val priceMultiplier = if (vipLevel.level > 0) 1 else 100
    
    // 获取用户已购买的物品列表
    val purchasedItems by inventoryDao.getAllItems().collectAsState(initial = emptyList())
    val purchasedPanelIds = remember(purchasedItems) {
        purchasedItems
            .filter { it.itemId.startsWith("panel_") }
            .map { it.itemId.removePrefix("panel_") }
            .toSet()
    }
    val purchasedButtonIds = remember(purchasedItems) {
        purchasedItems
            .filter { it.itemId.startsWith("button_") }
            .map { it.itemId.removePrefix("button_") }
            .toSet()
    }
    
    val userCoins by shopViewModel.userCoins.collectAsState()
    val canClaimFreeCoins by shopViewModel.canClaimFreeCoins.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var selectedShopItem by remember { mutableStateOf<ShopItem?>(null) }
    var showShopItemDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(ShopCategory.ALL) }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    
    val claimSuccessMsg = stringResource(R.string.shop_claim_success)
    val claimedTodayMsg = stringResource(R.string.shop_claimed_today)
    
    // 🔥 从数据库读取商品
    val dbShopItems by shopViewModel.shopItems.collectAsState()
    
    // 🔥 调试日志
    LaunchedEffect(dbShopItems) {
        android.util.Log.d("ShopScreen", "=== Database Shop Items ===")
        android.util.Log.d("ShopScreen", "Total items: ${dbShopItems.size}")
        dbShopItems.forEach { item ->
            android.util.Log.d("ShopScreen", "Item: ${item.name} (type=${item.type}, value=${item.value}, price=${item.price})")
        }
        val buttonSkins = dbShopItems.filter { it.type == "button_skin" }
        android.util.Log.d("ShopScreen", "Button skins: ${buttonSkins.size}")
    }
    
    // 🔥 只使用数据库商品，移除所有硬编码商品
    val allProductsWithDb = remember(dbShopItems) {
        android.util.Log.d("ShopScreen", "=== Converting DB Items to Products ===")
        android.util.Log.d("ShopScreen", "Total DB items: ${dbShopItems.size}")
        
        // 🔥 按assetPath去重（防止重复商品）
        val uniqueItems = dbShopItems
            .groupBy { it.assetPath ?: "${it.type}_${it.id}" }
            .mapValues { it.value.first() }  // 每个assetPath只保留第一个
            .values
            .toList()
        
        android.util.Log.d("ShopScreen", "After deduplication: ${uniqueItems.size} items")
        
        uniqueItems.mapNotNull { item ->
            // 根据商品类型确定分类
            val categoryResId = when (item.type) {
                "avatar_frame" -> R.string.shop_category_decorations  // 🔥 新增：头像框分类
                "button_skin" -> R.string.shop_category_skins
                "anniversary_frame" -> R.string.shop_category_decorations
                "makeup_card", "coins", "spin_chance", "pet_food", "pet_toy", "lucky_charm", "exp_boost" -> R.string.shop_category_props
                "theme" -> R.string.shop_category_skins
                "badge", "vip" -> R.string.shop_category_decorations
                else -> R.string.shop_category_props
            }
            
            // 应用VIP价格倍数（免费商品除外）
            val actualPrice = if (item.price == 0) 0 else item.price * priceMultiplier
            
            // 根据实际价格和类型确定稀有度
            val rarity = when {
                actualPrice == 0 -> ProductRarity.COMMON
                actualPrice <= 50 -> ProductRarity.COMMON
                actualPrice <= 150 -> ProductRarity.RARE
                actualPrice <= 500 -> ProductRarity.EPIC
                else -> ProductRarity.LEGENDARY
            }
            
            // 🔥 生成唯一ID（使用数据库ID确保唯一性）
            val productId = when (item.type) {
                "avatar_frame" -> 4000 + item.id  // 🔥 头像框ID范围
                "button_skin" -> 1000 + item.value
                "anniversary_frame" -> 2000 + item.value
                else -> 3000 + item.id
            }
            
            android.util.Log.d("ShopScreen", "Converting: ${item.name} (type=${item.type}, dbId=${item.id}, productId=$productId, assetPath=${item.assetPath})")
            
            ShopProduct(
                id = productId,
                nameResId = categoryResId, // 占位，实际使用 nameText
                emoji = item.icon,
                price = actualPrice,
                descriptionResId = categoryResId, // 占位，实际使用 descriptionText
                categoryResId = categoryResId,
                isNew = item.type == "button_skin" || item.type == "anniversary_frame" || item.type == "avatar_frame",  // 🔥 新增
                rarity = rarity,
                nameText = item.name,
                descriptionText = item.description,
                dbItem = item
            )
        }
    }
    
    android.util.Log.d("ShopScreen", "Total products: ${allProductsWithDb.size}, Anniversary frame products: ${allProductsWithDb.count { it.dbItem?.type == "anniversary_frame" }}")
    
    val filteredProducts = remember(selectedCategory, allProductsWithDb) {
        val filtered = if (selectedCategory == ShopCategory.ALL) {
            allProductsWithDb
        } else {
            allProductsWithDb.filter { it.categoryResId == selectedCategory.displayNameResId }
        }
        android.util.Log.d("ShopScreen", "Selected category: ${selectedCategory.name}, Filtered products: ${filtered.size}")
        filtered.forEach { product ->
            android.util.Log.d("ShopScreen", "  - ${product.nameText ?: "N/A"} (categoryResId=${product.categoryResId}, type=${product.dbItem?.type})")
        }
        filtered
    }

    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF3E0),
                            Color(0xFFFFE0B2),
                            Color(0xFFFFCC80),
                            Color(0xFFFFB74D)
                        )
                    )
                )
        ) {
            BackgroundStars()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
            ) {
                ShopTopBar(
                    userCoins = userCoins?.coins ?: 0,
                    onNavigateBack = onNavigateBack
                )
                
                CategorySelector(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
                
                // VIP价格提示卡片
                if (vipLevel.level > 0) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(vipLevel.color).copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(vipLevel.icon, fontSize = 24.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "VIP专属优惠",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(vipLevel.color)
                                )
                                Text(
                                    "所有商品仅需1金币！",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFE0B2)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💡", fontSize = 24.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "开通VIP享受超值优惠",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "VIP用户商品仅需1金币，立省99%！",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        // 判断商品是否已购买（只检查数据库商品）
                        val isPurchased = when {
                            product.id >= 4000 && product.id < 5000 -> {
                                // 🔥 头像框商品
                                val frameId = product.dbItem?.id ?: 0
                                purchasedItems.any { it.itemId == "avatar_frame_$frameId" }
                            }
                            product.id >= 1000 && product.id < 2000 -> {
                                // 按钮皮肤商品
                                val buttonIndex = product.dbItem?.value ?: 0
                                "pf_$buttonIndex" in purchasedButtonIds
                            }
                            product.id >= 2000 && product.id < 3000 -> {
                                // 纪念日相框商品
                                val frameIndex = product.dbItem?.value ?: 0
                                purchasedItems.any { it.itemId == "jinian_card_$frameIndex" }
                            }
                            else -> false
                        }
                        
                        ProductCard(
                            product = product,
                            userCoins = userCoins?.coins ?: 0,
                            canClaim = canClaimFreeCoins,
                            isPurchased = isPurchased,
                            onClick = {
                                if (product.price == 0 && product.dbItem?.type == "coins") {
                                    // 免费领取金币
                                    if (canClaimFreeCoins) {
                                        scope.launch {
                                            val success = shopViewModel.claimFreeCoins()
                                            if (success) {
                                                snackbarHostState.showSnackbar(claimSuccessMsg)
                                            } else {
                                                snackbarHostState.showSnackbar(claimedTodayMsg)
                                            }
                                        }
                                    }
                                } else if (product.dbItem != null) {
                                    // 所有数据库商品统一处理
                                    selectedShopItem = product.dbItem
                                    showShopItemDialog = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    val purchaseSuccessMsg = stringResource(R.string.shop_purchase_success)
    val purchaseFailedMsg = stringResource(R.string.shop_purchase_failed)
    
    // 🔥 数据库商品购买对话框
    if (showShopItemDialog && selectedShopItem != null) {
        ShopItemPurchaseDialog(
            item = selectedShopItem!!,
            userCoins = userCoins?.coins ?: 0,
            onDismiss = { showShopItemDialog = false },
            onConfirm = {
                scope.launch {
                    val database = (context.applicationContext as com.example.funlife.FunLifeApplication).database
                    val inventoryDao = database.inventoryDao()
                    
                    // 🔥 检查是否已拥有该物品（防重复购买）
                    val itemId = when (selectedShopItem!!.type) {
                        "avatar_frame" -> "avatar_frame_${selectedShopItem!!.id}"  // 🔥 新增
                        "button_skin" -> "button_pf_${selectedShopItem!!.value}"
                        "anniversary_frame" -> "jinian_card_${selectedShopItem!!.value}"
                        else -> "${selectedShopItem!!.type}_${selectedShopItem!!.id}"
                    }
                    
                    val alreadyOwned = inventoryDao.getItemByItemId(1L, itemId) != null
                    
                    if (alreadyOwned) {
                        snackbarHostState.showSnackbar("您已拥有该物品")
                        showShopItemDialog = false
                        return@launch
                    }
                    
                    val success = shopViewModel.purchaseItem(selectedShopItem!!.price)
                    if (success) {
                        // 根据商品类型创建库存物品
                        val itemType = when (selectedShopItem!!.type) {
                            "avatar_frame" -> com.example.funlife.data.model.InventoryItemType.AVATAR_FRAME  // 🔥 新增
                            "button_skin" -> com.example.funlife.data.model.InventoryItemType.BUTTON_SKIN
                            "anniversary_frame" -> com.example.funlife.data.model.InventoryItemType.ANNIVERSARY_FRAME
                            "makeup_card", "spin_chance", "pet_food", "pet_toy", "lucky_charm", "exp_boost" -> 
                                com.example.funlife.data.model.InventoryItemType.CONSUMABLE
                            "theme" -> com.example.funlife.data.model.InventoryItemType.DECORATION
                            "badge", "vip" -> com.example.funlife.data.model.InventoryItemType.DECORATION
                            else -> com.example.funlife.data.model.InventoryItemType.CONSUMABLE
                        }
                        
                        val inventoryItem = com.example.funlife.data.model.InventoryItem(
                            userId = 1L,
                            itemId = itemId,
                            itemName = selectedShopItem!!.name,
                            itemType = itemType,
                            itemRarity = com.example.funlife.data.model.ItemRarity.COMMON,
                            iconEmoji = selectedShopItem!!.icon,
                            description = selectedShopItem!!.description,
                            quantity = selectedShopItem!!.value,
                            isUsable = true,
                            effectValue = 0,
                            purchasePrice = selectedShopItem!!.price,
                            obtainedTime = System.currentTimeMillis()
                        )
                        inventoryDao.insertItem(inventoryItem)
                        
                        showShopItemDialog = false
                        showSuccessAnimation = true
                        delay(2000)
                        showSuccessAnimation = false
                        snackbarHostState.showSnackbar(purchaseSuccessMsg)
                    } else {
                        snackbarHostState.showSnackbar(purchaseFailedMsg)
                    }
                }
            }
        )
    }
    
    if (showSuccessAnimation) {
        PurchaseSuccessAnimation()
    }
}


@Composable
fun BackgroundStars() {
    // 移除动画以提升性能
}

@Composable
fun ShopTopBar(
    userCoins: Int,
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.ArrowBack, 
                stringResource(R.string.shop_back), 
                tint = Color(0xFFE65100),
                modifier = Modifier.size(22.dp)
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("🛍", fontSize = 22.sp)
            Text(
                stringResource(R.string.shop_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE65100)
            )
        }
        
        val coinScale by rememberInfiniteTransition(label = "coin").animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "coin_scale"
        )
        
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFD700), Color(0xFFFF8F00))
                ),
                shape = RoundedCornerShape(18.dp)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text("💰", fontSize = 16.sp, modifier = Modifier.scale(coinScale))
                Text("$userCoins", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6F00))
            }
        }
    }
}

@Composable
fun CategorySelector(
    selectedCategory: ShopCategory,
    onCategorySelected: (ShopCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ShopCategory.values().forEach { category ->
            val isSelected = category == selectedCategory
            
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "category_scale"
            )
            
            Card(
                modifier = Modifier
                    .weight(1f)
                    .scale(scale)
                    .clickable { onCategorySelected(category) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFFFF6F00) else Color.White.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(category.icon, fontSize = 22.sp)
                    Text(
                        stringResource(category.displayNameResId),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF424242),
                        maxLines = 1
                    )
                }
            }
        }
    }
}


@Composable
fun ProductCard(
    product: ShopProduct,
    userCoins: Int,
    canClaim: Boolean = true,
    isPurchased: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (product.rarity == ProductRarity.EPIC) {
                        Modifier.border(
                            width = 2.dp,
                            color = product.rarity.color.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(24.dp)
                        )
                    } else Modifier
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // 徽章行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        if (isPurchased) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF2196F3),
                                shadowElevation = 0.dp
                            ) {
                                Text("✓", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        if (product.isHot) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFF5722),
                                shadowElevation = 0.dp
                            ) {
                                Text("🔥", modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), fontSize = 9.sp)
                            }
                        }
                        if (product.isNew) {
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF4CAF50), shadowElevation = 0.dp) {
                                Text("NEW", modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        if (product.discount != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE91E63),
                                shadowElevation = 0.dp
                            ) {
                                Text("-${product.discount}%", modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        if (product.stock != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFF9800).copy(alpha = 0.9f),
                                shadowElevation = 0.dp
                            ) {
                                Text("×${product.stock}", modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(4.dp))
                
                // 图片区域 - 占据最大空间
                Box(
                    contentAlignment = Alignment.Center, 
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    when {
                        // 🔥 头像框 (ID >= 4000，数据库商品)
                        product.id >= 4000 && product.id < 5000 && product.dbItem != null -> {
                            val context = LocalContext.current
                            val assetPath = product.dbItem.assetPath
                            val frameBitmap = remember(product.id) {
                                try {
                                    if (assetPath != null) {
                                        context.assets.open(assetPath).use { inputStream ->
                                            android.graphics.BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                                        }
                                    } else {
                                        null
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("ShopScreen", "Failed to load avatar frame: $assetPath", e)
                                    null
                                }
                            }
                            
                            if (frameBitmap != null) {
                                Image(
                                    bitmap = frameBitmap,
                                    contentDescription = product.nameText ?: "头像框",
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text(product.emoji, fontSize = 64.sp)
                            }
                        }
                        // 结算面板皮肤 (ID 21-25)
                        product.id in 21..25 -> {
                            val context = LocalContext.current
                            val panelIndex = product.id - 20
                            val panelBitmap = remember(product.id) {
                                try {
                                    context.assets.open("login/js_$panelIndex.png").use { inputStream ->
                                        android.graphics.BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            
                            if (panelBitmap != null) {
                                Image(
                                    bitmap = panelBitmap,
                                    contentDescription = stringResource(product.nameResId),
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text(product.emoji, fontSize = 64.sp)
                            }
                        }
                        // 纪念日相框 (ID >= 2000，数据库商品)
                        product.id >= 2000 && product.dbItem != null -> {
                            val context = LocalContext.current
                            val frameIndex = product.dbItem.value
                            val frameBitmap = remember(product.id) {
                                try {
                                    context.assets.open("login/jinian_card_$frameIndex.png").use { inputStream ->
                                        android.graphics.BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("ShopScreen", "Failed to load frame image: jinian_card_$frameIndex.png", e)
                                    null
                                }
                            }
                            
                            if (frameBitmap != null) {
                                Image(
                                    bitmap = frameBitmap,
                                    contentDescription = product.nameText ?: "纪念日相框",
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text(product.emoji, fontSize = 64.sp)
                            }
                        }
                        // 按钮皮肤 (ID >= 1000，数据库商品)
                        product.id >= 1000 && product.dbItem != null -> {
                            val context = LocalContext.current
                            val buttonIndex = product.dbItem.value
                            val buttonBitmap = remember(product.id) {
                                try {
                                    context.assets.open("login/pf_$buttonIndex.png").use { inputStream ->
                                        android.graphics.BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            
                            if (buttonBitmap != null) {
                                Image(
                                    bitmap = buttonBitmap,
                                    contentDescription = product.nameText ?: "按钮皮肤",
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text(product.emoji, fontSize = 64.sp)
                            }
                        }
                        // 按钮皮肤 (ID 100-125 对应 pf_1到pf_26)
                        product.id in 100..125 -> {
                            val context = LocalContext.current
                            val buttonIndex = product.id - 99  // 100->1, 101->2, ..., 125->26
                            val buttonBitmap = remember(product.id) {
                                try {
                                    context.assets.open("login/pf_$buttonIndex.png").use { inputStream ->
                                        android.graphics.BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            
                            if (buttonBitmap != null) {
                                Image(
                                    bitmap = buttonBitmap,
                                    contentDescription = "按钮皮肤 $buttonIndex",
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text(product.emoji, fontSize = 64.sp)
                            }
                        }
                        else -> {
                            Text(product.emoji, fontSize = 64.sp)
                        }
                    }
                }
                
                // 稀有度标签
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = product.rarity.color.copy(alpha = 0.2f),
                    modifier = Modifier.border(1.dp, product.rarity.color, RoundedCornerShape(10.dp))
                ) {
                    Text(
                        stringResource(product.rarity.labelResId),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = product.rarity.color
                    )
                }
                
                Spacer(Modifier.height(2.dp))
                
                // 商品名称
                Text(
                    product.nameText ?: stringResource(product.nameResId),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                )
                
                Spacer(Modifier.height(4.dp))
                
                // 按钮
                if (product.price == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (canClaim) Color(0xFF4CAF50) else Color(0xFF9E9E9E))
                            .clickable(onClick = onClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(if (canClaim) R.string.shop_button_free else R.string.shop_claimed_today),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFFF6F00),
                                        Color(0xFFFF8F00),
                                        Color(0xFFFFB300)
                                    )
                                )
                            )
                            .clickable(onClick = onClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💰", fontSize = 13.sp)
                            Spacer(Modifier.width(3.dp))
                            Text("${product.price}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun PurchaseDialog(
    product: ShopProduct,
    userCoins: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val scale by rememberInfiniteTransition(label = "dialog").animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emoji_scale"
    )
    
    val rotation by rememberInfiniteTransition(label = "dialog_rotation").animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emoji_rotation"
    )
    
    val afterBalance = userCoins - product.price
    val canAfford = afterBalance >= 0
    
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                // 如果是面板商品（ID 21-25），显示真实图片
                if (product.id in 21..25) {
                    val panelIndex = product.id - 20
                    val panelBitmap = remember(product.id) {
                        try {
                            context.assets.open("login/js_$panelIndex.png").use { inputStream ->
                                android.graphics.BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    if (panelBitmap != null) {
                        Image(
                            bitmap = panelBitmap,
                            contentDescription = stringResource(product.nameResId),
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(
                            product.emoji,
                            fontSize = 64.sp
                        )
                    }
                } else {
                    Text(
                        product.emoji,
                        fontSize = 64.sp
                    )
                }
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    stringResource(R.string.shop_dialog_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = product.rarity.color.copy(alpha = 0.2f),
                    modifier = Modifier.border(
                        width = 1.5.dp,
                        color = product.rarity.color,
                        shape = RoundedCornerShape(10.dp)
                    )
                ) {
                    Text(
                        stringResource(product.rarity.labelResId),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = product.rarity.color
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            stringResource(product.nameResId),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            stringResource(product.descriptionResId),
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Divider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Color.Gray.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.shop_dialog_price),
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE0B2)),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("💰", fontSize = 14.sp)
                                Text(
                                    "${product.price}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF6F00)
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.shop_dialog_current_coins),
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("💰", fontSize = 14.sp)
                                Text(
                                    "$userCoins",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1976D2)
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.shop_dialog_after_purchase),
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (canAfford) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("💰", fontSize = 14.sp)
                                Text(
                                    "$afterBalance",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (canAfford) Color(0xFF4CAF50) else Color(0xFFE53935)
                                )
                            }
                        }
                    }
                }
                
                // 金币不足警告（已禁用）
                // if (!canAfford && com.example.funlife.config.VipConfig.SHOW_INSUFFICIENT_COINS_WARNING) {
                //     Card(
                //         shape = RoundedCornerShape(12.dp),
                //         colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                //         elevation = CardDefaults.cardElevation(0.dp),
                //         modifier = Modifier.border(
                //             width = 1.5.dp,
                //             color = Color(0xFFE53935),
                //             shape = RoundedCornerShape(12.dp)
                //         )
                //     ) {
                //         Row(
                //             modifier = Modifier.fillMaxWidth().padding(10.dp),
                //             horizontalArrangement = Arrangement.spacedBy(6.dp),
                //             verticalAlignment = Alignment.CenterVertically
                //         ) {
                //             Text("⚠️", fontSize = 16.sp)
                //             Text(
                //                 "金币不足，还需 ${product.price - userCoins} 金币",
                //                 fontSize = 13.sp,
                //                 color = Color(0xFFE53935),
                //                 fontWeight = FontWeight.Bold
                //             )
                //         }
                //     }
                // }
                
                if (product.stock != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚠️", fontSize = 14.sp)
                            Text(
                                stringResource(R.string.shop_dialog_limited_warning, product.stock),
                                fontSize = 12.sp,
                                color = Color(0xFFFF6F00),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = canAfford,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6F00),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFBDBDBD),
                    disabledContentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    hoveredElevation = 0.dp
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("✨", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.shop_dialog_confirm),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, Color(0xFFE0E0E0)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent
                )
            ) {
                Text(
                    stringResource(R.string.shop_dialog_cancel),
                    fontSize = 14.sp,
                    color = Color(0xFF757575),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}

@Composable
fun PurchaseSuccessAnimation() {
    var visible by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(2000)
        visible = false
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val scale by rememberInfiniteTransition(label = "success").animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "success_scale"
                    )
                    
                    Text("✨", fontSize = 72.sp, modifier = Modifier.scale(scale))
                    Text(stringResource(R.string.shop_purchase_success_title), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    Text(stringResource(R.string.shop_purchase_success_message), fontSize = 16.sp, color = Color.Gray)
                }
            }
        }
    }
}


// 🔥 数据库商品购买对话框
@Composable
fun ShopItemPurchaseDialog(
    item: ShopItem,
    userCoins: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val afterBalance = userCoins - item.price
    val canAfford = afterBalance >= 0
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                // 根据商品类型显示不同的图片
                val imageBitmap = remember(item.id, item.type) {
                    try {
                        val imagePath = when (item.type) {
                            "avatar_frame" -> item.assetPath  // 🔥 新增：头像框使用assetPath
                            "button_skin" -> "login/pf_${item.value}.png"
                            "anniversary_frame" -> "login/jinian_card_${item.value}.png"
                            else -> null
                        }
                        
                        imagePath?.let { path ->
                            context.assets.open(path).use { inputStream ->
                                android.graphics.BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ShopItemPurchaseDialog", "Failed to load image for ${item.type}", e)
                        null
                    }
                }
                
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = item.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(item.icon, fontSize = 64.sp)
                }
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    stringResource(R.string.shop_dialog_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ProductRarity.COMMON.color.copy(alpha = 0.2f),
                    modifier = Modifier.border(
                        width = 1.5.dp,
                        color = ProductRarity.COMMON.color,
                        shape = RoundedCornerShape(10.dp)
                    )
                ) {
                    Text(
                        stringResource(ProductRarity.COMMON.labelResId),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProductRarity.COMMON.color
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            item.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            item.description,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Divider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Color.Gray.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.shop_dialog_price),
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE0B2)),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("💰", fontSize = 14.sp)
                                Text(
                                    "${item.price}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF6F00)
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.shop_dialog_current_coins),
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("💰", fontSize = 14.sp)
                                Text(
                                    "$userCoins",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1976D2)
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.shop_dialog_after_purchase),
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (canAfford) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("💰", fontSize = 14.sp)
                                Text(
                                    "$afterBalance",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (canAfford) Color(0xFF4CAF50) else Color(0xFFE53935)
                                )
                            }
                        }
                    }
                }
                
                // 金币不足警告（已禁用）
                // if (!canAfford && com.example.funlife.config.VipConfig.SHOW_INSUFFICIENT_COINS_WARNING) {
                //     Card(
                //         shape = RoundedCornerShape(12.dp),
                //         colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                //         elevation = CardDefaults.cardElevation(0.dp),
                //         modifier = Modifier.border(
                //             width = 1.5.dp,
                //             color = Color(0xFFE53935),
                //             shape = RoundedCornerShape(12.dp)
                //         )
                //     ) {
                //         Row(
                //             modifier = Modifier.fillMaxWidth().padding(10.dp),
                //             horizontalArrangement = Arrangement.spacedBy(6.dp),
                //             verticalAlignment = Alignment.CenterVertically
                //         ) {
                //             Text("⚠️", fontSize = 16.sp)
                //             Text(
                //                 "金币不足，还需 ${item.price - userCoins} 金币",
                //                 fontSize = 13.sp,
                //                 color = Color(0xFFE53935),
                //                 fontWeight = FontWeight.Bold
                //             )
                //         }
                //     }
                // }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = canAfford,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6F00),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFBDBDBD),
                    disabledContentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    hoveredElevation = 0.dp
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("✨", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.shop_dialog_confirm),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, Color(0xFFE0E0E0)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent
                )
            ) {
                Text(
                    stringResource(R.string.shop_dialog_cancel),
                    fontSize = 14.sp,
                    color = Color(0xFF757575),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}
