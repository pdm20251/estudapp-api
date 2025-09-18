package com.estudoapp.domain.repositories

import com.estudoapp.domain.model.ChatMessage

interface ChatRepository {
    // Busca as últimas 'limit' mensagens de um chat
    suspend fun getLatestMessages(userId: String, limit: Int): List<ChatMessage>

    // Adiciona uma nova mensagem ao histórico do chat
    suspend fun addMessage(userId: String, message: ChatMessage)
}