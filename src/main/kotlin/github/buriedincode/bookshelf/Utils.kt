package github.buriedincode.bookshelf

import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Slf4jSqlDebugLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


object Utils : Logging {
    private val HOME_ROOT: String = System.getProperty("user.home")

    internal const val VERSION = "0.0.0"
    internal val CACHE_ROOT = Paths.get(HOME_ROOT, ".cache", "bookshelf")
    internal val CONFIG_ROOT = Paths.get(HOME_ROOT, ".config", "bookshelf")
    internal val DATA_ROOT = Paths.get(HOME_ROOT, ".local", "share", "bookshelf")

    val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val database: Database = Database.connect(url = "jdbc:sqlite:${DATA_ROOT}/${Settings.INSTANCE.databaseName}", driver = "org.sqlite.JDBC")

    internal fun <T> query(description: String = "", block: () -> T): T {
        val startTime = LocalDateTime.now()
        val transaction = transaction(
            transactionIsolation = Connection.TRANSACTION_SERIALIZABLE,
            repetitionAttempts = 1,
            db = database
        ) {
            addLogger(Slf4jSqlDebugLogger)
            block()
        }
        logger.debug("Took ${ChronoUnit.MILLIS.between(startTime, LocalDateTime.now())}ms to $description")
        return transaction
    }

    init {
        if (!Files.exists(CACHE_ROOT))
            try {
                Files.createDirectories(CACHE_ROOT)
            } catch (ioe: IOException) {
                logger.error("Unable to create cache folder", ioe)
            }
        if (!Files.exists(CONFIG_ROOT))
            try {
                Files.createDirectories(CONFIG_ROOT)
            } catch (ioe: IOException) {
                logger.error("Unable to create config folder", ioe)
            }
        if (!Files.exists(DATA_ROOT))
            try {
                Files.createDirectories(DATA_ROOT)
            } catch (ioe: IOException) {
                logger.error("Unable to create data folder", ioe)
            }
    }

    fun getUserDateFormatter(date: LocalDate): DateTimeFormatter = DateTimeFormatter.ofPattern("d'${getDayNumberSuffix(date.dayOfMonth)}' MMM yyyy")

    private fun getDayNumberSuffix(day: Int): String {
        return if (day in 11..13) {
            "th"
        } else when (day % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
}