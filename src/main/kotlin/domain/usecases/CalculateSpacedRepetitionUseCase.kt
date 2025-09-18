package com.estudoapp.domain.usecases

import com.estudoapp.domain.repositories.DeckRepository
import com.estudoapp.domain.repositories.FlashcardRepository
import com.estudoapp.infrastructure.gateways.dto.GeminiService
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CalculateSpacedRepetitionUseCase(
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
    private val geminiService: GeminiService
) {

    suspend fun execute(userId: String, deckId: String) {
        // 1. Buscar o deck e os flashcards associados para obter o histórico.
        val deck = deckRepository.findById(deckId, userId)
            ?: throw Exception("Deck não encontrado.")
        val flashcards = flashcardRepository.findAllByDeckId(deckId, userId)

        if (flashcards.isEmpty()) {
            println("[RECOMENDAÇÃO] O deck '${deck.name}' não possui flashcards. Nenhuma data de revisão foi calculada.")
            return
        }

        // 2. Criar um resumo simples da performance do usuário.
        val performanceSummary = flashcards.joinToString("\n") {
            "- Card ID ${it.id?.take(5)}: ${it.repeticoes ?: 0} revisões, fator de facilidade ${it.fatorFacilidade ?: 2.5}"
        }

        // 3. Montar o prompt para a LLM.
        val prompt = buildPrompt(deck.name, performanceSummary)
        println("[RECOMENDAÇÃO] Prompt enviado para a IA:\n$prompt")

        // 4. Chamar a IA para obter a data.
        val response = geminiService.calculateNextReviewDate(prompt)

        // 5. Converter a data "YYYY-MM-DD" para um timestamp e salvar no deck.
        val nextReviewDate = LocalDate.parse(response.proximaDataRevisao, DateTimeFormatter.ISO_LOCAL_DATE)
        val nextReviewTimestamp = nextReviewDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val updates = mapOf("proximaRevisaoTimestamp" to nextReviewTimestamp)
        deckRepository.update(deckId, userId, updates)

        println("[RECOMENDAÇÃO] Próxima revisão para o deck '${deck.name}' definida para ${response.proximaDataRevisao} ($nextReviewTimestamp)")
    }

    private fun buildPrompt(deckName: String, performanceSummary: String): String {
        val hoje = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        return """
            Você é um especialista em algoritmos de repetição espaçada (Spaced Repetition System - SRS).
            Sua tarefa é calcular a data ideal para a próxima sessão de revisão de um baralho de estudos (deck).

            **Contexto:**
            - **Data de Hoje:** $hoje
            - **Nome do Deck:** "$deckName"
            - **Histórico de Performance do Usuário nos Cards:**
            $performanceSummary

            **Tarefa:**
            Com base no histórico e nos princípios de repetição espaçada (revisar mais tarde o que é fácil, revisar mais cedo o que é difícil),
            calcule a data ideal para a próxima revisão deste deck. A data deve ser no futuro.

            **Formato da Resposta:**
            Responda APENAS com um objeto JSON válido, sem nenhum texto adicional ou markdown.
            A estrutura do JSON deve ser a seguinte:
            {
              "proximaDataRevisao": "YYYY-MM-DD"
            }
        """.trimIndent()
    }
}
