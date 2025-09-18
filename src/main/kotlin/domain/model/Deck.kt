package com.estudoapp.domain.model


import kotlinx.serialization.Serializable


@Serializable
data class Deck(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var userId: String = "",
    var cardCount: Int = 0
)