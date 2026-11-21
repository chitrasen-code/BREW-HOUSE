package com.example.ui.customer

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.BrewPricing
import com.example.model.LoyaltyTier
import com.example.model.LoyaltyTransaction
import com.example.model.RewardOffer
import com.example.model.UserPermissions
import com.example.model.UserProfile
import com.example.model.WorkspaceMode
import com.example.ui.components.BadgePill
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.CremaWhite
import com.example.ui.theme.EspressoBrown
import com.example.ui.theme.MatchaGreen
import com.example.ui.theme.MochaBrown
import com.example.ui.theme.WarmCream
import com.example.viewmodel.UiState

@Composable
fun RewardsScreen(
  profile: UserProfile?,
  transactionsState: UiState<List<LoyaltyTransaction>>,
  isSubmitting: Boolean,
  onRedeemReward: (RewardOffer) -> Unit,
  modifier: Modifier = Modifier,
) {
  val points = profile?.loyaltyPoints ?: 0L
  val lifetime = profile?.lifetimePoints ?: 0L
  val tier = LoyaltyTier.fromLifetimePoints(lifetime)
  val nextThreshold = tier.nextThreshold
  val progress =
    if (nextThreshold == null) 1f
    else ((lifetime - tier.minLifetimePoints).toFloat() / (nextThreshold - tier.minLifetimePoints).toFloat()).coerceIn(0f, 1f)

  val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Text("Brew House Rewards", style = MaterialTheme.typography.headlineLarge)
    }

    item {
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
            BadgePill(text = "${tier.label.uppercase()} MEMBER", container = CaramelGold, content = EspressoBrown)
            Icon(Icons.Rounded.Star, contentDescription = null, tint = CaramelGold)
          }
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = "$points pts",
            style = MaterialTheme.typography.displayMedium,
            color = CremaWhite,
          )
          Text(
            text = "Available to redeem • $lifetime lifetime points",
            style = MaterialTheme.typography.bodySmall,
            color = WarmCream.copy(alpha = 0.8f),
          )
          Spacer(modifier = Modifier.height(14.dp))
          LinearProgressIndicator(
            progress = { progress },
            color = CaramelGold,
            trackColor = WarmCream.copy(alpha = 0.25f),
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text =
              if (nextThreshold != null) "${(nextThreshold - lifetime).coerceAtLeast(0L)} pts to next tier"
              else "Highest Platinum Tier unlocked!",
            style = MaterialTheme.typography.labelSmall,
            color = CaramelGold,
          )
        }
      }
    }

    item {
      Text("Rewards Catalogue", style = MaterialTheme.typography.headlineSmall)
    }

    items(BrewPricing.rewardCatalogue, key = { it.id }) { offer ->
      val canRedeem = points >= offer.pointsCost
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = MochaBrown, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              BadgePill(text = offer.badge, container = WarmCream, content = MochaBrown)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(offer.title, style = MaterialTheme.typography.titleMedium)
            Text(offer.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Text("${offer.pointsCost} pts", style = MaterialTheme.typography.labelLarge, color = MochaBrown)
          }
          Spacer(modifier = Modifier.width(12.dp))
          Button(
            onClick = { onRedeemReward(offer) },
            enabled = canRedeem && !isSubmitting,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EspressoBrown, contentColor = CremaWhite),
            modifier = Modifier.height(48.dp).testTag("redeem_${offer.id}"),
          ) {
            Text(if (canRedeem) "Redeem" else "Locked")
          }
        }
      }
    }

    item {
      Text("Points Ledger", style = MaterialTheme.typography.headlineSmall)
    }

    if (transactions.isEmpty()) {
      item {
        Text(
          text = "Earn 1 point for every ₹10 spent on pickup orders.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      items(transactions, key = { it.id }) { tx ->
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(tx.description, style = MaterialTheme.typography.bodyMedium)
              Text(tx.type, style = MaterialTheme.typography.labelSmall, color = MochaBrown)
            }
            Text(
              text = if (tx.pointsDelta >= 0) "+${tx.pointsDelta} pts" else "${tx.pointsDelta} pts",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = if (tx.pointsDelta >= 0) MatchaGreen else MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    }
  }
}

@Composable
fun ProfileScreen(
  profile: UserProfile?,
  userEmail: String?,
  permissions: UserPermissions,
  currentMode: WorkspaceMode,
  isSubmitting: Boolean,
  onSelectWorkspaceMode: (WorkspaceMode) -> Unit,
  onSaveProfile: (displayName: String, phone: String, preferredMilk: String, notificationsEnabled: Boolean) -> Unit,
  onOpenOrders: () -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var name by remember(profile?.displayName) { mutableStateOf(profile?.displayName ?: "") }
  var phone by remember(profile?.phone) { mutableStateOf(profile?.phone ?: "") }
  var preferredMilk by remember(profile?.preferredMilk) { mutableStateOf(profile?.preferredMilk ?: "Whole Milk") }
  var notificationsEnabled by remember(profile?.notificationsEnabled) { mutableStateOf(profile?.notificationsEnabled ?: true) }

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text("Profile & Preferences", style = MaterialTheme.typography.headlineLarge)

    Card(
      shape = MaterialTheme.shapes.medium,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
      Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(14.dp), color = EspressoBrown, modifier = Modifier.size(52.dp)) {
          Icon(
            imageVector = Icons.Rounded.Person,
            contentDescription = null,
            tint = CaramelGold,
            modifier = Modifier.padding(12.dp),
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(profile?.displayName ?: "Brew House Guest", style = MaterialTheme.typography.titleLarge)
          Text(userEmail ?: profile?.email ?: "", style = MaterialTheme.typography.bodySmall, color = MochaBrown)
        }
      }
    }

    // Role / Workspace Switcher for authorized Staff & Owner
    if (permissions.availableModes.size > 1) {
      Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = EspressoBrown),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Workspace Mode", style = MaterialTheme.typography.titleMedium, color = CremaWhite)
          Text(
            text = "Switch between Customer ordering, Barista queue, and Owner Admin.",
            style = MaterialTheme.typography.bodySmall,
            color = WarmCream.copy(alpha = 0.8f),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            permissions.availableModes.forEach { mode ->
              FilterChip(
                selected = currentMode == mode,
                onClick = { onSelectWorkspaceMode(mode) },
                label = { Text(mode.label) },
                modifier = Modifier.testTag("switch_mode_${mode.name.lowercase()}"),
              )
            }
          }
        }
      }
    }

    OutlinedTextField(
      value = name,
      onValueChange = { name = it },
      label = { Text("Display Name") },
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
    )

    OutlinedTextField(
      value = phone,
      onValueChange = { phone = it },
      label = { Text("Phone Number") },
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth().testTag("profile_phone_input"),
    )

    Text("Saved Milk Preference", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      listOf("Whole Milk", "Oat Milk", "Almond Milk").forEach { milk ->
        FilterChip(
          selected = preferredMilk == milk,
          onClick = { preferredMilk = milk },
          label = { Text(milk) },
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("Order Status Notifications", style = MaterialTheme.typography.titleSmall)
        Text("Live barista prep & pickup updates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
    }

    Button(
      onClick = { onSaveProfile(name, phone, preferredMilk, notificationsEnabled) },
      enabled = !isSubmitting,
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.buttonColors(containerColor = EspressoBrown, contentColor = CremaWhite),
      modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_profile_button"),
    ) {
      Text("Save Preferences")
    }

    OutlinedButton(
      onClick = onOpenOrders,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier.fillMaxWidth().height(48.dp),
    ) {
      Text("View Order History")
    }

    HorizontalDivider()

    OutlinedButton(
      onClick = onSignOut,
      colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button"),
    ) {
      Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text("Sign Out")
    }

    Spacer(modifier = Modifier.height(88.dp))
  }
}
