package com.example.ui.customer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.BrewPricing
import com.example.model.CafeOrder
import com.example.model.OrderStatus
import com.example.model.PaymentStatus
import com.example.model.PickupOption
import com.example.ui.components.BrewHouseEmptyState
import com.example.ui.components.OrderStatusPill
import com.example.ui.components.PaymentStatusPill
import com.example.ui.theme.BerryRed
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.EspressoBrown
import com.example.ui.theme.MatchaGreen
import com.example.ui.theme.MochaBrown
import com.example.viewmodel.UiState

@Composable
fun OrdersScreen(
  ordersState: UiState<List<CafeOrder>>,
  onOrderClick: (String) -> Unit,
  onExploreMenu: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize()) {
    Text(
      text = "Your Orders",
      style = MaterialTheme.typography.headlineLarge,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )

    when (ordersState) {
      UiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = EspressoBrown)
        }
      }
      is UiState.Error -> {
        BrewHouseEmptyState(
          icon = Icons.AutoMirrored.Rounded.ReceiptLong,
          title = "Orders Unavailable",
          subtitle = ordersState.message,
        )
      }
      is UiState.Success -> {
        val orders = ordersState.data
        if (orders.isEmpty()) {
          BrewHouseEmptyState(
            icon = Icons.Rounded.Coffee,
            title = "No Pickup Orders Yet",
            subtitle = "Customize a handcrafted espresso or cold brew and track it live here.",
            actionLabel = "Order Coffee",
            onAction = onExploreMenu,
          )
        } else {
          LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(orders, key = { it.id }) { order ->
              OrderSummaryCard(order = order, onClick = { onOrderClick(order.id) })
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OrderSummaryCard(
  order: CafeOrder,
  onClick: () -> Unit,
) {
  val status = OrderStatus.safeValueOf(order.status)
  val payment = PaymentStatus.safeValueOf(order.paymentStatus)

  Card(
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("order_card_${order.id}"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = order.orderNumber,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = EspressoBrown,
        )
        OrderStatusPill(status = status)
      }
      Text(
        text = order.storeName,
        style = MaterialTheme.typography.bodySmall,
        color = MochaBrown,
      )
      order.itemsSummary.take(2).forEach { line ->
        Text(
          text = line,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (order.itemsSummary.size > 2) {
        Text(
          text = "+${order.itemsSummary.size - 2} more item(s)",
          style = MaterialTheme.typography.labelSmall,
          color = MochaBrown,
        )
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        PaymentStatusPill(paymentStatus = payment)
        Text(
          text = BrewPricing.formatInr(order.totalPaise),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = EspressoBrown,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
  order: CafeOrder?,
  isSubmitting: Boolean,
  onBack: () -> Unit,
  onCancelOrder: (String) -> Unit,
) {
  BackHandler(onBack = onBack)

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(order?.orderNumber ?: "Order Tracking", style = MaterialTheme.typography.headlineSmall) },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("tracking_back_button")) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
      )
    },
  ) { innerPadding ->
    if (order == null) {
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = EspressoBrown)
      }
    } else {
      val currentStatus = OrderStatus.safeValueOf(order.status)
      val paymentStatus = PaymentStatus.safeValueOf(order.paymentStatus)
      val pickupOption = PickupOption.safeValueOf(order.pickupOption)
      val steps =
        listOf(
          OrderStatus.PENDING,
          OrderStatus.CONFIRMED,
          OrderStatus.PREPARING,
          OrderStatus.READY_FOR_PICKUP,
          OrderStatus.COMPLETED,
        )
      val activeStepIndex = steps.indexOf(currentStatus)

      Column(
        modifier =
          Modifier.fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = EspressoBrown),
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Order ${order.orderNumber}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
              )
              OrderStatusPill(status = currentStatus)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "${order.storeName} • ${pickupOption.label}",
              style = MaterialTheme.typography.bodyMedium,
              color = CaramelGold,
            )
            if (!order.pickupInstructions.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Note: ${order.pickupInstructions}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
              )
            }
          }
        }

        if (currentStatus == OrderStatus.CANCELLED) {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text("Order Cancelled", style = MaterialTheme.typography.titleMedium, color = BerryRed)
              Text(
                text = order.cancelReason ?: "This order was cancelled.",
                style = MaterialTheme.typography.bodySmall,
                color = BerryRed,
              )
            }
          }
        } else {
          Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text("Live Barista Progress", style = MaterialTheme.typography.titleMedium)
              steps.forEachIndexed { idx, step ->
                val reached = idx <= activeStepIndex
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Surface(
                    shape = CircleShape,
                    color = if (reached) MatchaGreen else MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.size(28.dp),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      if (reached) {
                        Icon(
                          imageVector = Icons.Rounded.CheckCircle,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.onPrimary,
                          modifier = Modifier.size(18.dp),
                        )
                      } else {
                        Text(
                          text = "${idx + 1}",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                    }
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                    text = step.label,
                    style =
                      MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (idx == activeStepIndex) FontWeight.Bold else FontWeight.Normal
                      ),
                    color = if (reached) EspressoBrown else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }

        Card(
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Items & Customizations", style = MaterialTheme.typography.titleMedium)
            order.itemsSummary.forEach { line ->
              Text(text = "• $line", style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              PaymentStatusPill(paymentStatus = paymentStatus)
              Text(
                text = "Total ${BrewPricing.formatInr(order.totalPaise)}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              )
            }
          }
        }

        if (currentStatus == OrderStatus.PENDING) {
          OutlinedButton(
            onClick = { onCancelOrder("Cancelled by customer") },
            enabled = !isSubmitting,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = BerryRed),
            border = BorderStroke(1.dp, BerryRed),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("cancel_order_button"),
          ) {
            Text("Cancel Pending Order")
          }
        }
      }
    }
  }
}
