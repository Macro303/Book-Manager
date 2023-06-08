package github.buriedincode.bookshelf.routers.api

import github.buriedincode.bookshelf.ErrorResponse
import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.docs.BookEntry
import github.buriedincode.bookshelf.models.*
import github.buriedincode.bookshelf.services.OpenLibrary
import github.buriedincode.bookshelf.services.openlibrary.*
import github.buriedincode.bookshelf.tables.*
import io.javalin.apibuilder.*
import io.javalin.http.*
import io.javalin.openapi.*
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or

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
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(Array<BookEntry>::class)])
        ],
        summary = "List all Books",
        tags = ["Book"]
    )
    override fun getAll(ctx: Context): Unit = Utils.query(description = "List Books") {
        val books = Book.all()
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
        tags = ["Book"]
    )
    override fun create(ctx: Context): Unit = Utils.query(description = "Create Book") {
        val body = ctx.getBody()
        val exists = Book.find {
            (BookTable.titleCol eq body.title) and (BookTable.subtitleCol eq body.subtitle)
        }.firstOrNull()
        if (exists != null)
            throw ConflictResponse(message = "Book already exists")
        val book = Book.new {
            description = body.description
            format = body.format
            genres = SizedCollection(body.genreIds.map {
                Genre.findById(id = it)
                    ?: throw NotFoundResponse(message = "Genre not found")
            })
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
            readers = SizedCollection(body.readerIds.map {
                User.findById(id = it)
                    ?: throw NotFoundResponse(message = "User not found")
            })
            subtitle = body.subtitle
            title = body.title
            wishers = SizedCollection(body.wisherIds.map {
                User.findById(id = it)
                    ?: throw NotFoundResponse(message = "User not found")
            })
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
        tags = ["Book"]
    )
    override fun getOne(ctx: Context, resourceId: String): Unit = Utils.query(description = "Get Book") {
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
        tags = ["Book"]
    )
    override fun update(ctx: Context, resourceId: String): Unit = Utils.query(description = "Update Book") {
        val book = getResource(resourceId = resourceId)
        val body = ctx.getBody()
        val exists = Book.find {
            (BookTable.titleCol eq body.title) and (BookTable.subtitleCol eq body.subtitle)
        }.firstOrNull()
        if (exists != null && exists != book)
            throw ConflictResponse(message = "Book already exists")
        book.description = body.description
        book.format = body.format
        book.genres = SizedCollection(body.genreIds.map {
            Genre.findById(id = it)
                ?: throw NotFoundResponse(message = "Genre not found")
        })
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
        book.readers = SizedCollection(body.wisherIds.map {
            User.findById(id = it)
                ?: throw NotFoundResponse(message = "User not found")
        })
        book.subtitle = body.subtitle
        book.title = body.title
        book.wishers = SizedCollection(body.wisherIds.map {
            User.findById(id = it)
                ?: throw NotFoundResponse(message = "User not found")
        })
        body.credits.forEach {
            val creator = Creator.findById(id = it.creatorId)
                ?: throw NotFoundResponse(message = "Creator not found")
            val role = Role.findById(id = it.roleId)
                ?: throw NotFoundResponse(message = "Role not found")
            val bookCreatorRole = BookCreatorRole.find {
                (BookCreatorRoleTable.bookCol eq book.id) and (BookCreatorRoleTable.creatorCol eq creator.id) and (BookCreatorRoleTable.roleCol eq role.id)
            }.firstOrNull()
            if (bookCreatorRole == null)
                BookCreatorRole.new {
                    this.book = book
                    this.creator = creator
                    this.role = role
                }
        }
        body.series.forEach {
            val series = Series.findById(id = it.seriesId)
                ?: throw NotFoundResponse(message = "Series not found")
            val bookSeries = BookSeries.find {
                (BookSeriesTable.bookCol eq book.id) and (BookSeriesTable.seriesCol eq series.id)
            }.firstOrNull()
            if (bookSeries == null)
                BookSeries.new {
                    this.book = book
                    this.series = series
                    number = if (it.number == 0) null else it.number
                }
            else
                bookSeries.number = if (it.number == 0) null else it.number
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
        tags = ["Book"]
    )
    override fun delete(ctx: Context, resourceId: String): Unit = Utils.query(description = "Delete Book") {
        val book = getResource(resourceId = resourceId)
        book.delete()
        ctx.status(HttpStatus.NO_CONTENT)
    }

    private fun Context.getImportBody(): BookImport = this.bodyValidator<BookImport>()
        .check({ !it.goodreadsId.isNullOrBlank() || !it.googleBooksId.isNullOrBlank() || !it.isbn.isNullOrBlank() || !it.libraryThingId.isNullOrBlank() || !it.openLibraryId.isNullOrBlank() }, error = "At least 1 id to import must be specified")
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
        tags = ["Book"]
    )
    fun importBook(ctx: Context): Unit = Utils.query(description = "Import Book") {
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
        } else
            throw NotImplementedResponse(message = "Only import via OpenLibrary edition id or Isbn currently supported.")
        val exists = if (edition.isbn == null)
            Book.find {
                ((BookTable.titleCol eq edition.title) and (BookTable.subtitleCol eq edition.subtitle)) or (BookTable.openLibraryCol eq edition.editionId)
            }.firstOrNull()
        else
            Book.find {
                ((BookTable.titleCol eq edition.title) and (BookTable.subtitleCol eq edition.subtitle)) or (BookTable.openLibraryCol eq edition.editionId) or (BookTable.isbnCol eq edition.isbn)
            }.firstOrNull()
        if (exists != null)
            throw ConflictResponse(message = "This Book already exists in the database.")

        val book = Book.new {
            description = edition.description ?: work.description
            format = Format.PAPERBACK // TODO
            genres = SizedCollection(edition.genres.map {
                Genre.find {
                    GenreTable.titleCol eq it
                }.firstOrNull() ?: Genre.new {
                    title = it
                }
            })
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
            wishers = SizedCollection(body.wisherIds.map {
                User.findById(id = it)
                    ?: throw NotFoundResponse(message = "User not found")
            })
        }
        work.authors.map {
            OpenLibrary.getAuthor(authorId = it.authorId)
        }.map {
            Creator.find {
                CreatorTable.nameCol eq it
            }.firstOrNull() ?: Creator.new {
                name = it
            }
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
        tags = ["Book"]
    )
    fun refreshBook(ctx: Context): Unit = Utils.query(description = "Refresh Book") {
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
        } else
            throw NotImplementedResponse(message = "Only refresh via OpenLibrary edition id or Isbn currently supported.")
        val exists = if (edition.isbn == null)
            Book.find {
                ((BookTable.titleCol eq edition.title) and (BookTable.subtitleCol eq edition.subtitle)) or (BookTable.openLibraryCol eq edition.editionId)
            }.firstOrNull()
        else
            Book.find {
                ((BookTable.titleCol eq edition.title) and (BookTable.subtitleCol eq edition.subtitle)) or (BookTable.openLibraryCol eq edition.editionId) or (BookTable.isbnCol eq edition.isbn)
            }.firstOrNull()
        if (exists != null && exists != book)
            throw ConflictResponse(message = "This Book already exists in the database.")

        book.description = edition.description ?: work.description
        // book.format = Format.PAPERBACK
        book.genres = SizedCollection(edition.genres.map {
            Genre.find {
                GenreTable.titleCol eq it
            }.firstOrNull() ?: Genre.new {
                title = it
            }
        })
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
            Creator.find {
                CreatorTable.nameCol eq it
            }.firstOrNull() ?: Creator.new {
                name = it
            }
        }.forEach {
            val role = Role.find {
                RoleTable.titleCol eq "Author"
            }.firstOrNull() ?: Role.new {
                title = "Author"
            }
            BookCreatorRole.find {
                (BookCreatorRoleTable.bookCol eq book.id) and (BookCreatorRoleTable.creatorCol eq it.id) and (BookCreatorRoleTable.roleCol eq role.id)
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
                (BookCreatorRoleTable.bookCol eq book.id) and (BookCreatorRoleTable.creatorCol eq creator.id) and (BookCreatorRoleTable.roleCol eq role.id)
            }.firstOrNull() ?: BookCreatorRole.new {
                this.book = book
                this.creator = creator
                this.role = role
            }
        }

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
        tags = ["Book"]
    )
    fun discardBook(ctx: Context): Unit = Utils.query(description = "Discard Book") {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        book.isCollected = false
        book.readers = SizedCollection()
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
        tags = ["Book"]
    )
    fun collectBook(ctx: Context): Unit = Utils.query(description = "Collect Book") {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        book.isCollected = true
        book.wishers = SizedCollection()
        ctx.json(book.toJson(showAll = true))
    }

    private fun Context.getIdValue(): IdValue = this.bodyValidator<IdValue>()
        .check({ it.id > 0 }, error = "Id must be greater than 0")
        .get()

    @OpenApi(
        description = "Unread Book",
        methods = [HttpMethod.DELETE],
        operationId = "unreadBook",
        path = "/books/{book-id}/read",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Unread Book",
        tags = ["Book"]
    )
    fun unreadBook(ctx: Context): Unit = Utils.query(description = "Unread Book") {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        if (!book.isCollected)
            throw BadRequestResponse(message = "Book hasn't been collected to be able to unread")
        val body = ctx.getIdValue()
        val user = User.findById(id = body.id)
            ?: throw NotFoundResponse(message = "User not found")
        if (!book.readers.contains(user))
            throw BadRequestResponse(message = "Book hasn't been read by User")
        val temp = book.readers.toMutableList()
        temp.remove(user)
        book.readers = SizedCollection(temp)

        ctx.json(book.toJson(showAll = true))
    }

    @OpenApi(
        description = "Read Book",
        methods = [HttpMethod.PATCH],
        operationId = "readBook",
        path = "/books/{book-id}/read",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Read Book",
        tags = ["Book"]
    )
    fun readBook(ctx: Context): Unit = Utils.query(description = "Read Book") {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        if (!book.isCollected)
            throw BadRequestResponse(message = "Book hasn't been collected to be able to read")
        val body = ctx.getIdValue()
        val user = User.findById(id = body.id)
            ?: throw NotFoundResponse(message = "User not found")
        if (user in book.readers)
            throw BadRequestResponse(message = "Book has already been read by User")
        val temp = book.readers.toMutableList()
        temp.add(user)
        book.readers = SizedCollection(temp)

        ctx.json(book.toJson(showAll = true))
    }

    @OpenApi(
        description = "Unwish Book",
        methods = [HttpMethod.DELETE],
        operationId = "unwishBook",
        path = "/books/{book-id}/wish",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Unwish Book",
        tags = ["Book"]
    )
    fun unwishBook(ctx: Context): Unit = Utils.query(description = "Unwish Book") {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        if (book.isCollected)
            throw BadRequestResponse(message = "Book has already been collected")
        val body = ctx.getIdValue()
        val user = User.findById(id = body.id)
            ?: throw NotFoundResponse(message = "User not found")
        if (!book.wishers.contains(user))
            throw BadRequestResponse(message = "Book hasn't been wished by User")
        val temp = book.wishers.toMutableList()
        temp.remove(user)
        book.wishers = SizedCollection(temp)

        ctx.json(book.toJson(showAll = true))
    }

    @OpenApi(
        description = "Wish Book",
        methods = [HttpMethod.PATCH],
        operationId = "wishBook",
        path = "/books/{book-id}/wish",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(IdValue::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Wish Book",
        tags = ["Book"]
    )
    fun wishBook(ctx: Context): Unit = Utils.query(description = "Wish Book") {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        if (book.isCollected)
            throw BadRequestResponse(message = "Book has already been collected")
        val body = ctx.getIdValue()
        val user = User.findById(id = body.id)
            ?: throw NotFoundResponse(message = "User not found")
        if (user in book.wishers)
            throw BadRequestResponse(message = "Book has already been wished by User")
        val temp = book.wishers.toMutableList()
        temp.add(user)
        book.wishers = SizedCollection(temp)

        ctx.json(book.toJson(showAll = true))
    }

    private fun Context.getCreditBody(): CreatorRoleInput = this.bodyValidator<CreatorRoleInput>()
        .get()

    @OpenApi(
        description = "Add Creator and Role to Book",
        methods = [HttpMethod.PATCH],
        operationId = "addCreditToBook",
        path = "/books/{book-id}/credits",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(CreatorRoleInput::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "409", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Add Creator and Role to Book",
        tags = ["Book"]
    )
    fun addCredit(ctx: Context): Unit = Utils.query {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        val body = ctx.getCreditBody()
        val creator = Creator.findById(id = body.creatorId)
            ?: throw NotFoundResponse(message = "Creator not found")
        val role = Role.findById(id = body.roleId)
            ?: throw NotFoundResponse(message = "Role not found")
        val credit = BookCreatorRole.find {
            (BookCreatorRoleTable.bookCol eq book.id) and (BookCreatorRoleTable.creatorCol eq creator.id) and (BookCreatorRoleTable.roleCol eq role.id)
        }.firstOrNull()
        if (credit != null) {
            throw ConflictResponse(message = "Book Creator already has this role")
        } else
            BookCreatorRole.new {
                this.book = book
                this.creator = creator
                this.role = role
            }

        ctx.json(book.toJson(showAll = true))
    }

    @OpenApi(
        description = "Remove Creator and Role from Book",
        methods = [HttpMethod.DELETE],
        operationId = "removeCreditFromBook",
        path = "/books/{book-id}/credits",
        pathParams = [OpenApiParam(name = "book-id", type = Long::class, required = true)],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(CreatorRoleInput::class)]),
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(github.buriedincode.bookshelf.docs.Book::class)]),
            OpenApiResponse(status = "400", content = [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse(status = "404", content = [OpenApiContent(ErrorResponse::class)]),
        ],
        summary = "Remove Creator and Role from Book",
        tags = ["Book"]
    )
    fun removeCredit(ctx: Context): Unit = Utils.query(description = "Remove Creator from Book") {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        val body = ctx.getCreditBody()
        val creator = Creator.findById(id = body.creatorId)
            ?: throw NotFoundResponse(message = "Creator not found")
        val role = Role.findById(id = body.roleId)
            ?: throw NotFoundResponse(message = "Role not found")
        val credit = BookCreatorRole.find {
            (BookCreatorRoleTable.bookCol eq book.id) and (BookCreatorRoleTable.creatorCol eq creator.id) and (BookCreatorRoleTable.roleCol eq role.id)
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
        tags = ["Book"]
    )
    fun addGenre(ctx: Context): Unit = Utils.query(description = "Add Genre to Book") {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        val body = ctx.getIdValue()
        val genre = Genre.findById(id = body.id)
            ?: throw NotFoundResponse(message = "Genre not found")
        if (genre in book.genres)
            throw ConflictResponse(message = "Genre is already linked to Book")
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
        tags = ["Book"]
    )
    fun removeGenre(ctx: Context): Unit = Utils.query(description = "Remove Genre from Book") {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        val body = ctx.getIdValue()
        val genre = Genre.findById(id = body.id)
            ?: throw NotFoundResponse(message = "Genre not found")
        if (!book.genres.contains(genre))
            throw BadRequestResponse(message = "Genre is already linked to Book")
        val temp = book.genres.toMutableList()
        temp.remove(genre)
        book.genres = SizedCollection(temp)

        ctx.json(book.toJson(showAll = true))
    }

    private fun Context.getSeriesBody(): BookSeriesInput = this.bodyValidator<BookSeriesInput>()
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
        tags = ["Book"]
    )
    fun addSeries(ctx: Context): Unit = Utils.query(description = "Add Series to Book") {
        val book = getResource(resourceId = ctx.pathParam("book-id"))
        val body = ctx.getSeriesBody()
        val series = Series.findById(id = body.seriesId)
            ?: throw NotFoundResponse(message = "Series not found")
        val bookSeries = BookSeries.find {
            (BookSeriesTable.bookCol eq book.id) and (BookSeriesTable.seriesCol eq series.id)
        }.firstOrNull()
        if (bookSeries != null)
            throw ConflictResponse(message = "Series already is linked to Series")
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
        tags = ["Book"]
    )
    fun removeSeries(ctx: Context): Unit = Utils.query(description = "Remove Series from Book") {
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