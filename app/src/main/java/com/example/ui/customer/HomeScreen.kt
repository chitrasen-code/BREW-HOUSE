package com.example.ui.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BrewPricing
import com.example.model.LoyaltyTier
import com.example.model.MenuCategory
import com.example.model.MenuProduct
import com.example.model.PromoCoupon
import com.example.model.StoreLocation
import com.example.model.UserProfile
import com.example.ui.components.BrewHouseEmptyState
import com.example.ui.components.BrewHouseLogoBadge
import com.example.ui.components.LoyaltyTierBadge
import com.example.ui.components.ProductCard
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.CremaWhite
import com.example.ui.theme.EspressoBrown
import com.example.ui.theme.MochaBrown
import com.example.ui.theme.WarmCream
import com.example.viewmodel.UiState

@Composable
fun HomeScreen(
  profile: UserProfile?,
  store: StoreLocation,
  categoriesState: UiState<List<MenuCategory>>,
  productsState: UiState<List<MenuProduct>>,
  couponsState: UiState<List<PromoCoupon>>,
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  onOpenMenuWithCategory: (String?) -> Unit,
  onProductClick: (String) -> Unit,
  onApplyCoupon: (String) -> Unit,
  onOpenRewards: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val firstName = profile?.displayName?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Friend"
  val points = profile?.loyaltyPoints ?: 0L
  val tier = LoyaltyTier.fromLifetimePoints(profile?.lifetimePoints ?: 0L)

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    // 1. Header & Greeting
    item {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            BrewHouseLogoBadge()
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "BREW HOUSE",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
              )
              Text(
                text = "Your Coffee. Your Way.",
                style = MaterialTheme.typography.bodySmall,
                color = MochaBrown,
              )
            }
          }
          LoyaltyTierBadge(
            tierLabel = tier.label,
            points = points,
            modifier = Modifier.clickable(onClick = onOpenRewards).testTag("home_loyalty_badge"),
          )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "Good day, $firstName",
          style = MaterialTheme.typography.headlineLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.surfaceContainer,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.LocationOn,
              contentDescription = "Pickup store",
              tint = MochaBrown,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Pickup: ${store.name}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

    // 2. Search Bar
    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Search espresso, caramel latte, croissants...") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search menu") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedBorderColor = EspressoBrown,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
          ),
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("home_search_input"),
      )
    }

    // 3. Featured Seasonal Banner
    item {
      Card(
        shape = MaterialTheme.shapes.large,
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(172.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable { onOpenMenuWithCategory(null) }
            .testTag("home_hero_banner"),
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
          Image(
            painter = painterResource(R.drawable.img_hero_coffeehouse),
            contentDescription = "Featured Caramel Latte",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier =
              Modifier.fillMaxSize()
                .background(
                  Brush.horizontalGradient(
                    listOf(
                      EspressoBrown.copy(alpha = 0.90f),
                      EspressoBrown.copy(alpha = 0.50f),
                      Color.Transparent,
                    )
                  )
                )
          )
          Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Surface(shape = RoundedCornerShape(50), color = CaramelGold) {
              Text(
                text = "SEASONAL ROAST",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = EspressoBrown,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
            Column {
              Text(
                text = "Burnt Caramel & Oat",
                style = MaterialTheme.typography.headlineMedium,
                color = CremaWhite,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Silky double-ristretto crafted fresh for pickup",
                style = MaterialTheme.typography.bodySmall,
                color = WarmCream.copy(alpha = 0.9f),
              )
            }
          }
        }
      }
    }

    // 4. Categories Strip
    item {
      val cats = (categoriesState as? UiState.Success)?.data.orEmpty().filter { it.active }
      if (cats.isNotEmpty()) {
        Column {
          SectionHeader(
            title = "Explore Categories",
            actionLabel = "Full Menu",
            onAction = { onOpenMenuWithCategory(null) },
          )
          LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(cats, key = { it.id }) { cat ->
              FilterChip(
                selected = false,
                onClick = { onOpenMenuWithCategory(cat.id) },
                label = { Text(cat.name) },
                colors =
                  FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                  ),
                modifier = Modifier.testTag("home_category_${cat.id}"),
              )
            }
          }
        }
      }
    }

    // 5. Products (Search Results or Best Sellers + Seasonal)
    when (productsState) {
      UiState.Loading -> {
        item {
          Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EspressoBrown)
          }
        }
      }
      is UiState.Error -> {
        item {
          BrewHouseEmptyState(
            icon = Icons.Rounded.Search,
            title = "Menu Unavailable",
            subtitle = productsState.message,
          )
        }
      }
      is UiState.Success -> {
        val allProducts = productsState.data
        val filtered =
          if (searchQuery.isBlank()) allProducts
          else allProducts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
              it.categoryName.contains(searchQuery, ignoreCase = true) ||
              it.description.contains(searchQuery, ignoreCase = true)
          }

        if (allProducts.isEmpty()) {
          item {
            BrewHouseEmptyState(
              icon = Icons.Rounded.LocationOn,
              title = "Menu Awaiting Café Setup",
              subtitle = "Sign in with the café owner account to initialize the Brew House menu.",
            )
          }
        } else if (searchQuery.isNotBlank()) {
          item {
            SectionHeader(title = "Search Results (${filtered.size})", actionLabel = null, onAction = {})
          }
          if (filtered.isEmpty()) {
            item {
              BrewHouseEmptyState(
                icon = Icons.Rounded.Search,
                title = "No matching drinks",
                subtitle = "Try searching for Espresso, Latte, Cold Coffee, or Croissant.",
              )
            }
          } else {
            items(filtered.chunked(2)) { rowItems ->
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                rowItems.forEach { prod ->
                  ProductCard(
                    product = prod,
                    onClick = { onProductClick(prod.id) },
                    modifier = Modifier.weight(1f),
                  )
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        } else {
          val bestSellers = allProducts.filter { it.bestSeller && it.available }
          if (bestSellers.isNotEmpty()) {
            item {
              Column {
                SectionHeader(
                  title = "Best-Selling Drinks",
                  actionLabel = "See All",
                  onAction = { onOpenMenuWithCategory(null) },
                )
                LazyRow(
                  contentPadding = PaddingValues(horizontal = 16.dp),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  items(bestSellers, key = { it.id }) { prod ->
                    ProductCard(
                      product = prod,
                      onClick = { onProductClick(prod.id) },
                      modifier = Modifier.width(188.dp),
                    )
                  }
                }
              }
            }
          }

          val activeCoupons = (couponsState as? UiState.Success)?.data.orEmpty().filter { it.active }
          if (activeCoupons.isNotEmpty()) {
            item {
              Column {
                SectionHeader(title = "Exclusive Café Offers", actionLabel = null, onAction = {})
                LazyRow(
                  contentPadding = PaddingValues(horizontal = 16.dp),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  items(activeCoupons, key = { it.id }) { coupon ->
                    CouponHighlightCard(coupon = coupon, onApply = { onApplyCoupon(coupon.code) })
                  }
                }
              }
            }
          }

          val seasonal = allProducts.filter { it.seasonal && it.available }
          if (seasonal.isNotEmpty()) {
            item {
              Column {
                SectionHeader(
                  title = "Seasonal Specials",
                  actionLabel = "Explore",
                  onAction = { onOpenMenuWithCategory(null) },
                )
                LazyRow(
                  contentPadding = PaddingValues(horizontal = 16.dp),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  items(seasonal, key = { it.id }) { prod ->
                    ProductCard(
                      product = prod,
                      onClick = { onProductClick(prod.id) },
                      modifier = Modifier.width(188.dp),
                    )
                  }
                }
              }
            }
          }

          val recommended = allProducts.filter { !it.bestSeller && it.available }.take(4)
          if (recommended.isNotEmpty()) {
            item {
              SectionHeader(title = "Barista Recommendations", actionLabel = null, onAction = {})
            }
            items(recommended.chunked(2)) { rowItems ->
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                rowItems.forEach { prod ->
                  ProductCard(
                    product = prod,
                    onClick = { onProductClick(prod.id) },
                    modifier = Modifier.weight(1f),
                  )
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SectionHeader(
  title: String,
  actionLabel: String?,
  onAction: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.onBackground,
    )
    if (actionLabel != null) {
      TextButton(onClick = onAction) {
        Text(
          text = actionLabel,
          style = MaterialTheme.typography.labelLarge,
          color = MochaBrown,
        )
      }
    }
  }
}

@Composable
private fun CouponHighlightCard(
  coupon: PromoCoupon,
  onApply: () -> Unit,
) {
  val discountBadge =
    if (coupon.discountType == "PERCENT") "${coupon.discountValue}% OFF"
    else "${BrewPricing.formatInr(coupon.discountValue)} OFF"

  Card(
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = EspressoBrown),
    modifier = Modifier.width(236.dp).clickable(onClick = onApply).testTag("coupon_card_${coupon.code}"),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Rounded.LocalOffer,
          contentDescription = null,
          tint = CaramelGold,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = discountBadge,
          style = MaterialTheme.typography.labelLarge,
          color = CaramelGold,
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = coupon.code,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = CremaWhite,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = coupon.description ?: "Min order ${BrewPricing.formatInr(coupon.minOrderPaise)}",
        style = MaterialTheme.typography.bodySmall,
        color = WarmCream.copy(alpha = 0.8f),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
