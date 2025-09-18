package com.estudoapp.domain.model

import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.serialization.Serializable

@Serializable
@IgnoreExtraProperties
data class ChatMessage(
    val id: String = "",
    val sender: String = "USER", // "USER" ou "LLM"
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)