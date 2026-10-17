/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.appfunctions.agent.data

/** Enum representing the supported LLM providers. */
enum class LlmProviderName {
    GEMINI,
    OPENAI,
    GROQ,
}

/**
 * Enum representing the supported LLM models. Threads persist the constant name, so retired
 * constants stay (see [LEGACY]) and point at a model that is still served.
 */
enum class LlmModel private constructor(val providerName: LlmProviderName, val modelName: String) {
    // Groq (free plan available). Model IDs checked against Groq's deprecations page, October 2026.
    GROQ_GPT_OSS_120B(LlmProviderName.GROQ, "openai/gpt-oss-120b"),
    GROQ_GPT_OSS_20B(LlmProviderName.GROQ, "openai/gpt-oss-20b"),
    GROQ_QWEN_3_8_27B(LlmProviderName.GROQ, "qwen/qwen3.8-27b"),

    // OpenAI current flagships.
    OPENAI_GPT_6_LUNA(LlmProviderName.OPENAI, "gpt-6-luna"),
    OPENAI_GPT_6_1_SOL(LlmProviderName.OPENAI, "gpt-6.1-sol"),

    // Gemini stable (GA) models: consistent latency and production rate limits. Prefer these.
    GEMINI_3_8_FLASH(LlmProviderName.GEMINI, "gemini-3.8-flash"),
    GEMINI_3_5_FLASH_LITE(LlmProviderName.GEMINI, "gemini-3.5-flash-lite"),
    GEMINI_4_ARGON(LlmProviderName.GEMINI, "gemini-4-argon"),

    // Gemini preview: newer capabilities but restrictive rate limits and no latency SLA.
    GEMINI_3_1_PRO_PREVIEW(LlmProviderName.GEMINI, "gemini-3.1-pro-preview"),

    // Legacy: kept so saved threads still load; hidden from the model picker.
    OPENAI_GPT_5_4_MINI(LlmProviderName.OPENAI, "gpt-5.4-mini"),
    GEMINI_3_5_FLASH(LlmProviderName.GEMINI, "gemini-3.5-flash"),
    GEMINI_3_1_FLASH_LITE(LlmProviderName.GEMINI, "gemini-3.1-flash-lite"),
    GEMINI_3_FLASH_PREVIEW(LlmProviderName.GEMINI, "gemini-3-flash-preview"),
    // gemini-3.1-flash-lite-preview passed its shutdown date; route to its stable successor.
    GEMINI_3_1_FLASH_LITE_PREVIEW(LlmProviderName.GEMINI, "gemini-3.1-flash-lite"),
    ;

    companion object {
        val DEFAULT = GROQ_GPT_OSS_120B

        /** Models offered for new selections, in picker order. */
        val SELECTABLE =
            listOf(
                GROQ_GPT_OSS_120B,
                GROQ_GPT_OSS_20B,
                GROQ_QWEN_3_8_27B,
                OPENAI_GPT_6_LUNA,
                OPENAI_GPT_6_1_SOL,
                GEMINI_3_8_FLASH,
                GEMINI_3_5_FLASH_LITE,
                GEMINI_4_ARGON,
                GEMINI_3_1_PRO_PREVIEW,
            )

        val LEGACY = entries - SELECTABLE.toSet()

        /** A new thread keeps the current model unless that model is no longer offered. */
        fun forNewThread(current: LlmModel): LlmModel = if (current in SELECTABLE) current else DEFAULT
    }
}
