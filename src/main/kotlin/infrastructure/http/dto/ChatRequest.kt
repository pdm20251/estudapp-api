package com.estudoapp.infrastructure.http.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(val text: String)