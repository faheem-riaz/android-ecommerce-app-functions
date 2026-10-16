---
title: "Building an Android Ecommerce App That AI Agents Can Use"
slug: "android-ecommerce-appfunctions-kotlin"
description: "Build a Kotlin ecommerce demo with Android AppFunctions: product search, structured results, stock checks, and a persistent cart for authorized agents."
tags: [Android, Kotlin, Jetpack Compose, AppFunctions, AI]
status: draft
---

# Building an Android Ecommerce App That AI Agents Can Use

“Find wireless headphones below €100 that are in stock.”

A shopping app already knows how to answer that request. Its catalog contains the names, prices, categories and stock quantities. The interesting engineering question is how to make that capability available to an AI agent without making the agent navigate the shopping screens.

For this project, I built **Feemag Shop**, an Android ecommerce demo with a Compose interface and seven AppFunctions. A person can browse the catalog and manage a bag. An authorized agent can call functions that use the same underlying repository.

The demo deliberately stops at a local cart. It has no real payments, shipping, customer accounts or order placement. That keeps the example focused on exposing useful, structured capabilities.

## Starting with Google's sample

The starting reference was Google's [`android/appfunctions`](https://github.com/android/appfunctions) repository. It contains a chat application and a separate testing agent. The chat example shows how to expose app operations; the agent provides a way to inspect and exercise them.

Feemag Shop is a standalone ecommerce project based on those integration patterns. It uses the sample's AppFunctions `1.0.0-alpha10` service-entry-point architecture, with Kotlin Symbol Processing generating the concrete service and function metadata. Version details matter here: older examples may use a different configuration model. The [AndroidX release notes](https://developer.android.com/jetpack/androidx/releases/appfunctions) describe the service-entry-point change.

AppFunctions is experimental. Android's documentation describes support on Android 16 and higher, with discovery and execution restricted to authorized callers. Installing an app with annotated functions does not, by itself, mean that every assistant—including Gemini—can use it. Check the [current AppFunctions overview](https://developer.android.com/ai/appfunctions) for availability and access requirements.

## Choose functions around shopping tasks

I chose a small set of operations with clear responsibilities:

| Function | What the caller gets or changes |
| --- | --- |
| `searchProducts` | Products matching keywords and optional filters |
| `getProducts` | A page of products without a search query |
| `getProductDetails` | Full details for one stable product ID |
| `getCategories` | Valid category IDs and display names |
| `getProductAvailability` | Illustrative stock for one product |
| `getCart` | Cart lines, item count and subtotal |
| `setCartItemQuantity` | An absolute quantity in the local cart |

Six functions read data. One changes the cart. There is no function that silently turns a product recommendation into a purchase.

The catalog includes 12 demo products across Audio, Workspace and Everyday. The data is intentionally local and deterministic, making it easy to reproduce a search, inspect a result and test an edge case.

## Share the business logic

The architecture is small:

```text
Compose screens ────────────┐
                            ├── ShopRepository ── Demo catalog
AppFunctions service ───────┘                  └── Local cart storage
```

`ShopRepository` owns search behavior, validation, stock checks and cart calculations. The application creates one repository instance. Both the UI and the AppFunctions service use it.

This prevents a common integration problem: implementing one set of rules for people and another for agents. If a product is unavailable, both interfaces apply the same restriction. When a function changes the cart, the repository publishes the new state and the Compose UI observes it.

For a production store, this repository would be the boundary for your API and database. Exposing AppFunctions does not remove the need for server-side pricing, stock validation or authentication.

## Return objects an agent can work with

A search result should include enough information for the next step. A product name alone is not enough: the caller also needs a stable ID, price units and stock information.

The implementation uses `@AppFunctionSerializable` data classes. Here is the product shape, abbreviated to keep the example readable:

```kotlin
@AppFunctionSerializable(isDescribedByKDoc = true)
data class Product(
    val id: String,
    val name: String,
    val description: String,
    val categoryId: String,
    val priceMinor: Long,
    val currency: String,
    val stock: Int,
)
```

In the project, every field has KDoc describing its meaning. `priceMinor` uses integer EUR cents, so `7990` means €79.90. The explicit currency field helps the caller avoid guessing, while integer arithmetic keeps cart calculations free from floating-point rounding surprises.

The result wrapper also makes pagination explicit:

```kotlin
@AppFunctionSerializable(isDescribedByKDoc = true)
data class ProductPage(
    val products: List<Product>,
    val total: Int,
    val nextOffset: Int,
)
```

`total` is the number of matching products before pagination. `nextOffset` tells the caller where to continue; `-1` means there are no more results. A search with no matches returns an empty page, not an exception.

## Wire the AppFunctions service

The project adds the runtime and compiler dependencies:

```kotlin
implementation("androidx.appfunctions:appfunctions:1.0.0-alpha10")
ksp("androidx.appfunctions:appfunctions-compiler:1.0.0-alpha10")
```

The base service is declared like this:

```kotlin
@AppFunctionServiceEntryPoint(
    serviceName = "ShopAppFunctionService",
    appFunctionXmlFileName = "shop_app_functions",
)
abstract class BaseShopAppFunctionService : AppFunctionService() {
    // Annotated functions delegate to the shared repository.
}
```

KSP generates the concrete service and metadata. The manifest registers that generated service with the platform binding permission and the schema properties. App-level metadata explains that this is a demo store, how prices are represented, and which workflow the caller should follow. The full project includes this wiring; copying an annotation alone is not the entire integration.

The declaration follows the [official integration guidance](https://developer.android.com/ai/appfunctions/add-appfunctions). Keeping the runtime and compiler versions aligned is especially useful while the API is evolving.

## Make search predictable

`searchProducts` accepts these inputs:

```kotlin
suspend fun searchProducts(
    query: String,
    categoryId: String? = null,
    maxPriceMinor: Long? = null,
    inStockOnly: Boolean = false,
    sortBy: String? = null,
    offset: Int = 0,
    limit: Int? = null,
): ProductPage
```

The actual implementation is annotated with `@AppFunction(isDescribedByKDoc = true)` and documents each argument, the return value and expected errors.

Search is case-insensitive keyword matching across the product name, description and category. Every whitespace-separated word must match. It is not semantic search: the agent translates the user's request into structured filters, and the app performs a deterministic query.

For the opening example, the intended arguments are:

```json
{
  "query": "wireless headphones",
  "maxPriceMinor": 10000,
  "inStockOnly": true,
  "sortBy": "price_asc",
  "offset": 0,
  "limit": 20
}
```

These are function argument values, not a complete platform execution request. A caller must supply the package and the function identifier reported by discovery.

Against the demo catalog, this search returns `audio-01`, Studio Wireless Headphones, at 7990 cents with 12 available demo units. The caller can then use that ID for details or availability. It should not invent IDs from product names.

Optional `sortBy` and `limit` are nullable at the function boundary; the service applies `name` and `20` when they are absent. This makes the defaults explicit in the implementation rather than relying on the generated dispatcher to preserve a nonzero Kotlin default.

Inputs have explicit bounds: queries are nonblank and at most 200 characters, pages contain 1–50 products, offsets are nonnegative, and maximum prices cannot be negative. Valid sort values are `name`, `price_asc` and `price_desc`.

## Design cart updates for retries

Agents may repeat tool calls. A function called `addToCart(productId, quantity)` can be ambiguous under retry: did the user want another two units, or did the caller repeat the previous request?

This demo exposes an absolute update instead:

```kotlin
setCartItemQuantity(productId = "audio-01", quantity = 2)
```

Calling it twice leaves two headphones in the cart. A quantity of zero removes the line. Quantities must be between zero and 99 and cannot exceed the product's demo stock.

The repository serializes updates, writes the new cart to private app storage, and only then publishes the updated state. If persistence fails, it does not tell the UI or agent that an unsaved update succeeded. AppFunction writes run on an IO dispatcher.

The function documentation also says to change the cart only when the user requests it. That is guidance for the caller, not a security mechanism. A production app must enforce its own identity, permissions and confirmation requirements for sensitive actions.

## Give callers useful errors

The service converts domain failures into AppFunctions errors:

- Invalid filters, blank searches or excessive quantities become `AppFunctionInvalidArgumentException`.
- Unknown product IDs become `AppFunctionElementNotFoundException`.
- A failed cart save becomes `AppFunctionAppUnknownException`.

The messages explain how to recover: use `getCategories` for a valid category, search again for a valid product ID, or retry a failed save. An empty result set remains a normal result. Those distinctions let an agent choose a sensible next action.

## Test the repository and the platform separately

The repository tests cover multiword search, case handling, inclusive price limits, empty results, pagination, invalid arguments, unknown IDs, stock constraints, totals, removal, idempotence, persistence and failed saves.

Run the project checks with:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Passing those checks does not prove that an external agent can discover the functions. Platform testing is a separate step:

1. Install the APK on a compatible Android device or emulator.
2. Inspect `adb shell cmd app_function help` to see what that system image supports.
3. Where available, use `list-app-functions` to inspect registration and obtain the actual function IDs.
4. Use the repository's [testing agent](https://github.com/android/appfunctions/tree/main/agent) for deterministic manual calls and, when configured, LLM-based testing.
5. Search for a product, set its cart quantity, read the cart, and confirm that the app UI shows the same result.

The README includes exact inputs and expected outcomes for each of the seven functions. The supplied verification report records which checks were actually completed in the development environment; these instructions should not be read as a claim that every device or assistant integration was tested.

## Where to take it next

A real store needs authenticated carts, fresh inventory, backend price validation, shipping and tax calculations, and an explicit order-review flow. A local catalog is useful for learning the integration, but it is not a commerce backend.

The useful result of this project is a reusable boundary: product discovery and cart operations live in ordinary application logic, with a thin, documented AppFunctions layer exposing them to authorized callers. The Compose interface remains available to people, and agents get structured inputs and results they can reason about.
