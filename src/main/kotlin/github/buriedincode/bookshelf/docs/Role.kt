package github.buriedincode.bookshelf.docs

class RoleEntry(
    val roleId: Long,
    val title: String,
)

class Role(
    val roleId: Long,
    val credits: List<RoleCredit>,
    val title: String,
)

class RoleCredit(
    val book: BookEntry,
    val creator: CreatorEntry,
)
