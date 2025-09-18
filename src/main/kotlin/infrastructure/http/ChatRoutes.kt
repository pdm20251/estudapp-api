package com.estudoapp.infrastructure.http

import com.estudoapp.domain.model.ChatMessage
import com.estudoapp.domain.model.UserPrincipal
import com.estudoapp.domain.usecases.ProcessChatResponseUseCase
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
    val processChatUseCase = ProcessChatResponseUseCase(chatRepository)

    authenticate("firebase-auth") {
        post("/chat/respond") {
            val principal = call.principal<UserPrincipal>()!!
            val request = call.receive<ChatRequest>()

            val userMessage = ChatMessage(
                sender = "USER",
                text = request.text
            )

            // ✅ A MÁGICA DO "FIRE-AND-FORGET" ✅
            // Lançamos uma nova coroutine que rodará em segundo plano.
            // O código dentro do launch { ... } não bloqueará a resposta.
            // ✅ A CORREÇÃO É APENAS ADICIONAR "call." AQUI ✅
            call.launch {
                processChatUseCase.execute(principal.uid, userMessage)
            }

            call.respond(HttpStatusCode.Accepted)

            // App faz uma requisição...
            // ... e a API responde IMEDIATAMENTE com "Accepted",
            // enquanto o UseCase começa a trabalhar em background.
            call.respond(HttpStatusCode.Accepted)
        }

        get("/chat/history") {
            val principal = call.principal<UserPrincipal>()!!

            // Pega o parâmetro 'limit' da URL (ex: /chat/history?limit=20)
            // O método .toIntOrNull() garante que a app não quebre se o texto não for um número.
            // O operador '?: 50' define um valor padrão de 50 se o parâmetro não for fornecido.
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50

            // Chama o método que já tínhamos implementado no repositório
            val messages = chatRepository.getLatestMessages(
                userId = principal.uid,
                limit = limit
            )

            // Retorna a lista de mensagens como JSON para o cliente
            call.respond(messages)
        }
    }
}