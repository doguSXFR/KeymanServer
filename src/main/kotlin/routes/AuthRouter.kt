package com.wave.routes

import com.wave.ErrorType

import com.wave.database.SessionService
import com.wave.database.UserService
import com.wave.di.logger
import com.wave.models.CreateUser
import com.wave.models.ExposedUser
import com.wave.models.UserLogin
import com.wave.models.UserLoginResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject


fun Application.hookAuthRouter() {

    val userService: UserService by inject()
    val sessionService: SessionService by inject()
    val logger = logger()

    routing {
        authenticate {
            get("/auth/profile") {
                val user = call.authentication.principal<ExposedUser>()
                if (user != null) {
                    logger.info("User {} fetched profile", user.username)
                    call.respond(user)
                } else {
                    logger.warn("User not authenticated or session expired")
                    call.respond(HttpStatusCode.Unauthorized, ErrorType.INVALID_USER)
                }
            }
        }

        post("/auth/login") {
            val userAttempt: UserLogin = call.receive()
            val authResult = userService.authenticate(userAttempt.username, userAttempt.password);
            logger.info("User {} logged in", userAttempt.username)
            authResult.onSuccess { user ->
                sessionService.createSession(user.id).onSuccess { token ->
                    logger.debug("User {} created session", user.username)
                    call.respond(UserLoginResponse(token))
                }.onFailure {
                    logger.warn("Failed to create session for user {}", user.username)
                    call.respond(status = HttpStatusCode.Unauthorized, ErrorType.INVALID_USER)
                }
            }.onFailure {
                logger.warn("Failed to authenticate user {}", userAttempt.username)
                call.respond(status = HttpStatusCode.Unauthorized, ErrorType.INVALID_CREDENTIALS)
            }
        }

        post("/auth/register") {
            val userAttempt = call.receive<UserLogin>()
            println("User attempt: $userAttempt")
            userService.create(CreateUser(userAttempt.username, userAttempt.password)).onSuccess { user ->
                sessionService.createSession(user.id).onSuccess { token ->
                    call.respond(UserLoginResponse(token))
                }.onFailure {
                    call.respond(status = HttpStatusCode.Unauthorized, ErrorType.INVALID_USER)
                }
            }.onFailure {
                call.respond(status = HttpStatusCode.Unauthorized, ErrorType.INVALID_CREDENTIALS)
            }
        }
    }


}