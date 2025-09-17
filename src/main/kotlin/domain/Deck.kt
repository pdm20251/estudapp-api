package com.estudoapp.domain

import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.serialization.Serializable

// que não existem nesta classe, apenas ignore-os em vez de dar erro".
@IgnoreExtraProperties
// @Serializable é do Kotlinx. Ele é usado pelo Ktor para converter este objeto
// para JSON na hora de enviar a resposta para o Postman/App.
@Serializable
data class Deck(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val userId: String = "",
    val cardCount: Int = 0
)