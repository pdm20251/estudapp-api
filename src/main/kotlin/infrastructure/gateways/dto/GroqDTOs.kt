package com.estudoapp.infrastructure.gateways.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// --- Requisição para a API do Groq ---

@Serializable
data class GroqRequest(
    val messages: List<GroqMessage>,
    // CORREÇÃO: Atualizado para o modelo mais recente que você encontrou.
    val model: String = "llama-3.3-70b-versatile" //Testando com: llama-3.1-8b-instant (Ruim), openai/gpt-oss-120b (Bom) ou llama-3.3-70b-versatile (Bom)
)

@Serializable
data class GroqMessage(
    val role: String, // "system", "user", ou "assistant"
    val content: String
)


// --- Resposta da API do Groq (ATUALIZADA PARA REFLETIR A RESPOSTA COMPLETA) ---

@Serializable
data class GroqResponse(
    val id: String? = null,
    val `object`: String? = null,
    val created: Long? = 0,
    val model: String? = null,
    val choices: List<GroqChoice> = emptyList(),
    val usage: GroqUsage? = null,
    val system_fingerprint: String? = null,
    val x_groq: GroqExtendedInfo? = null,
    val usage_breakdown: JsonElement? = null,
    // Adicionado para mapear o campo final da resposta
    val service_tier: String? = null
)

@Serializable
data class GroqChoice(
    val index: Int? = 0,
    val message: GroqMessage,
    // Adicionado para lidar com o campo 'logprobs' que pode ser nulo ou um objeto.
    val logprobs: JsonElement? = null,
    val finish_reason: String? = null
)

@Serializable
data class GroqUsage(
    val prompt_tokens: Int? = 0,
    val completion_tokens: Int? = 0,
    val total_tokens: Int? = 0,
    val queue_time: Double? = 0.0,
    val prompt_time: Double? = 0.0,
    val completion_time: Double? = 0.0,
    val total_time: Double? = 0.0
)

// Classe para o objeto aninhado 'x_groq'
@Serializable
data class GroqExtendedInfo(
    val id: String? = null
)

