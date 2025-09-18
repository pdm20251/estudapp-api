package com.estudoapp.infrastructure.http

import com.estudoapp.domain.model.UserPrincipal
import com.estudoapp.domain.repositories.DeckRepository
import com.estudoapp.domain.usecases.GenerateFlashcardUseCase
import com.estudoapp.domain.usecases.ValidateFlashcardAnswerUseCase
import com.estudoapp.infrastructure.gateways.dto.GeminiService
import com.estudoapp.infrastructure.http.dto.GenerateFlashcardRequest
import com.estudoapp.infrastructure.http.dto.ValidateAnswerRequest
import com.estudoapp.infrastructure.persistence.FireBaseDeckRepository
import com.estudoapp.infrastructure.persistence.FirebaseFlashcardRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.intercept
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.flashcardRoutes() {

    val flashcardRepository = FirebaseFlashcardRepository()
    val geminiService = GeminiService() // Instanciado aqui
    val deckRepository: DeckRepository = FireBaseDeckRepository()

    val validateUseCase = ValidateFlashcardAnswerUseCase(flashcardRepository, geminiService)
    // Injetando o geminiService no construtor
    val generateUseCase = GenerateFlashcardUseCase(deckRepository, flashcardRepository, geminiService)

    authenticate("firebase-auth") {
        post("/flashcards/validate") {
            val request = call.receive<ValidateAnswerRequest>()
            val response = validateUseCase.execute(
                deckId = request.deckId,
                flashcardId = request.flashcardId,
                userAnswer = request.userAnswer
            )
            call.respond(response)
        }

        route("/decks/{deckId}/flashcards") {
            intercept(ApplicationCallPipeline.Call) {
                val principal = call.principal<UserPrincipal>()!!
                val deckId = call.parameters["deckId"] ?: return@intercept call.respond(HttpStatusCode.BadRequest)

                val deck = deckRepository.findById(deckId, principal.uid)
                if (deck == null) {
                    return@intercept call.respond(HttpStatusCode.Forbidden, "Este deck não pertence a você.")
                }
            }


            post("/generate") {
                val principal = call.principal<UserPrincipal>()!!
                val deckId = call.parameters["deckId"] ?: return@post call.respond(HttpStatusCode.BadRequest, "ID do Deck não fornecido")
                val request = call.receive<GenerateFlashcardRequest>()


                try {
                    // 1. Gera o flashcard usando a IA
                    val generatedFlashcard = generateUseCase.execute(
                        deckId = deckId,
                        userId = principal.uid,
                        requestedType = request.type,
                        userComment = request.userComment
                    )

                    // 2. Salva o flashcard gerado no banco de dados
                    val savedFlashcard = flashcardRepository.create(deckId, principal.uid, generatedFlashcard)


                    call.respond(HttpStatusCode.Created, savedFlashcard)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Erro ao gerar flashcard: ${e.message}")
                }
            }
        }
    }
}