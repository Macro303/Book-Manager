package github.macro

/**
 * Created by Macro303 on 2019-Oct-31
 */
interface ISendable {
	fun toJson(full: Boolean = true): Map<String, Any?>
}