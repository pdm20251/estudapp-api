package com.estudoapp.domain.repositories

import com.estudoapp.domain.model.Deck

interface DeckRepository {
    suspend fun findByUserId(userId: String): List<Deck>
    suspend fun create(deck: Deck, userId: String): Deck
    suspend fun findById(deckId: String, userId: String): Deck?
    // ADICIONADO: A assinatura da função de atualização que faltava.
    suspend fun update(deckId: String, userId: String, updates: Map<String, Any?>)
}