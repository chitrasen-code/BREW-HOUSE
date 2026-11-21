package com.example.ui.staff

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.BrewPricing
import com.example.model.CafeOrder
import com.example.model.InventoryItem
import com.example.model.OrderStatus
import com.example.model.PaymentStatus
import com.example.model.PickupOption
import com.example.ui.components.BadgePill
import com.example.ui.components.BrewHouseEmptyState
import com.example.ui.components.OrderStatusPill
import com.example.ui.components.PaymentStatusPill
import com.example.ui.theme.BerryRed
import com.example.ui.theme.BerryRedContainer
import com.example.ui.theme.CremaWhite
import com.example.ui.theme.EspressoBrown
import com.example.ui.theme.MatchaGreen
import com.example.ui.theme.MochaBrown
import com.example.viewmodel.UiState

private enum class StaffQueueFilter(val label: String, val status: OrderStatus?) {
  NEW("New", OrderStatus.PENDING),
  CONFIRMED("Confirmed", OrderStatus.CONFIRMED),
  PREPARING("Preparing", OrderStatus.PREPARING),
  READY("Ready", OrderStatus.READY_FOR_PICKUP),
  COMPLETED("Completed", OrderStatus.COMPLETED),
  STOCK("Stock", null),
}

@Composable
fun StaffDashboardScreen(
  ordersState: UiState<List<CafeOrder>>,
  inventoryState: UiState<List<InventoryItem>>,
  onAdvanceOrder: (CafeOrder, OrderStatus, Boolean, String?) -> Unit,
  onAdjustInventory: (InventoryItem, Long, String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedTab by remember { mutableStateOf(StaffQueueFilter.NEW) }
  var rejectingOrder by remember { mutableStateOf<CafeOrder?>(null) }
  var rejectReason by remember { mutableStateOf("") }

  Column(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
      Text("Barista Queue", style = MaterialTheme.typography.headlineLarge)
      Text(
        text = "Live café pickup workflow • Payment & prep tracked separately",
        style = MaterialTheme.typography.bodySmall,
        color = MochaBrown,
      )
    }

    val allOrders = (ordersState as? UiState.Success)?.data.orEmpty()
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(StaffQueueFilter.entries) { filter ->
        val count = if (filter.status != null) allOrders.count { it.status == filter.status.name } else null
        val label = if (count != null) "${filter.label} ($count)" else filter.label
        FilterChip(
          selected = selectedTab == filter,
          onClick = { selectedTab = filter },
          label = { Text(label) },
          modifier = Modifier.testTag("staff_tab_${filter.name.lowercase()}"),
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (selectedTab == StaffQueueFilter.STOCK) {
      val stockList = (inventoryState as? UiState.Success)?.data.orEmpty()
      if (stockList.isEmpty()) {
        BrewHouseEmptyState(
          icon = Icons.Rounded.Inventory2,
          title = "No Inventory Items",
          subtitle = "Add ingredients in Owner Admin to track stock.",
        )
      } else {
        LazyColumn(
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(stockList, key = { it.id }) { item ->
            val low = item.currentStock <= item.lowStockThreshold
            Card(
              shape = MaterialTheme.shapes.medium,
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, if (low) BerryRed else MaterialTheme.colorScheme.outlineVariant),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    if (low) {
                      Spacer(modifier = Modifier.width(8.dp))
                      BadgePill("Low", BerryRedContainer, BerryRed)
                    }
                  }
                  Text("${item.category} • ${item.currentStock} ${item.unit}", style = MaterialTheme.typography.bodySmall, color = MochaBrown)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(onClick = { onAdjustInventory(item, -1L, "Barista shift usage") }) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Decrement stock")
                  }
                  IconButton(onClick = { onAdjustInventory(item, 1L, "Barista restock") }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Increment stock")
                  }
                }
              }
            }
          }
        }
      }
    } else {
      when (ordersState) {
        UiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EspressoBrown)
          }
        }
        is UiState.Error -> {
          BrewHouseEmptyState(
            icon = Icons.Rounded.Coffee,
            title = "Queue Unavailable",
            subtitle = ordersState.message,
          )
        }
        is UiState.Success -> {
          val targetStatus = selectedTab.status
          val filtered = allOrders.filter { it.status == targetStatus?.name }
          if (filtered.isEmpty()) {
            BrewHouseEmptyState(
              icon = Icons.Rounded.Coffee,
              title = "No ${selectedTab.label} Orders",
              subtitle = "Orders in this stage will appear here automatically in real time.",
            )
          } else {
            LazyColumn(
              contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              items(filtered, key = { it.id }) { order ->
                StaffOrderCard(
                  order = order,
                  onAccept = { onAdvanceOrder(order, OrderStatus.CONFIRMED, false, null) },
                  onStartPrep = { onAdvanceOrder(order, OrderStatus.PREPARING, false, null) },
                  onMarkReady = { onAdvanceOrder(order, OrderStatus.READY_FOR_PICKUP, false, null) },
                  onComplete = { onAdvanceOrder(order, OrderStatus.COMPLETED, true, null) },
                  onReject = {
                    rejectingOrder = order
                    rejectReason = ""
                  },
                )
              }
            }
          }
        }
      }
    }
  }

  if (rejectingOrder != null) {
    AlertDialog(
      onDismissRequest = { rejectingOrder = null },
      title = { Text("Reject Order ${rejectingOrder?.orderNumber}") },
      text = {
        OutlinedTextField(
          value = rejectReason,
          onValueChange = { rejectReason = it },
          label = { Text("Reason for customer") },
          placeholder = { Text("e.g. Ingredient temporarily out of stock") },
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            val target = rejectingOrder ?: return@Button
            onAdvanceOrder(
              target,
              OrderStatus.CANCELLED,
              false,
              rejectReason.ifBlank { "Unable to fulfill order" },
            )
            rejectingOrder = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = BerryRed, contentColor = CremaWhite),
        ) {
          Text("Reject Order")
        }
      },
      dismissButton = {
        TextButton(onClick = { rejectingOrder = null }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun StaffOrderCard(
  order: CafeOrder,
  onAccept: () -> Unit,
  onStartPrep: () -> Unit,
  onMarkReady: () -> Unit,
  onComplete: () -> Unit,
  onReject: () -> Unit,
) {
  val status = OrderStatus.safeValueOf(order.status)
  val payment = PaymentStatus.safeValueOf(order.paymentStatus)
  val pickup = PickupOption.safeValueOf(order.pickupOption)

  Card(
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "${order.orderNumber} • ${order.customerName}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          )
          Text(
            text = "${pickup.label} • ${BrewPricing.formatInr(order.totalPaise)}",
            style = MaterialTheme.typography.bodySmall,
            color = MochaBrown,
          )
        }
        OrderStatusPill(status = status)
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PaymentStatusPill(paymentStatus = payment)
        if (!order.pickupInstructions.isNullOrBlank()) {
          BadgePill("Note: ${order.pickupInstructions}", MaterialTheme.colorScheme.surfaceContainer, EspressoBrown)
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      order.itemsSummary.forEach { line ->
        Text("• $line", style = MaterialTheme.typography.bodyMedium)
      }

      Spacer(modifier = Modifier.height(4.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        when (status) {
          OrderStatus.PENDING -> {
            OutlinedButton(
              onClick = onReject,
              colors = ButtonDefaults.outlinedButtonColors(contentColor = BerryRed),
              border = BorderStroke(1.dp, BerryRed),
              modifier = Modifier.weight(1f).height(48.dp),
            ) {
              Text("Reject")
            }
            Button(
              onClick = onAccept,
              colors = ButtonDefaults.buttonColors(containerColor = EspressoBrown, contentColor = CremaWhite),
              modifier = Modifier.weight(1f).height(48.dp).testTag("staff_accept_${order.id}"),
            ) {
              Text("Accept")
            }
          }
          OrderStatus.CONFIRMED -> {
            Button(
              onClick = onStartPrep,
              colors = ButtonDefaults.buttonColors(containerColor = EspressoBrown, contentColor = CremaWhite),
              modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
              Text("Start Preparing")
            }
          }
          OrderStatus.PREPARING -> {
            Button(
              onClick = onMarkReady,
              colors = ButtonDefaults.buttonColors(containerColor = MatchaGreen, contentColor = CremaWhite),
              modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
              Text("Mark Ready for Pickup")
            }
          }
          OrderStatus.READY_FOR_PICKUP -> {
            Button(
              onClick = onComplete,
              colors = ButtonDefaults.buttonColors(containerColor = EspressoBrown, contentColor = CremaWhite),
              modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
              Text("Confirm Pickup & Paid")
            }
          }
          else -> Unit
        }
      }
    }
  }
}
