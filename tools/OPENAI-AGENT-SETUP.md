# OpenAI AppFunctions Testing Agent

This is a modified checkout of https://github.com/android/appfunctions at commit `16aa38ab1a5f31424c7f96ea020763e67d41f166`. It adds OpenAI to the official Gemini testing agent. Feemag Shop is a separate installed app.

Persistent source location: `/Volumes/usb/projects/my_projects/android_ecommerce_app_functions/tools/appfunctions/agent`.

## Start

Use Android Studio's **Medium Phone API 37.0** emulator. From the agent directory:

```sh
./run_privileged.sh --serial emulator-5554
```

Use `--build` after changing source. The installed SDK is `/Volumes/usb/development/android-sdk`; the JDK is `/Volumes/usb/Applications/Android Studio.app/Contents/jbr/Contents/Home`. Gradle uses the `all` distribution of 9.6.1.

The official launcher adds the agent to the emulator's development allowlist and starts shell-identity instrumentation. Restart it after force-stopping the app or rebooting. This development launch is not a production permission mechanism. Do not run UIAutomator while this instrumentation is active.

## OpenAI setup

1. Open the testing agent's **Settings**.
2. Enter your OpenAI API key in the masked **OpenAI API key** field. Do not send the key in chat or commit it.
3. Leave Settings to save it.
4. Start a new Agent Demo chat with **gpt-5.4-mini**, the default for new chats. Existing Gemini chats keep their original model.
5. Connect/select **Feemag Shop**.
6. Ask: “Find in-stock wireless headphones under EUR 100 in Feemag Shop.”
7. Inspect the tool call and returned product. The demo catalog contains Studio Wireless Headphones, `audio-01`, EUR 79.90.
8. Ask: “Set those headphones to quantity two in my cart.” Then: “Show my cart.” Expected subtotal: EUR 159.80.
9. Restore the demo: “Set audio-01 quantity to zero.”

OpenAI API access and billing are separate from a ChatGPT subscription. The Gemini service-tier setting does not affect OpenAI requests.

## Integration

The agent discovers enabled Android AppFunctions, converts their parameter metadata into Responses API function tools, executes model-selected calls through Android, then returns results with the original `call_id`. `previous_response_id` preserves conversation state, including reasoning context. Tool names include a package/function hash to prevent cross-app collisions. Only advertised functions can be dispatched; incomplete, malformed and unknown-call responses fail without execution. Interaction loops stop after 12 model rounds.

App function inputs/results are sent to OpenAI. Responses use `store: true` to continue conversations by response ID. The local demo saves the key in app-private DataStore and excludes it from cloud backup/device transfer. For distribution, use a backend instead of embedding a shared API key in an APK.

Gemini remains available. Its sample-specific internal tools are excluded from OpenAI conversations. Claude would require a separate Messages API adapter; it has not been added. This is an Android agent using the OpenAI API, not an integration into the consumer ChatGPT app.

## Verification boundaries

Unit tests use a simulated HTTP response to exercise tool selection and result return. Android discovery/execution and a live OpenAI model round trip are separate checks. A live AI pass requires an entered API key and successful chat; a built APK alone does not establish that.

References: [OpenAI function calling](https://developers.openai.com/api/docs/guides/function-calling), [GPT-5.4 mini](https://developers.openai.com/api/docs/models/gpt-5.4-mini), [official Android sample](https://github.com/android/appfunctions).
