package macro.library

import org.apache.logging.log4j.LogManager

/**
 * Created by Macro303 on 2019-Oct-01
 */
object Util {
	private val LOGGER = LogManager.getLogger(Util::class.java)
	internal const val SQLITE_DATABASE = "Book-Manager.sqlite"
	const val DATABASE_URL = "jdbc:sqlite:$SQLITE_DATABASE"

	fun displayISBN(isbn: Long?): String{
		isbn ?: return ""
		val str = isbn.toString()
		if (str.length == 10)
			return str[0] + "-" + str.substring(1,5) + "-" + str.substring(5,9) + "-" + str[9]
		return str.substring(0,3) + "-" + str[3] + "-" + str.substring(4,8) + "-" + str.substring(8,12) + "-" + str[12]
	}
}