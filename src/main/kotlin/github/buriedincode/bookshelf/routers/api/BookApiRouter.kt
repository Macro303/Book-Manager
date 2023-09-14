package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.ErrorResponse
import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.Utils.asEnumOrNull
import github.buriedincode.bookshelf.docs.BookEntry
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.BookCreatorRole
import github.buriedincode.bookshelf.models.BookCreditInput
import github.buriedincode.bookshelf.models.BookImport
import github.buriedincode.bookshelf.models.BookInput
import github.buriedincode.bookshelf.models.BookSeries
import github.buriedincode.bookshelf.models.BookSeriesInput
import github.buriedincode.bookshelf.models.Creator
import github.buriedincode.bookshelf.models.Format
import github.buriedincode.bookshelf.models.Genre
import github.buriedincode.bookshelf.models.IdValue
import github.buriedincode.bookshelf.models.Publisher
import github.buriedincode.bookshelf.models.ReadBook
import github.buriedincode.bookshelf.models.Role
import github.buriedincode.bookshelf.models.Series
import github.buriedincode.bookshelf.models.User
import github.buriedincode.bookshelf.services.OpenLibrary
import github.buriedincode.bookshelf.services.openlibrary.Edition
import github.buriedincode.bookshelf.services.openlibrary.Work
import github.buriedincode.bookshelf.tables.BookCreatorRoleTable
import github.buriedincode.bookshelf.tables.BookSeriesTable
import github.buriedincode.bookshelf.tables.BookTable
import github.buriedincode.bookshelf.tables.CreatorTable
import github.buriedincode.bookshelf.tables.GenreTable
import github.buriedincode.bookshelf.tables.PublisherTable
import github.buriedincode.bookshelf.tables.ReadBookTable
import github.buriedincode.bookshelf.tables.RoleTable
import io.javalin.apibuilder.CrudHandler
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.NotFoundResponse
import io.javalin.http.NotImplementedResponse
import io.javalin.http.bodyValidator
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiRequestBody
import io.javalin.openapi.OpenApiResponse
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import java.time.LocalDate

object BookApiRouter : CrudHandler, Logging {
    private fun getResource(resourceId: String): Book {
        return resourceId.toLongOrNull()?.let {
            Book.findById(id = it) ?: throw NotFoundResponse(message = "Book not found")
        } ?: throw BadRequestResponse(message = "Invalid Book Id")
    }

    private fun Context.getBody(): BookInput = this.bodyValidator<BookInput>()
        .check({ it.title.isNotBlank() }, error = "Title must not be empty")
        .check({ it.series.all { it.seriesId > 0 } }, error = "bookId must be greater than 0")
        .get()

    @OpenApi(
        description = "List all Books",
        methods = [HttpMethod.GET],
        operationId = "listBooks",
        path = "/books",
        queryParams = [
            OpenApiParam(name = "creator-id", type = Long::class),
            OpenApiParam(name = "format", type = String::class),
            OpenApiParam(name = "genre-id", type = Long::class),
            OpenApiParam(name = "publisher-id", type = Long::class),
            OpenApiParam(name = "series-id", type = Long::class),
            OpenApiParam(name = "title", type = String::class),
        ],
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(Array<BookEntry>::class)]),
        ],
        summary = "List all Books",
        tags = ["Book"],
    )
    override fun getAll(ctx: Context): Unit = Utils.query {
        var books = Book.all().toList()
        val creator = ctx.queryParam("creator-id")?.toLongOrNull()?.let {
            Creator.findById(id = it)
        }
        if (creator != null) {
            books = books.filter {
                creator in it.credits.map { it.creator }
            }
        }
        val format: Format? = ctx.queryParam("format")?.asEnumOrNull<Format>()
        if (format != null) {
            books = books.filter {
                it.format == format
            }
        }
        val genre = ctx.queryParam("genre-id")?.toLongOrNull()?.let {
            Genre.findById(id = it)
        }
        if (genre != null) {
            books = books.filter {
                genre in it.genres
            }
        }
        val publisher = ctx.queryParam("publisher-id")?.toLongOrNull()?.let {
            Publisher.findById(id = it)
        }
        if (publisher != null) {
            books = books.filter {
                it.publisher == publisher
            }
        }
        val series = ctx.queryParam("series-id")?.toLongOrNull()?.let {
            Series.findById(id = it)
        }
        if (series != null) {
            books = books.filter {
                series in it.series.map { it.series }
            }
        }
        val title = ctx.queryParam("title")
        if (title != null) {
            books = books.filter {
                (
                    it.title.contains(title, ignoreCase = true) || title.contains(
                        it.title,
                        ignoreCase = true,
                    )
                    ) || (
                    it.subtitle?.let {
                        it.contains(title, ignoreCase = true) || title.contains(it, ignoreCase = true)
                    } ?: false
                    )
            }
        }
        ctx.json(books.sorted().map { it.toJson() })
    }

    @OpenApi(
        description = "Create Book",
        methods = [HttpMethod.POST],
        operationId = "createBook",
        path = "/books",
        requestBody = OpenApiRequestBody(content = [OpenApiContent(BookInput::class)], required = true),
        responses = [
            OpenApiResponse(status = "201", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Create Book",
        tags = ["Book"],
    )
    override fun create(ctx: Context): Unit = Utils.query {
        val body = ctx.getBody()
        val exists = Book.find {
            (BookTable.titleCol eq body.title) and (BookTable.subtitleCol eq body.subtitle)
        }.firstOrNull()
        if (exists != null) {
            throw ConflictResponse(message = "Book already exists")
        }
        val book = Book.new {
            description = body.description
            format = body.format
            genres = SizedCollection(
                body.genreIds.map {
                    Genre.findById(id = it)
                        ?: throw NotFoundResponse(message = "Genre not found")
                },
            )
            goodreadsId = body.goodreadsId
            googleBooksId = body.googleBooksId
            imageUrl = body.imageUrl
            isCollected = body.isCollected
            isbn = body.isbn
            libraryThingId = body.libraryThingId
            openLibraryId = body.openLibraryId
            publishDate = body.publishDate
            publisher = body.publisherId?.let {
                Publisher.findById(id = it)
                    ?: throw NotFoundResponse(message = "Publisher not found")
            }
            subtitle = body.subtitle
            title = body.title
            wishers = SizedCollection(
                body.wisherIds.map {
                    User.findById(id = it)
                        ?: throw NotFoundResponse(message = "Wisher not found")
                },
            )
        }
        body.credits.forEach {
            val creator = Creator.findById(id = it.creatorId)
                ?: throw NotFoundResponse(message = "Creator not found")
            val role = Role.findById(id = it.roleId)
                ?: throw NotFoundResponse(message = "Role not found")
            BookCreatorRole.new {
                this.book = book
                this.creator = creator
                this.role = role
            }
        }
        body.readers.forEach {
            val user = User.findById(id = it.userId)
                ?: throw NotFoundResponse(message = "Reader not found")
            ReadBook.new {
                this.book = book
                this.user = user
            }
        }
        body.series.forEach {
            val series = Series.findById(id = it.seriesId)
                ?: throw NotFoundResponse(message = "Series not found")
            BookSeries.new {
                this.book = book
                this.series = series
                number = if (it.number == 0) null else it.number
            }
        }

        ctx.status(HttpStatus.CREATED).json(book.toJson(showAll = true))
    }

    @OpenApi(
        description = "Get Book by id",
        methods = [HttpMethod.GET],
        operationId = "getBook",
        path = "/books/{book-id}",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Get Book by id",
        tags = ["Book"],
    )
    override fun getOne(ctx: Context, resourceId: String): Unit = Utils.query {
        val book = getResource(resourceId = resourceId)
        ctx.json(book.toJson(showAll = true))
    }

    @OpenApi(
        description = "Update Book",
        methods = [HttpMethod.PATCH],
        operationId = "updateBook",
        path = "/books/{book-id}",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(BookInput::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Update Book",
        tags = ["Book"],
    )
    override fun update(ctx: Context, resourceId: String): Unit = Utils.query {
        val book = getResource(resourceId = resourceId)
        val body = ctx.getBody()
        val exists = Book.find {
            (BookTable.titleCol eq body.title) and (BookTable.subtitleCol eq body.subtitle)
        }.firstOrNull()
        if (exists != null && exists != book) {
            throw ConflictResponse(message = "Book already exists")
        }
        book.description = body.description
        book.format = body.format
        book.genres = SizedCollection(
            body.genreIds.map {
                Genre.findById(id = it)
                    ?: throw NotFoundResponse(message = "Genre not found")
            },
        )
        book.goodreadsId = body.goodreadsId
        book.googleBooksId = body.googleBooksId
        book.imageUrl = body.imageUrl
        book.isCollected = body.isCollected
        book.isbn = body.isbn
        book.libraryThingId = body.libraryThingId
        book.openLibraryId = body.openLibraryId
        book.publishDate = body.publishDate
        book.publisher = body.publisherId?.let {
            Publisher.findById(id = it)
                ?: throw NotFoundResponse(message = "Publisher not found")
        }
        book.subtitle = body.subtitle
        book.title = body.title
        book.wishers = SizedCollection(
            body.wisherIds.map {
                User.findById(id = it)
                    ?: throw NotFoundResponse(message = "User not found")
            },
        )
        body.credits.forEach {
            val creator = Creator.findById(id = it.creatorId)
                ?: throw NotFoundResponse(message = "Creator not found")
            val role = Role.findById(id = it.roleId)
                ?: throw NotFoundResponse(message = "Role not found")
            val bookCreatorRole = BookCreatorRole.find {
                (BookCreatorRoleTable.bookCol eq book.id) and
                    (BookCreatorRoleTable.creatorCol eq creator.id) and
                    (BookCreatorRoleTable.roleCol eq role.id)
            }.firstOrNull()
            if (bookCreatorRole == null) {
                BookCreatorRole.new {
                    this.book = book
                    this.creator = creator
                    this.role = role
                }
            }
        }
        body.readers.forEach {
            val user = User.findById(id = it.userId)
                ?: throw NotFoundResponse(message = "Reader not found")
            val readBook = ReadBook.find {
                (ReadBookTable.bookCol eq book.id) and (ReadBookTable.userCol eq user.id)
            }.firstOrNull()
            if (readBook == null) {
                ReadBook.new {
                    this.book = book
                    this.user = user
                }
            }
        }
        body.series.forEach {
            val series = Series.findById(id = it.seriesId)
                ?: throw NotFoundResponse(message = "Series not found")
            val bookSeries = BookSeries.find {
                (BookSeriesTable.bookCol eq book.id) and (BookSeriesTable.seriesCol eq series.id)
            }.firstOrNull()
            if (bookSeries == null) {
                BookSeries.new {
                    this.book = book
                    this.series = series
                    number = if (it.number == 0) null else it.number
                }
            } else {
                bookSeries.number = if (it.number == 0) null else it.number
            }
        }

        ctx.json(book.toJson(showAll = true))
    }

    @OpenApi(
        description = "Delete Book",
        methods = [HttpMethod.DELETE],
        operationId = "deleteBook",
        path = "/books/{book-id}",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        responses = [
            OpenApiResponse(status = "204"),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Delete Book",
        tags = ["Book"],
    )
    override fun delete(ctx: Context, resourceId: String): Unit = Utils.query {
        val book = getResource(resourceId = resourceId)
        book.credits.forEach {
            it.delete()
        }
        book.readers.forEach {
            it.delete()
        }
        book.series.forEach {
            it.delete()
        }
        book.delete()
        ctx.status(HttpStatus.NO_CONTENT)
    }

    private fun Context.getImportBody(): BookImport = this.bodyValidator<BookImport>()
        .check(
            {
                !it.goodreadsId.isNullOrBlank() ||
                    !it.googleBooksId.isNullOrBlank() ||
                    !it.isbn.isNullOrBlank() ||
                    !it.libraryThingId.isNullOrBlank() ||
                    !it.openLibraryId.isNullOrBlank()
            },
            error = "At least 1 id to import must be specified",
        )
        .get()

    @OpenApi(
        description = "Import Book",
        methods = [HttpMethod.POST],
        operationId = "importBook",
        path = "/books/import",
        requestBody = OpenApiRequestBody(content = [OpenApiContent(BookImport::class)], required = true),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Import Book",
        tags = ["Book"],
    )
    fun importBook(ctx: Context): Unit = Utils.query {
        val body = ctx.getImportBody()

        val edition: Edition
        val work: Work
        if (body.openLibraryId != null) {
            val temp = OpenLibrary.getBook(editionId = body.openLibraryId)
            edition = temp.first
            work = temp.second
        } else if (body.isbn != null) {
            val temp = OpenLibrary.lookupBook(isbn = body.isbn)
            edition = temp.first
            work = temp.second
        } else {
            throw NotImplementedResponse(message = "Only import via OpenLibrary edition id or Isbn currently supported.")
        }
        val exists = if (edition.isbn == null) {
            Book.find {
                (
                    (BookTable.titleCol eq edition.title) and
                        (BookTable.subtitleCol eq edition.subtitle)
                    ) or (BookTable.openLibraryCol eq edition.editionId)
            }.firstOrNull()
        } else {
            Book.find {
                (
                    (BookTable.titleCol eq edition.title) and
                        (BookTable.subtitleCol eq edition.subtitle)
                    ) or (BookTable.openLibraryCol eq edition.editionId) or (BookTable.isbnCol eq edition.isbn)
            }.firstOrNull()
        }
        if (exists != null) {
            throw ConflictResponse(message = "This Book already exists in the database.")
        }

        val book = Book.new {
            description = edition.description ?: work.description
            format = Format.PAPERBACK // TODO
            genres = SizedCollection(
                edition.genres.map {
                    Genre.find {
                        GenreTable.titleCol eq it
                    }.firstOrNull() ?: Genre.new {
                        title = it
                    }
                },
            )
            goodreadsId = edition.identifiers.goodreads.firstOrNull()
            googleBooksId = edition.identifiers.google.firstOrNull()
            imageUrl = "https://covers.openlibrary.org/b/OLID/${edition.editionId}-L.jpg"
            isbn = edition.isbn
            isCollected = body.isCollected
            libraryThingId = edition.identifiers.librarything.firstOrNull()
            openLibraryId = edition.editionId
            publishDate = edition.publishDate
            edition.publishers.firstOrNull()?.let {
                publisher = Publisher.find {
                    PublisherTable.titleCol eq it
                }.firstOrNull() ?: Publisher.new {
                    title = it
                }
            }
            subtitle = edition.subtitle
            title = edition.title
            wishers = SizedCollection(
                body.wisherIds.map {
                    User.findById(id = it)
                        ?: throw NotFoundResponse(message = "User not found")
                },
            )
        }
        work.authors.map {
            OpenLibrary.getAuthor(authorId = it.authorId)
        }.map {
            val creator = Creator.find {
                CreatorTable.nameCol eq it.name
            }.firstOrNull() ?: Creator.new {
                name = it.name
            }
            it.photos.firstOrNull()?.let {
                creator.imageUrl = "https://covers.openlibrary.org/a/id/$it-L.jpg"
            }
            creator
        }.forEach {
            BookCreatorRole.new {
                this.book = book
                creator = it
                role = Role.find {
                    RoleTable.titleCol eq "Author"
                }.firstOrNull() ?: Role.new {
                    title = "Author"
                }
            }
        }
        edition.contributors.forEach {
            BookCreatorRole.new {
                this.book = book
                creator = Creator.find {
                    CreatorTable.nameCol eq it.name
                }.firstOrNull() ?: Creator.new {
                    name = it.name
                }
                role = Role.find {
                    RoleTable.titleCol eq it.role
                }.firstOrNull() ?: Role.new {
                    title = it.role
                }
            }
        }
        ctx.status(HttpStatus.CREATED).json(book.toJson(showAll = true))
    }

    @OpenApi(
        description = "Refresh Book",
        methods = [HttpMethod.PUT],
        operationId = "refreshBook",
        path = "/books/{book-id}/refresh",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Refresh Book",
        tags = ["Book"],
    )
    fun refreshBook(ctx: Context): Unit = Utils.query {
        val book = getResource(resourceId = ctx.pathParam("book-id"))

        val edition: Edition
        val work: Work
        if (book.openLibraryId != null) {
            val temp = OpenLibrary.getBook(editionId = book.openLibraryId!!)
            edition = temp.first
            work = temp.second
        } else if (book.isbn != null) {
            val temp = OpenLibrary.lookupBook(isbn = book.isbn!!)
            edition = temp.first
            work = temp.second
        } else {
            throw NotImplementedResponse(message = "Only refresh via OpenLibrary edition id or Isbn currently supported.")
        }
        val exists = if (edition.isbn == null) {
            Book.find {
                (
                    (BookTable.titleCol eq edition.title) and
                        (BookTable.subtitleCol eq edition.subtitle)
                    ) or (BookTable.openLibraryCol eq edition.editionId)
            }.firstOrNull()
        } else {
            Book.find {
                (
                    (BookTable.titleCol eq edition.title) and
                        (BookTable.subtitleCol eq edition.subtitle)
                    ) or (BookTable.openLibraryCol eq edition.editionId) or (BookTable.isbnCol eq edition.isbn)
            }.firstOrNull()
        }
        if (exists != null && exists != book) {
            throw ConflictResponse(message = "This Book already exists in the database.")
        }

        book.description = edition.description ?: work.description
        // book.format = Format.PAPERBACK
        book.genres = SizedCollection(
            edition.genres.map {
                Genre.find {
                    GenreTable.titleCol eq it
                }.firstOrNull() ?: Genre.new {
                    title = it
                }
            },
        )
        book.goodreadsId = edition.identifiers.goodreads.firstOrNull()
        book.googleBooksId = edition.identifiers.google.firstOrNull()
        book.imageUrl = "https://covers.openlibrary.org/b/OLID/${edition.editionId}-L.jpg"
        book.isbn = edition.isbn
        book.libraryThingId = edition.identifiers.librarything.firstOrNull()
        book.openLibraryId = edition.editionId
        book.publishDate = edition.publishDate
        edition.publishers.firstOrNull()?.let {
            book.publisher = Publisher.find {
                PublisherTable.titleCol eq it
            }.firstOrNull() ?: Publisher.new {
                title = it
            }
        }
        book.subtitle = edition.subtitle
        book.title = edition.title
        work.authors.map {
            OpenLibrary.getAuthor(authorId = it.authorId)
        }.map {
            val creator = Creator.find {
                CreatorTable.nameCol eq it.name
            }.firstOrNull() ?: Creator.new {
                name = it.name
            }
            it.photos.firstOrNull()?.let {
                creator.imageUrl = "https://covers.openlibrary.org/a/id/$it-L.jpg"
            }
            creator
        }.forEach {
            val role = Role.find {
                RoleTable.titleCol eq "Author"
            }.firstOrNull() ?: Role.new {
                title = "Author"
            }
            BookCreatorRole.find {
                (BookCreatorRoleTable.bookCol eq book.id) and
                    (BookCreatorRoleTable.creatorCol eq it.id) and
                    (BookCreatorRoleTable.roleCol eq role.id)
            }.firstOrNull() ?: BookCreatorRole.new {
                this.book = book
                creator = it
                this.role = role
            }
        }
        edition.contributors.forEach {
            val creator = Creator.find {
                CreatorTable.nameCol eq it.name
            }.firstOrNull() ?: Creator.new {
                name = it.name
            }
            val role = Role.find {
                RoleTable.titleCol eq it.role
            }.firstOrNull() ?: Role.new {
                title = it.role
            }
            BookCreatorRole.find {
                (BookCreatorRoleTable.bookCol eq book.id) and
                    (BookCreatorRoleTable.creatorCol eq creator.id) and
                    (BookCreatorRoleTable.roleCol eq role.id)
            }.firstOrNull() ?: BookCreatorRole.new {
                this.book = book
                this.creator = creator
                this.role = role
            }
        }

        ctx.json(book.toJson(showAll = true))
    }

    @OpenApi(
        description = "Collect Book",
        methods = [HttpMethod.PATCH],
        operationId = "collectBook",
        path = "/books/{book-id}/collect",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Collect Book",
        tags = ["Book"],
    )
    fun collectBook(ctx: Context): Unit = Utils.query {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        book.isCollected = true
        book.wishers = SizedCollection()
        ctx.json(book.toJson(showAll = true))
    }

    @OpenApi(
        description = "Discard Book",
        methods = [HttpMethod.DELETE],
        operationId = "discardBook",
        path = "/books/{book-id}/collect",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Discard Book",
        tags = ["Book"],
    )
    fun discardBook(ctx: Context): Unit = Utils.query {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        book.isCollected = false
        book.readers.forEach {
            it.delete()
        }
        ctx.json(book.toJson(showAll = true))
    }

    private fun Context.getIdValue(): IdValue = this.bodyValidator<IdValue>()
        .check({ it.id > 0 }, error = "Id must be greater than 0")
        .get()

    @OpenApi(
        description = "Add Reader to Book",
        methods = [HttpMethod.PATCH],
        operationId = "addReader",
        path = "/books/{book-id}/read",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Add Reader to Book",
        tags = ["Book"],
    )
    fun addReader(ctx: Context): Unit = Utils.query {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        if (!book.isCollected) {
            throw BadRequestResponse(message = "Book hasn't been collected to be able to read")
        }
        val body = ctx.getIdValue()
        val user = User.findById(id = body.id)
            ?: throw NotFoundResponse(message = "User not found")
        val exists = ReadBook.find {
            (ReadBookTable.bookCol eq book.id) and (ReadBookTable.userCol eq user.id)
        }.firstOrNull()
        if (exists != null) {
            throw BadRequestResponse(message = "Book has already been read by User")
        }
        ReadBook.new {
            this.book = book
            this.user = user
            readDate = LocalDate.now()
        }

        ctx.json(book.toJson(showAll = true))
    }

    @OpenApi(
        description = "Remove Reader from Book",
        methods = [HttpMethod.DELETE],
        operationId = "removeReader",
        path = "/books/{book-id}/read",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Remove Reader from Book",
        tags = ["Book"],
    )
    fun removeReader(ctx: Context): Unit = Utils.query {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        if (!book.isCollected) {
            throw BadRequestResponse(message = "Book hasn't been collected to be able to unread")
        }
        val body = ctx.getIdValue()
        val user = User.findById(id = body.id)
            ?: throw NotFoundResponse(message = "User not found")
        val exists = ReadBook.find {
            (ReadBookTable.bookCol eq book.id) and (ReadBookTable.userCol eq user.id)
        }.firstOrNull() ?: throw BadRequestResponse(message = "Book has not been read by this User.")
        exists.delete()

        ctx.json(book.toJson(showAll = true))
    }

    @OpenApi(
        description = "Add Wisher to Book",
        methods = [HttpMethod.PATCH],
        operationId = "addWisher",
        path = "/books/{book-id}/wish",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Add Wisher to Book",
        tags = ["Book"],
    )
    fun addWisher(ctx: Context): Unit = Utils.query {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        if (book.isCollected) {
            throw BadRequestResponse(message = "Book has already been collected")
        }
        val body = ctx.getIdValue()
        val user = User.findById(id = body.id)
            ?: throw NotFoundResponse(message = "User not found")
        if (user in book.wishers) {
            throw BadRequestResponse(message = "Book has already been wished by User")
        }
        val temp = book.wishers.toMutableList()
        temp.add(user)
        book.wishers = SizedCollection(temp)

        ctx.json(book.toJson(showAll = true))
    }

    @OpenApi(
        description = "Remove Wisher from Book",
        methods = [HttpMethod.DELETE],
        operationId = "removeWisher",
        path = "/books/{book-id}/wish",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Remove Wisher from Book",
        tags = ["Book"],
    )
    fun removeWisher(ctx: Context): Unit = Utils.query {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        if (book.isCollected) {
            throw BadRequestResponse(message = "Book has already been collected")
        }
        val body = ctx.getIdValue()
        val user = User.findById(id = body.id)
            ?: throw NotFoundResponse(message = "User not found")
        if (!book.wishers.contains(user)) {
            throw BadRequestResponse(message = "Book hasn't been wished by User")
        }
        val temp = book.wishers.toMutableList()
        temp.remove(user)
        book.wishers = SizedCollection(temp)

        ctx.json(book.toJson(showAll = true))
    }

    private fun Context.getBookCreditBody(): BookCreditInput = this.bodyValidator<BookCreditInput>()
        .get()

    @OpenApi(
        description = "Add Credit to Book",
        methods = [HttpMethod.PATCH],
        operationId = "addBookCredit",
        path = "/books/{book-id}/credits",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(BookCreditInput::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Add Credit to Book",
        tags = ["Book"],
    )
    fun addCredit(ctx: Context): Unit = Utils.query {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        val body = ctx.getBookCreditBody()
        val creator = Creator.findById(id = body.creatorId)
            ?: throw NotFoundResponse(message = "Creator not found")
        val role = Role.findById(id = body.roleId)
            ?: throw NotFoundResponse(message = "Role not found")
        val credit = BookCreatorRole.find {
            (BookCreatorRoleTable.bookCol eq book.id) and
                (BookCreatorRoleTable.creatorCol eq creator.id) and
                (BookCreatorRoleTable.roleCol eq role.id)
        }.firstOrNull()
        if (credit != null) {
            throw ConflictResponse(message = "Book Creator already has this role")
        } else {
            BookCreatorRole.new {
                this.book = book
                this.creator = creator
                this.role = role
            }
        }

        ctx.json(book.toJson(showAll = true))
    }

    @OpenApi(
        description = "Remove Credit from Book",
        methods = [HttpMethod.DELETE],
        operationId = "removeBookCredit",
        path = "/books/{book-id}/credits",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(BookCreditInput::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Remove Credit from Book",
        tags = ["Book"],
    )
    fun removeCredit(ctx: Context): Unit = Utils.query {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        val body = ctx.getBookCreditBody()
        val creator = Creator.findById(id = body.creatorId)
            ?: throw NotFoundResponse(message = "Creator not found")
        val role = Role.findById(id = body.roleId)
            ?: throw NotFoundResponse(message = "Role not found")
        val credit = BookCreatorRole.find {
            (BookCreatorRoleTable.bookCol eq book.id) and
                (BookCreatorRoleTable.creatorCol eq creator.id) and
                (BookCreatorRoleTable.roleCol eq role.id)
        }.firstOrNull() ?: throw NotFoundResponse(message = "Unable to find Book Creator Role")
        credit.delete()

        ctx.json(book.toJson(showAll = true))
    }

    @OpenApi(
        description = "Add Genre to Book",
        methods = [HttpMethod.PATCH],
        operationId = "addGereToBook",
        path = "/books/{book-id}/genres",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Add Genre to Book",
        tags = ["Book"],
    )
    fun addGenre(ctx: Context): Unit = Utils.query {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        val body = ctx.getIdValue()
        val genre = Genre.findById(id = body.id)
            ?: throw NotFoundResponse(message = "Genre not found")
        if (genre in book.genres) {
            throw ConflictResponse(message = "Genre is already linked to Book")
        }
        val temp = book.genres.toMutableList()
        temp.add(genre)
        book.genres = SizedCollection(temp)

        ctx.json(book.toJson(showAll = true))
    }

    @OpenApi(
        description = "Remove Genre from Book",
        methods = [HttpMethod.DELETE],
        operationId = "removeGenreFromBook",
        path = "/books/{book-id}/genres",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Remove Genre from Book",
        tags = ["Book"],
    )
    fun removeGenre(ctx: Context): Unit = Utils.query {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        val body = ctx.getIdValue()
        val genre = Genre.findById(id = body.id)
            ?: throw NotFoundResponse(message = "Genre not found")
        if (!book.genres.contains(genre)) {
            throw BadRequestResponse(message = "Genre is already linked to Book")
        }
        val temp = book.genres.toMutableList()
        temp.remove(genre)
        book.genres = SizedCollection(temp)

        ctx.json(book.toJson(showAll = true))
    }

    private fun Context.getBookSeriesBody(): BookSeriesInput = this.bodyValidator<BookSeriesInput>()
        .check({ it.seriesId > 0 }, error = "seriesId must be greater than 0")
        .get()

    @OpenApi(
        description = "Add Series to Book",
        methods = [HttpMethod.PATCH],
        operationId = "addSeriesToBook",
        path = "/books/{book-id}/series",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(BookSeriesInput::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Add Series to Book",
        tags = ["Book"],
    )
    fun addSeries(ctx: Context): Unit = Utils.query {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        val body = ctx.getBookSeriesBody()
        val series = Series.findById(id = body.seriesId)
            ?: throw NotFoundResponse(message = "Series not found")
        val bookSeries = BookSeries.find {
            (BookSeriesTable.bookCol eq book.id) and (BookSeriesTable.seriesCol eq series.id)
        }.firstOrNull()
        if (bookSeries != null) {
            throw ConflictResponse(message = "Series already is linked to Series")
        }
        BookSeries.new {
            this.book = book
            this.series = series
            number = body.number
        }

        ctx.json(book.toJson(showAll = true))
    }

    @OpenApi(
        description = "Remove Series from Book",
        methods = [HttpMethod.DELETE],
        operationId = "removeSeriesFromBook",
        path = "/books/{book-id}/series",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Remove Series from Book",
        tags = ["Book"],
    )
    fun removeSeries(ctx: Context): Unit = Utils.query {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        val body = ctx.getIdValue()
        val series = Series.findById(id = body.id)
            ?: throw NotFoundResponse(message = "Series not found")
        val bookSeries = BookSeries.find {
            (BookSeriesTable.bookCol eq book.id) and (BookSeriesTable.seriesCol eq series.id)
        }.firstOrNull() ?: throw NotFoundResponse(message = "Book isn't linked to Series")
        bookSeries.delete()

        ctx.json(book.toJson(showAll = true))
    }
}
