package com.wave.database

import com.wave.di.DatabaseService
import com.wave.di.logger
import com.wave.models.SharedKey
import com.wave.safeExecute
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction


class KeyShareService(val database: DatabaseService) {

    private val logger = logger()

    object KeyShare : Table("KeyShare") {
        val userId = integer("user_id").references(UserService.Users.id)
        val keyId = integer("key_id").references(KeyService.Keys.id)
        val tickets = integer("tickets").nullable()

        override val primaryKey = PrimaryKey(userId, keyId)
    }

    init {
        transaction(database.getConnection()) {
            SchemaUtils.create(KeyShare)
        }
    }


    suspend fun getUserAvailableKeys(userId: Int) = safeExecute {
        return@safeExecute dbQuery {
            return@dbQuery KeyService.Keys.join(
                otherTable = KeyShare,
                otherColumn = KeyShare.keyId,
                joinType = JoinType.LEFT,
                onColumn = KeyService.Keys.id
            ).selectAll().where {
                KeyShare.userId eq userId
            }.orWhere {
                KeyService.Keys.owner eq userId
            }.map {
                SharedKey(
                    id = it[KeyService.Keys.id],
                    name = it[KeyService.Keys.name],
                    ownerId = it[KeyService.Keys.owner],
                    tickets = it[KeyShare.tickets]
                )
            }
        }
    }

    suspend fun shareKey(userId: Int, keyId: Int, tickets: Int?) = safeExecute {
        dbQuery {
            KeyShare.insert {
                it[KeyShare.userId] = userId
                it[KeyShare.keyId] = keyId
                it[KeyShare.tickets] = tickets
            }[KeyShare.keyId]
        }
    }

    suspend fun changeKeyTickets(userId: Int, keyId: Int, tickets: Int) = safeExecute {
        dbQuery {
            KeyShare.update({ (KeyShare.userId eq userId) and (KeyShare.keyId eq keyId) }) {
                it[KeyShare.tickets] = tickets
            }
        }
    }

    suspend fun revokeKey(userId: Int, keyId: Int) = safeExecute {
        dbQuery {
            KeyShare.deleteWhere { (KeyShare.userId eq userId) and (KeyShare.keyId eq keyId) }
        }
    }

    suspend fun hasUserKey(userId: Int, keyId: Int) = safeExecute {
        return@safeExecute dbQuery {
            val key = KeyService.Keys.join(
                otherTable = KeyShare,
                otherColumn = KeyShare.keyId,
                joinType = JoinType.LEFT,
                onColumn = KeyService.Keys.id
            ).selectAll().where {
                (KeyService.Keys.id eq keyId) and (KeyService.Keys.owner eq userId)
            }.orWhere {
                (KeyShare.keyId eq keyId) and (KeyShare.userId eq userId)
            }.limit(1).map {
                SharedKey(
                    id = it[KeyService.Keys.id],
                    name = it[KeyService.Keys.name],
                    ownerId = it[KeyService.Keys.owner],
                    tickets = it[KeyShare.tickets]
                )
            }.singleOrNull()

            logger.info("Found shared/owned key {} of user id {}", key?.name, userId)

            return@dbQuery key
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database.getConnection()) { block() }
}
