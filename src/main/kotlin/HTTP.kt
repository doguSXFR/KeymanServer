package com.wave

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.ucasoft.ktor.simpleCache.SimpleCache
import com.ucasoft.ktor.simpleCache.cacheOutput
import com.ucasoft.ktor.simpleRedisCache.*
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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import java.time.Duration
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import org.jetbrains.exposed.sql.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureHTTP() {
    install(ContentNegotiation) {
        json()
    }
    install(SimpleCache) {
        redisCache {
            invalidateAt = 10.seconds
            host = "192.168.0.4"
            port = 6379
        }
    }
    routing {
        swaggerUI(path = "openapi")
    }
    routing {
        openAPI(path = "openapi")
    }
    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
        header("CodeMan", "6ix4our")
    }
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("MyCustomHeader")
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }
    install(Compression)
    routing {
        cacheOutput(5.seconds) {
            get("/short") {
                call.respond(Random.nextInt().toString())
            }
        }
        cacheOutput {
            get("/default") {
                call.respond(Random.nextInt().toString())
            }
        }
    }
}

@Serializable
enum class ErrorType {
    INVALID_CREDENTIALS,
    USER_NOT_FOUND,
    USER_ALREADY_EXISTS,
    INVALID_SESSION,
    INVALID_KEY,
    INVALID_LOG,
    INVALID_USER,
    INVALID_EVENT_TYPE,
    INTERNAL_ERROR,
    UNALLOWED_UNLOCK,
    NOT_ENOUGH_TICKETS
}


suspend fun <T> safeExecute(
    block: suspend () -> T,
): Result<T> {
    try {
        val result = block()
        return Result.success(result)
    } catch (e: Exception) {
        return Result.failure(e)
    }
}