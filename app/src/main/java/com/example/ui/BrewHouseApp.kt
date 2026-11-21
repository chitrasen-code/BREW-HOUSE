package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.BrewHouseRepository
import com.example.data.attemptAutoSignIn
import com.example.data.signOutUser
import com.example.model.WorkspaceMode
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.auth.WelcomeAuthScreen
import com.example.ui.components.FloatingCartBar
import com.example.ui.customer.CartScreen
import com.example.ui.customer.CheckoutScreen
import com.example.ui.customer.HomeScreen
import com.example.ui.customer.MenuScreen
import com.example.ui.customer.OrderTrackingScreen
import com.example.ui.customer.OrdersScreen
import com.example.ui.customer.ProductDetailScreen
import com.example.ui.customer.ProfileScreen
import com.example.ui.customer.RewardsScreen
import com.example.ui.staff.StaffDashboardScreen
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.CremaWhite
import com.example.ui.theme.EspressoBrown
import com.example.viewmodel.AppScreen
import com.example.viewmodel.BrewHouseViewModel
import com.example.viewmodel.CustomerTab
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun BrewHouseApp(auth: FirebaseAuth = Firebase.auth) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var currentUser by remember { mutableStateOf(auth.currentUser) }

  DisposableEffect(auth) {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      currentUser = firebaseAuth.currentUser
    }
    auth.addAuthStateListener(listener)
    onDispose { auth.removeAuthStateListener(listener) }
  }

  LaunchedEffect(Unit) {
    if (auth.currentUser == null) {
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = { currentUser = auth.currentUser },
        onUnauthenticated = {},
        scope = scope,
      )
    }
  }

  val user = currentUser
  if (user == null) {
    WelcomeAuthScreen(
      onAuthSuccess = { currentUser = auth.currentUser }
    )
  } else {
    val uid = user.uid
    key(uid) {
      val viewModel: BrewHouseViewModel =
        viewModel(
          key = uid,
          factory =
            viewModelFactory {
              initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                  "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                BrewHouseViewModel(
                  repository = BrewHouseRepository(db),
                  currentUserId = uid,
                  userDisplayName = user.displayName,
                  userEmail = user.email,
                  isEmailVerified = user.isEmailVerified,
                )
              }
            },
        )
      AuthenticatedBrewHouseShell(
        viewModel = viewModel,
        userEmail = user.email,
        onSignOut = {
          signOutUser(
            credentialManager = credentialManager,
            scope = scope,
            onSignOutComplete = { currentUser = null },
          )
        },
      )
    }
  }
}

@Composable
private fun AuthenticatedBrewHouseShell(
  viewModel: BrewHouseViewModel,
  userEmail: String?,
  onSignOut: () -> Unit,
) {
  val permissions by viewModel.permissions.collectAsStateWithLifecycle()
  val workspaceMode by viewModel.workspaceMode.collectAsStateWithLifecycle()
  val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
  val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

  val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
  val cartState by viewModel.cartItems.collectAsStateWithLifecycle()
  val loyaltyTxState by viewModel.loyaltyTransactions.collectAsStateWithLifecycle()
  val categoriesState by viewModel.categories.collectAsStateWithLifecycle()
  val productsState by viewModel.products.collectAsStateWithLifecycle()
  val couponsState by viewModel.coupons.collectAsStateWithLifecycle()
  val customerOrdersState by viewModel.customerOrders.collectAsStateWithLifecycle()
  val staffOrdersState by viewModel.staffOrders.collectAsStateWithLifecycle()
  val inventoryState by viewModel.inventoryItems.collectAsStateWithLifecycle()
  val movementsState by viewModel.stockMovements.collectAsStateWithLifecycle()
  val staffState by viewModel.staffMembers.collectAsStateWithLifecycle()
  val auditLogsState by viewModel.auditLogs.collectAsStateWithLifecycle()

  val activeStore by viewModel.activeStore.collectAsStateWithLifecycle()
  val appliedCoupon by viewModel.appliedCoupon.collectAsStateWithLifecycle()
  val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
  val onlyBestSellers by viewModel.onlyBestSellers.collectAsStateWithLifecycle()
  val onlySeasonal by viewModel.onlySeasonal.collectAsStateWithLifecycle()
  val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
  val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(viewModel) {
    viewModel.toastEvents.collect { msg -> snackbarHostState.showSnackbar(msg) }
  }

  val profile = (profileState as? UiState.Success)?.data
  val cartItems = (cartState as? UiState.Success)?.data.orEmpty()
  val cartCount = cartItems.sumOf { it.quantity }.toInt()
  val cartSubtotal = cartItems.sumOf { it.unitPricePaise * it.quantity }
  val productsList = (productsState as? UiState.Success)?.data.orEmpty()
  val couponsList = (couponsState as? UiState.Success)?.data.orEmpty()
  val customerOrders = (customerOrdersState as? UiState.Success)?.data.orEmpty()

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(snackbarHostState) },
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      when (workspaceMode) {
        WorkspaceMode.BARISTA -> {
          Column(modifier = Modifier.fillMaxSize()) {
            WorkspaceTopRibbon(
              currentMode = workspaceMode,
              onBackToCustomer = { viewModel.selectWorkspaceMode(WorkspaceMode.CUSTOMER) },
            )
            StaffDashboardScreen(
              ordersState = staffOrdersState,
              inventoryState = inventoryState,
              onAdvanceOrder = viewModel::advanceOrderStatus,
              onAdjustInventory = viewModel::adjustInventory,
              modifier = Modifier.weight(1f),
            )
          }
        }
        WorkspaceMode.OWNER -> {
          Column(modifier = Modifier.fillMaxSize()) {
            WorkspaceTopRibbon(
              currentMode = workspaceMode,
              onBackToCustomer = { viewModel.selectWorkspaceMode(WorkspaceMode.CUSTOMER) },
            )
            AdminDashboardScreen(
              store = activeStore,
              ordersState = staffOrdersState,
              productsState = productsState,
              categoriesState = categoriesState,
              inventoryState = inventoryState,
              movementsState = movementsState,
              couponsState = couponsState,
              staffState = staffState,
              auditLogsState = auditLogsState,
              onSaveProduct = viewModel::saveProduct,
              onDeleteProduct = viewModel::deleteProduct,
              onSaveCategory = viewModel::saveCategory,
              onAdjustInventory = viewModel::adjustInventory,
              onSaveCoupon = viewModel::saveCoupon,
              onSaveStaff = viewModel::saveStaffMember,
              onSaveStore = viewModel::updateStoreSettings,
              modifier = Modifier.weight(1f),
            )
          }
        }
        WorkspaceMode.CUSTOMER -> {
          when (val screen = currentScreen) {
            is AppScreen.ProductDetail -> {
              val product = productsList.firstOrNull { it.id == screen.productId }
              if (product != null) {
                val editingItem =
                  screen.editingCartItemId?.let { id -> cartItems.firstOrNull { it.id == id } }
                ProductDetailScreen(
                  product = product,
                  editingCartItem = editingItem,
                  defaultMilk = profile?.preferredMilk,
                  isSubmitting = isSubmitting,
                  onBack = viewModel::navigateBack,
                  onSaveToCart = { qty, temp, size, milk, sugar, addons ->
                    viewModel.saveCartItem(
                      editingCartItemId = screen.editingCartItemId,
                      product = product,
                      quantity = qty,
                      temperature = temp,
                      size = size,
                      milkOption = milk,
                      sugarLevel = sugar,
                      addons = addons,
                    )
                  },
                )
              }
            }
            AppScreen.Cart -> {
              CartScreen(
                cartItems = cartItems,
                store = activeStore,
                appliedCoupon = appliedCoupon,
                availableCoupons = couponsList,
                onBack = viewModel::navigateBack,
                onBrowseMenu = {
                  viewModel.selectTab(CustomerTab.MENU)
                },
                onEditItem = { item ->
                  viewModel.openProductDetail(item.productId, item.id)
                },
                onUpdateQuantity = viewModel::updateCartQuantity,
                onRemoveItem = viewModel::removeCartItem,
                onApplyCoupon = viewModel::applyCouponCode,
                onClearCoupon = viewModel::clearCoupon,
                onProceedToCheckout = viewModel::openCheckout,
              )
            }
            AppScreen.Checkout -> {
              CheckoutScreen(
                profile = profile,
                store = activeStore,
                cartItems = cartItems,
                appliedCoupon = appliedCoupon,
                isSubmitting = isSubmitting,
                onBack = viewModel::navigateBack,
                onPlaceOrder = viewModel::placeOrder,
              )
            }
            is AppScreen.OrderTracking -> {
              val trackedOrder = customerOrders.firstOrNull { it.id == screen.orderId }
              OrderTrackingScreen(
                order = trackedOrder,
                isSubmitting = isSubmitting,
                onBack = viewModel::navigateBack,
                onCancelOrder = { reason ->
                  viewModel.cancelCustomerOrder(screen.orderId, reason)
                },
              )
            }
            AppScreen.MainTabs -> {
              BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWide = maxWidth >= 600.dp
                Scaffold(
                  contentWindowInsets = WindowInsets(0),
                  bottomBar = {
                    if (!isWide) {
                      Column {
                        if (selectedTab == CustomerTab.HOME || selectedTab == CustomerTab.MENU) {
                          FloatingCartBar(
                            itemCount = cartCount,
                            subtotalPaise = cartSubtotal,
                            onClick = viewModel::openCart,
                          )
                        }
                        CustomerBottomBar(
                          selectedTab = selectedTab,
                          cartCount = cartCount,
                          onSelectTab = viewModel::selectTab,
                          onOpenCart = viewModel::openCart,
                        )
                      }
                    }
                  },
                ) { tabPadding ->
                  Row(modifier = Modifier.fillMaxSize().padding(tabPadding)) {
                    if (isWide) {
                      CustomerNavRail(
                        selectedTab = selectedTab,
                        cartCount = cartCount,
                        onSelectTab = viewModel::selectTab,
                        onOpenCart = viewModel::openCart,
                      )
                    }
                    Box(
                      modifier = Modifier.fillMaxSize(),
                      contentAlignment = Alignment.TopCenter,
                    ) {
                      val contentModifier = Modifier.widthIn(max = 840.dp).fillMaxSize()
                      when (selectedTab) {
                        CustomerTab.HOME -> {
                          HomeScreen(
                            profile = profile,
                            store = activeStore,
                            categoriesState = categoriesState,
                            productsState = productsState,
                            couponsState = couponsState,
                            searchQuery = searchQuery,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onOpenMenuWithCategory = { catId ->
                              viewModel.selectCategory(catId)
                              viewModel.selectTab(CustomerTab.MENU)
                            },
                            onProductClick = { prodId -> viewModel.openProductDetail(prodId) },
                            onApplyCoupon = { code ->
                              viewModel.applyCouponCode(code)
                              if (cartCount > 0) viewModel.openCart()
                            },
                            onOpenRewards = { viewModel.selectTab(CustomerTab.REWARDS) },
                            modifier = contentModifier,
                          )
                        }
                        CustomerTab.MENU -> {
                          MenuScreen(
                            categoriesState = categoriesState,
                            productsState = productsState,
                            filteredProducts = filteredProducts,
                            searchQuery = searchQuery,
                            selectedCategoryId = selectedCategoryId,
                            onlyBestSellers = onlyBestSellers,
                            onlySeasonal = onlySeasonal,
                            sortOption = sortOption,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onSelectCategory = viewModel::selectCategory,
                            onToggleBestSellers = viewModel::toggleBestSellersOnly,
                            onToggleSeasonal = viewModel::toggleSeasonalOnly,
                            onSelectSort = viewModel::setSortOption,
                            onProductClick = { prodId -> viewModel.openProductDetail(prodId) },
                            modifier = contentModifier,
                          )
                        }
                        CustomerTab.REWARDS -> {
                          RewardsScreen(
                            profile = profile,
                            transactionsState = loyaltyTxState,
                            isSubmitting = isSubmitting,
                            onRedeemReward = viewModel::redeemReward,
                            modifier = contentModifier,
                          )
                        }
                        CustomerTab.ORDERS -> {
                          OrdersScreen(
                            ordersState = customerOrdersState,
                            onOrderClick = { orderId -> viewModel.openOrderTracking(orderId) },
                            onExploreMenu = { viewModel.selectTab(CustomerTab.MENU) },
                            modifier = contentModifier,
                          )
                        }
                        CustomerTab.PROFILE -> {
                          ProfileScreen(
                            profile = profile,
                            userEmail = userEmail,
                            permissions = permissions,
                            currentMode = workspaceMode,
                            isSubmitting = isSubmitting,
                            onSelectWorkspaceMode = viewModel::selectWorkspaceMode,
                            onSaveProfile = viewModel::updateProfile,
                            onOpenOrders = { viewModel.selectTab(CustomerTab.ORDERS) },
                            onSignOut = onSignOut,
                            modifier = contentModifier,
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun WorkspaceTopRibbon(
  currentMode: WorkspaceMode,
  onBackToCustomer: () -> Unit,
) {
  Surface(color = EspressoBrown, modifier = Modifier.statusBarsPadding()) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "${currentMode.label} Mode",
        style = MaterialTheme.typography.labelLarge,
        color = CaramelGold,
        modifier = Modifier.weight(1f),
      )
      TextButton(onClick = onBackToCustomer, modifier = Modifier.testTag("back_to_customer_mode")) {
        Text("Switch to Customer", color = CremaWhite)
      }
    }
  }
}

@Composable
private fun CustomerBottomBar(
  selectedTab: CustomerTab,
  cartCount: Int,
  onSelectTab: (CustomerTab) -> Unit,
  onOpenCart: () -> Unit,
) {
  NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
    CustomerTab.entries.forEach { tab ->
      val selected = selectedTab == tab
      val icon =
        when (tab) {
          CustomerTab.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
          CustomerTab.MENU -> if (selected) Icons.Filled.LocalCafe else Icons.Outlined.LocalCafe
          CustomerTab.REWARDS -> if (selected) Icons.Filled.Star else Icons.Outlined.StarOutline
          CustomerTab.ORDERS -> if (selected) Icons.AutoMirrored.Filled.ReceiptLong else Icons.AutoMirrored.Outlined.ReceiptLong
          CustomerTab.PROFILE -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
        }
      NavigationBarItem(
        selected = selected,
        onClick = { onSelectTab(tab) },
        icon = { Icon(icon, contentDescription = tab.label) },
        label = { Text(tab.label) },
        modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
      )
    }
    IconButton(
      onClick = onOpenCart,
      modifier = Modifier.testTag("nav_cart_button"),
    ) {
      BadgedBox(
        badge = {
          if (cartCount > 0) {
            Badge(containerColor = EspressoBrown, contentColor = CremaWhite) {
              Text(cartCount.toString())
            }
          }
        }
      ) {
        Icon(Icons.Rounded.ShoppingBag, contentDescription = "Shopping bag")
      }
    }
  }
}

@Composable
private fun CustomerNavRail(
  selectedTab: CustomerTab,
  cartCount: Int,
  onSelectTab: (CustomerTab) -> Unit,
  onOpenCart: () -> Unit,
) {
  NavigationRail(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    modifier = Modifier.fillMaxHeight(),
  ) {
    CustomerTab.entries.forEach { tab ->
      val selected = selectedTab == tab
      val icon =
        when (tab) {
          CustomerTab.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
          CustomerTab.MENU -> if (selected) Icons.Filled.LocalCafe else Icons.Outlined.LocalCafe
          CustomerTab.REWARDS -> if (selected) Icons.Filled.Star else Icons.Outlined.StarOutline
          CustomerTab.ORDERS -> if (selected) Icons.AutoMirrored.Filled.ReceiptLong else Icons.AutoMirrored.Outlined.ReceiptLong
          CustomerTab.PROFILE -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
        }
      NavigationRailItem(
        selected = selected,
        onClick = { onSelectTab(tab) },
        icon = { Icon(icon, contentDescription = tab.label) },
        label = { Text(tab.label) },
      )
    }
    Spacer(modifier = Modifier.weight(1f))
    NavigationRailItem(
      selected = false,
      onClick = onOpenCart,
      icon = {
        BadgedBox(
          badge = {
            if (cartCount > 0) {
              Badge { Text(cartCount.toString()) }
            }
          }
        ) {
          Icon(Icons.Rounded.ShoppingBag, contentDescription = "Bag")
        }
      },
      label = { Text("Bag") },
    )
  }
}
