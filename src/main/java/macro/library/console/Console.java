package macro.library.console;

import macro.library.Util;
import macro.library.book.Book;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Macro303 on 2019-Oct-21
 */
public abstract class Console {
	private static final Logger LOGGER = LogManager.getLogger(Console.class);

	public static void displayHeader(@NotNull String text) {
		colourConsole("=".repeat(text.length() + 4), Colour.BLUE);
		displaySubHeader(text);
		colourConsole("=".repeat(text.length() + 4), Colour.BLUE);
	}

	public static void displaySubHeader(@NotNull String text) {
		colourConsole("  " + text + "  ", Colour.BLUE);
	}

	public static int displayMenu(@NotNull String header, @NotNull String[] options) {
		return displayMenu(header, options, "Back");
	}

	public static int displayMenu(@NotNull String header, @NotNull String[] options, @Nullable String exit) {
		displayHeader(header);
		if (options.length == 0)
			return 0;
		var padCount = String.valueOf(options.length).length();
		for (int i = 0; i < options.length; i++)
			displayItemValue(Util.padStr(String.valueOf(i + 1), padCount), options[i]);
		if (exit != null)
			displayItemValue(Util.padStr("0", padCount), exit);
		try {
			return Integer.parseInt(displayPrompt("Option"));
		} catch (NumberFormatException nfe) {
			return 0;
		}
	}

	public static boolean displayAgreement(@NotNull String text) {
		var input = displayPrompt(text + " (Y/N)");
		return input.equalsIgnoreCase("y");
	}

	@NotNull
	public static String displayPrompt(@NotNull String text) {
		return Reader.readConsole(text).trim();
	}

	public static void displayItemValue(@NotNull String item, @Nullable Object value) {
		colourConsole(item + ": ", Colour.BLUE, false);
		colourConsole(String.valueOf(value), Colour.WHITE);
	}

	public static void display(@Nullable String text) {
		display(text, Colour.WHITE);
	}

	public static void display(@Nullable String text, @NotNull Colour colour) {
		colourConsole(text, colour);
	}

	private static void colourConsole(@Nullable String text, @NotNull Colour colour) {
		colourConsole(text, colour, true);
	}

	private static void colourConsole(@Nullable String text, @NotNull Colour colour, boolean newLine) {
		if (newLine)
			System.out.println(colour + text + Colour.RESET);
		else
			System.out.print(colour + text + Colour.RESET);
	}

	public static void displayTable(List<Book> books) {
		var isbnSize = 4;
		var maxISBN = books.stream().max(Comparator.comparing(it -> String.valueOf(it.getISBN()).length()));
		if (maxISBN.isPresent()) {
			isbnSize = String.valueOf(maxISBN.get().getISBN()).length();
			if (isbnSize < 4)
				isbnSize = 4;
		}
		var titleSize = 5;
		var maxTitle = books.stream().max(Comparator.comparing(it -> it.getTitle().length()));
		if (maxTitle.isPresent()) {
			titleSize = maxTitle.get().getTitle().length();
			if (titleSize < 5)
				titleSize = 5;
		}
		var subtitleSize = 8;
		var maxSubtitle = books.stream().max(Comparator.comparing(it -> String.valueOf(it.getSubtitle()).length()));
		if (maxSubtitle.isPresent()) {
			subtitleSize = String.valueOf(maxSubtitle.get().getSubtitle()).length();
			if (subtitleSize < 8)
				subtitleSize = 8;
		}
		var authorSize = 6;
		var maxAuthor = books.stream().max(Comparator.comparing(it -> it.getAuthor().length()));
		if (maxAuthor.isPresent()) {
			authorSize = maxAuthor.get().getAuthor().length();
			if (authorSize < 6)
				authorSize = 6;
		}
		var publisherSize = 9;
		var maxPublisher = books.stream().max(Comparator.comparing(it -> it.getPublisher().length()));
		if (maxPublisher.isPresent()) {
			publisherSize = maxPublisher.get().getPublisher().length();
			if (publisherSize < 9)
				publisherSize = 9;
		}
		var formatSize = 6;
		var maxFormat = books.stream().max(Comparator.comparing(it -> it.getFormat().getDisplay().length()));
		if (maxFormat.isPresent()) {
			formatSize = maxFormat.get().getFormat().getDisplay().length();
			if (formatSize < 6)
				formatSize = 6;
		}
		var titleOutput = "| ";
		titleOutput += Util.padStr("ISBN", isbnSize * -1) + " | ";
		titleOutput += Util.padStr("Title", titleSize * -1) + " | ";
		titleOutput += Util.padStr("Subtitle", subtitleSize * -1) + " | ";
		titleOutput += Util.padStr("Author", authorSize * -1) + " | ";
		titleOutput += Util.padStr("Publisher", publisherSize * -1) + " | ";
		titleOutput += Util.padStr("Format", formatSize * -1) + " | ";
		colourConsole(titleOutput, Colour.BLUE);
		var tableOutput = "| ";
		tableOutput += "-".repeat(isbnSize) + " | ";
		tableOutput += "-".repeat(titleSize) + " | ";
		tableOutput += "-".repeat(subtitleSize) + " | ";
		tableOutput += "-".repeat(authorSize) + " | ";
		tableOutput += "-".repeat(publisherSize) + " | ";
		tableOutput += "-".repeat(formatSize) + " | ";
		colourConsole(tableOutput, Colour.BLUE);
		Collections.sort(books);
		for (var book : books) {
			var bookOutput = Colour.BLUE + "| " + Colour.WHITE;
			bookOutput += Util.padStr(String.valueOf(book.getISBN()), isbnSize) + Colour.BLUE + " | " + Colour.WHITE;
			bookOutput += Util.padStr(book.getTitle(), titleSize * -1) + Colour.BLUE + " | " + Colour.WHITE;
			bookOutput += Util.padStr(book.getSubtitle(), subtitleSize * -1) + Colour.BLUE + " | " + Colour.WHITE;
			bookOutput += Util.padStr(book.getAuthor(), authorSize * -1) + Colour.BLUE + " | " + Colour.WHITE;
			bookOutput += Util.padStr(book.getPublisher(), publisherSize * -1) + Colour.BLUE + " | " + Colour.WHITE;
			bookOutput += Util.padStr(book.getFormat().getDisplay(), formatSize * -1) + Colour.BLUE + " | " + Colour.WHITE;
			colourConsole(bookOutput, Colour.WHITE);
		}
	}
}
