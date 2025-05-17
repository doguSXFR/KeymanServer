package com.wave

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.ucasoft.ktor.simpleCache.SimpleCache
import com.ucasoft.ktor.simpleCache.cacheOutput
import com.ucasoft.ktor.simpleRedisCache.*
import com.wave.database.SessionService
import com.wave.di.logger
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import org.jetbrains.exposed.sql.*
import org.koin.dsl.module
import org.koin.ktor.ext.getKoin
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import kotlin.onSuccess

fun Application.configureSecurity() {
    // Please read the jwt property from the config file if you are using EngineMain

    val sessionService by inject<SessionService>()
    val logger = logger()

    authentication {
        bearer {
            realm = "Access to private API"
            authenticate { tokenCredential ->
                return@authenticate sessionService.getUserByToken(tokenCredential.token)
                    .onSuccess { user ->
                        logger.info("Found user {} for token {}", user?.username, tokenCredential.token)
                        return@authenticate user
                    }
                    .onFailure { error ->
                        logger.error("Could not find user for token {}", tokenCredential.token, error)
                        return@authenticate null
                    }
            }
        }
    }
}
