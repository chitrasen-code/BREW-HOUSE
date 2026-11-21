package com.example.ui.customer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BrewHouseSeedData
import com.example.model.BrewPricing
import com.example.model.CartItem
import com.example.model.MenuProduct
import com.example.ui.components.BadgePill
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.CremaWhite
import com.example.ui.theme.EspressoBrown
import com.example.ui.theme.MochaBrown
import com.example.ui.theme.WarmCream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
  product: MenuProduct,
  editingCartItem: CartItem?,
  defaultMilk: String?,
  isSubmitting: Boolean,
  onBack: () -> Unit,
  onSaveToCart: (
    quantity: Int,
    temperature: String,
    size: String,
    milkOption: String,
    sugarLevel: String,
    addons: List<String>,
  ) -> Unit,
) {
  BackHandler(onBack = onBack)

  val isBeverage = product.allowHot || product.allowIced
  var temperature by remember(product.id, editingCartItem?.id) {
    mutableStateOf(
      editingCartItem?.temperature
        ?: if (product.allowHot) "HOT" else if (product.allowIced) "ICED" else "STANDARD"
    )
  }
  var size by remember(product.id, editingCartItem?.id) {
    mutableStateOf(editingCartItem?.size ?: "MEDIUM")
  }
  var milkOption by remember(product.id, editingCartItem?.id) {
    mutableStateOf(
      editingCartItem?.milkOption
        ?: if (product.allowMilkOptions) (defaultMilk ?: "Whole Milk") else ""
    )
  }
  var sugarLevel by remember(product.id, editingCartItem?.id) {
    mutableStateOf(
      editingCartItem?.sugarLevel
        ?: if (product.allowSugarOptions) "Regular sugar" else ""
    )
  }
  var selectedAddons by remember(product.id, editingCartItem?.id) {
    mutableStateOf(editingCartItem?.addons?.toSet() ?: emptySet())
  }
  var quantity by remember(product.id, editingCartItem?.id) {
    mutableIntStateOf(editingCartItem?.quantity?.toInt() ?: 1)
  }

  val unitPricePaise =
    BrewPricing.calculateCustomizedUnitPricePaise(
      product = product,
      size = size,
      milkOption = milkOption,
      addons = selectedAddons.toList(),
    )
  val totalPricePaise = unitPricePaise * quantity

  Scaffold(
    contentWindowInsets = WindowInsets(0),
    bottomBar = {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .navigationBarsPadding()
              .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = { if (quantity > 1) quantity-- },
                modifier = Modifier.size(48.dp).testTag("detail_qty_minus"),
              ) {
                Icon(Icons.Rounded.Remove, contentDescription = "Decrease quantity")
              }
              Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 6.dp),
              )
              IconButton(
                onClick = { if (quantity < 20) quantity++ },
                modifier = Modifier.size(48.dp).testTag("detail_qty_plus"),
              ) {
                Icon(Icons.Rounded.Add, contentDescription = "Increase quantity")
              }
            }
          }

          Button(
            onClick = {
              onSaveToCart(
                quantity,
                temperature,
                size,
                milkOption,
                sugarLevel,
                selectedAddons.toList(),
              )
            },
            enabled = product.available && !isSubmitting,
            shape = RoundedCornerShape(16.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = EspressoBrown,
                contentColor = CremaWhite,
              ),
            modifier = Modifier.weight(1f).height(52.dp).testTag("add_to_cart_button"),
          ) {
            if (isSubmitting) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = CremaWhite,
                strokeWidth = 2.dp,
              )
            } else {
              Text(
                text =
                  if (editingCartItem != null)
                    "Update Bag • ${BrewPricing.formatInr(totalPricePaise)}"
                  else
                    "Add to Bag • ${BrewPricing.formatInr(totalPricePaise)}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
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
          .verticalScroll(rememberScrollState()),
    ) {
      Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
        Image(
          painter = painterResource(BrewHouseSeedData.drawableResForImageKey(product.imageKey)),
          contentDescription = product.name,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .statusBarsPadding()
              .padding(14.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = EspressoBrown.copy(alpha = 0.78f),
            modifier = Modifier.size(48.dp),
          ) {
            IconButton(
              onClick = onBack,
              modifier = Modifier.fillMaxSize().testTag("detail_back_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = CremaWhite,
              )
            }
          }
        }
      }

      Column(modifier = Modifier.padding(20.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          BadgePill(
            text = product.categoryName,
            container = WarmCream,
            content = MochaBrown,
          )
          if (product.bestSeller) {
            BadgePill(text = "Best Seller", container = CaramelGold, content = EspressoBrown)
          }
          if (product.seasonal) {
            BadgePill(text = "Seasonal", container = EspressoBrown, content = WarmCream)
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top,
        ) {
          Text(
            text = product.name,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = BrewPricing.formatInr(unitPricePaise),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MochaBrown,
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = product.description,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!product.ingredients.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(12.dp))
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "INGREDIENTS",
                style = MaterialTheme.typography.labelSmall,
                color = MochaBrown,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = product.ingredients,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }

        if (isBeverage) {
          Spacer(modifier = Modifier.height(20.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          Spacer(modifier = Modifier.height(16.dp))

          // Temperature
          if (product.allowHot || product.allowIced) {
            CustomizationTitle("Temperature")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              if (product.allowHot) {
                FilterChip(
                  selected = temperature == "HOT",
                  onClick = { temperature = "HOT" },
                  label = { Text("Hot") },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = EspressoBrown,
                      selectedLabelColor = CremaWhite,
                    ),
                  modifier = Modifier.testTag("option_temp_hot"),
                )
              }
              if (product.allowIced) {
                FilterChip(
                  selected = temperature == "ICED",
                  onClick = { temperature = "ICED" },
                  label = { Text("Iced") },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = EspressoBrown,
                      selectedLabelColor = CremaWhite,
                    ),
                  modifier = Modifier.testTag("option_temp_iced"),
                )
              }
            }
            Spacer(modifier = Modifier.height(14.dp))
          }

          // Size
          CustomizationTitle("Cup Size")
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BrewPricing.sizes.forEach { s ->
              val upcharge = BrewPricing.sizeUpchargePaise(s, isBeverage = true)
              val label =
                if (upcharge == 0L) BrewPricing.sizeDisplayName(s)
                else "${BrewPricing.sizeDisplayName(s)} (+${BrewPricing.formatInr(upcharge)})"
              FilterChip(
                selected = size == s,
                onClick = { size = s },
                label = { Text(label) },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EspressoBrown,
                    selectedLabelColor = CremaWhite,
                  ),
                modifier = Modifier.testTag("option_size_${s.lowercase()}"),
              )
            }
          }
          Spacer(modifier = Modifier.height(14.dp))

          // Milk options
          if (product.allowMilkOptions) {
            CustomizationTitle("Milk Choice")
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              BrewPricing.milkOptions.forEach { milk ->
                val upcharge = BrewPricing.milkUpchargePaise(milk, true)
                val label = if (upcharge == 0L) milk else "$milk (+${BrewPricing.formatInr(upcharge)})"
                FilterChip(
                  selected = milkOption == milk,
                  onClick = { milkOption = milk },
                  label = { Text(label) },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = EspressoBrown,
                      selectedLabelColor = CremaWhite,
                    ),
                )
              }
            }
            Spacer(modifier = Modifier.height(14.dp))
          }

          // Sugar
          if (product.allowSugarOptions) {
            CustomizationTitle("Sweetness")
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              BrewPricing.sugarLevels.forEach { level ->
                FilterChip(
                  selected = sugarLevel == level,
                  onClick = { sugarLevel = level },
                  label = { Text(level) },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = EspressoBrown,
                      selectedLabelColor = CremaWhite,
                    ),
                )
              }
            }
            Spacer(modifier = Modifier.height(14.dp))
          }

          // Additional Add-ons
          if (product.allowAddons) {
            CustomizationTitle("Signature Add-ons")
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              BrewPricing.availableAddons.forEach { addon ->
                val selected = addon in selectedAddons
                val upcharge = BrewPricing.addonUpchargePaise(addon)
                FilterChip(
                  selected = selected,
                  onClick = {
                    selectedAddons =
                      if (selected) selectedAddons - addon else selectedAddons + addon
                  },
                  label = { Text("$addon (+${BrewPricing.formatInr(upcharge)})") },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = CaramelGold,
                      selectedLabelColor = EspressoBrown,
                    ),
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
private fun CustomizationTitle(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onBackground,
    modifier = Modifier.padding(bottom = 8.dp),
  )
}
