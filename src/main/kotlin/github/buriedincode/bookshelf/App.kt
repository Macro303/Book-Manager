package github.buriedincode.bookshelf

import github.buriedincode.bookshelf.controllers.*
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.Genre
import github.buriedincode.bookshelf.models.Publisher
import github.buriedincode.bookshelf.models.Series
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.NotFoundResponse
import io.javalin.openapi.OpenApiContact
import io.javalin.openapi.OpenApiInfo
import io.javalin.openapi.OpenApiLicense
import io.javalin.openapi.OpenApiServer
import io.javalin.openapi.plugin.OpenApiConfiguration
import io.javalin.openapi.plugin.OpenApiPlugin
import io.javalin.openapi.plugin.redoc.ReDocConfiguration
import io.javalin.openapi.plugin.redoc.ReDocPlugin
import io.javalin.openapi.plugin.swagger.SwaggerConfiguration
import io.javalin.openapi.plugin.swagger.SwaggerPlugin
import org.apache.logging.log4j.kotlin.logger
import org.jetbrains.exposed.dao.load

private const val VERSION = "0.0.0"

fun main() {
    val logger = logger("github.buriedincode.bookshelf.App")
    val app = Javalin.create {
        it.requestLogger.http { ctx, ms ->
            logger.info("${ctx.method()} - ${ctx.path()} - ${ctx.statusCode()} - ${ctx.ip()} => ${ms}ms")
        }
        it.routing.ignoreTrailingSlashes = true
        it.routing.treatMultipleSlashesAsSingleSlash = true
        it.staticFiles.add {
            it.hostedPath = "/static"
        }
        it.plugins.register(OpenApiPlugin(OpenApiConfiguration().apply {
            info = OpenApiInfo().apply {
                title = "Bookshelf API"
                description = "Lots and lots of well described and documented APIs."
                contact = OpenApiContact().apply {
                    name = "Jonah Jackson"
                    url = "https://github.com/Buried-In-Code/Bookshelf"
                    email = "BuriedInCode@tuta.io"
                }
                license = OpenApiLicense().apply {
                    name = "MIT License"
                    identifier = "MIT"
                }
                version = VERSION
            }
            servers = arrayOf(OpenApiServer().apply {
                url = "http://localhost:8003/api/v${VERSION.split(".")[0]}/"
                description = "Local DEV Server"
            })
        }))
        it.plugins.register(SwaggerPlugin(SwaggerConfiguration().apply {
            uiPath = "/docs"
        }))
        it.plugins.register(ReDocPlugin(ReDocConfiguration()))
    }
    app.routes {
        path("/") {
            get {
                it.render("index.kte")
            }
            path("books") {
                get {
                    Utils.query {
                        val books = Book.all().toList()
                        it.render(filePath = "list_books.kte", mapOf("books" to books))
                    }
                }
                path("{book-id}") {
                    get {
                        val bookId = it.pathParam("book-id").toLongOrNull()
                            ?: throw NotFoundResponse(message = "Book not found")
                        Utils.query {
                            val book = Book.findById(id = bookId)
                                ?.load(Book::genres, Book::publisher, Book::readers, Book::series, Book::wishers)
                                ?: throw NotFoundResponse(message = "Book not found")
                            it.render(filePath = "view_book.kte", mapOf("book" to book))
                        }
                    }
                    get("edit") {
                        val bookId = it.pathParam("book-id").toLongOrNull()
                            ?: throw NotFoundResponse(message = "Book not found")
                        Utils.query {
                            val book = Book.findById(id = bookId)
                                ?.load(Book::genres, Book::publisher, Book::readers, Book::series, Book::wishers)
                                ?: throw NotFoundResponse(message = "Book not found")
                            val genres = Genre.all().toList().filterNot { it in book.genres }
                            val publishers = Publisher.all().toList().filterNot { it == book.publisher }
                            val series = Series.all().toList().filterNot { it in book.series.map { it.series } }
                            it.render(
                                filePath = "edit_book.kte", mapOf(
                                    "book" to book,
                                    "genres" to genres,
                                    "publishers" to publishers,
                                    "series" to series
                                )
                            )
                        }
                    }
                }
            }
        }
        path("api") {
            path("v${VERSION.split(".")[0]}") {
                path("books") {
                    get(BookController::listBooks)
                    post(BookController::createBook)
                    path("{book-id}") {
                        get(BookController::getBook)
                        put(BookController::updateBook)
                        delete(BookController::deleteBook)
                    }
                }
                path("genres") {
                    get(GenreController::listGenres)
                    post(GenreController::createGenre)
                    path("{genre-id}") {
                        get(GenreController::getGenre)
                        put(GenreController::updateGenre)
                        delete(GenreController::deleteGenre)
                    }
                }
                path("publishers") {
                    get(PublisherController::listPublishers)
                    post(PublisherController::createPublisher)
                    path("{publisher-id}") {
                        get(PublisherController::getPublisher)
                        put(PublisherController::updatePublisher)
                        delete(PublisherController::deletePublisher)
                    }
                }
                path("series") {
                    get(SeriesController::listSeries)
                    post(SeriesController::createSeries)
                    path("{series-id}") {
                        get(SeriesController::getSeries)
                        put(SeriesController::updateSeries)
                        delete(SeriesController::deleteSeries)
                    }
                }
                path("users") {
                    get(UserController::listUsers)
                    post(UserController::createUser)
                    path("{user-id}") {
                        get(UserController::getUser)
                        put(UserController::updateUser)
                        delete(UserController::deleteUser)
                    }
                }
            }
        }
    }
    app.start(Settings.INSTANCE.websiteHost, Settings.INSTANCE.websitePort)
}
