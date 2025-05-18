package com.wave.di

import com.wave.database.KeyShareService
import com.wave.database.LogService
import com.wave.database.SessionService
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import com.wave.database.UserService
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.routing.RoutingCall
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Application.configureFrameworks() {
    install(Koin) {
        slf4jLogger()
        modules(
            module {
                single<ApplicationConfig> { environment.config }
                singleOf(::DatabaseService)
            },
            module {
                singleOf(::UserService)
            },
            module {
                singleOf(::LogService)
            },
            module {
                singleOf(::SessionService)
            },
            module {
                singleOf(::KeyShareService)
            }
        )
    }
}

inline fun <reified T> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)