package com.feemag.shop

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AppFunctionServiceEntryPoint(serviceName = "ShopAppFunctionService", appFunctionXmlFileName = "shop_app_functions")
abstract class BaseShopAppFunctionService : AppFunctionService() {
 private val repository get() = (application as ShopApplication).repository

 /**
  * Search the demo product catalog by keywords. All whitespace-separated words must match the name, description or category. No semantic search is performed. Read-only.
  * @param query Nonblank keywords, at most 200 characters; use getProducts to browse without a query.
  * @param categoryId Optional exact category ID from getCategories.
  * @param maxPriceMinor Optional inclusive maximum price in EUR cents; 10000 means EUR 100.
  * @param inStockOnly Whether to exclude unavailable products.
  * @param sortBy One of name, price_asc, price_desc. Null defaults to name.
  * @param offset Zero-based result offset, at least zero.
  * @param limit Page size from 1 to 50. Null defaults to 20.
  * @return Matching products, total count and nextOffset (-1 when finished). No matches is an empty page.
  * @throws AppFunctionInvalidArgumentException For invalid filters or pagination. Correct the input before retrying.
  */
 @AppFunction(isDescribedByKDoc = true)
 suspend fun searchProducts(query: String, categoryId: String? = null, maxPriceMinor: Long? = null,
  inStockOnly: Boolean = false, sortBy: String? = null, offset: Int = 0, limit: Int? = null): ProductPage =
  translate { repository.products(query, categoryId, maxPriceMinor, inStockOnly, sortBy ?: "name", offset, limit ?: 20) }

 /**
  * Browse the fixed demo catalog without a search query. Read-only.
  * @param categoryId Optional category ID returned by getCategories.
  * @param maxPriceMinor Optional inclusive maximum price in nonnegative EUR cents.
  * @param inStockOnly True to exclude products with zero stock.
  * @param sortBy One of name, price_asc, price_desc. Null defaults to name.
  * @param offset Zero-based offset, at least zero.
  * @param limit Number of results from 1 to 50. Null defaults to 20.
  * @return A page with total count and nextOffset; -1 means the final page.
  * @throws AppFunctionInvalidArgumentException For invalid filters or pagination.
  */
 @AppFunction(isDescribedByKDoc = true)
 suspend fun getProducts(categoryId: String? = null, maxPriceMinor: Long? = null,
  inStockOnly: Boolean = false, sortBy: String? = null, offset: Int = 0, limit: Int? = null): ProductPage =
  translate { repository.products(null, categoryId, maxPriceMinor, inStockOnly, sortBy ?: "name", offset, limit ?: 20) }

 /**
  * Fetch the full details and current demo price of one product. Read-only.
  * @param productId Exact ID from getProducts or searchProducts.
  * @return The matching product.
  * @throws AppFunctionElementNotFoundException If the ID is unknown; search again to obtain a valid ID.
  */
 @AppFunction(isDescribedByKDoc = true)
 suspend fun getProductDetails(productId: String): Product = translate { repository.product(productId) }

 /**
  * List all categories for browsing or filtering. Read-only.
  * @return Stable category identifiers and their display names.
  */
 @AppFunction(isDescribedByKDoc = true)
 suspend fun getCategories(): List<Category> = Catalog.categories

 /**
  * Check illustrative stock for a product. Does not reserve inventory. Read-only.
  * @param productId Exact product ID obtained from searchProducts or getProducts.
  * @return Available quantity and whether at least one unit is in stock.
  * @throws AppFunctionElementNotFoundException If the product ID is unknown.
  */
 @AppFunction(isDescribedByKDoc = true)
 suspend fun getProductAvailability(productId: String): Availability = translate {
  repository.product(productId).let { Availability(it.id, it.stock, it.stock > 0) }
 }

 /**
  * Read the local cart and its subtotal. No checkout or stock reservation is performed.
  * @return Cart lines and subtotal in EUR cents, excluding shipping and tax calculations.
  */
 @AppFunction(isDescribedByKDoc = true)
 suspend fun getCart(): Cart = repository.cart.value

 /**
  * Set the absolute quantity of a product in the local demo cart only when the user asks to change the cart. Repeating the same arguments is idempotent. Does not purchase anything or reserve stock.
  * @param productId Exact ID from searchProducts or getProducts.
  * @param quantity Absolute desired quantity from 0 to 99, no greater than stock. Zero removes the item.
  * @return Updated persisted cart and subtotal in EUR cents.
  * @throws AppFunctionInvalidArgumentException For a quantity outside the range or exceeding stock.
  * @throws AppFunctionElementNotFoundException For an unknown product ID; search again.
  * @throws AppFunctionAppUnknownException If the cart cannot be saved; suggest retrying.
  */
 @AppFunction(isDescribedByKDoc = true)
 suspend fun setCartItemQuantity(productId: String, quantity: Int): Cart = withContext(Dispatchers.IO) {
  translate { repository.setQuantity(productId, quantity) }
 }

 private inline fun <T> translate(block: () -> T): T = try { block() }
 catch (e: NoSuchElementException) { throw AppFunctionElementNotFoundException(e.message ?: "Product not found") }
 catch (e: IllegalArgumentException) { throw AppFunctionInvalidArgumentException(e.message ?: "Invalid argument") }
 catch (e: IllegalStateException) { throw AppFunctionAppUnknownException(e.message ?: "Storage unavailable") }
}
