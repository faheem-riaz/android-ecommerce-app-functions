# Groq / OpenAI AppFunctions Testing Agent

This is a modified checkout of https://github.com/android/appfunctions at commit `16aa38ab1a5f31424c7f96ea020763e67d41f166`. It adds Groq (free plan) and OpenAI to the official Gemini testing agent. Feemag Shop is a separate installed app.

Persistent source location: `/Volumes/usb/projects/my_projects/android_ecommerce_app_functions/tools/appfunctions/agent`.

## Start

Use Android Studio's **Medium Phone API 37.0** emulator. From the agent directory:

```sh
./run_privileged.sh --serial emulator-5554
```

Use `--build` after changing source. The installed SDK is `/Volumes/usb/development/android-sdk`; the JDK is `/Volumes/usb/Applications/Android Studio.app/Contents/jbr/Contents/Home`. Gradle uses the `all` distribution of 9.6.1.

The official launcher adds the agent to the emulator's development allowlist and starts shell-identity instrumentation. Restart it after force-stopping the app or rebooting. This development launch is not a production permission mechanism. Do not run UIAutomator while this instrumentation is active.

## Groq setup (free plan)

1. Create a key at https://console.groq.com (free plan; limits are shown on your account's Limits page).
2. In the testing agent's **Settings**, paste it into the masked **Groq API key** field, then leave Settings to save it.
3. Start a new Agent Demo chat. New chats default to **openai/gpt-oss-120b** on Groq. `openai/gpt-oss-20b` and `qwen/qwen3.8-27b` are also offered.
4. Run the same Feemag Shop prompts as in the OpenAI steps below.

Groq's Responses API has no `previous_response_id`, so the Groq adapter uses Chat Completions and keeps each conversation in app memory. After the agent restarts, start a new chat. Free-plan rate limits are low: a 429 error means wait a minute.

## Model list (checked 17 October 2026)

| Provider | Offered models |
| --- | --- |
| Groq | openai/gpt-oss-120b (default), openai/gpt-oss-20b, qwen/qwen3.8-27b |
| OpenAI | gpt-6-luna, gpt-6.1-sol |
| Gemini | gemini-3.8-flash, gemini-3.5-flash-lite, gemini-4-argon, gemini-3.1-pro-preview |

Retired Groq models (llama-3.3-70b-versatile, llama-3.1-8b-instant, Llama 4, Kimi) are not offered. Older OpenAI/Gemini entries are hidden from the picker but kept so saved chats still load. `gemini-3.1-flash-lite-preview` now maps to `gemini-3.1-flash-lite`.

## OpenAI setup

1. Open the testing agent's **Settings**.
2. Enter your OpenAI API key in the masked **OpenAI API key** field. Do not send the key in chat or commit it.
3. Leave Settings to save it.
4. Start a new Agent Demo chat and pick **gpt-6-luna**. Existing chats keep their original model.
5. Connect/select **Feemag Shop**.
6. Ask: “Find in-stock wireless headphones under EUR 100 in Feemag Shop.”
7. Inspect the tool call and returned product. The demo catalog contains Studio Wireless Headphones, `audio-01`, EUR 79.90.
8. Ask: “Set those headphones to quantity two in my cart.” Then: “Show my cart.” Expected subtotal: EUR 159.80.
9. Restore the demo: “Set audio-01 quantity to zero.”

OpenAI API access and billing are separate from a ChatGPT subscription. The Gemini service-tier setting does not affect OpenAI requests.

## Integration

The agent discovers enabled Android AppFunctions, converts their parameter metadata into Responses API function tools, executes model-selected calls through Android, then returns results with the original `call_id`. `previous_response_id` preserves conversation state, including reasoning context. Tool names include a package/function hash to prevent cross-app collisions. Only advertised functions can be dispatched; incomplete, malformed and unknown-call responses fail without execution. Interaction loops stop after 12 model rounds.

App function inputs/results are sent to OpenAI. Responses use `store: true` to continue conversations by response ID. The local demo saves the key in app-private DataStore and excludes it from cloud backup/device transfer. For distribution, use a backend instead of embedding a shared API key in an APK.

Gemini remains available. Its sample-specific internal tools are excluded from OpenAI and Groq conversations. Claude would require a separate Messages API adapter; it has not been added. This is an Android agent using the OpenAI API, not an integration into the consumer ChatGPT app.

## Verification boundaries

Unit tests use a simulated HTTP response to exercise tool selection and result return. Android discovery/execution and a live OpenAI model round trip are separate checks. A live AI pass requires an entered API key and successful chat; a built APK alone does not establish that.

References: [OpenAI function calling](https://developers.openai.com/api/docs/guides/function-calling), [OpenAI models](https://developers.openai.com/api/docs/models), [Groq tool use](https://console.groq.com/docs/tool-use), [Groq deprecations](https://console.groq.com/docs/deprecations), [official Android sample](https://github.com/android/appfunctions).
