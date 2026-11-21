package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AuditLogEntry
import com.example.model.BrewPricing
import com.example.model.CafeOrder
import com.example.model.InventoryItem
import com.example.model.MenuCategory
import com.example.model.MenuProduct
import com.example.model.OrderStatus
import com.example.model.PromoCoupon
import com.example.model.StaffMember
import com.example.model.StockMovement
import com.example.model.StoreLocation
import com.example.ui.components.BadgePill
import com.example.ui.components.OrderStatusPill
import com.example.ui.theme.BerryRed
import com.example.ui.theme.BerryRedContainer
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.CremaWhite
import com.example.ui.theme.EspressoBrown
import com.example.ui.theme.MatchaGreen
import com.example.ui.theme.MatchaGreenContainer
import com.example.ui.theme.MochaBrown
import com.example.ui.theme.WarmCream
import com.example.viewmodel.UiState

private enum class AdminSection(val label: String) {
  ANALYTICS("Analytics"),
  PRODUCTS("Products"),
  CATEGORIES("Categories"),
  ORDERS("Orders"),
  INVENTORY("Inventory"),
  COUPONS("Coupons"),
  STAFF("Staff & Logs"),
  STORE("Store"),
}

@Composable
fun AdminDashboardScreen(
  store: StoreLocation,
  ordersState: UiState<List<CafeOrder>>,
  productsState: UiState<List<MenuProduct>>,
  categoriesState: UiState<List<MenuCategory>>,
  inventoryState: UiState<List<InventoryItem>>,
  movementsState: UiState<List<StockMovement>>,
  couponsState: UiState<List<PromoCoupon>>,
  staffState: UiState<List<StaffMember>>,
  auditLogsState: UiState<List<AuditLogEntry>>,
  onSaveProduct: (MenuProduct, Boolean) -> Unit,
  onDeleteProduct: (String) -> Unit,
  onSaveCategory: (MenuCategory, Boolean) -> Unit,
  onAdjustInventory: (InventoryItem, Long, String) -> Unit,
  onSaveCoupon: (PromoCoupon, Boolean) -> Unit,
  onSaveStaff: (StaffMember, Boolean) -> Unit,
  onSaveStore: (StoreLocation) -> Unit,
  modifier: Modifier = Modifier,
) {
  var section by remember { mutableStateOf(AdminSection.ANALYTICS) }
  var editingProduct by remember { mutableStateOf<MenuProduct?>(null) }
  var isNewProduct by remember { mutableStateOf(false) }

  var showCouponDialog by remember { mutableStateOf(false) }
  var showStaffDialog by remember { mutableStateOf(false) }
  var showCategoryDialog by remember { mutableStateOf(false) }

  val orders = (ordersState as? UiState.Success)?.data.orEmpty()
  val products = (productsState as? UiState.Success)?.data.orEmpty()
  val categories = (categoriesState as? UiState.Success)?.data.orEmpty()
  val inventory = (inventoryState as? UiState.Success)?.data.orEmpty()
  val movements = (movementsState as? UiState.Success)?.data.orEmpty()
  val coupons = (couponsState as? UiState.Success)?.data.orEmpty()
  val staffList = (staffState as? UiState.Success)?.data.orEmpty()
  val auditLogs = (auditLogsState as? UiState.Success)?.data.orEmpty()

  Column(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
      Text("Owner Dashboard", style = MaterialTheme.typography.headlineLarge)
      Text(store.name, style = MaterialTheme.typography.bodySmall, color = MochaBrown)
    }

    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(AdminSection.entries) { item ->
        FilterChip(
          selected = section == item,
          onClick = { section = item },
          label = { Text(item.label) },
          modifier = Modifier.testTag("admin_tab_${item.name.lowercase()}"),
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    when (section) {
      AdminSection.ANALYTICS -> AdminAnalyticsView(orders = orders, inventory = inventory)
      AdminSection.PRODUCTS -> AdminProductsView(
        products = products,
        onAdd = {
          editingProduct =
            MenuProduct(
              categoryId = categories.firstOrNull()?.id ?: "cat_espresso",
              categoryName = categories.firstOrNull()?.name ?: "Espresso",
              basePricePaise = 18000L,
            )
          isNewProduct = true
        },
        onEdit = {
          editingProduct = it
          isNewProduct = false
        },
        onToggleAvailable = { prod -> onSaveProduct(prod.copy(available = !prod.available), false) },
        onDelete = { onDeleteProduct(it.id) },
      )
      AdminSection.CATEGORIES -> AdminCategoriesView(
        categories = categories,
        onAdd = { showCategoryDialog = true },
        onToggleActive = { cat -> onSaveCategory(cat.copy(active = !cat.active), false) },
      )
      AdminSection.ORDERS -> AdminOrdersView(orders = orders)
      AdminSection.INVENTORY -> AdminInventoryView(
        inventory = inventory,
        movements = movements,
        onAdjust = onAdjustInventory,
      )
      AdminSection.COUPONS -> AdminCouponsView(
        coupons = coupons,
        onAdd = { showCouponDialog = true },
        onToggle = { c -> onSaveCoupon(c.copy(active = !c.active), false) },
      )
      AdminSection.STAFF -> AdminStaffAndLogsView(
        staffList = staffList,
        auditLogs = auditLogs,
        onAddStaff = { showStaffDialog = true },
        onToggleActive = { m -> onSaveStaff(m.copy(active = !m.active), false) },
      )
      AdminSection.STORE -> AdminStoreSettingsView(store = store, onSave = onSaveStore)
    }
  }

  if (editingProduct != null) {
    ProductEditorDialog(
      initial = editingProduct!!,
      categories = categories,
      isNew = isNewProduct,
      onDismiss = { editingProduct = null },
      onSave = { updated ->
        onSaveProduct(updated, isNewProduct)
        editingProduct = null
      },
    )
  }

  if (showCategoryDialog) {
    SimpleCategoryDialog(
      nextOrder = (categories.maxOfOrNull { it.displayOrder } ?: 0L) + 1L,
      onDismiss = { showCategoryDialog = false },
      onSave = { cat ->
        onSaveCategory(cat, true)
        showCategoryDialog = false
      },
    )
  }

  if (showCouponDialog) {
    SimpleCouponDialog(
      onDismiss = { showCouponDialog = false },
      onSave = { coupon ->
        onSaveCoupon(coupon, true)
        showCouponDialog = false
      },
    )
  }

  if (showStaffDialog) {
    SimpleStaffDialog(
      storeId = store.id,
      onDismiss = { showStaffDialog = false },
      onSave = { member ->
        onSaveStaff(member, true)
        showStaffDialog = false
      },
    )
  }
}

@Composable
private fun AdminAnalyticsView(orders: List<CafeOrder>, inventory: List<InventoryItem>) {
  val activeOrders = orders.filter { it.status != OrderStatus.CANCELLED.name }
  val revenuePaise = activeOrders.sumOf { it.totalPaise }
  val pendingCount = orders.count { it.status == OrderStatus.PENDING.name }
  val completedCount = orders.count { it.status == OrderStatus.COMPLETED.name }
  val avgOrderPaise = if (activeOrders.isNotEmpty()) revenuePaise / activeOrders.size else 0L
  val lowStock = inventory.filter { it.currentStock <= it.lowStockThreshold }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KpiCard("Revenue", BrewPricing.formatInr(revenuePaise), Modifier.weight(1f))
        KpiCard("Orders", "${orders.size}", Modifier.weight(1f))
      }
    }
    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KpiCard("Pending / Comp.", "$pendingCount / $completedCount", Modifier.weight(1f))
        KpiCard("Avg Order", BrewPricing.formatInr(avgOrderPaise), Modifier.weight(1f))
      }
    }
    item {
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("Order Volume by Stage (Live DB)", style = MaterialTheme.typography.titleMedium)
          Spacer(modifier = Modifier.height(12.dp))
          val counts =
            listOf(
              orders.count { it.status == OrderStatus.PENDING.name },
              orders.count { it.status == OrderStatus.CONFIRMED.name },
              orders.count { it.status == OrderStatus.PREPARING.name },
              orders.count { it.status == OrderStatus.READY_FOR_PICKUP.name },
              orders.count { it.status == OrderStatus.COMPLETED.name },
            )
          val maxVal = (counts.maxOrNull() ?: 1).coerceAtLeast(1)
          Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val barWidth = size.width / (counts.size * 2f)
            counts.forEachIndexed { idx, c ->
              val h = (c.toFloat() / maxVal.toFloat()) * (size.height - 12.dp.toPx())
              val x = (idx * 2f + 0.5f) * barWidth
              drawRoundRect(
                color = if (idx == 4) MatchaGreen else CaramelGold,
                topLeft = Offset(x, size.height - h),
                size = Size(barWidth, h.coerceAtLeast(6.dp.toPx())),
                cornerRadius = CornerRadius(12f, 12f),
              )
            }
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("Pend", "Conf", "Prep", "Ready", "Done").forEach { lbl ->
              Text(lbl, style = MaterialTheme.typography.labelSmall, color = MochaBrown)
            }
          }
        }
      }
    }
    if (lowStock.isNotEmpty()) {
      item {
        Card(
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = BerryRedContainer),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Low-Stock Alerts (${lowStock.size})", style = MaterialTheme.typography.titleMedium, color = BerryRed)
            lowStock.forEach { item ->
              Text(
                text = "• ${item.name}: ${item.currentStock} ${item.unit} (threshold ${item.lowStockThreshold})",
                style = MaterialTheme.typography.bodySmall,
                color = BerryRed,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun KpiCard(title: String, value: String, modifier: Modifier = Modifier) {
  Card(
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(title, style = MaterialTheme.typography.labelMedium, color = MochaBrown)
      Spacer(modifier = Modifier.height(4.dp))
      Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = EspressoBrown)
    }
  }
}

@Composable
private fun AdminProductsView(
  products: List<MenuProduct>,
  onAdd: () -> Unit,
  onEdit: (MenuProduct) -> Unit,
  onToggleAvailable: (MenuProduct) -> Unit,
  onDelete: (MenuProduct) -> Unit,
) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      Button(
        onClick = onAdd,
        colors = ButtonDefaults.buttonColors(containerColor = EspressoBrown, contentColor = CremaWhite),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_add_product_button"),
      ) {
        Icon(Icons.Rounded.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Add Menu Product")
      }
    }
    items(products, key = { it.id }) { prod ->
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(prod.name, style = MaterialTheme.typography.titleMedium)
            Text("${prod.categoryName} • ${BrewPricing.formatInr(prod.basePricePaise)}", style = MaterialTheme.typography.bodySmall, color = MochaBrown)
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = prod.available, onCheckedChange = { onToggleAvailable(prod) })
            IconButton(onClick = { onEdit(prod) }) {
              Icon(Icons.Rounded.Edit, contentDescription = "Edit product")
            }
            IconButton(onClick = { onDelete(prod) }) {
              Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete product", tint = BerryRed)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdminCategoriesView(
  categories: List<MenuCategory>,
  onAdd: () -> Unit,
  onToggleActive: (MenuCategory) -> Unit,
) {
  LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    item {
      Button(
        onClick = onAdd,
        colors = ButtonDefaults.buttonColors(containerColor = EspressoBrown, contentColor = CremaWhite),
        modifier = Modifier.fillMaxWidth().height(48.dp),
      ) {
        Text("Add Category")
      }
    }
    items(categories, key = { it.id }) { cat ->
      Card(
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(cat.name, style = MaterialTheme.typography.titleMedium)
            Text("Order #${cat.displayOrder}", style = MaterialTheme.typography.bodySmall, color = MochaBrown)
          }
          Switch(checked = cat.active, onCheckedChange = { onToggleActive(cat) })
        }
      }
    }
  }
}

@Composable
private fun AdminOrdersView(orders: List<CafeOrder>) {
  var search by remember { mutableStateOf("") }
  val filtered =
    if (search.isBlank()) orders
    else orders.filter { it.orderNumber.contains(search, ignoreCase = true) || it.customerName.contains(search, ignoreCase = true) }

  LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    item {
      OutlinedTextField(
        value = search,
        onValueChange = { search = it },
        label = { Text("Search order # or customer") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
    }
    items(filtered, key = { it.id }) { order ->
      Card(shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${order.orderNumber} • ${order.customerName}", style = MaterialTheme.typography.titleSmall)
            OrderStatusPill(OrderStatus.safeValueOf(order.status))
          }
          Text(
            text = "${BrewPricing.formatInr(order.totalPaise)} • ${order.paymentStatus}",
            style = MaterialTheme.typography.bodySmall,
            color = MochaBrown,
          )
        }
      }
    }
  }
}

@Composable
private fun AdminInventoryView(
  inventory: List<InventoryItem>,
  movements: List<StockMovement>,
  onAdjust: (InventoryItem, Long, String) -> Unit,
) {
  LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    items(inventory, key = { it.id }) { item ->
      val low = item.currentStock <= item.lowStockThreshold
      Card(
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, if (low) BerryRed else MaterialTheme.colorScheme.outlineVariant),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium)
            Text("${item.currentStock} ${item.unit} (alert <= ${item.lowStockThreshold})", style = MaterialTheme.typography.bodySmall, color = if (low) BerryRed else MochaBrown)
          }
          Row {
            IconButton(onClick = { onAdjust(item, -1L, "Owner adjustment") }) {
              Icon(Icons.Rounded.Remove, contentDescription = "Decrease stock")
            }
            IconButton(onClick = { onAdjust(item, 5L, "Restock +5") }) {
              Icon(Icons.Rounded.Add, contentDescription = "Restock +5")
            }
          }
        }
      }
    }
    if (movements.isNotEmpty()) {
      item { Text("Recent Stock Movements", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
      items(movements.take(10), key = { it.id }) { m ->
        Text(
          text = "• ${m.itemName}: ${if (m.delta >= 0) "+${m.delta}" else m.delta} (${m.reason})",
          style = MaterialTheme.typography.bodySmall,
        )
      }
    }
  }
}

@Composable
private fun AdminCouponsView(
  coupons: List<PromoCoupon>,
  onAdd: () -> Unit,
  onToggle: (PromoCoupon) -> Unit,
) {
  LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    item {
      Button(
        onClick = onAdd,
        colors = ButtonDefaults.buttonColors(containerColor = EspressoBrown, contentColor = CremaWhite),
        modifier = Modifier.fillMaxWidth().height(48.dp),
      ) {
        Text("Create Coupon")
      }
    }
    items(coupons, key = { it.id }) { coupon ->
      Card(shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(coupon.code, style = MaterialTheme.typography.titleMedium)
            Text(coupon.description ?: "", style = MaterialTheme.typography.bodySmall, color = MochaBrown)
          }
          Switch(checked = coupon.active, onCheckedChange = { onToggle(coupon) })
        }
      }
    }
  }
}

@Composable
private fun AdminStaffAndLogsView(
  staffList: List<StaffMember>,
  auditLogs: List<AuditLogEntry>,
  onAddStaff: () -> Unit,
  onToggleActive: (StaffMember) -> Unit,
) {
  LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    item {
      Button(
        onClick = onAddStaff,
        colors = ButtonDefaults.buttonColors(containerColor = EspressoBrown, contentColor = CremaWhite),
        modifier = Modifier.fillMaxWidth().height(48.dp),
      ) {
        Text("Add Staff / Barista Account")
      }
    }
    items(staffList, key = { it.uid }) { member ->
      Card(shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("${member.name} (${member.role})", style = MaterialTheme.typography.titleSmall)
            Text(member.email, style = MaterialTheme.typography.bodySmall, color = MochaBrown)
          }
          Switch(checked = member.active, onCheckedChange = { onToggleActive(member) })
        }
      }
    }
    item {
      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
      Text("Audit Logs (${auditLogs.size})", style = MaterialTheme.typography.titleMedium)
    }
    items(auditLogs.take(15), key = { it.id }) { log ->
      Text("• ${log.actorName}: ${log.action} — ${log.details ?: ""}", style = MaterialTheme.typography.bodySmall)
    }
  }
}

@Composable
private fun AdminStoreSettingsView(
  store: StoreLocation,
  onSave: (StoreLocation) -> Unit,
) {
  var name by remember(store.id, store.name) { mutableStateOf(store.name) }
  var address by remember(store.id, store.address) { mutableStateOf(store.address) }
  var phone by remember(store.id, store.phone) { mutableStateOf(store.phone) }
  var hours by remember(store.id, store.openingHours) { mutableStateOf(store.openingHours) }
  var instructions by remember(store.id, store.pickupInstructions) { mutableStateOf(store.pickupInstructions ?: "") }
  var taxBps by remember(store.id, store.taxRateBps) { mutableStateOf(store.taxRateBps.toString()) }
  var open by remember(store.id, store.open) { mutableStateOf(store.open) }
  var pickupEnabled by remember(store.id, store.pickupEnabled) { mutableStateOf(store.pickupEnabled) }

  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text("Store Configuration", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Café Name") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Café Address (Editable)") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Café Phone (Editable)") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = hours, onValueChange = { hours = it }, label = { Text("Opening Hours") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = instructions, onValueChange = { instructions = it }, label = { Text("Pickup Instructions") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = taxBps, onValueChange = { taxBps = it.filter(Char::isDigit) }, label = { Text("Tax Rate (bps, e.g. 500 = 5%)") }, modifier = Modifier.fillMaxWidth())
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("Store Open")
      Switch(checked = open, onCheckedChange = { open = it })
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("Pickup Ordering Enabled")
      Switch(checked = pickupEnabled, onCheckedChange = { pickupEnabled = it })
    }
    Button(
      onClick = {
        onSave(
          store.copy(
            name = name,
            address = address,
            phone = phone,
            openingHours = hours,
            pickupInstructions = instructions,
            taxRateBps = taxBps.toLongOrNull() ?: 500L,
            open = open,
            pickupEnabled = pickupEnabled,
          )
        )
      },
      colors = ButtonDefaults.buttonColors(containerColor = EspressoBrown, contentColor = CremaWhite),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier.fillMaxWidth().height(50.dp).testTag("admin_save_store_button"),
    ) {
      Text("Save Store Settings")
    }
  }
}

@Composable
private fun ProductEditorDialog(
  initial: MenuProduct,
  categories: List<MenuCategory>,
  isNew: Boolean,
  onDismiss: () -> Unit,
  onSave: (MenuProduct) -> Unit,
) {
  var name by remember { mutableStateOf(initial.name) }
  var description by remember { mutableStateOf(initial.description) }
  var priceRupees by remember { mutableStateOf((initial.basePricePaise / 100L).toString()) }
  var selectedCat by remember { mutableStateOf(categories.firstOrNull { it.id == initial.categoryId } ?: categories.firstOrNull()) }
  var bestSeller by remember { mutableStateOf(initial.bestSeller) }
  var seasonal by remember { mutableStateOf(initial.seasonal) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (isNew) "Add Product" else "Edit ${initial.name}") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
        OutlinedTextField(value = priceRupees, onValueChange = { priceRupees = it.filter(Char::isDigit) }, label = { Text("Base Price (₹)") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
          Text("Best Seller")
          Switch(checked = bestSeller, onCheckedChange = { bestSeller = it })
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
          Text("Seasonal")
          Switch(checked = seasonal, onCheckedChange = { seasonal = it })
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val paise = (priceRupees.toLongOrNull() ?: 150L) * 100L
          onSave(
            initial.copy(
              name = name.ifBlank { "Special Brew" },
              description = description.ifBlank { "Freshly crafted at Brew House." },
              basePricePaise = paise,
              categoryId = selectedCat?.id ?: initial.categoryId,
              categoryName = selectedCat?.name ?: initial.categoryName,
              bestSeller = bestSeller,
              seasonal = seasonal,
            )
          )
        }
      ) { Text("Save") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun SimpleCategoryDialog(
  nextOrder: Long,
  onDismiss: () -> Unit,
  onSave: (MenuCategory) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New Category") },
    text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category Name") }) },
    confirmButton = {
      Button(onClick = { if (name.isNotBlank()) onSave(MenuCategory(name = name, displayOrder = nextOrder, active = true)) }) {
        Text("Create")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun SimpleCouponDialog(
  onDismiss: () -> Unit,
  onSave: (PromoCoupon) -> Unit,
) {
  var code by remember { mutableStateOf("") }
  var rupeesOff by remember { mutableStateOf("50") }
  var minRupees by remember { mutableStateOf("200") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New Fixed Coupon") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = code, onValueChange = { code = it.uppercase() }, label = { Text("Coupon Code") })
        OutlinedTextField(value = rupeesOff, onValueChange = { rupeesOff = it.filter(Char::isDigit) }, label = { Text("Discount (₹)") })
        OutlinedTextField(value = minRupees, onValueChange = { minRupees = it.filter(Char::isDigit) }, label = { Text("Min Order (₹)") })
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (code.length >= 2) {
            val dPaise = (rupeesOff.toLongOrNull() ?: 50L) * 100L
            val minPaise = (minRupees.toLongOrNull() ?: 200L) * 100L
            onSave(
              PromoCoupon(
                code = code,
                discountType = "FIXED",
                discountValue = dPaise,
                minOrderPaise = minPaise,
                description = "Flat ₹${dPaise / 100L} off orders ₹${minPaise / 100L}+",
              )
            )
          }
        }
      ) { Text("Create") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun SimpleStaffDialog(
  storeId: String,
  onDismiss: () -> Unit,
  onSave: (StaffMember) -> Unit,
) {
  var uid by remember { mutableStateOf("") }
  var name by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Barista / Staff") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = uid, onValueChange = { uid = it }, label = { Text("Staff Firebase UID") })
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") })
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (uid.isNotBlank() && name.isNotBlank() && email.isNotBlank()) {
            onSave(StaffMember(uid = uid, name = name, email = email, role = "BARISTA", storeId = storeId, active = true))
          }
        }
      ) { Text("Add") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
