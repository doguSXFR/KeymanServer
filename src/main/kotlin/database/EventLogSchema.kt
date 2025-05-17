package com.wave.database

import com.google.protobuf.Timestamp
import com.wave.di.DatabaseService

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.Date

@Serializable
enum class EventType {
    DOOR_OPENED,
    DOOR_CLOSED,
    DOOR_PROBLEM,
    NOT_ENOUGH_TICKETS,
}

@Serializable
data class ExposedLog(
    val id: Int,
    val timestamp: Instant,
    val userId: Int,
    val eventType: EventType,
    val keyId: Int
)

@Serializable
data class CreateLog(
    val timestamp: Instant,
    val userId: Int,
    val eventType: EventType,
    val keyId: Int
)


class LogService(val database: DatabaseService) {
    object Logs : Table("EventLog") {
        val id = integer("id").autoIncrement()
        val timestamp = timestamp("timestamp").default(Clock.System.now())
        val userId = reference("user_id", UserService.Users.id, onDelete = ReferenceOption.CASCADE)
        val keyId = reference("key_id", KeyService.Keys.id, onDelete = ReferenceOption.CASCADE)
        val eventType = enumeration("event_type", EventType::class)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database.getConnection()) {
            SchemaUtils.create(Logs)
        }
    }

    suspend fun journal(key: CreateLog): Int = dbQuery {
        Logs.insert {
            it[timestamp] = key.timestamp
            it[userId] = key.userId
            it[keyId] = key.keyId
            it[eventType] = key.eventType
        }[Logs.id]
    }

    suspend fun since(timestamp: Instant): ExposedLog? {
        return dbQuery {
            Logs.selectAll().where { Logs.timestamp greaterEq timestamp }.map {
                ExposedLog(
                    id = it[Logs.id],
                    timestamp = it[Logs.timestamp],
                    userId = it[Logs.userId],
                    eventType = it[Logs.eventType],
                    keyId = it[Logs.keyId]
                )
            }.singleOrNull()
        }
    }


    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database.getConnection()) { block() }
}
