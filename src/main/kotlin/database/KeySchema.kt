package com.wave.database

import com.wave.database.KeyShareService.KeyShare
import com.wave.di.DatabaseService
import com.wave.models.CreateKey
import com.wave.models.ExposedKey
import com.wave.safeExecute
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction


class KeyService(val database: DatabaseService) {
    object Keys : Table("DoorKeys") {
        val id = integer("id").autoIncrement()
        val name = varchar("name", length = 50)
        val owner = reference("owner", UserService.Users.id, onDelete = ReferenceOption.CASCADE)
        val endpoint = varchar("endpoint", length = 200)
        val method = varchar("method", length = 50)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database.getConnection()) {
            SchemaUtils.create(Keys)
        }
    }

    suspend fun getUserOwnedKeys(userId: Int) = safeExecute {
        return@safeExecute dbQuery {
            return@dbQuery Keys.selectAll().where {
                Keys.owner eq userId
            }.andWhere { Keys.owner eq userId }.limit(1).map {
                ExposedKey(
                    id = it[Keys.id], name = it[Keys.name], ownerId = it[Keys.owner]
                )
                }
        }
    }


    suspend fun isOwner(userId: Int, keyId: Int) = safeExecute {
        return@safeExecute dbQuery {
            val data = Keys.selectAll().where {
                Keys.id eq keyId
            }.andWhere { Keys.owner eq userId }.limit(1).map {
                    ExposedKey(
                        id = it[Keys.id], name = it[Keys.name], ownerId = it[Keys.owner]
                    )
                }

            if (data.isNotEmpty()) {
                return@dbQuery true
            } else {
                return@dbQuery false
            }
        }
    }

    suspend fun create(key: CreateKey): Int = dbQuery {
        Keys.insert {
            it[name] = key.name
            it[owner] = key.owner.id
            it[endpoint] = key.endpoint
            it[method] = key.method
        }[Keys.id]
    }

    suspend fun read(id: Int): ExposedKey? {
        return dbQuery {
            Keys.selectAll().where { Keys.id eq id }.map {
                ExposedKey(
                    id = it[Keys.id], name = it[Keys.name], ownerId = it[Keys.owner]
                )
            }.singleOrNull()
        }
    }

    suspend fun update(id: Int, key: CreateKey) {
        dbQuery {
            Keys.update({ Keys.id eq id }) {
                it[name] = key.name
                it[owner] = key.owner.id
                it[endpoint] = key.endpoint
                it[method] = key.method
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            Keys.deleteWhere { Keys.id.eq(id) }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database.getConnection()) { block() }
}
