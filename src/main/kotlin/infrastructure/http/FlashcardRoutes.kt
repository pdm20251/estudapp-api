package com.estudoapp.infrastructure.http

import com.estudoapp.domain.UserPrincipal
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
    val geminiService = GeminiService()
    val validateUseCase = ValidateFlashcardAnswerUseCase(flashcardRepository, geminiService)

    val deckRepository: DeckRepository = FireBaseDeckRepository()

    val generateUseCase = GenerateFlashcardUseCase(deckRepository)

    authenticate("firebase-auth") {
        post("/flashcards/validate") {
            val request = call.receive<ValidateAnswerRequest>()
            // A chamada ao UseCase agora executa o fluxo completo
            val response = validateUseCase.execute(
                deckId = request.deckId,
                flashcardId = request.flashcardId,
                userAnswer = request.userAnswer
            )
            call.respond(response)
        }

        route("/decks/{deckId}/flashcards") {
            // Middleware de checagem de posse do deck
            intercept(ApplicationCallPipeline.Call) {
                val principal = call.principal<UserPrincipal>()!!
                val deckId = call.parameters["deckId"] ?: return@intercept call.respond(HttpStatusCode.BadRequest)

                val deck = deckRepository.findById(deckId, principal.uid)
                if (deck == null) {
                    return@intercept call.respond(HttpStatusCode.Forbidden, "Este deck não pertence a você.")
                }
                // Se o deck existe e pertence ao usuário, a requisição continua.
            }

            // GET .../decks/{deckId}/flashcards - Lista todos os flashcards do deck
            post("/generate") {
                val principal = call.principal<UserPrincipal>()!!
                val deckId = call.parameters["deckId"] ?: return@post call.respond(HttpStatusCode.BadRequest, "ID do Deck não fornecido")
                val request = call.receive<GenerateFlashcardRequest>()

                try {
                    // Chama o UseCase para executar toda a lógica de negócio
                    val generatedFlashcard = generateUseCase.execute(
                        deckId = deckId,
                        userId = principal.uid,
                        requestedType = request.type,
                        userComment = request.userComment
                    )

                    // Retorna o flashcard "mock" gerado com sucesso
                    call.respond(HttpStatusCode.OK, generatedFlashcard)
                } catch (e: Exception) {
                    // Se o deck não for encontrado ou o tipo for inválido, retorna um erro claro
                    call.respond(HttpStatusCode.InternalServerError, "Erro ao gerar flashcard: ${e.message}")
                }
            }

        }
    }
}