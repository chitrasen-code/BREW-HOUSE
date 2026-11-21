package com.example.data

import android.content.Context
import com.example.R
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
import com.example.model.StaffMember
import com.example.model.StockMovement
import com.example.model.StoreLocation
import com.example.model.UserPermissions
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BrewHouseRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  private fun <T> Flow<T>.guardFirestoreErrors(
    uid: String,
    operationType: OperationType,
    path: String,
  ): Flow<T> = catch { error ->
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val toLog = firestoreError ?: (error as? Exception)
    if (toLog != null && auth.currentUser?.uid == uid) {
      handleFirestoreError(toLog, operationType, path)
    }
    throw (firestoreError ?: error)
  }

  suspend fun resolveUserPermissions(
    uid: String,
    email: String?,
    emailVerified: Boolean,
  ): UserPermissions {
    if (uid.isBlank()) return UserPermissions()
    val isBootstrappedOwner =
      email == BOOTSTRAPPED_OWNER_EMAIL && emailVerified
    val isAdminDoc =
      try {
        db.collection("admins").document(uid).get().await().exists()
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        false
      }
    val isStaffDoc =
      try {
        val snap = db.collection("staff").document(uid).get().await()
        snap.exists() && (snap.getBoolean("active") == true)
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        false
      }
    return UserPermissions(
      isOwner = isBootstrappedOwner || isAdminDoc,
      isStaff = isStaffDoc,
    )
  }

  suspend fun ensureProfileAndSeedIfOwner(
    uid: String,
    displayName: String?,
    email: String?,
    isOwner: Boolean,
  ): Result<Unit> =
    try {
      val activeUid = requireUserId()
      check(activeUid == uid) { "Signed-in user mismatch." }
      val userRef = db.collection("users").document(uid)
      val snap = userRef.get().await()
      if (!snap.exists()) {
        val safeName = (displayName?.takeIf { it.isNotBlank() } ?: "Coffee Lover").take(80)
        val payload =
          mapOf<String, Any?>(
            "userId" to uid,
            "displayName" to safeName,
            "email" to email?.take(120),
            "preferredStoreId" to BrewHouseSeedData.DEFAULT_STORE_ID,
            "preferredMilk" to "Whole Milk",
            "loyaltyPoints" to 0L,
            "lifetimePoints" to 0L,
            "notificationsEnabled" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        userRef.set(payload).await()
      }

      if (isOwner) {
        seedCatalogIfEmpty()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "users/$uid")
      Result.failure(e)
    }

  private suspend fun seedCatalogIfEmpty() {
    val storeSnap = db.collection("stores").document(BrewHouseSeedData.DEFAULT_STORE_ID).get().await()
    val productsSnap = db.collection("products").limit(1).get().await()
    if (storeSnap.exists() && !productsSnap.isEmpty) return

    val batch = db.batch()
    if (!storeSnap.exists()) {
      val s = BrewHouseSeedData.defaultStore
      batch.set(
        db.collection("stores").document(s.id),
        mapOf<String, Any?>(
          "name" to s.name,
          "address" to s.address,
          "phone" to s.phone,
          "openingHours" to s.openingHours,
          "pickupInstructions" to s.pickupInstructions,
          "currencyCode" to s.currencyCode,
          "taxRateBps" to s.taxRateBps,
          "open" to s.open,
          "pickupEnabled" to s.pickupEnabled,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null },
      )
    }

    if (productsSnap.isEmpty) {
      BrewHouseSeedData.defaultCategories.forEach { cat ->
        batch.set(
          db.collection("categories").document(cat.id),
          mapOf(
            "name" to cat.name,
            "slug" to cat.slug,
            "displayOrder" to cat.displayOrder,
            "active" to cat.active,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      BrewHouseSeedData.defaultProducts.forEach { p ->
        batch.set(
          db.collection("products").document(p.id),
          mapOf<String, Any?>(
            "name" to p.name,
            "categoryId" to p.categoryId,
            "categoryName" to p.categoryName,
            "description" to p.description,
            "ingredients" to p.ingredients,
            "basePricePaise" to p.basePricePaise,
            "imageKey" to p.imageKey,
            "available" to p.available,
            "bestSeller" to p.bestSeller,
            "seasonal" to p.seasonal,
            "allowHot" to p.allowHot,
            "allowIced" to p.allowIced,
            "allowMilkOptions" to p.allowMilkOptions,
            "allowSugarOptions" to p.allowSugarOptions,
            "allowAddons" to p.allowAddons,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null },
        )
      }
      BrewHouseSeedData.defaultCoupons.forEach { c ->
        batch.set(
          db.collection("coupons").document(c.id),
          mapOf<String, Any?>(
            "code" to c.code,
            "discountType" to c.discountType,
            "discountValue" to c.discountValue,
            "minOrderPaise" to c.minOrderPaise,
            "maxUses" to c.maxUses,
            "usedCount" to c.usedCount,
            "active" to c.active,
            "description" to c.description,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null },
        )
      }
      BrewHouseSeedData.defaultInventory.forEach { inv ->
        batch.set(
          db.collection("inventory_items").document(inv.id),
          mapOf(
            "name" to inv.name,
            "category" to inv.category,
            "unit" to inv.unit,
            "currentStock" to inv.currentStock,
            "lowStockThreshold" to inv.lowStockThreshold,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
    }

    batch.commit().await()
  }

  fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
    val activeUid = requireUserId()
    check(activeUid == uid)
    emitAll(
      db.collection("users").document(uid)
        .snapshots()
        .map { snap -> snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .guardFirestoreErrors(uid, OperationType.GET, "users/$uid")
    )
  }

  fun observeCart(uid: String): Flow<List<CartItem>> = flow {
    val activeUid = requireUserId()
    check(activeUid == uid)
    emitAll(
      db.collection("users").document(uid).collection("cart")
        .snapshots()
        .map { snap ->
          snap.toObjects(CartItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.createdAt?.seconds ?: 0L }
        }
        .guardFirestoreErrors(uid, OperationType.LIST, "users/$uid/cart")
    )
  }

  fun observeLoyaltyTransactions(uid: String): Flow<List<LoyaltyTransaction>> = flow {
    val activeUid = requireUserId()
    check(activeUid == uid)
    emitAll(
      db.collection("users").document(uid).collection("loyalty_transactions")
        .snapshots()
        .map { snap ->
          snap.toObjects(LoyaltyTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .guardFirestoreErrors(uid, OperationType.LIST, "users/$uid/loyalty_transactions")
    )
  }

  fun observeStores(uid: String): Flow<List<StoreLocation>> = flow {
    requireUserId()
    emitAll(
      db.collection("stores")
        .snapshots()
        .map { snap -> snap.toObjects(StoreLocation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .guardFirestoreErrors(uid, OperationType.LIST, "stores")
    )
  }

  fun observeCategories(uid: String): Flow<List<MenuCategory>> = flow {
    requireUserId()
    emitAll(
      db.collection("categories")
        .snapshots()
        .map { snap ->
          snap.toObjects(MenuCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.displayOrder }
        }
        .guardFirestoreErrors(uid, OperationType.LIST, "categories")
    )
  }

  fun observeProducts(uid: String): Flow<List<MenuProduct>> = flow {
    requireUserId()
    emitAll(
      db.collection("products")
        .snapshots()
        .map { snap ->
          snap.toObjects(MenuProduct::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedWith(compareByDescending<MenuProduct> { it.bestSeller }.thenBy { it.name })
        }
        .guardFirestoreErrors(uid, OperationType.LIST, "products")
    )
  }

  fun observeCoupons(uid: String): Flow<List<PromoCoupon>> = flow {
    requireUserId()
    emitAll(
      db.collection("coupons")
        .snapshots()
        .map { snap -> snap.toObjects(PromoCoupon::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .guardFirestoreErrors(uid, OperationType.LIST, "coupons")
    )
  }

  fun observeCustomerOrders(uid: String): Flow<List<CafeOrder>> = flow {
    val activeUid = requireUserId()
    check(activeUid == uid)
    emitAll(
      db.collection("orders")
        .whereEqualTo("userId", uid)
        .snapshots()
        .map { snap ->
          snap.toObjects(CafeOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .guardFirestoreErrors(uid, OperationType.LIST, "orders")
    )
  }

  fun observeAllOrders(uid: String): Flow<List<CafeOrder>> = flow {
    requireUserId()
    emitAll(
      db.collection("orders")
        .snapshots()
        .map { snap ->
          snap.toObjects(CafeOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .guardFirestoreErrors(uid, OperationType.LIST, "orders")
    )
  }

  fun observeInventory(uid: String): Flow<List<InventoryItem>> = flow {
    requireUserId()
    emitAll(
      db.collection("inventory_items")
        .snapshots()
        .map { snap ->
          snap.toObjects(InventoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.name }
        }
        .guardFirestoreErrors(uid, OperationType.LIST, "inventory_items")
    )
  }

  fun observeStockMovements(uid: String): Flow<List<StockMovement>> = flow {
    requireUserId()
    emitAll(
      db.collection("stock_movements")
        .snapshots()
        .map { snap ->
          snap.toObjects(StockMovement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .guardFirestoreErrors(uid, OperationType.LIST, "stock_movements")
    )
  }

  fun observeStaff(uid: String): Flow<List<StaffMember>> = flow {
    requireUserId()
    emitAll(
      db.collection("staff")
        .snapshots()
        .map { snap -> snap.toObjects(StaffMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .guardFirestoreErrors(uid, OperationType.LIST, "staff")
    )
  }

  fun observeAuditLogs(uid: String): Flow<List<AuditLogEntry>> = flow {
    requireUserId()
    emitAll(
      db.collection("audit_logs")
        .snapshots()
        .map { snap ->
          snap.toObjects(AuditLogEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .guardFirestoreErrors(uid, OperationType.LIST, "audit_logs")
    )
  }

  suspend fun upsertCartItem(
    uid: String,
    existingCartItemId: String?,
    product: MenuProduct,
    quantity: Int,
    temperature: String,
    size: String,
    milkOption: String,
    sugarLevel: String,
    addons: List<String>,
  ): Result<Unit> {
    val safeQty = quantity.coerceIn(1, 20)
    val docId = existingCartItemId?.takeIf { it.isNotBlank() } ?: "cart_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val path = "users/$uid/cart/$docId"
    return try {
      val activeUid = requireUserId()
      check(activeUid == uid)
      val unitPrice =
        BrewPricing.calculateCustomizedUnitPricePaise(
          product = product,
          size = size,
          milkOption = milkOption,
          addons = addons,
        )
      val cleanAddons = addons.distinct().take(10)
      val docRef = db.collection("users").document(uid).collection("cart").document(docId)
      if (existingCartItemId.isNullOrBlank()) {
        docRef.set(
          mapOf(
            "productId" to product.id,
            "productName" to product.name.take(100),
            "imageKey" to product.imageKey,
            "quantity" to safeQty.toLong(),
            "unitPricePaise" to unitPrice,
            "temperature" to temperature,
            "size" to size,
            "milkOption" to milkOption,
            "sugarLevel" to sugarLevel,
            "addons" to cleanAddons,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else {
        docRef.update(
          mapOf(
            "quantity" to safeQty.toLong(),
            "unitPricePaise" to unitPrice,
            "temperature" to temperature,
            "size" to size,
            "milkOption" to milkOption,
            "sugarLevel" to sugarLevel,
            "addons" to cleanAddons,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun updateCartQuantity(uid: String, cartItemId: String, newQuantity: Int): Result<Unit> {
    val path = "users/$uid/cart/$cartItemId"
    return try {
      val activeUid = requireUserId()
      check(activeUid == uid)
      val docRef = db.collection("users").document(uid).collection("cart").document(cartItemId)
      if (newQuantity <= 0) {
        docRef.delete().await()
      } else {
        docRef.update(
          mapOf(
            "quantity" to newQuantity.coerceIn(1, 20).toLong(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun removeCartItem(uid: String, cartItemId: String): Result<Unit> {
    val path = "users/$uid/cart/$cartItemId"
    return try {
      val activeUid = requireUserId()
      check(activeUid == uid)
      db.collection("users").document(uid).collection("cart").document(cartItemId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  suspend fun placeOrder(
    uid: String,
    customerName: String,
    customerPhone: String?,
    store: StoreLocation,
    cartItems: List<CartItem>,
    productsById: Map<String, MenuProduct>,
    coupon: PromoCoupon?,
    pickupOption: PickupOption,
    pickupInstructions: String?,
    paymentMethod: PaymentMethod,
  ): Result<String> {
    val orderId = "ord_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val path = "orders/$orderId"
    return try {
      val activeUid = requireUserId()
      check(activeUid == uid)
      require(cartItems.isNotEmpty()) { "Cart is empty." }
      require(store.open && store.pickupEnabled) { "Selected café is currently closed for pickup." }

      // Recalculate prices from authoritative product catalog rather than trusting client cart cache
      val verifiedItems =
        cartItems.take(30).map { item ->
          val product = productsById[item.productId]
          val verifiedUnitPrice =
            if (product != null) {
              BrewPricing.calculateCustomizedUnitPricePaise(
                product = product,
                size = item.size,
                milkOption = item.milkOption,
                addons = item.addons,
              )
            } else {
              item.unitPricePaise
            }
          item.copy(unitPricePaise = verifiedUnitPrice)
        }

      val breakdown =
        BrewPricing.calculateOrderBreakdown(
          items = verifiedItems,
          coupon = coupon,
          taxRateBps = store.taxRateBps,
        )

      val summaryLines =
        verifiedItems.map { item ->
          val details =
            buildList {
              add(BrewPricing.sizeDisplayName(item.size))
              add(item.temperature.lowercase().replaceFirstChar { it.uppercase() })
              if (item.milkOption.isNotBlank()) add(item.milkOption)
              if (item.addons.isNotEmpty()) add(item.addons.joinToString(", "))
            }.joinToString(" • ")
          val lineTotal = BrewPricing.formatInr(item.unitPricePaise * item.quantity)
          "${item.quantity}x ${item.productName} ($details) — $lineTotal"
        }

      val orderNumber = "BH-${(1000..9999).random()}"
      val batch = db.batch()
      val orderRef = db.collection("orders").document(orderId)
      val orderPayload =
        mapOf<String, Any?>(
          "orderNumber" to orderNumber,
          "userId" to uid,
          "customerName" to customerName.trim().ifBlank { "Guest" }.take(80),
          "customerPhone" to customerPhone?.trim()?.takeIf { it.isNotBlank() }?.take(24),
          "storeId" to store.id,
          "storeName" to store.name.take(100),
          "pickupOption" to pickupOption.name,
          "pickupInstructions" to pickupInstructions?.trim()?.takeIf { it.isNotBlank() }?.take(250),
          "status" to OrderStatus.PENDING.name,
          "paymentMethod" to paymentMethod.name,
          "paymentStatus" to PaymentStatus.UNPAID.name,
          "couponCode" to (coupon?.code?.takeIf { breakdown.discountPaise > 0L }),
          "subtotalPaise" to breakdown.subtotalPaise,
          "discountPaise" to breakdown.discountPaise,
          "taxPaise" to breakdown.taxPaise,
          "totalPaise" to breakdown.totalPaise,
          "loyaltyPointsEarned" to breakdown.pointsEarned,
          "itemsSummary" to summaryLines,
          "itemCount" to verifiedItems.sumOf { it.quantity }.coerceIn(1L, 100L),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }
      batch.set(orderRef, orderPayload)

      // Clear customer cart atomically
      cartItems.forEach { item ->
        batch.delete(db.collection("users").document(uid).collection("cart").document(item.id))
      }

      // Award loyalty points transactionally
      if (breakdown.pointsEarned > 0L) {
        val userRef = db.collection("users").document(uid)
        batch.update(
          userRef,
          mapOf(
            "loyaltyPoints" to FieldValue.increment(breakdown.pointsEarned),
            "lifetimePoints" to FieldValue.increment(breakdown.pointsEarned),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        val txId = "loy_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        batch.set(
          userRef.collection("loyalty_transactions").document(txId),
          mapOf(
            "userId" to uid,
            "type" to "EARN",
            "pointsDelta" to breakdown.pointsEarned,
            "description" to "Earned from pickup order $orderNumber",
            "orderId" to orderId,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
      }

      batch.commit().await()
      Result.success(orderId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun cancelCustomerOrder(uid: String, orderId: String, reason: String): Result<Unit> {
    val path = "orders/$orderId"
    return try {
      val activeUid = requireUserId()
      check(activeUid == uid)
      db.collection("orders").document(orderId)
        .update(
          mapOf(
            "status" to OrderStatus.CANCELLED.name,
            "cancelReason" to reason.trim().ifBlank { "Cancelled by customer" }.take(200),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun redeemReward(
    uid: String,
    currentPoints: Long,
    rewardTitle: String,
    pointsCost: Long,
  ): Result<String> {
    val path = "users/$uid"
    return try {
      val activeUid = requireUserId()
      check(activeUid == uid)
      require(currentPoints >= pointsCost) { "Not enough points to redeem this reward." }
      val voucherCode = "BREW-${(1000..9999).random()}"
      val batch = db.batch()
      val userRef = db.collection("users").document(uid)
      batch.update(
        userRef,
        mapOf(
          "loyaltyPoints" to FieldValue.increment(-pointsCost),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      val txId = "loy_${UUID.randomUUID().toString().replace("-", "").take(14)}"
      batch.set(
        userRef.collection("loyalty_transactions").document(txId),
        mapOf(
          "userId" to uid,
          "type" to "REDEEM",
          "pointsDelta" to -pointsCost,
          "description" to "Redeemed $rewardTitle (Code: $voucherCode)",
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(voucherCode)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun updateProfilePreferences(
    uid: String,
    displayName: String,
    phone: String?,
    preferredMilk: String,
    notificationsEnabled: Boolean,
  ): Result<Unit> {
    val path = "users/$uid"
    return try {
      val activeUid = requireUserId()
      check(activeUid == uid)
      val cleanName = displayName.trim().ifBlank { "Coffee Lover" }.take(80)
      val cleanPhone = phone?.trim()?.take(24) ?: ""
      db.collection("users").document(uid)
        .update(
          mapOf(
            "displayName" to cleanName,
            "phone" to cleanPhone,
            "preferredMilk" to preferredMilk.take(40),
            "notificationsEnabled" to notificationsEnabled,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  // --- Staff Actions ---

  suspend fun updateOrderStatusByStaff(
    orderId: String,
    newStatus: OrderStatus,
    newPaymentStatus: PaymentStatus? = null,
    cancelReason: String? = null,
    staffNote: String? = null,
    actorName: String,
  ): Result<Unit> {
    val path = "orders/$orderId"
    return try {
      val actorUid = requireUserId()
      val updates = mutableMapOf<String, Any>(
        "status" to newStatus.name,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      if (newPaymentStatus != null) updates["paymentStatus"] = newPaymentStatus.name
      if (!cancelReason.isNullOrBlank()) updates["cancelReason"] = cancelReason.trim().take(200)
      if (!staffNote.isNullOrBlank()) updates["staffNote"] = staffNote.trim().take(200)

      val batch = db.batch()
      batch.update(db.collection("orders").document(orderId), updates)
      val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(14)}"
      batch.set(
        db.collection("audit_logs").document(logId),
        mapOf(
          "actorUid" to actorUid,
          "actorName" to actorName.take(80),
          "action" to "ORDER_${newStatus.name}",
          "targetPath" to path,
          "details" to (cancelReason ?: staffNote ?: "Status moved to ${newStatus.label}").take(250),
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun adjustInventoryStock(
    item: InventoryItem,
    delta: Long,
    reason: String,
    actorName: String,
  ): Result<Unit> {
    val path = "inventory_items/${item.id}"
    return try {
      val actorUid = requireUserId()
      val nextStock = (item.currentStock + delta).coerceIn(0L, 1_000_000L)
      val batch = db.batch()
      batch.update(
        db.collection("inventory_items").document(item.id),
        mapOf(
          "currentStock" to nextStock,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      val movId = "mov_${UUID.randomUUID().toString().replace("-", "").take(14)}"
      batch.set(
        db.collection("stock_movements").document(movId),
        mapOf(
          "inventoryItemId" to item.id,
          "itemName" to item.name.take(80),
          "delta" to delta,
          "reason" to reason.trim().ifBlank { "Manual stock adjustment" }.take(160),
          "actorUid" to actorUid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(14)}"
      batch.set(
        db.collection("audit_logs").document(logId),
        mapOf(
          "actorUid" to actorUid,
          "actorName" to actorName.take(80),
          "action" to "INVENTORY_ADJUST",
          "targetPath" to path,
          "details" to "${item.name}: $delta (${item.unit}) -> $nextStock".take(250),
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  // --- Owner / Admin Actions ---

  suspend fun saveProduct(product: MenuProduct, isNew: Boolean): Result<Unit> {
    val docId = if (isNew || product.id.isBlank()) "prod_${UUID.randomUUID().toString().replace("-", "").take(14)}" else product.id
    val path = "products/$docId"
    return try {
      requireUserId()
      val ref = db.collection("products").document(docId)
      if (isNew) {
        ref.set(
          mapOf<String, Any?>(
            "name" to product.name.trim().take(100),
            "categoryId" to product.categoryId,
            "categoryName" to product.categoryName.take(60),
            "description" to product.description.trim().take(400),
            "ingredients" to product.ingredients?.trim()?.takeIf { it.isNotBlank() }?.take(300),
            "basePricePaise" to product.basePricePaise.coerceIn(0L, 500_000L),
            "imageKey" to product.imageKey,
            "available" to product.available,
            "bestSeller" to product.bestSeller,
            "seasonal" to product.seasonal,
            "allowHot" to product.allowHot,
            "allowIced" to product.allowIced,
            "allowMilkOptions" to product.allowMilkOptions,
            "allowSugarOptions" to product.allowSugarOptions,
            "allowAddons" to product.allowAddons,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        ).await()
      } else {
        ref.update(
          mapOf<String, Any?>(
            "name" to product.name.trim().take(100),
            "categoryId" to product.categoryId,
            "categoryName" to product.categoryName.take(60),
            "description" to product.description.trim().take(400),
            "ingredients" to (product.ingredients?.trim()?.take(300) ?: ""),
            "basePricePaise" to product.basePricePaise.coerceIn(0L, 500_000L),
            "imageKey" to product.imageKey,
            "available" to product.available,
            "bestSeller" to product.bestSeller,
            "seasonal" to product.seasonal,
            "allowHot" to product.allowHot,
            "allowIced" to product.allowIced,
            "allowMilkOptions" to product.allowMilkOptions,
            "allowSugarOptions" to product.allowSugarOptions,
            "allowAddons" to product.allowAddons,
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun deleteProduct(productId: String): Result<Unit> {
    val path = "products/$productId"
    return try {
      requireUserId()
      db.collection("products").document(productId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  suspend fun saveCategory(category: MenuCategory, isNew: Boolean): Result<Unit> {
    val docId = if (isNew || category.id.isBlank()) "cat_${UUID.randomUUID().toString().replace("-", "").take(12)}" else category.id
    val path = "categories/$docId"
    return try {
      requireUserId()
      val ref = db.collection("categories").document(docId)
      if (isNew) {
        ref.set(
          mapOf(
            "name" to category.name.trim().take(60),
            "slug" to category.name.trim().lowercase().replace(" ", "-").take(60),
            "displayOrder" to category.displayOrder.coerceIn(0L, 1000L),
            "active" to category.active,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else {
        ref.update(
          mapOf(
            "name" to category.name.trim().take(60),
            "slug" to category.name.trim().lowercase().replace(" ", "-").take(60),
            "displayOrder" to category.displayOrder.coerceIn(0L, 1000L),
            "active" to category.active,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun saveCoupon(coupon: PromoCoupon, isNew: Boolean): Result<Unit> {
    val docId = if (isNew || coupon.id.isBlank()) "coupon_${UUID.randomUUID().toString().replace("-", "").take(12)}" else coupon.id
    val path = "coupons/$docId"
    return try {
      requireUserId()
      val ref = db.collection("coupons").document(docId)
      if (isNew) {
        ref.set(
          mapOf<String, Any?>(
            "code" to coupon.code.trim().uppercase().take(32),
            "discountType" to coupon.discountType,
            "discountValue" to coupon.discountValue.coerceIn(1L, 100_000L),
            "minOrderPaise" to coupon.minOrderPaise.coerceIn(0L, 1_000_000L),
            "maxUses" to coupon.maxUses.coerceIn(1L, 100_000L),
            "usedCount" to 0L,
            "active" to coupon.active,
            "description" to coupon.description?.trim()?.takeIf { it.isNotBlank() }?.take(160),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        ).await()
      } else {
        ref.update(
          mapOf(
            "code" to coupon.code.trim().uppercase().take(32),
            "discountType" to coupon.discountType,
            "discountValue" to coupon.discountValue.coerceIn(1L, 100_000L),
            "minOrderPaise" to coupon.minOrderPaise.coerceIn(0L, 1_000_000L),
            "maxUses" to coupon.maxUses.coerceIn(1L, 100_000L),
            "usedCount" to coupon.usedCount.coerceIn(0L, 100_000L),
            "active" to coupon.active,
            "description" to (coupon.description?.trim()?.take(160) ?: ""),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun saveStaffMember(member: StaffMember, isNew: Boolean): Result<Unit> {
    val cleanUid = member.uid.trim()
    val path = "staff/$cleanUid"
    return try {
      requireUserId()
      require(cleanUid.isNotBlank()) { "Staff Firebase UID is required." }
      val ref = db.collection("staff").document(cleanUid)
      if (isNew) {
        ref.set(
          mapOf(
            "uid" to cleanUid,
            "name" to member.name.trim().take(80),
            "email" to member.email.trim().take(120),
            "role" to member.role,
            "storeId" to member.storeId.ifBlank { BrewHouseSeedData.DEFAULT_STORE_ID },
            "active" to member.active,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else {
        ref.update(
          mapOf(
            "name" to member.name.trim().take(80),
            "email" to member.email.trim().take(120),
            "role" to member.role,
            "storeId" to member.storeId.ifBlank { BrewHouseSeedData.DEFAULT_STORE_ID },
            "active" to member.active,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun updateStoreSettings(store: StoreLocation): Result<Unit> {
    val path = "stores/${store.id}"
    return try {
      requireUserId()
      db.collection("stores").document(store.id)
        .update(
          mapOf(
            "name" to store.name.trim().take(100),
            "address" to store.address.trim().take(200),
            "phone" to store.phone.trim().take(30),
            "openingHours" to store.openingHours.trim().take(120),
            "pickupInstructions" to (store.pickupInstructions?.trim()?.take(250) ?: ""),
            "currencyCode" to store.currencyCode.ifBlank { "INR" }.take(8),
            "taxRateBps" to store.taxRateBps.coerceIn(0L, 3000L),
            "open" to store.open,
            "pickupEnabled" to store.pickupEnabled,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  companion object {
    const val BOOTSTRAPPED_OWNER_EMAIL = "chitrasengautam16@gmail.com"
  }
}
