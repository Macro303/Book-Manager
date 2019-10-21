package macro.library.console;

import org.jetbrains.annotations.NotNull;

import java.util.Scanner;

/**
 * Created by Macro303 on 2019-Oct-21
 */
abstract class Reader {
	private static final Scanner READER = new Scanner(System.in);

	static String readConsole(@NotNull String text) {
		System.out.print(Colour.GREEN + text + " >> ");
		var input = READER.nextLine().trim();
		System.out.print(Colour.RESET);
		return input;
	}
}