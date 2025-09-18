package com.estudoapp.infrastructure.gateways.dto

import com.estudoapp.domain.model.Flashcard
import com.estudoapp.infrastructure.http.dto.ValidateAnswerResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json


class GeminiService() {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                classDiscriminator = "type" // Certifique-se que está igual ao Serialization.kt
            })
        }
    }

    private val apiKey = System.getenv("GEMINI_API_KEY");
    private val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"


    private suspend fun callGeminiApi(prompt: String): String {
        val safetySettings = listOf(
            SafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_NONE"),
            SafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_NONE"),
            SafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_NONE"),
            SafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_NONE")
        )
        val generationConfig = GenerationConfig(responseMimeType = "application/json")

        val requestBody = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            safetySettings = safetySettings,
            generationConfig = generationConfig
        )

        // MUDANÇA: Captura a resposta HTTP completa para depuração
        val httpResponse: HttpResponse = client.post(apiUrl) {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        val rawBody = httpResponse.bodyAsText()
        // LOG DE DEPURAÇÃO: Imprime a resposta crua da IA no console
        println("[GEMINI DEBUG] Raw Response Body: $rawBody")

        if (!httpResponse.status.isSuccess()) {
            throw Exception("A chamada à API Gemini falhou com status ${httpResponse.status}: $rawBody")
        }

        // Agora, desserializamos a string que já temos
        val response = Json.decodeFromString<GeminiResponse>(rawBody)

        val rawText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("Resposta da IA em formato inesperado ou vazia. (Verifique o log 'Raw Response Body' para detalhes)")

        return rawText.trim()
    }

    suspend fun validateAnswerWithIA(prompt: String): ValidateAnswerResponse {
        val cleanedJsonText = callGeminiApi(prompt)
        return Json.decodeFromString<ValidateAnswerResponse>(cleanedJsonText)
    }

    suspend fun generateFlashcardWithIA(prompt: String): Flashcard {
        val cleanedJsonText = callGeminiApi(prompt)
        return Json.decodeFromString<Flashcard>(cleanedJsonText)
    }
}
