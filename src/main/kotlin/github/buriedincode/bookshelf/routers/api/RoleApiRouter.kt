package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Creator
import github.buriedincode.bookshelf.models.Credit
import github.buriedincode.bookshelf.models.Role
import github.buriedincode.bookshelf.models.RoleInput
import github.buriedincode.bookshelf.tables.CreditTable
import github.buriedincode.bookshelf.tables.RoleTable
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.bodyValidator
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.and
import github.buriedincode.bookshelf.models.RoleInput.Credit as CreditInput

object RoleApiRouter : Logging {
    fun listEndpoint(ctx: Context): Unit =
        Utils.query {
            var roles = Role.all().toList()
            val title = ctx.queryParam("title")
            if (title != null) {
                roles = roles.filter {
                    it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true)
                }
            }
            ctx.json(roles.sorted().map { it.toJson() })
        }

    private fun Context.getInput(): RoleInput =
        this.bodyValidator<RoleInput>()
            .check({ it.title.isNotBlank() }, error = "Title must not be empty")
            .get()

    fun createEndpoint(ctx: Context): Unit =
        Utils.query {
            val input = ctx.getInput()
            val exists = Role.find {
                RoleTable.titleCol eq input.title
            }.firstOrNull()
            if (exists != null) {
                throw ConflictResponse(message = "Role already exists")
            }
            val role = Role.new {
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

    private fun Context.getResource(): Role {
        return this.pathParam("role-id").toLongOrNull()?.let {
            Role.findById(id = it) ?: throw NotFoundResponse(message = "Unable to find Role: `$it`")
        } ?: throw BadRequestResponse(message = "Unable to find Role: `${this.pathParam("role-id")}`")
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
            val exists = Role.find {
                RoleTable.titleCol eq input.title
            }.firstOrNull()
            if (exists != null && exists != resource) {
                throw ConflictResponse(message = "Role already exists")
            }
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

    fun deleteEndpoint(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            resource.credits.forEach {
                it.delete()
            }
            resource.delete()
            ctx.status(HttpStatus.NO_CONTENT)
        }

    private fun Context.getCreditInput(): CreditInput =
        this.bodyValidator<CreditInput>()
            .get()

    fun addCredit(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getCreditInput()
            val book = Book.findById(id = input.bookId)
                ?: throw NotFoundResponse(message = "Unable to find Book: `${input.bookId}`")
            val creator = Creator.findById(id = input.creatorId)
                ?: throw NotFoundResponse(message = "Unable to find Creator: `${input.creatorId}`")
            Credit.find {
                (CreditTable.bookCol eq book.id) and
                    (CreditTable.creatorCol eq creator.id) and
                    (CreditTable.roleCol eq resource.id)
            }.firstOrNull() ?: Credit.new {
                this.book = book
                this.creator = creator
                this.role = resource
            }

            ctx.json(resource.toJson(showAll = true))
        }

    fun removeCredit(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getCreditInput()
            val book = Book.findById(id = input.bookId)
                ?: throw NotFoundResponse(message = "Unable to find Book: `${input.bookId}`")
            val creator = Creator.findById(id = input.creatorId)
                ?: throw NotFoundResponse(message = "Unable to find Creator: `${input.creatorId}`")
            val credit = Credit.find {
                (CreditTable.bookCol eq book.id) and
                    (CreditTable.creatorCol eq creator.id) and
                    (CreditTable.roleCol eq resource.id)
            }.firstOrNull() ?: throw BadRequestResponse(message = "Book Creator does not have this role")
            credit.delete()

            ctx.json(resource.toJson(showAll = true))
        }
}
