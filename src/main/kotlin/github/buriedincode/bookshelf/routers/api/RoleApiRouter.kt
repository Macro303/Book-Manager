package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.ErrorResponse
import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.docs.RoleEntry
import github.buriedincode.bookshelf.models.Role
import github.buriedincode.bookshelf.models.RoleInput
import github.buriedincode.bookshelf.tables.RoleTable
import io.javalin.http.*
import io.javalin.openapi.*
import org.apache.logging.log4j.kotlin.Logging

object RoleApiRouter : Logging {
    private fun Context.getRole(): Role {
        return this.pathParam("role-id").toLongOrNull()?.let {
            Role.findById(id = it) ?: throw NotFoundResponse(message = "Role not found")
        } ?: throw BadRequestResponse(message = "Invalid Role Id")
    }

    private fun Context.getRoleInput(): RoleInput = this.bodyValidator<RoleInput>()
        .check({ it.title.isNotBlank() }, error = "Title must not be empty")
        .get()

    @OpenApi(
        description = "List all Roles",
        methods = [HttpMethod.GET],
        operationId = "listRoles",
        path = "/roles",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(Array<RoleEntry>::class)]),
        ],
        security = [],
        summary = "List all Roles",
        tags = ["Role"]
    )
    fun listRoles(ctx: Context): Unit = Utils.query(description = "List Roles") {
        val roles = Role.all()
        ctx.json(roles.map { it.toJson() })
    }

    @OpenApi(
        description = "Create Role",
        methods = [HttpMethod.POST],
        operationId = "createRole",
        path = "/roles",
        pathParams = [],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(RoleInput::class)], required = true),
        responses = [
            OpenApiResponse(status = "201", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Role::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Create Role",
        tags = ["Role"]
    )
    fun createRole(ctx: Context): Unit = Utils.query(description = "Create Role") {
        val body = ctx.getRoleInput()
        val exists = Role.find {
            RoleTable.titleCol eq body.title
        }.firstOrNull()
        if (exists != null)
            throw ConflictResponse(message = "Role already exists")
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
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Role::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Get Role by id",
        tags = ["Role"]
    )
    fun getRole(ctx: Context): Unit = Utils.query(description = "Get Role") {
        val role = ctx.getRole()
        ctx.json(role.toJson(showAll = true))
    }

    @OpenApi(
        description = "Update Role",
        methods = [HttpMethod.PUT],
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
        security = [],
        summary = "Update Role",
        tags = ["Role"]
    )
    fun updateRole(ctx: Context): Unit = Utils.query(description = "Update Role") {
        val role = ctx.getRole()
        val body = ctx.getRoleInput()
        val exists = Role.find {
            RoleTable.titleCol eq body.title
        }.firstOrNull()
        if (exists != null && exists != role)
            throw ConflictResponse(message = "Role already exists")
        role.title = body.title

        ctx.json(role.toJson(showAll = true))
    }

    @OpenApi(
        description = "Delete Role",
        methods = [HttpMethod.DELETE],
        operationId = "deleteRole",
        path = "/roles/{role-id}",
        pathParams = [OpenApiParam(name = "role-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = []),
        responses = [
            OpenApiResponse(status = "204"),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        security = [],
        summary = "Delete Role",
        tags = ["Role"]
    )
    fun deleteRole(ctx: Context): Unit = Utils.query(description = "Delete Role") {
        val role = ctx.getRole()
        role.delete()
        ctx.status(HttpStatus.NO_CONTENT)
    }
}