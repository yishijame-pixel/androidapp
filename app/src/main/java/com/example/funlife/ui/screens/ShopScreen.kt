// ShopScreen.kt - 精美商城页面
// 功能：
// 1. 展示商城商品列表，支持分类筛选
// 2. 每日免费领取金币（每个账号每天只能领取一次）
// 3. 购买商品功能
// 4. 商品稀有度系统（普通、稀有、史诗、传说）
package com.example.funlife.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val rarity: ProductRarity = ProductRarity.COMMON
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
    onNavigateBack: () -> Unit = {}
) {
    val userCoins by shopViewModel.userCoins.collectAsState()
    val canClaimFreeCoins by shopViewModel.canClaimFreeCoins.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var selectedProduct by remember { mutableStateOf<ShopProduct?>(null) }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(ShopCategory.ALL) }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    
    val claimSuccessMsg = stringResource(R.string.shop_claim_success)
    val claimedTodayMsg = stringResource(R.string.shop_claimed_today)
    
    val allProducts = remember {
        listOf(
            ShopProduct(1, R.string.shop_product_coin_pack, "💰", 0, R.string.shop_desc_coin_pack, R.string.shop_category_props, isHot = true, rarity = ProductRarity.COMMON),
            ShopProduct(2, R.string.shop_product_lucky_star, "⭐", 300, R.string.shop_desc_lucky_star, R.string.shop_category_props, rarity = ProductRarity.RARE),
            ShopProduct(3, R.string.shop_product_double_card, "🎴", 500, R.string.shop_desc_double_card, R.string.shop_category_props, discount = 20, isNew = true, rarity = ProductRarity.EPIC),
            ShopProduct(4, R.string.shop_product_guarantee_ticket, "🎫", 800, R.string.shop_desc_guarantee_ticket, R.string.shop_category_props, stock = 5, rarity = ProductRarity.EPIC),
            ShopProduct(5, R.string.shop_product_lucky_box, "📦", 1000, R.string.shop_desc_lucky_box, R.string.shop_category_props, isHot = true, rarity = ProductRarity.RARE),
            ShopProduct(6, R.string.shop_product_rainbow_skin, "🌈", 800, R.string.shop_desc_rainbow_skin, R.string.shop_category_skins, isHot = true, rarity = ProductRarity.EPIC),
            ShopProduct(7, R.string.shop_product_sakura_skin, "🌸", 800, R.string.shop_desc_sakura_skin, R.string.shop_category_skins, isNew = true, rarity = ProductRarity.EPIC),
            ShopProduct(8, R.string.shop_product_starry_skin, "✨", 1000, R.string.shop_desc_starry_skin, R.string.shop_category_skins, rarity = ProductRarity.EPIC),
            ShopProduct(9, R.string.shop_product_ocean_skin, "🌊", 1000, R.string.shop_desc_ocean_skin, R.string.shop_category_skins, rarity = ProductRarity.EPIC),
            ShopProduct(10, R.string.shop_product_fire_skin, "🔥", 1200, R.string.shop_desc_fire_skin, R.string.shop_category_skins, discount = 15, rarity = ProductRarity.LEGENDARY),
            ShopProduct(11, R.string.shop_product_ice_skin, "❄️", 1200, R.string.shop_desc_ice_skin, R.string.shop_category_skins, rarity = ProductRarity.LEGENDARY),
            ShopProduct(12, R.string.shop_product_achievement_badge, "🏆", 600, R.string.shop_desc_achievement_badge, R.string.shop_category_decorations, rarity = ProductRarity.RARE),
            ShopProduct(13, R.string.shop_product_crown, "👑", 1500, R.string.shop_desc_crown, R.string.shop_category_decorations, isHot = true, stock = 3, rarity = ProductRarity.LEGENDARY),
            ShopProduct(14, R.string.shop_product_wings, "🦋", 900, R.string.shop_desc_wings, R.string.shop_category_decorations, rarity = ProductRarity.EPIC),
            ShopProduct(15, R.string.shop_product_halo, "💫", 1100, R.string.shop_desc_halo, R.string.shop_category_decorations, isNew = true, rarity = ProductRarity.EPIC),
            ShopProduct(16, R.string.shop_product_effect_pack, "🎆", 1500, R.string.shop_desc_effect_pack, R.string.shop_category_effects, isHot = true, rarity = ProductRarity.LEGENDARY),
            ShopProduct(17, R.string.shop_product_firework, "🎇", 700, R.string.shop_desc_firework, R.string.shop_category_effects, rarity = ProductRarity.RARE),
            ShopProduct(18, R.string.shop_product_star_effect, "⭐", 600, R.string.shop_desc_star_effect, R.string.shop_category_effects, discount = 10, rarity = ProductRarity.RARE),
            ShopProduct(19, R.string.shop_product_ribbon, "🎀", 500, R.string.shop_desc_ribbon, R.string.shop_category_effects, rarity = ProductRarity.COMMON),
            ShopProduct(20, R.string.shop_product_magic, "✨", 2000, R.string.shop_desc_magic, R.string.shop_category_effects, stock = 1, rarity = ProductRarity.LEGENDARY)
        )
    }
    
    val filteredProducts = remember(selectedCategory) {
        if (selectedCategory == ShopCategory.ALL) {
            allProducts
        } else {
            allProducts.filter { it.categoryResId == selectedCategory.displayNameResId }
        }
    }

    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
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
                    .padding(padding)
            ) {
                ShopTopBar(
                    userCoins = userCoins?.coins ?: 0,
                    onNavigateBack = onNavigateBack
                )
                
                CategorySelector(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProducts) { product ->
                        ProductCard(
                            product = product,
                            userCoins = userCoins?.coins ?: 0,
                            canClaim = canClaimFreeCoins,
                            onClick = {
                                if (product.price == 0) {
                                    // 免费领取
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
                                } else {
                                    // 付费商品
                                    selectedProduct = product
                                    showPurchaseDialog = true
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
    
    if (showPurchaseDialog && selectedProduct != null) {
        PurchaseDialog(
            product = selectedProduct!!,
            userCoins = userCoins?.coins ?: 0,
            onDismiss = { showPurchaseDialog = false },
            onConfirm = {
            scope.launch {
                    val success = shopViewModel.purchaseItem(selectedProduct!!.price)
                    if (success) {
                        showPurchaseDialog = false
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
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    
    repeat(15) { index ->
        val offsetX = remember { (0..100).random() }
        val offsetY = remember { (0..100).random() }
        val delay = remember { index * 200 }
        
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500 + delay, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "star_alpha_$index"
        )
        
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000 + delay, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "star_scale_$index"
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val x = size.width * offsetX / 100f
                    val y = size.height * offsetY / 100f
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = 3f * scale,
                        center = Offset(x, y)
                    )
                }
        )
    }
}

@Composable
fun ShopTopBar(
    userCoins: Int,
    onNavigateBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.9f),
                                Color.White.copy(alpha = 0.7f)
                            )
                        ),
                        shape = CircleShape
                    )
            ) {
                Icon(Icons.Default.ArrowBack, stringResource(R.string.shop_back), tint = Color(0xFFE65100))
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🛍", fontSize = 32.sp)
                Text(
                    stringResource(R.string.shop_title),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100),
                    modifier = Modifier.drawBehind {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.3f),
                            radius = size.height * 0.8f
                        )
                    }
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
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFFD700), Color(0xFFFF8F00))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("💰", fontSize = 24.sp, modifier = Modifier.scale(coinScale))
                    Text("$userCoins", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6F00))
                }
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
    onClick: () -> Unit
) {
    val canAfford = userCoins >= product.price
    
    val infiniteTransition = rememberInfiniteTransition(label = "card_float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .graphicsLayer {
                translationY = if (product.rarity == ProductRarity.LEGENDARY) floatOffset else 0f
            }
            .clickable(enabled = canAfford, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (canAfford) Color.White else Color.White.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (product.rarity == ProductRarity.LEGENDARY) {
                        Modifier.border(
                            width = 3.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFFFFD700), Color(0xFFFF8F00),
                                    Color(0xFFFFD700), Color(0xFFFF8F00), Color(0xFFFFD700)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                    } else if (product.rarity == ProductRarity.EPIC) {
                        Modifier.border(
                            width = 2.dp,
                            color = product.rarity.color.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(24.dp)
                        )
                    } else Modifier
                )
        ) {
            if (product.rarity == ProductRarity.LEGENDARY || product.rarity == ProductRarity.EPIC) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    product.rarity.color.copy(alpha = glowAlpha * 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (product.isHot) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFF5722),
                                shadowElevation = 0.dp
                            ) {
                                Text(
                                    "🔥 " + stringResource(R.string.shop_label_hot),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        if (product.isNew) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF4CAF50),
                                shadowElevation = 0.dp
                            ) {
                                Text(
                                    "✨ " + stringResource(R.string.shop_label_new),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (product.discount != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFE91E63),
                                shadowElevation = 0.dp
                            ) {
                                Text(
                                    stringResource(R.string.shop_label_discount, product.discount),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        if (product.stock != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFF9800).copy(alpha = 0.9f),
                                shadowElevation = 0.dp
                            ) {
                                Text(
                                    stringResource(R.string.shop_label_stock, product.stock),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
                
                Box(contentAlignment = Alignment.Center) {
                    if (product.rarity == ProductRarity.LEGENDARY) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .rotate(rotation)
                                .drawBehind {
                                    val radius = size.width / 2
                                    repeat(8) { i ->
                                        val angle = (i * 45f) * (Math.PI / 180f)
                                        val x = center.x + (radius * cos(angle)).toFloat()
                                        val y = center.y + (radius * sin(angle)).toFloat()
                                        drawCircle(
                                            color = Color(0xFFFFD700).copy(alpha = 0.6f),
                                            radius = 4f,
                                            center = Offset(x, y)
                                        )
                                    }
                                }
                        )
                    }
                    Text(product.emoji, fontSize = 56.sp, modifier = Modifier.padding(vertical = 4.dp))
                }
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = product.rarity.color.copy(alpha = 0.2f),
                    modifier = Modifier.border(1.dp, product.rarity.color, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        stringResource(product.rarity.labelResId),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = product.rarity.color
                    )
                }
                
                Text(
                    stringResource(product.nameResId),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (canAfford) Color(0xFF424242) else Color.Gray,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                )
                
                Text(
                    stringResource(product.descriptionResId),
                    fontSize = 9.sp,
                    color = if (canAfford) Color.Gray else Color.LightGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    maxLines = 2,
                    lineHeight = 11.sp
                )
                
                if (product.price == 0) {
                    Button(
                        onClick = onClick,
                        enabled = canClaim,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canClaim) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF9E9E9E),
                            disabledContentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            hoveredElevation = 0.dp
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (canClaim) {
                                Text("🎁", fontSize = 18.sp)
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                stringResource(if (canClaim) R.string.shop_button_free else R.string.shop_claimed_today),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (canAfford) {
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFFFF6F00),
                                            Color(0xFFFF8F00),
                                            Color(0xFFFFB300)
                                        )
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFFBDBDBD),
                                            Color(0xFF9E9E9E)
                                        )
                                    )
                                }
                            )
                            .clickable(enabled = canAfford, onClick = onClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "💰",
                                fontSize = 18.sp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${product.price}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            
            if (!canAfford && product.price > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.5f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF424242).copy(alpha = 0.9f),
                                            Color(0xFF212121).copy(alpha = 0.95f)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 3.dp,
                                    color = Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                stringResource(R.string.shop_label_locked),
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFF6F00).copy(alpha = 0.9f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("💰", fontSize = 16.sp)
                                Text(
                                    "${product.price}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
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
        targetValue = 1.1f,
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
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp)
            ) {
                if (product.rarity == ProductRarity.LEGENDARY) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        product.rarity.color.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                }
                Text(
                    product.emoji,
                    fontSize = 56.sp,
                    modifier = Modifier
                        .scale(scale)
                        .rotate(rotation)
                )
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.shop_dialog_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
                
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = product.rarity.color.copy(alpha = 0.2f),
                    modifier = Modifier.border(
                        width = 2.dp,
                        color = product.rarity.color,
                        shape = RoundedCornerShape(14.dp)
                    )
                ) {
                    Text(
                        stringResource(product.rarity.labelResId),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = product.rarity.color
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            stringResource(product.nameResId),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            stringResource(product.descriptionResId),
                            fontSize = 15.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.Gray.copy(alpha = 0.3f),
                    thickness = 2.dp
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.shop_dialog_price),
                            fontSize = 16.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE0B2)),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("💰", fontSize = 20.sp)
                                Text(
                                    "${product.price}",
                                    fontSize = 20.sp,
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
                            fontSize = 16.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("💰", fontSize = 20.sp)
                                Text(
                                    "$userCoins",
                                    fontSize = 20.sp,
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
                            fontSize = 16.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        val afterBalance = userCoins - product.price
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (afterBalance >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("💰", fontSize = 20.sp)
                                Text(
                                    "$afterBalance",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (afterBalance >= 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                                )
                            }
                        }
                    }
                }
                
                if (product.stock != null) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚠️", fontSize = 18.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.shop_dialog_limited_warning, product.stock),
                                fontSize = 14.sp,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6F00),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    hoveredElevation = 0.dp
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("✨", fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.shop_dialog_confirm),
                        fontSize = 18.sp,
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
                    .height(58.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(2.dp, Color(0xFFE0E0E0)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent
                )
            ) {
                Text(
                    stringResource(R.string.shop_dialog_cancel),
                    fontSize = 18.sp,
                    color = Color(0xFF757575),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        shape = RoundedCornerShape(32.dp),
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
