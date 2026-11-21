package com.example.tourify_v2

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.tourify_v2.model.ChatBookingProposal
import com.example.tourify_v2.model.TicketBookingRequest
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.tourify_v2.screen.AccountScreen
import com.example.tourify_v2.screen.AuthScreen
import com.example.tourify_v2.screen.BottomNavBar
import com.example.tourify_v2.screen.ChatbotAssistantScreen
import com.example.tourify_v2.screen.ChatbotConversationState
import com.example.tourify_v2.screen.chatbotCircleReveal
import com.example.tourify_v2.screen.DestinationScreen
import com.example.tourify_v2.screen.DestinationsScreen
import com.example.tourify_v2.screen.HomeScreen
import com.example.tourify_v2.screen.TicketsScreen
import com.example.tourify_v2.ui.theme.Tourify_v2Theme
import com.example.tourify_v2.ui.theme.TourifyMotion
import com.example.tourify_v2.ui.theme.detailEnterTransition
import com.example.tourify_v2.ui.theme.detailExitTransition
import com.example.tourify_v2.viewmodel.AuthViewModel
import com.example.tourify_v2.viewmodel.TourifyViewModel
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

private const val destinationDetailRoute = "destination/{destinationId}"
private val bottomNavRoutes = setOf("home", "destinations", "ticket", "account")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Biar gambar header terlihat sampai ke belakang status bar.
        enableEdgeToEdge()

        setContent {
            Tourify_v2Theme {
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(1200)
                    showSplash = false
                }

                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(durationMillis = 600),
                    label = "splash"
                ) { splashVisible ->
                    if (splashVisible) {
                        SplashScreen()
                    } else {
                        TourifyApp()
                    }
                }
            }
        }
    }
}

@Composable
// Tampilkan splash screen saat aplikasi dibuka.
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Tourify",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(260.dp)
        )
    }
}

@Composable
// Minta izin jaringan lokal kalau belum diizinkan.
private fun TourifyApp() {
    val context = LocalContext.current
    val requiresLocalNetworkPermission = Build.VERSION.SDK_INT >= 37
    var hasLocalNetworkPermission by remember {
        mutableStateOf(
            !requiresLocalNetworkPermission ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_LOCAL_NETWORK
                ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocalNetworkPermission = granted
    }

    LaunchedEffect(Unit) {
        if (requiresLocalNetworkPermission && !hasLocalNetworkPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
    }

    if (!hasLocalNetworkPermission) {
        LocalNetworkPermissionScreen {
            permissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
        return
    }

    TourifyMainContent()
}

@Composable
// Jelaskan kenapa aplikasi perlu akses jaringan lokal.
private fun LocalNetworkPermissionScreen(onRequestPermission: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Izinkan akses jaringan lokal",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Tourify perlu terhubung ke server MAMP di komputer ini untuk memuat akun dan tiket.",
                modifier = Modifier.padding(vertical = 16.dp)
            )
            Button(onClick = onRequestPermission) {
                Text("Izinkan akses")
            }
        }
    }
}

@Composable
private fun TourifyMainContent() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val tourifyViewModel: TourifyViewModel = viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val currentUser = authViewModel.currentUsername
    // Kirim token login supaya tiket dan chat memakai akun yang sama.
    val currentAccessToken = authViewModel.currentAccessToken

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "login"
    var showLoginHomeWipe by remember { mutableStateOf(false) }
    var showBookingSuccess by rememberSaveable { mutableStateOf(false) }
    var isTicketDetailVisible by rememberSaveable { mutableStateOf(false) }
    // Simpan permintaan buka tiket dari Beranda sampai detail menerima ID-nya.
    var requestedTicketId by rememberSaveable(currentUser) {
        mutableStateOf<String?>(null)
    }
    // Simpan percakapan chat saat panel dibuka atau ditutup.
    val chatbotConversation = remember(currentUser) {
        ChatbotConversationState()
    }
    var isChatbotVisible by remember { mutableStateOf(false) }
    var isChatbotOpen by remember { mutableStateOf(false) }
    // Simpan titik tengah item Asisten untuk animasi buka chat.
    var chatbotNavItemCenter by remember { mutableStateOf(Offset.Zero) }
    val chatbotReveal = remember { Animatable(0f) }

    // Ikon status bar mengikuti warna latar halaman.
    DisposableEffect(currentRoute, isChatbotVisible) {
        val window = (context as? Activity)?.window
        val insetsController = window?.let {
            WindowCompat.getInsetsController(
                it,
                it.decorView
            )
        }
        val destinationOwnsStatusBar =
            currentRoute == destinationDetailRoute && !isChatbotVisible
        if (!destinationOwnsStatusBar) {
            insetsController?.isAppearanceLightStatusBars = true
        }
        onDispose {}
    }

    // Kirim info ke server kalau pelanggan sedang aktif di aplikasi.
    LaunchedEffect(lifecycleOwner, authViewModel.currentAccessToken) {
        if (authViewModel.currentAccessToken.isBlank()) return@LaunchedEffect

        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                authViewModel.updateCustomerPresence()
                delay(60_000L)
            }
        }
    }

    // Muat ulang foto profil saat Beranda dibuka.
    LaunchedEffect(currentRoute, currentUser, authViewModel.currentAccessToken) {
        if (currentRoute == "home" && currentUser.isNotBlank()) {
            authViewModel.loadProfileImage()
        }
    }

    BackHandler(enabled = isChatbotVisible) {
        isChatbotOpen = false
    }

    // Jalankan efek buka chat, lalu tutup lagi sebelum panel dilepas.
    LaunchedEffect(isChatbotVisible, isChatbotOpen) {
        if (isChatbotOpen) {
            chatbotReveal.snapTo(0f)
            chatbotReveal.animateTo(
                targetValue = 1f,
                animationSpec = tween(560, easing = FastOutSlowInEasing)
            )
        } else if (isChatbotVisible) {
            chatbotReveal.animateTo(
                targetValue = 0f,
                animationSpec = tween(360, easing = FastOutSlowInEasing)
            )
            isChatbotVisible = false
        }
    }
    // Pakai fade singkat agar perpindahan halaman terasa ringan.
    val systemAnimationsDisabled =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        !ValueAnimator.areAnimatorsEnabled()
    val fadeDurationMillis = if (systemAnimationsDisabled) 0 else 180
    val pageFadeSpec = tween<Float>(
        durationMillis = fadeDurationMillis,
        easing = TourifyMotion.navigationEasing
    )
    // Layar tab baru naik sedikit sambil muncul. Ikuti pengaturan animasi Android.
    val tabOffsetPx = with(LocalDensity.current) { 12.dp.roundToPx() }
    val tabEnterTransition = if (systemAnimationsDisabled) {
        EnterTransition.None
    } else {
        fadeIn(animationSpec = tween(fadeDurationMillis, easing = FastOutSlowInEasing)) +
            slideInVertically(
                animationSpec = tween(fadeDurationMillis, easing = FastOutSlowInEasing),
                initialOffsetY = { tabOffsetPx }
            )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isTicketDetailVisible) {
                    Color.Transparent
                } else {
                    MaterialTheme.colorScheme.background
                }
            )
    ) {
        Scaffold(
            containerColor = if (isTicketDetailVisible) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.background
            },
            // Tiap layar mengatur jarak isinya dari status bar.
            contentWindowInsets = if (currentRoute == destinationDetailRoute) {
                ScaffoldDefaults.contentWindowInsets
                    .exclude(WindowInsets.statusBars)
                    .exclude(WindowInsets.navigationBars)
            } else {
                ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.statusBars)
            },
            bottomBar = {
                if (
                    currentRoute in bottomNavRoutes &&
                    !(currentRoute == "ticket" && isTicketDetailVisible)
                ) {
                    BottomNavBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            // Ketuk tab aktif tidak perlu membuat transisi baru.
                            if (route != navController.currentDestination?.route) {
                                navController.navigate(route) {
                                    popUpTo("home")
                                    launchSingleTop = true
                                }
                            }
                        },
                        // Item Asisten di navbar membuka layar chat.
                        onAssistantClick = {
                            isChatbotVisible = true
                            isChatbotOpen = true
                        },
                        onAssistantCenterChanged = { chatbotNavItemCenter = it }
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                val layoutDirection = LocalLayoutDirection.current
                val showFloatingNav = currentRoute in bottomNavRoutes &&
                    !(currentRoute == "ticket" && isTicketDetailVisible)
                // Detail destinasi dan tiket mengisi area sampai ke belakang navbar.
                val drawsBehindNavigationBar =
                    currentRoute == destinationDetailRoute ||
                        (currentRoute == "ticket" && isTicketDetailVisible)
                val screenPadding = PaddingValues(
                    start = paddingValues.calculateStartPadding(layoutDirection),
                    top = paddingValues.calculateTopPadding(),
                    end = paddingValues.calculateEndPadding(layoutDirection),
                    bottom = if (showFloatingNav || drawsBehindNavigationBar) {
                        0.dp
                    } else {
                        paddingValues.calculateBottomPadding()
                    }
                )
                // Saat pindah tab, hanya layar yang masuk yang naik dan memudar masuk.
                NavHost(
                    navController = navController,
                    startDestination = "login",
                    modifier = Modifier.padding(screenPadding),
                    enterTransition = {
                        if (initialState.destination.route in bottomNavRoutes &&
                            targetState.destination.route in bottomNavRoutes
                        ) {
                            tabEnterTransition
                        } else {
                            fadeIn(animationSpec = pageFadeSpec)
                        }
                    },
                    exitTransition = {
                        if (targetState.destination.route == destinationDetailRoute ||
                            (initialState.destination.route in bottomNavRoutes &&
                                targetState.destination.route in bottomNavRoutes)
                        ) {
                            ExitTransition.KeepUntilTransitionsFinished
                        } else {
                            ExitTransition.None
                        }
                    },
                    popEnterTransition = {
                        when {
                            initialState.destination.route == destinationDetailRoute ->
                                EnterTransition.None
                            initialState.destination.route in bottomNavRoutes &&
                                targetState.destination.route in bottomNavRoutes -> tabEnterTransition
                            else -> fadeIn(animationSpec = pageFadeSpec)
                        }
                    },
                    popExitTransition = {
                        ExitTransition.None
                    }
                ) {
                    composable("login") {
                        // Form masuk dan daftar berganti di dalam layar akun yang sama.
                        AuthScreen(
                            authViewModel = authViewModel,
                            onLoginSuccess = { showLoginHomeWipe = true },
                            isActive = currentRoute == "login",
                            playIntro = !systemAnimationsDisabled,
                            formFadeSpec = pageFadeSpec
                        )
                    }
                    // Rute Beranda dan detail destinasi.
                    composable("home") {
                        HomeScreen(
                            viewModel = tourifyViewModel,
                            currentUser = currentUser,
                            profileImageUrl = authViewModel.profileImageUrl,
                            accessToken = currentAccessToken,
                            onOpenTickets = {
                                requestedTicketId = null
                                navController.navigate("ticket") {
                                    popUpTo("home")
                                    launchSingleTop = true
                                }
                            },
                            onOpenTicket = { ticketId ->
                                requestedTicketId = ticketId
                                navController.navigate("ticket") {
                                    popUpTo("home")
                                    launchSingleTop = true
                                }
                            },
                            // Pintasan bantuan membuka asisten yang sama dengan tombol navbar.
                            onOpenAssistant = {
                                isChatbotVisible = true
                                isChatbotOpen = true
                            },
                            onDestinationClick = { id -> navController.navigate("destination/$id") },
                            // Ketuk judul ini untuk membuka daftar destinasi.
                            onExploreDestinations = {
                                navController.navigate("destinations") {
                                    popUpTo("home")
                                    launchSingleTop = true
                                }
                            },
                            // Siapkan judul saat animasi login berjalan, lalu tampilkan setelah selesai.
                            welcomeTitleIntroPending = showLoginHomeWipe,
                            playWelcomeTitleIntro = currentRoute == "home" && !showLoginHomeWipe
                        )
                    }
                    composable("destinations") {
                        DestinationsScreen(
                            viewModel = tourifyViewModel,
                            onDestinationClick = { id -> navController.navigate("destination/$id") }
                        )
                    }
                    composable(
                        route = destinationDetailRoute,
                        enterTransition = { detailEnterTransition() },
                        popExitTransition = { detailExitTransition() }
                    ) { entry ->
                        DestinationScreen(
                            destinationId = entry.arguments?.getString("destinationId")?.toIntOrNull() ?: -1,
                            viewModel = tourifyViewModel,
                            currentUser = currentUser,
                            accessToken = currentAccessToken,
                            onBack = { navController.popBackStack() },
                            onBookingSuccess = {
                                showBookingSuccess = true
                                navController.navigate("ticket") {
                                    popUpTo("home")
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable("ticket") {
                        TicketsScreen(
                            viewModel = tourifyViewModel,
                            currentUser = currentUser,
                            accessToken = currentAccessToken,
                            showBookingSuccess = showBookingSuccess,
                            onDismissBookingSuccess = { showBookingSuccess = false },
                            onTicketDetailVisibilityChanged = { isTicketDetailVisible = it },
                            requestedTicketId = requestedTicketId,
                            onTicketRequestConsumed = {
                                requestedTicketId = null
                            }
                        )
                    }
                    composable("account") {
                        // Segarkan tiket dan foto profil saat tab akun dibuka.
                        LaunchedEffect(currentUser, currentAccessToken) {
                            if (currentUser.isNotBlank()) {
                                tourifyViewModel.loadTickets(
                                    username = currentUser,
                                    accessToken = currentAccessToken
                                )
                                authViewModel.loadProfileImage()
                            }
                        }
                        AccountScreen(
                            currentUser = currentUser,
                            profileImageUrl = authViewModel.profileImageUrl,
                            isUploadingProfileImage = authViewModel.isUploadingProfileImage,
                            profileImageError = authViewModel.profileImageError,
                            tickets = tourifyViewModel.tickets,
                            isLoadingTickets = tourifyViewModel.isLoadingTickets,
                            ticketsError = tourifyViewModel.ticketsError,
                            onRetryTickets = {
                                // Muat ulang ringkasan profil.
                                tourifyViewModel.loadTickets(
                                    username = currentUser,
                                    accessToken = currentAccessToken,
                                    forceRefresh = true
                                )
                            },
                            onProfileImageSelected = authViewModel::uploadProfileImage,
                            onTicketsClick = { navController.navigate("ticket") },
                            onDestinationsClick = { navController.navigate("destinations") },
                            onLogout = {
                                authViewModel.logout {
                                    navController.navigate("login") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                }

            }
        }

        if (showLoginHomeWipe) {
            LoginHomeWipe(
                onCovered = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onFinished = { showLoginHomeWipe = false }
            )
        }

        // Tampilkan chat di atas layar lain saat item Asisten dipilih.
        if (isChatbotVisible) {
            ChatbotAssistantScreen(
                currentUser = currentUser,
                conversationState = chatbotConversation,
                isSending = tourifyViewModel.isSendingChat,
                isBooking = tourifyViewModel.isBooking,
                // Kirim katalog supaya chat bisa mengenali nama wahana.
                availableWahana = tourifyViewModel.wahana,
                // Pakai tiket milik akun login untuk menjawab kunjungan terdekat.
                userTickets = if (tourifyViewModel.ticketsOwner == currentUser) {
                    tourifyViewModel.tickets
                } else {
                    emptyList()
                },
                onSendMessage = { message, history, onComplete ->
                    tourifyViewModel.sendChatMessage(
                        accessToken = currentAccessToken,
                        message = message,
                        history = history,
                        onComplete = onComplete
                    )
                },
                onConfirmBooking = { proposal, onComplete ->
                    val request = TicketBookingRequest(
                        wahanaId = proposal.wahanaId,
                        visitDate = proposal.visitDate,
                        quantity = proposal.quantity,
                        paymentMethod = proposal.paymentMethod
                    )
                    tourifyViewModel.bookTicket(
                        username = currentUser,
                        accessToken = currentAccessToken,
                        request = request
                    ) { success ->
                        onComplete(success, tourifyViewModel.bookingMessage)
                    }
                },
                onClose = { isChatbotOpen = false },
                // Tautan dari ringkasan pesanan membuka tab Tiket.
                onOpenTickets = {
                    isChatbotOpen = false
                    navController.navigate("ticket") {
                        popUpTo("home")
                        launchSingleTop = true
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .chatbotCircleReveal(
                        progress = chatbotReveal.value,
                        origin = chatbotNavItemCenter
                    )
            )
        }
    }
}

@Composable
private fun LoginHomeWipe(
    onCovered: () -> Unit,
    onFinished: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }.coerceAtLeast(1f)
        val offsetX = remember(widthPx) { Animatable(widthPx) }
        val cornerRadius = 48.dp * abs(offsetX.value / widthPx).coerceIn(0f, 1f)

        LaunchedEffect(widthPx) {
            offsetX.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 440, easing = FastOutSlowInEasing)
            )
            onCovered()
            withFrameNanos { }
            delay(100)
            offsetX.animateTo(
                targetValue = -widthPx,
                animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
            )
            onFinished()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .clip(RoundedCornerShape(cornerRadius))
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
