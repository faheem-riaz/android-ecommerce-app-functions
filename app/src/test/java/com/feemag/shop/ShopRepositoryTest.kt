package com.feemag.shop

import org.junit.Assert.*
import org.junit.Test

class ShopRepositoryTest {
 private class MemoryStorage(var data: Map<String, Int> = emptyMap()) : CartStorage {
  var fail = false
  override fun load() = data
  override fun save(quantities: Map<String, Int>) {
   check(!fail) { "Storage failure" }
   data = quantities.toMap()
  }
 }
 private fun repository() = ShopRepository(MemoryStorage())
 @Test fun keywordSearchCombinesWordsAndFilters() {
  val page = repository().products(" WIRELESS   headphones ", "audio", 10000, true)
  assertEquals(listOf("audio-01"), page.products.map { it.id })
 }
 @Test fun emptyResultsAreSuccessful() {
  val page = repository().products("spaceship")
  assertEquals(0, page.total)
  assertEquals(-1, page.nextOffset)
 }
 @Test fun priceBoundaryIsInclusive() {
  assertEquals(listOf("daily-03"), repository().products(maxPriceMinor = 990).products.map { it.id })
 }
 @Test fun paginationDoesNotDuplicateProducts() {
  val repo = repository()
  val ids = mutableListOf<String>()
  var offset = 0
  do {
   val page = repo.products(sortBy = "price_asc", offset = offset, limit = 3)
   ids.addAll(page.products.map { it.id })
   offset = page.nextOffset
  } while (offset != -1)
  assertEquals(12, ids.size)
  assertEquals(12, ids.toSet().size)
  assertEquals("daily-03", ids.first())
 }
 @Test fun offsetBeyondEndDoesNotOverflow() {
  val page = repository().products(offset = Int.MAX_VALUE)
  assertTrue(page.products.isEmpty())
  assertEquals(-1, page.nextOffset)
 }
 @Test fun invalidInputsAreRejected() {
  val repo = repository()
  listOf<() -> Unit>(
   { repo.products(" ") }, { repo.products("a".repeat(201)) },
   { repo.products(categoryId = "unknown") }, { repo.products(maxPriceMinor = -1) },
   { repo.products(sortBy = "unknown") }, { repo.products(offset = -1) },
   { repo.products(limit = 0) }, { repo.products(limit = 51) }
  ).forEach { operation -> assertThrows(IllegalArgumentException::class.java) { operation() } }
 }
 @Test fun cartUpdatesAreIdempotentAndPersistAcrossInstances() {
  val storage = MemoryStorage()
  val repo = ShopRepository(storage)
  repo.setQuantity("audio-01", 2)
  val cart = repo.setQuantity("audio-01", 2)
  assertEquals(2, cart.itemCount)
  assertEquals(15980L, cart.subtotalMinor)
  assertEquals(cart, ShopRepository(storage).cart.value)
 }
 @Test fun cartTotalsAndRemoval() {
  val repo = repository()
  repo.setQuantity("audio-01", 2)
  assertEquals(16970L, repo.setQuantity("daily-03", 1).subtotalMinor)
  assertEquals(990L, repo.setQuantity("audio-01", 0).subtotalMinor)
  assertEquals(0, repo.setQuantity("daily-03", 0).itemCount)
 }
 @Test fun stockAndUnknownIdsAreValidated() {
  val repo = repository()
  assertThrows(IllegalArgumentException::class.java) { repo.setQuantity("audio-03", 1) }
  assertThrows(IllegalArgumentException::class.java) { repo.setQuantity("audio-01", 13) }
  assertThrows(IllegalArgumentException::class.java) { repo.setQuantity("audio-01", -1) }
  assertThrows(IllegalArgumentException::class.java) { repo.setQuantity("audio-01", 100) }
  assertThrows(NoSuchElementException::class.java) { repo.product("missing") }
  assertThrows(NoSuchElementException::class.java) { repo.setQuantity("missing", 0) }
  assertEquals(0, repo.cart.value.itemCount)
 }
 @Test fun storageFailureDoesNotPublishAnUnsavedCart() {
  val storage = MemoryStorage()
  val repo = ShopRepository(storage)
  repo.setQuantity("daily-03", 1)
  storage.fail = true
  assertThrows(IllegalStateException::class.java) { repo.setQuantity("daily-03", 2) }
  assertEquals(1, repo.cart.value.itemCount)
  assertEquals(1, storage.data["daily-03"])
 }
 @Test fun invalidStoredLinesAreDiscarded() {
  val repo = ShopRepository(MemoryStorage(mapOf("missing" to 2, "audio-03" to 1, "daily-03" to 2, "audio-01" to -1)))
  assertEquals(2, repo.cart.value.itemCount)
  assertEquals("daily-03", repo.cart.value.lines.single().product.id)
 }
}
