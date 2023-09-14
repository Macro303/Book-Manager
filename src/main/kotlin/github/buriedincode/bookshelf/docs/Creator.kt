package github.buriedincode.bookshelf.docs

class CreatorEntry(
    val creatorId: Long,
    val name: String,
    val imageUrl: String?,
)

class Creator(
    val creatorId: Long,
    val credits: List<CreatorCredit>,
    val name: String,
    val imageUrl: String?,
)

class CreatorCredit(
    val book: BookEntry,
    val role: RoleEntry,
)
