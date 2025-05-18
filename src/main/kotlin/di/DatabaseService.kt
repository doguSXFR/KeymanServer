package com.wave.di

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.engine.applicationEnvironment
import org.jetbrains.exposed.sql.Database

class DatabaseService(private val config: ApplicationConfig) {

    private val hikariConfig = HikariConfig()
    private val datasource: HikariDataSource;
    private val logger = logger()

    init {
        val url = config.property("storage.jdbcURL").getString()
        val user = config.property("storage.user").getString()
        val password = config.property("storage.password").getString()

        hikariConfig.jdbcUrl = url
        hikariConfig.username = user
        hikariConfig.password = password
        hikariConfig.maximumPoolSize = 50
        hikariConfig.driverClassName = "com.impossibl.postgres.jdbc.PGDriver"
        datasource = HikariDataSource(hikariConfig)
    }

    fun getConnection(): Database {
        logger.debug("Sharing new database connection from pool")
        return Database.connect(
            datasource = datasource,
        )
    }


}