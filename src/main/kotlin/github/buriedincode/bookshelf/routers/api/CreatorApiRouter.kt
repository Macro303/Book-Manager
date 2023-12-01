package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Creator
import github.buriedincode.bookshelf.models.CreatorInput
import github.buriedincode.bookshelf.models.Credit
import github.buriedincode.bookshelf.models.Role
import github.buriedincode.bookshelf.tables.CreatorTable
import github.buriedincode.bookshelf.tables.CreditTable
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.bodyValidator
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.and

object CreatorApiRouter : BaseApiRouter<Creator>(entity = Creator), Logging {
    override fun listEndpoint(ctx: Context) {
        Utils.query {
            var resources = Creator.all().toList()
            ctx.queryParam("book-id")?.toLongOrNull()?.let {
                Book.findById(it)?.let { book ->
                    resources = resources.filter { book in it.credits.map { it.book } }
                }
            }
            ctx.queryParam("has-image")?.lowercase()?.toBooleanStrictOrNull()?.let { image ->
                resources = resources.filter { it.image != null == image }
            }
            ctx.queryParam("name")?.let { name ->
                resources = resources.filter { it.name.contains(name, ignoreCase = true) || name.contains(it.name, ignoreCase = true) }
            }
            ctx.queryParam("role-id")?.toLongOrNull()?.let {
                Role.findById(it)?.let { role ->
                    resources = resources.filter { role in it.credits.map { it.role } }
                }
            }
            ctx.json(resources.sorted().map { it.toJson() })
        }
    }

    private fun Context.getInput(): CreatorInput =
        this.bodyValidator<CreatorInput>()
            .check({ it.name.isNotBlank() }, error = "Name must not be empty")
            .get()

    override fun createEndpoint(ctx: Context) {
        Utils.query {
            val input = ctx.getInput()
            val exists = Creator.find {
                CreatorTable.nameCol eq input.name
            }.firstOrNull()
            if (exists != null) {
                throw ConflictResponse(message = "Creator already exists")
            }
            val creator = Creator.new {
                image = input.image
                name = input.name
                summary = input.summary
            }
            input.credits.forEach {
                val book = Book.findById(id = it.bookId)
                    ?: throw NotFoundResponse(message = "Unable to find Book: `${it.bookId}`")
                val role = Role.findById(id = it.roleId)
                    ?: throw NotFoundResponse(message = "Unable to find Role: `${it.roleId}`")
                Credit.find {
                    (CreditTable.bookCol eq book.id) and
                        (CreditTable.creatorCol eq creator.id) and
                        (CreditTable.roleCol eq role.id)
                }.firstOrNull() ?: Credit.new {
                    this.book = book
                    this.creator = creator
                    this.role = role
                }
            }

            ctx.status(HttpStatus.CREATED).json(creator.toJson(showAll = true))
        }
    }

    override fun updateEndpoint(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getInput()
            val exists = Creator.find {
                CreatorTable.nameCol eq input.name
            }.firstOrNull()
            if (exists != null && exists != resource) {
                throw ConflictResponse(message = "Creator already exists")
            }
            resource.image = input.image
            resource.name = input.name
            resource.summary = input.summary
            input.credits.forEach {
                val book = Book.findById(it.bookId)
                    ?: throw NotFoundResponse("Unable to find Book: `${it.bookId}`")
                val role = Role.findById(it.roleId)
                    ?: throw NotFoundResponse("Unable to find Role: `${it.roleId}`")
                Credit.find {
                    (CreditTable.bookCol eq book.id) and
                        (CreditTable.creatorCol eq resource.id) and
                        (CreditTable.roleCol eq role.id)
                }.firstOrNull() ?: Credit.new {
                    this.book = book
                    this.creator = resource
                    this.role = role
                }
            }

            ctx.json(resource.toJson(showAll = true))
        }
    }
}
