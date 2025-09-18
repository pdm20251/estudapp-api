package com.estudoapp.infrastructure.gateways.dto

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class GroqService {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                // CORREÇÃO: Força a inclusão de valores padrão no JSON enviado
                encodeDefaults = true
            })
        }
    }

    private val apiKey = System.getenv("GROQ_API_KEY")
    private val apiUrl = "https://api.groq.com/openai/v1/chat/completions"

    suspend fun generateChatResponse(messages: List<GroqMessage>): String {
        if (apiKey.isNullOrEmpty()) {
            throw Exception("A chave da API do Groq (GROQ_API_KEY) não foi configurada.")
        }

        val requestBody = GroqRequest(messages = messages)

        try {
            // Captura a resposta HTTP completa para análise
            val httpResponse: HttpResponse = client.post(apiUrl) {
                contentType(ContentType.Application.Json)
                bearerAuth(apiKey)
                setBody(requestBody)
            }

            val rawBody = httpResponse.bodyAsText()
            // LOG DE DEPURAÇÃO: Imprime a resposta crua do Groq no console
            println("[GROQ DEBUG] Raw Response Body: $rawBody")

            // Verifica se a chamada foi bem-sucedida
            if (!httpResponse.status.isSuccess()) {
                throw Exception("A chamada à API Groq falhou com status ${httpResponse.status}: $rawBody")
            }

            // Desserializa a resposta que já temos como texto
            val response = Json.decodeFromString<GroqResponse>(rawBody)

            return response.choices.firstOrNull()?.message?.content
                ?: "Desculpe, não consegui gerar uma resposta no momento. (Verifique o log 'Raw Response Body' para detalhes)"

        } catch (e: Exception) {
            println("🔥🔥🔥 ERRO ao chamar a API do Groq: ${e.message}")
            // Retorna a mensagem de erro da exceção para ser salva no Firebase
            return "Erro ao processar a resposta da IA: ${e.message}"
        }
    }
}

