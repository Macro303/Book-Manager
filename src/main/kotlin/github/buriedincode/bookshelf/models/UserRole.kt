package github.buriedincode.bookshelf.models

import io.javalin.security.RouteRole

enum class UserRole : RouteRole {
    GUEST,
    USER,
    MODERATOR,
    ADMIN,
}
