package com.estudoapp.infrastructure.gateways.dto

import kotlinx.serialization.Serializable

// DTOs para a Resposta (ATUALIZADOS)
@Serializable
data class GeminiResponse(
    val candidates: List<Candidate> = emptyList(),
    val usageMetadata: UsageMetadata? = null,
    val modelVersion: String? = null,
    val responseId: String? = null // <-- CORREÇÃO FINAL
)

@Serializable
data class Candidate(
    val content: Content,
    val finishReason: String? = null,
    val safetyRatings: List<SafetyRating>? = null,
    val avgLogprobs: Double? = null
)

@Serializable
data class SafetyRating(
    val category: String,
    val probability: String
)

@Serializable
data class UsageMetadata(
    val promptTokenCount: Int,
    val candidatesTokenCount: Int,
    val totalTokenCount: Int,
    val promptTokensDetails: List<TokensDetail>? = null,
    val candidatesTokensDetails: List<TokensDetail>? = null
)

@Serializable
data class TokensDetail(
    val modality: String,
    val tokenCount: Int
)


// DTOs para a Requisição (sem novas mudanças)
@Serializable
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig,
    val safetySettings: List<SafetySetting>
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@Serializable data class Part(val text: String)

@Serializable
data class GenerationConfig(
    val responseMimeType: String,
    val temperature: Double = 0.7
)

@Serializable
data class SafetySetting(
    val category: String,
    val threshold: String
)