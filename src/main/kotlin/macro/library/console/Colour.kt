package macro.library.console

/**
 * Created by Macro303 on 2019-Oct-02
 */
internal enum class Colour(val ansicode: String) {
	RESET(ansicode = "\u001B[0m"),
	BLACK(ansicode = "\u001B[30;1m"),
	RED(ansicode = "\u001B[31;1m"),
	GREEN(ansicode = "\u001B[32;1m"),
	YELLOW(ansicode = "\u001B[33;1m"),
	BLUE(ansicode = "\u001B[34;1m"),
	MAGENTA(ansicode = "\u001B[35;1m"),
	CYAN(ansicode = "\u001B[36;1m"),
	WHITE(ansicode = "\u001B[37;1m");
}