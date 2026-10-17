package com.example.appfunctions.agent.data

import androidx.appfunctions.metadata.*
import com.example.appfunctions.agent.domain.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class GroqProviderImplTest {
    private val converter = OpenAiToolConverter()
    private val tool = AppFunctionMetadata(
        id = "Shop#searchProducts", packageName = "com.feemag.shop", isEnabled = true, schema = null,
        parameters = listOf(AppFunctionParameterMetadata("query", true,
            AppFunctionStringTypeMetadata(false), "Search query")),
        response = AppFunctionResponseMetadata(AppFunctionStringTypeMetadata(false), "Products"),
        components = AppFunctionComponentsMetadata(emptyMap()), description = "Search products", deprecation = null,
    )
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    @Test fun toolCallRoundTripReplaysHistoryWithToolCallId() = runBlocking {
        val requests = mutableListOf<JsonObject>()
        val name = converter.getToolName(tool)
        val client = HttpClient(MockEngine { request ->
            requests += Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            val body = if (requests.size == 1)
                """{"choices":[{"finish_reason":"tool_calls","message":{"role":"assistant","content":null,"reasoning":"think","tool_calls":[{"id":"call_1","type":"function","function":{"name":"$name","arguments":"{\"query\":\"headphones\"}"}}]}}]}"""
            else """{"choices":[{"finish_reason":"stop","message":{"role":"assistant","content":"Headphones cost EUR 79.90."}}]}"""
            respond(body, HttpStatusCode.OK, json)
        })
        try {
            val provider = GroqProviderImpl(client, converter)
            val first = provider.generateResponse(null, LlmInput.UserMessage("Find headphones"), listOf(tool), "test", "openai/gpt-oss-120b") as LlmResponse.Success
            val call = first.parts.single() as LlmResponsePart.ToolCall
            assertEquals("com.feemag.shop", call.packageName)
            assertEquals("headphones", call.arguments["query"])
            assertEquals("call_1", call.callId)

            val sentTool = requests[0].getValue("tools").jsonArray.single().jsonObject
            assertEquals("function", sentTool.getValue("type").jsonPrimitive.content)
            assertEquals(name, sentTool.getValue("function").jsonObject.getValue("name").jsonPrimitive.content)
            assertFalse(sentTool.getValue("function").jsonObject.containsKey("strict"))

            val second = provider.generateResponse(first.interactionId,
                LlmInput.ToolResponse(listOf(ToolOutput(call.functionId, call.callId, "{\"priceMinor\":7990}"))),
                listOf(tool), "test", "openai/gpt-oss-120b") as LlmResponse.Success
            assertEquals("Headphones cost EUR 79.90.", (second.parts.single() as LlmResponsePart.Text).text)

            val replay = requests[1].getValue("messages").jsonArray.map { it.jsonObject }
            assertEquals(listOf("system", "user", "assistant", "tool"), replay.map { it.getValue("role").jsonPrimitive.content })
            assertFalse("reasoning must not be replayed", replay[2].containsKey("reasoning"))
            assertEquals("call_1", replay[3].getValue("tool_call_id").jsonPrimitive.content)
            assertNotEquals(first.interactionId, second.interactionId)
        } finally { client.close() }
    }

    @Test fun rejectsUnknownFunctionsTruncationErrorsAndLostHistory() = runBlocking {
        for ((status, body) in listOf(
            HttpStatusCode.OK to """{"choices":[{"finish_reason":"tool_calls","message":{"tool_calls":[{"id":"c","function":{"name":"unadvertised","arguments":"{}"}}]}}]}""",
            HttpStatusCode.OK to """{"choices":[{"finish_reason":"length","message":{"content":"partial"}}]}""",
            HttpStatusCode.TooManyRequests to """{"error":{"message":"rate limit"}}""",
            HttpStatusCode.OK to "not json",
        )) {
            val client = HttpClient(MockEngine { respond(body, status, json) })
            try {
                assertTrue(body, GroqProviderImpl(client, converter).generateResponse(null, LlmInput.UserMessage("test"), listOf(tool), "test", "openai/gpt-oss-120b") is LlmResponse.Error)
            } finally { client.close() }
        }
        val client = HttpClient(MockEngine { error("must not call Groq without history") })
        try {
            val result = GroqProviderImpl(client, converter).generateResponse("groq_unknown",
                LlmInput.ToolResponse(listOf(ToolOutput("f", "c", "{}"))), listOf(tool), "test", "openai/gpt-oss-120b")
            assertTrue(result is LlmResponse.Error)
            assertTrue(GroqProviderImpl(client, converter).generateResponse(null, LlmInput.UserMessage("x"), listOf(tool), " ", "m") is LlmResponse.Error)
        } finally { client.close() }
    }

    @Test fun pickerOffersOnlyCurrentModelsAndKeepsLegacyThreadsLoadable() {
        assertEquals(LlmModel.GROQ_GPT_OSS_120B, LlmModel.DEFAULT)
        assertTrue(LlmModel.DEFAULT in LlmModel.SELECTABLE)
        assertTrue(LlmModel.OPENAI_GPT_5_4_MINI in LlmModel.LEGACY)
        assertEquals(LlmModel.entries.size, LlmModel.SELECTABLE.size + LlmModel.LEGACY.size)
        assertEquals("gemini-3.1-flash-lite", LlmModel.valueOf("GEMINI_3_1_FLASH_LITE_PREVIEW").modelName)
        assertEquals(LlmModel.DEFAULT, LlmModel.forNewThread(LlmModel.GEMINI_3_5_FLASH))
        assertEquals(LlmModel.GEMINI_3_8_FLASH, LlmModel.forNewThread(LlmModel.GEMINI_3_8_FLASH))
    }
}
