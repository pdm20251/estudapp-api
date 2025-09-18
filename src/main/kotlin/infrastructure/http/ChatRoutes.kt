package com.estudoapp.infrastructure.http

import com.estudoapp.domain.model.ChatMessage
import com.estudoapp.domain.model.UserPrincipal
import com.estudoapp.domain.usecases.ProcessChatResponseUseCase
import com.estudoapp.infrastructure.gateways.dto.GroqService
import com.estudoapp.infrastructure.http.dto.ChatRequest
import com.estudoapp.infrastructure.persistence.FirebaseChatRepository
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch

fun Route.chatRoutes() {
    val chatRepository = FirebaseChatRepository()
    // Instancia o novo GroqService
    val groqService = GroqService()
    // Injeta o GroqService no UseCase
    val processChatUseCase = ProcessChatResponseUseCase(chatRepository, groqService)

    authenticate("firebase-auth") {
        post("/chat/respond") {
            val principal = call.principal<UserPrincipal>()!!
            val request = call.receive<ChatRequest>()

            // Cria o objeto ChatMessage a partir da requisição
            val userMessage = ChatMessage(
                sender = "USER",
                text = request.text
            )

            // A lógica "fire-and-forget" continua a mesma, mas agora
            // passamos a mensagem do usuário para o UseCase.
            call.launch {
                processChatUseCase.execute(principal.uid, userMessage)
            }

            // Responde imediatamente para o app não ficar travado esperando a IA
            call.respond(HttpStatusCode.Accepted)
        }

        get("/chat/history") {
            val principal = call.principal<UserPrincipal>()!!
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            val messages = chatRepository.getLatestMessages(
                userId = principal.uid,
                limit = limit
            )
            call.respond(messages)
        }
    }
}
