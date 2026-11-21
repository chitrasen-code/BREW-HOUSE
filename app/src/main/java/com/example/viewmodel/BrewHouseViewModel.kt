package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BrewHouseRepository
import com.example.data.BrewHouseSeedData
import com.example.model.AuditLogEntry
import com.example.model.BrewPricing
import com.example.model.CafeOrder
import com.example.model.CartItem
import com.example.model.InventoryItem
import com.example.model.LoyaltyTransaction
import com.example.model.MenuCategory
import com.example.model.MenuProduct
import com.example.model.OrderStatus
import com.example.model.PaymentMethod
import com.example.model.PaymentStatus
import com.example.model.PickupOption
import com.example.model.PromoCoupon
import com.example.model.RewardOffer
import com.example.model.StaffMember
import com.example.model.StockMovement
import com.example.model.StoreLocation
import com.example.model.UserPermissions
import com.example.model.UserProfile
import com.example.model.WorkspaceMode
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

enum class CustomerTab(val label: String) {
  HOME("Home"),
  MENU("Menu"),
  REWARDS("Rewards"),
  ORDERS("Orders"),
  PROFILE("Profile"),
}

enum class MenuSortOption(val label: String) {
  FEATURED("Featured"),
  BEST_SELLERS("Best Sellers"),
  PRICE_LOW("Price: Low to High"),
  PRICE_HIGH("Price: High to Low"),
}

sealed interface AppScreen {
  data object MainTabs : AppScreen
  data class ProductDetail(val productId: String, val editingCartItemId: String? = null) : AppScreen
  data object Cart : AppScreen
  data object Checkout : AppScreen
  data class OrderTracking(val orderId: String) : AppScreen
}

@OptIn(ExperimentalCoroutinesApi::class)
class BrewHouseViewModel(
  private val repository: BrewHouseRepository,
  private val currentUserId: String,
  private val userDisplayName: String?,
  private val userEmail: String?,
  private val isEmailVerified: Boolean,
) : ViewModel() {

  private val _permissions = MutableStateFlow(UserPermissions())
  val permissions: StateFlow<UserPermissions> = _permissions.asStateFlow()

  private val _workspaceMode = MutableStateFlow(WorkspaceMode.CUSTOMER)
  val workspaceMode: StateFlow<WorkspaceMode> = _workspaceMode.asStateFlow()

  private val _selectedTab = MutableStateFlow(CustomerTab.HOME)
  val selectedTab: StateFlow<CustomerTab> = _selectedTab.asStateFlow()

  private val _backStack = MutableStateFlow<List<AppScreen>>(listOf(AppScreen.MainTabs))
  val currentScreen: StateFlow<AppScreen> =
    _backStack.map { it.lastOrNull() ?: AppScreen.MainTabs }
      .stateIn(viewModelScope, SharingStarted.Eagerly, AppScreen.MainTabs)

  val canNavigateBack: StateFlow<Boolean> =
    _backStack.map { it.size > 1 }
      .stateIn(viewModelScope, SharingStarted.Eagerly, false)

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedCategoryId = MutableStateFlow<String?>(null)
  val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

  private val _onlyBestSellers = MutableStateFlow(false)
  val onlyBestSellers: StateFlow<Boolean> = _onlyBestSellers.asStateFlow()

  private val _onlySeasonal = MutableStateFlow(false)
  val onlySeasonal: StateFlow<Boolean> = _onlySeasonal.asStateFlow()

  private val _sortOption = MutableStateFlow(MenuSortOption.FEATURED)
  val sortOption: StateFlow<MenuSortOption> = _sortOption.asStateFlow()

  private val _appliedCouponCode = MutableStateFlow<String?>(null)
  val appliedCouponCode: StateFlow<String?> = _appliedCouponCode.asStateFlow()

  private val _isSubmitting = MutableStateFlow(false)
  val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

  private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val toastEvents = _toastEvents.asSharedFlow()

  init {
    viewModelScope.launch {
      val resolved =
        repository.resolveUserPermissions(
          uid = currentUserId,
          email = userEmail,
          emailVerified = isEmailVerified,
        )
      _permissions.value = resolved
      val initResult =
        repository.ensureProfileAndSeedIfOwner(
          uid = currentUserId,
          displayName = userDisplayName,
          email = userEmail,
          isOwner = resolved.isOwner,
        )
      initResult.onFailure { err ->
        _toastEvents.tryEmit(err.localizedMessage ?: "Failed to initialize profile")
      }
    }
  }

  private fun <T> Flow<T>.toUiState(fallbackMsg: String): StateFlow<UiState<T>> =
    map<T, UiState<T>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        emit(UiState.Error(rootError.localizedMessage ?: fallbackMsg))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading,
      )

  val userProfile: StateFlow<UiState<UserProfile?>> =
    repository.observeUserProfile(currentUserId).toUiState("Unable to load profile")

  val cartItems: StateFlow<UiState<List<CartItem>>> =
    repository.observeCart(currentUserId).toUiState("Unable to load cart")

  val loyaltyTransactions: StateFlow<UiState<List<LoyaltyTransaction>>> =
    repository.observeLoyaltyTransactions(currentUserId).toUiState("Unable to load rewards ledger")

  val stores: StateFlow<UiState<List<StoreLocation>>> =
    repository.observeStores(currentUserId).toUiState("Unable to load store locations")

  val categories: StateFlow<UiState<List<MenuCategory>>> =
    repository.observeCategories(currentUserId).toUiState("Unable to load categories")

  val products: StateFlow<UiState<List<MenuProduct>>> =
    repository.observeProducts(currentUserId).toUiState("Unable to load menu")

  val coupons: StateFlow<UiState<List<PromoCoupon>>> =
    repository.observeCoupons(currentUserId).toUiState("Unable to load offers")

  val customerOrders: StateFlow<UiState<List<CafeOrder>>> =
    repository.observeCustomerOrders(currentUserId).toUiState("Unable to load your orders")

  // Staff / Owner gated streams: only attached when role permits
  val staffOrders: StateFlow<UiState<List<CafeOrder>>> =
    permissions
      .flatMapLatest { perms ->
        if (perms.isStaff || perms.isOwner) repository.observeAllOrders(currentUserId)
        else flowOf(emptyList())
      }
      .toUiState("Unable to load barista order queue")

  val inventoryItems: StateFlow<UiState<List<InventoryItem>>> =
    permissions
      .flatMapLatest { perms ->
        if (perms.isStaff || perms.isOwner) repository.observeInventory(currentUserId)
        else flowOf(emptyList())
      }
      .toUiState("Unable to load inventory")

  val stockMovements: StateFlow<UiState<List<StockMovement>>> =
    permissions
      .flatMapLatest { perms ->
        if (perms.isStaff || perms.isOwner) repository.observeStockMovements(currentUserId)
        else flowOf(emptyList())
      }
      .toUiState("Unable to load stock movements")

  val staffMembers: StateFlow<UiState<List<StaffMember>>> =
    permissions
      .flatMapLatest { perms ->
        if (perms.isOwner) repository.observeStaff(currentUserId)
        else flowOf(emptyList())
      }
      .toUiState("Unable to load staff list")

  val auditLogs: StateFlow<UiState<List<AuditLogEntry>>> =
    permissions
      .flatMapLatest { perms ->
        if (perms.isOwner) repository.observeAuditLogs(currentUserId)
        else flowOf(emptyList())
      }
      .toUiState("Unable to load audit logs")

  val activeStore: StateFlow<StoreLocation> =
    combine(stores, userProfile) { storesState, profileState ->
      val list = (storesState as? UiState.Success)?.data.orEmpty()
      val preferredId = (profileState as? UiState.Success)?.data?.preferredStoreId
      list.firstOrNull { it.id == preferredId } ?: list.firstOrNull() ?: BrewHouseSeedData.defaultStore
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), BrewHouseSeedData.defaultStore)

  val appliedCoupon: StateFlow<PromoCoupon?> =
    combine(coupons, appliedCouponCode) { couponsState, code ->
      if (code.isNullOrBlank()) null
      else {
        val list = (couponsState as? UiState.Success)?.data.orEmpty()
        list.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) && it.active }
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

  val filteredProducts: StateFlow<List<MenuProduct>> =
    combine(
      products,
      searchQuery,
      selectedCategoryId,
      onlyBestSellers,
      onlySeasonal,
      sortOption,
    ) { values ->
      @Suppress("UNCHECKED_CAST")
      val prodState = values[0] as UiState<List<MenuProduct>>
      val query = values[1] as String
      val catId = values[2] as String?
      val bestOnly = values[3] as Boolean
      val seasonalOnly = values[4] as Boolean
      val sort = values[5] as MenuSortOption

      val base = (prodState as? UiState.Success)?.data.orEmpty()
      val filtered =
        base.filter { p ->
          val matchesCat = catId == null || p.categoryId == catId
          val matchesQuery =
            query.isBlank() ||
              p.name.contains(query, ignoreCase = true) ||
              p.categoryName.contains(query, ignoreCase = true) ||
              p.description.contains(query, ignoreCase = true)
          val matchesBest = !bestOnly || p.bestSeller
          val matchesSeasonal = !seasonalOnly || p.seasonal
          matchesCat && matchesQuery && matchesBest && matchesSeasonal
        }
      when (sort) {
        MenuSortOption.FEATURED -> filtered
        MenuSortOption.BEST_SELLERS -> filtered.sortedByDescending { it.bestSeller }
        MenuSortOption.PRICE_LOW -> filtered.sortedBy { it.basePricePaise }
        MenuSortOption.PRICE_HIGH -> filtered.sortedByDescending { it.basePricePaise }
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  // --- Navigation & Filters ---

  fun selectWorkspaceMode(mode: WorkspaceMode) {
    if (mode in _permissions.value.availableModes) {
      _workspaceMode.value = mode
      _backStack.value = listOf(AppScreen.MainTabs)
    }
  }

  fun selectTab(tab: CustomerTab) {
    _selectedTab.value = tab
    _backStack.value = listOf(AppScreen.MainTabs)
  }

  fun openProductDetail(productId: String, editingCartItemId: String? = null) {
    _backStack.value = _backStack.value + AppScreen.ProductDetail(productId, editingCartItemId)
  }

  fun openCart() {
    _backStack.value = _backStack.value + AppScreen.Cart
  }

  fun openCheckout() {
    _backStack.value = _backStack.value + AppScreen.Checkout
  }

  fun openOrderTracking(orderId: String) {
    _backStack.value = _backStack.value + AppScreen.OrderTracking(orderId)
  }

  fun navigateBack() {
    val stack = _backStack.value
    if (stack.size > 1) {
      _backStack.value = stack.dropLast(1)
    }
  }

  fun updateSearchQuery(q: String) {
    _searchQuery.value = q
  }

  fun selectCategory(categoryId: String?) {
    _selectedCategoryId.value = categoryId
  }

  fun toggleBestSellersOnly() {
    _onlyBestSellers.value = !_onlyBestSellers.value
  }

  fun toggleSeasonalOnly() {
    _onlySeasonal.value = !_onlySeasonal.value
  }

  fun setSortOption(option: MenuSortOption) {
    _sortOption.value = option
  }

  fun applyCouponCode(code: String) {
    val clean = code.trim().uppercase()
    if (clean.isBlank()) {
      _appliedCouponCode.value = null
      return
    }
    val list = (coupons.value as? UiState.Success)?.data.orEmpty()
    val found = list.firstOrNull { it.code.equals(clean, ignoreCase = true) && it.active }
    if (found == null) {
      _toastEvents.tryEmit("Coupon '$clean' is invalid or inactive.")
    } else {
      val subtotal = (cartItems.value as? UiState.Success)?.data.orEmpty().sumOf { it.unitPricePaise * it.quantity }
      if (subtotal < found.minOrderPaise) {
        _toastEvents.tryEmit("Minimum order ${BrewPricing.formatInr(found.minOrderPaise)} required for ${found.code}.")
      } else {
        _appliedCouponCode.value = found.code
        _toastEvents.tryEmit("Coupon ${found.code} applied!")
      }
    }
  }

  fun clearCoupon() {
    _appliedCouponCode.value = null
  }

  // --- Customer Mutations ---

  fun saveCartItem(
    editingCartItemId: String?,
    product: MenuProduct,
    quantity: Int,
    temperature: String,
    size: String,
    milkOption: String,
    sugarLevel: String,
    addons: List<String>,
  ) {
    viewModelScope.launch {
      _isSubmitting.value = true
      val res =
        repository.upsertCartItem(
          uid = currentUserId,
          existingCartItemId = editingCartItemId,
          product = product,
          quantity = quantity,
          temperature = temperature,
          size = size,
          milkOption = milkOption,
          sugarLevel = sugarLevel,
          addons = addons,
        )
      _isSubmitting.value = false
      res.onSuccess {
        _toastEvents.tryEmit(
          if (editingCartItemId == null) "${product.name} added to bag"
          else "${product.name} updated"
        )
        navigateBack()
      }.onFailure { err ->
        _toastEvents.tryEmit(err.localizedMessage ?: "Failed to update bag")
      }
    }
  }

  fun updateCartQuantity(cartItemId: String, newQty: Int) {
    viewModelScope.launch {
      repository.updateCartQuantity(currentUserId, cartItemId, newQty)
        .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Could not update quantity") }
    }
  }

  fun removeCartItem(cartItemId: String) {
    viewModelScope.launch {
      repository.removeCartItem(currentUserId, cartItemId)
        .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Could not remove item") }
    }
  }

  fun placeOrder(
    customerName: String,
    customerPhone: String,
    pickupOption: PickupOption,
    pickupInstructions: String,
    paymentMethod: PaymentMethod,
  ) {
    val items = (cartItems.value as? UiState.Success)?.data.orEmpty()
    if (items.isEmpty()) {
      _toastEvents.tryEmit("Your bag is empty.")
      return
    }
    val prodMap = (products.value as? UiState.Success)?.data.orEmpty().associateBy { it.id }
    val store = activeStore.value
    val coupon = appliedCoupon.value

    viewModelScope.launch {
      _isSubmitting.value = true
      val res =
        repository.placeOrder(
          uid = currentUserId,
          customerName = customerName,
          customerPhone = customerPhone,
          store = store,
          cartItems = items,
          productsById = prodMap,
          coupon = coupon,
          pickupOption = pickupOption,
          pickupInstructions = pickupInstructions,
          paymentMethod = paymentMethod,
        )
      _isSubmitting.value = false
      res.onSuccess { orderId ->
        _appliedCouponCode.value = null
        _toastEvents.tryEmit("Order placed! Track live barista progress.")
        _backStack.value = listOf(AppScreen.MainTabs, AppScreen.OrderTracking(orderId))
        _selectedTab.value = CustomerTab.ORDERS
      }.onFailure { err ->
        _toastEvents.tryEmit(err.localizedMessage ?: "Order placement failed")
      }
    }
  }

  fun cancelCustomerOrder(orderId: String, reason: String) {
    viewModelScope.launch {
      _isSubmitting.value = true
      repository.cancelCustomerOrder(currentUserId, orderId, reason)
        .onSuccess { _toastEvents.tryEmit("Order cancelled.") }
        .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Unable to cancel order") }
      _isSubmitting.value = false
    }
  }

  fun redeemReward(offer: RewardOffer) {
    val profile = (userProfile.value as? UiState.Success)?.data
    val currentPoints = profile?.loyaltyPoints ?: 0L
    if (currentPoints < offer.pointsCost) {
      _toastEvents.tryEmit("You need ${offer.pointsCost - currentPoints} more points.")
      return
    }
    viewModelScope.launch {
      _isSubmitting.value = true
      repository.redeemReward(
        uid = currentUserId,
        currentPoints = currentPoints,
        rewardTitle = offer.title,
        pointsCost = offer.pointsCost,
      ).onSuccess { code ->
        _toastEvents.tryEmit("Reward unlocked! Show code $code at the counter.")
      }.onFailure { err ->
        _toastEvents.tryEmit(err.localizedMessage ?: "Redemption failed")
      }
      _isSubmitting.value = false
    }
  }

  fun updateProfile(
    displayName: String,
    phone: String,
    preferredMilk: String,
    notificationsEnabled: Boolean,
  ) {
    viewModelScope.launch {
      _isSubmitting.value = true
      repository.updateProfilePreferences(
        uid = currentUserId,
        displayName = displayName,
        phone = phone,
        preferredMilk = preferredMilk,
        notificationsEnabled = notificationsEnabled,
      ).onSuccess {
        _toastEvents.tryEmit("Profile saved")
      }.onFailure { err ->
        _toastEvents.tryEmit(err.localizedMessage ?: "Failed to save profile")
      }
      _isSubmitting.value = false
    }
  }

  // --- Staff Mutations ---

  fun advanceOrderStatus(
    order: CafeOrder,
    nextStatus: OrderStatus,
    markPaid: Boolean = false,
    cancelReason: String? = null,
  ) {
    val actorName = (userProfile.value as? UiState.Success)?.data?.displayName ?: userDisplayName ?: "Barista"
    val paymentStatus = if (markPaid) PaymentStatus.PAID else null
    viewModelScope.launch {
      repository.updateOrderStatusByStaff(
        orderId = order.id,
        newStatus = nextStatus,
        newPaymentStatus = paymentStatus,
        cancelReason = cancelReason,
        staffNote = null,
        actorName = actorName,
      ).onSuccess {
        _toastEvents.tryEmit("Order ${order.orderNumber} -> ${nextStatus.label}")
      }.onFailure { err ->
        _toastEvents.tryEmit(err.localizedMessage ?: "Failed to update order")
      }
    }
  }

  fun adjustInventory(item: InventoryItem, delta: Long, reason: String) {
    val actorName = (userProfile.value as? UiState.Success)?.data?.displayName ?: userDisplayName ?: "Staff"
    viewModelScope.launch {
      repository.adjustInventoryStock(item, delta, reason, actorName)
        .onSuccess { _toastEvents.tryEmit("Updated ${item.name} stock") }
        .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Stock update failed") }
    }
  }

  // --- Admin Mutations ---

  fun saveProduct(product: MenuProduct, isNew: Boolean) {
    viewModelScope.launch {
      repository.saveProduct(product, isNew)
        .onSuccess { _toastEvents.tryEmit("Saved ${product.name}") }
        .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Failed to save product") }
    }
  }

  fun deleteProduct(productId: String) {
    viewModelScope.launch {
      repository.deleteProduct(productId)
        .onSuccess { _toastEvents.tryEmit("Product deleted") }
        .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Failed to delete product") }
    }
  }

  fun saveCategory(category: MenuCategory, isNew: Boolean) {
    viewModelScope.launch {
      repository.saveCategory(category, isNew)
        .onSuccess { _toastEvents.tryEmit("Saved category ${category.name}") }
        .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Failed to save category") }
    }
  }

  fun saveCoupon(coupon: PromoCoupon, isNew: Boolean) {
    viewModelScope.launch {
      repository.saveCoupon(coupon, isNew)
        .onSuccess { _toastEvents.tryEmit("Saved coupon ${coupon.code}") }
        .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Failed to save coupon") }
    }
  }

  fun saveStaffMember(member: StaffMember, isNew: Boolean) {
    viewModelScope.launch {
      repository.saveStaffMember(member, isNew)
        .onSuccess { _toastEvents.tryEmit("Saved staff member ${member.name}") }
        .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Failed to save staff member") }
    }
  }

  fun updateStoreSettings(store: StoreLocation) {
    viewModelScope.launch {
      repository.updateStoreSettings(store)
        .onSuccess { _toastEvents.tryEmit("Store settings updated") }
        .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Failed to update store") }
    }
  }
}
