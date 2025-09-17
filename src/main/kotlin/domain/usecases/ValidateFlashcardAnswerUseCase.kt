package com.estudoapp.domain.usecases

import com.estudoapp.domain.ClozeFlashcard
import com.estudoapp.domain.DigiteRespostaFlashcard
import com.estudoapp.domain.FrenteVersoFlashcard
import com.estudoapp.domain.MultiplaEscolhaFlashcard
import com.estudoapp.domain.repositories.FlashcardRepository
import com.estudoapp.infrastructure.gateways.dto.GeminiService
import com.estudoapp.infrastructure.http.dto.ValidateAnswerResponse

class ValidateFlashcardAnswerUseCase(
    private val flashcardRepository: FlashcardRepository,
    private val geminiService: GeminiService
) {
    suspend fun execute(deckId: String, flashcardId: String, userAnswer: String): ValidateAnswerResponse {
        val flashcard = flashcardRepository.findById(deckId, flashcardId)
            ?: throw Exception("Flashcard não encontrado")

        return when (flashcard) {
            is DigiteRespostaFlashcard -> {

                val pergunta = flashcard.pergunta?: "Pergunta não encontrada"
                val respostasValidas = (flashcard.respostasValidas ?: emptyList()).joinToString(", ")

                val prompt = """
                    Você é um professor corrigindo a resposta de um flashcard.
                    A pergunta é: "$pergunta"
                    As respostas consideradas 100% corretas são: "$respostasValidas".
                    A resposta do aluno foi: "$userAnswer".

                    Analise se a resposta do aluno é semanticamente equivalente a uma das respostas corretas...
                    
                    Responda APENAS com um objeto JSON com a seguinte estrutura:
                    { "isCorrect": boolean, "score": um inteiro de 0 a 100, "explanation": "uma breve explicação" }
                """.trimIndent()

                try {
                    geminiService.validateAnswerWithIA(prompt)
                } catch (e: Exception) {
                    throw Exception("Erro ao validar a resposta com IA: ${e.message}", e)
                }
            }

            is ClozeFlashcard, is FrenteVersoFlashcard, is MultiplaEscolhaFlashcard -> {
                throw UnsupportedOperationException("Validação por IA não suportada para o tipo ${flashcard.type}")
            }
        }
    }
}