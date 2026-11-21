package com.example.ui.customer

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.MenuCategory
import com.example.model.MenuProduct
import com.example.ui.components.BrewHouseEmptyState
import com.example.ui.components.ProductCard
import com.example.ui.theme.EspressoBrown
import com.example.viewmodel.MenuSortOption
import com.example.viewmodel.UiState

@Composable
fun MenuScreen(
  categoriesState: UiState<List<MenuCategory>>,
  productsState: UiState<List<MenuProduct>>,
  filteredProducts: List<MenuProduct>,
  searchQuery: String,
  selectedCategoryId: String?,
  onlyBestSellers: Boolean,
  onlySeasonal: Boolean,
  sortOption: MenuSortOption,
  onSearchQueryChange: (String) -> Unit,
  onSelectCategory: (String?) -> Unit,
  onToggleBestSellers: () -> Unit,
  onToggleSeasonal: () -> Unit,
  onSelectSort: (MenuSortOption) -> Unit,
  onProductClick: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var sortExpanded by remember { mutableStateOf(false) }
  val categories = (categoriesState as? UiState.Success)?.data.orEmpty().filter { it.active }

  Column(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
      Text(
        text = "Our Coffee Menu",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Spacer(modifier = Modifier.height(10.dp))
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Search drinks, teas, bakery...") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search menu") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
        modifier = Modifier.fillMaxWidth().testTag("menu_search_input"),
      )
    }

    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      item {
        FilterChip(
          selected = selectedCategoryId == null,
          onClick = { onSelectCategory(null) },
          label = { Text("All") },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = EspressoBrown,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            ),
          modifier = Modifier.testTag("menu_cat_all"),
        )
      }
      items(categories, key = { it.id }) { cat ->
        FilterChip(
          selected = selectedCategoryId == cat.id,
          onClick = { onSelectCategory(if (selectedCategoryId == cat.id) null else cat.id) },
          label = { Text(cat.name) },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = EspressoBrown,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            ),
          modifier = Modifier.testTag("menu_cat_${cat.id}"),
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = onlyBestSellers,
          onClick = onToggleBestSellers,
          label = { Text("Best Sellers") },
          modifier = Modifier.testTag("menu_filter_bestsellers"),
        )
        FilterChip(
          selected = onlySeasonal,
          onClick = onToggleSeasonal,
          label = { Text("Seasonal") },
          modifier = Modifier.testTag("menu_filter_seasonal"),
        )
      }

      Box {
        OutlinedButton(
          onClick = { sortExpanded = true },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.height(40.dp).testTag("menu_sort_button"),
        ) {
          Icon(Icons.Rounded.SwapVert, contentDescription = "Sort menu")
          Text(
            text = sortOption.label.substringBefore(":"),
            style = MaterialTheme.typography.labelMedium,
          )
        }
        DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
          MenuSortOption.entries.forEach { option ->
            DropdownMenuItem(
              text = { Text(option.label) },
              onClick = {
                onSelectSort(option)
                sortExpanded = false
              },
            )
          }
        }
      }
    }

    when (productsState) {
      UiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = EspressoBrown)
        }
      }
      is UiState.Error -> {
        BrewHouseEmptyState(
          icon = Icons.Rounded.Coffee,
          title = "Could Not Load Menu",
          subtitle = productsState.message,
        )
      }
      is UiState.Success -> {
        if (filteredProducts.isEmpty()) {
          BrewHouseEmptyState(
            icon = Icons.Rounded.Coffee,
            title = "No Items Match Filters",
            subtitle = "Clear your filters or search query to explore all Brew House handcrafted items.",
            actionLabel = "Reset Filters",
            onAction = {
              onSearchQueryChange("")
              onSelectCategory(null)
              if (onlyBestSellers) onToggleBestSellers()
              if (onlySeasonal) onToggleSeasonal()
            },
          )
        } else {
          LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(filteredProducts.chunked(2)) { rowItems ->
              Row(
                modifier = Modifier.fillMaxWidth(),
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
