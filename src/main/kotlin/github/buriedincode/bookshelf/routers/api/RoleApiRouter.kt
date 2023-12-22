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
import io.javalin.http.bodyAsClass
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

    override fun createEndpoint(ctx: Context) {
        Utils.query {
            val body = ctx.bodyAsClass<RoleInput>()
            val exists = Role.find {
                RoleTable.titleCol eq body.title
            }.firstOrNull()
            if (exists != null) {
                throw ConflictResponse("Role already exists")
            }
            val resource = Role.new {
                this.summary = body.summary
                this.title = body.title
            }
            body.credits.forEach {
                val book = Book.findById(it.bookId)
                    ?: throw NotFoundResponse("No Book found.")
                val creator = Creator.findById(it.creatorId)
                    ?: throw NotFoundResponse("No Creator found.")
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

            ctx.status(HttpStatus.CREATED).json(resource.toJson(showAll = true))
        }
    }

    override fun updateEndpoint(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<RoleInput>()
            val exists = Role.find {
                RoleTable.titleCol eq body.title
            }.firstOrNull()
            if (exists != null && exists != resource) {
                throw ConflictResponse("Role already exists")
            }
            resource.summary = body.summary
            resource.title = body.title
            body.credits.forEach {
                val book = Book.findById(it.bookId)
                    ?: throw NotFoundResponse("No Book found.")
                val creator = Creator.findById(it.creatorId)
                    ?: throw NotFoundResponse("No Creator found.")
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

    fun addCredit(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<RoleInput.Credit>()
            val book = Book.findById(body.bookId)
                ?: throw NotFoundResponse("No Book found.")
            val creator = Creator.findById(body.creatorId)
                ?: throw NotFoundResponse("No Creator found.")
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
    }

    fun removeCredit(ctx: Context) {
        Utils.query {
            val resource = ctx.getResource()
            val body = ctx.bodyAsClass<RoleInput.Credit>()
            val book = Book.findById(body.bookId)
                ?: throw NotFoundResponse("No Book found.")
            val creator = Book.findById(body.creatorId)
                ?: throw NotFoundResponse("No Creator found.")
            val credit = Credit.find {
                (CreditTable.bookCol eq book.id) and
                    (CreditTable.creatorCol eq creator.id) and
                    (CreditTable.roleCol eq resource.id)
            }.firstOrNull() ?: throw BadRequestResponse("Credit not found.")
            credit.delete()

            ctx.status(HttpStatus.NO_CONTENT)
        }
    }
}
