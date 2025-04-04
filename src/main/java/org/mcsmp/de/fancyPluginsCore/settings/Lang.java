package org.mcsmp.de.fancyPluginsCore.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.mcsmp.de.fancyPluginsCore.exception.FoException;
import org.mcsmp.de.fancyPluginsCore.model.CaseNumberFormat;
import org.mcsmp.de.fancyPluginsCore.model.FancyComponent;
import org.mcsmp.de.fancyPluginsCore.model.Variables;
import org.mcsmp.de.fancyPluginsCore.platform.Platform;
import org.mcsmp.de.fancyPluginsCore.utility.ChatUtil;
import org.mcsmp.de.fancyPluginsCore.utility.CommonCore;
import org.mcsmp.de.fancyPluginsCore.utility.FileUtil;
import org.mcsmp.de.fancyPluginsCore.utility.ValidCore;

import java.io.File;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Represents a localization system for your plugin. All localization keys
 * are stored in a json file with no nested keys, and can be layered.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Lang {

	/**
	 * The instance of this class
	 */
	private static final Lang instance = new Lang();

	/*
	 * The entire dictionary with all keys, unchanged, as a JsonObject.
	 */
	private JsonObject dictionary;

	/*
	 * The different caches for maximum performance. Legacy caches contain
	 * keys with MiniMessage and & colors translated to §.
	 */
	private Map<String, String> plainCache;
	private Map<String, String> legacyCache;
	private Map<String, FancyComponent> componentCache;
	private final Map<String, CaseNumberFormat> numberFormatCache = new HashMap<>();

	/*
	 * Return a plain String from the language file, throwing an error if the key is missing.
	 */
	private String getPlain(final String path) {
		ValidCore.checkNotNull(this.plainCache, "Dictionary not loaded yet! Call Lang.Storage.download() first!");
		ValidCore.checkBoolean(this.plainCache.containsKey(path), "Missing localization key '" + path + "'");

		return this.plainCache.get(path);
	}

	/*
	 * Return a legacy key from the given path in the language file.
	 */
	private String getLegacy(final String path) {
		ValidCore.checkNotNull(this.legacyCache, "Dictionary not loaded yet! Call Lang.Storage.download() first!");
		ValidCore.checkBoolean(this.legacyCache.containsKey(path), "Missing localization key '" + path + "'");

		return this.legacyCache.get(path);
	}

	/*
	 * Return a CaseNumberFormat from the given path in the language file,
	 * caching the result if it does not exist.
	 */
	private CaseNumberFormat getCaseNumberFormat(final String path) {
		CaseNumberFormat format = this.numberFormatCache.get(path);

		if (format == null) {
			format = CaseNumberFormat.fromString(this.getPlain(path));

			this.numberFormatCache.put(path, format);
		}

		return format;
	}

	/*
	 * Return a component from the given path in the language file.
	 */
	private FancyComponent getComponent(final String path) {
		ValidCore.checkNotNull(this.componentCache, "Dictionary not loaded yet! Call Lang.Storage.download() first!");
		ValidCore.checkBoolean(this.componentCache.containsKey(path), "Missing localization key '" + path + "'");

		return this.componentCache.get(path);
	}

	/*
	 * Return true if the plain cache has a key at the given path.
	 */
	private boolean has(final String path) {
		return this.plainCache.containsKey(path);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Getters
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the entire dictionary with all keys, unchanged, as a JsonObject.
	 *
	 * @return
	 */
	public static JsonObject dictionary() {
		return instance.dictionary;
	}

	/**
	 * Return if the given key exists in the language file.
	 *
	 * @see Storage#load()
	 *
	 * @param path
	 * @return
	 */
	public static boolean exists(final String path) {
		return instance.has(path);
	}

	/**
	 * Return a plain String from the language file, throwing an error if the key is missing.
	 *
	 * No modifications are done to the key.
	 *
	 * @param path
	 * @return
	 */
	public static String plain(final String path) {
		return instance.getPlain(path);
	}

	/**
	 * Return a legacy key from the given path in the language file.
	 *
	 * Throws an error if the key is missing.
	 *
	 * Variables are supported, where key must be a string and value either a string or
	 * FancyComponent, or a list of either.
	 *
	 * Example: legacyVars("my-locale-path", "arena", arena.getName()) translates {arena}
	 * key from the locale path.
	 *
	 * MiniMessage tags and & legacy colors are translated to §.
	 *
	 * @param path
	 * @param placeholders
	 * @return
	 */
	public static String legacy(final String path, final Object... placeholders) {
		final String value = instance.getLegacy(path);
		final Variables variables = Variables.builder();

		if (placeholders != null && placeholders.length > 0)
			variables.placeholderArray(placeholders);

		return variables.replaceLegacy(value);
	}

	/**
	 * Return a legacy key from the given path in the language file.
	 *
	 * Throws an error if the key is missing.
	 *
	 * Variables are supported, where key must be a string and value either a string or
	 * FancyComponent, or a list of either.
	 *
	 * Example: legacyVars("my-locale-path", "arena", arena.getName()) translates {arena}
	 * key from the locale path.
	 *
	 * MiniMessage tags and & legacy colors are translated to §.
	 *
	 * @param path
	 * @param placeholders
	 * @return
	 */
	public static String legacy(final String path, final Map<String, Object> placeholders) {
		final String value = instance.getLegacy(path);
		final Variables variables = Variables.builder();

		if (placeholders != null)
			variables.placeholders(placeholders);

		return variables.replaceLegacy(value);
	}

	/**
	 * Return a component from the given path in the language file.
	 *
	 * Throws an error if the key is missing.
	 *
	 * Variables are supported, where key must be a string and value either a string or
	 * FancyComponent, or a list of either.
	 *
	 * Example: componentVars("my-locale-path", "arena", arena.getName()) translates {arena}
	 * key from the locale path.
	 *
	 * @param path
	 * @param placeholders
	 * @return
	 */
	public static FancyComponent component(final String path, final Object... placeholders) {
		return component(Variables.builder(), path, placeholders);
	}

	/**
	 * Return a component from the given path in the language file.
	 *
	 * Throws an error if the key is missing.
	 *
	 * Variables are supported, where key must be a string and value either a string or
	 * FancyComponent, or a list of either.
	 *
	 * Example: componentVars("my-locale-path", "arena", arena.getName()) translates {arena}
	 * key from the locale path.
	 *
	 * @param variables
	 * @param path
	 * @param placeholders
	 * @return
	 */
	public static FancyComponent component(final Variables variables, final String path, final Object... placeholders) {
		final FancyComponent component = instance.getComponent(path);

		if (placeholders != null && placeholders.length > 0)
			variables.placeholderArray(placeholders);

		return variables.replaceComponent(component);
	}

	/**
	 * Return a prefix or null if set to none
	 *
	 * @param path
	 * @return the prefix or null
	 *
	 * @deprecated internal use only
	 */
	@Deprecated
	public static FancyComponent prefix(final String path) {
		return instance.componentCache.get(path);
	}

	/**
	 * Return a prefix or null if set to none
	 *
	 * @param path
	 * @return the prefix or null
	 *
	 * @deprecated internal use only
	 */
	@Deprecated
	public static FancyComponent prefixOrEmpty(final String path) {
		return instance.componentCache.getOrDefault(path, FancyComponent.empty());
	}

	/**
	 * Return a component from the given path in the language file.
	 *
	 * Throws an error if the key is missing.
	 *
	 * Variables are supported, where key must be a string and value either a string or
	 * FancyComponent, or a list of either.
	 *
	 * Example: componentVars("my-locale-path", "arena", arena.getName()) translates {arena}
	 * key from the locale path.
	 *
	 * @param path
	 * @param placeholders
	 * @return
	 */
	public static FancyComponent component(final String path, final Map<String, Object> placeholders) {
		final FancyComponent component = instance.getComponent(path);
		final Variables variables = Variables.builder();

		if (placeholders != null)
			variables.placeholders(placeholders);

		return variables.replaceComponent(component);
	}

	/**
	 * Return a String key from the given path in the language file.
	 *
	 * Singular or plural form is automatically chosen based on the amount and the
	 * result includes the amount itself.
	 *
	 * Throws an error if the key is missing.
	 *
	 * Example: numberFormat("case-apples", 5) returns "5 apples" if the
	 * key at "case-apples" is "apple, apples"
	 *
	 * @param amount
	 * @param path
	 * @return
	 */
	public static String numberFormat(final String path, final long amount) {
		return instance.getCaseNumberFormat(path).formatWithCount(amount);
	}

	/**
	 * Return a String key from the given path in the language file.
	 *
	 * Singular or plural form is automatically chosen based on the amount and the
	 * result excludes the amount.
	 *
	 * Throws an error if the key is missing.
	 *
	 * Example: numberFormat("case-apples", 5) returns "apples" if the
	 * key at "case-apples" is "apple, apples"
	 *
	 * @param amount
	 * @param path
	 * @return
	 */
	public static String numberFormatNoAmount(final String path, final long amount) {
		return instance.getCaseNumberFormat(path).formatWithoutCount(amount);
	}

	/**
	 * Stores some default keys from the main overlay that need to be initialized into a
	 * class for maximum performance.
	 */
	public static final class Default {

		/**
		 * The {date}, {date_short} and {date_month} formats.
		 */
		private static DateFormat dateFormat;
		private static DateFormat dateFormatShort;
		private static DateFormat dateFormatMonth;

		/**
		 * The format used in the {date} placeholder.
		 *
		 * @see Variables
		 *
		 * @return
		 */
		public static DateFormat getDateFormat() {
			if (dateFormat == null)
				dateFormat = makeFormat("format-date", "dd.MM.yyyy HH:mm:ss");

			return dateFormat;
		}

		/**
		 * The format used in the {date_short} placeholder.
		 *
		 * @see Variables
		 *
		 * @return
		 */
		public static DateFormat getDateFormatShort() {
			if (dateFormatShort == null)
				dateFormatShort = makeFormat("format-date-short", "dd.MM.yyyy HH:mm");

			return dateFormatShort;
		}

		/**
		 * The format used in the {date_month} placeholder.
		 *
		 * @see Variables
		 *
		 * @return
		 */
		public static DateFormat getDateFormatMonth() {
			if (dateFormatMonth == null)
				dateFormatMonth = makeFormat("format-date-month", "dd.MM HH:mm");

			return dateFormatMonth;
		}

		/*
		 * A helper method to create a date format from the given plain lang key.
		 */
		private static DateFormat makeFormat(final String key, final String def) {
			final String raw = exists(key) ? plain(key) : def;

			try {
				return new SimpleDateFormat(raw);

			} catch (final IllegalArgumentException ex) {
				CommonCore.throwError(ex, "Date format at '" + key + "' is invalid: '" + raw + "'! See https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html for syntax'");

				return null;
			}
		}
	}

	/**
	 * A class that handles downloading the localization keys.
	 */
	public static final class Storage {

		/**
		 * Will dump the locale keys onto the lang/xx_YY.json file.
		 *
		 * Existing keys will be preserved, new keys will be added, and unused keys will be removed.
		 *
		 * If the file does not exist, it will be created.
		 *
		 * @return
		 */
		public static File createAndDumpToFile() {
			return dumpToFile0(true);
		}

		/**
		 * Will update the locale keys in the lang/xx_YY.json file if the file exists.
		 *
		 * Existing keys will be preserved, new keys will be added, and unused keys will be removed.
		 *
		 * @return
		 */
		public static File updateFileIfExists() {
			return dumpToFile0(false);
		}

		/*
		 * Implementation of the file dump.
		 */
		private static File dumpToFile0(final boolean createFileIfNotExists) {
			final String path = "lang/" + FancySettings.LOCALE + ".json";
			final File localFile = FileUtil.getFile(path);

			if (!localFile.exists()) {
				if (createFileIfNotExists)
					FileUtil.createIfNotExists(path);
				else
					return localFile;
			}

			JsonObject localJson;

			try {
				localJson = CommonCore.GSON.fromJson(String.join("\n", FileUtil.readLinesFromFile(localFile)), JsonObject.class);

			} catch (final JsonSyntaxException ex) {
				throw new FoException("Invalid JSON in " + localFile + " file. Use services like https://jsonformatter.org/ to correct it. Error: " + ex.getMessage(), false);
			}

			if (localJson == null)
				localJson = new JsonObject();

			// First, remove local keys that no longer exist in our dictionary
			for (final Map.Entry<String, JsonElement> entry : localJson.entrySet()) {
				final String key = entry.getKey();

				if (!instance.dictionary.has(key)) {
					CommonCore.log("Removing unused key '" + key + "' from locale file " + localFile);

					localJson.remove(key);
				}
			}

			// Then, add new keys to the local file
			for (final Map.Entry<String, JsonElement> entry : instance.dictionary.entrySet()) {
				final String key = entry.getKey();

				if (!localJson.has(key)) {
					CommonCore.log("Adding new key '" + key + "' from locale file " + localFile);

					localJson.add(key, instance.dictionary.get(key));
				}
			}

			// Trick to sort keys.
			final String unsortedDump = CommonCore.GSON_PRETTY.toJson(localJson);
			final Map<String, Object> map = CommonCore.GSON.fromJson(unsortedDump, TreeMap.class);

			FileUtil.write(localFile, Arrays.asList(CommonCore.GSON_PRETTY.toJson(map)), StandardOpenOption.TRUNCATE_EXISTING);

			return localFile;
		}

		/**
		 * Load the localization keys from the plugin jar and disk. This is done in layers,
		 * first come the base overlay which should be shipped in lang/overlay/xx_YY.json in Foundation,
		 * then the plugin-specific overlay in lang/yy_XX.json, and lastly the file in lang/xx_YY.json file on disk.
		 *
		 * Each load is further split into first loading the English keys and then the language specific
		 * if they exists.
		 *
		 * The code is further split into {} because I love it.
		 */
		public static void load() {
			final String englishLangTag = Locale.US.getLanguage() + "_" + Locale.US.getCountry();
			final boolean isEnglish = FancySettings.LOCALE.equals("en_US");

			List<String> content;
			final JsonObject dictionary = new JsonObject();

			// Set early to make dumpLocale work to update old files
			instance.dictionary = dictionary;

			// Foundation locale
			{
				content = FileUtil.readLinesFromInternalPath("lang/overlay/" + englishLangTag + ".json");

				// Base overlay must be set
				ValidCore.checkNotNull(content, "Locale file lang/overlay/en_US.json is missing! Did you reload or used PlugMan(X)? Make sure Foundation is shaded properly!");
				putToDictionary(dictionary, content);

				// Language specific base overlay can be null
				if (!isEnglish) {
					content = FileUtil.readLinesFromInternalPath("lang/overlay/" + FancySettings.LOCALE + ".json");

					putToDictionary(dictionary, content);
				}

			}

			// Plugin-specific, in jar
			{
				// Optional
				content = FileUtil.readLinesFromInternalPath("lang/" + englishLangTag + ".json");
				putToDictionary(dictionary, content);

				if (!isEnglish) {

					// Base overlay must be set when using non-English locale
					ValidCore.checkNotNull(content, "When using non-English locale (" + FancySettings.LOCALE + "), the base overlay en_US.json must exists in " + Platform.getPlugin().getName());

					content = FileUtil.readLinesFromInternalPath("lang/" + FancySettings.LOCALE + ".json");

					if (content != null)
						putToDictionary(dictionary, content);

					else
						CommonCore.warning("No such localization: " + FancySettings.LOCALE + " in plugin's jar, using keys from the disk file or from the default English locale for keys that are missing.");
				}
			}

			// On disk
			{
				// Start with base locale as overlay
				content = FileUtil.readLinesFromFile("lang/" + englishLangTag + ".json");

				if (content != null)
					try {
						putToDictionary(dictionary, content);
					} catch (final JsonSyntaxException ex) {
						CommonCore.warning("Invalid syntax in localization file " + englishLangTag + ". Use services like https://jsonformatter.org/ to correct it. Error: " + ex.getMessage());
					}

				if (!isEnglish) {
					content = FileUtil.readLinesFromFile("lang/" + FancySettings.LOCALE + ".json");

					if (content != null)
						try {
							putToDictionary(dictionary, content);

						} catch (final JsonSyntaxException ex) {
							CommonCore.warning("Invalid syntax in localization file " + FancySettings.LOCALE + ". Use services like https://jsonformatter.org/ to correct it. Error: " + ex.getMessage());
						}
				}
			}

			// At last, update the dictionary on disk if the file exists
			updateFileIfExists();

			// Cache all the keys for maximum performance
			final Map<String, String> plainCache = new HashMap<>();
			final Map<String, String> legacyCache = new HashMap<>();
			final Map<String, FancyComponent> componentCache = new HashMap<>();

			for (final Map.Entry<String, JsonElement> entry : dictionary.entrySet()) {
				final String key = entry.getKey();
				final JsonElement value = dictionary.get(key);

				if (value.isJsonPrimitive()) {
					String string = value.getAsString();

					if (string.isEmpty())
						string = "none";

					if (key.startsWith("prefix-") && "none".equals(string)) {
						// ignore
					} else {
						final FancyComponent component = FancyComponent.fromMiniAmpersand(string);

						plainCache.put(key, string);
						componentCache.put(key, component);
						legacyCache.put(key, component.toLegacySection(null));
					}
				}

				// else if it it is array, join with \n
				else if (value.isJsonArray()) {
					final JsonArray array = value.getAsJsonArray();

					final List<String> plainList = new ArrayList<>();
					final List<FancyComponent> componentList = new ArrayList<>();
					final List<String> legacyList = new ArrayList<>();

					for (final JsonElement element : array)
						if (element.isJsonPrimitive()) {
							String string = element.getAsString();

							// Need to do this now because components merge using \n and it wont work in sending them
							if (string.startsWith("<center>"))
								string = ChatUtil.center(string.substring(8).trim());

							final FancyComponent component = FancyComponent.fromMiniAmpersand(string);

							plainList.add(string);
							componentList.add(component);
							legacyList.add(component.toLegacySection(null));

						} else {
							ValidCore.checkBoolean(element != null && !element.isJsonNull(), "Missing element in array for lang key " + key + "! Make sure to remove ',' at the end of the list");

							CommonCore.warning("Invalid element in array for lang key " + key + ": " + element + ", only Strings and primitives are supported");
						}

					plainCache.put(key, String.join("\n", plainList));
					componentCache.put(key, FancyComponent.join(componentList));
					legacyCache.put(key, String.join("\n", legacyList));

				} else {
					ValidCore.checkBoolean(value != null && !value.isJsonNull(), "Missing element for lang key " + key + ", check for trailing commas");

					CommonCore.warning("Invalid element for lang key " + key + ": " + value + ", only Strings, primitives and arrays are supported");
				}
			}

			instance.plainCache = plainCache;
			instance.legacyCache = legacyCache;
			instance.componentCache = componentCache;
		}

		/*
		 * Helper method to turn the lines content into a single dump, parse to JSON and
		 * put the keys into the dictionary.
		 */
		private static void putToDictionary(final JsonObject dictionary, final List<String> content) {
			if (content != null && !content.isEmpty()) {
				final JsonObject json = CommonCore.GSON.fromJson(String.join("\n", content), JsonObject.class);

				for (final Map.Entry<String, JsonElement> entry : json.entrySet()) {
					final String key = entry.getKey();

					dictionary.add(key, json.get(key));
				}
			}
		}
	}
}
