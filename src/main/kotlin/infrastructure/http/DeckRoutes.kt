package com.estudoapp.infrastructure.http

import com.estudoapp.domain.model.Deck
import com.estudoapp.domain.model.UserPrincipal
import com.estudoapp.domain.repositories.DeckRepository
import com.estudoapp.domain.usecases.CalculateSpacedRepetitionUseCase
import com.estudoapp.infrastructure.gateways.dto.GeminiService
import com.estudoapp.infrastructure.persistence.FireBaseDeckRepository
import com.estudoapp.infrastructure.persistence.FirebaseFlashcardRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.coroutines.launch

fun Route.deckRoutes() {

    val deckRepository: DeckRepository = FireBaseDeckRepository()

    // ... (as rotas /decks-test, /my-decks-auth-test, /my-decks continuam as mesmas) ...
    get("/decks-test") {
        call.respondText("O arquivo DeckRoutes.kt foi alcançado!")
    }

    authenticate("firebase-auth") {

        get("/my-decks-auth-test") {
            val principal = call.principal<UserPrincipal>()!!
            call.respondText("Autenticação bem-sucedida! Seu UID é: ${principal.uid}")
        }

        get("/my-decks") {
            val principal = call.principal<UserPrincipal>()!!
            val decks = deckRepository.findByUserId(principal.uid)
            call.respond(decks)
        }

        post("/my-decks") {
            val principal = call.principal<UserPrincipal>()
            val deckRequest = call.receive<Deck>()

            val createdDeck = deckRepository.create(deckRequest, principal?.uid ?: "unknown")
            call.respond(createdDeck)
        }

        /**
         * NOVO ENDPOINT: Calcula a próxima data de revisão para um deck.
         * Deve ser chamado pelo app cliente quando uma sessão de estudos termina.
         */
        post("/decks/{deckId}/calculate-next-review") {
            val principal = call.principal<UserPrincipal>()!!
            val deckId = call.parameters["deckId"] ?: return@post call.respond(HttpStatusCode.BadRequest, "ID do Deck não fornecido")

            // Instanciamos as dependências necessárias para o caso de uso.
            val flashcardRepository = FirebaseFlashcardRepository()
            val geminiService = GeminiService()
            val useCase = CalculateSpacedRepetitionUseCase(deckRepository, flashcardRepository, geminiService)

            // Usamos "fire-and-forget" para não bloquear a resposta ao usuário.
            call.launch {
                try {
                    useCase.execute(principal.uid, deckId)
                } catch (e: Exception) {
                    println("🔥🔥🔥 ERRO ao calcular a próxima revisão para o deck $deckId: ${e.message}")
                }
            }

            call.respond(HttpStatusCode.Accepted, mapOf("message" to "Cálculo da próxima revisão agendado com sucesso."))
        }
    }
}
