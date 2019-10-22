package macro.library.config;

import org.apache.http.HttpHost;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Macro303 on 2019-Oct-22
 */
public class Connection {
	@Nullable
	private String hostName;
	private int port;

	public Connection() {
		this(null, -1);
	}

	public Connection(@Nullable String hostName, int port) {
		this.hostName = hostName;
		this.port = port;
	}

	@Nullable
	public String getHostName() {
		return hostName;
	}

	public void setHostName(@Nullable String hostName) {
		this.hostName = hostName;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Nullable
	public HttpHost getHttpHost() {
		if (hostName == null)
			return null;
		if (port == -1)
			return null;
		return new HttpHost(hostName, port);
	}
}