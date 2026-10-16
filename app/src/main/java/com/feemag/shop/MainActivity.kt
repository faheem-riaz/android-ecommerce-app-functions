package com.feemag.shop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Currency

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContent {
   MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF146A56), background = Color(0xFFF7F7F0), surface = Color(0xFFFEFEFA))) {
    Shop((application as ShopApplication).repository)
   }
  }
 }
}
private fun money(minor: Long): String = NumberFormat.getCurrencyInstance().apply {
 currency = Currency.getInstance("EUR")
}.format(java.math.BigDecimal.valueOf(minor, 2))

@Composable
private fun Shop(repository: ShopRepository) {
 var tab by rememberSaveable { mutableIntStateOf(0) }
 var query by rememberSaveable { mutableStateOf("") }
 var category by rememberSaveable { mutableStateOf<String?>(null) }
 var stockOnly by rememberSaveable { mutableStateOf(false) }
 var sort by rememberSaveable { mutableStateOf("name") }
 var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
 var error by remember { mutableStateOf<String?>(null) }
 var busy by remember { mutableStateOf(false) }
 val cart by repository.cart.collectAsStateWithLifecycle()
 val scope = rememberCoroutineScope()
 fun setQuantity(id: String, quantity: Int) {
  if (busy) return
  busy = true
  scope.launch {
   try { withContext(Dispatchers.IO) { repository.setQuantity(id, quantity) } }
   catch (e: IllegalArgumentException) { error = e.message }
   catch (e: IllegalStateException) { error = e.message }
   finally { busy = false }
  }
 }
 val products = remember(query, category, stockOnly, sort) {
  repository.products(query.takeIf { it.isNotBlank() }, category, inStockOnly = stockOnly, sortBy = sort, limit = 50).products
 }
 Scaffold(bottomBar = {
  NavigationBar {
   listOf("Discover", "Cart (${cart.itemCount})", "Functions").forEachIndexed { index, title ->
    NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Text(listOf("◈", "▣", "ƒ")[index]) }, label = { Text(title) })
   }
  }
 }) { padding ->
  LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
   item {
    Text("FEEMAG / SHOP", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(8.dp))
    Text(when(tab) { 0 -> "Everyday, thoughtfully chosen."; 1 -> "Your shopping bag"; else -> "Made for agents, too." }, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
    Text("Offline demo · EUR prices · No real purchases", style = MaterialTheme.typography.bodySmall)
   }
   when(tab) {
    0 -> {
     item {
      OutlinedTextField(query, { query = it.take(200) }, label = { Text("Search products") }, singleLine = true, modifier = Modifier.fillMaxWidth())
      Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
       FilterChip(category == null, { category = null }, label = { Text("All") })
       Catalog.categories.forEach { c -> FilterChip(category == c.id, { category = c.id }, label = { Text(c.name) }) }
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
       Checkbox(stockOnly, { stockOnly = it }); Text("In stock", modifier = Modifier.weight(1f))
       TextButton(onClick = { sort = when(sort) { "name" -> "price_asc"; "price_asc" -> "price_desc"; else -> "name" } }) {
        Text(when(sort) { "price_asc" -> "Price ↑"; "price_desc" -> "Price ↓"; else -> "Name ↕" })
       }
      }
      Text("${products.size} ${if (products.size == 1) "product" else "products"}", style = MaterialTheme.typography.labelMedium)
     }
     if (products.isEmpty()) item { Text("No products found. Try another keyword or category.") }
     items(products, key = { it.id }) { product ->
      Card(Modifier.fillMaxWidth().clickable { selectedId = product.id }, shape = RoundedCornerShape(20.dp)) {
       Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(when(product.categoryId) { "audio" -> "♫"; "workspace" -> "▤"; else -> "◇" }, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
         Text(product.name, fontWeight = FontWeight.SemiBold)
         Text(product.categoryId.uppercase(), style = MaterialTheme.typography.labelSmall)
         Text(money(product.priceMinor), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
         if (product.stock == 0) Text("Out of stock", style = MaterialTheme.typography.labelSmall)
        }
        Text("→")
       }
      }
     }
    }
    1 -> {
     if (cart.lines.isEmpty()) item { Text("Your bag is empty. Discover something useful."); Button(onClick = { tab = 0 }) { Text("Explore products") } }
     items(cart.lines, key = { it.product.id }) { line ->
      Card(Modifier.fillMaxWidth()) {
       Column(Modifier.padding(16.dp)) {
        Text(line.product.name, fontWeight = FontWeight.SemiBold); Text(money(line.lineTotalMinor))
        Row(verticalAlignment = Alignment.CenterVertically) {
         OutlinedButton(enabled = !busy, onClick = { setQuantity(line.product.id, line.quantity - 1) }) { Text("−") }
         Text("${line.quantity}", Modifier.padding(18.dp))
         OutlinedButton(enabled = !busy && line.quantity < minOf(line.product.stock, 99), onClick = { setQuantity(line.product.id, line.quantity + 1) }) { Text("+") }
         TextButton(enabled = !busy, onClick = { setQuantity(line.product.id, 0) }) { Text("Remove") }
        }
       }
      }
     }
     if (cart.lines.isNotEmpty()) item {
      HorizontalDivider(); Spacer(Modifier.height(16.dp))
      Text("Subtotal  ${money(cart.subtotalMinor)}", style = MaterialTheme.typography.headlineSmall)
      Text("Demo cart only. Shipping, tax calculation and checkout are not implemented. Items are not reserved.")
     }
    }
    else -> {
     item { Text("The same catalog and cart power this screen and seven callable functions. An authorized caller can discover them on a supported Android device.") }
     items(listOf(
      "searchProducts" to "Search keywords with category, price and stock filters.",
      "getProducts" to "Browse and paginate products sorted by name or price.",
      "getProductDetails" to "Read full details using a stable product ID.",
      "getCategories" to "Get valid category IDs for filtering.",
      "getProductAvailability" to "Read demo stock without reserving it.",
      "getCart" to "Read cart lines and subtotal.",
      "setCartItemQuantity" to "Set an absolute quantity. Zero removes a line."
     )) { (name, description) ->
      Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
       Text(name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text(description)
      } }
     }
     item { Text("Example: Find wireless headphones below €100 that are in stock.\n\nAppFunctions is experimental. This screen documents the integration; use the upstream testing agent to verify OS discovery and execution.") }
    }
   }
  }
 }
 selectedId?.let { id ->
  val product = repository.product(id)
  val quantity = cart.lines.find { it.product.id == id }?.quantity ?: 0
  AlertDialog(onDismissRequest = { selectedId = null }, title = { Text(product.name) },
   text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(product.description); Text(money(product.priceMinor), style = MaterialTheme.typography.headlineSmall)
    Text(if (product.stock > 0) "${product.stock} demo units available" else "Out of stock")
    Text("Product ID: ${product.id}", style = MaterialTheme.typography.labelSmall)
    if (quantity > 0) Text("In your bag: $quantity")
   } },
   confirmButton = { Button(enabled = !busy && quantity < minOf(product.stock, 99), onClick = { setQuantity(id, quantity + 1) }) { Text(if (product.stock == 0) "Out of stock" else "Add to bag") } },
   dismissButton = { TextButton(onClick = { selectedId = null }) { Text("Close") } })
 }
 error?.let { message ->
  AlertDialog(onDismissRequest = { error = null }, title = { Text("Couldn’t update your bag") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } })
 }
}
