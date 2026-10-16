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

class OpenAiProviderImplTest {
    private val converter = OpenAiToolConverter()
    private fun tool(pkg: String = "com.feemag.shop") = AppFunctionMetadata(
        id = "Shop#searchProducts", packageName = pkg, isEnabled = true, schema = null,
        parameters = listOf(AppFunctionParameterMetadata("query", true,
            AppFunctionStringTypeMetadata(false), "Search query"),
            AppFunctionParameterMetadata("maxPriceMinor", false,
                AppFunctionLongTypeMetadata(true), "Optional limit")),
        response = AppFunctionResponseMetadata(AppFunctionStringTypeMetadata(false), "Products"),
        components = AppFunctionComponentsMetadata(emptyMap()), description = "Search products", deprecation = null,
    )

    @Test fun schemaPreservesOptionalNullableParametersAndUniqueNames() {
        val schema = converter.convert(tool())
        assertEquals(false, schema.getValue("strict").jsonPrimitive.boolean)
        val parameters = schema.getValue("parameters").jsonObject
        assertEquals(listOf("query"), parameters.getValue("required").jsonArray.map { it.jsonPrimitive.content })
        assertTrue(parameters.getValue("properties").jsonObject.getValue("maxPriceMinor").jsonObject.containsKey("anyOf"))
        assertNotEquals(converter.getToolName(tool()), converter.getToolName(tool("other.app")))
        assertTrue(converter.getToolName(tool()).length <= 64)
    }

    @Test fun toolCallRoundTripUsesResponseAndCallIds() = runBlocking {
        val requests = mutableListOf<JsonObject>()
        val client = HttpClient(MockEngine { request ->
            requests += Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            val output = if (requests.size == 1)
                """{"id":"resp_1","status":"completed","output":[{"type":"function_call","name":"${converter.getToolName(tool())}","arguments":"{\"query\":\"headphones\"}","call_id":"call_1"}]}"""
            else """{"id":"resp_2","status":"completed","output":[{"type":"message","content":[{"type":"output_text","text":"Headphones cost 79.90."}]}]}"""
            respond(output, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            val provider = OpenAiProviderImpl(client, converter)
            val first = provider.generateResponse(null, LlmInput.UserMessage("Find headphones"), listOf(tool()), "test", "gpt-5.4-mini", ServiceTier.STANDARD) as LlmResponse.Success
            val call = first.parts.single() as LlmResponsePart.ToolCall
            assertEquals("com.feemag.shop", call.packageName)
            assertEquals("headphones", call.arguments["query"])
            val second = provider.generateResponse(first.interactionId, LlmInput.ToolResponse(listOf(ToolOutput(call.functionId, call.callId, "{\"priceMinor\":7990}"))), listOf(tool()), "test", "gpt-5.4-mini", ServiceTier.STANDARD)
            assertTrue(second is LlmResponse.Success)
            assertEquals("resp_1", requests[1].getValue("previous_response_id").jsonPrimitive.content)
            val output = requests[1].getValue("input").jsonArray.single().jsonObject
            assertEquals("call_1", output.getValue("call_id").jsonPrimitive.content)
            assertEquals("function_call_output", output.getValue("type").jsonPrimitive.content)
            assertFalse(requests[0].containsKey("service_tier"))
        } finally { client.close() }
    }

    @Test fun rejectsUnknownFunctionsAndIncompleteResponses() = runBlocking {
        for (body in listOf(
            """{"id":"r","status":"completed","output":[{"type":"function_call","name":"unadvertised","arguments":"{}","call_id":"c"}]}""",
            """{"id":"r","status":"incomplete","output":[]}""",
            "not json",
        )) {
            val client = HttpClient(MockEngine { respond(body) })
            try {
                assertTrue(OpenAiProviderImpl(client, converter).generateResponse(null, LlmInput.UserMessage("test"), listOf(tool()), "test", "gpt-5.4-mini", ServiceTier.STANDARD) is LlmResponse.Error)
            } finally { client.close() }
        }
    }
}
