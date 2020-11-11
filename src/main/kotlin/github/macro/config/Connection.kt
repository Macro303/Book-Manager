package github.macro.config

import org.apache.http.HttpHost

/**
 * Created by Macro303 on 2019-Oct-30
 */
data class Connection(var hostName: String? = null, var port: Int? = null) {

	fun getHttpHost(): HttpHost? {
		hostName ?: return null
		port ?: return null
		return HttpHost(hostName!!, port!!)
	}
}