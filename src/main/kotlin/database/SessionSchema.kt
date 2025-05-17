package com.wave.database

import com.wave.di.DatabaseService
import com.wave.di.logger
import com.wave.models.ExposedUser
import com.wave.safeExecute
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.ThreadLocalRandom
import kotlin.streams.asSequence


class SessionService(val databaseService: DatabaseService) {

    private val logger = logger()

    object Sessions : Table("Sessions") {
        val id = integer("id").autoIncrement()
        val sessionToken = varchar("session_token", length = 150).uniqueIndex()
        val userId = integer("user_id").references(UserService.Users.id)
        val createdAt = timestamp("created_at")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(databaseService.getConnection()) {
            SchemaUtils.create(Sessions)
        }
    }

    suspend fun createSession(userId: Int): Result<String> = dbQuery {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val randomString =
            ThreadLocalRandom.current().ints(100.toLong(), 0, charPool.size).asSequence().map(charPool::get)
                .joinToString("")

        try {
            Sessions.insert {
                it[this.userId] = userId
                it[this.sessionToken] = randomString
                it[this.createdAt] = Clock.System.now()
            }[Sessions.id]
            return@dbQuery Result.success(randomString)
        } catch (error: Exception) {
            return@dbQuery Result.failure(error)
        }
    }

    suspend fun getUserByToken(token: String): Result<ExposedUser?> = safeExecute {
        dbQuery {
            logger.debug("Checking for token {}", token)
            UserService.Users.join(
                otherTable = Sessions,
                joinType = JoinType.LEFT,
                onColumn = UserService.Users.id,
                otherColumn = Sessions.userId
            ).selectAll().where { Sessions.sessionToken eq token }.map {
                ExposedUser(
                    id = it[UserService.Users.id], username = it[UserService.Users.username]
                )
            }.singleOrNull()
        }
    }


    private suspend fun <T> dbQuery(block: suspend () -> T): T {
        return newSuspendedTransaction(Dispatchers.IO, db = databaseService.getConnection()) {
            block()
        }
    }

}


