package com.wave.routes

import com.wave.ErrorType
import com.wave.database.CreateLog
import com.wave.database.KeyShareService
import com.wave.database.LogService
import com.wave.di.logger
import com.wave.models.ExposedUser
import com.wave.models.OpenDoorKeyAttempt
import com.wave.models.OpenDoorKeyResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import org.koin.ktor.ext.inject

fun Application.hookKeyRouter() {

    val logService: LogService by inject()
    val keyShareService: KeyShareService by inject()
    val logger = logger()

    routing {
        authenticate {
            get("/mykeys") {
                val user = call.authentication.principal<ExposedUser>()
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorType.INVALID_KEY)
                    return@get
                }

                logger.info("User {} fetched owned and shared keys", user.username)

                keyShareService.getUserAvailableKeys(user.id).onSuccess { keys ->
                    call.respond(HttpStatusCode.OK, keys)
                }.onFailure {
                    call.respond(HttpStatusCode.InternalServerError, ErrorType.INTERNAL_ERROR)
                }
            }

            post("/unlock") {
                // Retrieve the authenticated user from the session
                val user = call.authentication.principal<ExposedUser>()
                // Parse the incoming request body into an OpenDoorKeyAttempt object
                val unlockAttempt: OpenDoorKeyAttempt = call.receive()

                // If the user is not authenticated, respond with an Unauthorized status
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorType.INVALID_SESSION)
                    logger.error("User trying to unlock door id {} without session", unlockAttempt.keyId)
                    return@post
                }

                // Check if the user has access to the specified key
                keyShareService.hasUserKey(user.id, unlockAttempt.keyId).onSuccess { key ->
                    if (key == null) {
                        // If the key is not valid for the user, respond with an Unauthorized status
                        call.respond(HttpStatusCode.Unauthorized, ErrorType.UNALLOWED_UNLOCK)
                        logger.error(
                            "User {} trying to unlock door id {} without access",
                            user.username,
                            unlockAttempt.keyId
                        )
                        return@post
                    }


                    // If the key has tickets and no tickets are remaining, log the event and respond with an error
                    if (key.tickets != null && key.tickets == 0) {
                        logService.journal(
                            CreateLog(
                                timestamp = Clock.System.now(),
                                userId = user.id,
                                eventType = com.wave.database.EventType.NOT_ENOUGH_TICKETS,
                                keyId = unlockAttempt.keyId
                            )
                        )
                        logger.error(
                            "User {} trying to unlock door id {} without tickets",
                            user.username,
                            unlockAttempt.keyId
                        )
                        call.respond(HttpStatusCode.Unauthorized, ErrorType.NOT_ENOUGH_TICKETS)
                        return@post
                    }

                    // Log the successful door unlock attempt
                    logService.journal(
                        CreateLog(
                            timestamp = Clock.System.now(),
                            userId = user.id,
                            eventType = com.wave.database.EventType.DOOR_OPENED,
                            keyId = unlockAttempt.keyId
                        )
                    )
                    logger.info(
                        "User {} unlocked door id {}",
                        user.username,
                        unlockAttempt.keyId
                    )
                    // Decrement the remaining tickets for the key if applicable
                    val remainingTickets = key.tickets?.minus(1)
                    if (remainingTickets != null) {
                        logger.info(
                            "{} tickets for door id {} changed to {}",
                            user.username,
                            unlockAttempt.keyId,
                            remainingTickets
                        )
                        keyShareService.changeKeyTickets(user.id, unlockAttempt.keyId, remainingTickets)
                    }
                    // Respond with a success status and the remaining tickets
                    call.respond(HttpStatusCode.OK, OpenDoorKeyResponse(true, remainingTickets))

                }.onFailure {
                    // If there is an error during the key validation process, respond with an Unauthorized status
                    call.respond(HttpStatusCode.Unauthorized, ErrorType.INVALID_KEY)
                    return@post
                }
            }
        }
    }
}
