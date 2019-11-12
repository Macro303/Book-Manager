package macro.library.console

import java.util.*

/**
 * Created by Macro303 on 2019-Oct-30
 */
internal object Reader {
	private val READER = Scanner(System.`in`)

	fun readConsole(text: String): String {
		print("${Colour.GREEN}$text >> ")
		val input = READER.nextLine().trim()
		print(Colour.RESET)
		return input
	}
}