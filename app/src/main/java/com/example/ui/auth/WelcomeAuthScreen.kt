package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.GoogleSignInButton
import com.example.ui.theme.BerryRed
import com.example.ui.theme.BerryRedContainer
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.CremaWhite
import com.example.ui.theme.EspressoBrown
import com.example.ui.theme.WarmCream

@Composable
fun WelcomeAuthScreen(
  onAuthSuccess: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var visible by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) { visible = true }

  Box(modifier = modifier.fillMaxSize().background(EspressoBrown)) {
    Image(
      painter = painterResource(R.drawable.img_hero_coffeehouse),
      contentDescription = "Brew House espresso bar",
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors =
                listOf(
                  EspressoBrown.copy(alpha = 0.35f),
                  EspressoBrown.copy(alpha = 0.80f),
                  EspressoBrown.copy(alpha = 0.97f),
                )
            )
          )
    )

    Column(
      modifier =
        Modifier.fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(horizontal = 24.dp, vertical = 20.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Surface(
          shape = RoundedCornerShape(50),
          color = EspressoBrown.copy(alpha = 0.78f),
          border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.6f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.Coffee,
              contentDescription = null,
              tint = CaramelGold,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "ARTISANAL COFFEEHOUSE",
              style = MaterialTheme.typography.labelSmall,
              color = WarmCream,
            )
          }
        }
      }

      AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 6 }),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "BREW HOUSE",
            style = MaterialTheme.typography.displayLarge,
            color = CremaWhite,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Your Coffee. Your Way.",
            style = MaterialTheme.typography.headlineSmall,
            color = CaramelGold,
          )
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text =
              "Crafted single-origin espresso, plant-milk customizations, live barista pickup tracking, and curated rewards in every cup.",
            style = MaterialTheme.typography.bodyLarge,
            color = WarmCream.copy(alpha = 0.88f),
          )
          Spacer(modifier = Modifier.height(20.dp))

          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WelcomeHighlightRow(
              icon = Icons.Rounded.LocalCafe,
              title = "Tailored to Your Taste",
              detail = "Temperature, roast size, oat/almond milk & house syrups",
            )
            WelcomeHighlightRow(
              icon = Icons.Rounded.Timer,
              title = "Express Café Pickup",
              detail = "Skip the queue with live barista preparation updates",
            )
            WelcomeHighlightRow(
              icon = Icons.Rounded.Star,
              title = "Brew House Rewards",
              detail = "Earn loyalty points on every order & unlock free treats",
            )
          }

          Spacer(modifier = Modifier.height(24.dp))

          if (errorMessage != null) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = BerryRedContainer,
              modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            ) {
              Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = BerryRed,
                modifier = Modifier.padding(12.dp),
              )
            }
          }

          GoogleSignInButton(
            onAuthSuccess = {
              errorMessage = null
              onAuthSuccess()
            },
            onAuthError = { msg -> errorMessage = msg },
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Secured with Google Sign-In & Cloud Firestore",
            style = MaterialTheme.typography.bodySmall,
            color = WarmCream.copy(alpha = 0.65f),
            modifier = Modifier.align(Alignment.CenterHorizontally),
          )
        }
      }
    }
  }
}

@Composable
private fun WelcomeHighlightRow(
  icon: ImageVector,
  title: String,
  detail: String,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = Color.White.copy(alpha = 0.12f),
      modifier = Modifier.size(38.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = CaramelGold,
          modifier = Modifier.size(20.dp),
        )
      }
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = CremaWhite,
      )
      Text(
        text = detail,
        style = MaterialTheme.typography.bodySmall,
        color = WarmCream.copy(alpha = 0.78f),
      )
    }
  }
}
