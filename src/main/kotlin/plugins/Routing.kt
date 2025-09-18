package com.estudoapp.plugins

import com.estudoapp.infrastructure.http.chatRoutes
import com.estudoapp.infrastructure.http.deckRoutes
import com.estudoapp.infrastructure.http.flashcardRoutes
// import com.estudoapp.infrastructure.http.flashcardRoutes
import io.ktor.server.application.*
import io.ktor.server.response.respondText
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/health") {
            call.respondText("Servidor está no ar!")
        }
        deckRoutes()
        flashcardRoutes()
        chatRoutes()
    }
}