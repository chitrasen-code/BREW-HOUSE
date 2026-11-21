package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BrewHouseSeedData
import com.example.model.BrewPricing
import com.example.model.MenuProduct
import com.example.model.OrderStatus
import com.example.model.PaymentStatus
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberGoldContainer
import com.example.ui.theme.BerryRed
import com.example.ui.theme.BerryRedContainer
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.CremaWhite
import com.example.ui.theme.EspressoBrown
import com.example.ui.theme.MatchaGreen
import com.example.ui.theme.MatchaGreenContainer
import com.example.ui.theme.MochaBrown
import com.example.ui.theme.WarmCream

@Composable
fun BrewHouseLogoBadge(modifier: Modifier = Modifier) {
  Surface(
    shape = CircleShape,
    color = EspressoBrown,
    border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.6f)),
    modifier = modifier.size(40.dp),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = Icons.Rounded.Coffee,
        contentDescription = "Brew House crest",
        tint = CaramelGold,
        modifier = Modifier.size(22.dp),
      )
    }
  }
}

@Composable
fun ProductCard(
  product: MenuProduct,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier =
      modifier
        .clip(MaterialTheme.shapes.medium)
        .clickable(enabled = product.available, onClick = onClick)
        .testTag("product_card_${product.id}"),
  ) {
    Column {
      Box(modifier = Modifier.fillMaxWidth().height(132.dp)) {
        Image(
          painter = painterResource(BrewHouseSeedData.drawableResForImageKey(product.imageKey)),
          contentDescription = product.name,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(
                Brush.verticalGradient(
                  listOf(Color.Transparent, EspressoBrown.copy(alpha = 0.35f))
                )
              )
        )
        Row(
          modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          if (product.bestSeller) {
            BadgePill(text = "Best Seller", container = CaramelGold, content = EspressoBrown)
          }
          if (product.seasonal) {
            BadgePill(text = "Seasonal", container = EspressoBrown, content = WarmCream)
          }
          if (!product.available) {
            BadgePill(text = "Sold Out", container = BerryRedContainer, content = BerryRed)
          }
        }
      }
      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = product.categoryName.uppercase(),
          style = MaterialTheme.typography.labelSmall,
          color = MochaBrown,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = product.name,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = product.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = BrewPricing.formatInr(product.basePricePaise),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
          )
          Surface(
            shape = CircleShape,
            color = if (product.available) EspressoBrown else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(34.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Customize ${product.name}",
                tint = CremaWhite,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun BadgePill(
  text: String,
  container: Color,
  content: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(50),
    color = container,
    modifier = modifier,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
      color = content,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
    )
  }
}

@Composable
fun OrderStatusPill(status: OrderStatus, modifier: Modifier = Modifier) {
  val (bg, fg) =
    when (status) {
      OrderStatus.PENDING -> AmberGoldContainer to AmberGold
      OrderStatus.CONFIRMED -> WarmCream to MochaBrown
      OrderStatus.PREPARING -> CaramelGold.copy(alpha = 0.35f) to EspressoBrown
      OrderStatus.READY_FOR_PICKUP -> MatchaGreenContainer to MatchaGreen
      OrderStatus.COMPLETED -> MatchaGreen to CremaWhite
      OrderStatus.CANCELLED -> BerryRedContainer to BerryRed
    }
  BadgePill(text = status.label, container = bg, content = fg, modifier = modifier)
}

@Composable
fun PaymentStatusPill(paymentStatus: PaymentStatus, modifier: Modifier = Modifier) {
  val (bg, fg) =
    when (paymentStatus) {
      PaymentStatus.PAID -> MatchaGreenContainer to MatchaGreen
      PaymentStatus.UNPAID -> AmberGoldContainer to AmberGold
      PaymentStatus.FAILED -> BerryRedContainer to BerryRed
      PaymentStatus.REFUNDED -> WarmCream to MochaBrown
    }
  BadgePill(text = paymentStatus.label, container = bg, content = fg, modifier = modifier)
}

@Composable
fun FloatingCartBar(
  itemCount: Int,
  subtotalPaise: Long,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (itemCount <= 0) return
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = EspressoBrown,
    shadowElevation = 8.dp,
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .clip(RoundedCornerShape(18.dp))
        .clickable(onClick = onClick)
        .testTag("floating_cart_bar"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = CaramelGold) {
          Icon(
            imageVector = Icons.Rounded.ShoppingBag,
            contentDescription = null,
            tint = EspressoBrown,
            modifier = Modifier.padding(8.dp).size(18.dp),
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "$itemCount ${if (itemCount == 1) "item" else "items"} in bag",
            style = MaterialTheme.typography.titleSmall,
            color = CremaWhite,
          )
          Text(
            text = "Tap to review & checkout",
            style = MaterialTheme.typography.bodySmall,
            color = CaramelGold,
          )
        }
      }
      Text(
        text = BrewPricing.formatInr(subtotalPaise),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = CaramelGold,
      )
    }
  }
}

@Composable
fun BrewHouseEmptyState(
  icon: ImageVector,
  title: String,
  subtitle: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Surface(
      shape = CircleShape,
      color = MaterialTheme.colorScheme.surfaceContainer,
      modifier = Modifier.size(72.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MochaBrown,
          modifier = Modifier.size(34.dp),
        )
      }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.onBackground,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    if (actionLabel != null && onAction != null) {
      Spacer(modifier = Modifier.height(18.dp))
      Button(
        onClick = onAction,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = EspressoBrown, contentColor = CremaWhite),
        modifier = Modifier.height(48.dp).testTag("empty_state_action_button"),
      ) {
        Text(text = actionLabel)
      }
    }
  }
}

@Composable
fun LoyaltyTierBadge(tierLabel: String, points: Long, modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(50),
    color = EspressoBrown,
    border = BorderStroke(1.dp, CaramelGold),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Rounded.Star,
        contentDescription = null,
        tint = CaramelGold,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "$tierLabel • $points pts",
        style = MaterialTheme.typography.labelMedium,
        color = WarmCream,
      )
    }
  }
}
