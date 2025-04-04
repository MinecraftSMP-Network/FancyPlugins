package org.mcsmp.de.fancyPluginsCore.debug;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.mcsmp.de.fancyPluginsCore.exception.FoException;
import org.mcsmp.de.fancyPluginsCore.exception.HandledException;
import org.mcsmp.de.fancyPluginsCore.platform.MinecraftPlugin;
import org.mcsmp.de.fancyPluginsCore.platform.Platform;
import org.mcsmp.de.fancyPluginsCore.settings.FancySettings;
import org.mcsmp.de.fancyPluginsCore.utility.CommonCore;
import org.mcsmp.de.fancyPluginsCore.utility.FileUtil;
import org.mcsmp.de.fancyPluginsCore.utility.ReflectionUtil;

import java.util.*;
import java.util.function.Supplier;

/**
 * Utility class for solving problems and errors.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Debugger {

	/**
	 * Used to prevent duplicated reporting to sentry
	 */
	private static final Set<String> reportedExceptions = new HashSet<>();

	/**
	 * Tags to be added to the Sentry error reporting.
	 */
	private static final List<Supplier<Map<String, String>>> sentryTags = new ArrayList<>();

	/**
	 * Add a tag to the Sentry error reporting.
	 *
	 * @param tag
	 */
	public static void addSentryTag(Supplier<Map<String, String>> tag) {
		sentryTags.add(tag);
	}

	/**
	 * Logs a message to the console if the section name is within {@link FancySettings#DEBUG_SECTIONS}
	 * or if it is "*".
	 *
	 * Debug sections are by default stored in settings.yml under the "Debug" key.
	 *
	 * @param section
	 * @param messages
	 */
	public static void debug(final String section, final String... messages) {
		if (isDebugged(section))
			for (final String message : messages)
				log("[" + section + "] " + message);
	}

	/**
	 * Returns true if the section is within {@link FancySettings#DEBUG_SECTIONS} or if it is "*".
	 *
	 * Debug sections are by default stored in settings.yml under the "Debug" key.
	 *
	 * @param section
	 * @return
	 */
	public static boolean isDebugged(final String section) {
		return FancySettings.DEBUG_SECTIONS.contains(section) || FancySettings.DEBUG_SECTIONS.contains("*");
	}

	// ----------------------------------------------------------------------------------------------------
	// Saving errors to file
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Save an error and relevant information to an `error.log` file.
	 *
	 * <p>This method stores the error details, additional messages, system information,
	 * and the stack trace of the error for debugging purposes.</p>
	 *
	 * @param throwable The exception or error that occurred. The stack trace will be logged, and its causes will be chained.
	 * @param messages  Optional additional information that may help identify or explain the issue.
	 *
	 * <p><b>Example Usage:</b></p>
	 * <pre>{@code
	 * try {
	 *   // Some code that might throw an error
	 * } catch (Throwable t) {
	 *   saveError(t, "Something went wrong while executing this operation.");
	 * }
	 * }</pre>
	 *
	 * <p>The method also logs a message to the server console, informing the user to check the `error.log`.</p>
	 *
	 * <p>In case another error occurs while saving the log, it will attempt to log that error to the system.</p>
	 *
	 * <p>File is written to the server's base directory.</p>
	 */
	public static void saveError(Throwable throwable, final String... messages) {
		saveError(true, throwable, messages);
	}

	/**
	 * Save an error and relevant information to an `error.log` file.
	 *
	 * <p>This method stores the error details, additional messages, system information,
	 * and the stack trace of the error for debugging purposes.</p>
	 *
	 * @param sentry
	 * @param throwable The exception or error that occurred. The stack trace will be logged, and its causes will be chained.
	 * @param messages  Optional additional information that may help identify or explain the issue.
	 *
	 * <p><b>Example Usage:</b></p>
	 * <pre>{@code
	 * try {
	 *   // Some code that might throw an error
	 * } catch (Throwable t) {
	 *   saveError(t, "Something went wrong while executing this operation.");
	 * }
	 * }</pre>
	 *
	 * <p>The method also logs a message to the server console, informing the user to check the `error.log`.</p>
	 *
	 * <p>In case another error occurs while saving the log, it will attempt to log that error to the system.</p>
	 *
	 * <p>File is written to the server's base directory.</p>
	 */
	public static void saveError(boolean sentry, Throwable throwable, final String... messages) {

		if (!Platform.hasPlatform()) {
			System.out.println("Fatal error saving error, platform not set yet.");
			System.out.println("This is typically caused by a previous error, check console.");

			throwable.printStackTrace();

			return;
		}

		// Log to sentry if enabled.
		final MinecraftPlugin plugin = Platform.getPlugin();

		// Ignore PlugMan errors
		for (final StackTraceElement element : throwable.getStackTrace())
			if (element.getClassName().contains(".plugman.") || element.getClassName().contains(".plugmanx.")) {
				CommonCore.warning("Please do not use PlugMan to interact with " + Platform.getPlugin().getName() + " because it causes issues. Restart your server or use the inbuilt reload command instead.");

				return;
			}

		// Do not report errors from outdated plugin versions
		if (sentry && plugin.getSentryDsn() != null && FancySettings.SENTRY && !(throwable instanceof OutOfMemoryError)) {
			final Throwable finalThrowable = throwable;

			// Prevent duplicated reporting
			final StackTraceElement[] elements = finalThrowable.getStackTrace();
			final String key = Arrays.toString(elements);

			if (!reportedExceptions.contains(key) && elements.length > 0) {
				boolean hasSentry = false;

				if (!ReflectionUtil.isClassAvailable("io.sentry.Sentry"))
					try {
						plugin.loadLibrary("io.sentry", "sentry", "8.4.0");

						hasSentry = true;

					} catch (final Throwable t) {
						CommonCore.log("Failed to load error reporting library Sentry:.");
						t.printStackTrace();

						CommonCore.log("Saving error locally due to missing sentry:");
						saveErrorLocally(throwable, messages);
					}
				reportedExceptions.add(key);
			}
		}

		// Else, only log locally.
		else
			saveErrorLocally(throwable, messages);
	}

	private static void saveErrorLocally(Throwable throwable, final String... messages) {
		final String systemInfo = "Running " + Platform.getPlatformName() + " " + Platform.getPlatformVersion() + " and Java " + System.getProperty("java.version");

		try {
			final List<String> lines = new ArrayList<>();
			final String header = Platform.getPlugin().getName() + " " + Platform.getPlugin().getVersion() + " encountered " + throwable.getClass().getSimpleName();
			final Date date = new Date();

			// Write out header and server info
			fill(lines,
					"------------------------------------[ " + date + " ]-----------------------------------",
					header,
					systemInfo,
					"Plugins: " + CommonCore.join(Platform.getPlugins()),
					"----------------------------------------------------------------------------------------------");

			// Write additional data
			if (messages != null && !String.join("", messages).isEmpty()) {
				fill(lines, "\nMore Information: ");
				fill(lines, messages);
			}

			// Write the stack trace
			do {
				// Write the error header
				fill(lines, throwable == null ? "Unknown error" : throwable.getClass().getSimpleName() + " " + CommonCore.getOrDefault(throwable.getMessage(), "(Unknown cause)"));

				int count = 0;

				for (final StackTraceElement el : throwable.getStackTrace()) {
					count++;

					final String trace = el.toString();

					if (trace.contains("sun.reflect"))
						continue;

					if (count > 6 && trace.startsWith("net.minecraft.server"))
						break;

					fill(lines, "\t at " + el.toString());
				}
			} while ((throwable = throwable.getCause()) != null);

			fill(lines, "----------------------------------------------------------------------------------------------", System.lineSeparator());

			// Log to the console
			CommonCore.log(header + "! Please check your error.log and report this issue with the information in that file. " + systemInfo);

			// Finally, save the error file
			FileUtil.write("error.log", lines);

		} catch (final Throwable secondError) {

			// Use system in case CommonCore#log threw the error
			log(CommonCore.configLine());
			log("Got error when saving another error!");
			log("Original error that is not saved:");
			log(CommonCore.configLine());
			throwable.printStackTrace();

			log(CommonCore.configLine());
			log("New error:");
			log(CommonCore.configLine());
			secondError.printStackTrace();
			log(CommonCore.configLine());
		}
	}

	/*
	 * Fill the list with the messages.
	 */
	private static void fill(final List<String> list, final String... messages) {
		list.addAll(Arrays.asList(messages));
	}

	// ----------------------------------------------------------------------------------------------------
	// Utility methods
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Prints array values with their indexes on each line.
	 *
	 * @param values
	 */
	public static void printValues(final Object[] values) {
		if (values != null) {
			log(CommonCore.chatLine());
			log("Enumeration of " + values.length + "x" + values.getClass().getSimpleName().toLowerCase().replace("[]", ""));

			for (int i = 0; i < values.length; i++)
				log("&8[" + i + "] &7" + values[i]);
		} else
			log("Value are null");
	}

	/**
	 * Prints stack trace until we reach the native MC/Bukkit with a custom message.
	 *
	 * @param debugLogMessage purely informative message to wrap the thrown stack trace around
	 */
	public static void printStackTrace(final String debugLogMessage) {
		final StackTraceElement[] trace = new Exception().getStackTrace();

		log("!----------------------------------------------------------------------------------------------------------!");
		log(debugLogMessage);
		log("!----------------------------------------------------------------------------------------------------------!");

		for (int i = 1; i < trace.length; i++) {
			final String line = trace[i].toString();

			if (canPrint(line))
				log("\tat " + line);
		}

		log("--------------------------------------------------------------------------------------------------------end-");
	}

	/**
	 * Print the stack trace of a {@link Throwable} to the console.
	 *
	 * <p>This method logs the exception details and its causes in a structured way.</p>
	 *
	 * <p>If the exception has causes (i.e., it wraps other exceptions), those will also be logged.
	 * For custom {@link FoException}s, it skips printing the main exception if it's simply a wrapper
	 * to avoid console spam, and only logs the underlying cause.</p>
	 *
	 * @param throwable The exception or error to print. Its stack trace and the stack trace of any causes will be logged.
	 *
	 * <p><b>Example Usage:</b></p>
	 * <pre>{@code
	 * try {
	 *   // Some code that might throw an error
	 * } catch (Throwable t) {
	 *   printStackTrace(t);
	 * }
	 * }</pre>
	 *
	 * <p>The method first logs the message of the original throwable, then prints the stack trace elements.
	 * If there are additional causes, it logs and prints them as well.</p>
	 */
	public static void printStackTrace(@NonNull final Throwable throwable) {

		if (throwable instanceof HandledException)
			return;

		// Load all causes
		final List<Throwable> causes = new ArrayList<>();

		if (throwable.getCause() != null) {
			Throwable cause = throwable.getCause();

			do
				causes.add(cause);
			while ((cause = cause.getCause()) != null);
		}

		if (throwable instanceof FoException && !causes.isEmpty())
			// Do not print parent exception if we are only wrapping it, saves console spam
			log(throwable.getMessage());

		else {
			log(throwable.toString());

			printStackTraceElements(throwable);
		}

		if (!causes.isEmpty()) {
			final Throwable lastCause = causes.get(causes.size() - 1);

			log(lastCause.toString());
			printStackTraceElements(lastCause);
		}
	}

	/*
	 * Print the stack trace elements of the throwable.
	 */
	private static void printStackTraceElements(final Throwable throwable) {
		for (final StackTraceElement element : throwable.getStackTrace()) {
			final String line = element.toString();

			if (canPrint(line))
				log("\tat " + line);
		}
	}

	/**
	 * Returns whether a line is suitable for printing as an error line.
	 * We ignore stuff from NMS and other spam as this is not needed.
	 *
	 * @param stackTraceLine
	 * @return
	 */
	private static boolean canPrint(final String stackTraceLine) {
		return !stackTraceLine.startsWith("net.minecraft") &&
				!stackTraceLine.startsWith("org.bukkit.") &&
				!stackTraceLine.startsWith("org.github.paperspigot.") &&
				!stackTraceLine.startsWith("java.") &&
				!stackTraceLine.startsWith("javax.script") &&
				!stackTraceLine.startsWith("nashorn") &&
				!stackTraceLine.startsWith("org.yaml.snakeyaml") &&
				!stackTraceLine.startsWith("sun.reflect") &&
				!stackTraceLine.startsWith("sun.misc");

		//!stackTraceLine.contains("org.bukkit.craftbukkit") &&
		//!stackTraceLine.contains("java.lang.Thread.run") &&
		//!stackTraceLine.contains("java.util.concurrent.ThreadPoolExecutor");
	}

	/*
	 * Helper method to log the message.
	 */
	private static void log(final String message) {
		Platform.log(message);
	}
}
