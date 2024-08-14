package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Creator
import github.buriedincode.bookshelf.models.CreatorInput
import github.buriedincode.bookshelf.models.Credit
import github.buriedincode.bookshelf.models.Role
import github.buriedincode.bookshelf.routers.api.BookApiRouter.getResource
import github.buriedincode.bookshelf.tables.CreatorTable
import github.buriedincode.bookshelf.tables.CreditTable
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.bodyAsClass
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.and

object CreatorApiRouter : BaseApiRouter<Creator>(entity = Creator), Logging {
    override fun list(ctx: Context) {
        Utils.query {
            var resources = Creator.all().toList()
            ctx.queryParam("book-id")?.toLongOrNull()?.let {
                Book.findById(it)?.let { book ->
                    resources = resources.filter { book in it.credits.map { it.book } }
                }
            }
            ctx.queryParam("has-image")?.lowercase()?.toBooleanStrictOrNull()?.let { image ->
                resources = resources.filter { it.imageUrl != null == image }
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

    override fun create(ctx: Context) {
        Utils.query {
            val body = ctx.bodyAsClass<CreatorInput>()
            val exists = Creator
                .find {
                    CreatorTable.nameCol eq body.name
                }.firstOrNull()
            if (exists != null) {
                throw ConflictResponse("Creator already exists")
            }
            val creator = Creator.new {
                imageUrl = body.imageUrl
                name = body.name
                summary = body.summary
            }
            body.credits.forEach {
                val book = Book.findById(it.bookId) ?: throw NotFoundResponse("Book not found.")
                val role = Role.findById(it.roleId) ?: throw NotFoundResponse("Role not found.")
                Credit
                    .find {
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

    override fun update(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<CreatorInput>()
            val exists = Creator
                .find {
                    CreatorTable.nameCol eq body.name
                }.firstOrNull()
            if (exists != null && exists != resource) {
                throw ConflictResponse("Creator already exists")
            }
            resource.imageUrl = body.imageUrl
            resource.name = body.name
            resource.summary = body.summary
            body.credits.forEach {
                val book = Book.findById(it.bookId) ?: throw NotFoundResponse("Book not found.")
                val role = Role.findById(it.roleId) ?: throw NotFoundResponse("Role not found.")
                Credit
                    .find {
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

    override fun delete(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            resource.credits.forEach {
                it.delete()
            }
            resource.delete()
            ctx.status(HttpStatus.NO_CONTENT)
        }
    }

    fun addCredit(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<CreatorInput.Credit>()

            val book = Book.findById(body.bookId) ?: throw NotFoundResponse("Book not found.")
            val role = Role.findById(body.roleId) ?: throw NotFoundResponse("Role not found.")
            Credit
                .find {
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
    }

    fun removeCredit(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<CreatorInput.Credit>()

            val book = Book.findById(body.bookId) ?: throw NotFoundResponse("Book not found.")
            val role = Role.findById(body.roleId) ?: throw NotFoundResponse("Role not found.")
            val credit = Credit
                .find {
                    (CreditTable.bookCol eq book.id) and
                        (CreditTable.creatorCol eq resource.id) and
                        (CreditTable.roleCol eq role.id)
                }.firstOrNull() ?: throw BadRequestResponse("Unable to find Credit")
            credit.delete()

            ctx.status(HttpStatus.NO_CONTENT)
        }
    }
}
