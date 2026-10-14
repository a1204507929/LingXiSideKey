package com.lingxikey.custom

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.RoundedCorner
import android.view.SoundEffectConstants
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lingxikey.custom.data.EnvStatus
import com.lingxikey.custom.data.ModuleScope
import com.lingxikey.custom.data.SettingsStore
import com.lingxikey.custom.data.XposedServiceManager
import io.github.libxposed.service.XposedService
import com.lingxikey.custom.model.ActionType
import com.lingxikey.custom.model.AppProfile
import com.lingxikey.custom.model.AppSettings
import com.lingxikey.custom.model.CommonAction
import com.lingxikey.custom.model.CustomActionSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import org.json.JSONObject

// ---------------------------------------------------------------------------
// 皮肤：冷调靛蓝 + 柔和渐变，浅色主题
// ---------------------------------------------------------------------------
private val AppBackground = Color(0xFFF4F6FC)
private val AppBackgroundTop = Color(0xFFF7F9FF)
private val AppBackgroundBottom = Color(0xFFEDF1FB)
private val AppBlue = Color(0xFF4C6BF5)
private val AppBlueDark = Color(0xFF2B42B8)
private val AppBlueSoft = Color(0xFFE9EDFE)
private val AppAccent = Color(0xFF7B5CFF)
private val AppText = Color(0xFF16192B)
private val AppMuted = Color(0xFF7B879C)
private val AppGreen = Color(0xFF17A673)
private val AppBorder = Color(0xFFE4E9F5)
private val AppCardGradient = listOf(Color(0xFFE9EDFE), Color(0xFFF1EAFF))
private val AppBrandGradient = listOf(Color(0xFF5B76FF), Color(0xFF8A5CFF))
private const val PageShadeOpacity = 0.24f
private const val PageSlideFraction = 0.25f
private const val PageTransitionDurationMs = 350

/** Packages whose app-scoped side-key actions are supported. */
private const val CAMERA_PACKAGE = "com.oplus.camera"

private val AppColors = lightColorScheme(
    primary = AppBlue,
    onPrimary = Color.White,
    primaryContainer = AppBlueSoft,
    onPrimaryContainer = AppBlueDark,
    inversePrimary = Color(0xFFC3CEFF),
    secondary = AppAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFEAFF),
    onSecondaryContainer = Color(0xFF3D2A9E),
    tertiary = Color(0xFF1F8A6B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDFF5EC),
    onTertiaryContainer = Color(0xFF145845),
    background = AppBackground,
    onBackground = AppText,
    surface = Color.White,
    onSurface = AppText,
    surfaceVariant = Color(0xFFEDF1FA),
    onSurfaceVariant = AppMuted,
    surfaceTint = AppBlue,
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFE9EEF8),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFCFDFF),
    surfaceContainer = Color(0xFFF8FAFF),
    surfaceContainerHigh = Color(0xFFF1F4FC),
    surfaceContainerHighest = Color(0xFFE9EFF9),
    inverseSurface = AppText,
    inverseOnSurface = Color.White,
    outline = Color(0xFF98A3B8),
    outlineVariant = AppBorder,
    error = Color(0xFFB43F43),
    onError = Color.White,
    errorContainer = Color(0xFFFDE8E8),
    onErrorContainer = Color(0xFF6D1C22),
    scrim = Color.Black
)

private data class ActivityPreset(
    val title: String,
    val packageName: String,
    val className: String,
    val action: String = "",
    val launchInSmallWindow: Boolean = true
)

private val activityPresets = listOf(
    ActivityPreset(
        title = "便签速记",
        packageName = "com.coloros.note",
        className = "com.nearme.note.activity.richedit.QuickNoteViewRichEditActivity"
    )
)

private enum class BottomTab { HOME, ABOUT }

private enum class GestureKind(val title: String, val subtitle: String) {
    SINGLE("单击", "按下并松开后立即触发"),
    DOUBLE("双击", "连续两次按下并松开"),
    LONG("长按", "按住并达到指定时间")
}

private sealed interface Route {
    data object Home : Route
    data object About : Route
    data object Feedback : Route
    data object Advanced : Route
    data object AppProfiles : Route
    data class AppProfileEdit(val packageName: String) : Route
    data class Gesture(val kind: GestureKind) : Route
}

private data class ActionChoice(val action: ActionType, val common: CommonAction? = null) {
    val title: String get() = common?.title ?: action.title
    val icon: ImageVector get() = if (common != null) Icons.Default.Tune else actionIcon(action)
}

/**
 * 只在「按应用配置」里出现的动作，全局功能菜单不展示。
 * 相机快门只在相机（com.oplus.camera）的配置里可选。
 */
private val appScopedOnlyActions = setOf(ActionType.CAMERA_SHUTTER)

private val actionChoices = ActionType.entries
    .filterNot { it in appScopedOnlyActions }
    .flatMap { action ->
        if (action == ActionType.COMMON_FUNCTION) {
            CommonAction.entries.map { ActionChoice(action, it) }
        } else {
            listOf(ActionChoice(action))
        }
    }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CustomSideButtonApp() }
    }
}

@Composable
fun rememberScreenCornerRadius(): Dp {
    val view = LocalView.current
    var radiusPx by remember(view) { mutableIntStateOf(0) }

    DisposableEffect(view) {
        val observer = view.viewTreeObserver
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            radiusPx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                view.rootWindowInsets
                    ?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
                    ?.radius ?: 0
            } else {
                0
            }
        }

        observer.addOnGlobalLayoutListener(listener)
        listener.onGlobalLayout()
        onDispose {
            if (observer.isAlive) observer.removeOnGlobalLayoutListener(listener)
        }
    }

    return with(LocalDensity.current) { radiusPx.toDp() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
private fun CustomSideButtonApp() {
    val context = LocalContext.current
    val view = LocalView.current
    var settings by remember { mutableStateOf(SettingsStore.load(context)) }
    var route by remember { mutableStateOf<Route>(Route.Home) }
    var displayedDetail by remember { mutableStateOf<Route?>(null) }
    var predictiveTarget by remember { mutableStateOf<Route?>(null) }
    val predictiveProgress = remember { Animatable(0f) }
    var routeTransitionEpoch by remember { mutableIntStateOf(0) }
    var skipCommittedReturn by remember { mutableStateOf(false) }
    var contentWidth by remember { mutableFloatStateOf(0f) }

    fun persist(next: AppSettings) {
        settings = next
        SettingsStore.save(next)
    }

    fun navigate(next: Route) {
        if (route != next) {
            skipCommittedReturn = false
            if (!isPrimaryRoute(next)) displayedDetail = next
            route = next
        }
    }

    val primaryRoute: Route = when (route) {
        Route.About -> Route.About
        else -> Route.Home
    }

    val animatedShade by animateFloatAsState(
        targetValue = if (isPrimaryRoute(route)) 0f else PageShadeOpacity,
        animationSpec = tween(PageTransitionDurationMs),
        label = "Page shade"
    )
    val pageShade = when {
        predictiveTarget != null -> animatedShade * (1f - predictiveProgress.value)
        skipCommittedReturn -> 0f
        else -> animatedShade
    }
    val animatedRootOffset by animateFloatAsState(
        targetValue = if (isPrimaryRoute(route)) 0f else -PageSlideFraction,
        animationSpec = tween(PageTransitionDurationMs),
        label = "Primary page offset"
    )
    val rootOffset = when {
        predictiveTarget != null -> -PageSlideFraction * (1f - predictiveProgress.value)
        skipCommittedReturn -> 0f
        else -> animatedRootOffset
    }
    val cornerDp = rememberScreenCornerRadius()

    PredictiveBackHandler(enabled = !isPrimaryRoute(route)) { progress ->
        val destination = backDestination()
        try {
            predictiveTarget = destination
            progress.collect { event ->
                predictiveProgress.snapTo(event.progress.coerceIn(0f, 1f))
            }
            predictiveProgress.animateTo(1f, tween(PageTransitionDurationMs))
            // Replace the transition host at commit so it cannot retain the outgoing page.
            routeTransitionEpoch++
            skipCommittedReturn = true
            route = destination
            predictiveTarget = null
        } catch (_: CancellationException) {
            predictiveProgress.animateTo(0f, tween(180))
        } finally {
            predictiveTarget = null
            predictiveProgress.snapTo(0f)
        }
    }

    MaterialTheme(colorScheme = AppColors) {
        Scaffold(
            containerColor = AppBackground
        ) { padding ->
            Box(Modifier.fillMaxSize().onSizeChanged { contentWidth = it.width.toFloat() }) {
                // 皮肤：整页柔和渐变底
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(AppBackgroundTop, AppBackgroundBottom))
                    )
                )
                PrimaryPageLayer(
                    primaryRoute = primaryRoute,
                    settings = settings,
                    navigate = ::navigate,
                    persist = ::persist,
                    padding = padding,
                    contentWidth = contentWidth,
                    rootOffset = rootOffset,
                    onTabSelected = { tab ->
                        clickSound(view)
                        navigate(
                            when (tab) {
                                BottomTab.HOME -> Route.Home
                                BottomTab.ABOUT -> Route.About
                            }
                        )
                    }
                )

                Box(Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = pageShade))
                    .then(if (!isPrimaryRoute(route) || pageShade > 0f) Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    } else Modifier)
                )

                key(routeTransitionEpoch) {
                    AnimatedVisibility(
                        visible = !isPrimaryRoute(route),
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            val progress = if (predictiveTarget != null) predictiveProgress.value else 0f
                            translationX = contentWidth * progress
                            scaleX = 1f - 0.2f * progress
                            scaleY = 1f - 0.2f * progress
                        },
                        enter = slideInHorizontally(tween(PageTransitionDurationMs)) { it } +
                            scaleIn(initialScale = 0.8f, animationSpec = tween(PageTransitionDurationMs)),
                        exit = slideOutHorizontally(tween(PageTransitionDurationMs)) { it } +
                            scaleOut(targetScale = 0.8f, animationSpec = tween(PageTransitionDurationMs)),
                        label = "Detail transition"
                    ) {
                        displayedDetail?.let { detail ->
                            val clipDetail = predictiveTarget != null ||
                                transition.currentState != transition.targetState
                            Box(Modifier.fillMaxSize().graphicsLayer {
                                shape = RoundedCornerShape(cornerDp)
                                clip = clipDetail
                            }.background(AppBackground)) {
                                RouteScreen(
                                    route = detail,
                                    settings = settings,
                                    navigate = ::navigate,
                                    persist = ::persist,
                                    padding = padding
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun PrimaryPageLayer(
    primaryRoute: Route,
    settings: AppSettings,
    navigate: (Route) -> Unit,
    persist: (AppSettings) -> Unit,
    padding: PaddingValues,
    contentWidth: Float,
    rootOffset: Float,
    onTabSelected: (BottomTab) -> Unit
) {
    val selectedTab = when (primaryRoute) {
        Route.About -> BottomTab.ABOUT
        else -> BottomTab.HOME
    }
    Column(
        Modifier.fillMaxSize()
            .graphicsLayer { translationX = contentWidth * rootOffset }
            .background(AppBackground)
    ) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            AnimatedContent(
                targetState = primaryRoute,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val direction = routeDirection(initialState, targetState)
                    (slideInHorizontally(tween(PageTransitionDurationMs)) { it * direction } togetherWith
                        slideOutHorizontally(tween(PageTransitionDurationMs)) { -it * direction })
                        .using(SizeTransform(clip = true))
                },
                label = "Tab transition"
            ) { current ->
                val actualPadding = object : PaddingValues by padding {
                    override fun calculateBottomPadding(): Dp = 0.dp
                }

                RouteScreen(
                    route = current,
                    settings = settings,
                    navigate = navigate,
                    persist = persist,
                    padding = actualPadding
                )
            }
        }
        NavigationBar(containerColor = Color.White) {
            NavigationBarItem(
                selected = selectedTab == BottomTab.HOME,
                onClick = { onTabSelected(BottomTab.HOME) },
                icon = { Icon(Icons.Default.Home, contentDescription = "首页") },
                label = { Text("首页") }
            )
            NavigationBarItem(
                selected = selectedTab == BottomTab.ABOUT,
                onClick = { onTabSelected(BottomTab.ABOUT) },
                icon = { Icon(Icons.Default.Info, contentDescription = "关于") },
                label = { Text("关于") }
            )
        }
    }
}

private fun backDestination(): Route = Route.Home

private fun isPrimaryRoute(route: Route): Boolean =
    route is Route.Home || route is Route.About

private fun routeDirection(from: Route, to: Route): Int =
    if (primaryRouteIndex(to) >= primaryRouteIndex(from)) 1 else -1

private fun primaryRouteIndex(route: Route): Int = when (route) {
    Route.Home -> 0
    Route.About -> 1
    else -> 0
}

@Composable
private fun RouteScreen(
    route: Route,
    settings: AppSettings,
    navigate: (Route) -> Unit,
    persist: (AppSettings) -> Unit,
    padding: PaddingValues
) {
    when (route) {
        Route.Home -> HomeScreen(settings, navigate, persist, padding)
        Route.About -> AboutScreen(padding)
        Route.Feedback -> FeedbackScreen(settings, persist, { navigate(Route.Home) }, padding)
        Route.Advanced -> AdvancedScreen(settings, persist, { navigate(Route.Home) }, padding)
        Route.AppProfiles -> AppProfilesScreen(settings, persist, navigate, { navigate(Route.Home) }, padding)
        is Route.AppProfileEdit -> AppProfileEditScreen(route.packageName, settings, persist, { navigate(Route.AppProfiles) }, padding)
        is Route.Gesture -> GestureScreen(route.kind, settings, persist, { navigate(Route.Home) }, padding)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun HomeScreen(
    settings: AppSettings,
    navigate: (Route) -> Unit,
    persist: (AppSettings) -> Unit,
    padding: PaddingValues
) {
    val view = LocalView.current
    Column(Modifier.fillMaxSize().padding(padding)) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(AppBrandGradient))
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Column {
                Text(
                    "侧键功能设置",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "灵犀键 · 单击 / 双击 / 长按自定义",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { EnvStatusRow() }
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, AppBorder)
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Default.Tune)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("启用自定义侧键功能", fontWeight = FontWeight.SemiBold)
                            Text(if (settings.enabled) "当前状态：已启用" else "当前状态：已停用", style = MaterialTheme.typography.bodySmall, color = if (settings.enabled) AppGreen else AppMuted)
                        }
                        Switch(settings.enabled, { clickSound(view); persist(settings.copy(enabled = it)) })
                    }
                }
            }
            item { SectionLabel("功能") }
            item {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GestureRow(GestureKind.SINGLE, settings.singleAction, settings.singleCustom, navigate)
                    GestureRow(GestureKind.DOUBLE, settings.doubleAction, settings.doubleCustom, navigate)
                    GestureRow(GestureKind.LONG, settings.longAction, settings.longCustom, navigate)
                }
            }
            item { SectionLabel("其他设置") }
            item {
                SettingRow(Icons.Default.Apps, "按应用配置", "为指定应用单独设置按键功能（优先于全局）") { navigate(Route.AppProfiles) }
            }
            item {
                SettingRow(Icons.Default.Vibration, "振动与提示", "配置触发振动和 Toast 提示") { navigate(Route.Feedback) }
            }
            item {
                SettingRow(Icons.Default.Settings, "高级设置", "作用域检查与配置备份") { navigate(Route.Advanced) }
            }
        }
    }
}

/**
 * 首页标题下的环境状态：root（KSU）与 Xposed 框架是否激活。
 * root 未激活时点击跳转 GhostLock；框架未生效时弹窗询问是否软重启尝试恢复。
 */
@Composable
private fun EnvStatusRow() {
    val context = LocalContext.current
    val view = LocalView.current
    var rootActive by remember { mutableStateOf<Boolean?>(null) }
    var frameworkInstalled by remember { mutableStateOf<Boolean?>(null) }
    var frameworkPkg by remember { mutableStateOf<String?>(null) }
    var moduleActive by remember { mutableStateOf(XposedServiceManager.isModuleActive) }
    var frameworkDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        moduleActive = XposedServiceManager.isModuleActive
        XposedServiceManager.registerBindListener { moduleActive = true }
        val probe = withContext(Dispatchers.IO) {
            EnvStatus.isRootActive() to EnvStatus.frameworkPackage(context)
        }
        rootActive = probe.first
        frameworkPkg = probe.second
        frameworkInstalled = probe.second != null
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        EnvStatusBox(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Security,
            title = "Root (KSU)",
            status = when (rootActive) {
                null -> "检测中…"
                true -> "已激活"
                false -> "未激活"
            },
            statusColor = when (rootActive) {
                null -> AppMuted
                true -> AppGreen
                false -> Color(0xFFD9534F)
            },
            onClick = {
                clickSound(view)
                if (rootActive == true) {
                    Toast.makeText(context, "Root 已激活", Toast.LENGTH_SHORT).show()
                } else {
                    // 点击时先重新探测一次：从 GhostLock 回来后不用重启 App 也能刷新状态。
                    Thread {
                        val active = runCatching { EnvStatus.isRootActive() }.getOrDefault(false)
                        rootActive = active
                    }.start()
                    val opened = runCatching { EnvStatus.openGhostLock(context) }.getOrDefault(false)
                    if (!opened) {
                        Toast.makeText(context, "未安装 GhostLock，无法激活 root", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        EnvStatusBox(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Extension,
            // 检测到管理器时直接显示其名称（Vector / LSPosed），一眼看出用的是哪套框架。
            title = EnvStatus.frameworkName(frameworkPkg)?.let { "框架 · $it" } ?: "框架",
            status = when {
                moduleActive -> "已激活"
                frameworkInstalled == null -> "检测中…"
                frameworkInstalled == true -> "未生效"
                else -> "未安装"
            },
            statusColor = when {
                moduleActive -> AppGreen
                frameworkInstalled == null -> AppMuted
                else -> Color(0xFFD9534F)
            },
            onClick = {
                when {
                    moduleActive -> Toast.makeText(context, "框架已激活，模块已注入系统框架", Toast.LENGTH_SHORT).show()
                    frameworkInstalled == true -> frameworkDialog = "reboot"
                    frameworkInstalled == false -> frameworkDialog = "missing"
                }
            }
        )
    }

    when (frameworkDialog) {
        "reboot" -> AlertDialog(
            onDismissRequest = { frameworkDialog = null },
            title = { Text("框架未生效") },
            text = {
                Text(
                    "已检测到 ${EnvStatus.frameworkName(frameworkPkg) ?: "Xposed"} 框架，但模块尚未注入系统框架。\n\n" +
                        "常见原因是启用模块或修改作用域后没有重启系统框架。\n\n" +
                        "是否现在软重启尝试恢复？（软重启会立即重启系统界面）"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    clickSound(view)
                    frameworkDialog = null
                    Toast.makeText(context, "正在软重启…", Toast.LENGTH_SHORT).show()
                    EnvStatus.softReboot { ok ->
                        if (!ok) Toast.makeText(context, "软重启失败，请检查 root 授权", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("软重启") }
            },
            dismissButton = { TextButton(onClick = { clickSound(view); frameworkDialog = null }) { Text("取消") } }
        )
        "missing" -> AlertDialog(
            onDismissRequest = { frameworkDialog = null },
            title = { Text("未安装框架") },
            text = {
                Text(
                    "未检测到 Vector / LSPosed 框架。\n\n" +
                        "本模块依赖 Xposed 框架把按键拦截注入系统框架，请先安装框架并在其中启用「灵犀按键自定义」。"
                )
            },
            confirmButton = { TextButton(onClick = { clickSound(view); frameworkDialog = null }) { Text("知道了") } }
        )
    }
}

@Composable
private fun EnvStatusBox(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    status: String,
    statusColor: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val view = LocalView.current
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, AppBorder),
        modifier = modifier.clip(shape).clickable {
            clickSound(view)
            onClick()
        }
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, size = 36.dp, iconSize = 19.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(status, style = MaterialTheme.typography.bodySmall, color = statusColor)
            }
        }
    }
}

@Composable
private fun GestureRow(
    kind: GestureKind,
    action: ActionType,
    custom: CustomActionSettings,
    navigate: (Route) -> Unit
) {
    val title = when {
        action == ActionType.NONE -> "未配置"
        action == ActionType.COMMON_FUNCTION -> custom.commonAction.title
        // 「打开应用」已经选好目标时，直接显示成 打开应用(应用名)，一眼看出会打开什么。
        action == ActionType.LAUNCH_APP && custom.launchAppLabel.isNotBlank() ->
            "${action.title}(${custom.launchAppLabel})"
        else -> action.title
    }
    SettingRow(gestureIcon(kind), kind.title, title) {
        navigate(Route.Gesture(kind))
    }
}

@Composable
private fun GestureScreen(
    kind: GestureKind,
    settings: AppSettings,
    persist: (AppSettings) -> Unit,
    back: () -> Unit,
    padding: PaddingValues
) {
    val view = LocalView.current
    var sliderValue by remember(kind, settings) {
        mutableFloatStateOf(
            when (kind) {
                GestureKind.DOUBLE -> settings.doubleClickWindowMs.coerceIn(100L, 800L).toFloat()
                GestureKind.LONG -> settings.longPressMs.coerceIn(100L, 800L).toFloat()
                GestureKind.SINGLE -> 0f
            }
        )
    }
    val currentAction = selectedAction(kind, settings)
    val currentCustom = selectedCustom(kind, settings)
    Column(Modifier.fillMaxSize().padding(padding)) {
        BackTitle(kind.title, back)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          item {
            IntroCard(
                icon = gestureIcon(kind),
                title = kind.title,
                description = when (kind) {
                    GestureKind.SINGLE -> "按下并松开后立即触发。如果未配置双击，则无需等待。"
                    GestureKind.DOUBLE -> "连续两次按下并松开，需要在指定的等待时间内完成第二次按下。"
                    GestureKind.LONG -> "按下并保持达到指定时间后触发，松开不会再次触发单击。"
                }
            )
          }
          if (kind != GestureKind.SINGLE) {
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, AppBorder)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(if (kind == GestureKind.DOUBLE) "等待时间" else "按下时长", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                value = sliderValue,
                                onValueChange = { sliderValue = it },
                                onValueChangeFinished = {
                                    clickSound(view)
                                    val value = sliderValue.toLong()
                                    persist(if (kind == GestureKind.DOUBLE) settings.copy(doubleClickWindowMs = value) else settings.copy(longPressMs = value))
                                },
                                valueRange = 100f..800f,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            TimeValueEditor(
                                value = sliderValue.toLong(),
                                minValue = 100L,
                                maxValue = 800L,
                                onChange = { value ->
                                    sliderValue = value.toFloat()
                                    clickSound(view)
                                    persist(
                                        if (kind == GestureKind.DOUBLE) {
                                            settings.copy(doubleClickWindowMs = value)
                                        } else {
                                            settings.copy(longPressMs = value)
                                        }
                                    )
                                }
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("100ms", style = MaterialTheme.typography.labelSmall, color = AppMuted)
                            Text("800ms", style = MaterialTheme.typography.labelSmall, color = AppMuted)
                        }
                    }
                }
            }
          }
          item { SectionLabel("选择要执行的功能") }
          item {
            val selected = ActionChoice(
                currentAction,
                if (currentAction == ActionType.COMMON_FUNCTION) currentCustom.commonAction else null
            )
            ActionPicker(selected) { choice ->
                val next = updateAction(kind, settings, choice.action)
                persist(if (choice.common != null) {
                    updateCustom(kind, next, currentCustom.copy(commonAction = choice.common))
                } else next)
            }
          }
          if (currentAction in listOf(ActionType.XIAOBU_SHORTCUT, ActionType.LAUNCH_APP,
                  ActionType.CUSTOM_ACTIVITY, ActionType.CUSTOM_URL, ActionType.SHELL_COMMAND)) {
            item {
                CustomActionEditor(currentAction, currentCustom) {
                    persist(updateCustom(kind, settings, it))
                }
            }
          }
          item {
            InfoCard("振动在「振动与提示 → 按下时振动」里统一开关：打开后按下灵犀键即振动，单击一下、双击两下、长按持续振动。")
          }
          item {
            InfoCard(if (kind == GestureKind.DOUBLE && settings.doubleAction == ActionType.NONE) "双击未配置，单击松开后会立即触发。" else if (kind == GestureKind.LONG) "长按触发后不会再触发单击或双击。" else "修改后会立即保存并由 Xposed 模块热加载。")
          }
        }
    }
}

@Composable
private fun FeedbackScreen(settings: AppSettings, persist: (AppSettings) -> Unit, back: () -> Unit, padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        BackTitle("振动与提示", back)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          item { SectionLabel("振动设置") }
          item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, AppBorder)) {
                Column(Modifier.padding(14.dp)) {
                    SettingSwitchRow("按下时振动", settings.vibrationEnabled) { persist(settings.copy(vibrationEnabled = it)) }
                }
            }
          }
          item { SectionLabel("Toast 提示") }
          item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, AppBorder)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingSwitchRow("显示 Toast 提示", settings.toastEnabled) { persist(settings.copy(toastEnabled = it)) }
                    OutlinedTextField(
                        value = settings.toastText,
                        onValueChange = { persist(settings.copy(toastText = it)) },
                        label = { Text("提示内容") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
          }
          item { InfoCard("Toast 提示会在系统框架进程中显示，具体样式由当前 ROM 决定。") }
        }
    }
}

@Composable
private fun AdvancedScreen(settings: AppSettings, persist: (AppSettings) -> Unit, back: () -> Unit, padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        BackTitle("高级设置", back)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          item { SectionLabel("作用域检查") }
          item { ScopeCheckCard() }
          item { SectionLabel("配置备份") }
          item { ConfigBackupCard(settings, persist) }
          item { InfoCard("配置以 JSON 文件导出 / 导入，包含手势动作、按应用配置与全部参数，可用于备份或迁移。按键码与设备路径已写死在程序内，不再作为配置项。") }
        }
    }
}

/**
 * 配置导入 / 导出。导出内容与 Xposed 侧共享的 settings.json 同构（外加少量元数据字段），
 * 导入后立即写入本地与远程配置，无需重启。
 */
@Composable
private fun ConfigBackupCard(settings: AppSettings, persist: (AppSettings) -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val result = runCatching {
            val json = SettingsStore.toJson(settings).apply {
                put("_app", "lingxikey-custom")
                put("_format_version", 1)
                put("_exported_at", System.currentTimeMillis())
            }
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(json.toString().toByteArray(Charsets.UTF_8))
                output.flush()
            } ?: error("无法打开输出位置")
        }
        Toast.makeText(
            context,
            if (result.isSuccess) "配置已导出" else "导出失败：${result.exceptionOrNull()?.message}",
            Toast.LENGTH_SHORT
        ).show()
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val result = runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: error("无法读取所选文件")
            SettingsStore.fromJson(JSONObject(text))
        }
        result.onSuccess { imported ->
            persist(imported)
            Toast.makeText(context, "配置已导入并生效", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "导入失败：${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, AppBorder)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("配置导入 / 导出", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text("导出为 JSON 文件备份，或从文件恢复配置。", style = MaterialTheme.typography.bodySmall, color = AppMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { clickSound(view); exportLauncher.launch("lingxikey-config.json") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("导出配置")
                }
                OutlinedButton(
                    onClick = {
                        clickSound(view)
                        importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("导入配置")
                }
            }
        }
    }
}

/**
 * 作用域（模块生效范围）自检。列出模块用到的每个作用域是否已勾选，
 * 缺哪个就一键向框架申请，避免"装好了却没反应"这种最常见的问题。
 */
@Composable
private fun ScopeCheckCard() {
    val context = LocalContext.current
    val view = LocalView.current
    val service = XposedServiceManager.xposedService
    var scope by remember { mutableStateOf<List<String>?>(null) }
    var installed by remember { mutableStateOf<Set<String>?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var requesting by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }

    fun refresh() {
        val svc = XposedServiceManager.xposedService
        scope = runCatching { svc?.scope }.getOrNull()
    }

    LaunchedEffect(service) {
        installed = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            ModuleScope.allPackages.filter { pkg ->
                runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess
            }.toSet()
        }
        if (service != null) refresh()
    }

    val current = scope
    val inst = installed
    // 缺失的作用域拆成两类：装了的才值得申请 / 展示为红；没装的是历史遗留声明，忽略即可。
    val missingAll = if (current != null) ModuleScope.missing(current) else emptyList()
    val actionableMissing = if (inst != null) missingAll.filter { it in inst } else missingAll
    val ignorableMissing = if (inst != null) missingAll.filterNot { it in inst } else emptyList()
    val missingRequired = if (current != null) ModuleScope.missingRequired(current) else emptyList()

    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, AppBorder)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("作用域检查", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)

            when {
                service == null -> Text(
                    "模块当前未激活，无法读取作用域。请先在框架管理器中启用本模块并勾选作用域。",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppMuted
                )
                current == null -> Text("正在读取作用域…", style = MaterialTheme.typography.bodySmall, color = AppMuted)
                else -> {
                    ModuleScope.entries.forEach { entry ->
                        val checked = ModuleScope.isChecked(current, entry.packageName)
                        val notInstalled = inst != null && entry.packageName !in inst
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                when {
                                    checked -> Icons.Default.CheckCircle
                                    notInstalled -> Icons.Default.HelpOutline
                                    else -> Icons.Default.RadioButtonUnchecked
                                },
                                contentDescription = null,
                                tint = when {
                                    checked -> Color(0xFF2E9E5B)
                                    notInstalled -> Color(0xFF9AA5B1)
                                    else -> Color(0xFFD9534F)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    entry.label + if (entry.required) "（必需）" else "",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    if (notInstalled) "本机未安装该应用，此作用域可忽略" else entry.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppMuted
                                )
                            }
                        }
                    }

                    if (actionableMissing.isEmpty()) {
                        if (ignorableMissing.isEmpty()) {
                            Text("全部已勾选，模块功能完整。", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E9E5B))
                        } else {
                            Text(
                                "需要的作用域已齐全（未勾选的项本机未安装，可忽略）。",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E9E5B)
                            )
                        }
                    } else {
                        Text(
                            buildString {
                                if (missingRequired.isNotEmpty()) {
                                    append("缺少必需作用域，模块不会生效。")
                                } else {
                                    append("有功能缺少作用域，对应操作会静默失效。")
                                }
                                append("缺：")
                                append(actionableMissing.joinToString("、") {
                                    ModuleScope.entries.first { e -> e.packageName == it }.label
                                })
                                if (ignorableMissing.isNotEmpty()) {
                                    append("；另有本机未安装的可忽略项：")
                                    append(ignorableMissing.joinToString("、") {
                                        ModuleScope.entries.first { e -> e.packageName == it }.label
                                    })
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD9534F)
                        )
                        Button(
                            onClick = {
                                clickSound(view)
                                val svc = XposedServiceManager.xposedService ?: return@Button
                                requesting = true
                                message = null
                                runCatching {
                                    // 只申请「缺失且本机已安装」的作用域，避免框架因包不存在而报 Package not found。
                                    svc.requestScope(actionableMissing, object : XposedService.OnScopeEventListener {
                                        override fun onScopeRequestApproved(approved: MutableList<String>) {
                                            message = "已申请：" + approved.joinToString("、")
                                            requesting = false
                                            refresh()
                                        }

                                        override fun onScopeRequestFailed(reason: String) {
                                            message = "申请失败：$reason。" +
                                                "若管理器里显示已勾选，请软重启系统框架后点「重新检查」"
                                            requesting = false
                                        }
                                    })
                                }.onFailure {
                                    requesting = false
                                    message = "申请失败：${it.message}"
                                }
                            },
                            enabled = !requesting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (requesting) "正在申请…" else "一键申请作用域")
                        }
                    }

                    message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AppMuted) }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = {
                            clickSound(view)
                            refresh()
                            Toast.makeText(context, scopeCheckResultText(context), Toast.LENGTH_SHORT).show()
                        }) { Text("重新检查") }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { clickSound(view); showManualDialog = true }) { Text("如何手动勾选") }
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        ManualScopeDialog(onDismiss = { showManualDialog = false })
    }
}

/** 重新读取作用域后给用户的吐司摘要（本机未安装的作用域视为可忽略，不计入缺失）。 */
private fun scopeCheckResultText(context: android.content.Context): String {
    val current = runCatching { XposedServiceManager.xposedService?.scope }.getOrNull()
        ?: return "无法读取作用域：模块当前未激活"
    val pm = context.packageManager
    val installed = ModuleScope.allPackages.filterTo(mutableSetOf()) { pkg ->
        runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess
    }
    val missing = ModuleScope.missing(current).filter { it in installed }
    if (missing.isEmpty()) return "作用域检查完成：需要的作用域已全部勾选 ✓"
    val labels = missing.map { pkg -> ModuleScope.entries.first { it.packageName == pkg }.label }
    val prefix = if (ModuleScope.missingRequired(current).isNotEmpty()) "缺少必需作用域：" else "缺少可选作用域："
    return "作用域检查完成：$prefix${labels.joinToString("、")}"
}

/**
 * 「如何手动勾选」弹窗：说明要到框架管理器里勾选哪些作用域，
 * 并提供「去勾选」按钮直接跳转 Vector / LSPosed 管理器。
 */
@Composable
private fun ManualScopeDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("手动勾选作用域") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("打开框架管理器（Vector / LSPosed），进入「模块」→「灵犀按键自定义」，勾选以下作用域：")
                ModuleScope.entries.forEach { entry ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (entry.required) "必需" else "可选",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (entry.required) Color(0xFFD9534F) else AppMuted,
                            modifier = Modifier
                                .background(AppBlueSoft, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(entry.label, style = MaterialTheme.typography.bodyMedium)
                            Text(entry.packageName, style = MaterialTheme.typography.labelSmall, color = AppMuted)
                        }
                    }
                }
                Text(
                    "勾选后需重启系统框架（软重启）才会生效。",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppMuted
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                clickSound(view)
                launchFrameworkManager(context)
                onDismiss()
            }) { Text("去勾选") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

private fun launchFrameworkManager(context: Context) {
    val opened = runCatching { EnvStatus.openFrameworkManager(context) }.getOrDefault(false)
    if (!opened) {
        Toast.makeText(context, "未找到框架管理器，请手动打开 Vector / LSPosed", Toast.LENGTH_SHORT).show()
    }
}

/** 本项目的开源仓库地址（「关于」页展示与跳转）。 */
private const val OPEN_SOURCE_URL = "https://github.com/a1204507929/LingXiSideKey"

@Composable
private fun AboutScreen(padding: PaddingValues) {
    val context = LocalContext.current
    val view = LocalView.current
    Column(Modifier.fillMaxSize().padding(padding)) {
        Text("关于", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
          item {
            Spacer(Modifier.height(34.dp))
            IconBadge(Icons.Default.Tune, size = 76.dp, iconSize = 38.dp)
            Spacer(Modifier.height(16.dp))
            Text("灵犀按键自定义", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("让你的灵犀键，做更多的事", color = AppMuted, modifier = Modifier.padding(top = 6.dp))
            Text(BuildConfig.APPLICATION_ID, style = MaterialTheme.typography.bodySmall, color = AppMuted, modifier = Modifier.padding(top = 8.dp))
            Text("版本 ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = AppMuted)
          }
           item {
             Spacer(Modifier.height(22.dp))
             Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, AppBorder)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("开源地址", style = MaterialTheme.typography.labelMedium, color = AppMuted)
                    Text("github.com/a1204507929/LingXiSideKey", modifier = Modifier.padding(top = 4.dp))
                    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Color(0xFFEAF0F7))
                    Text("原项目", style = MaterialTheme.typography.labelMedium, color = AppMuted)
                    Text("OppoCustomSideButtonFunctions", modifier = Modifier.padding(top = 4.dp))
                    Text("原作者：SlimeNull", style = MaterialTheme.typography.bodySmall, color = AppMuted, modifier = Modifier.padding(top = 2.dp))
                    Text(
                        "基于原项目做的修改：适配 OPPO K15 Pro+（重写按键拦截，单击 / 双击 / 长按自定义，游戏模式放行原厂）；新增打开任意应用、灯效开关、游戏助手、风扇开关（走原厂通道，切换有流体云胶囊）；按应用单独配置；按下即振动；全新界面皮肤；作用域检查与一键申请；配置 JSON 导入 / 导出；首页 Root / 框架激活状态显示与一键修复。在此感谢原作者的出色工作。",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppMuted,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                 }
             }
           }
           item {
             Row(
                 Modifier.fillMaxWidth(),
                 horizontalArrangement = Arrangement.spacedBy(10.dp)
             ) {
                 OutlinedButton(
                     onClick = {
                         clickSound(view)
                         openExternalUrl(context, OPEN_SOURCE_URL)
                     },
                     modifier = Modifier.weight(1f)
                 ) {
                     Icon(Icons.Default.OpenInNew, contentDescription = null)
                     Spacer(Modifier.width(6.dp))
                     Text("开源地址")
                 }
                 OutlinedButton(
                     onClick = {
                         clickSound(view)
                         openExternalUrl(context, "https://github.com/SlimeNull/OppoCustomSideButtonFunctions")
                     },
                     modifier = Modifier.weight(1f)
                 ) {
                     Icon(Icons.Default.Code, contentDescription = null)
                     Spacer(Modifier.width(6.dp))
                     Text("原项目仓库")
                 }
             }
           }
           item {
             Text(
                 "GNU Lesser General Public License v3.0",
                 style = MaterialTheme.typography.bodySmall,
                 color = AppMuted,
                 modifier = Modifier.fillMaxWidth(),
                 textAlign = androidx.compose.ui.text.style.TextAlign.Center
             )
           }
        }
    }
}

private fun openExternalUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addCategory(Intent.CATEGORY_BROWSABLE)
        )
    }.onXposedFailure("open external URL")
}

@Composable
private fun BackTitle(title: String, back: () -> Unit) {
    val view = LocalView.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { clickSound(view); back() }) { Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = AppText) }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val view = LocalView.current
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, AppBorder),
        modifier = Modifier.fillMaxWidth().clip(shape).clickable {
            clickSound(view)
            onClick()
        }
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppMuted)
            }
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = AppMuted, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun IntroCard(icon: ImageVector, title: String, description: String) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, AppBorder)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, size = 48.dp, iconSize = 26.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = AppMuted, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionPicker(
    selected: ActionChoice,
    choices: List<ActionChoice> = actionChoices,
    onSelect: (ActionChoice) -> Unit
) {
    val view = LocalView.current
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { clickSound(view); expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, AppBorder),
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(selected.icon, size = 34.dp, iconSize = 19.dp)
                Spacer(Modifier.width(12.dp))
                Text(selected.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ExpandMore, contentDescription = "选择功能", tint = AppMuted)
            }
        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(14.dp)
        ) {
            choices.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice.title) },
                    leadingIcon = { Icon(choice.icon, contentDescription = null) },
                    trailingIcon = if (choice == selected) {
                        { Icon(Icons.Default.CheckCircle, contentDescription = "已选择", tint = AppBlue) }
                    } else null,
                    onClick = {
                        clickSound(view)
                        expanded = false
                        onSelect(choice)
                    }
                )
            }
        }
    }
}

@Composable
private fun ActivityPresetPicker(onSelect: (ActivityPreset) -> Unit) {
    val view = LocalView.current
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { clickSound(view); expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("载入预设", modifier = Modifier.weight(1f))
            Icon(Icons.Default.ExpandMore, contentDescription = "选择预设")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(14.dp)
        ) {
            activityPresets.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(preset.title) },
                    onClick = {
                        clickSound(view)
                        expanded = false
                        onSelect(preset)
                    }
                )
            }
        }
    }
}

@Composable
private fun CustomActionEditor(
    action: ActionType,
    custom: CustomActionSettings,
    onChange: (CustomActionSettings) -> Unit
) {
    when (action) {
        ActionType.COMMON_FUNCTION -> {
            ParameterCard("常用功能") {
                Text("已选择：${custom.commonAction.title}", color = AppMuted)
            }
        }
        ActionType.XIAOBU_SHORTCUT -> {
            ParameterCard("小布快捷指令") {
                OutlinedTextField(
                    value = custom.xiaobuShortcutId,
                    onValueChange = { onChange(custom.copy(xiaobuShortcutId = it)) },
                    label = { Text("快捷指令 ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SettingSwitchRow(
                    title = "显示执行结果 Toast",
                    checked = custom.shellToastEnabled,
                    onCheckedChange = { onChange(custom.copy(shellToastEnabled = it)) }
                )
            }
        }
        ActionType.LAUNCH_APP -> {
            ParameterCard("选择要打开的应用") {
                val context = LocalContext.current
                var showAppPicker by remember { mutableStateOf(false) }
                val label = custom.launchAppLabel.ifBlank { custom.launchAppPackage }
                Text(
                    if (label.isBlank()) "尚未选择应用，请先选择。" else "已选择：$label",
                    color = AppMuted
                )
                if (custom.launchAppPackage.isNotBlank()) {
                    Text(custom.launchAppPackage, style = MaterialTheme.typography.labelSmall, color = AppMuted)
                }
                OutlinedButton(
                    onClick = { showAppPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AppShortcut, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (custom.launchAppPackage.isBlank()) "选择应用" else "重新选择")
                }
                if (showAppPicker) {
                    InstalledAppPicker(
                        context = context,
                        onDismiss = { showAppPicker = false },
                        onPicked = { pkg, name ->
                            showAppPicker = false
                            onChange(custom.copy(launchAppPackage = pkg, launchAppLabel = name))
                        }
                    )
                }
            }
        }
        ActionType.CUSTOM_ACTIVITY -> {
            ParameterCard("Activity 参数") {
                OutlinedTextField(custom.activityPackage, { onChange(custom.copy(activityPackage = it)) }, label = { Text("包名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(custom.activityClass, { onChange(custom.copy(activityClass = it)) }, label = { Text("Activity 类名（可选，留空使用默认 Activity）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(custom.activityAction, { onChange(custom.copy(activityAction = it)) }, label = { Text("Intent Action（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                SettingSwitchRow(
                    title = "以小窗模式运行",
                    checked = custom.launchInSmallWindow,
                    onCheckedChange = { onChange(custom.copy(launchInSmallWindow = it)) }
                )
                ActivityPresetPicker { preset ->
                    onChange(
                        custom.copy(
                            activityPackage = preset.packageName,
                            activityClass = preset.className,
                            activityAction = preset.action,
                            launchInSmallWindow = preset.launchInSmallWindow
                        )
                    )
                }
            }
        }
        ActionType.CUSTOM_URL -> {
            ParameterCard("Url Scheme 参数") {
                OutlinedTextField(
                    value = custom.urlScheme,
                    onValueChange = { onChange(custom.copy(urlScheme = it)) },
                    label = { Text("Url Scheme") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        ActionType.SHELL_COMMAND -> {
            ParameterCard("Shell 参数") {
                OutlinedTextField(
                    value = custom.shellCommand,
                    onValueChange = { onChange(custom.copy(shellCommand = it)) },
                    label = { Text("Shell 指令") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                SettingSwitchRow(
                    title = "显示执行结果 Toast",
                    checked = custom.shellToastEnabled,
                    onCheckedChange = { onChange(custom.copy(shellToastEnabled = it)) }
                )
            }
        }
        else -> Unit
    }
}

@Composable
private fun ParameterCard(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, AppBorder)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun SettingSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val view = LocalView.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked, { clickSound(view); onCheckedChange(it) })
    }
}

@Composable
private fun IconBadge(icon: ImageVector, size: androidx.compose.ui.unit.Dp = 40.dp, iconSize: androidx.compose.ui.unit.Dp = 21.dp) {
    Box(
        Modifier
            .size(size)
            .background(Brush.linearGradient(AppCardGradient), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = AppBlueDark, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    ) {
        Box(
            Modifier
                .size(width = 3.dp, height = 14.dp)
                .background(Brush.verticalGradient(listOf(AppBlue, AppAccent)), RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(7.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = AppText)
    }
}

@Composable
private fun InfoCard(text: String) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = AppBlueSoft), border = BorderStroke(1.dp, AppBorder)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = AppBlue)
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = AppBlueDark)
        }
    }
}

@Composable
private fun TimeValueEditor(
    value: Long,
    minValue: Long,
    maxValue: Long,
    suffix: String = "ms",
    onChange: (Long) -> Unit
) {
    var text by remember(value) { mutableStateOf(value.coerceIn(minValue, maxValue).toString()) }
    val focusManager = LocalFocusManager.current

    fun commit() {
        val committed = (text.toLongOrNull() ?: value).coerceIn(minValue, maxValue)
        text = committed.toString()
        if (committed != value) onChange(committed)
    }

    BasicTextField(
        value = text,
        onValueChange = { input ->
            text = input.filter(Char::isDigit).take(6)
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.labelMedium.copy(color = AppText),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            commit()
            focusManager.clearFocus()
        }),
        modifier = Modifier
            .width(54.dp)
            .height(32.dp)
            .onFocusChanged { state -> if (!state.isFocused) commit() },
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF3F7FC), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFD6E1EC), RoundedCornerShape(8.dp))
                    .padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) { innerTextField() }
                Text(suffix, style = MaterialTheme.typography.labelSmall, color = AppMuted)
            }
        }
    )
}

private fun clickSound(view: View) {
    view.playSoundEffect(SoundEffectConstants.CLICK)
}

private fun selectedAction(kind: GestureKind, settings: AppSettings): ActionType = when (kind) {
    GestureKind.SINGLE -> settings.singleAction
    GestureKind.DOUBLE -> settings.doubleAction
    GestureKind.LONG -> settings.longAction
}

private fun selectedCustom(kind: GestureKind, settings: AppSettings): CustomActionSettings = when (kind) {
    GestureKind.SINGLE -> settings.singleCustom
    GestureKind.DOUBLE -> settings.doubleCustom
    GestureKind.LONG -> settings.longCustom
}

private fun updateAction(kind: GestureKind, settings: AppSettings, action: ActionType): AppSettings = when (kind) {
    GestureKind.SINGLE -> settings.copy(singleAction = action, singleWakeScreen = action == ActionType.CYCLE_RINGER, singleVibrationEnabled = false)
    GestureKind.DOUBLE -> settings.copy(doubleAction = action, doubleWakeScreen = action == ActionType.CYCLE_RINGER, doubleVibrationEnabled = false)
    GestureKind.LONG -> settings.copy(longAction = action, longWakeScreen = action == ActionType.CYCLE_RINGER, longVibrationEnabled = false)
}

private fun updateCustom(kind: GestureKind, settings: AppSettings, custom: CustomActionSettings): AppSettings = when (kind) {
    GestureKind.SINGLE -> settings.copy(singleCustom = custom)
    GestureKind.DOUBLE -> settings.copy(doubleCustom = custom)
    GestureKind.LONG -> settings.copy(longCustom = custom)
}

private fun gestureIcon(kind: GestureKind): ImageVector = when (kind) {
    GestureKind.SINGLE -> Icons.Default.Vibration
    GestureKind.DOUBLE -> Icons.Default.Vibration
    GestureKind.LONG -> Icons.Default.AccessTime
}

private fun actionIcon(action: ActionType): ImageVector = when (action) {
    ActionType.NONE -> Icons.Default.RemoveCircleOutline
    ActionType.SHOW_RINGER -> Icons.Default.VolumeUp
    ActionType.CYCLE_RINGER -> Icons.Default.VolumeUp
    ActionType.TOGGLE_DND -> Icons.Default.NotificationsOff
    ActionType.FLASHLIGHT -> Icons.Default.FlashOn
    ActionType.COOLING_FAN_TOGGLE -> Icons.Default.Air
    ActionType.FAN_LIGHT_TOGGLE -> Icons.Default.Lightbulb
    ActionType.RECORDING -> Icons.Default.Mic
    ActionType.SCREENSHOT -> Icons.Default.CropFree
    ActionType.COMMON_FUNCTION -> Icons.Default.Tune
    ActionType.XIAOBU_SHORTCUT -> Icons.Default.Tune
    ActionType.XIAOBU_SCREEN_RECOGNIZE -> Icons.Default.ScreenSearchDesktop
    ActionType.GAME_ASSISTANT -> Icons.Default.SportsEsports
    ActionType.LAUNCH_APP -> Icons.Default.AppShortcut
    ActionType.CUSTOM_ACTIVITY -> Icons.Default.Code
    ActionType.CUSTOM_URL -> Icons.Default.OpenInNew
    ActionType.SHELL_COMMAND -> Icons.Default.Code
    ActionType.CAMERA_SHUTTER -> Icons.Default.CameraAlt
    ActionType.MUSIC_PLAY_PAUSE -> Icons.Default.PlayArrow
    ActionType.MUSIC_NEXT -> Icons.Default.SkipNext
    ActionType.MUSIC_PREVIOUS -> Icons.Default.SkipPrevious
    ActionType.VIDEO_SEEK_FORWARD -> Icons.Default.FastForward
    ActionType.VIDEO_SEEK_BACKWARD -> Icons.Default.FastRewind
}

// ---------------------------------------------------------------------------
// Per-application gesture configuration
// ---------------------------------------------------------------------------

/**
 * Actions offered when configuring a specific app. App-scoped actions are only shown for the app
 * they belong to, so e.g. the camera shutter never appears under an unrelated profile.
 */
private fun appGestureChoices(packageName: String): List<ActionChoice> {
    // 相机是特殊场景：按键在相机里只做一件事——拍照 / 录制。
    // 其他动作（音乐、截屏、风扇…）在相机前台没有使用场景，一并隐藏，避免误配。
    if (packageName == CAMERA_PACKAGE) {
        return listOf(
            ActionChoice(ActionType.NONE),
            ActionChoice(ActionType.CAMERA_SHUTTER)
        )
    }
    val shared = listOf(
        ActionType.MUSIC_PLAY_PAUSE,
        ActionType.MUSIC_NEXT,
        ActionType.MUSIC_PREVIOUS,
        ActionType.VIDEO_SEEK_FORWARD,
        ActionType.VIDEO_SEEK_BACKWARD,
        ActionType.SCREENSHOT,
        ActionType.FLASHLIGHT,
        ActionType.COOLING_FAN_TOGGLE,
        ActionType.FAN_LIGHT_TOGGLE,
        ActionType.XIAOBU_SCREEN_RECOGNIZE,
        ActionType.XIAOBU_SHORTCUT,
        ActionType.GAME_ASSISTANT,
        ActionType.LAUNCH_APP,
        ActionType.CUSTOM_ACTIVITY,
        ActionType.CUSTOM_URL,
        ActionType.SHELL_COMMAND
    )
    return buildList {
        add(ActionChoice(ActionType.NONE))
        addAll(shared.map { ActionChoice(it) })
    }
}

@Composable
private fun AppProfilesScreen(
    settings: AppSettings,
    persist: (AppSettings) -> Unit,
    navigate: (Route) -> Unit,
    back: () -> Unit,
    padding: PaddingValues
) {
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(padding)) {
        BackTitle("按应用配置", back)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                InfoCard("为指定应用单独设置单击 / 双击 / 长按。当该应用在前台时优先执行这里的设置；未配置的手势回落到全局设置。")
            }
            item {
                Button(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("添加应用")
                }
            }
            if (settings.appProfiles.isEmpty()) {
                item { InfoCard("尚未添加任何应用配置。") }
            }
            items(settings.appProfiles.size) { index ->
                val profile = settings.appProfiles[index]
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, AppBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Default.Apps, size = 34.dp, iconSize = 19.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(profile.label.ifBlank { profile.packageName }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(profile.packageName, style = MaterialTheme.typography.labelSmall, color = AppMuted)
                            }
                            Switch(
                                checked = profile.enabled,
                                onCheckedChange = { enabled ->
                                    persist(settings.copy(appProfiles = settings.appProfiles.toMutableList().also {
                                        it[index] = profile.copy(enabled = enabled)
                                    }))
                                }
                            )
                        }
                        Text(
                            gestureSummary(profile),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppMuted
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { navigate(Route.AppProfileEdit(profile.packageName)) },
                                modifier = Modifier.weight(1f)
                            ) { Text("编辑") }
                            OutlinedButton(
                                onClick = {
                                    persist(settings.copy(appProfiles = settings.appProfiles.filterIndexed { i, _ -> i != index }))
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("删除") }
                        }
                    }
                }
            }
        }
    }
    if (showPicker) {
        InstalledAppPicker(
            context = context,
            onDismiss = { showPicker = false },
            onPicked = { pkg, label ->
                showPicker = false
                if (settings.appProfiles.none { it.packageName == pkg }) {
                    persist(
                        settings.copy(
                            appProfiles = settings.appProfiles + AppProfile(packageName = pkg, label = label)
                        )
                    )
                }
            }
        )
    }
}

private fun gestureSummary(profile: AppProfile): String {
    fun name(action: ActionType?): String = action?.title ?: "跟随全局"
    return "单击: ${name(profile.singleAction)} · 双击: ${name(profile.doubleAction)} · 长按: ${name(profile.longAction)}"
}

@Composable
private fun AppProfileEditScreen(
    packageName: String,
    settings: AppSettings,
    persist: (AppSettings) -> Unit,
    back: () -> Unit,
    padding: PaddingValues
) {
    val index = settings.appProfiles.indexOfFirst { it.packageName == packageName }
    if (index < 0) {
        Column(Modifier.fillMaxSize().padding(padding)) {
            BackTitle("应用配置", back)
            InfoCard("该应用配置已不存在。")
        }
        return
    }
    val profile = settings.appProfiles[index]
    fun update(block: (AppProfile) -> AppProfile) {
        persist(
            settings.copy(
                appProfiles = settings.appProfiles.toMutableList().also { it[index] = block(it[index]) }
            )
        )
    }
    Column(Modifier.fillMaxSize().padding(padding)) {
        BackTitle(profile.label.ifBlank { profile.packageName }, back)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                InfoCard("仅在该应用位于前台时生效。选择“不执行操作”会屏蔽此手势，选择“跟随全局”则使用全局配置。")
            }
            appGestureSection(
                title = "单击",
                packageName = profile.packageName,
                selected = profile.singleAction,
                custom = profile.singleCustom,
                onSelect = { action -> update { it.copy(singleAction = action) } }
            )
            appGestureSection(
                title = "双击",
                packageName = profile.packageName,
                selected = profile.doubleAction,
                custom = profile.doubleCustom,
                onSelect = { action -> update { it.copy(doubleAction = action) } }
            )
            appGestureSection(
                title = "长按",
                packageName = profile.packageName,
                selected = profile.longAction,
                custom = profile.longCustom,
                onSelect = { action -> update { it.copy(longAction = action) } }
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.appGestureSection(
    title: String,
    packageName: String,
    selected: ActionType?,
    custom: CustomActionSettings,
    onSelect: (ActionType?) -> Unit
) {
    item { SectionLabel(title) }
    item {
        val selectedChoice = selected?.let { ActionChoice(it) }
        val fallback = ActionChoice(ActionType.NONE)
        FollowGlobalPicker(
            packageName = packageName,
            selected = selectedChoice ?: fallback,
            followGlobal = selected == null,
            onSelectFollowGlobal = { onSelect(null) },
            onSelectAction = { onSelect(it.action) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FollowGlobalPicker(
    packageName: String,
    selected: ActionChoice,
    followGlobal: Boolean,
    onSelectFollowGlobal: () -> Unit,
    onSelectAction: (ActionChoice) -> Unit
) {
    val view = LocalView.current
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { clickSound(view); expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, AppBorder),
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        ) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(if (followGlobal) Icons.Default.Tune else selected.icon, size = 34.dp, iconSize = 19.dp)
                Spacer(Modifier.width(12.dp))
                Text(if (followGlobal) "跟随全局" else selected.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ExpandMore, contentDescription = "选择功能", tint = AppMuted)
            }
        }
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, shape = RoundedCornerShape(14.dp)) {
            DropdownMenuItem(
                text = { Text("跟随全局") },
                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                trailingIcon = if (followGlobal) {
                    { Icon(Icons.Default.CheckCircle, contentDescription = "已选择", tint = AppBlue) }
                } else null,
                onClick = { clickSound(view); expanded = false; onSelectFollowGlobal() }
            )
            appGestureChoices(packageName).forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice.title) },
                    leadingIcon = { Icon(choice.icon, contentDescription = null) },
                    trailingIcon = if (!followGlobal && choice == selected) {
                        { Icon(Icons.Default.CheckCircle, contentDescription = "已选择", tint = AppBlue) }
                    } else null,
                    onClick = { clickSound(view); expanded = false; onSelectAction(choice) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Lists launchable apps for the per-app picker.
 *
 * Android 11+ package visibility hides most packages from a plain queryIntentActivities call.
 * The manifest declares <queries> + QUERY_ALL_PACKAGES, but we additionally ask with
 * MATCH_ALL|MATCH_UNINSTALLED_PACKAGES and finally fall back to getInstalledApplications so the
 * list stays complete even on ROMs that restrict visibility.
 */
/** 应用选择器列表项：包名、显示名与图标。 */
private data class AppEntry(
    val pkg: String,
    val label: String,
    val icon: Drawable
)

private fun loadInstalledApps(
    pm: android.content.pm.PackageManager,
    selfPackage: String
): List<AppEntry> {
    val result = LinkedHashMap<String, AppEntry>()

    // Primary: launcher activities (best labels).
    runCatching {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_ALL)
            .forEach { info ->
                val pkg = info.activityInfo.packageName
                if (pkg != selfPackage) {
                    result[pkg] = AppEntry(
                        pkg,
                        info.loadLabel(pm).toString(),
                        runCatching { info.loadIcon(pm) }.getOrDefault(pm.defaultActivityIcon)
                    )
                }
            }
    }

    // Secondary: every installed application that has a launcher entry of any kind.
    runCatching {
        @Suppress("DEPRECATION")
        pm.getInstalledApplications(android.content.pm.PackageManager.MATCH_ALL).forEach { appInfo ->
            val pkg = appInfo.packageName
            if (pkg == selfPackage) return@forEach
            if (result.containsKey(pkg)) return@forEach
            val hasLauncher = pm.getLaunchIntentForPackage(pkg) != null
            if (hasLauncher) {
                result[pkg] = AppEntry(
                    pkg,
                    runCatching { appInfo.loadLabel(pm).toString() }.getOrDefault(pkg),
                    runCatching { appInfo.loadIcon(pm) }.getOrDefault(pm.defaultActivityIcon)
                )
            }
        }
    }

    return result.values.sortedBy { it.label }
}

@Composable
private fun InstalledAppPicker(
    context: android.content.Context,
    onDismiss: () -> Unit,
    onPicked: (String, String) -> Unit
) {
    val pm = context.packageManager
    val apps = remember {
        runCatching { loadInstalledApps(pm, context.packageName) }.getOrDefault(emptyList())
    }
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, apps) {
        if (query.isBlank()) apps else apps.filter {
            it.label.contains(query, ignoreCase = true) || it.pkg.contains(query, ignoreCase = true)
        }
    }
    // 图标渲染成小尺寸位图，避免自适应图标按 108dp 原尺寸转换导致内存暴涨。
    val iconPx = with(LocalDensity.current) { 36.dp.roundToPx() }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("选择应用") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("搜索") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    items(filtered.size) { i ->
                        val app = filtered[i]
                        DropdownMenuItem(
                            leadingIcon = {
                                Image(
                                    bitmap = remember(app.pkg) {
                                        runCatching { app.icon.toBitmap(iconPx, iconPx) }
                                            .getOrDefault(app.icon.toBitmap())
                                            .asImageBitmap()
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp)
                                )
                            },
                            text = {
                                Column {
                                    Text(app.label, style = MaterialTheme.typography.bodyMedium)
                                    Text(app.pkg, style = MaterialTheme.typography.labelSmall, color = AppMuted)
                                }
                            },
                            onClick = { onPicked(app.pkg, app.label) }
                        )
                    }
                }
            }
        }
    )
}
