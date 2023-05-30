package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.ErrorResponse
import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.docs.CreatorEntry
import github.buriedincode.bookshelf.models.*
import github.buriedincode.bookshelf.tables.*
import io.javalin.http.*
import io.javalin.apibuilder.*
import io.javalin.openapi.*
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.and

object CreatorApiRouter : CrudHandler, Logging {
    private fun getResource(resourceId: String): Creator {
        return resourceId.toLongOrNull()?.let {
            Creator.findById(id = it) ?: throw NotFoundResponse(message = "Creator not found")
        } ?: throw BadRequestResponse(message = "Invalid Creator Id")
    }

    private fun Context.getBody(): CreatorInput = this.bodyValidator<CreatorInput>()
        .check({ it.name.isNotBlank() }, error = "Name must not be empty")
        .get()

    @OpenApi(
        description = "List all Creators",
        methods = [HttpMethod.GET],
        operationId = "listCreators",
        path = "/creators",
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(Array<CreatorEntry>::class)]),
        ],
        summary = "List all Creators",
        tags = ["Creator"]
    )
    override fun getAll(ctx: Context): Unit = Utils.query {
        val creators = Creator.all()
        ctx.json(creators.map { it.toJson() })
    }

    @OpenApi(
        description = "Create Creator",
        methods = [HttpMethod.POST],
        operationId = "createCreator",
        path = "/creators",
        requestBody = OpenApiRequestBody(content = [OpenApiContent(CreatorInput::class)], required = true),
        responses = [
            OpenApiResponse(status = "201", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Creator::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Create Creator",
        tags = ["Creator"]
    )
    override fun create(ctx: Context): Unit = Utils.query {
        val body = ctx.getBody()
        val exists = Creator.find {
            CreatorTable.nameCol eq body.name
        }.firstOrNull()
        if (exists != null)
            throw ConflictResponse(message = "Creator already exists")
        val creator = Creator.new {
            imageUrl = body.imageUrl
            name = body.name
        }

        ctx.status(HttpStatus.CREATED).json(creator.toJson(showAll = true))
    }

    @OpenApi(
        description = "Get Creator by id",
        methods = [HttpMethod.GET],
        operationId = "getCreator",
        path = "/creators/{creator-id}",
        pathParams = [OpenApiParam(name = "creator-id", type = Long::class, required = true)],
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Creator::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Get Creator by id",
        tags = ["Creator"]
    )
    override fun getOne(ctx: Context, resourceId: String): Unit = Utils.query {
        val resource = getResource(resourceId=resourceId)
        ctx.json(resource.toJson(showAll = true))
    }

    @OpenApi(
        description = "Update Creator",
        methods = [HttpMethod.PUT],
        operationId = "updateCreator",
        path = "/creators/{creator-id}",
        pathParams = [OpenApiParam(name = "creator-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(CreatorInput::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Creator::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Update Creator",
        tags = ["Creator"]
    )
    override fun update(ctx: Context, resourceId: String): Unit = Utils.query {
        val resource = getResource(resourceId=resourceId)
        val body = ctx.getBody()
        val exists = Creator.find {
            CreatorTable.nameCol eq body.name
        }.firstOrNull()
        if (exists != null && exists != resource)
            throw ConflictResponse(message = "Creator already exists")
        resource.imageUrl = body.imageUrl
        resource.name = body.name

        ctx.json(resource.toJson(showAll = true))
    }

    @OpenApi(
        description = "Delete Creator",
        methods = [HttpMethod.DELETE],
        operationId = "deleteCreator",
        path = "/creators/{creator-id}",
        pathParams = [OpenApiParam(name = "creator-id", type = Long::class, required = true)],
        responses = [
            OpenApiResponse(status = "204"),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Delete Creator",
        tags = ["Creator"]
    )
    override fun delete(ctx: Context, resourceId: String): Unit = Utils.query {
        val resource = getResource(resourceId=resourceId)
        resource.delete()
        ctx.status(HttpStatus.NO_CONTENT)
    }
    
    private fun Context.getCreditBody(): BookRoleInput = this.bodyValidator<BookRoleInput>()
        .get()
    
    fun addCredit(ctx: Context): Unit = Utils.query {
        val resource = getResource(resourceId=ctx.pathParam("creator-id"))
        val body = ctx.getCreditBody()
        val book = Book.findById(id = body.bookId)
            ?: throw NotFoundResponse(message = "Book not found")
        val role = Role.findById(id = body.roleId)
            ?: throw NotFoundResponse(message = "Role not found")
        val credit = BookCreatorRole.find {
            (BookCreatorRoleTable.bookCol eq book.id) and (BookCreatorRoleTable.creatorCol eq resource.id) and (BookCreatorRoleTable.roleCol eq role.id)
        }.firstOrNull()
        if (credit != null) {
            throw ConflictResponse(message = "Book Creator already has this role")
        } else
            BookCreatorRole.new {
                this.book = book
                creator = resource
                this.role = role
            }

        ctx.json(resource.toJson(showAll = true))
    }
    
    fun removeCredit(ctx: Context): Unit = Utils.query {
        val resource = getResource(resourceId=ctx.pathParam("creator-id"))
        val body = ctx.getCreditBody()
        val book = Book.findById(id = body.bookId)
            ?: throw NotFoundResponse(message = "Book not found")
        val role = Role.findById(id = body.roleId)
            ?: throw NotFoundResponse(message = "Role not found")
        val credit = BookCreatorRole.find {
            (BookCreatorRoleTable.bookCol eq book.id) and (BookCreatorRoleTable.creatorCol eq resource.id) and (BookCreatorRoleTable.roleCol eq role.id)
        }.firstOrNull() ?: throw NotFoundResponse(message = "Unable to find Book Creator Role")
        credit.delete()

        ctx.json(resource.toJson(showAll = true))
    }
}