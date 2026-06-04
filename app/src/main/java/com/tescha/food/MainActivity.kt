package com.tescha.food

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.tescha.food.ui.components.GlassyBottomBar
import com.tescha.food.ui.navigation.AppNavHost
import com.tescha.food.ui.screens.AuthMode
import com.tescha.food.ui.screens.AuthScreen
import com.tescha.food.ui.screens.WelcomeScreen
import com.tescha.food.ui.theme.MyApplicationTheme
import com.tescha.food.ui.viewmodel.AuthRoute
import com.tescha.food.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MyApplicationApp()
            }
        }
    }
}

// Lista de permisos que la app necesita pedir en tiempo de ejecución
private fun requiredPermissions(): Array<String> {
    val perms = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        perms.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    return perms.toTypedArray()
}

@Composable
fun MyApplicationApp(mainViewModel: MainViewModel = viewModel()) {
    val isLoggedIn  by mainViewModel.isLoggedIn.collectAsState()
    val authRoute   by mainViewModel.authRoute.collectAsState()
    val userLocation by mainViewModel.userLocation.collectAsState()

    // Estado: ¿ya pedimos permisos esta sesión?
    var permissionsRequested by remember { mutableStateOf(false) }

    val multiPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsRequested = true
        val locationGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                              results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) mainViewModel.requestLocation()
    }

    // Pedir todos los permisos al primer Compose del árbol (antes del login)
    LaunchedEffect(Unit) {
        multiPermLauncher.launch(requiredPermissions())
    }

    // Si ya tenemos permiso de ubicación pero aún no la cargamos, pedirla
    LaunchedEffect(isLoggedIn, permissionsRequested) {
        if (isLoggedIn && userLocation == null) {
            mainViewModel.requestLocation()
        }
    }

    if (isLoggedIn) {
        val navController = rememberNavController()
        Scaffold(
            bottomBar = { GlassyBottomBar(navController) }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                AppNavHost(
                    navController = navController,
                    mainViewModel = mainViewModel,
                    userLocation = userLocation,
                )
            }
        }
    } else {
        when (authRoute) {
            AuthRoute.WELCOME -> WelcomeScreen(
                onSignUpClick = { mainViewModel.navigateTo(AuthRoute.REGISTER) },
                onLogInClick  = { mainViewModel.navigateTo(AuthRoute.LOGIN) },
            )
            AuthRoute.LOGIN -> AuthScreen(
                initialMode = AuthMode.LOGIN,
                onBack    = { mainViewModel.navigateTo(AuthRoute.WELCOME) },
                onSuccess = { user -> mainViewModel.onLoginSuccess(user) },
            )
            AuthRoute.REGISTER -> AuthScreen(
                initialMode = AuthMode.REGISTER,
                onBack    = { mainViewModel.navigateTo(AuthRoute.WELCOME) },
                onSuccess = { user -> mainViewModel.onLoginSuccess(user) },
            )
        }
    }
}
