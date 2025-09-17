package com.estudoapp.infrastructure.http


import com.estudoapp.domain.Deck
import com.estudoapp.domain.UserPrincipal
import com.estudoapp.domain.repositories.DeckRepository
import com.estudoapp.infrastructure.persistence.FireBaseDeckRepository
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.deckRoutes() {

    val deckRepository: DeckRepository = FireBaseDeckRepository()

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
            val principal = call.principal<UserPrincipal>()!!
            val deckRequest = call.receive<Deck>()

            val createdDeck = deckRepository.create(deckRequest, principal.uid)
            call.respond(createdDeck)
        }
    }
}