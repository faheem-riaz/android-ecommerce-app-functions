package com.feemag.shop

import androidx.appfunctions.AppFunctionSerializable

/** A product from the fixed demo catalog; prices and stock are illustrative. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class Product(
 /** Stable identifier to use in detail, stock and cart functions. */
 val id: String,
 /** Product display name. */
 val name: String,
 /** Searchable description of the product. */
 val description: String,
 /** Category identifier returned by getCategories. */
 val categoryId: String,
 /** Price in integer EUR cents; 7990 means EUR 79.90. */
 val priceMinor: Long,
 /** ISO 4217 currency code, always EUR in this demo. */
 val currency: String,
 /** Illustrative available quantity; carts do not reserve stock. */
 val stock: Int,
)

/** A bounded page of products with a continuation offset. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class ProductPage(
 /** Products in this page. */
 val products: List<Product>,
 /** Total number matching the filters before pagination. */
 val total: Int,
 /** Offset for the next page; -1 means there are no more results. */
 val nextOffset: Int,
)

/** A category that can be used to filter products. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class Category(
 /** Stable category identifier. */
 val id: String,
 /** Human-readable category name. */
 val name: String,
)

/** One line of the local demo shopping cart. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class CartLine(
 /** The product at current demo catalog pricing. */
 val product: Product,
 /** Number of units selected. */
 val quantity: Int,
 /** Price times quantity in EUR cents. */
 val lineTotalMinor: Long,
)

/** Local cart snapshot. No payment, order or stock reservation is created. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class Cart(
 /** All nonzero cart lines. */
 val lines: List<CartLine>,
 /** Sum of quantities across all lines. */
 val itemCount: Int,
 /** Sum of line totals in EUR cents, excluding shipping and tax calculations. */
 val subtotalMinor: Long,
 /** ISO currency code. */
 val currency: String,
)

/** Stock information for a single demo product. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class Availability(
 /** Stable product identifier. */
 val productId: String,
 /** Available demo units. */
 val availableQuantity: Int,
 /** True when at least one unit is available. */
 val inStock: Boolean,
)
