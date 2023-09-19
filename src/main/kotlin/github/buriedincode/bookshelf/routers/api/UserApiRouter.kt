package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.ErrorResponse
import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.docs.UserEntry
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
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiRequestBody
import io.javalin.openapi.OpenApiResponse
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.and

object UserApiRouter : Logging {
    private fun Context.getResource(): User {
        return this.pathParam("user-id").toLongOrNull()?.let {
            User.findById(id = it) ?: throw NotFoundResponse(message = "User not found")
        } ?: throw BadRequestResponse(message = "Invalid User Id")
    }

    private fun Context.getInput(): UserInput =
        this.bodyValidator<UserInput>()
            .check({ it.role >= 0 }, error = "Role must be greater than or equal to 0")
            .check({ it.username.isNotBlank() }, error = "Username must not be empty")
            .get()

    @OpenApi(
        description = "List all Users",
        methods = [HttpMethod.GET],
        operationId = "listUsers",
        path = "/users",
        queryParams = [
            OpenApiParam(name = "username", type = String::class),
        ],
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(Array<UserEntry>::class)]),
        ],
        summary = "List all Users",
        tags = ["User"],
    )
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

    @OpenApi(
        description = "Create User",
        methods = [HttpMethod.POST],
        operationId = "createUser",
        path = "/users",
        requestBody = OpenApiRequestBody(content = [OpenApiContent(UserInput::class)], required = true),
        responses = [
            OpenApiResponse(status = "201", content = [OpenApiContent(github.buriedincode.bookshelf.docs.User::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Create User",
        tags = ["User"],
    )
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
                            ?: throw NotFoundResponse(message = "Book not found")
                    },
                )
            }
            input.readBooks.forEach {
                val book = Book.findById(id = it.bookId)
                    ?: throw NotFoundResponse(message = "Book not found")
                ReadBook.new {
                    this.book = book
                    this.user = user
                }
            }

            ctx.status(HttpStatus.CREATED).json(user.toJson(showAll = true))
        }

    @OpenApi(
        description = "Get User by id",
        methods = [HttpMethod.GET],
        operationId = "getUser",
        path = "/users/{user-id}",
        pathParams = [OpenApiParam(name = "user-id", type = Long::class, required = true)],
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.User::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Get User by id",
        tags = ["User"],
    )
    fun getEndpoint(ctx: Context): Unit =
        Utils.query {
            val user = ctx.getResource()
            ctx.json(user.toJson(showAll = true))
        }

    @OpenApi(
        description = "Update User",
        methods = [HttpMethod.PUT],
        operationId = "updateUser",
        path = "/users/{user-id}",
        pathParams = [OpenApiParam(name = "user-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(UserInput::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.User::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Update User",
        tags = ["User"],
    )
    fun updateEndpoint(ctx: Context): Unit =
        Utils.query {
            val user = ctx.getResource()
            val input = ctx.getInput()
            val exists = User.find {
                UserTable.usernameCol eq input.username
            }.firstOrNull()
            if (exists != null && exists != user) {
                throw ConflictResponse(message = "User already exists")
            }
            user.imageUrl = input.imageUrl
            input.readBooks.forEach {
                val book = Book.findById(id = it.bookId)
                    ?: throw NotFoundResponse(message = "Book not found")
                val readBook = ReadBook.find {
                    (ReadBookTable.bookCol eq book.id) and (ReadBookTable.userCol eq user.id)
                }.firstOrNull()
                if (readBook == null) {
                    ReadBook.new {
                        this.book = book
                        this.user = user
                    }
                }
            }
            user.role = input.role
            user.username = input.username
            user.wishedBooks = SizedCollection(
                input.wishedBookIds.map {
                    Book.findById(id = it)
                        ?: throw NotFoundResponse(message = "Book not found")
                },
            )

            ctx.json(user.toJson(showAll = true))
        }

    @OpenApi(
        description = "Delete User",
        methods = [HttpMethod.DELETE],
        operationId = "deleteUser",
        path = "/users/{user-id}",
        pathParams = [OpenApiParam(name = "user-id", type = Long::class, required = true)],
        responses = [
            OpenApiResponse(status = "204"),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Delete User",
        tags = ["User"],
    )
    fun deleteEndpoint(ctx: Context): Unit =
        Utils.query {
            val user = ctx.getResource()
            user.readBooks.forEach {
                it.delete()
            }
            user.delete()
            ctx.status(HttpStatus.NO_CONTENT)
        }

    private fun Context.getIdValue(): IdValue =
        this.bodyValidator<IdValue>()
            .check({ it.id > 0 }, error = "Id must be greater than 0")
            .get()

    private fun Context.getReadInput(): UserReadInput =
        this.bodyValidator<UserReadInput>()
            .check({ it.bookId > 0 }, error = "BookId must be greater than 0")
            .get()

    @OpenApi(
        description = "Add Book to User read list",
        methods = [HttpMethod.PATCH],
        operationId = "addReadBook",
        path = "/users/{user-id}/read",
        pathParams = [OpenApiParam(name = "user-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(UserReadInput::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.User::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Add Book to User read list",
        tags = ["User"],
    )
    fun addReadBook(ctx: Context): Unit =
        Utils.query {
            val user = ctx.getResource()
            val input = ctx.getReadInput()
            val book = Book.findById(id = input.bookId)
                ?: throw NotFoundResponse(message = "Book not found")
            val exists = ReadBook.find {
                (ReadBookTable.bookCol eq book.id) and (ReadBookTable.userCol eq user.id)
            }.firstOrNull()
            if (exists != null) {
                throw BadRequestResponse(message = "User has already been read the Book")
            }
            ReadBook.new {
                this.book = book
                this.user = user
                this.readDate = input.readDate
            }

            ctx.json(obj = user.toJson(showAll = true))
        }

    @OpenApi(
        description = "Remove Book from User read list",
        methods = [HttpMethod.DELETE],
        operationId = "removeReadBook",
        path = "/users/{user-id}/read",
        pathParams = [OpenApiParam(name = "user-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.User::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Remove Book from User read list",
        tags = ["User"],
    )
    fun removeReadBook(ctx: Context): Unit =
        Utils.query {
            val user = ctx.getResource()
            val body = ctx.getIdValue()
            val book = Book.findById(id = body.id)
                ?: throw NotFoundResponse(message = "Book not found")
            val exists = ReadBook.find {
                (ReadBookTable.bookCol eq book.id) and (ReadBookTable.userCol eq user.id)
            }.firstOrNull() ?: throw BadRequestResponse(message = "Book has not been read by this User.")
            exists.delete()

            ctx.json(obj = user.toJson(showAll = true))
        }

    @OpenApi(
        description = "Add Book to User wished list",
        methods = [HttpMethod.PATCH],
        operationId = "addWishedBook",
        path = "/users/{user-id}/wished",
        pathParams = [OpenApiParam(name = "user-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.User::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Add Book to User wished list",
        tags = ["User"],
    )
    fun addWishedBook(ctx: Context): Unit =
        Utils.query {
            val user = ctx.getResource()
            val body = ctx.getIdValue()
            val book = Book.findById(id = body.id)
                ?: throw NotFoundResponse(message = "Book not found")
            if (book in user.wishedBooks) {
                throw ConflictResponse(message = "Book already is on User wished list")
            }
            val temp = user.wishedBooks.toMutableList()
            temp.add(book)
            user.wishedBooks = SizedCollection(temp)

            ctx.json(obj = user.toJson(showAll = true))
        }

    @OpenApi(
        description = "Remove Book from User wished list",
        methods = [HttpMethod.DELETE],
        operationId = "removeWishedBook",
        path = "/users/{user-id}/wished",
        pathParams = [OpenApiParam(name = "user-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.User::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Remove Book from User wished list",
        tags = ["User"],
    )
    fun removeWishedBook(ctx: Context): Unit =
        Utils.query {
            val user = ctx.getResource()
            val body = ctx.getIdValue()
            val book = Book.findById(id = body.id)
                ?: throw NotFoundResponse(message = "Book not found")
            if (!user.wishedBooks.contains(book)) {
                throw NotFoundResponse(message = "Book isn't linked to User wished list")
            }
            val temp = user.wishedBooks.toMutableList()
            temp.remove(book)
            user.wishedBooks = SizedCollection(temp)

            ctx.json(obj = user.toJson(showAll = true))
        }
}
