package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Creator
import github.buriedincode.bookshelf.models.Credit
import github.buriedincode.bookshelf.models.Role
import github.buriedincode.bookshelf.models.RoleInput
import github.buriedincode.bookshelf.tables.CreditTable
import github.buriedincode.bookshelf.tables.RoleTable
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.bodyValidator
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.and

object RoleApiRouter : BaseApiRouter<Role>(entity = Role), Logging {
    override fun listEndpoint(ctx: Context) {
        Utils.query {
            var resources = Role.all().toList()
            ctx.queryParam("book-id")?.toLongOrNull()?.let {
                Book.findById(it)?.let { book ->
                    resources = resources.filter { book in it.credits.map { it.book } }
                }
            }
            ctx.queryParam("creator-id")?.toLongOrNull()?.let {
                Creator.findById(it)?.let { creator ->
                    resources = resources.filter { creator in it.credits.map { it.creator } }
                }
            }
            ctx.queryParam("title")?.let { title ->
                resources = resources.filter { it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true) }
            }
            ctx.json(resources.sorted().map { it.toJson() })
        }
    }

    private fun Context.getInput(): RoleInput =
        this.bodyValidator<RoleInput>()
            .check({ it.title.isNotBlank() }, error = "Title must not be empty")
            .get()

    override fun createEndpoint(ctx: Context) {
        Utils.query {
            val input = ctx.getInput()
            val exists = Role.find {
                RoleTable.titleCol eq input.title
            }.firstOrNull()
            if (exists != null) {
                throw ConflictResponse(message = "Role already exists")
            }
            val role = Role.new {
                summary = input.summary
                title = input.title
            }
            input.credits.forEach {
                val book = Book.findById(id = it.bookId)
                    ?: throw NotFoundResponse(message = "Unable to find Book: `${it.bookId}`")
                val creator = Creator.findById(id = it.creatorId)
                    ?: throw NotFoundResponse(message = "Unable to find Creator: `${it.creatorId}`")
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

            ctx.status(HttpStatus.CREATED).json(role.toJson(showAll = true))
        }
    }

    override fun updateEndpoint(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getInput()
            val exists = Role.find {
                RoleTable.titleCol eq input.title
            }.firstOrNull()
            if (exists != null && exists != resource) {
                throw ConflictResponse(message = "Role already exists")
            }
            resource.summary = input.summary
            resource.title = input.title
            input.credits.forEach {
                val book = Book.findById(id = it.bookId)
                    ?: throw NotFoundResponse(message = "Unable to find Book: `${it.bookId}`")
                val creator = Creator.findById(id = it.creatorId)
                    ?: throw NotFoundResponse(message = "Unable to find Creator: `${it.creatorId}`")
                Credit.find {
                    (CreditTable.bookCol eq book.id) and
                        (CreditTable.creatorCol eq creator.id) and
                        (CreditTable.roleCol eq resource.id)
                }.firstOrNull() ?: Credit.new {
                    this.book = book
                    this.creator = creator
                    this.role = resource
                }
            }

            ctx.json(resource.toJson(showAll = true))
        }
    }

    override fun deleteEndpoint(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            resource.credits.forEach {
                it.delete()
            }
            resource.delete()
            ctx.status(HttpStatus.NO_CONTENT)
        }
    }
}
