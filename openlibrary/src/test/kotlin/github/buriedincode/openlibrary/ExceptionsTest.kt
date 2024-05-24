package github.buriedincode.openlibrary

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.time.Duration
import kotlin.jvm.java

@TestInstance(Lifecycle.PER_CLASS)
class ExceptionsTest {
    @Nested
    inner class Service {
        @Test
        fun `Test throwing a ServiceException for a 404`() {
            val session = OpenLibrary(cache = null)
            assertThrows(ServiceException::class.java) {
                val uri = session.encodeURI(endpoint = "/invalid")
                session.getRequest(uri = uri)
            }
        }

        @Test
        fun `Test throwing a ServiceException for a timeout`() {
            val session = OpenLibrary(cache = null, timeout = Duration.ofMillis(1))
            assertThrows(ServiceException::class.java) {
                session.getEdition(id = "OL26964454M")
            }
        }
    }
}
