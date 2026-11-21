package com.example

import com.example.data.BrewHouseSeedData
import com.example.model.BrewPricing
import com.example.model.CartItem
import com.example.model.LoyaltyTier
import com.example.model.PromoCoupon
import org.junit.Assert.assertEquals
import org.junit.Test

class BrewPricingTest {
  @Test
  fun customizedBeveragePriceIncludesSizeMilkAndAddons() {
    val latte = BrewHouseSeedData.defaultProducts.first { it.id == "prod_caffe_latte" }
    // Base ₹190 (19000) + Large ₹60 (6000) + Oat Milk ₹45 (4500) + Extra Shot ₹50 (5000) = ₹345 (34500)
    val unitPrice =
      BrewPricing.calculateCustomizedUnitPricePaise(
        product = latte,
        size = "LARGE",
        milkOption = "Oat Milk",
        addons = listOf("Extra Espresso Shot"),
      )
    assertEquals(34500L, unitPrice)
    assertEquals("₹345", BrewPricing.formatInr(unitPrice))
  }

  @Test
  fun bakeryProductIgnoresBeverageUpcharges() {
    val muffin = BrewHouseSeedData.defaultProducts.first { it.id == "prod_blueberry_muffin" }
    val unitPrice =
      BrewPricing.calculateCustomizedUnitPricePaise(
        product = muffin,
        size = "LARGE",
        milkOption = "Oat Milk",
        addons = listOf("Extra Espresso Shot"),
      )
    assertEquals(13000L, unitPrice)
  }

  @Test
  fun orderBreakdownAppliesCouponAndGstAccurately() {
    val items =
      listOf(
        CartItem(
          id = "1",
          productId = "prod_cappuccino",
          productName = "Cappuccino",
          quantity = 2L,
          unitPricePaise = 20000L,
        )
      )
    val coupon =
      PromoCoupon(
        id = "c1",
        code = "BREW10",
        discountType = "PERCENT",
        discountValue = 10L,
        minOrderPaise = 20000L,
        maxUses = 100L,
        usedCount = 0L,
        active = true,
      )
    // Subtotal = 40000, 10% discount = 4000, taxable = 36000, 5% tax = 1800, total = 37800 (₹378)
    val breakdown = BrewPricing.calculateOrderBreakdown(items, coupon, taxRateBps = 500L)
    assertEquals(40000L, breakdown.subtotalPaise)
    assertEquals(4000L, breakdown.discountPaise)
    assertEquals(1800L, breakdown.taxPaise)
    assertEquals(37800L, breakdown.totalPaise)
    assertEquals(37L, breakdown.pointsEarned)
  }

  @Test
  fun loyaltyTierResolvesCorrectly() {
    assertEquals(LoyaltyTier.BRONZE, LoyaltyTier.fromLifetimePoints(0L))
    assertEquals(LoyaltyTier.SILVER, LoyaltyTier.fromLifetimePoints(500L))
    assertEquals(LoyaltyTier.GOLD, LoyaltyTier.fromLifetimePoints(1600L))
    assertEquals(LoyaltyTier.PLATINUM, LoyaltyTier.fromLifetimePoints(4000L))
  }
}
