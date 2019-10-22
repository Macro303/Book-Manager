package macro.library;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Created by Macro303 on 2019-Oct-21
 */
public class Isbn implements Comparable<Isbn> {
	@NotNull
	private static final Logger LOGGER = LogManager.getLogger(Isbn.class);
	@NotNull
	private final String normalizedIsbn;

	private Isbn(@NotNull String originalIsbn) {
		this.normalizedIsbn = originalIsbn.replaceAll("-", "");
	}

	@NotNull
	public static Isbn of(@NotNull String number) throws IllegalArgumentException {
		if (!isValid(number))
			throw new IllegalArgumentException();

		return number.replaceAll("-", "").length() == 10 ? new Isbn(toIsbn13(number)) : new Isbn(number);
	}

	private static boolean isValid(@NotNull String numberSequence) {
		var normalizedSequence = numberSequence.replaceAll("-", "");
		if (normalizedSequence.length() == 13)
			return isValidAsIsbn13(normalizedSequence);
		else if (normalizedSequence.length() == 10)
			return isValidAsIsbn10(normalizedSequence);
		return false;
	}

	private static String toIsbn13(@NotNull String isbn10) {
		var temp = stringToIntArray(isbn10.substring(0, isbn10.length() - 1));
		var digits = new int[temp.length + 3];
		digits[0] = 9;
		digits[1] = 7;
		digits[2] = 8;
		System.arraycopy(temp, 0, digits, 3, temp.length);

		var checkDigit = isbn13CheckDigit(digits);
		var sb = new StringBuilder();
		for (var digit : digits)
			sb.append(digit);
		return sb.toString() + checkDigit;
	}

	private static boolean isValidAsIsbn13(@NotNull String number) {
		var digits = stringToIntArray(number);
		return digits[12] == isbn13CheckDigit(digits);
	}

	private static boolean isValidAsIsbn10(@NotNull String number) {
		var digits = stringToIntArray(number);
		return digits[9] == isbn10CheckDigit(digits);
	}

	private static int[] stringToIntArray(@NotNull String str) {
		return Arrays.stream(str.split("")).mapToInt(s -> {
			try {
				return Integer.parseInt(s);
			} catch (NumberFormatException nfe) {
				return 10;
			}
		}).toArray();
	}

	private static int isbn13CheckDigit(@NotNull int[] digits) {
		var weights = new int[]{1, 3};
		var sum = 0;
		for (var i = 0; i < 12; i++)
			sum += digits[i] * weights[i % 2];
		return sum % 10 == 0 ? 0 : 10 - sum % 10;
	}

	private static int isbn10CheckDigit(@NotNull int[] digits) {
		var sum = 0;
		for (int i = 0, weight = 10; i < 9; i++, weight--)
			sum += digits[i] * weight;
		return 11 - (sum % 11);
	}

	@Override
	public int hashCode() {
		return normalizedIsbn.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Isbn)) return false;

		Isbn isbn = (Isbn) o;

		return normalizedIsbn.equals(isbn.normalizedIsbn);
	}

	@Override
	public String toString() {
		return normalizedIsbn;
	}

	/**
	 * @return description of ISBN. It includes hyphens like ###-##-####-###-#.
	 */
	public String getDisplay() {
		return normalizedIsbn.substring(0, 3) + "-" + normalizedIsbn.substring(3, 5) + "-" + normalizedIsbn.substring(5, 9) + "-" + normalizedIsbn.substring(9, 12) + "-" + normalizedIsbn.charAt(12);
	}

	@Override
	public int compareTo(@NotNull Isbn other) {
		return normalizedIsbn.compareToIgnoreCase(other.normalizedIsbn);
	}
}