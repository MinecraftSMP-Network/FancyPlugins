package org.mcsmp.de.fancyPluginsCore.utility;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.mcsmp.de.fancyPluginsCore.exception.FoException;
import org.mcsmp.de.fancyPluginsCore.model.CompChatColor;
import org.mcsmp.de.fancyPluginsCore.platform.Platform;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.ClosedByInterruptException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for managing files.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileUtil {

	/**
	 * Extract the file name (without extension) from the given file.
	 *
	 * @see #getFileName(String)
	 *
	 * @param file
	 * @return
	 */
	public static String getFileName(final File file) {
		return getFileName(file.getName());
	}

	/**
	 * Extract the file name (without extension) from a given path.
	 *
	 * <p>This method takes a full file path and returns only the file name, excluding the extension and any directory paths.
	 *
	 * <ul>
	 * <li> For example, calling {@code getFileName("/folder/example.txt")} will return "example".
	 * </ul>
	 *
	 * @param path the full file path to extract the name from
	 * @return the file name without its extension
	 * @throws IllegalArgumentException if the path is null or empty
	 */
	public static String getFileName(String path) {
		ValidCore.checkBoolean(path != null && !path.isEmpty(), "The given path must not be empty!");

		int pos = path.lastIndexOf("/");

		if (pos > 0)
			path = path.substring(pos + 1);

		pos = path.lastIndexOf(".");

		if (pos > 0)
			path = path.substring(0, pos);

		return path;
	}

	// ----------------------------------------------------------------------------------------------------
	// Getting files
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Create a new file in our plugin folder, supporting sub-directories paths.
	 *
	 * Example: logs/admin/console.log or worlds/nether.yml will make the appropriate file and directories.
	 *
	 * @param path
	 * @return the file itself
	 */
	public static File createIfNotExists(final String path) {
		final File datafolder = Platform.getPlugin().getDataFolder();
		final int lastIndex = path.lastIndexOf('/');
		final File directory = new File(datafolder, path.substring(0, lastIndex >= 0 ? lastIndex : 0));

		directory.mkdirs();

		final File destination = new File(datafolder, path);

		if (!destination.exists())
			try {
				destination.createNewFile();

			} catch (final Throwable t) {
				CommonCore.throwError(t, "Could not create new file '" + destination + "' due to " + t);
			}

		return destination;
	}

	/**
	 * Return a file in a path in our plugin folder, file may or may not exist.
	 *
	 * @param path
	 * @return
	 */
	public static File getFile(final String path) {
		return new File(Platform.getPlugin().getDataFolder(), path);
	}

	/**
	 * Return all files in our plugin directory within a given path, ending with the given extension.
	 *
	 * @param directory inside your plugin's folder
	 * @param extension where dot is placed automatically in case it is lacking
	 * @return
	 */
	public static File[] getFiles(@NonNull final String directory, @NonNull String extension) {

		// Remove initial dot, if any
		if (extension.charAt(0) == '.')
			extension = extension.substring(1);

		final File dataFolder = new File(Platform.getPlugin().getDataFolder(), directory);

		if (!dataFolder.exists())
			dataFolder.mkdirs();

		final String finalExtension = extension;

		return dataFolder.listFiles((FileFilter) file -> !file.isDirectory() && file.getName().endsWith("." + finalExtension));
	}

	/**
	 * Return all files in the given directory, ending with the given extension.
	 *
	 * @param directory the directory to search in
	 * @param extension where dot is placed automatically in case it is lacking
	 * @return
	 */
	public static File[] getFiles(@NonNull final File directory, @NonNull String extension) {

		// Remove initial dot, if any
		if (extension.charAt(0) == '.')
			extension = extension.substring(1);

		final String finalExtension = extension;
		return directory.listFiles((FileFilter) file -> !file.isDirectory() && file.getName().endsWith("." + finalExtension));
	}

	// ----------------------------------------------------------------------------------------------------
	// Reading
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Read all lines of text from a URL and return them as a list of strings.
	 *
	 * This method makes an HTTP request to the specified URL and reads the response line by line.
	 * It appends a query parameter with a timestamp token to avoid caching.
	 *
	 * <ul>
	 * <li> For example, calling {@code readLinesFromUrl("http://example.com")} will return the lines from the page's response.
	 * </ul>
	 *
	 * By default the user agent pretents to be Chrome, you can set or remove it in {@link NetworkUtil#setRemoteUserAgent(String)}.
	 *
	 * @see NetworkUtil#setRemoteUserAgent(String)
	 *
	 * @param url the URL to connect and read lines from
	 * @return a list of strings, where each string is a line from the URL response
	 * @throws IOException if an I/O issue occurs while connecting or reading from the URL
	 * @throws IllegalArgumentException if the URL is malformed
	 */
	public static List<String> readLinesFromUrl(final String url) throws IOException {
		final List<String> lines = new ArrayList<>();
		final URLConnection connection = new URL(url + "?token=" + System.currentTimeMillis()).openConnection();

		if (NetworkUtil.getRemoteUserAgent() != null)
			connection.setRequestProperty("User-Agent", NetworkUtil.getRemoteUserAgent());

		connection.setConnectTimeout(5000);
		connection.setReadTimeout(5000);
		connection.setDoOutput(true);

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			String inputLine;

			while ((inputLine = reader.readLine()) != null)
				lines.add(inputLine);
		}

		return lines;
	}

	/**
	 * Return all lines from file in a path in our plugin folder or null if the file does not exist.
	 *
	 * @param fileName
	 * @return
	 */
	public static List<String> readLinesFromFile(final String fileName) {
		return readLinesFromFile(getFile(fileName));
	}

	/**
	 * Return all lines in the file, returning null if the file does not exists.
	 *
	 * @param file
	 * @return
	 */
	public static List<String> readLinesFromFile(@NonNull final File file) {
		if (!file.exists())
			return null;

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			final List<String> lines = new ArrayList<>();
			String line;

			while ((line = br.readLine()) != null)
				lines.add(line);

			return lines;

		} catch (final IOException ee) {
			throw new FoException(ee, "Could not read lines from " + file.getName());
		}
	}

	/**
	 * Read all lines of text from a file located within the internal jar resources and return them as a list of strings.
	 *
	 * <p>This method searches for the specified internal path within the running jar file, opens the file, and reads its contents line by line.
	 * It works for resources packaged within the plugin jar. The method ensures the file is read using UTF-8 encoding.
	 *
	 * <ul>
	 * <li> For example, calling {@code readLinesFromInternalPath("config.yml")} will return the lines from the internal `config.yml` file.
	 * </ul>
	 *
	 * @param path the internal path to the file within the jar (e.g., "config.yml" or "lang/en_US.json")
	 * @return a list of strings, where each string is a line from the file, or {@code null} if the file is not found or an error occurs
	 */
	public static List<String> readLinesFromInternalPath(@NonNull final String path) {
		return readLinesFromInternalPath(Platform.getPlugin().getFile(), path);
	}

	/**
	 * Read all lines of text from a file located within the internal jar resources and return them as a list of strings.
	 *
	 * <p>This method searches for the specified internal path within the running jar file, opens the file, and reads its contents line by line.
	 * It works for resources packaged within the plugin jar. The method ensures the file is read using UTF-8 encoding.
	 *
	 * <ul>
	 * <li> For example, calling {@code readLinesFromInternalPath("config.yml")} will return the lines from the internal `config.yml` file.
	 * </ul>
	 *
	 * @param pluginFile the plugin file to read the internal path from
	 * @param path the internal path to the file within the jar (e.g., "config.yml" or "lang/en_US.json")
	 * @return a list of strings, where each string is a line from the file, or {@code null} if the file is not found or an error occurs
	 */
	public static List<String> readLinesFromInternalPath(@NonNull final File pluginFile, @NonNull final String path) {
		try (JarFile jarFile = new JarFile(pluginFile)) {

			for (final Enumeration<JarEntry> it = jarFile.entries(); it.hasMoreElements();) {
				final JarEntry entry = it.nextElement();

				if (entry.toString().equals(path)) {
					final InputStream is = jarFile.getInputStream(entry);
					final BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
					final List<String> lines = reader.lines().collect(Collectors.toList());

					reader.close();
					return lines;
				}
			}

		} catch (final Throwable ex) {
			ex.printStackTrace();
		}

		return null;
	}

	// ----------------------------------------------------------------------------------------------------
	// Writing
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Write a line to file prefixed with a formatted date.
	 *
	 * @see #writeFormatted(String, String, String)
	 *
	 * @param to
	 * @param message
	 */
	public static void writeFormatted(final String to, final String message) {
		writeFormatted(to, null, message);
	}

	/**
	 * Write a formatted message to a specified file, prefixing each line with a timestamp and an optional prefix.
	 *
	 * <p>This method removes any color codes from the message, splits it into lines, and formats each line with a timestamp,
	 * and an optional prefix before writing it to the file. The message is not written if it equals "none" or is empty.
	 *
	 * <ul>
	 * <li> For example, calling {@code writeFormatted("log.txt", "INFO", "Hello World")} will write each line with the format: `[timestamp] INFO: Hello World`.
	 * </ul>
	 *
	 * @param to the file path to write to
	 * @param prefix an optional prefix to add before each message line (e.g., "INFO"), or {@code null} for no prefix
	 * @param message the message to be written, which can contain multiple lines
	 */
	public static void writeFormatted(final String to, final String prefix, String message) {
		message = CompChatColor.stripColorCodes(message);

		if (!message.equalsIgnoreCase("none") && !message.isEmpty())
			for (final String line : message.split("\n"))
				if (!line.isEmpty())
					write(to, "[" + TimeUtil.getFormattedDate() + "] " + (prefix != null ? prefix + ": " : "") + line);
	}

	/**
	 * Write lines to a file path in our plugin directory,
	 * creating the file if it does not exist, appending lines at the end.
	 *
	 * @param to
	 * @param lines
	 */
	public static void write(final String to, final String... lines) {
		write(to, Arrays.asList(lines));
	}

	/**
	 * Write lines to a file that exists, appending lines at the end.
	 *
	 * @param to
	 * @param lines
	 */
	public static void write(final File to, final String... lines) {
		write(to, Arrays.asList(lines));
	}

	/**
	 * Write lines to a file path in our plugin directory,
	 * creating the file if it does not exist, appending lines at the end.
	 *
	 * @param to
	 * @param lines
	 */
	public static void write(final String to, final Collection<String> lines) {
		write(createIfNotExists(to), lines, StandardOpenOption.APPEND);
	}

	/**
	 * Write the given lines to a file using the specified options.
	 *
	 * @param to
	 * @param lines
	 * @param options
	 */
	public static void write(final File to, final Collection<String> lines, final StandardOpenOption... options) {
		ValidCore.checkBoolean(to.exists(), "Cannot write to non-existing file: " + to);

		try {
			final Path path = Paths.get(to.toURI());

			Files.write(path, lines, StandardCharsets.UTF_8, options);

		} catch (final ClosedByInterruptException ex) {
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(to, true))) {
				for (final String line : lines)
					writer.append(System.lineSeparator() + line);

			} catch (final IOException exception) {
				exception.printStackTrace();
			}

		} catch (final Exception ex) {

			// do not throw our exception since it would cause an infinite loop if there is a problem due to error writing
			if (ex instanceof IOException && "There is not enough space on the disk".equals(ex.getMessage()))
				ex.printStackTrace();
			else
				throw new FoException(ex, "Failed to write to " + to, false);
		}
	}

	// ----------------------------------------------------------------------------------------------------
	// Extracting from our plugin .jar file
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Copy file from your plugin jar to the given destination.
	 * No action is done if the file already exists.
	 *
	 * @param path the path to the file inside the plugin
	 * @return the extracted file
	 */
	public static File extract(final String path) {
		return extract(path, path);
	}

	/**
	 * Copy file from your plugin jar to the given destination.
	 * For example: extract("class.yml", "classes/Archer.yml")
	 *
	 * Variables such as {plugin_name} and {file} in the file are replaced automatically.
	 *
	 * @param from     the path to the file inside the plugin
	 * @param to       the path where the file will be copyed inside the plugin
	 *                 folder
	 * @return the extracted file
	 */
	public static File extract(final String from, final String to) {
		final List<String> lines = readLinesFromInternalPath(from);
		ValidCore.checkNotNull(lines, "Inbuilt " + from + " not found! Did you reload?");

		return extract(lines, to);
	}

	/**
	 * Copy the given lines to the given destination.
	 *
	 * Variables such as {plugin_name} and {file} in the file are replaced automatically.
	 *
	 * @param lines
	 * @param to
	 * @return the extracted file
	 */
	public static File extract(final List<String> lines, final String to) {
		File file = new File(Platform.getPlugin().getDataFolder(), to);

		if (file.exists())
			return file;

		file = createIfNotExists(to);

		try {
			final String fileName = getFileName(file);

			// Replace variables in lines
			for (int i = 0; i < lines.size(); i++)
				lines.set(i, replaceVariables(lines.get(i), fileName));

			Files.write(file.toPath(), lines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

		} catch (final IOException ex) {
			CommonCore.error(ex,
					"Failed to copy lines to file " + to,
					"Error: {error}");
		}

		return file;
	}

	/**
	 * Similar to {@link #extract(String, String)} but intended
	 * for non-text file types such as images etc. No variables are replaced.
	 *
	 * @param path
	 * @return
	 */
	public static File extractRaw(final String path) {
		File file = new File(Platform.getPlugin().getDataFolder(), path);

		try (JarFile jarFile = new JarFile(Platform.getPlugin().getFile())) {

			for (final Enumeration<JarEntry> it = jarFile.entries(); it.hasMoreElements();) {
				final JarEntry entry = it.nextElement();

				if (entry.toString().equals(path)) {
					final InputStream is = jarFile.getInputStream(entry);

					if (file.exists())
						return file;

					file = createIfNotExists(path);

					try {
						Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);

					} catch (final IOException ex) {
						CommonCore.error(ex,
								"Failed to extract " + path,
								"Error: {error}");
					}

					return file;

				}
			}

		} catch (final Throwable ex) {
			ex.printStackTrace();
		}

		throw new FoException("Inbuilt file not found: " + path);
	}

	/*
	 * A helper method to replace variables in files we are extracting.
	 *
	 * Saves us time so that we can distribute the same file across multiple
	 * plugins each having its own unique plugin name and file name.
	 */
	private static String replaceVariables(final String line, final String fileName) {
		return line
				.replace("{plugin_name}", Platform.getPlugin().getName())
				.replace("{plugin_name_lower}", Platform.getPlugin().getName().toLowerCase())
				.replace("{file}", fileName)
				.replace("{file_lowercase}", fileName);
	}

	/**
	 * Extract the folder and all of its content from the JAR file to
	 * the given path in your plugin folder.
	 *
	 * @param folder      the source folder in your JAR plugin file
	 * @param destination the destination folder name in your plugin folder
	 */
	public static void extractFolderFromJar(final String folder, final String destination) {
		ValidCore.checkBoolean(folder.charAt(0) != '/', "Folder must not start with '/'! Given: " + folder);
		ValidCore.checkBoolean(folder.charAt(folder.length() - 1) == '/', "Folder must end with '/'! Given: " + folder);

		if (getFile(folder).exists())
			return;

		try (JarFile jarFile = new JarFile(Platform.getPlugin().getFile())) {
			for (final Enumeration<JarEntry> it = jarFile.entries(); it.hasMoreElements();) {
				final JarEntry jarEntry = it.nextElement();
				final String entryName = jarEntry.getName();

				// Copy each individual file manually
				if (entryName.startsWith(folder) && !entryName.equals(folder))
					extract(entryName);
			}

		} catch (final Throwable t) {
			CommonCore.throwError(t, "Failed to copy folder " + folder + " to " + destination);
		}
	}

	/**
	 * Remove the given file with all subfolders.
	 *
	 * @param file
	 */
	public static void deleteRecursivelly(final File file) {
		if (file.isDirectory())
			for (final File subfolder : file.listFiles())
				deleteRecursivelly(subfolder);

		if (file.exists())
			ValidCore.checkBoolean(file.delete(), "Failed to delete file: " + file);
	}

	/**
	 * Create a ZIP archive from the given source directory (inside our plugin folder)
	 * to the given full path (in our plugin folder).
	 *
	 * Please do not specify any extension, just the dir and the file name.
	 *
	 * @param sourceDirectory
	 * @param to
	 * @throws IOException
	 */
	public static void zip(final String sourceDirectory, final String to) throws IOException {
		final File parent = Platform.getPlugin().getDataFolder().getParentFile().getParentFile();
		final File toFile = new File(parent, to + ".zip");

		if (toFile.exists())
			ValidCore.checkBoolean(toFile.delete(), "Failed to delete old file " + toFile);

		final Path pathTo = Files.createFile(Paths.get(toFile.toURI()));

		try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(pathTo))) {
			final Path pathFrom = Paths.get(new File(parent, sourceDirectory).toURI());

			Files.walk(pathFrom).filter(path -> !Files.isDirectory(path)).forEach(path -> {
				final ZipEntry zipEntry = new ZipEntry(pathFrom.relativize(path).toString());

				try {
					zs.putNextEntry(zipEntry);

					Files.copy(path, zs);
					zs.closeEntry();
				} catch (final IOException ex) {
					ex.printStackTrace();
				}
			});
		}
	}
}
