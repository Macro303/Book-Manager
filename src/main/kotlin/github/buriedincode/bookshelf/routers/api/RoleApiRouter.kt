package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.ErrorResponse
import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.docs.RoleEntry
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.BookCreatorRole
import github.buriedincode.bookshelf.models.Creator
import github.buriedincode.bookshelf.models.Role
import github.buriedincode.bookshelf.models.RoleCreditInput
import github.buriedincode.bookshelf.models.RoleInput
import github.buriedincode.bookshelf.tables.BookCreatorRoleTable
import github.buriedincode.bookshelf.tables.RoleTable
import io.javalin.apibuilder.CrudHandler
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
import org.jetbrains.exposed.sql.and

object RoleApiRouter : CrudHandler, Logging {
    private fun getResource(resourceId: String): Role {
        return resourceId.toLongOrNull()?.let {
            Role.findById(id = it) ?: throw NotFoundResponse(message = "Role not found")
        } ?: throw BadRequestResponse(message = "Invalid Role Id")
    }

    private fun Context.getBody(): RoleInput = this.bodyValidator<RoleInput>()
        .check({ it.title.isNotBlank() }, error = "Title must not be empty")
        .get()

    @OpenApi(
        description = "List all Roles",
        methods = [HttpMethod.GET],
        operationId = "listRoles",
        path = "/roles",
        queryParams = [
            OpenApiParam(name = "title", type = String::class),
        ],
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(Array<RoleEntry>::class)]),
        ],
        summary = "List all Roles",
        tags = ["Role"],
    )
    override fun getAll(ctx: Context): Unit = Utils.query {
        var roles = Role.all().toList()
        val title = ctx.queryParam("title")
        if (title != null) {
            roles = roles.filter {
                it.title.contains(title, ignoreCase = true) || title.contains(
                    it.title,
                    ignoreCase = true,
                )
            }
        }
        ctx.json(roles.sorted().map { it.toJson() })
    }

    @OpenApi(
        description = "Create Role",
        methods = [HttpMethod.POST],
        operationId = "createRole",
        path = "/roles",
        requestBody = OpenApiRequestBody(content = [OpenApiContent(RoleInput::class)], required = true),
        responses = [
            OpenApiResponse(status = "201", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Role::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Create Role",
        tags = ["Role"],
    )
    override fun create(ctx: Context): Unit = Utils.query {
        val body = ctx.getBody()
        val exists = Role.find {
            RoleTable.titleCol eq body.title
        }.firstOrNull()
        if (exists != null) {
            throw ConflictResponse(message = "Role already exists")
        }
        val role = Role.new {
            title = body.title
        }

        ctx.status(HttpStatus.CREATED).json(role.toJson(showAll = true))
    }

    @OpenApi(
        description = "Get Role by id",
        methods = [HttpMethod.GET],
        operationId = "getRole",
        path = "/roles/{role-id}",
        pathParams = [OpenApiParam(name = "role-id", type = Long::class, required = true)],
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Role::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Get Role by id",
        tags = ["Role"],
    )
    override fun getOne(ctx: Context, resourceId: String): Unit = Utils.query {
        val role = getResource(resourceId = resourceId)
        ctx.json(role.toJson(showAll = true))
    }

    @OpenApi(
        description = "Update Role",
        methods = [HttpMethod.PATCH],
        operationId = "updateRole",
        path = "/roles/{role-id}",
        pathParams = [OpenApiParam(name = "role-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(RoleInput::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Role::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Update Role",
        tags = ["Role"],
    )
    override fun update(ctx: Context, resourceId: String): Unit = Utils.query {
        val role = getResource(resourceId = resourceId)
        val body = ctx.getBody()
        val exists = Role.find {
            RoleTable.titleCol eq body.title
        }.firstOrNull()
        if (exists != null && exists != role) {
            throw ConflictResponse(message = "Role already exists")
        }
        role.title = body.title

        ctx.json(role.toJson(showAll = true))
    }

    @OpenApi(
        description = "Delete Role",
        methods = [HttpMethod.DELETE],
        operationId = "deleteRole",
        path = "/roles/{role-id}",
        pathParams = [OpenApiParam(name = "role-id", type = Long::class, required = true)],
        responses = [
            OpenApiResponse(status = "204"),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Delete Role",
        tags = ["Role"],
    )
    override fun delete(ctx: Context, resourceId: String): Unit = Utils.query {
        val role = getResource(resourceId = resourceId)
        role.credits.forEach {
            it.delete()
        }
        role.delete()
        ctx.status(HttpStatus.NO_CONTENT)
    }

    private fun Context.getCreditBody(): RoleCreditInput = this.bodyValidator<RoleCreditInput>()
        .get()

    @OpenApi(
        description = "Add Book and Creator to Role",
        methods = [HttpMethod.PATCH],
        operationId = "addCreditToRole",
        path = "/roles/{role-id}/credits",
        pathParams = [OpenApiParam(name = "role-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(RoleCreditInput::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Role::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Add Book and Creator to Role",
        tags = ["Role"],
    )
    fun addCredit(ctx: Context): Unit = Utils.query {
        val resource = getResource(resourceId = ctx.pathParam("role-id"))
        val body = ctx.getCreditBody()
        val book = Book.findById(id = body.bookId)
            ?: throw NotFoundResponse(message = "Book not found")
        val creator = Creator.findById(id = body.creatorId)
            ?: throw NotFoundResponse(message = "Creator not found")
        val credit = BookCreatorRole.find {
            (BookCreatorRoleTable.bookCol eq book.id) and
                (BookCreatorRoleTable.creatorCol eq creator.id) and
                (BookCreatorRoleTable.roleCol eq resource.id)
        }.firstOrNull()
        if (credit != null) {
            throw ConflictResponse(message = "Book Creator already has this role")
        } else {
            BookCreatorRole.new {
                this.book = book
                this.creator = creator
                role = resource
            }
        }

        ctx.json(resource.toJson(showAll = true))
    }

    @OpenApi(
        description = "Remove Book and Creator from Role",
        methods = [HttpMethod.DELETE],
        operationId = "removeCreditFromRole",
        path = "/roles/{role-id}/credits",
        pathParams = [OpenApiParam(name = "role-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(RoleCreditInput::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Role::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Remove Book and Creator from Role",
        tags = ["Role"],
    )
    fun removeCredit(ctx: Context): Unit = Utils.query {
        val resource = getResource(resourceId = ctx.pathParam("role-id"))
        val body = ctx.getCreditBody()
        val book = Book.findById(id = body.bookId)
            ?: throw NotFoundResponse(message = "Book not found")
        val creator = Creator.findById(id = body.creatorId)
            ?: throw NotFoundResponse(message = "Creator not found")
        val credit = BookCreatorRole.find {
            (BookCreatorRoleTable.bookCol eq book.id) and
                (BookCreatorRoleTable.creatorCol eq creator.id) and
                (BookCreatorRoleTable.roleCol eq resource.id)
        }.firstOrNull() ?: throw NotFoundResponse(message = "Unable to find Book Creator Role")
        credit.delete()

        ctx.json(resource.toJson(showAll = true))
    }
}
