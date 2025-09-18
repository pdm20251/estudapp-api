package com.estudoapp.domain.usecases

import com.estudoapp.domain.model.ChatMessage
import com.estudoapp.domain.repositories.ChatRepository
import com.estudoapp.infrastructure.gateways.dto.GroqMessage
import com.estudoapp.infrastructure.gateways.dto.GroqService

class ProcessChatResponseUseCase(
    private val chatRepository: ChatRepository,
    private val groqService: GroqService // Injetando o novo serviço
) {
    suspend fun execute(userId: String, userMessage: ChatMessage) {
        try {
            // 1. Salva a mensagem do usuário que acabamos de receber
            chatRepository.addMessage(userId, userMessage)

            // 2. Busca o histórico recente para dar contexto à IA
            val history = chatRepository.getLatestMessages(userId, 10)

            // 3. Monta a lista de mensagens no formato que a API do Groq espera
            val groqMessages = buildPromptMessages(history)

            // 4. Roda o prompt na LLM usando o GroqService
            val llmTextResponse = groqService.generateChatResponse(groqMessages)

            // 5. Armazena a resposta da IA no Firebase
            val llmMessage = ChatMessage(
                sender = "LLM",
                text = llmTextResponse
            )
            chatRepository.addMessage(userId, llmMessage)

            println("[CHAT BACKGROUND] Resposta do Groq salva com sucesso no Firebase.")

        } catch (e: Exception) {
            println("🔥🔥🔥 ERRO no processamento de chat em segundo plano: ${e.message}")
            // Opcional: Salvar uma mensagem de erro no chat para o usuário
            val errorMessage = ChatMessage(sender = "LLM", text = "Desculpe, ocorreu um erro ao processar sua pergunta.")
            chatRepository.addMessage(userId, errorMessage)
        }
    }

    private fun buildPromptMessages(history: List<ChatMessage>): List<GroqMessage> {
        val messages = mutableListOf<GroqMessage>()

        // Adiciona a instrução do sistema (persona do tutor)
        messages.add(
            GroqMessage(
                role = "system",
                content = """
                Você é um tutor pessoal especializado e amigável chamado Estudy. 
                Sua principal função é ajudar os alunos a entenderem o conteúdo, não apenas dar as respostas.
                Responda de forma educativa, conecte com tópicos já estudados se possível, 
                mantenha um tom encorajador e sugira os próximos passos ou flashcards relacionados quando for relevante.
                """.trimIndent()
            )
        )

        // Converte o histórico do Firebase para o formato do Groq
        history.forEach { chatMessage ->
            val role = if (chatMessage.sender == "USER") "user" else "assistant"
            messages.add(GroqMessage(role = role, content = chatMessage.text))
        }

        return messages
    }
}

