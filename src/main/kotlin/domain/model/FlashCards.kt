package com.estudoapp.domain.model

import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@IgnoreExtraProperties
sealed class Flashcard {
    abstract val id: String?
    abstract val deckId: String?
    abstract val userId: String?
    // A propriedade 'type' foi removida para resolver o conflito com o discriminador do JSON.
    abstract val fatorFacilidade: Double?
    abstract val repeticoes: Int?
    abstract val intervaloEmDias: Int?
    abstract val proximaRevisaoTimestamp: Long?
}

@Serializable
@SerialName("FRENTE_VERSO")
@IgnoreExtraProperties
data class FrenteVersoFlashcard(
    override val id: String? = "",
    override val deckId: String? = "",
    override val userId: String? = "",
    override val fatorFacilidade: Double? = 2.5,
    override val repeticoes: Int? = 0,
    override val intervaloEmDias: Int? = 1,
    override val proximaRevisaoTimestamp: Long? = 0L,
    val frente: String? = "",
    val verso: String? = ""
) : Flashcard()

@Serializable
@SerialName("CLOZE")
@IgnoreExtraProperties
data class ClozeFlashcard(
    override val id: String? = "",
    override val deckId: String? = "",
    override val userId: String? = "",
    override val fatorFacilidade: Double? = 2.5,
    override val repeticoes: Int? = 0,
    override val intervaloEmDias: Int? = 1,
    override val proximaRevisaoTimestamp: Long? = 0L,
    val textoComLacunas: String? = "",
    val respostasCloze: Map<String, String>? = emptyMap()
) : Flashcard()

@Serializable
@SerialName("DIGITE_RESPOSTA")
@IgnoreExtraProperties
data class DigiteRespostaFlashcard(
    override val id: String? = "",
    override val deckId: String? = "",
    override val userId: String? = "",
    override val fatorFacilidade: Double? = 2.5,
    override val repeticoes: Int? = 0,
    override val intervaloEmDias: Int? = 1,
    override val proximaRevisaoTimestamp: Long? = 0L,
    val pergunta: String? = "",
    val respostasValidas: List<String>? = emptyList()
) : Flashcard()

@Serializable
@SerialName("MULTIPLA_ESCOLHA")
@IgnoreExtraProperties
data class MultiplaEscolhaFlashcard(
    override val id: String? = "",
    override val deckId: String? = "",
    override val userId: String? = "",
    override val fatorFacilidade: Double? = 2.5,
    override val repeticoes: Int? = 0,
    override val intervaloEmDias: Int? = 1,
    override val proximaRevisaoTimestamp: Long? = 0L,
    val pergunta: String? = "",
    val alternativas: List<Alternativa>? = emptyList(),
    val respostaCorreta: String? = ""
) : Flashcard()

@Serializable
@IgnoreExtraProperties
data class Alternativa(
    val text: String? = "",
    val isCorrect: Boolean? = false
)