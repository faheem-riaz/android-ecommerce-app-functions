package com.feemag.shop

import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface CartStorage {
 fun load(): Map<String, Int>
 fun save(quantities: Map<String, Int>)
}

class ShopRepository(private val storage: CartStorage) {
 private val quantities = storage.load().filter { (id, qty) ->
  Catalog.products.any { it.id == id && qty in 1..minOf(it.stock, 99) }
 }.toMutableMap()
 private val mutableCart = MutableStateFlow(snapshot())
 val cart = mutableCart.asStateFlow()

 fun product(id: String): Product = Catalog.products.find { it.id == id }
  ?: throw NoSuchElementException("Unknown product ID. Use searchProducts or getProducts first.")

 fun products(query: String? = null, categoryId: String? = null, maxPriceMinor: Long? = null,
  inStockOnly: Boolean = false, sortBy: String = "name", offset: Int = 0, limit: Int = 20): ProductPage {
  require(query == null || query.isNotBlank()) { "Search query must not be blank. Use getProducts to browse." }
  require(query == null || query.length <= 200) { "Search query must be at most 200 characters." }
  require(categoryId == null || Catalog.categories.any { it.id == categoryId }) { "Unknown category ID. Use getCategories." }
  require(maxPriceMinor == null || maxPriceMinor >= 0) { "Maximum price must be nonnegative EUR cents." }
  require(offset >= 0) { "Offset must be nonnegative." }
  require(limit in 1..50) { "Limit must be between 1 and 50." }
  require(sortBy in listOf("name", "price_asc", "price_desc")) { "Sort must be name, price_asc or price_desc." }
  val terms = query?.trim()?.lowercase(Locale.ROOT)?.split(Regex("\\s+")) ?: emptyList()
  val matches = Catalog.products.filter { p ->
   val text = "${p.name} ${p.description} ${p.categoryId}".lowercase(Locale.ROOT)
   terms.all { it in text } && (categoryId == null || p.categoryId == categoryId) &&
    (maxPriceMinor == null || p.priceMinor <= maxPriceMinor) && (!inStockOnly || p.stock > 0)
  }
  val sorted = when (sortBy) {
   "price_asc" -> matches.sortedWith(compareBy<Product> { it.priceMinor }.thenBy { it.id })
   "price_desc" -> matches.sortedWith(compareByDescending<Product> { it.priceMinor }.thenBy { it.id })
   else -> matches.sortedWith(compareBy<Product> { it.name }.thenBy { it.id })
  }
  val page = sorted.drop(offset).take(limit)
  val end = offset.toLong() + page.size
  return ProductPage(page, sorted.size, if (end < sorted.size) end.toInt() else -1)
 }

 @Synchronized fun setQuantity(productId: String, quantity: Int): Cart {
  val product = product(productId)
  require(quantity in 0..99) { "Quantity must be between 0 and 99; zero removes a line." }
  require(quantity <= product.stock) { "Requested quantity exceeds available demo stock (${product.stock})." }
  val next = quantities.toMutableMap()
  if (quantity == 0) next.remove(productId) else next[productId] = quantity
  storage.save(next)
  quantities.clear()
  quantities.putAll(next)
  return snapshot().also { mutableCart.value = it }
 }

 private fun snapshot(): Cart {
  val lines = quantities.toSortedMap().map { (id, qty) ->
   val product = product(id)
   CartLine(product, qty, product.priceMinor * qty)
  }
  return Cart(lines, lines.sumOf { it.quantity }, lines.sumOf { it.lineTotalMinor }, "EUR")
 }
}
