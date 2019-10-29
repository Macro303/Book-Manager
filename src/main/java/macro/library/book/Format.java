package macro.library.book;

import macro.library.console.Console;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by Macro303 on 2019-Oct-22
 */
public enum Format {
	PAPERBACK,
	HARDCOVER,
	MASS_MEDIA_PAPERBACK;

	public static Format selection() {
		var options = Format.values();
		var selection = Console.displayMenu("Format", Arrays.stream(options).map(Format::getDisplay).toArray(String[]::new), null);
		Format format;
		try {
			format = options[selection - 1];
		} catch (ArrayIndexOutOfBoundsException aiooe) {
			format = options[0];
		}
		return format;
	}

	public String getDisplay() {
		return Arrays.stream(name().toLowerCase().replaceAll("_", " ").split(" ")).map(it -> it.substring(0, 1).toUpperCase() + it.substring(1)).collect(Collectors.joining(" "));
	}
}