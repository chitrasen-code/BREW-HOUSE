package com.example.model

import kotlin.math.roundToLong

object BrewPricing {
  val sizes = listOf("SMALL", "MEDIUM", "LARGE")
  val milkOptions = listOf("Whole Milk", "Skimmed Milk", "Oat Milk", "Almond Milk", "Soy Milk")
  val sugarLevels = listOf("No sugar", "Less sugar", "Regular sugar", "Extra sugar")
  val availableAddons =
    listOf(
      "Extra Espresso Shot",
      "Vanilla Syrup",
      "Caramel Syrup",
      "Hazelnut Syrup",
      "Whipped Cream",
      "Chocolate Topping",
    )

  val rewardCatalogue =
    listOf(
      RewardOffer(
        id = "rew_espresso_shot",
        title = "Free Extra Espresso Shot",
        subtitle = "Boost any handcrafted drink on your next bar pickup",
        pointsCost = 80L,
        badge = "Popular",
      ),
      RewardOffer(
        id = "rew_bakery_treat",
        title = "Artisan Bakery Treat",
        subtitle = "Redeem for a Blueberry Muffin or Chocolate Croissant",
        pointsCost = 180L,
        badge = "Bakery",
      ),
      RewardOffer(
        id = "rew_signature_latte",
        title = "Complimentary Signature Latte",
        subtitle = "Any medium hot or iced latte with custom milk",
        pointsCost = 280L,
        badge = "Signature",
      ),
      RewardOffer(
        id = "rew_birthday_box",
        title = "Birthday Brew & Pastry Duo",
        subtitle = "Celebrate with any large beverage and pastry pairing",
        pointsCost = 420L,
        badge = "Special",
      ),
    )

  fun sizeUpchargePaise(size: String, isBeverage: Boolean): Long {
    if (!isBeverage) return 0L
    return when (size.uppercase()) {
      "SMALL" -> 0L
      "LARGE" -> 6000L
      else -> 3000L // MEDIUM
    }
  }

  fun sizeDisplayName(size: String): String =
    when (size.uppercase()) {
      "SMALL" -> "Small"
      "LARGE" -> "Large"
      else -> "Medium"
    }

  fun milkUpchargePaise(milk: String, allowMilk: Boolean): Long {
    if (!allowMilk) return 0L
    return when (milk) {
      "Oat Milk", "Almond Milk" -> 4500L
      "Soy Milk" -> 3500L
      else -> 0L
    }
  }

  fun addonUpchargePaise(addon: String): Long =
    when (addon) {
      "Extra Espresso Shot" -> 5000L
      "Vanilla Syrup", "Caramel Syrup", "Hazelnut Syrup" -> 3500L
      "Whipped Cream", "Chocolate Topping" -> 3000L
      else -> 3000L
    }

  fun calculateCustomizedUnitPricePaise(
    product: MenuProduct,
    size: String,
    milkOption: String,
    addons: List<String>,
  ): Long {
    val isBeverage = product.allowHot || product.allowIced
    val sizeDelta = sizeUpchargePaise(size, isBeverage)
    val milkDelta = milkUpchargePaise(milkOption, product.allowMilkOptions)
    val addonsDelta =
      if (product.allowAddons) addons.distinct().sumOf { addonUpchargePaise(it) } else 0L
    return (product.basePricePaise + sizeDelta + milkDelta + addonsDelta).coerceAtLeast(0L)
  }

  fun calculateDiscountPaise(subtotalPaise: Long, coupon: PromoCoupon?): Long {
    if (coupon == null || !coupon.active || subtotalPaise < coupon.minOrderPaise) return 0L
    if (coupon.usedCount >= coupon.maxUses) return 0L
    val raw =
      if (coupon.discountType == "PERCENT") {
        (subtotalPaise * coupon.discountValue) / 100L
      } else {
        coupon.discountValue
      }
    return raw.coerceIn(0L, subtotalPaise)
  }

  fun calculateOrderBreakdown(
    items: List<CartItem>,
    coupon: PromoCoupon?,
    taxRateBps: Long,
  ): OrderBreakdown {
    val subtotal = items.sumOf { it.unitPricePaise * it.quantity }
    val discount = calculateDiscountPaise(subtotal, coupon)
    val taxable = (subtotal - discount).coerceAtLeast(0L)
    val tax = ((taxable * taxRateBps) / 10_000.0).roundToLong()
    val total = taxable + tax
    val pointsEarned = (total / 1000L).coerceIn(0L, 250L)
    return OrderBreakdown(
      subtotalPaise = subtotal,
      discountPaise = discount,
      taxPaise = tax,
      totalPaise = total,
      pointsEarned = pointsEarned,
    )
  }

  fun formatInr(paise: Long): String {
    val rupees = paise / 100L
    val remPaise = paise % 100L
    return if (remPaise == 0L) {
      "₹$rupees"
    } else {
      "₹$rupees.${remPaise.toString().padStart(2, '0')}"
    }
  }
}

data class OrderBreakdown(
  val subtotalPaise: Long,
  val discountPaise: Long,
  val taxPaise: Long,
  val totalPaise: Long,
  val pointsEarned: Long,
)
