package com.wave.database

import com.wave.di.DatabaseService
import com.wave.models.CreateUser
import com.wave.models.ExposedUser
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import kotlin.let




class UserService(val databaseService: DatabaseService) {

    object Users : Table("Users") {
        val id = integer("id").autoIncrement()
        val username = varchar("username", length = 50).uniqueIndex()
        val password = varchar("password", length = 200)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(databaseService.getConnection()) {
            SchemaUtils.create(Users)
        }
    }

    suspend fun getAll(): List<ExposedUser> {
        return dbQuery {
            Users.selectAll().map { ExposedUser(it[Users.id], it[Users.username]) }
        }
    }

    suspend fun create(user: CreateUser): Result<ExposedUser> = dbQuery {
        val arg2SpringSecurity = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        val passwordHash = arg2SpringSecurity.encode(user.password)

        try {
            Users.insertReturning(listOf(Users.id, Users.username)) {
                it[username] = user.username
                it[password] = passwordHash
            }.map { id ->
                val insertedId = id[Users.id]
                val insertedUsername = id[Users.username]
                return@dbQuery Result.success(ExposedUser(insertedId, insertedUsername))
            }
            return@dbQuery Result.failure(Exception("User already exists"))
        } catch (e: Exception) {
            return@dbQuery Result.failure(e)
        }
    }

    suspend fun authenticate(username: String, password: String): Result<ExposedUser> {
        return dbQuery {
            val user = Users.selectAll().where {
                Users.username eq username
            }.limit(1).firstOrNull();

            if (user != null) {
                val arg2SpringSecurity = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
                if (arg2SpringSecurity.matches(password, user[Users.password])) {
                    return@dbQuery Result.success(ExposedUser(user[Users.id], user[Users.username]))
                } else {
                    return@dbQuery Result.failure(Exception("Invalid password"))
                }
            } else {
                return@dbQuery Result.failure(Exception("User not found"))
            }
        }
    }

    suspend fun read(id: Int): ExposedUser? {
        return dbQuery {
            Users.selectAll().where { Users.id eq id }.map { ExposedUser(id, it[Users.username]) }.singleOrNull()
        }
    }

    suspend fun update(id: Int, user: CreateUser) {
        dbQuery {
            val arg2SpringSecurity = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
            val passwordHash = arg2SpringSecurity.encode(user.password)

            Users.update({ Users.id eq id }) {
                it[username] = user.username
                it[password] = passwordHash
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            Users.deleteWhere { Users.id.eq(id) }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T {
        return newSuspendedTransaction(Dispatchers.IO, db = databaseService.getConnection()) {
            block()
        }
    }

}

