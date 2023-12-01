package github.buriedincode.bookshelf.models

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.time.LocalDate

data class UserInput(
    val image: String? = null,
    val readBooks: List<ReadBook> = ArrayList(),
    val role: UserRole = UserRole.GUEST,
    val username: String,
    val wishedBookIds: List<Long> = ArrayList(),
) {
    data class ReadBook(
        val bookId: Long,
        @JsonDeserialize(using = LocalDateDeserializer::class)
        val readDate: LocalDate? = LocalDate.now(),
    )
}
