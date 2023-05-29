package github.buriedincode.bookshelf.routers.html

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.Book
import github.buriedincode.bookshelf.models.User
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.dao.load

object UserHtmlRouter {
    fun listEndpoint(ctx: Context) = Utils.query {
        val users = User.all().toList()
        ctx.render(filePath = "templates/user/list.kte", mapOf("users" to users))
    }

    fun viewEndpoint(ctx: Context) = Utils.query {
        val userId = ctx.pathParam("user-id").toLongOrNull()
            ?: throw NotFoundResponse(message = "User not found")
        val user = User.findById(id = userId)
            ?.load(User::readBooks, User::wishedBooks)
            ?: throw NotFoundResponse(message = "User not found")
        ctx.render(filePath = "templates/user/view.kte", mapOf("user" to user))
    }

    fun editEndpoint(ctx: Context) = Utils.query {
        val userId = ctx.pathParam("user-id").toLongOrNull()
            ?: throw NotFoundResponse(message = "User not found")
        val user = User.findById(id = userId)
            ?.load(User::readBooks, User::wishedBooks)
            ?: throw NotFoundResponse(message = "User not found")
        val readBooks = Book.all().toList().filter { it.isCollected }.filterNot { it in user.readBooks }
        val wishedBooks = Book.all().toList().filterNot { it.isCollected }.filterNot { it in user.wishedBooks }
        ctx.render(
            filePath = "templates/user/edit.kte", mapOf(
                "user" to user,
                "readBooks" to readBooks,
                "wishedBooks" to wishedBooks
            )
        )
    }

    fun wishlistEndpoint(ctx: Context) = Utils.query {
        val userId = ctx.pathParam("user-id").toLongOrNull()
            ?: throw NotFoundResponse(message = "User not found")
        val user = User.findById(id = userId)
            ?.load(User::wishedBooks)
            ?: throw NotFoundResponse(message = "User not found")
        val books = user.wishedBooks.toList().plus(Book.all().filterNot { it.isCollected }.filter { it.wishers.empty() }.toList())
        ctx.render(
            filePath = "templates/user/wishlist.kte", mapOf(
                "user" to user,
                "books" to books
            )
        )
    }
}