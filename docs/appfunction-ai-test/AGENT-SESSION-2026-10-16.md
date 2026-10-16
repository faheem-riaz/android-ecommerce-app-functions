# AI-agent session — 16 October 2026 (evening)

Claude Code acted as the AI caller. It discovered the functions from Android's registry, chose functions and arguments for natural-language shopping requests, and used each response to decide the next call. Every call went through `adb shell cmd app_function execute-app-function` → Android's AppFunctionManager → the installed `ShopAppFunctionService`. No mocks.

Environment: Medium Phone API 37.0 emulator (Android 17, arm64). The installed APK's dex and `app_metadata.xml` are byte-identical to a fresh `./gradlew :app:assembleDebug` build. The build signature differs, so the existing install was tested. 11/11 unit tests passed.

## Scenarios

| Request | Calls the agent made | Result |
| --- | --- | --- |
| "Home office: lamp, desk mat and the cheapest in-stock audio item, under €150, add to bag" | `searchProducts(lamp)`, `searchProducts(desk mat)`, `getProducts(audio, inStockOnly, price_asc, limit 1)`, 3× `setCartItemQuantity`, `getCart` | desk-01 + desk-04 + audio-02 = €100.70; UI showed the same cart | PASS |
| "Make it 10 lamps" | `setCartItemQuantity(desk-01, 10)` | Rejected: exceeds stock (8); cart unchanged | PASS |
| "Add the earbuds" | `searchProducts(earbuds)`, `setCartItemQuantity(audio-03, 1)` | Rejected: stock 0; cart unchanged | PASS |
| "Bluetooth headphones under €100" | `searchProducts(Bluetooth headphones)` → 0 results; agent retried `searchProducts(headphones, max 10000, inStockOnly)` | Found audio-01 €79.90 on retry | PASS (see note) |
| "Make it 2 headphones, drop the speaker" | `setCartItemQuantity(audio-01, 2)` twice, `setCartItemQuantity(audio-02, 0)` | €260.50 → idempotent repeat → €225.60; UI matched | PASS |
| Change made in the UI (lamp −) | `getCart` | Agent saw the lamp removed, €179.70 | PASS |
| Invalid inputs: blank query, unknown ID, limit 0, bad sort | various | Error 1001 / 1500 with recovery hints | PASS |
| Persistence | `getCart` after two emulator reboots | Cart intact | PASS |

The cart was empty at the start and was restored to empty afterwards.

## Notes for agent quality

- Search is literal keyword matching (documented). "Bluetooth headphones", "earphones" and "gift" return 0 results. An LLM recovers by retrying with fewer words or by browsing `getProducts`. Adding synonyms or tags to the catalog would make first attempts succeed more often.
- Out-of-stock add says "exceeds available demo stock (0)". "Out of stock" would read better when an agent relays it to a user.

## Environment problem found (not an app bug)

Calls began timing out (30 s, also with `--timeout-duration 90`), even for `getCategories`, which returns a constant. Root cause: the AVD has `hw.cpu.ncore=1`. Inside the guest, CPU pressure was 100%, `surfaceflinger` had used 7+ minutes of CPU, and Google system apps (GMS, Google app, Android System Intelligence) were also failing to start in time. Restarting the emulator with `-cores 4 -gpu host` fixed it: the first call returned in 2 s.

Recommended: set `hw.cpu.ncore=4` in `~/.android/avd/Medium_Phone.avd/config.ini` (or in Device Manager → Edit → Advanced → CPU cores).

## Testing Agent "Missing Privileged Access"

That dialog appears when the AppFunction Testing Agent is opened from its launcher icon. It must be started with `tools/appfunctions/agent/startAppFunctionTestingAgent -s emulator-5554` (or `run_privileged.sh`). The script allowlists it and holds `EXECUTE_APP_FUNCTIONS` through shell-identity instrumentation. Keep that process running; don't run `uiautomator` while it is active.
