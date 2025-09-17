package com.estudoapp.infrastructure.http.dto

import kotlinx.serialization.Serializable

@Serializable
data class ValidateAnswerRequest(
    val deckId: String,
    val flashcardId: String,
    val userAnswer: String
)

@Serializable
data class ValidateAnswerResponse(
    val isCorrect: Boolean,
    val score: Int,
    val explanation: String
)

@Serializable
data class GenerateFlashcardRequest(
    val type: String,
    val userComment: String? = null
)