package github.buriedincode.bookshelf.models

data class RoleInput(
    val credits: List<Credit> = ArrayList(),
    val title: String,
) {
    data class Credit(
        val bookId: Long,
        val creatorId: Long,
    )
}
