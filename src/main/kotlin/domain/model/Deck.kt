package com.estudoapp.domain.model


import kotlinx.serialization.Serializable


@Serializable
data class Deck(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var userId: String = "",
    var cardCount: Int = 0,
    // NOVO CAMPO: Armazena a data/hora da próxima revisão em formato de timestamp (milissegundos).
    var proximaRevisaoTimestamp: Long? = null
)