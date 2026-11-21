package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val email: String? = null,
  val phone: String? = null,
  val preferredStoreId: String? = null,
  val preferredMilk: String? = null,
  val loyaltyPoints: Long = 0L,
  val lifetimePoints: Long = 0L,
  val notificationsEnabled: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CartItem(
  @DocumentId val id: String = "",
  val productId: String = "",
  val productName: String = "",
  val imageKey: String = "hot_latte",
  val quantity: Long = 1L,
  val unitPricePaise: Long = 0L,
  val temperature: String = "HOT",
  val size: String = "MEDIUM",
  val milkOption: String = "Whole Milk",
  val sugarLevel: String = "Regular sugar",
  val addons: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class LoyaltyTransaction(
  @DocumentId val id: String = "",
  val userId: String = "",
  val type: String = "EARN",
  val pointsDelta: Long = 0L,
  val description: String = "",
  val orderId: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class StoreLocation(
  @DocumentId val id: String = "",
  val name: String = "",
  val address: String = "",
  val phone: String = "",
  val openingHours: String = "",
  val pickupInstructions: String? = null,
  val currencyCode: String = "INR",
  val taxRateBps: Long = 500L,
  val open: Boolean = true,
  val pickupEnabled: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class MenuCategory(
  @DocumentId val id: String = "",
  val name: String = "",
  val slug: String = "",
  val displayOrder: Long = 0L,
  val active: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class MenuProduct(
  @DocumentId val id: String = "",
  val name: String = "",
  val categoryId: String = "",
  val categoryName: String = "",
  val description: String = "",
  val ingredients: String? = null,
  val basePricePaise: Long = 0L,
  val imageKey: String = "hot_latte",
  val available: Boolean = true,
  val bestSeller: Boolean = false,
  val seasonal: Boolean = false,
  val allowHot: Boolean = true,
  val allowIced: Boolean = true,
  val allowMilkOptions: Boolean = true,
  val allowSugarOptions: Boolean = true,
  val allowAddons: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CafeOrder(
  @DocumentId val id: String = "",
  val orderNumber: String = "",
  val userId: String = "",
  val customerName: String = "",
  val customerPhone: String? = null,
  val storeId: String = "",
  val storeName: String = "",
  val pickupOption: String = PickupOption.PICKUP_NOW.name,
  val pickupInstructions: String? = null,
  val status: String = OrderStatus.PENDING.name,
  val paymentMethod: String = PaymentMethod.PAY_AT_COUNTER.name,
  val paymentStatus: String = PaymentStatus.UNPAID.name,
  val couponCode: String? = null,
  val subtotalPaise: Long = 0L,
  val discountPaise: Long = 0L,
  val taxPaise: Long = 0L,
  val totalPaise: Long = 0L,
  val loyaltyPointsEarned: Long = 0L,
  val itemsSummary: List<String> = emptyList(),
  val itemCount: Long = 0L,
  val cancelReason: String? = null,
  val staffNote: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PromoCoupon(
  @DocumentId val id: String = "",
  val code: String = "",
  val discountType: String = "FIXED",
  val discountValue: Long = 0L,
  val minOrderPaise: Long = 0L,
  val maxUses: Long = 100L,
  val usedCount: Long = 0L,
  val active: Boolean = true,
  val description: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class InventoryItem(
  @DocumentId val id: String = "",
  val name: String = "",
  val category: String = "",
  val unit: String = "",
  val currentStock: Long = 0L,
  val lowStockThreshold: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class StockMovement(
  @DocumentId val id: String = "",
  val inventoryItemId: String = "",
  val itemName: String = "",
  val delta: Long = 0L,
  val reason: String = "",
  val actorUid: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class StaffMember(
  @DocumentId val id: String = "",
  val uid: String = "",
  val name: String = "",
  val email: String = "",
  val role: String = "BARISTA",
  val storeId: String = "store_main",
  val active: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AuditLogEntry(
  @DocumentId val id: String = "",
  val actorUid: String = "",
  val actorName: String = "",
  val action: String = "",
  val targetPath: String = "",
  val details: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

enum class OrderStatus(val label: String) {
  PENDING("Pending"),
  CONFIRMED("Confirmed"),
  PREPARING("Preparing"),
  READY_FOR_PICKUP("Ready for Pickup"),
  COMPLETED("Completed"),
  CANCELLED("Cancelled");

  companion object {
    fun safeValueOf(raw: String): OrderStatus =
      entries.firstOrNull { it.name == raw } ?: PENDING
  }
}

enum class PaymentMethod(val label: String) {
  PAY_AT_COUNTER("Pay at Café Counter"),
  ONLINE_RAZORPAY("Razorpay Online (Config Required)");

  companion object {
    fun safeValueOf(raw: String): PaymentMethod =
      entries.firstOrNull { it.name == raw } ?: PAY_AT_COUNTER
  }
}

enum class PaymentStatus(val label: String) {
  UNPAID("Unpaid"),
  PAID("Paid"),
  FAILED("Failed"),
  REFUNDED("Refunded");

  companion object {
    fun safeValueOf(raw: String): PaymentStatus =
      entries.firstOrNull { it.name == raw } ?: UNPAID
  }
}

enum class PickupOption(val label: String, val subtitle: String) {
  PICKUP_NOW("Pick up ASAP", "Ready in 8–12 mins"),
  SCHEDULED_15M("In 15 mins", "Freshly timed at bar"),
  SCHEDULED_30M("In 30 mins", "Scheduled pickup");

  companion object {
    fun safeValueOf(raw: String): PickupOption =
      entries.firstOrNull { it.name == raw } ?: PICKUP_NOW
  }
}

enum class LoyaltyTier(val label: String, val minLifetimePoints: Long, val nextThreshold: Long?) {
  BRONZE("Bronze", 0L, 500L),
  SILVER("Silver", 500L, 1500L),
  GOLD("Gold", 1500L, 3500L),
  PLATINUM("Platinum", 3500L, null);

  companion object {
    fun fromLifetimePoints(points: Long): LoyaltyTier =
      when {
        points >= PLATINUM.minLifetimePoints -> PLATINUM
        points >= GOLD.minLifetimePoints -> GOLD
        points >= SILVER.minLifetimePoints -> SILVER
        else -> BRONZE
      }
  }
}

enum class WorkspaceMode(val label: String) {
  CUSTOMER("Customer"),
  BARISTA("Barista Queue"),
  OWNER("Owner Admin"),
}

data class UserPermissions(
  val isOwner: Boolean = false,
  val isStaff: Boolean = false,
) {
  val availableModes: List<WorkspaceMode>
    get() = buildList {
      add(WorkspaceMode.CUSTOMER)
      if (isStaff || isOwner) add(WorkspaceMode.BARISTA)
      if (isOwner) add(WorkspaceMode.OWNER)
    }
}

data class RewardOffer(
  val id: String,
  val title: String,
  val subtitle: String,
  val pointsCost: Long,
  val badge: String,
)
