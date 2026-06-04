package com.tescha.food.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tescha.food.ui.screens.*
import com.tescha.food.ui.viewmodel.ActivityViewModel
import com.tescha.food.ui.viewmodel.CartViewModel
import com.tescha.food.ui.viewmodel.CheckoutViewModel
import com.tescha.food.ui.viewmodel.MainViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home     : Screen("home",     "Inicio",    Icons.Default.Home)
    object Activity : Screen("activity", "Actividad", Icons.Default.History)
    object Wallet   : Screen("wallet",   "Billetera", Icons.Default.AccountBalanceWallet)
    object Profile  : Screen("profile",  "Perfil",    Icons.Default.Person)
    object Merchant : Screen("merchant", "Mi Tienda", Icons.Default.Storefront)
    object Admin    : Screen("admin",    "Admin",     Icons.Default.AdminPanelSettings)
    // Flujo de compra
    object Cart          : Screen("cart",           "", Icons.Default.Home)
    object Checkout      : Screen("checkout",       "", Icons.Default.Home)
    object PaymentResult : Screen("payment_result", "", Icons.Default.Home)
}

val mainDestinations = listOf(
    Screen.Home,
    Screen.Activity,
    Screen.Wallet,
    Screen.Profile,
    Screen.Merchant,
)

@Composable
fun AppNavHost(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    userLocation: Pair<Double, Double>? = null,
) {
    val cartViewModel: CartViewModel = viewModel()
    val checkoutViewModel: CheckoutViewModel = viewModel()
    val activityViewModel: ActivityViewModel = viewModel()
    val summary by cartViewModel.summary.collectAsState()

    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                userLocation = userLocation,
                cartViewModel = cartViewModel,
                onProductAdded = {
                    if (cartViewModel.summary.value.items.isNotEmpty())
                        navController.navigate(Screen.Cart.route)
                },
            )
        }

        composable(Screen.Activity.route) { ActivityScreen(activityViewModel = activityViewModel) }

        composable(Screen.Wallet.route) {
            val currentUser by mainViewModel.currentUser.collectAsState()
            WalletScreen(balance = currentUser?.balance ?: 0.0)
        }

        composable(Screen.Profile.route) {
            val currentUser by mainViewModel.currentUser.collectAsState()
            when (currentUser?.email) {
                "admin@tescha.edu.mx" -> AdminScreen()
                else -> ProfileScreen(
                    onLogout = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                        mainViewModel.logout()
                    }
                )
            }
        }

        composable(Screen.Merchant.route) { MerchantScreen() }

        composable(Screen.Cart.route) {
            CartScreen(
                cartViewModel = cartViewModel,
                onBack = { navController.popBackStack() },
                onCheckout = { navController.navigate(Screen.Checkout.route) },
            )
        }

        composable(Screen.Checkout.route) {
            val currentUser by mainViewModel.currentUser.collectAsState()
            CheckoutScreen(
                summary = summary,
                checkoutViewModel = checkoutViewModel,
                currentUser = currentUser,
                onBack = { navController.popBackStack() },
                onPaymentProcessing = {
                    navController.navigate(Screen.PaymentResult.route) {
                        popUpTo(Screen.Cart.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.PaymentResult.route) {
            PaymentResultScreen(
                summary = summary,
                checkoutViewModel = checkoutViewModel,
                onFinish = {
                    // Refrescar el saldo del usuario tras el descuento
                    mainViewModel.refreshCurrentUser()
                    // Iniciar repartidor fantasma con coords de la primera tienda del carrito
                    val firstItem = summary.items.firstOrNull()
                    val storeLat = firstItem?.product?.merchantLatitude ?: 19.2591
                    val storeLng = firstItem?.product?.merchantLongitude ?: -98.8975
                    activityViewModel.startGhostDelivery(storeLat, storeLng)
                    cartViewModel.clear()
                    navController.navigate(Screen.Activity.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onError = {
                    // Pago fallido: volver al inicio sin vaciar el carrito
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
            )
        }
    }
}
