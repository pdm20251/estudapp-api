package com.estudoapp.infrastructure.http

import com.estudoapp.domain.model.ClozeFlashcard
import com.estudoapp.domain.model.Deck
import com.estudoapp.domain.model.DigiteRespostaFlashcard
import com.estudoapp.domain.model.FrenteVersoFlashcard
import com.estudoapp.domain.model.MultiplaEscolhaFlashcard
import com.estudoapp.domain.model.UserPrincipal
import com.estudoapp.domain.repositories.DeckRepository
import com.estudoapp.domain.repositories.FlashcardRepository
import com.estudoapp.infrastructure.persistence.FireBaseDeckRepository
import com.estudoapp.infrastructure.persistence.FirebaseFlashcardRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.util.UUID

fun Route.deckRoutes() {

    val deckRepository: DeckRepository = FireBaseDeckRepository()
    val flashcardRepository: FlashcardRepository = FirebaseFlashcardRepository()

    get("/decks-test") {
        call.respondText("O arquivo DeckRoutes.kt foi alcançado!")
    }

    get("/share-deck/{deckId}/{userId}") {
        val deckId = call.parameters["deckId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "ID do Deck não fornecido")
        val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "ID do Usuário não fornecido")

        val deck = deckRepository.findById(deckId, userId);

        if (deck != null) {
            call.respond(HttpStatusCode.OK, deck)
        } else {
            call.respond(HttpStatusCode.NotFound, "Deck não encontrado ou não pertence a este usuário")
        }
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

        post("/share-deck/{deckId}/{userId}") {
            // 1. Extrair os parâmetros da URL
            val originalDeckId = call.parameters["deckId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "ID do Deck a ser copiado não fornecido")

            val originalOwnerId = call.parameters["userId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "ID do dono original não fornecido")

            // 2. Obter o ID do usuário que está fazendo a requisição (autenticado)
            val principal = call.principal<UserPrincipal>()!!
            val authenticatedUserId = principal.uid

            if(authenticatedUserId.isEmpty()) {
                return@post call.respond(HttpStatusCode.Unauthorized, "Não foi possível identificar o usuário")
            }
            // 3. Validações
            if (originalOwnerId == authenticatedUserId) {
                return@post call.respond(HttpStatusCode.Conflict, "Não é possível copiar o próprio deck.")
            }

            val originalDeck = deckRepository.findById(originalDeckId, originalOwnerId)
                ?: return@post call.respond(HttpStatusCode.NotFound, "O deck que você está tentando copiar não existe.")

            val originalFlashcards = flashcardRepository.findAllByDeckId(originalDeckId, originalOwnerId)

            val newDeck = originalDeck.copy(
                id = UUID.randomUUID().toString(),
                userId = authenticatedUserId
            )
            deckRepository.create(newDeck, authenticatedUserId)

            val newFlashcards = originalFlashcards.map { originalFlashcard ->

                val newFlashcardId = UUID.randomUUID().toString()

                when (originalFlashcard) {
                    is FrenteVersoFlashcard -> originalFlashcard.copy(
                        id = newFlashcardId,
                        deckId = newDeck.id,
                        userId = authenticatedUserId
                    )
                    is ClozeFlashcard -> originalFlashcard.copy(
                        id = newFlashcardId,
                        deckId = newDeck.id,
                        userId = authenticatedUserId
                    )
                    is DigiteRespostaFlashcard -> originalFlashcard.copy(
                        id = newFlashcardId,
                        deckId = newDeck.id,
                        userId = authenticatedUserId
                    )
                    is MultiplaEscolhaFlashcard -> originalFlashcard.copy(
                        id = newFlashcardId,
                        deckId = newDeck.id,
                        userId = authenticatedUserId
                    )
                }
            }

            if (newFlashcards.isNotEmpty()) {
                flashcardRepository.saveAll(newFlashcards)
            }

            call.respond(HttpStatusCode.Created, newDeck)
        }
    }
}