package com.wave

import com.wave.di.configureFrameworks
import com.wave.routes.hookAuthRouter
import com.wave.routes.hookKeyRouter
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    environment.config

    configureFrameworks()
    configureSecurity()
    configureHTTP()
    configureSockets()
    hookAuthRouter()
    hookKeyRouter()
}
