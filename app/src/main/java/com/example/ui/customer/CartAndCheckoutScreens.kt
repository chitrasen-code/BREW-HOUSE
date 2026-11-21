package com.example.ui.customer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BrewHouseSeedData
import com.example.model.BrewPricing
import com.example.model.CartItem
import com.example.model.OrderBreakdown
import com.example.model.PaymentMethod
import com.example.model.PickupOption
import com.example.model.PromoCoupon
import com.example.model.StoreLocation
import com.example.model.UserProfile
import com.example.ui.components.BrewHouseEmptyState
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberGoldContainer
import com.example.ui.theme.CremaWhite
import com.example.ui.theme.EspressoBrown
import com.example.ui.theme.MatchaGreen
import com.example.ui.theme.MochaBrown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
  cartItems: List<CartItem>,
  store: StoreLocation,
  appliedCoupon: PromoCoupon?,
  availableCoupons: List<PromoCoupon>,
  onBack: () -> Unit,
  onBrowseMenu: () -> Unit,
  onEditItem: (CartItem) -> Unit,
  onUpdateQuantity: (String, Int) -> Unit,
  onRemoveItem: (String) -> Unit,
  onApplyCoupon: (String) -> Unit,
  onClearCoupon: () -> Unit,
  onProceedToCheckout: () -> Unit,
) {
  BackHandler(onBack = onBack)
  var couponInput by remember(appliedCoupon?.code) { mutableStateOf(appliedCoupon?.code ?: "") }
  val breakdown = BrewPricing.calculateOrderBreakdown(cartItems, appliedCoupon, store.taxRateBps)

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Your Bag", style = MaterialTheme.typography.headlineSmall) },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("cart_back_button")) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
      )
    },
    bottomBar = {
      if (cartItems.isNotEmpty()) {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 10.dp,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)
          ) {
            PriceBreakdownCard(breakdown = breakdown, taxRateBps = store.taxRateBps)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = onProceedToCheckout,
              shape = RoundedCornerShape(16.dp),
              colors = ButtonDefaults.buttonColors(containerColor = EspressoBrown, contentColor = CremaWhite),
              modifier = Modifier.fillMaxWidth().height(54.dp).testTag("proceed_checkout_button"),
            ) {
              Text(
                text = "Proceed to Checkout • ${BrewPricing.formatInr(breakdown.totalPaise)}",
                style = MaterialTheme.typography.titleMedium,
              )
            }
          }
        }
      }
    },
  ) { innerPadding ->
    if (cartItems.isEmpty()) {
      BrewHouseEmptyState(
        icon = Icons.Rounded.ShoppingBag,
        title = "Your Bag is Empty",
        subtitle = "Explore single-origin espresso, cold coffee, and freshly baked pastries.",
        actionLabel = "Browse Menu",
        onAction = onBrowseMenu,
        modifier = Modifier.fillMaxSize().padding(innerPadding),
      )
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        items(cartItems, key = { it.id }) { item ->
          CartItemCard(
            item = item,
            onEdit = { onEditItem(item) },
            onIncrement = { onUpdateQuantity(item.id, item.quantity.toInt() + 1) },
            onDecrement = { onUpdateQuantity(item.id, item.quantity.toInt() - 1) },
            onRemove = { onRemoveItem(item.id) },
          )
        }

        item {
          Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = "Promo Coupon",
                style = MaterialTheme.typography.titleMedium,
              )
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedTextField(
                  value = couponInput,
                  onValueChange = { couponInput = it.uppercase() },
                  placeholder = { Text("e.g. BREW10") },
                  singleLine = true,
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.weight(1f).testTag("coupon_code_input"),
                )
                if (appliedCoupon != null) {
                  OutlinedButton(
                    onClick = {
                      couponInput = ""
                      onClearCoupon()
                    },
                    modifier = Modifier.height(52.dp),
                  ) {
                    Text("Remove")
                  }
                } else {
                  Button(
                    onClick = { onApplyCoupon(couponInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = EspressoBrown, contentColor = CremaWhite),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(52.dp).testTag("apply_coupon_button"),
                  ) {
                    Text("Apply")
                  }
                }
              }

              val activeCoupons = availableCoupons.filter { it.active }
              if (activeCoupons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  activeCoupons.take(3).forEach { c ->
                    FilterChip(
                      selected = appliedCoupon?.code == c.code,
                      onClick = {
                        couponInput = c.code
                        onApplyCoupon(c.code)
                      },
                      label = { Text(c.code) },
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

@Composable
private fun CartItemCard(
  item: CartItem,
  onEdit: () -> Unit,
  onIncrement: () -> Unit,
  onDecrement: () -> Unit,
  onRemove: () -> Unit,
) {
  val customSummary =
    buildList {
      if (item.temperature != "STANDARD") {
        add(item.temperature.lowercase().replaceFirstChar { it.uppercase() })
      }
      add(BrewPricing.sizeDisplayName(item.size))
      if (item.milkOption.isNotBlank()) add(item.milkOption)
      if (item.sugarLevel.isNotBlank()) add(item.sugarLevel)
      if (item.addons.isNotEmpty()) add(item.addons.joinToString(", "))
    }.joinToString(" • ")

  Card(
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
      Image(
        painter = painterResource(BrewHouseSeedData.drawableResForImageKey(item.imageKey)),
        contentDescription = item.productName,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(74.dp).clip(RoundedCornerShape(14.dp)),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = item.productName,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
          )
          IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.Edit, contentDescription = "Edit customization", tint = MochaBrown)
          }
          IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove item", tint = MaterialTheme.colorScheme.error)
          }
        }
        Text(
          text = customSummary,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = BrewPricing.formatInr(item.unitPricePaise * item.quantity),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = EspressoBrown,
          )
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(onClick = onDecrement, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.Remove, contentDescription = "Decrease quantity")
              }
              Text(
                text = item.quantity.toString(),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 4.dp),
              )
              IconButton(onClick = onIncrement, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = "Increase quantity")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun PriceBreakdownCard(
  breakdown: OrderBreakdown,
  taxRateBps: Long,
  modifier: Modifier = Modifier,
) {
  val taxPct = taxRateBps / 100.0
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    PriceRow("Subtotal", BrewPricing.formatInr(breakdown.subtotalPaise))
    if (breakdown.discountPaise > 0L) {
      PriceRow("Promo Discount", "-${BrewPricing.formatInr(breakdown.discountPaise)}", highlight = MatchaGreen)
    }
    PriceRow("Taxes (${"%.1f".format(taxPct)}%)", BrewPricing.formatInr(breakdown.taxPaise))
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text("Total", style = MaterialTheme.typography.titleMedium)
      Text(
        text = BrewPricing.formatInr(breakdown.totalPaise),
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = EspressoBrown,
      )
    }
    Text(
      text = "You will earn +${breakdown.pointsEarned} Brew House Rewards points",
      style = MaterialTheme.typography.labelSmall,
      color = MochaBrown,
    )
  }
}

@Composable
private fun PriceRow(
  label: String,
  value: String,
  highlight: androidx.compose.ui.graphics.Color? = null,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
      color = highlight ?: MaterialTheme.colorScheme.onSurface,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
  profile: UserProfile?,
  store: StoreLocation,
  cartItems: List<CartItem>,
  appliedCoupon: PromoCoupon?,
  isSubmitting: Boolean,
  onBack: () -> Unit,
  onPlaceOrder: (
    customerName: String,
    customerPhone: String,
    pickupOption: PickupOption,
    pickupInstructions: String,
    paymentMethod: PaymentMethod,
  ) -> Unit,
) {
  BackHandler(onBack = onBack)

  var customerName by remember(profile?.displayName) { mutableStateOf(profile?.displayName ?: "") }
  var customerPhone by remember(profile?.phone) { mutableStateOf(profile?.phone ?: "") }
  var pickupOption by remember { mutableStateOf(PickupOption.PICKUP_NOW) }
  var pickupNotes by remember { mutableStateOf("") }
  var paymentMethod by remember { mutableStateOf(PaymentMethod.PAY_AT_COUNTER) }

  val breakdown = BrewPricing.calculateOrderBreakdown(cartItems, appliedCoupon, store.taxRateBps)

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Pickup Checkout", style = MaterialTheme.typography.headlineSmall) },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("checkout_back_button")) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
      )
    },
    bottomBar = {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
          Button(
            onClick = {
              onPlaceOrder(customerName, customerPhone, pickupOption, pickupNotes, paymentMethod)
            },
            enabled = !isSubmitting && store.open && store.pickupEnabled && paymentMethod == PaymentMethod.PAY_AT_COUNTER,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EspressoBrown, contentColor = CremaWhite),
            modifier = Modifier.fillMaxWidth().height(54.dp).testTag("place_order_button"),
          ) {
            if (isSubmitting) {
              CircularProgressIndicator(modifier = Modifier.size(22.dp), color = CremaWhite, strokeWidth = 2.dp)
            } else {
              Text(
                text = "Place Pickup Order • ${BrewPricing.formatInr(breakdown.totalPaise)}",
                style = MaterialTheme.typography.titleMedium,
              )
            }
          }
        }
      }
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Cafe location card
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = MochaBrown)
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(text = store.name, style = MaterialTheme.typography.titleMedium)
            Text(text = store.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "Hours: ${store.openingHours}", style = MaterialTheme.typography.labelSmall, color = MochaBrown)
          }
        }
      }

      // Customer Contact
      Text("Pickup Contact", style = MaterialTheme.typography.titleMedium)
      OutlinedTextField(
        value = customerName,
        onValueChange = { customerName = it },
        label = { Text("Your Name") },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("checkout_name_input"),
      )
      OutlinedTextField(
        value = customerPhone,
        onValueChange = { customerPhone = it },
        label = { Text("Phone Number (optional)") },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("checkout_phone_input"),
      )

      // Pickup Window
      Text("Pickup Timing", style = MaterialTheme.typography.titleMedium)
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PickupOption.entries.forEach { option ->
          Surface(
            shape = RoundedCornerShape(14.dp),
            border =
              BorderStroke(
                width = if (pickupOption == option) 2.dp else 1.dp,
                color = if (pickupOption == option) EspressoBrown else MaterialTheme.colorScheme.outlineVariant,
              ),
            modifier = Modifier.fillMaxWidth().clickable { pickupOption = option },
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(selected = pickupOption == option, onClick = { pickupOption = option })
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(option.label, style = MaterialTheme.typography.titleSmall)
                Text(option.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }

      OutlinedTextField(
        value = pickupNotes,
        onValueChange = { pickupNotes = it },
        label = { Text("Barista / Pickup Instructions (optional)") },
        placeholder = { Text("e.g. Extra hot, sleeve on cup") },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      // Payment Method
      Text("Payment Method", style = MaterialTheme.typography.titleMedium)
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PaymentMethod.entries.forEach { method ->
          Surface(
            shape = RoundedCornerShape(14.dp),
            border =
              BorderStroke(
                width = if (paymentMethod == method) 2.dp else 1.dp,
                color = if (paymentMethod == method) EspressoBrown else MaterialTheme.colorScheme.outlineVariant,
              ),
            modifier = Modifier.fillMaxWidth().clickable { paymentMethod = method },
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(selected = paymentMethod == method, onClick = { paymentMethod = method })
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(method.label, style = MaterialTheme.typography.titleSmall)
                Text(
                  text =
                    if (method == PaymentMethod.PAY_AT_COUNTER)
                      "Order goes straight to the barista queue; pay when picking up."
                    else
                      "Requires Razorpay merchant keys & server webhook configuration.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }

      if (paymentMethod == PaymentMethod.ONLINE_RAZORPAY) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = AmberGoldContainer,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "Razorpay online checkout is not configured with live merchant keys in this build. Please select Pay at Café Counter to place your pickup order.",
            style = MaterialTheme.typography.bodySmall,
            color = AmberGold,
            modifier = Modifier.padding(12.dp),
          )
        }
      }

      // Summary
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.LocalCafe, contentDescription = null, tint = MochaBrown, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Order Summary (${cartItems.sumOf { it.quantity }} items)", style = MaterialTheme.typography.titleMedium)
          }
          PriceBreakdownCard(breakdown = breakdown, taxRateBps = store.taxRateBps)
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
