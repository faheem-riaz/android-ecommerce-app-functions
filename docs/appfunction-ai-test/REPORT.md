# AppFunctions AI-directed test report

Tested: 16 October 2026, Europe/Berlin.

**All seven registered AppFunctions succeeded. All 24 additional on-device regression checks passed. No functional defect was found in the tested scenarios.**

## How the AI test was performed

Codex acted as the AI caller: it inspected registered function descriptions, selected functions and arguments for shopping scenarios, interpreted actual Android responses, and used returned product IDs and continuation offsets in follow-up calls. Calls went through `adb shell cmd app_function execute-app-function`, Android's platform service, and the installed shop service. They were not mocked responses or direct repository calls.

The 24 additional checks used scripted assertions against the same real platform calls. These are regression checks, not 24 independent LLM reasoning trials.

The upstream Gemini-powered testing agent was inspected but was not installed or run. Gemini API access was not configured for this test. These results establish AI-directed device-bridge execution on this emulator, not Gemini application compatibility, autonomous Gemini tool selection, or permissions for an ordinary third-party caller.

## Environment

- Existing Medium Phone API 37.0 emulator, Android 17, ARM64; emulator-5554.
- Package: com.feemag.shop. All seven functions were discovered in Android's registry and enabled by default.
- The previous test used Pixel 7 Pro API 36. Today's API 37 emulator provides the missing AppFunctions shell commands.
- Tested the installed app. No app source changes or replacement APK were necessary.

## AI-directed scenarios

| Scenario / function | Actual result | Outcome |
| --- | --- | --- |
| Discover categories / getCategories | audio, workspace, everyday | PASS |
| Find in-stock wireless headphones below EUR 100 / searchProducts | audio-01, Studio Wireless Headphones, EUR 79.90, 12 units | PASS |
| Read returned ID / getProductDetails | Matching name, description, price and stock | PASS |
| Check headphone availability / getProductAvailability | inStock=true, availableQuantity=12 | PASS |
| Browse workspace products under EUR 50 / getProducts | Felt Desk Mat EUR 19.90, Laptop Stand EUR 29.90; total=3, nextOffset=2 | PASS |
| Follow nextOffset / getProducts | Focus Desk Lamp EUR 45.90; nextOffset=-1, no duplicates | PASS |
| Set the found headphones to two / setCartItemQuantity | itemCount=2, subtotalMinor=15980 | PASS |
| Read changed cart / getCart | Two headphones, EUR 159.80 | PASS |
| Unknown product ID / getProductDetails | Error code 1500 with recovery instructions | PASS |
| Restart app, call getCart | Two units and EUR 159.80 persisted | PASS |
| Inspect app's cart UI | Same function-written product, quantity and subtotal | PASS |
| Remove test line through AppFunctions | Empty cart; open UI updates without restart | PASS |

The cart was initially empty and was restored to empty after testing. The existing emulator was left running.

## Additional platform regression checks

| Check | Outcome |
| --- | --- |
| Browse with all optional arguments omitted | PASS |
| Search with optional arguments omitted | PASS |
| Case and repeated whitespace | PASS |
| No matching products is an empty success | PASS |
| Exact inclusive price boundary | PASS |
| In-stock filter excludes unavailable earbuds | PASS |
| Descending price order | PASS |
| Offset beyond result count | PASS |
| Zero stock is reported accurately | PASS |
| Blank query rejected | PASS |
| Overlong query rejected | PASS |
| Unknown category rejected | PASS |
| Negative price rejected | PASS |
| Invalid sort rejected | PASS |
| Negative offset rejected | PASS |
| Zero page size rejected | PASS |
| Oversized page rejected | PASS |
| Unavailable product cannot enter cart | PASS |
| Stock limit enforced | PASS |
| Negative cart quantity rejected | PASS |
| Quantity over 99 rejected | PASS |
| Unknown cart product rejected | PASS |
| Retrying quantity two does not duplicate items | PASS |
| Cart unchanged by rejected mutations | PASS |

## Evidence

- `calls.jsonl`: 35 recorded calls, including arguments, raw responses and exit codes. Expected error responses are preserved.
- `checks.json`: 24 named assertions and outcomes.
- `discovered-functions.json`: registry metadata returned by Android.
- `cart-from-appfunction.png`: UI showing two headphones and EUR 159.80 after AppFunction execution and app restart.
- `cart-ui.xml` and `restored-cart-ui.xml`: UI state before and after cleanup.

## Limits

This verifies the tested paths on API 37. Gemini-powered interaction, physical devices, other OS images and multi-user behavior are not covered. The app still uses demo inventory and does not implement checkout or real payments.
