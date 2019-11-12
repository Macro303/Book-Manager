package macro.library

/**
 * Created by Macro303 on 2019-Oct-31
 */
interface ISendable {
	fun toJson(full: Boolean = false, showUnique: Boolean = false): Map<String, Any?>
}