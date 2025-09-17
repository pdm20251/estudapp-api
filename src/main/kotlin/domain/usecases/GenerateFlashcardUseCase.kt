package com.estudoapp.domain.usecases

import com.estudoapp.domain.Alternativa
import com.estudoapp.domain.ClozeFlashcard
import com.estudoapp.domain.DigiteRespostaFlashcard
import com.estudoapp.domain.Flashcard
import com.estudoapp.domain.FrenteVersoFlashcard
import com.estudoapp.domain.MultiplaEscolhaFlashcard
import com.estudoapp.domain.repositories.DeckRepository
import com.estudoapp.domain.repositories.FlashcardRepository
import java.util.UUID

class GenerateFlashcardUseCase(
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository
) {
    suspend fun execute(deckId: String, userId: String, requestedType: String, userComment: String?): Flashcard {

        val deck = deckRepository.findById(deckId, userId)
            ?: throw Exception("Deck com id '$deckId' não foi encontrado ou não pertence a este usuário.")

        val existingFlashcards = flashcardRepository.findAllByDeckId(deckId, userId)

        val existingCardsContext = if (existingFlashcards.isNotEmpty()) {
            "Para evitar repetição, aqui estão alguns flashcards já existentes neste deck:\n" +
                    existingFlashcards.take(5).joinToString("\n") { card -> // Pega até 5 exemplos
                        when (card) {
                            is FrenteVersoFlashcard -> "- Pergunta: '${card.frente}'"
                            is DigiteRespostaFlashcard -> "- Pergunta: '${card.pergunta}'"
                            is MultiplaEscolhaFlashcard -> "- Pergunta: '${card.pergunta}'"
                            is ClozeFlashcard -> "- Texto: '${card.textoComLacunas}'"
                        }
                    }
        } else {
            "Este é o primeiro flashcard a ser criado neste deck."
        }


        println("[API LOG] Requisição para gerar flashcard do tipo '$requestedType' para o deck '${deck.name}'.")
        println("[API LOG] Contexto dos Cards: $existingCardsContext")
        println("[API LOG] Comentário do usuário: '${userComment ?: "Nenhum"}'")

        val newId = UUID.randomUUID().toString()
        return when (requestedType.uppercase()) {
            "FRENTE_VERSO" -> FrenteVersoFlashcard(
                id = newId,
                deckId = deckId,
                userId = userId,
                frente = "Frente (Gerado pela API)",
                verso = "Verso (Baseado no comentário: ${userComment ?: "genérico"})"
            )

            "MULTIPLA_ESCOLHA" -> MultiplaEscolhaFlashcard(
                id = newId,
                deckId = deckId,
                userId = userId,
                pergunta = "Qual a pergunta? (Gerado pela API)",
                alternativas = listOf(
                    Alternativa(text = "Alternativa A"),
                    Alternativa(text = "Alternativa B (Correta)"),
                    Alternativa(text = "Alternativa C")
                ),
                respostaCorreta = "Alternativa B (Correta)"
            )


            else -> throw IllegalArgumentException("Tipo de flashcard desconhecido ou não suportado para geração: $requestedType")
        }
    }
}