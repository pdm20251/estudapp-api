package com.estudoapp.domain.usecases

import com.estudoapp.domain.model.ClozeFlashcard
import com.estudoapp.domain.model.DigiteRespostaFlashcard
import com.estudoapp.domain.model.Flashcard
import com.estudoapp.domain.model.FrenteVersoFlashcard
import com.estudoapp.domain.model.MultiplaEscolhaFlashcard
import com.estudoapp.domain.repositories.DeckRepository
import com.estudoapp.domain.repositories.FlashcardRepository
import com.estudoapp.infrastructure.gateways.dto.GeminiService

class GenerateFlashcardUseCase(
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
    private val geminiService: GeminiService // Injeção do serviço da IA
) {

    suspend fun execute(deckId: String, userId: String, requestedType: String, userComment: String?): Flashcard {
        // 1. Acessa o firebase e extrai as informações do deck
        val deck = deckRepository.findById(deckId, userId)
            ?: throw Exception("Deck com id '$deckId' não foi encontrado ou não pertence a este usuário.")

        // 2. Busca flashcards existentes para dar contexto à IA
        val existingFlashcards = flashcardRepository.findAllByDeckId(deckId, userId)

        val existingCardsContext = if (existingFlashcards.isNotEmpty()) {
            "Para evitar repetição, aqui estão os 5 flashcards mais recentes já existentes neste deck:\n" +
                    existingFlashcards.take(5).joinToString("\n") { card ->
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

        // 3. Monta o prompt para a LLM
        val prompt = buildPrompt(deck.name, deck.description, existingCardsContext, requestedType, userComment)

        println("[API LOG] Prompt enviado para a IA para geração do flashcard:\n$prompt")

        // 4. Roda o prompt na LLM
        val generatedFlashcard = geminiService.generateFlashcardWithIA(prompt)

        // 5. Retorna o flashcard gerado pela IA (sem salvar no banco ainda)
        // A camada de rota será responsável por chamar o repositório para salvar.
        return generatedFlashcard
    }

    private fun buildPrompt(
        deckName: String,
        deckDescription: String,
        existingCardsContext: String,
        requestedType: String,
        userComment: String?
    ): String {
        val comment = userComment ?: "um tema geral relacionado ao deck."
        val (tipo, estruturaJson) = getEstruturaPorTipo(requestedType)

        return """
            Você é um assistente educacional especialista em criar flashcards.
            Sua tarefa é criar um novo flashcard para um deck de estudos.

            **Contexto do Deck:**
            - **Nome:** "$deckName"
            - **Descrição:** "$deckDescription"
            - **Flashcards Existentes:** $existingCardsContext

            **Tarefa:**
            Crie um flashcard do tipo **"$tipo"** sobre **"$comment"**.
            O flashcard deve ser relevante ao contexto do deck, mas original e não uma cópia dos existentes.

            **Formato da Resposta:**
            Responda APENAS com um objeto JSON válido, sem nenhum texto adicional ou markdown.
            A estrutura do JSON deve ser a seguinte:
            $estruturaJson
        """.trimIndent()
    }

    private fun getEstruturaPorTipo(type: String): Pair<String, String> {
        return when (type.uppercase()) {
            "FRENTE_VERSO" -> Pair(
                "Frente e Verso",
                """
                {
                  "type": "FRENTE_VERSO",
                  "frente": "Texto da pergunta ou conceito...",
                  "verso": "Texto da resposta..."
                }
                """
            )
            "MULTIPLA_ESCOLHA" -> Pair(
                "Múltipla Escolha",
                """
                {
                  "type": "MULTIPLA_ESCOLHA",
                  "pergunta": "Texto da pergunta...",
                  "alternativas": [
                    { "text": "Texto da alternativa A", "isCorrect": false },
                    { "text": "Texto da alternativa B", "isCorrect": true },
                    { "text": "Texto da alternativa C", "isCorrect": false }
                  ],
                  "respostaCorreta": "Texto da alternativa B"
                }
                """
            )
            "CLOZE" -> Pair(
                "Omisso (Cloze)",
                """
                {
                  "type": "CLOZE",
                  "textoComLacunas": "O Sol é uma {{c1::estrela}} no centro do {{c2::Sistema Solar}}.",
                  "respostasCloze": {
                    "c1": "estrela",
                    "c2": "Sistema Solar"
                  }
                }
                """
            )
            "DIGITE_RESPOSTA" -> Pair(
                "Digite a Resposta",
                """
                {
                  "type": "DIGITE_RESPOSTA",
                  "pergunta": "Qual a capital do Brasil?",
                  "respostasValidas": ["Brasília", "Brasilia"]
                }
                """
            )
            else -> throw IllegalArgumentException("Tipo de flashcard desconhecido: $type")
        }
    }
}