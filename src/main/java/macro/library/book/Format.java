package macro.library.book;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by Macro303 on 2019-Oct-22
 */
public enum Format {
	PAPERBACK,
	HARDCOVER,
	MASS_MEDIA_PAPERBACK;

	public String getDisplay() {
		return Arrays.stream(name().toLowerCase().replaceAll("_", " ").split(" ")).map(it -> it.substring(0, 1).toUpperCase() + it.substring(1)).collect(Collectors.joining(" "));
	}
}
