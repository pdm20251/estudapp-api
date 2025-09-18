package com.estudoapp.domain.repositories

import com.estudoapp.domain.model.Flashcard

interface FlashcardRepository {
    suspend fun findById(deckId: String, flashcardId: String): Flashcard?
    suspend fun create(deckId: String, userId: String, flashcard: Flashcard): Flashcard
    suspend fun findAllByDeckId(deckId: String, userId: String): List<Flashcard>
}