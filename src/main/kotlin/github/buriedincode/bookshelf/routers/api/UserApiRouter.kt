package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.ErrorResponse
import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.docs.UserEntry
import github.buriedincode.bookshelf.models.*
import github.buriedincode.bookshelf.tables.UserTable
import io.javalin.apibuilder.CrudHandler
import io.javalin.http.*
import io.javalin.openapi.*
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.SizedCollection

object UserApiRouter : CrudHandler, Logging {
    private fun getResource(resourceId: String): User {
        return resourceId.toLongOrNull()?.let {
            User.findById(id = it) ?: throw NotFoundResponse(message = "User not found")
        } ?: throw BadRequestResponse(message = "Invalid User Id")
    }

    private fun Context.getBody(): UserInput = this.bodyValidator<UserInput>()
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
        tags = ["User"]
    )
    override fun getAll(ctx: Context): Unit = Utils.query {
        var users = User.all().toList()
        val username = ctx.queryParam("username")
        if (username != null) {
            users = users.filter { it.username in username || username in it.username }
        }
        ctx.json(users.map { it.toJson() })
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
        tags = ["User"]
    )
    override fun create(ctx: Context): Unit = Utils.query {
        val body = ctx.getBody()
        val exists = User.find {
            UserTable.usernameCol eq body.username
        }.firstOrNull()
        if (exists != null)
            throw ConflictResponse(message = "User already exists")
        val user = User.new {
            imageUrl = body.imageUrl
            readBooks = SizedCollection(body.readBookIds.map {
                Book.findById(id = it)
                    ?: throw NotFoundResponse(message = "Book not found")
            })
            role = body.role
            username = body.username
            wishedBooks = SizedCollection(body.wishedBookIds.map {
                Book.findById(id = it)
                    ?: throw NotFoundResponse(message = "Book not found")
            })
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
        tags = ["User"]
    )
    override fun getOne(ctx: Context, resourceId: String): Unit = Utils.query {
        val user = getResource(resourceId = resourceId)
        ctx.json(user.toJson(showAll = true))
    }

    @OpenApi(
        description = "Update User",
        methods = [HttpMethod.PATCH],
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
        tags = ["User"]
    )
    override fun update(ctx: Context, resourceId: String): Unit = Utils.query {
        val user = getResource(resourceId = resourceId)
        val body = ctx.getBody()
        val exists = User.find {
            UserTable.usernameCol eq body.username
        }.firstOrNull()
        if (exists != null && exists != user)
            throw ConflictResponse(message = "User already exists")
        user.imageUrl = body.imageUrl
        user.readBooks = SizedCollection(body.readBookIds.map {
            Book.findById(id = it)
                ?: throw NotFoundResponse(message = "Book not found")
        })
        user.role = body.role
        user.username = body.username
        user.wishedBooks = SizedCollection(body.wishedBookIds.map {
            Book.findById(id = it)
                ?: throw NotFoundResponse(message = "Book not found")
        })

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
        tags = ["User"]
    )
    override fun delete(ctx: Context, resourceId: String): Unit = Utils.query {
        val user = getResource(resourceId = resourceId)
        user.delete()
        ctx.status(HttpStatus.NO_CONTENT)
    }

    private fun Context.getIdValue(): IdValue = this.bodyValidator<IdValue>()
        .check({ it.id > 0 }, error = "Id must be greater than 0")
        .get()

    @OpenApi(
        description = "Add Book to User read list",
        methods = [HttpMethod.PATCH],
        operationId = "addBookToUserReadList",
        path = "/users/{user-id}/read",
        pathParams = [OpenApiParam(name = "user-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.User::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Add Book to User read list",
        tags = ["User"]
    )
    fun addReadBook(ctx: Context): Unit = Utils.query {
        val user = getResource(resourceId = ctx.pathParam("user-id"))
        val body = ctx.getIdValue()
        val book = Book.findById(id = body.id)
            ?: throw NotFoundResponse(message = "Book not found")
        if (book in user.readBooks)
            throw ConflictResponse(message = "Book already is on User read list")
        val temp = user.readBooks.toMutableList()
        temp.add(book)
        user.readBooks = SizedCollection(temp)

        ctx.json(user.toJson(showAll = true))
    }

    @OpenApi(
        description = "Remove Book from User read list",
        methods = [HttpMethod.DELETE],
        operationId = "removeBookFromUserReadList",
        path = "/users/{user-id}/read",
        pathParams = [OpenApiParam(name = "user-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.User::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Remove Book from User read list",
        tags = ["User"]
    )
    fun removeReadBook(ctx: Context): Unit = Utils.query {
        val user = getResource(resourceId = ctx.pathParam("user-id"))
        val body = ctx.getIdValue()
        val book = Book.findById(id = body.id)
            ?: throw NotFoundResponse(message = "Book not found")
        if (!user.readBooks.contains(book))
            throw NotFoundResponse(message = "Book isn't linked to User read list")
        val temp = user.readBooks.toMutableList()
        temp.remove(book)
        user.readBooks = SizedCollection(temp)

        ctx.json(user.toJson(showAll = true))
    }

    @OpenApi(
        description = "Add Book to User wished list",
        methods = [HttpMethod.PATCH],
        operationId = "addBookToUserWishedList",
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
        tags = ["User"]
    )
    fun addWishedBook(ctx: Context): Unit = Utils.query {
        val user = getResource(resourceId = ctx.pathParam("user-id"))
        val body = ctx.getIdValue()
        val book = Book.findById(id = body.id)
            ?: throw NotFoundResponse(message = "Book not found")
        if (book in user.wishedBooks)
            throw ConflictResponse(message = "Book already is on User wished list")
        val temp = user.wishedBooks.toMutableList()
        temp.add(book)
        user.wishedBooks = SizedCollection(temp)

        ctx.json(user.toJson(showAll = true))
    }

    @OpenApi(
        description = "Remove Book from User wished list",
        methods = [HttpMethod.DELETE],
        operationId = "removeBookFromUserWishedList",
        path = "/users/{user-id}/wished",
        pathParams = [OpenApiParam(name = "user-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.User::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Remove Book from User wished list",
        tags = ["User"]
    )
    fun removeWishedBook(ctx: Context): Unit = Utils.query {
        val user = getResource(resourceId = ctx.pathParam("user-id"))
        val body = ctx.getIdValue()
        val book = Book.findById(id = body.id)
            ?: throw NotFoundResponse(message = "Book not found")
        if (!user.wishedBooks.contains(book))
            throw NotFoundResponse(message = "Book isn't linked to User wished list")
        val temp = user.wishedBooks.toMutableList()
        temp.remove(book)
        user.wishedBooks = SizedCollection(temp)

        ctx.json(user.toJson(showAll = true))
    }
}
