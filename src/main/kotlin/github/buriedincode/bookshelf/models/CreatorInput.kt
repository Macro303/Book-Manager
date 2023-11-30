package github.buriedincode.bookshelf.models

data class CreatorInput(
    val credits: List<Credit> = ArrayList(),
    val imageUrl: String? = null,
    val name: String,
) {
    data class Credit(
        val bookId: Long,
        val roleId: Long,
    )
}
