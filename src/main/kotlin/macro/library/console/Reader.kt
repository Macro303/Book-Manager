package macro.library.console

import java.util.*

/**
 * Created by Macro303 on 2019-Oct-02
 */
internal object Reader {
	private val READER = Scanner(System.`in`)

	internal fun readConsole(text: String): String {
		print("${Colour.GREEN.ansicode}$text >> ")
		val input = READER.nextLine().trim()
		print(Colour.RESET.ansicode)
		return input
	}
}