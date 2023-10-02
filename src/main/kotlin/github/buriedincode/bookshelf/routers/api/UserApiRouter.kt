package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.IdValue
import github.buriedincode.bookshelf.models.ReadBook
import github.buriedincode.bookshelf.models.User
import github.buriedincode.bookshelf.models.UserInput
import github.buriedincode.bookshelf.models.UserReadInput
import github.buriedincode.bookshelf.tables.ReadBookTable
import github.buriedincode.bookshelf.tables.UserTable
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.bodyValidator
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.and

object UserApiRouter : Logging {
    fun listEndpoint(ctx: Context): Unit =
        Utils.query {
            var users = User.all().toList()
            val username = ctx.queryParam(key = "username")
            if (username != null) {
                users = users.filter {
                    it.username.contains(username, ignoreCase = true) || username.contains(it.username, ignoreCase = true)
                }
            }
            ctx.json(users.sorted().map { it.toJson() })
        }

    private fun Context.getInput(): UserInput =
        this.bodyValidator<UserInput>()
            .check({ it.role >= 0 }, error = "Role must be greater than or equal to 0")
            .check({ it.username.isNotBlank() }, error = "Username must not be empty")
            .get()

    fun createEndpoint(ctx: Context): Unit =
        Utils.query {
            val input = ctx.getInput()
            val exists = User.find {
                UserTable.usernameCol eq input.username
            }.firstOrNull()
            if (exists != null) {
                throw ConflictResponse(message = "User already exists")
            }
            val user = User.new {
                imageUrl = input.imageUrl
                role = input.role
                username = input.username
                wishedBooks = SizedCollection(
                    input.wishedBookIds.map {
                        Book.findById(id = it)
                            ?: throw NotFoundResponse(message = "Unable to find Book: `$it`")
                    },
                )
            }
            input.readBooks.forEach {
                val book = Book.findById(id = it.bookId)
                    ?: throw NotFoundResponse(message = "Unable to find Book: `${it.bookId}`")
                val readBook = ReadBook.find {
                    (ReadBookTable.bookCol eq book.id) and (ReadBookTable.userCol eq user.id)
                }.firstOrNull() ?: ReadBook.new {
                    this.book = book
                    this.user = user
                }
                readBook.readDate = it.readDate
            }

            ctx.status(HttpStatus.CREATED).json(user.toJson(showAll = true))
        }

    private fun Context.getResource(): User {
        return this.pathParam("user-id").toLongOrNull()?.let {
            User.findById(id = it) ?: throw NotFoundResponse(message = "Unable to find User: `$it`")
        } ?: throw BadRequestResponse(message = "Unable to find User: `${this.pathParam("user-id")}`")
    }

    fun getEndpoint(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            ctx.json(resource.toJson(showAll = true))
        }

    fun updateEndpoint(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getInput()
            val exists = User.find {
                UserTable.usernameCol eq input.username
            }.firstOrNull()
            if (exists != null && exists != resource) {
                throw ConflictResponse(message = "User already exists")
            }
            resource.imageUrl = input.imageUrl
            input.readBooks.forEach {
                val book = Book.findById(id = it.bookId)
                    ?: throw NotFoundResponse(message = "Unable to find Book: `${it.bookId}`")
                val readBook = ReadBook.find {
                    (ReadBookTable.bookCol eq book.id) and (ReadBookTable.userCol eq resource.id)
                }.firstOrNull() ?: ReadBook.new {
                    this.book = book
                    this.user = resource
                }
                readBook.readDate = it.readDate
            }
            resource.role = input.role
            resource.username = input.username
            resource.wishedBooks = SizedCollection(
                input.wishedBookIds.map {
                    Book.findById(id = it)
                        ?: throw NotFoundResponse(message = "Unable to find Book: `$it`")
                },
            )

            ctx.json(resource.toJson(showAll = true))
        }

    fun deleteEndpoint(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            resource.readBooks.forEach {
                it.delete()
            }
            resource.delete()
            ctx.status(HttpStatus.NO_CONTENT)
        }

    private fun Context.getReadInput(): UserReadInput =
        this.bodyValidator<UserReadInput>()
            .check({ it.bookId > 0 }, error = "BookId must be greater than 0")
            .get()

    fun addReadBook(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getReadInput()
            val book = Book.findById(id = input.bookId)
                ?: throw NotFoundResponse(message = "Unable to find Book: `${input.bookId}`")
            val readBook = ReadBook.find {
                (ReadBookTable.bookCol eq book.id) and (ReadBookTable.userCol eq resource.id)
            }.firstOrNull() ?: ReadBook.new {
                this.book = book
                this.user = resource
            }
            readBook.readDate = input.readDate

            ctx.json(resource.toJson(showAll = true))
        }

    private fun Context.getIdValue(): IdValue =
        this.bodyValidator<IdValue>()
            .check({ it.id > 0 }, error = "Id must be greater than 0")
            .get()

    fun removeReadBook(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getIdValue()
            val book = Book.findById(id = input.id)
                ?: throw NotFoundResponse(message = "Unable to find Book: `${input.id}`")
            val exists = ReadBook.find {
                (ReadBookTable.bookCol eq book.id) and (ReadBookTable.userCol eq resource.id)
            }.firstOrNull() ?: throw BadRequestResponse(message = "User has not read this Book")
            exists.delete()

            ctx.json(resource.toJson(showAll = true))
        }

    fun addWishedBook(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getIdValue()
            val book = Book.findById(id = input.id)
                ?: throw NotFoundResponse(message = "Unable to find Book: `${input.id}`")
            val temp = resource.wishedBooks.toMutableSet()
            temp.add(book)
            resource.wishedBooks = SizedCollection(temp)

            ctx.json(resource.toJson(showAll = true))
        }

    fun removeWishedBook(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getIdValue()
            val book = Book.findById(id = input.id)
                ?: throw NotFoundResponse(message = "Unable to find Book: `${input.id}`")
            if (!resource.wishedBooks.contains(book)) {
                throw NotFoundResponse(message = "User has not wished for this Book")
            }
            val temp = resource.wishedBooks.toMutableSet()
            temp.remove(book)
            resource.wishedBooks = SizedCollection(temp)

            ctx.json(resource.toJson(showAll = true))
        }
}
