# Feemag Shop — ecommerce AppFunctions demo

A Kotlin/Jetpack Compose Android app with a fixed 12-product catalog, category and keyword search, stock filters, price sorting, product details and a persisted local cart. Seven AppFunctions expose the same repository to authorized callers. No backend, login, API key, payment or real orders are required or implemented.

## Open and build

Open this directory in Android Studio. Use a compatible modern Android Studio installation, JDK 17 or newer supported by Gradle 9.6.1, and Android SDK 37. The app runs on API 36+; AppFunctions availability also depends on the device's system implementation and caller permissions.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.feemag.shop/.MainActivity
```

Set `sdk.dir` in your own untracked `local.properties` if Android Studio does not supply it. Dependency versions follow the inspected upstream sample: AGP 9.3.1, Kotlin Compose compiler 2.4.10, KSP 2.3.10, AppFunctions 1.0.0-alpha10 and Compose BOM 2026.06.01. These are intentionally pinned rather than automatically upgraded.

## Functions

| Function | Purpose |
| --- | --- |
| `searchProducts` | All-keyword matching across name, description and category; optional category, price, stock, sorting and pagination |
| `getProducts` | Browse with the same filters, without a query |
| `getProductDetails` | Retrieve a product by stable ID |
| `getCategories` | Retrieve valid category identifiers |
| `getProductAvailability` | Read illustrative available stock |
| `getCart` | Read persisted cart state and subtotal |
| `setCartItemQuantity` | Set an absolute quantity; zero removes a line |

Prices are integer EUR cents. `maxPriceMinor=10000` means EUR 100. Search rejects blank queries and queries longer than 200 characters. `limit` is 1–50, `offset` is nonnegative, and `sortBy` is `name`, `price_asc` or `price_desc`. A page includes `total` and `nextOffset`; `-1` signals the end. An unmatched query succeeds with an empty page. Unknown IDs produce an element-not-found error; invalid arguments produce an invalid-argument error.

Cart quantities must be 0–99 and no greater than demo stock. Updates are serialized, saved before publication, and idempotent when repeated with identical arguments. The cart never reserves inventory. The app uses integer arithmetic for prices and totals.

## Verify agent discovery and execution

The Functions tab is documentation, not an embedded AI agent. Use the separate testing agent from [android/appfunctions](https://github.com/android/appfunctions/tree/main/agent) or device shell support to verify the OS integration.

1. Install this app on a compatible API 36+ device.
2. Run `adb shell cmd app_function help` and inspect the commands supported by that system image.
3. Where supported, run `adb shell cmd app_function list-app-functions` and find package `com.feemag.shop`. Use the exact function IDs reported by the device; do not assume a service class name is a function ID.
4. Alternatively clone the upstream repo, open `agent`, and follow its README to build and launch `./run_privileged.sh --build`. Privileged execution is necessary for the testing caller. The shop app itself does not request the caller's `EXECUTE_APP_FUNCTIONS` permission.
5. In the testing agent, choose this package, inspect the schema and manually execute these cases:

| Function | Input values | Expected result |
| --- | --- | --- |
| `getCategories` | none | audio, workspace, everyday |
| `searchProducts` | query `wireless headphones`, maxPriceMinor `10000`, inStockOnly `true` | audio-01, price 7990 |
| `getProducts` | categoryId `audio`, limit `2`, sortBy `price_asc` | total 3, nextOffset 2 |
| `getProductDetails` | productId `audio-01` | Studio Wireless Headphones |
| `getProductAvailability` | productId `audio-03` | availableQuantity 0, inStock false |
| `setCartItemQuantity` | productId `audio-01`, quantity `2` | itemCount 2 and subtotal 15980 when cart was empty |
| `setCartItemQuantity` | repeat identical arguments | Still two headphones |
| `getCart` | none | Same cart as the shop UI |
| `setCartItemQuantity` | productId `audio-01`, quantity `0` | Headphones removed |
| `searchProducts` | query `spaceship` | Empty page |
| `searchProducts` | query blank | Invalid argument |
| `getProductDetails` | productId `missing` | Element not found |
| `setCartItemQuantity` | productId `audio-03`, quantity `1` | Invalid argument: no stock |

For an LLM-based test, ask: “Find in-stock wireless headphones below €100.” The caller should search and present results. It should change the cart only after the user requests it. A subsequent request “Set the headphone quantity to two” should invoke the absolute-quantity function. LLM configuration and credentials belong to the testing agent, not this app.

AppFunctions is experimental. Merely installing this APK does not guarantee Gemini access; system integration and access programs may be restricted. See [Android's current overview](https://developer.android.com/ai/appfunctions).

## Structure

- `Catalog.kt`: fixed demo inventory and categories.
- `Models.kt`: documented serializable AppFunction results.
- `ShopRepository.kt`: search, validation, cart totals and state.
- `ShopApplication.kt`: application-scoped repository and private cart persistence.
- `BaseShopAppFunctionService.kt`: seven annotated entry points and typed errors.
- `MainActivity.kt`: Compose catalog, detail dialog, cart and function reference.
- `app/src/test`: repository behavior tests.

KSP generates the concrete `ShopAppFunctionService` and `shop_app_functions.xml`. The manifest declares the generated service with `BIND_APP_FUNCTION_SERVICE`, schema properties and app metadata. Do not manually create another concrete service with that name.

## Production work

Replace the fixed catalog with a real repository and durable database, add authenticated per-user carts, server-side inventory and pricing validation, accessibility/UI tests, and explicit order review before implementing checkout. KDoc is guidance to callers, not authorization enforcement. A real sensitive mutation must enforce its own authentication, authorization and confirmation requirements.

## Provenance and license

Integration and build patterns were adapted from Android's [AppFunctions samples](https://github.com/android/appfunctions), inspected at commit `16aa38ab1a5f31424c7f96ea020763e67d41f166`. The shop is a standalone project, not a fork of the full chat and testing-agent applications. Gradle wrapper files originate from that repository. See `LICENSE` and `NOTICE`.
