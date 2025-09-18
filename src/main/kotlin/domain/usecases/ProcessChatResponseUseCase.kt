package com.estudoapp.domain.usecases

import com.estudoapp.domain.model.ChatMessage
import com.estudoapp.domain.repositories.ChatRepository

class ProcessChatResponseUseCase(
    private val chatRepository: ChatRepository
    // Futuramente: private val geminiService: GeminiService
) {
    suspend fun execute(userId: String, userMessage: ChatMessage) {
        try {
            // 1. A primeira coisa é salvar a mensagem do usuário que recebemos
            chatRepository.addMessage(userId, userMessage)

            // 2. Acessa o firebase e extrai as últimas N mensagens
            val history = chatRepository.getLatestMessages(userId, 10) // Pega as últimas 10

            // 3. Monta o prompt (simulado por enquanto)
            val context = history.joinToString("\n") { "${it.sender}: ${it.text}" }
            val prompt = "Contexto da conversa:\n$context\n\nResponda a última mensagem."

            println("[CHAT BACKGROUND] Prompt que seria enviado para a IA:\n$prompt")

            // 4. Rodar o prompt na LLM (MOCK)
            // val llmTextResponse = geminiService.generateChatResponse(prompt)
            val llmTextResponse = "Esta é uma resposta automática da API. A IA está desativada."

            // 5. Armazenar no firebase a resposta da LLM
            val llmMessage = ChatMessage(
                sender = "LLM",
                text = llmTextResponse
            )
            chatRepository.addMessage(userId, llmMessage)

            println("[CHAT BACKGROUND] Resposta da LLM salva com sucesso no Firebase.")

        } catch (e: Exception) {
            println("🔥🔥🔥 ERRO no processamento de chat em segundo plano: ${e.message}")
        }
    }

    suspend fun getChatMessages(userId: String, limit: Int){
        try {
            chatRepository.getLatestMessages(userId, limit)
        } catch (e: Exception) {
            println("🔥🔥🔥 ERRO ao obter mensagens de chat em segundo plano: ${e.message}")
        }
    }
}