package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Creator
import github.buriedincode.bookshelf.models.CreatorCreditInput
import github.buriedincode.bookshelf.models.CreatorInput
import github.buriedincode.bookshelf.models.Credit
import github.buriedincode.bookshelf.models.Role
import github.buriedincode.bookshelf.tables.CreatorTable
import github.buriedincode.bookshelf.tables.CreditTable
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.bodyValidator
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.and

object CreatorApiRouter : Logging {
    fun listEndpoint(ctx: Context): Unit =
        Utils.query {
            var creators = Creator.all().toList()
            val name = ctx.queryParam("name")
            if (name != null) {
                creators = creators.filter {
                    it.name.contains(name, ignoreCase = true) || name.contains(it.name, ignoreCase = true)
                }
            }
            ctx.json(creators.sorted().map { it.toJson() })
        }

    private fun Context.getInput(): CreatorInput =
        this.bodyValidator<CreatorInput>()
            .check({ it.name.isNotBlank() }, error = "Name must not be empty")
            .get()

    fun createEndpoint(ctx: Context): Unit =
        Utils.query {
            val input = ctx.getInput()
            val exists = Creator.find {
                CreatorTable.nameCol eq input.name
            }.firstOrNull()
            if (exists != null) {
                throw ConflictResponse(message = "Creator already exists")
            }
            val creator = Creator.new {
                imageUrl = input.imageUrl
                name = input.name
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

    private fun Context.getResource(): Creator {
        return this.pathParam("creator-id").toLongOrNull()?.let {
            Creator.findById(id = it) ?: throw NotFoundResponse(message = "Unable to find Creator: `$it`")
        } ?: throw BadRequestResponse(message = "Unable to find Creator: `${this.pathParam("creator-id")}`")
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
            val exists = Creator.find {
                CreatorTable.nameCol eq input.name
            }.firstOrNull()
            if (exists != null && exists != resource) {
                throw ConflictResponse(message = "Creator already exists")
            }
            resource.imageUrl = input.imageUrl
            resource.name = input.name

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

    private fun Context.getCreditInput(): CreatorCreditInput =
        this.bodyValidator<CreatorCreditInput>()
            .get()

    fun addCredit(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getCreditInput()
            val book = Book.findById(id = input.bookId)
                ?: throw NotFoundResponse(message = "Unable to find Book: `${input.bookId}`")
            val role = Role.findById(id = input.roleId)
                ?: throw NotFoundResponse(message = "Unable to find Role: `${input.roleId}`")
            Credit.find {
                (CreditTable.bookCol eq book.id) and
                    (CreditTable.creatorCol eq resource.id) and
                    (CreditTable.roleCol eq role.id)
            }.firstOrNull() ?: Credit.new {
                this.book = book
                this.creator = resource
                this.role = role
            }

            ctx.json(resource.toJson(showAll = true))
        }

    fun removeCredit(ctx: Context): Unit =
        Utils.query {
            val resource = ctx.getResource()
            val input = ctx.getCreditInput()
            val book = Book.findById(id = input.bookId)
                ?: throw NotFoundResponse(message = "Unable to find Book: `${input.bookId}`")
            val role = Role.findById(id = input.roleId)
                ?: throw NotFoundResponse(message = "Unable to find Role: `${input.roleId}`")
            val credit = Credit.find {
                (CreditTable.bookCol eq book.id) and
                    (CreditTable.creatorCol eq resource.id) and
                    (CreditTable.roleCol eq role.id)
            }.firstOrNull() ?: throw BadRequestResponse(message = "Book Creator does not have this role")
            credit.delete()

            ctx.json(resource.toJson(showAll = true))
        }
}
