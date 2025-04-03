package org.mcsmp.de.fancyPluginsCore.model;

import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.mcsmp.de.fancyPluginsCore.MinecraftVersion;
import org.mcsmp.de.fancyPluginsCore.utility.ChatUtil;
import org.mcsmp.de.fancyPluginsCore.utility.ValidCore;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Simplistic enumeration of all supported color values for chat.
 *
 * @author md_5, backported for comp. reasons by kangarko
 */
public final class CompChatColor implements TextColor, ConfigStringSerializable {

	/**
	 * The special character which prefixes all chat colour codes. Use this if
	 * you need to dynamically convert colour codes from your custom format.
	 */
	public static final char COLOR_CHAR = '\u00A7';

	/**
	 * All legacy color codes
	 */
	public static final String ALL_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";

	/**
	 * MiniMessages to legacy color codes
	 */
	public static final Map<String, String> MINI_TO_LEGACY = new HashMap<>();

	/**
	 * MiniMessages tags to text color.
	 */
	public static final Map<String, TextColor> MINI_TO_COLOR = new HashMap<>();

	/**
	 * MiniMessages tags to text decoration.
	 */
	public static final Map<String, TextDecoration> MINI_TO_DECORATION = new HashMap<>();

	/**
	 * Stores legacy colors.
	 */
	public static final Map<String, String> LEGACY_TO_MINI = new HashMap<>();

	/**
	 * Colour instances keyed by their active character.
	 */
	private static final Map<Character, CompChatColor> BY_CHAR = new HashMap<>();

	/**
	 * Colour instances keyed by their name.
	 */
	private static final Map<String, CompChatColor> BY_NAME = new HashMap<>();

	/**
	 * Matches legacy ampersand and section color codes.
	 */
	private static final Pattern LEGACY_COLOR_MATCH = Pattern.compile("(&|" + COLOR_CHAR + ")([0-9a-fk-or])");

	/**
	 * Represents colors we can use for MC before 1.16
	 */
	private static final Color[] LEGACY_COLORS = {
			new Color(0, 0, 0),
			new Color(0, 0, 170),
			new Color(0, 170, 0),
			new Color(0, 170, 170),
			new Color(170, 0, 0),
			new Color(170, 0, 170),
			new Color(255, 170, 0),
			new Color(170, 170, 170),
			new Color(85, 85, 85),
			new Color(85, 85, 255),
			new Color(85, 255, 85),
			new Color(85, 255, 255),
			new Color(255, 85, 85),
			new Color(255, 85, 255),
			new Color(255, 255, 85),
			new Color(255, 255, 255),
	};

	/**
	 * Represents black.
	 */
	public static final CompChatColor BLACK = new CompChatColor('0', "black", new Color(0x000000));

	/**
	 * Represents dark blue.
	 */
	public static final CompChatColor DARK_BLUE = new CompChatColor('1', "dark_blue", new Color(0x0000AA));

	/**
	 * Represents dark green.
	 */
	public static final CompChatColor DARK_GREEN = new CompChatColor('2', "dark_green", new Color(0x00AA00));

	/**
	 * Represents dark blue (aqua).
	 */
	public static final CompChatColor DARK_AQUA = new CompChatColor('3', "dark_aqua", new Color(0x00AAAA));

	/**
	 * Represents dark red.
	 */
	public static final CompChatColor DARK_RED = new CompChatColor('4', "dark_red", new Color(0xAA0000));

	/**
	 * Represents dark purple.
	 */
	public static final CompChatColor DARK_PURPLE = new CompChatColor('5', "dark_purple", new Color(0xAA00AA));

	/**
	 * Represents gold.
	 */
	public static final CompChatColor GOLD = new CompChatColor('6', "gold", new Color(0xFFAA00));

	/**
	 * Represents gray.
	 */
	public static final CompChatColor GRAY = new CompChatColor('7', "gray", new Color(0xAAAAAA));

	/**
	 * Represents dark gray.
	 */
	public static final CompChatColor DARK_GRAY = new CompChatColor('8', "dark_gray", new Color(0x555555));

	/**
	 * Represents blue.
	 */
	public static final CompChatColor BLUE = new CompChatColor('9', "blue", new Color(0x05555FF));

	/**
	 * Represents green.
	 */
	public static final CompChatColor GREEN = new CompChatColor('a', "green", new Color(0x55FF55));

	/**
	 * Represents aqua.
	 */
	public static final CompChatColor AQUA = new CompChatColor('b', "aqua", new Color(0x55FFFF));

	/**
	 * Represents red.
	 */
	public static final CompChatColor RED = new CompChatColor('c', "red", new Color(0xFF5555));

	/**
	 * Represents light purple.
	 */
	public static final CompChatColor LIGHT_PURPLE = new CompChatColor('d', "light_purple", new Color(0xFF55FF));

	/**
	 * Represents yellow.
	 */
	public static final CompChatColor YELLOW = new CompChatColor('e', "yellow", new Color(0xFFFF55));

	/**
	 * Represents white.
	 */
	public static final CompChatColor WHITE = new CompChatColor('f', "white", new Color(0xFFFFFF));

	/**
	 * Represents magical characters that change around randomly.
	 */
	public static final CompChatColor MAGIC = new CompChatColor('k', "obfuscated");

	/**
	 * Makes the text bold.
	 */
	public static final CompChatColor BOLD = new CompChatColor('l', "bold");

	/**
	 * Makes a line appear through the text.
	 */
	public static final CompChatColor STRIKETHROUGH = new CompChatColor('m', "strikethrough");

	/**
	 * Makes the text appear underlined.
	 */
	public static final CompChatColor UNDERLINE = new CompChatColor('n', "underline");

	/**
	 * Makes the text italic.
	 */
	public static final CompChatColor ITALIC = new CompChatColor('o', "italic");

	/**
	 * Resets all previous chat colors or formats.
	 */
	public static final CompChatColor RESET = new CompChatColor('r', "reset");

	/**
	 * The code representing this color such as a, r, etc.
	 */
	private final char code;

	/**
	 * The name of this color
	 */
	@Getter
	private final String name;

	/**
	 * The RGB color of the ChatColor. null for non-colors (formatting)
	 */
	@Getter
	private final Color color;

	/**
	 * This colour's colour char prefixed by the {@link #COLOR_CHAR}.
	 */
	private final String toString;

	private CompChatColor(final char code, final String name) {
		this(code, name, null);
	}

	private CompChatColor(final char code, final String name, final Color color) {
		this.code = code;
		this.name = name;
		this.color = color;
		this.toString = new String(new char[] { COLOR_CHAR, code });

		BY_CHAR.put(code, this);
		BY_NAME.put(name.toUpperCase(Locale.ROOT), this);
	}

	private CompChatColor(final String name, final String toString, final int rgb) {
		this.code = '#';
		this.name = name;
		this.color = new Color(rgb);
		this.toString = toString;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 53 * hash + Objects.hashCode(this.toString);
		return hash;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;

		if (obj == null || this.getClass() != obj.getClass())
			return false;

		return Objects.equals(this.toString, ((CompChatColor) obj).toString);
	}

	/**
	 * Get the color code
	 *
	 * @return the code
	 */
	public char getCode() {
		ValidCore.checkBoolean(this.code != '#', "Cannot retrieve color code for HEX colors");

		return this.code;
	}

	/**
	 * Return true if the color is HEX?
	 *
	 * @return
	 */
	public boolean isHex() {
		return this.code == '#';
	}

	/**
	 * Return the literal value of the color, colorized by the color itself :)
	 *
	 * Example: returns "&6Gold" or "&cRed" or #cc44ff with the actual hex code that MC chat will parse before it.
	 *
	 * @return
	 */
	public String toColorizedChatString() {
		return this.toString /* prints color */ + this.toChatString();
	}

	/**
	 * Prints the face value of the color you can use in Minecraft chat,
	 * i.e. "Gold" instead of the actual gold magic letter or #cc44ff instead of actually coloring the chat.
	 *
	 * @return
	 */
	public String toChatString() {
		return this.isHex() ? this.getName() : ChatUtil.capitalizeFully(this.getName());
	}

	/**
	 * Return a string you can save to YAML config
	 *
	 * @return
	 */
	public String toSaveableString() {
		return this.getName();
	}

	/**
	 * Return true if this is a format
	 *
	 * @return
	 */
	public boolean isFormat() {
		return this.color == null;
	}

	/**
	 * Checks if this code is a color code as opposed to a format code.
	 *
	 * @return whether this ChatColor is a color code
	 */
	public boolean isColor() {
		return !this.isFormat() && this != RESET;
	}

	/**
	 * Convert this color to Adventure text color or format
	 *
	 * @return
	 */
	public Style toStyle() {
		return Style.style(this.isFormat() ? this.toTextDecoration() : this.toTextColor());
	}

	/**
	 * Get the Adventure text color
	 *
	 * @return the color or null if reset
	 */
	public TextColor toTextColor() {
		if (this == BLACK)
			return NamedTextColor.BLACK;
		else if (this == DARK_BLUE)
			return NamedTextColor.DARK_BLUE;
		else if (this == DARK_GREEN)
			return NamedTextColor.DARK_GREEN;
		else if (this == DARK_AQUA)
			return NamedTextColor.DARK_AQUA;
		else if (this == DARK_RED)
			return NamedTextColor.DARK_RED;
		else if (this == DARK_PURPLE)
			return NamedTextColor.DARK_PURPLE;
		else if (this == GOLD)
			return NamedTextColor.GOLD;
		else if (this == GRAY)
			return NamedTextColor.GRAY;
		else if (this == DARK_GRAY)
			return NamedTextColor.DARK_GRAY;
		else if (this == BLUE)
			return NamedTextColor.BLUE;
		else if (this == GREEN)
			return NamedTextColor.GREEN;
		else if (this == AQUA)
			return NamedTextColor.AQUA;
		else if (this == RED)
			return NamedTextColor.RED;
		else if (this == LIGHT_PURPLE)
			return NamedTextColor.LIGHT_PURPLE;
		else if (this == YELLOW)
			return NamedTextColor.YELLOW;
		else if (this == WHITE)
			return NamedTextColor.WHITE;
		else if (this == RESET)
			return null;
		else if (this.color == null)
			throw new RuntimeException("Cannot convert " + this + " to a text color");
		else
			return TextColor.color(this.color.getRGB());
	}

	/**
	 * Get the Adventure text format
	 *
	 * @return the format or null for reset
	 */
	public TextDecoration toTextDecoration() {
		if (this == BOLD)
			return TextDecoration.BOLD;
		else if (this == STRIKETHROUGH)
			return TextDecoration.STRIKETHROUGH;
		else if (this == UNDERLINE)
			return TextDecoration.UNDERLINED;
		else if (this == ITALIC)
			return TextDecoration.ITALIC;
		else if (this == MAGIC)
			return TextDecoration.OBFUSCATED;
		else if (this == RESET)
			return null;

		throw new RuntimeException("Cannot convert " + this + " to a text format");
	}

	/**
	 * Get the closest legacy color to this color.
	 *
	 * @return
	 */
	public String toClosestLegacy() {
		return getClosestLegacy(this.color).toString();
	}

	/**
	 * This will translate the color into the actual color, use getName to get the saveable color!
	 */
	@Override
	public String toString() {
		return this.toString;
	}

	/**
	 * @see #toSaveableString()
	 */
	@Override
	public String serialize() {
		return this.toSaveableString();
	}

	/**
	 * Get the color represented by the specified code.
	 *
	 * @param code the code to search for
	 * @return the mapped colour, or null if non exists
	 */
	public static CompChatColor getByChar(final char code) {
		return BY_CHAR.get(code);
	}

	/**
	 * Parse the given color to chat color
	 *
	 * @param color
	 * @return
	 */
	public static CompChatColor fromColor(final Color color) {
		return fromString("#" + Integer.toHexString(color.getRGB()).substring(2));
	}

	/**
	 * Get a color from #123456 HEX code, & color code or name
	 *
	 * @param string
	 * @return
	 */
	public static CompChatColor fromString(@NonNull String string) {
		if ("MAGENTA".equals(string.toUpperCase()))
			return LIGHT_PURPLE;

		if (string.charAt(0) == '<' && string.charAt(string.length() - 1) == '>')
			string = string.substring(1, string.length() - 1);

		if (string.charAt(0) == '#' && string.length() == 7) {
			if (MinecraftVersion.hasVersion() && MinecraftVersion.olderThan(MinecraftVersion.V.v1_16)) {
				final Color color = getColorFromHex(string);

				return getClosestLegacy(color);
			}

			int rgb;

			try {
				rgb = Integer.parseInt(string.substring(1), 16);

			} catch (final NumberFormatException ex) {
				throw new IllegalArgumentException("Illegal hex string " + string);
			}

			return new CompChatColor(string, "<" + string + ">", rgb);
		}

		if (string.length() == 2) {
			if (string.charAt(0) != '&')
				throw new IllegalArgumentException("Invalid syntax, please use & + color code. Got: " + string);

			final CompChatColor byChar = BY_CHAR.get(string.charAt(1));

			if (byChar != null)
				return byChar;

		} else {
			final CompChatColor byName = BY_NAME.get(string.toUpperCase(Locale.ROOT));

			if (byName != null)
				return byName;

			if (string.equalsIgnoreCase("magic"))
				return MAGIC;
		}

		throw new IllegalArgumentException("Could not parse CompChatColor " + string);
	}

	/**
	 * Return the parsed chat color from the given text color.
	 *
	 * @param color
	 * @return
	 */
	public static CompChatColor fromTextColor(final TextColor color) {
		return fromString(color.asHexString());
	}

	/**
	 * Return the parsed chat color from the given text decoration.
	 *
	 * @param decoration
	 * @return
	 */
	public static CompChatColor fromTextDecoration(final TextDecoration decoration) {
		if (decoration == TextDecoration.BOLD)
			return BOLD;
		else if (decoration == TextDecoration.STRIKETHROUGH)
			return STRIKETHROUGH;
		else if (decoration == TextDecoration.UNDERLINED)
			return UNDERLINE;
		else if (decoration == TextDecoration.ITALIC)
			return ITALIC;
		else if (decoration == TextDecoration.OBFUSCATED)
			return MAGIC;
		else
			throw new IllegalArgumentException("Unknown decoration: " + decoration);
	}

	/*
	 * Parse the given HEX into a Java Color object
	 */
	private static Color getColorFromHex(final String hex) {
		return new Color(Integer.parseInt(hex.substring(1, 3), 16), Integer.parseInt(hex.substring(3, 5), 16), Integer.parseInt(hex.substring(5, 7), 16));
	}

	/**
	 * Returns the closest legacy chat color from the given color.
	 *
	 * Uses all the available colors before HEX was added in MC 1.16.
	 *
	 * @param color
	 * @return
	 */
	public static CompChatColor getClosestLegacy(final Color color) {
		if (color.getAlpha() < 128)
			return null;

		int index = 0;
		double best = -1;

		for (int i = 0; i < LEGACY_COLORS.length; i++)
			if (areSimilar(LEGACY_COLORS[i], color))
				return CompChatColor.getColors().get(i);

		for (int i = 0; i < LEGACY_COLORS.length; i++) {
			final double distance = getDistance(color, LEGACY_COLORS[i]);

			if (distance < best || best == -1) {
				best = distance;
				index = i;
			}
		}

		return CompChatColor.getColors().get(index);
	}

	/*
	 * Return if colors are nearly identical
	 */
	private static boolean areSimilar(final Color first, final Color second) {
		return Math.abs(first.getRed() - second.getRed()) <= 5 &&
				Math.abs(first.getGreen() - second.getGreen()) <= 5 &&
				Math.abs(first.getBlue() - second.getBlue()) <= 5;

	}

	/*
	 * Returns how different two colors are
	 */
	private static double getDistance(final Color first, final Color second) {
		final double rmean = (first.getRed() + second.getRed()) / 2.0;
		final double r = first.getRed() - second.getRed();
		final double g = first.getGreen() - second.getGreen();
		final int b = first.getBlue() - second.getBlue();

		final double weightR = 2 + rmean / 256.0;
		final double weightG = 4.0;
		final double weightB = 2 + (255 - rmean) / 256.0;

		return weightR * r * r + weightG * g * g + weightB * b * b;
	}

	/**
	 * Replaces legacy & and MiniMessage color codes to paragraph character
	 *
	 * @param message
	 * @return
	 */
	public static String translateColorCodes(final String message) {
		final StringBuilder result = new StringBuilder();

		for (int i = 0; i < message.length(); i++) {
			if (message.charAt(i) == '<') {
				final int endIndex = message.indexOf('>', i);

				if (endIndex != -1) {
					final String code = message.substring(i, endIndex + 1);

					if (MINI_TO_LEGACY.containsKey(code)) {
						result.append(MINI_TO_LEGACY.get(code));
						i = endIndex;

						continue;
					}

					if (code.matches("<#[0-9a-fA-F]{6}>")) {
						appendHex(result, code.substring(1, code.length() - 1));

						i = endIndex;
						continue;
					}
				}

			} else if (i + 6 < message.length() && message.charAt(i) == '#' && message.substring(i + 1, i + 7).matches("[0-9a-fA-F]{6}")) {
				appendHex(result, message.substring(i, i + 7));

				i += 6;
				continue;

			} else if (message.charAt(i) == '&' && i + 1 < message.length() && ALL_CODES.indexOf(message.charAt(i + 1)) > -1) {
				result.append(CompChatColor.COLOR_CHAR).append(Character.toLowerCase(message.charAt(i + 1)));

				i += 1;
				continue;
			}

			result.append(message.charAt(i));
		}

		return result.toString();
	}

	/**
	 * Returns if the message contains & or § color codes, or MiniMessage tags.
	 *
	 * @param message
	 * @return
	 */
	public static boolean hasLegacyColors(final String message) {
		return LEGACY_COLOR_MATCH.matcher(message.toLowerCase()).find();
	}

	/*
	 * Append a hex color to the result
	 */
	private static void appendHex(final StringBuilder result, final String code) {
		if (MinecraftVersion.hasVersion() && MinecraftVersion.olderThan(MinecraftVersion.V.v1_16))
			result.append(getClosestLegacy(getColorFromHex(code)));

		else
			result.append(COLOR_CHAR).append("x")
					.append(COLOR_CHAR).append(code.charAt(1))
					.append(COLOR_CHAR).append(code.charAt(2))
					.append(COLOR_CHAR).append(code.charAt(3))
					.append(COLOR_CHAR).append(code.charAt(4))
					.append(COLOR_CHAR).append(code.charAt(5))
					.append(COLOR_CHAR).append(code.charAt(6));
	}

	/**
	 * Removes valid Minecraft color codes from a message. Valid color codes are sequences of
	 * '§' or '&' followed by a character in the ranges 0-9, a-f, A-F, k-o, K-O, or r/R.
	 *
	 * @param message The input message potentially containing Minecraft color codes.
	 * @return A new string with valid color codes removed.
	 */
	public static String stripColorCodes(final String message) {
		return stripColorCodes(message, true);
	}

	/**
	 * Removes valid Minecraft color codes from a message. Valid color codes are sequences of
	 * '§' or '&' followed by a character in the ranges 0-9, a-f, A-F, k-o, K-O, or r/R.
	 *
	 * @param message The input message potentially containing Minecraft color codes.
	 * @param ampersand True if '&' should be considered a color code as well.
	 * @return A new string with valid color codes removed.
	 */
	public static String stripColorCodes(final String message, final boolean ampersand) {
		final int messageLength = message.length();
		final char[] strippedMessage = new char[messageLength];
		int resultIndex = 0;

		for (int i = 0; i < messageLength; i++) {
			final char currentChar = message.charAt(i);

			if ((currentChar == '§' || (ampersand && currentChar == '&')) && i + 1 < messageLength) {
				final char nextChar = message.charAt(i + 1);

				if ((nextChar >= '0' && nextChar <= '9') ||
						(nextChar >= 'a' && nextChar <= 'f') ||
						(nextChar >= 'A' && nextChar <= 'F') ||
						(nextChar >= 'k' && nextChar <= 'o') ||
						(nextChar >= 'K' && nextChar <= 'O') ||
						nextChar == 'r' || nextChar == 'R' ||
						nextChar == 'x')
					i++; // Skip the valid color code
				else
					strippedMessage[resultIndex++] = currentChar;

			} else
				strippedMessage[resultIndex++] = currentChar;
		}

		return new String(strippedMessage, 0, resultIndex);
	}

	/**
	 * Gets the ChatColors used at the end of the given input string.
	 *
	 * @param input Input string to retrieve the colors from.
	 * @return Any remaining ChatColors to pass onto the next line.
	 */
	public static String getLastColors(final String input) {
		if (input == null)
			return "";

		String result = "";
		final int length = input.length();

		// Search backwards from the end as it is faster
		for (int index = length - 1; index > -1; index--) {
			final char section = input.charAt(index);

			if (section == COLOR_CHAR && index < length - 1) {
				final String hexColor = getHexColor(input, index);

				if (hexColor != null) {
					// We got a hex color
					result = hexColor + result;

					break;
				}

				// It is not a hex color, check normal color
				final char c = input.charAt(index + 1);
				final CompChatColor color = getByChar(c);

				if (color != null) {
					result = color.toString() + result;

					// Once we find a color or reset we can stop searching
					if (color.isColor() || color.equals(RESET))
						break;
				}
			}
		}

		return result;
	}

	/*
	 * Get a hex color from the input string, copied from ChatColor class.
	 */
	private static String getHexColor(final String input, final int index) {
		// Check for hex color with the format '§x§1§2§3§4§5§6'
		// Our index is currently on the last '§' which means to have a potential hex color
		// The index - 11 must be an 'x' and index - 12 must be a '§'
		// But first check if the string is long enough
		if (index < 12)
			return null;

		if (input.charAt(index - 11) != 'x' || input.charAt(index - 12) != COLOR_CHAR)
			return null;

		// We got a potential hex color
		// Now check if every the chars switches between '§' and a hex number
		// First check '§'
		for (int i = index - 10; i <= index; i += 2)
			if (input.charAt(i) != COLOR_CHAR)
				return null;

		for (int i = index - 9; i <= (index + 1); i += 2) {
			final char toCheck = input.charAt(i);

			if (toCheck < '0' || toCheck > 'f')
				return null;

			if (toCheck > '9' && toCheck < 'A')
				return null;

			if (toCheck > 'F' && toCheck < 'a')
				return null;
		}

		// We got a hex color return it
		return input.substring(index - 12, index + 2);
	}

	/**
	 * Replace legacy & color codes to MiniMessage tags. Does NOT parse the message.
	 *
	 * @param message
	 * @param supportAmpersand
	 * @return
	 */
	public static String convertLegacyToMini(final String message, final boolean supportAmpersand) {
		final StringBuilder result = new StringBuilder();

		for (int i = 0; i < message.length(); i++) {

			// Support §x§R§R§G§G§B§B hex colors
			if (i + 13 < message.length() && message.charAt(i) == '§' && message.charAt(i + 1) == 'x') {
				final StringBuilder hex = new StringBuilder("#");
				boolean isValidHexSequence = true;

				for (int j = 2; j <= 12; j += 2) {
					if (message.charAt(i + j) == '§')
						hex.append(message.charAt(i + j + 1));

					else {
						isValidHexSequence = false;

						break;
					}
				}

				if (isValidHexSequence) {
					result.append('<').append(hex).append('>');
					i += 13; // Skip the entire §x§R§R§G§G§B§B sequence

					continue;
				}
			}

			if (i + 1 < message.length() && ((message.charAt(i) == '&' && supportAmpersand) || message.charAt(i) == '§')) {
				final String code = message.substring(i, i + 2);

				if (LEGACY_TO_MINI.containsKey(code)) {
					result.append(LEGACY_TO_MINI.get(code));
					i++;

					continue;
				}
			}

			result.append(message.charAt(i));
		}

		return result.toString();
	}

	/**
	 * Converts the mini tags in the legacy message to section.
	 *
	 * @param minimessage
	 * @return
	 */
	public static String convertMiniToLegacy(final String minimessage) {
		final StringBuilder filteredMessage = new StringBuilder();

		// Stack to store open tags
		final Deque<String> tagStack = new ArrayDeque<>();

		final int length = minimessage.length();
		for (int i = 0; i < length; i++) {
			final char currentChar = minimessage.charAt(i);

			// Check for escaped tags prefixed with \
			if (currentChar == '\\' && i + 1 < length && minimessage.charAt(i + 1) == '<') {

				// Append the backslash and the '<' as is
				filteredMessage.append('\\').append('<');
				i++; // Skip the next character ('<')

				continue;
			}

			// Check for opening of a MiniMessage tag, e.g., <color>
			if (currentChar == '<') {
				final int closeIndex = minimessage.indexOf('>', i);

				// If next '>' is not found or tag is malformed, treat it as normal text
				if (closeIndex == -1 || minimessage.substring(i + 1, closeIndex).contains("<")) {
					filteredMessage.append(currentChar);

					continue;
				}

				final String tagContent = minimessage.substring(i + 1, closeIndex).toLowerCase();

				// Check for end tag, e.g., </red>
				if (tagContent.charAt(0) == '/') {
					final String endTag = tagContent.substring(1);

					if (!isValidTag(endTag)) {
						i = closeIndex;

						continue;
					}

					// Upon detecting an end tag, remove the tag from the stack
					if (!tagStack.isEmpty() && tagStack.peek().equals(endTag)) {
						tagStack.pop(); // Remove the matching start tag

						// Output the color code for the new top of the stack (if any), else reset color
						String colorCode;

						if (!tagStack.isEmpty()) {
							final String currentTag = tagStack.peek();
							colorCode = CompChatColor.MINI_TO_LEGACY.get("<" + currentTag + ">");

							if (currentTag.charAt(0) == '#' && currentTag.length() == 7)
								colorCode = CompChatColor.getClosestLegacy(getColorFromHex(currentTag)).toString();

							if (colorCode != null)
								filteredMessage.append(colorCode);

						} else

							// Reset formatting if no tags are left
							filteredMessage.append(CompChatColor.RESET.toString());
					}

					// Move past the end tag
					i = closeIndex;
					continue;

				} else {
					String tagName;

					if (tagContent.startsWith("color:"))
						tagName = tagContent.substring(6);
					else if (tagContent.startsWith("colour:"))
						tagName = tagContent.substring(7);
					else if (tagContent.startsWith("c:"))
						tagName = tagContent.substring(2);
					else
						tagName = tagContent;

					if (!isValidTag(tagName)) {
						i = closeIndex;

						continue;
					}

					// If tag is permitted, push it onto the stack and output its color code
					tagStack.push(tagName);

					String colorCode = CompChatColor.MINI_TO_LEGACY.get("<" + tagName + ">");

					if (tagName.charAt(0) == '#' && tagName.length() == 7)
						colorCode = CompChatColor.getClosestLegacy(getColorFromHex(tagName)).toString();

					if (colorCode != null)
						filteredMessage.append(colorCode);

					// Move past the tag
					i = closeIndex;
					continue;
				}

			} else

				// Normal character
				filteredMessage.append(currentChar);

		}

		// Ensure all tags have been closed
		while (!tagStack.isEmpty()) {
			tagStack.pop();

			filteredMessage.append(CompChatColor.RESET.toString());
		}

		return filteredMessage.toString();
	}

	/*
	 * Check if the sender has permission for the given color name.
	 */
	private static boolean isValidTag(String tag) {
		if (tag.isEmpty())
			return true;

		if (tag.charAt(0) == '#') {
			if (tag.length() == 7)
				return true;

			return false; // Disallow invalid tags to prevent exploits
		}

		tag = tag.toLowerCase();

		switch (tag) {
			case "grey":
				tag = "gray";
				break;
			case "dark_grey":
				tag = "dark_gray";
				break;
			case "insert":
				tag = "insertion";
				break;
			default:
				if (tag.contains(":"))
					tag = tag.split(":", 2)[0];

				break;
		}

		return "reset".equals(tag) || "b".equals(tag) || "bold".equals(tag) || "i".equals(tag) || "italic".equals(tag) || "u".equals(tag) || "underlined".equals(tag) || "st".equals(tag)
				|| "strikethrough".equals(tag) || "obf".equals(tag) || "obfuscated".equals(tag) || NamedTextColor.NAMES.value(tag) != null;
	}

	/**
	 * Get an array of all defined colors and formats.
	 *
	 * @return copied array of all colors and formats
	 */
	public static CompChatColor[] values() {
		return BY_CHAR.values().toArray(new CompChatColor[BY_CHAR.size()]);
	}

	/**
	 * Return a list of all colors
	 *
	 * @return
	 */
	public static List<CompChatColor> getColors() {
		return Arrays.asList(BLACK, DARK_BLUE, DARK_GREEN, DARK_AQUA, DARK_RED, DARK_PURPLE, GOLD, GRAY, DARK_GRAY, BLUE, GREEN, AQUA, RED, LIGHT_PURPLE, YELLOW, WHITE);
	}

	/**
	 * Return a list of all decorations
	 *
	 * @return
	 */
	public static List<CompChatColor> getDecorations() {
		return Arrays.asList(MAGIC, BOLD, STRIKETHROUGH, UNDERLINE, ITALIC);
	}

	static {
		MINI_TO_LEGACY.put("<black>", "§0");
		MINI_TO_LEGACY.put("<dark_blue>", "§1");
		MINI_TO_LEGACY.put("<dark_green>", "§2");
		MINI_TO_LEGACY.put("<dark_aqua>", "§3");
		MINI_TO_LEGACY.put("<dark_red>", "§4");
		MINI_TO_LEGACY.put("<dark_purple>", "§5");
		MINI_TO_LEGACY.put("<gold>", "§6");
		MINI_TO_LEGACY.put("<gray>", "§7");
		MINI_TO_LEGACY.put("<dark_gray>", "§8");
		MINI_TO_LEGACY.put("<blue>", "§9");
		MINI_TO_LEGACY.put("<green>", "§a");
		MINI_TO_LEGACY.put("<aqua>", "§b");
		MINI_TO_LEGACY.put("<red>", "§c");
		MINI_TO_LEGACY.put("<light_purple>", "§d");
		MINI_TO_LEGACY.put("<yellow>", "§e");
		MINI_TO_LEGACY.put("<white>", "§f");
		MINI_TO_LEGACY.put("<u>", "§n");
		MINI_TO_LEGACY.put("<underlined>", "§n");
		MINI_TO_LEGACY.put("<st>", "§m");
		MINI_TO_LEGACY.put("<strikethrough>", "§m");
		MINI_TO_LEGACY.put("<obf>", "§k");
		MINI_TO_LEGACY.put("<obfuscated>", "§k");
		MINI_TO_LEGACY.put("<i>", "§o");
		MINI_TO_LEGACY.put("<italic>", "§o");
		MINI_TO_LEGACY.put("<b>", "§l");
		MINI_TO_LEGACY.put("<bold>", "§l");
		MINI_TO_LEGACY.put("<r>", "§r");
		MINI_TO_LEGACY.put("<reset>", "§r");

		MINI_TO_COLOR.put("<black>", NamedTextColor.BLACK);
		MINI_TO_COLOR.put("<dark_blue>", NamedTextColor.DARK_BLUE);
		MINI_TO_COLOR.put("<dark_green>", NamedTextColor.DARK_GREEN);
		MINI_TO_COLOR.put("<dark_aqua>", NamedTextColor.DARK_AQUA);
		MINI_TO_COLOR.put("<dark_red>", NamedTextColor.DARK_RED);
		MINI_TO_COLOR.put("<dark_purple>", NamedTextColor.DARK_PURPLE);
		MINI_TO_COLOR.put("<gold>", NamedTextColor.GOLD);
		MINI_TO_COLOR.put("<gray>", NamedTextColor.GRAY);
		MINI_TO_COLOR.put("<dark_gray>", NamedTextColor.DARK_GRAY);
		MINI_TO_COLOR.put("<blue>", NamedTextColor.BLUE);
		MINI_TO_COLOR.put("<green>", NamedTextColor.GREEN);
		MINI_TO_COLOR.put("<aqua>", NamedTextColor.AQUA);
		MINI_TO_COLOR.put("<red>", NamedTextColor.RED);
		MINI_TO_COLOR.put("<light_purple>", NamedTextColor.LIGHT_PURPLE);
		MINI_TO_COLOR.put("<yellow>", NamedTextColor.YELLOW);
		MINI_TO_COLOR.put("<white>", NamedTextColor.WHITE);

		MINI_TO_DECORATION.put("<u>", TextDecoration.UNDERLINED);
		MINI_TO_DECORATION.put("<underlined>", TextDecoration.UNDERLINED);
		MINI_TO_DECORATION.put("<st>", TextDecoration.STRIKETHROUGH);
		MINI_TO_DECORATION.put("<strikethrough>", TextDecoration.STRIKETHROUGH);
		MINI_TO_DECORATION.put("<obf>", TextDecoration.OBFUSCATED);
		MINI_TO_DECORATION.put("<obfuscated>", TextDecoration.OBFUSCATED);
		MINI_TO_DECORATION.put("<i>", TextDecoration.ITALIC);
		MINI_TO_DECORATION.put("<italic>", TextDecoration.ITALIC);
		MINI_TO_DECORATION.put("<b>", TextDecoration.BOLD);
		MINI_TO_DECORATION.put("<bold>", TextDecoration.BOLD);
		MINI_TO_DECORATION.put("<r>", null);
		MINI_TO_DECORATION.put("<reset>", null);

		LEGACY_TO_MINI.put("&0", "<black>");
		LEGACY_TO_MINI.put("&1", "<dark_blue>");
		LEGACY_TO_MINI.put("&2", "<dark_green>");
		LEGACY_TO_MINI.put("&3", "<dark_aqua>");
		LEGACY_TO_MINI.put("&4", "<dark_red>");
		LEGACY_TO_MINI.put("&5", "<dark_purple>");
		LEGACY_TO_MINI.put("&6", "<gold>");
		LEGACY_TO_MINI.put("&7", "<gray>");
		LEGACY_TO_MINI.put("&8", "<dark_gray>");
		LEGACY_TO_MINI.put("&9", "<blue>");
		LEGACY_TO_MINI.put("§0", "<black>");
		LEGACY_TO_MINI.put("§1", "<dark_blue>");
		LEGACY_TO_MINI.put("§2", "<dark_green>");
		LEGACY_TO_MINI.put("§3", "<dark_aqua>");
		LEGACY_TO_MINI.put("§4", "<dark_red>");
		LEGACY_TO_MINI.put("§5", "<dark_purple>");
		LEGACY_TO_MINI.put("§6", "<gold>");
		LEGACY_TO_MINI.put("§7", "<gray>");
		LEGACY_TO_MINI.put("§8", "<dark_gray>");
		LEGACY_TO_MINI.put("§9", "<blue>");

		LEGACY_TO_MINI.put("&a", "<green>");
		LEGACY_TO_MINI.put("&b", "<aqua>");
		LEGACY_TO_MINI.put("&c", "<red>");
		LEGACY_TO_MINI.put("&d", "<light_purple>");
		LEGACY_TO_MINI.put("&e", "<yellow>");
		LEGACY_TO_MINI.put("&f", "<white>");
		LEGACY_TO_MINI.put("&A", "<green>");
		LEGACY_TO_MINI.put("&B", "<aqua>");
		LEGACY_TO_MINI.put("&C", "<red>");
		LEGACY_TO_MINI.put("&D", "<light_purple>");
		LEGACY_TO_MINI.put("&E", "<yellow>");
		LEGACY_TO_MINI.put("&F", "<white>");

		LEGACY_TO_MINI.put("§a", "<green>");
		LEGACY_TO_MINI.put("§b", "<aqua>");
		LEGACY_TO_MINI.put("§c", "<red>");
		LEGACY_TO_MINI.put("§d", "<light_purple>");
		LEGACY_TO_MINI.put("§e", "<yellow>");
		LEGACY_TO_MINI.put("§f", "<white>");
		LEGACY_TO_MINI.put("§A", "<green>");
		LEGACY_TO_MINI.put("§B", "<aqua>");
		LEGACY_TO_MINI.put("§C", "<red>");
		LEGACY_TO_MINI.put("§D", "<light_purple>");
		LEGACY_TO_MINI.put("§E", "<yellow>");
		LEGACY_TO_MINI.put("§F", "<white>");

		LEGACY_TO_MINI.put("&n", "<u>");
		LEGACY_TO_MINI.put("&m", "<st>");
		LEGACY_TO_MINI.put("&k", "<obf>");
		LEGACY_TO_MINI.put("&o", "<i>");
		LEGACY_TO_MINI.put("&l", "<b>");
		LEGACY_TO_MINI.put("&r", "<reset>");
		LEGACY_TO_MINI.put("&N", "<u>");
		LEGACY_TO_MINI.put("&M", "<st>");
		LEGACY_TO_MINI.put("&K", "<obf>");
		LEGACY_TO_MINI.put("&O", "<i>");
		LEGACY_TO_MINI.put("&L", "<b>");
		LEGACY_TO_MINI.put("&R", "<reset>");

		LEGACY_TO_MINI.put("§n", "<u>");
		LEGACY_TO_MINI.put("§m", "<st>");
		LEGACY_TO_MINI.put("§k", "<obf>");
		LEGACY_TO_MINI.put("§o", "<i>");
		LEGACY_TO_MINI.put("§l", "<b>");
		LEGACY_TO_MINI.put("§r", "<reset>");
		LEGACY_TO_MINI.put("§N", "<u>");
		LEGACY_TO_MINI.put("§M", "<st>");
		LEGACY_TO_MINI.put("§K", "<obf>");
		LEGACY_TO_MINI.put("§O", "<i>");
		LEGACY_TO_MINI.put("§L", "<b>");
		LEGACY_TO_MINI.put("§R", "<reset>");
	}

	/**
	 * The color, as an RGB value packed into an int.
	 *
	 */
	@Override
	public int value() {
		return this.color.getRGB();
	}
}
