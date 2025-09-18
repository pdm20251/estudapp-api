package com.estudoapp.infrastructure.http.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecommendationDTOs(
    // A IA deve retornar a data no formato "YYYY-MM-DD"
    val proximaDataRevisao: String
)