package com.wave.models

import kotlinx.serialization.Serializable

@Serializable
data class ExposedKey(val id: Int, val name: String, val ownerId: Int)

@Serializable
data class CreateKey(val name: String, val owner: ExposedUser, val endpoint: String, val method: String)

@Serializable
data class OpenDoorKeyAttempt(
    val keyId: Int
)

@Serializable
data class OpenDoorKeyResponse(
    val success: Boolean,
    val remainingTickets: Int?,
)

@Serializable
data class SharedKey(
    val id: Int,
    val name: String,
    val ownerId: Int,
    val tickets: Int?
)