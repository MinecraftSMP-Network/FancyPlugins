package org.mcsmp.de.fancyPluginsCore.command.subcommand;

import lombok.Setter;
import org.mcsmp.de.fancyPluginsCore.command.FancyCommandCore;
import org.mcsmp.de.fancyPluginsCore.command.FancyCommandGroup;
import org.mcsmp.de.fancyPluginsCore.platform.MinecraftPlugin;
import org.mcsmp.de.fancyPluginsCore.platform.Platform;
import org.mcsmp.de.fancyPluginsCore.settings.Lang;
import org.mcsmp.de.fancyPluginsCore.settings.YamlConfig;
import org.mcsmp.de.fancyPluginsCore.utility.CommonCore;
import org.mcsmp.de.fancyPluginsCore.utility.FileUtil;
import org.mcsmp.de.fancyPluginsCore.utility.TimeUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * A sample sub-command that you can automatically add
 * to your main command group to help collect debugging information
 * users can submit to you when they have problems.
 */
public final class DebugSubCommand extends FancySubCommandCore {

	/**
	 * Set the custom debug lines you would like to add to the debug file
	 */
	@Setter
	private static List<String> debugLines = new ArrayList<>();

	/**
	 * Create a new sub-command with the "debug" label registered in your
	 * {@link MinecraftPlugin#getDefaultCommandGroup()} command group.
	 */
	public DebugSubCommand() {
		this("debug");
	}

	/**
	 * Create a new sub-command with the given label registered in your
	 * {@link MinecraftPlugin#getDefaultCommandGroup()} command group.
	 *
	 * @param label
	 */
	public DebugSubCommand(final String label) {
		super(label);

		this.setProperties();
	}

	/**
	 * Create a new sub-command with the "debug" label registered in the given command group.
	 *
	 * @param group
	 */
	public DebugSubCommand(final FancyCommandGroup group) {
		this(group, "debug");
	}

	/**
	 * Create a new sub-command with the given label registered in the given command group.
	 *
	 * @param group
	 * @param label
	 */
	public DebugSubCommand(final FancyCommandGroup group, final String label) {
		super(group, label);

		this.setProperties();
	}

	/*
	 * Set the properties for this command
	 */
	private void setProperties() {
		this.setMaxArguments(0);
		this.setDescription("ZIP your settings for reporting bugs.");
	}

	@Override
	protected void onCommand() {
		this.tellInfo(Lang.component("command-debug-preparing"));

		final File debugFolder = FileUtil.getFile("debug");
		final List<File> files = this.listFilesRecursively(Platform.getPlugin().getDataFolder(), new ArrayList<>());

		// Clean up the old folder if exists
		FileUtil.deleteRecursivelly(debugFolder);

		// Collect general debug information first
		this.writeDebugInformation();

		// Copy all plugin files
		this.copyFilesToDebug(files);

		// Zip the folder
		this.zipAndRemoveFolder(debugFolder);

		this.tellSuccess(Lang.component("command-debug-success", "amount", String.valueOf(files.size())));
	}

	/*
	 * Write our own debug information
	 */
	private void writeDebugInformation() {
		final List<String> lines = CommonCore.toList(
				"Date: " + TimeUtil.getFormattedDate(),
				"Plugin: " + Platform.getPlugin().getName() + " " + Platform.getPlugin().getVersion(),
				"Server: " + Platform.getPlatformName() + " " + Platform.getPlatformVersion(),
				"Java: " + System.getProperty("java.version") + " (" + System.getProperty("java.specification.vendor") + "/" + System.getProperty("java.vm.vendor") + ")",
				"OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"),
				"Players Online: " + Platform.getOnlinePlayers().size(),
				"Plugins: " + CommonCore.join(Platform.getPlugins(), tuple -> tuple.getKey() + " " + tuple.getValue()));

		lines.addAll(debugLines);

		FileUtil.write("debug/general.txt", lines);
	}

	/*
	 * Copy the given files into debug/ folder
	 */
	private void copyFilesToDebug(final List<File> files) {
		for (final File file : files)
			try {
				// Get the path in our folder
				final String path = file.getPath().replace("\\", "/").replace("plugins/" + Platform.getPlugin().getName(), "");

				// Create a copy file
				final File copy = FileUtil.createIfNotExists("debug/" + path);

				// Strip sensitive keys from .YML files
				if (file.getName().endsWith(".yml")) {
					final YamlConfig config = YamlConfig.fromFile(file);
					final YamlConfig copyConfig = YamlConfig.fromFile(copy);

					for (final String key : config.getKeys(true)) {
						final String lowercasedKey = key.toLowerCase();
						final Object value = config.getObject(key);

						if (!lowercasedKey.contains("mysql") && !lowercasedKey.contains("database") && !lowercasedKey.contains("password"))
							copyConfig.set(key, value);
					}

					copyConfig.save();
				}

				else
					Files.copy(file.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);

			} catch (final Exception ex) {
				ex.printStackTrace();

				this.returnTell(Lang.component("command-debug-copy-fail", "file", file.getName()));
			}
	}

	/*
	 * Zips the given folder and removes it afterwards
	 */
	private void zipAndRemoveFolder(final File folder) {
		try {
			final String path = folder.getPath();

			FileUtil.zip(path, path);
			FileUtil.deleteRecursivelly(folder);

		} catch (final IOException ex) {
			ex.printStackTrace();

			this.returnTell(Lang.component("command-debug-zip-fail"));
		}
	}

	/*
	 * Load the list of files available to ZIP
	 */
	private List<File> listFilesRecursively(final File folder, final List<File> files) {
		for (final File file : folder.listFiles())
			if (file.isDirectory()) {
				// Ignore log directory and ignore the debug directory itself
				if (!file.getName().equals("logs") && !file.getName().equals("debug"))
					this.listFilesRecursively(file, files);

			} else // Ignore the debug zip file itself
				if (!file.getName().equals("debug.zip") && !file.getName().equals("mysql.yml"))
					files.add(file);

		return files;
	}

	/**
	 * @see FancyCommandCore#tabComplete()
	 */
	@Override
	protected List<String> tabComplete() {
		return NO_COMPLETE;
	}

	/**
	 * Add custom debug lines to the general.txt file in the compressed ZIP file.
	 *
	 * @param lines
	 */
	public static void addDebugLines(final String... lines) {
		for (final String line : lines)
			debugLines.add(line);
	}
}
