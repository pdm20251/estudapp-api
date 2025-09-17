package com.estudoapp.infrastructure.gateways.dto

import com.estudoapp.infrastructure.http.dto.ValidateAnswerResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json


class GeminiService() {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val apiKey = System.getenv("GEMINI_API_KEY");

    suspend fun validateAnswerWithIA(prompt: String): ValidateAnswerResponse {

        val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val requestBody = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

            val response: GeminiResponse = client.post(apiUrl) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()

            val rawText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Resposta da IA em formato inesperado ou vazia.")

            val cleanedJsonText = rawText
                .replace("```json", "")
                .replace("```", "")
                .trim()

        return Json.decodeFromString<ValidateAnswerResponse>(cleanedJsonText)
    }
}