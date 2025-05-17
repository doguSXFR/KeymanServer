package com.wave.models

import kotlinx.serialization.Serializable

@Serializable
data class ExposedUser(val id: Int, val username: String)

@Serializable
data class CreateUser(val username: String, val password: String)

@Serializable
data class UserLogin(
    val username: String,
    val password: String
)

@Serializable
data class UserLoginResponse(
    val token: String,
)