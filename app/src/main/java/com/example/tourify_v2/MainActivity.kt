package com.example.tourify_v2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.tourify_v2.model.ChatBookingProposal
import com.example.tourify_v2.model.TicketBookingRequest
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.tourify_v2.screen.AccountScreen
import com.example.tourify_v2.screen.BottomNavBar
import com.example.tourify_v2.screen.ChatbotAssistantScreen
import com.example.tourify_v2.screen.chatbotCircleReveal
import com.example.tourify_v2.screen.DestinationScreen
import com.example.tourify_v2.screen.DestinationsScreen
import com.example.tourify_v2.screen.HomeScreen
import com.example.tourify_v2.screen.LoginScreen
import com.example.tourify_v2.screen.RegisterScreen
import com.example.tourify_v2.screen.TicketsScreen
import com.example.tourify_v2.ui.theme.TourifyForest
import com.example.tourify_v2.ui.theme.Tourify_v2Theme
import com.example.tourify_v2.viewmodel.AuthViewModel
import com.example.tourify_v2.viewmodel.TourifyViewModel
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

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
// splash pas aplikasi baru dibuka
private fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
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
// minta izin jaringan lokal kalau dibutuhin
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
// penjelasan kalau akses jaringan belum diizinin
private fun LocalNetworkPermissionScreen(onRequestPermission: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
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
    val currentUser = authViewModel.currentUsername
    // Token ikut dikirim supaya tiket dan chat mengikuti akun yang sudah login.
    val currentAccessToken = authViewModel.currentAccessToken

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "login"
    var showLoginHomeWipe by remember { mutableStateOf(false) }
    var showBookingSuccess by rememberSaveable { mutableStateOf(false) }
    var isTicketDetailVisible by rememberSaveable { mutableStateOf(false) }
    // State ini menjaga layar chat tetap hidup selama animasi buka dan tutup.
    var isChatbotVisible by remember { mutableStateOf(false) }
    var isChatbotOpen by remember { mutableStateOf(false) }
    var chatbotButtonCenter by remember { mutableStateOf(Offset.Zero) }
    val chatbotReveal = remember { Animatable(0f) }

    // Kirim presensi selama aplikasi dibuka.
    LaunchedEffect(lifecycleOwner, authViewModel.currentAccessToken) {
        if (authViewModel.currentAccessToken.isBlank()) return@LaunchedEffect

        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                authViewModel.updateCustomerPresence()
                delay(60_000L)
            }
        }
    }

    // Segarkan foto saat beranda dibuka supaya perubahan profil langsung terlihat.
    LaunchedEffect(currentRoute, currentUser, authViewModel.currentAccessToken) {
        if (currentRoute == "home" && currentUser.isNotBlank()) {
            authViewModel.loadProfileImage()
        }
    }

    BackHandler(enabled = isChatbotVisible) {
        isChatbotOpen = false
    }

    // Reveal bergerak maju saat chat dibuka dan mundur sebelum layar dilepas.
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
    // animasi geser antar tab dan ke halaman destinasi
    val tabEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        val initialRoute = initialState.destination.route
        val targetRoute = targetState.destination.route
        when {
            initialRoute == "home" && targetRoute == "destination/{destinationId}" ->
                slideInHorizontally(
                    initialOffsetX = { width -> width },
                    animationSpec = tween(360, easing = FastOutSlowInEasing)
                )
            initialRoute == "destination/{destinationId}" && targetRoute == "home" ->
                slideInHorizontally(
                    initialOffsetX = { width -> -width },
                    animationSpec = tween(360, easing = FastOutSlowInEasing)
                )
            else -> {
                val direction = tabRouteDirection(initialRoute, targetRoute)
                if (direction == 0) EnterTransition.None else slideInHorizontally(
                    initialOffsetX = { width -> width * direction },
                    animationSpec = tween(360, easing = FastOutSlowInEasing)
                )
            }
        }
    }
    val tabExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        val initialRoute = initialState.destination.route
        val targetRoute = targetState.destination.route
        when {
            initialRoute == "home" && targetRoute == "destination/{destinationId}" ->
                slideOutHorizontally(
                    targetOffsetX = { width -> -width },
                    animationSpec = tween(360, easing = FastOutSlowInEasing)
                )
            initialRoute == "destination/{destinationId}" && targetRoute == "home" ->
                slideOutHorizontally(
                    targetOffsetX = { width -> width },
                    animationSpec = tween(360, easing = FastOutSlowInEasing)
                )
            else -> {
                val direction = tabRouteDirection(initialRoute, targetRoute)
                if (direction == 0) ExitTransition.None else slideOutHorizontally(
                    targetOffsetX = { width -> -width * direction },
                    animationSpec = tween(360, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isTicketDetailVisible) Color.Transparent else Color.White)
    ) {
        Scaffold(
            containerColor = if (isTicketDetailVisible) Color.Transparent else Color.White,
            // Hilangkan ruang latar putih di atas; tiap layar sudah mengatur posisi konten dari status bar.
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.statusBars),
            bottomBar = {
                if (
                    currentRoute in listOf("home", "destinations", "ticket", "account") &&
                    !(currentRoute == "ticket" && isTicketDetailVisible)
                ) {
                    BottomNavBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo("home")
                                launchSingleTop = true
                            }
                        },
                        // Tombol asisten di bar bawah membuka layar chat.
                        onAssistantClick = {
                            isChatbotVisible = true
                            isChatbotOpen = true
                        },
                        onAssistantCenterChanged = { chatbotButtonCenter = it }
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                val layoutDirection = LocalLayoutDirection.current
                val showFloatingNav = currentRoute in listOf("home", "destinations", "ticket", "account") &&
                    !(currentRoute == "ticket" && isTicketDetailVisible)
                // Konten tab berjalan sampai belakang navbar yang mengambang.
                val screenPadding = PaddingValues(
                    start = paddingValues.calculateStartPadding(layoutDirection),
                    top = paddingValues.calculateTopPadding(),
                    end = paddingValues.calculateEndPadding(layoutDirection),
                    bottom = if (showFloatingNav) 0.dp else paddingValues.calculateBottomPadding()
                )
                // daftar route dan layar utama aplikasi
                NavHost(
                    navController = navController,
                    startDestination = "login",
                    modifier = Modifier.padding(screenPadding),
                    enterTransition = tabEnterTransition,
                    exitTransition = tabExitTransition,
                    popEnterTransition = tabEnterTransition,
                    popExitTransition = tabExitTransition
                ) {
                    composable("login") {
                        LoginScreen(
                            authViewModel = authViewModel,
                            onLoginSuccess = { showLoginHomeWipe = true },
                            onNavigateToRegister = { navController.navigate("register") },
                            isActive = currentRoute == "login"
                        )
                    }
                    composable("register") {
                        RegisterScreen(
                            authViewModel = authViewModel,
                            onRegisterSuccess = { navController.popBackStack() },
                            onBackToLogin = { navController.popBackStack() }
                        )
                    }
                    // beranda dan halaman detail destinasi
                    composable("home") {
                        HomeScreen(
                            viewModel = tourifyViewModel,
                            currentUser = currentUser,
                            profileImageUrl = authViewModel.profileImageUrl,
                            onDestinationClick = { id -> navController.navigate("destination/$id") }
                        )
                    }
                    composable("destinations") {
                        DestinationsScreen(
                            viewModel = tourifyViewModel,
                            onDestinationClick = { id -> navController.navigate("destination/$id") }
                        )
                    }
                    composable("destination/{destinationId}") { entry ->
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
                            onTicketDetailVisibilityChanged = { isTicketDetailVisible = it }
                        )
                    }
                    composable("account") {
                        // Tiket dan foto profil disegarkan saat tab akun dibuka.
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
                                // Coba muat ulang ringkasan di profil.
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

        // Layar chat berada di atas seluruh app dan dibuka dari pusat tombol sparkle.
        if (isChatbotVisible) {
            ChatbotAssistantScreen(
                currentUser = currentUser,
                isSending = tourifyViewModel.isSendingChat,
                isBooking = tourifyViewModel.isBooking,
                // Katalog membantu chatbot mengenali nama wahana yang disebut pengguna.
                availableWahana = tourifyViewModel.wahana,
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
                modifier = Modifier
                    .fillMaxSize()
                    .chatbotCircleReveal(
                        progress = chatbotReveal.value,
                        origin = chatbotButtonCenter
                    )
            )
        }
    }
}

private fun tabRouteDirection(initialRoute: String?, targetRoute: String?): Int {
    val initialIndex = tabRouteIndex(initialRoute)
    val targetIndex = tabRouteIndex(targetRoute)
    if (initialIndex < 0 || targetIndex < 0) return 0
    return when {
        targetIndex > initialIndex -> 1
        targetIndex < initialIndex -> -1
        else -> 0
    }
}

private fun tabRouteIndex(route: String?): Int = when (route) {
    "home" -> 0
    "destinations" -> 1
    "ticket" -> 2
    "account" -> 3
    else -> -1
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
                .background(TourifyForest)
        )
    }
}
