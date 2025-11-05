package com.github.kr328.clash

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setUUID
import com.github.kr328.clash.common.util.ticker
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.bridge.Bridge
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.design.*
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.components.AppPreferences
import com.github.kr328.clash.design.components.BottomBar
import com.github.kr328.clash.design.model.ProxyState
import com.github.kr328.clash.design.screen.MainScreen
import com.github.kr328.clash.design.screen.ProfilesScreen
import com.github.kr328.clash.design.screen.ProxyScreen
import com.github.kr328.clash.design.screen.SettingsScreen
import com.github.kr328.clash.design.theme.YumeTheme
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.design.ui.icon.Icons
import com.github.kr328.clash.design.ui.icon.icons.`Arrow-down-up`
import com.github.kr328.clash.design.ui.icon.icons.Bolt
import com.github.kr328.clash.design.ui.icon.icons.House
import com.github.kr328.clash.design.ui.icon.icons.`Package-check`
import com.github.kr328.clash.remote.Broadcasts
import com.github.kr328.clash.remote.Remote
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import com.github.kr328.clash.util.withClash
import com.github.kr328.clash.util.withProfile
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class MainActivity : ComponentActivity(), Broadcasts.Observer, CoroutineScope by MainScope() {
    private val uiStore by lazy { com.github.kr328.clash.design.store.UiStore(this) }

    private val events = Channel<Event>(Channel.UNLIMITED)
    private var activityStarted = false
    private var activityResumed = false

    private val clashRunning: Boolean
        get() = Remote.broadcasts.clashRunning

    private var design: MainDesign? = null
    private var proxyDesignInstance: ProxyDesign? = null
    private var profilesDesignInstance: ProfilesDesign? = null

    private lateinit var startForResultLauncher: ActivityResultLauncher<Intent>
    private var startForResultContinuation: CancellableContinuation<ActivityResult>? = null

    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null

    private var lastProxyInfoUpdate = 0L
    private val proxyInfoUpdateDebounce = 50L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startForResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            startForResultContinuation?.resume(result)
            startForResultContinuation = null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher =
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { _: Boolean -> }
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        launch { mainLoop() }
    }

    override fun onStart() {
        super.onStart()
        activityStarted = true
        Remote.broadcasts.addObserver(this)
        events.trySend(Event.ActivityStart)
    }

    override fun onResume() {
        super.onResume()
        activityResumed = true
        events.trySend(Event.ActivityResume)
    }

    override fun onPause() {
        super.onPause()
        activityResumed = false
        events.trySend(Event.ActivityPause)
        proxyDesignInstance?.stopAllTesting()
    }

    override fun onStop() {
        super.onStop()
        activityStarted = false
        Remote.broadcasts.removeObserver(this)
        events.trySend(Event.ActivityStop)
    }

    override fun onDestroy() {
        super.onDestroy()
        design?.cancel()
        cancel()
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)

        if (isInMultiWindowMode) {
            handleFreeformDensity(newConfig)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleFreeformDensity(newConfig)
    }

    private fun handleFreeformDensity(newConfig: Configuration) {
        val isInFreeform = newConfig.toString().contains("mWindowingMode=freeform")
        if (!isInFreeform) {
            return
        }

        val res = resources
        val dm = DisplayMetrics()
        dm.setTo(res.displayMetrics)

        val smallestWidth = newConfig.smallestScreenWidthDp
        if (smallestWidth < 600) {
            val baseDensity = 2.75f
            dm.density = baseDensity
            dm.scaledDensity = baseDensity * (res.displayMetrics.scaledDensity / res.displayMetrics.density)
            dm.densityDpi = (160 * baseDensity).toInt()

            val config = Configuration(newConfig)
            config.densityDpi = dm.densityDpi
            res.updateConfiguration(config, dm)
        }
    }

    override fun onServiceRecreated() {
        events.trySend(Event.ServiceRecreated)
    }

    override fun onStarted() {
        events.trySend(Event.ClashStart)
    }

    override fun onStopped(cause: String?) {
        events.trySend(Event.ClashStop)
    }

    override fun onProfileChanged() {
        events.trySend(Event.ProfileChanged)
    }

    override fun onProfileUpdateCompleted(uuid: java.util.UUID?) {
        events.trySend(Event.ProfileUpdateCompleted)
    }

    override fun onProfileUpdateFailed(uuid: java.util.UUID?, reason: String?) {
        events.trySend(Event.ProfileUpdateFailed)
    }

    override fun onProfileLoaded() {
        events.trySend(Event.ProfileLoaded)
    }

    private suspend fun updateProxyGroupData(
        idx: Int,
        groupName: String,
        proxyStates: List<ProxyState>,
        proxyDesign: ProxyDesign
    ) {
        val group = withClash { queryProxyGroup(groupName, uiStore.proxySort) }
        val st = proxyStates.getOrNull(idx) ?: return
        st.now = group.now

        val currentDelays = group.proxies.associate { it.name to it.delay }
        proxyDesign.updateProxyDelaysRealtime(idx, currentDelays)

        val proxyLinks = group.proxies.associate {
            it.name to ProxyState(it.name, it.name, it.delay)
        }
        proxyDesign.updateGroup(idx, group.proxies, group.type == Proxy.Type.Selector, st, proxyLinks)
    }

    private suspend fun mainLoop() {
        var mode = withClash { queryOverride(Clash.OverrideSlot.Session).mode }
        var groupNames = withClash { queryProxyGroupNames(uiStore.proxyExcludeNotSelectable) }
        var proxyStates = groupNames.map { ProxyState(it, "?") }.toMutableList()
        var unorderedStates = groupNames.indices.associate { groupNames[it] to proxyStates[it] }.toMutableMap()
        val reloadLock = Semaphore(8)

        val proxyDesign = ProxyDesign(this, mode, groupNames, uiStore)
        proxyDesignInstance = proxyDesign

        val profilesDesign = ProfilesDesign(this)
        profilesDesignInstance = profilesDesign

        fun rebuildGroups(newNames: List<String>) {
            groupNames = newNames
            proxyStates = newNames.map { ProxyState(it, "?") }.toMutableList()
            unorderedStates = newNames.indices.associate { newNames[it] to proxyStates[it] }.toMutableMap()
            proxyDesign.applyNewGroupNames(newNames)
        }

        var prefetching = false
        fun triggerPrefetchIfNeeded(reason: String) {
            if (prefetching) return
            val need = groupNames.isEmpty() || groupNames.indices.any { proxyDesign.proxyGroups[it] == null }
            if (!need) return
            prefetching = true
            launch {
                try {
                    repeat(10) { attempt ->
                        val freshNames = withClash { queryProxyGroupNames(uiStore.proxyExcludeNotSelectable) }
                        if (freshNames.isNotEmpty()) {
                            if (freshNames != groupNames) rebuildGroups(freshNames)
                            val semaphore = Semaphore(6)
                            freshNames.mapIndexed { idx, name ->
                                async {
                                    try {
                                        semaphore.withPermit {
                                            updateProxyGroupData(idx, name, proxyStates, proxyDesign)
                                        }
                                    } catch (_: Throwable) {
                                    }
                                }
                            }.awaitAll()
                            return@launch
                        } else {
                            delay(200)
                        }
                    }
                } finally {
                    prefetching = false
                }
            }
        }

        val mainDesign = MainDesign(this, proxyDesign)
        design = mainDesign

        AppPreferences.init(uiStore)

        withContext(Dispatchers.Main) {
            setContent {
                YumeTheme {
                    val items = listOf(
                        NavigationItem("首页", Icons.House),
                        NavigationItem("节点", Icons.`Arrow-down-up`),
                        NavigationItem("配置", Icons.`Package-check`),
                        NavigationItem("设置", Icons.Bolt)
                    )
                    val pagerState = rememberPagerState(initialPage = 0, pageCount = { items.size })
                    val scope = rememberCoroutineScope()

                    var showDivider by remember { mutableStateOf(uiStore.bottomBarShowDivider) }
                    var useFloating by remember { mutableStateOf(uiStore.bottomBarFloating) }

                    LaunchedEffect(Unit) {
                        while (isActive) {
                            delay(100)
                            showDivider = uiStore.bottomBarShowDivider
                            useFloating = uiStore.bottomBarFloating
                        }
                    }

                    Scaffold(
                        bottomBar = {
                            BottomBar(
                                showDivider = showDivider,
                                useFloating = useFloating,
                                items = items,
                                selected = pagerState.currentPage,
                                onPageChange = { index -> scope.launch { pagerState.animateScrollToPage(index) } })
                        }) { paddingValues ->
                        HorizontalPager(
                            state = pagerState, beyondViewportPageCount = 0, userScrollEnabled = false
                        ) { page ->
                            when (page) {
                                0 -> MainScreen(
                                    running = mainDesign.clashRunning.value,
                                    currentProfile = mainDesign.profileName.value,
                                    currentForwarded = mainDesign.forwarded.value,
                                    currentUpload = mainDesign.upload.value,
                                    currentDownload = mainDesign.download.value,
                                    currentMode = mainDesign.mode.value,
                                    currentProxy = mainDesign.currentProxy.value,
                                    currentProxySubtitle = mainDesign.currentProxySubtitle.value,
                                    currentDelay = mainDesign.currentDelay.value,
                                    isToggling = mainDesign.toggleInProgress.value,
                                    onRequest = { mainDesign.request(it) })

                                1 -> ProxyScreen(proxyDesign, mainDesign.clashRunning.value)
                                2 -> ProfilesScreen(profilesDesign)
                                3 -> SettingsScreen(onMainRequest = { req ->
                                    when (req) {
                                        MainDesign.Request.OpenProfiles -> mainDesign.request(req)
                                        MainDesign.Request.OpenProviders -> mainDesign.request(req)
                                        MainDesign.Request.OpenLogs -> mainDesign.request(req)
                                        MainDesign.Request.OpenAbout -> mainDesign.request(req)
                                        else -> Unit
                                    }
                                }, onSettingsRequest = { req ->
                                    when (req) {
                                        SettingsDesign.Request.StartApp -> startActivity(AppSettingsActivity::class.intent)
                                        SettingsDesign.Request.StartNetwork -> startActivity(NetworkSettingsActivity::class.intent)
                                        SettingsDesign.Request.StartOverride -> startActivity(
                                            OverrideSettingsActivity::class.intent
                                        )

                                        SettingsDesign.Request.StartMetaFeature -> startActivity(
                                            MetaFeatureSettingsActivity::class.intent
                                        )
                                    }
                                })
                            }
                        }
                        LaunchedEffect(pagerState.currentPage) {
                            when (pagerState.currentPage) {
                                1 -> triggerPrefetchIfNeeded("enter_proxy_tab")
                                2 -> launch { fetchProfiles(profilesDesign) }
                            }
                        }
                    }
                }
            }
        }
        mainDesign.fetch()

        fetchProfiles(profilesDesign)

        proxyDesign.requests.trySend(ProxyDesign.Request.ReloadAll)
        triggerPrefetchIfNeeded("initial")

        val ticker = ticker(TimeUnit.SECONDS.toMillis(1))

        while (isActive) {
            select<Unit> {
                events.onReceive { ev ->
                    when (ev) {
                        Event.ActivityStart, Event.ServiceRecreated, Event.ClashStop, Event.ClashStart, Event.ProfileLoaded, Event.ProfileChanged -> {
                            mainDesign.fetch()
                            val newNames = withClash { queryProxyGroupNames(uiStore.proxyExcludeNotSelectable) }
                            if (newNames != groupNames) {
                                rebuildGroups(newNames)
                                proxyDesign.requests.trySend(ProxyDesign.Request.ReloadAll)
                            }
                            if (ev == Event.ClashStart || ev == Event.ProfileLoaded) {
                                triggerPrefetchIfNeeded(ev.name)
                            }
                            launch { fetchProfiles(profilesDesign) }
                        }

                        Event.ActivityResume -> {
                            launch {
                                delay(100)
                                mainDesign.fetch()
                            }
                        }

                        else -> Unit
                    }
                }
                mainDesign.requests.onReceive { req ->
                    when (req) {
                        MainDesign.Request.ToggleStatus -> {
                            if (mainDesign.toggleInProgress.value) return@onReceive
                            if (clashRunning) stopClashService()
                            else launch { mainDesign.startClash() }
                        }

                        MainDesign.Request.OpenProxy -> {}

                        MainDesign.Request.OpenProfiles -> startActivity(ProfilesActivity::class.intent)
                        MainDesign.Request.OpenProviders -> startActivity(ProvidersActivity::class.intent)
                        MainDesign.Request.OpenLogs -> {
                            if (LogcatService.running) startActivity(LogcatActivity::class.intent)
                            else startActivity(LogsActivity::class.intent)
                        }

                        MainDesign.Request.OpenSettings -> startActivity(SettingsActivity::class.intent)
                        MainDesign.Request.OpenHelp -> startActivity(HelpActivity::class.intent)
                        MainDesign.Request.OpenAbout -> startActivity(AboutActivity::class.intent)
                    }
                }
                proxyDesign.requests.onReceive { r ->
                    when (r) {
                        ProxyDesign.Request.ReLaunch -> {
                            rebuildGroups(groupNames)
                            triggerPrefetchIfNeeded("relaunch")
                        }

                        ProxyDesign.Request.ReloadAll -> {
                            groupNames.indices.forEach { idx ->
                                proxyDesign.requests.trySend(ProxyDesign.Request.Reload(idx))
                            }
                        }

                        is ProxyDesign.Request.Reload -> {
                            val idx = r.index
                            if (idx !in groupNames.indices) return@onReceive
                            launch {
                                val groupName = groupNames.getOrNull(idx) ?: return@launch
                                reloadLock.withPermit {
                                    if (groupNames.getOrNull(idx) == groupName) {
                                        updateProxyGroupData(idx, groupName, proxyStates, proxyDesign)
                                    }
                                }
                            }
                        }

                        is ProxyDesign.Request.Select -> {
                            val idx = r.index
                            val gName = groupNames.getOrNull(idx) ?: return@onReceive
                            withClash {
                                patchSelector(gName, r.name)
                                proxyStates.getOrNull(idx)?.now = r.name
                            }

                            mainDesign.updateCurrentProxyInfoDebounced()

                            launch {
                                val selectedGroup = withClash {
                                    val group = queryProxyGroup(gName, uiStore.proxySort)
                                    group.proxies.find { it.name == r.name }
                                }
                                if (selectedGroup?.type?.group == true) {
                                    val targetGroupIndex = groupNames.indexOf(r.name)
                                    if (targetGroupIndex >= 0) {
                                        proxyDesign.startBackgroundDelayTest(targetGroupIndex)
                                    }
                                }
                            }

                            proxyDesign.requestRedrawVisible()
                        }

                        is ProxyDesign.Request.UrlTest -> {
                            val idx = r.index
                            val gName = groupNames.getOrNull(idx) ?: return@onReceive
                            launch {
                                withClash { healthCheck(gName) }
                                if (idx in groupNames.indices) {
                                    proxyDesign.requests.send(ProxyDesign.Request.Reload(idx))
                                }
                            }
                        }

                        is ProxyDesign.Request.PatchMode -> {
                            withClash {
                                val o = queryOverride(Clash.OverrideSlot.Session)
                                o.mode = r.mode
                                patchOverride(Clash.OverrideSlot.Session, o)
                                mode = o.mode
                            }
                        }
                    }
                }
                profilesDesign.requests.onReceive { request: ProfilesDesign.Request ->
                    when (request) {
                        ProfilesDesign.Request.Create -> {
                            startActivity(NewProfileActivity::class.intent)
                        }

                        is ProfilesDesign.Request.Active -> {
                            withProfile {
                                setActive(request.profile)
                                profilesDesign.selectedUUID = request.profile.uuid
                            }
                            launch {
                                delay(100)
                                fetchProfiles(profilesDesign)
                            }
                        }

                        is ProfilesDesign.Request.Edit -> {
                            startActivity(NewProfileActivity::class.intent.setUUID(request.profile.uuid))
                        }

                        is ProfilesDesign.Request.Update -> {
                            launch {
                                withProfile { update(request.profile.uuid) }
                                delay(200)
                                fetchProfiles(profilesDesign)
                            }
                        }

                        ProfilesDesign.Request.UpdateAll -> {
                            launch {
                                val profiles =
                                    withProfile { queryAll() }.filter { it.imported && it.type != com.github.kr328.clash.service.model.Profile.Type.File }

                                profiles.forEach { profile ->
                                    try {
                                        withProfile { update(profile.uuid) }
                                    } catch (e: Exception) {
                                        // ignore
                                    }
                                }

                                profilesDesign.finishUpdateAll()
                                fetchProfiles(profilesDesign)
                            }
                        }

                        is ProfilesDesign.Request.Duplicate -> {
                            launch {
                                withProfile { duplicate(request.profile.uuid) }
                                delay(100)
                                fetchProfiles(profilesDesign)
                            }
                        }

                        is ProfilesDesign.Request.Delete -> {
                            launch {
                                withProfile { delete(request.profile.uuid) }
                                delay(100)
                                fetchProfiles(profilesDesign)
                            }
                        }
                    }
                }
                if (clashRunning && activityResumed) {
                    ticker.onReceive {
                        launch { mainDesign.fetchTraffic() }
                    }
                }
            }
        }
    }

    private suspend fun MainDesign.fetch() {
        setClashRunning(this@MainActivity.clashRunning)
        val state = withClash { queryTunnelState() }
        val providers = withClash { queryProviders() }
        setMode(state.mode)
        setHasProviders(providers.isNotEmpty())
        withProfile { setProfileName(queryActive()?.name) }

        this.updateCurrentProxyInfo()
    }

    private suspend fun fetchProfiles(design: ProfilesDesign) {
        val profiles = withProfile { queryAll() }
        val active = withProfile { queryActive() }
        design.patchProfiles(profiles)
        design.selectedUUID = active?.uuid
    }

    private suspend fun findEndpointProxy(
        groupName: String, visitedGroups: MutableSet<String> = mutableSetOf()
    ): Triple<String, String?, Int>? {
        if (groupName in visitedGroups) return null
        visitedGroups.add(groupName)

        return try {
            withClash {
                val group = queryProxyGroup(groupName, uiStore.proxySort)
                val currentProxy = group.now

                val proxyData = group.proxies.find { it.name == currentProxy }
                if (proxyData != null) {
                    if (!proxyData.type.group) {
                        Triple(currentProxy, proxyData.subtitle, proxyData.delay)
                    } else {
                        findEndpointProxy(currentProxy, visitedGroups)
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun findProxyInAllGroups(proxyName: String): com.github.kr328.clash.core.model.Proxy? {
        return try {
            withClash {
                val groupNames = queryProxyGroupNames(false)
                for (groupName in groupNames) {
                    val group = queryProxyGroup(groupName, uiStore.proxySort)
                    val proxy = group.proxies.find { it.name == proxyName }
                    if (proxy != null) return@withClash proxy
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun MainDesign.updateCurrentProxyInfo() {
        try {
            val groupNames = withClash { queryProxyGroupNames(false) }
            if (groupNames.isEmpty()) return

            val firstGroupName = groupNames[0]
            val endpointInfo = findEndpointProxy(firstGroupName)

            if (endpointInfo != null) {
                val (proxyName, subtitle, delay) = endpointInfo
                withContext(Dispatchers.Main) {
                    currentProxy.value = proxyName
                    currentProxySubtitle.value = subtitle
                    currentDelay.value = delay.takeIf { it > 0 }
                }
            } else {
                val firstGroup = withClash { queryProxyGroup(firstGroupName, uiStore.proxySort) }
                val currentProxyName = firstGroup.now
                val currentProxyData = firstGroup.proxies.find { it.name == currentProxyName }

                withContext(Dispatchers.Main) {
                    currentProxy.value = currentProxyName
                    currentProxySubtitle.value = currentProxyData?.subtitle
                    currentDelay.value = currentProxyData?.delay?.takeIf { it > 0 }
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun MainDesign.updateCurrentProxyInfoDebounced() {
        val now = System.currentTimeMillis()
        if (now - lastProxyInfoUpdate < proxyInfoUpdateDebounce) return
        lastProxyInfoUpdate = now
        launch {
            delay(100)
            updateCurrentProxyInfo()
        }
    }

    private suspend fun MainDesign.fetchTraffic() {
        withClash { setForwarded(queryTrafficTotal()) }

        this.updateCurrentProxyInfo()
    }

    private suspend fun MainDesign.startClash() {
        val active = withProfile { queryActive() }
        if (active == null || !active.imported) {
            showToast(R.string.no_profile_selected, ToastDuration.Long)
            return
        }
        val vpnRequest = startClashService()
        try {
            if (vpnRequest != null) {
                val result = awaitActivityResult(vpnRequest)
                if (result.resultCode == RESULT_OK) {
                    startClashService()
                } else {
                    showToast(R.string.unable_to_start_vpn, ToastDuration.Long)
                }
            }
        } catch (e: Exception) {
            showToast(R.string.unable_to_start_vpn, ToastDuration.Long)
        }
    }

    private suspend fun queryAppVersionName(): String = withContext(Dispatchers.IO) {
        packageManager.getPackageInfo(packageName, 0).versionName + "\n" + Bridge.nativeCoreVersion().replace("_", "-")
    }

    private suspend fun awaitActivityResult(intent: Intent): ActivityResult = suspendCancellableCoroutine { cont ->
        startForResultContinuation = cont
        cont.invokeOnCancellation {
            startForResultContinuation = null
        }
        startForResultLauncher.launch(intent)
    }

    private enum class Event {
        ServiceRecreated, ActivityStart, ActivityStop, ActivityResume, ActivityPause, ClashStop, ClashStart, ProfileLoaded, ProfileChanged, ProfileUpdateCompleted, ProfileUpdateFailed,
    }
}