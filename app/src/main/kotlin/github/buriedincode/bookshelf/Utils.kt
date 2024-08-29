package github.buriedincode.bookshelf

import com.sksamuel.hoplite.Secret
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.ExperimentalKeywordApi
import org.jetbrains.exposed.sql.Slf4jSqlDebugLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAccessor
import java.util.Locale
import kotlin.io.path.div

object Utils {
    @JvmStatic
    private val LOGGER = KotlinLogging.logger { }
    private val HOME_ROOT: Path by lazy { Paths.get(System.getProperty("user.home")) }
    private val XDG_CACHE: Path by lazy { System.getenv("XDG_CACHE_HOME")?.let(Paths::get) ?: (HOME_ROOT / ".cache") }
    private val XDG_CONFIG: Path by lazy { System.getenv("XDG_CONFIG_HOME")?.let(Paths::get) ?: (HOME_ROOT / ".config") }
    private val XDG_DATA: Path by lazy { System.getenv("XDG_DATA_HOME")?.let(Paths::get) ?: (HOME_ROOT / ".local" / "share") }

    internal val CACHE_ROOT: Path = XDG_CACHE / "bookshelf"
    internal val CONFIG_ROOT: Path = XDG_CONFIG / "bookshelf"
    internal val DATA_ROOT: Path = XDG_DATA / "bookshelf"
    internal const val VERSION = "0.3.1"

    private val DATABASE: Database by lazy {
        val settings = Settings.load()
        Database.connect(
            url = when (settings.database.source) {
                Settings.Database.Source.POSTGRES -> "jdbc:postgresql://${settings.database.url}"
                else -> "jdbc:sqlite:${settings.database.url}"
            },
            driver = when (settings.database.source) {
                Settings.Database.Source.POSTGRES -> "org.postgresql.Driver"
                else -> "org.sqlite.JDBC"
            },
            user = settings.database.user ?: "user",
            password = settings.database.password ?: "password",
            databaseConfig = DatabaseConfig {
                @OptIn(ExperimentalKeywordApi::class)
                preserveKeywordCasing = true
            },
        )
    }

    init {
        listOf(CACHE_ROOT, CONFIG_ROOT, DATA_ROOT).forEach {
            if (!Files.exists(it)) it.toFile().mkdirs()
        }
    }

    fun <T> query(block: () -> T): T {
        val startTime = LocalDateTime.now()
        val transaction = transaction(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, db = DATABASE) {
            addLogger(Slf4jSqlDebugLogger)
            block()
        }
        LOGGER.debug { "Took ${ChronoUnit.MILLIS.between(startTime, LocalDateTime.now())}ms" }
        return transaction
    }

    private fun getDayNumberSuffix(day: Int): String {
        return if (day in 11..13) {
            "th"
        } else {
            when (day % 10) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }
        }
    }

    internal fun KLogger.log(level: Level, message: () -> Any?) {
        when (level) {
            Level.TRACE -> this.trace(message)
            Level.DEBUG -> this.debug(message)
            Level.INFO -> this.info(message)
            Level.WARN -> this.warn(message)
            Level.ERROR -> this.error(message)
            else -> return
        }
    }

    internal fun toHumanReadable(milliseconds: Float): String {
        val duration = Duration.ofMillis(milliseconds.toLong())
        val minutes = duration.toMinutes()
        val seconds = duration.seconds - minutes * 60
        val millis = duration.toMillis() - (minutes * 60000 + seconds * 1000)
        return when {
            minutes > 0 -> "${minutes}min ${seconds}sec ${millis}ms"
            seconds > 0 -> "${seconds}sec ${millis}ms"
            else -> "${millis}ms"
        }
    }

    internal fun Secret?.isNullOrBlank(): Boolean = this?.value.isNullOrBlank()

    inline fun <reified T : Enum<T>> String.asEnumOrNull(): T? = enumValues<T>().firstOrNull {
        it.name.equals(this, ignoreCase = true) ||
            it.name.replace("_", " ").equals(this, ignoreCase = true)
    }

    inline fun <reified T : Enum<T>> T.titlecase(): String = this.name.lowercase().split("_").joinToString(" ") {
        it.replaceFirstChar(Char::uppercaseChar)
    }

    fun String.toLocalDateOrNull(pattern: String): LocalDate? {
        return try {
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)
            java.time.LocalDate.parse(this, formatter).toKotlinLocalDate()
        } catch (e: DateTimeParseException) {
            null
        }
    }

    fun LocalDate.toHumanReadable(showFull: Boolean = false): String {
        val pattern = if (showFull) {
            "EEE, d'${getDayNumberSuffix(this.dayOfMonth)}' MMM yyyy"
        } else {
            "d'${getDayNumberSuffix(this.dayOfMonth)}' MMM yyyy"
        }
        return this.toJavaLocalDate().formatToPattern(pattern)
    }

    fun LocalDate.toString(pattern: String): String {
        return this.toJavaLocalDate().formatToPattern(pattern)
    }

    private fun TemporalAccessor.formatToPattern(pattern: String): String {
        return DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH).format(this)
    }
}
