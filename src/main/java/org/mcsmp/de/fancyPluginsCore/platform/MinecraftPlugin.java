package org.mcsmp.de.fancyPluginsCore.platform;

import org.mcsmp.de.fancyPluginsCore.annotation.AutoRegister;
import org.mcsmp.de.fancyPluginsCore.command.FancyCommandCore;
import org.mcsmp.de.fancyPluginsCore.command.FancyCommandGroup;
import org.mcsmp.de.fancyPluginsCore.command.subcommand.FancySubCommandCore;
import org.mcsmp.de.fancyPluginsCore.database.FancyDatabase;
import org.mcsmp.de.fancyPluginsCore.debug.Debugger;
import org.mcsmp.de.fancyPluginsCore.exception.HandledException;
import org.mcsmp.de.fancyPluginsCore.filter.Filter;
import org.mcsmp.de.fancyPluginsCore.library.Library;
import org.mcsmp.de.fancyPluginsCore.library.LibraryManager;
import org.mcsmp.de.fancyPluginsCore.model.BuiltByBitUpdateCheck;
import org.mcsmp.de.fancyPluginsCore.proxy.ProxyListener;
import org.mcsmp.de.fancyPluginsCore.proxy.message.OutgoingMessage;
import org.mcsmp.de.fancyPluginsCore.settings.FancySettings;
import org.mcsmp.de.fancyPluginsCore.utility.ChatUtil;
import org.mcsmp.de.fancyPluginsCore.utility.CommonCore;
import org.mcsmp.de.fancyPluginsCore.utility.ValidCore;

import java.io.File;

/**
 * Represents a plugin using Foundation
 */
public interface MinecraftPlugin {

	/**
	 * Disable this plugin.
	 */
	void disable();

	/**
	 * Handles various startup problems
	 *
	 * @param throwable
	 */
	default void displayError(Throwable throwable) {
		Debugger.printStackTrace(throwable);

		CommonCore.log(
				"&4    ___                  _ ",
				"&4   / _ \\  ___  _ __  ___| |",
				"&4  | | | |/ _ \\| '_ \\/ __| |",
				"&4  | |_| | (_) | |_) \\__ \\_|",
				"&4   \\___/ \\___/| .__/|___(_)",
				"&4             |_|          ",
				"&4!-----------------------------------------------------!",
				" &cError loading " + this.getName() + " v" + this.getVersion() + ", plugin is disabled!",
				" &cRunning on " + Platform.getPlatformName() + " " + Platform.getPlatformVersion() + " & Java " + System.getProperty("java.version"),
				"&4!-----------------------------------------------------!");

		if (throwable.getClass().toString().contains("org.bukkit.configuration.InvalidConfigurationException")) {
			CommonCore.log(" &cSeems like your config is not a valid YAML.");
			CommonCore.log(" &cUse online services like");
			CommonCore.log(" &chttp://yaml-online-parser.appspot.com/");
			CommonCore.log(" &cto check for syntax errors!");

		} else if (throwable instanceof UnsupportedOperationException || throwable.getCause() != null && throwable.getCause() instanceof UnsupportedOperationException) {
			CommonCore.log(" &cUnable to setup reflection!");
			CommonCore.log(" &cYour server is either too old or");
			CommonCore.log(" &cthe plugin broke on the new version :(");
		}

		if (throwable instanceof HandledException)
			throwable = ((HandledException) throwable).getHandle();

		while (throwable.getCause() != null)
			throwable = throwable.getCause();

		if (!(throwable instanceof HandledException)) {
			String error = "Unable to get the error message, search above.";
			if (throwable.getMessage() != null && !throwable.getMessage().isEmpty() && !throwable.getMessage().equals("null"))
				error = throwable.getMessage();

			CommonCore.log(" &cError: " + error);
		} else
			CommonCore.log(" &cError: See above for stack trace.");

		CommonCore.log("&4!-----------------------------------------------------!");

		this.disable();
	}

	/**
	 * Return authors of the plugin, comma separated.
	 *
	 * @return
	 */
	String getAuthors();

	/**
	 * Return the bStats plugin id, if not -1, we automatically start reporting
	 * your plugin to bStats.
	 *
	 * @return
	 */
	default int getBStatsPluginId() {
		return -1;
	}

	/**
	 * Used for plugin update notifications, return -1 if unset otherwise
	 * return your BuiltByBit.com plugin ID (get it from the URL of your Overview page)
	 *
	 * @return
	 */
	default int getBuiltByBitId() {
		return -1;
	}

	/**
	 * Override this from {@link #getBuiltByBitId()} to work.
	 * See <a href="https://builtbybit.com/account/api">https://builtbybit.com/account/api</a> for more information.
	 *
	 * @return
	 */
	default String getBuiltByBitSharedToken() {
		return null;
	}

	/**
	 * Return the data folder of this plugin.
	 *
	 * @return
	 */
	File getDataFolder();

	/**
	 * Return the default command group, or null if not set.
	 *
	 * @see ProxyListener
	 * @see AutoRegister
	 *
	 * @return
	 */
	FancyCommandGroup getDefaultCommandGroup();

	/**
	 * Return the default proxy listener, or null if not set.
	 *
	 * @see ProxyListener
	 * @see AutoRegister
	 *
	 * @return
	 */
	ProxyListener getDefaultProxyListener();

	/**
	 * Return the jar file of this plugin.
	 *
	 * @return
	 */
	File getFile();

	/**
	 * Return the founded year of this plugin.
	 *
	 * This is shown in {@link FancyCommandGroup} on help.
	 *
	 * @return
	 */
	default int getFoundedYear() {
		return -1;
	}

	/**
	 * Return the library manager for this plugin.
	 *
	 * @return
	 */
	LibraryManager getLibraryManager();

	/**
	 * Get the name of this plugin.
	 *
	 * @return
	 */
	String getName();

	/**
	 * Return the class loader of this plugin.
	 *
	 * @return
	 */
	ClassLoader getPluginClassLoader();

	/**
	 * Return the Sentry DSN for this plugin used for error reporting.
	 *
	 * @return
	 */
	default String getSentryDsn() {
		return null;
	}

	/**
	 * The start-up fancy logo
	 *
	 * @return null by default
	 */
	default String[] getStartupLogo() {
		return null;
	}

	/**
	 * Return the plugin's version.
	 *
	 * @return
	 */
	String getVersion();

	/**
	 * Called after the plugin is enabled
	 *
	 * @deprecated internal use only
	 */
	@Deprecated
	default void internalPostEnable() {

		// Move the legacy localization folder to unused
		{
			final File localizationFolder = new File(this.getDataFolder(), "localization");

			if (localizationFolder.exists()) {
				CommonCore.warning("The localization/ folder is now unused, run '/" + FancySettings.MAIN_COMMAND_ALIASES.get(0) + " dumplocale' to download the new locale format. Moving to unused/ ...");

				final File unusedFolder = new File(this.getDataFolder(), "unused");

				if (!unusedFolder.exists())
					unusedFolder.mkdirs();

				localizationFolder.renameTo(new File(unusedFolder, "localization"));
			}
		}

		ValidCore.checkNotEmpty(Filter.getFilters(), "Failed to load filters, this is a bug!");

		Platform.runTaskTimerAsync(20, FancyDatabase.RowQueueWriter.getInstance());
		Platform.runTaskTimerAsync(20, BossBarTask.getInstance());

		if (FancySettings.NOTIFY_NEW_VERSIONS)
			Platform.runTaskAsync(new BuiltByBitUpdateCheck());
	}

	/**
	 * Return true if the plugin is enabled.
	 *
	 * @return
	 */
	boolean isPluginEnabled();

	/**
	 * Return true if the plugin has not yet reached onPluginStart() method.
	 *
	 * @return
	 */
	boolean isInitializing();

	/**
	 * Should Pattern.CASE_INSENSITIVE be applied when compiling regular expressions in the Common class?
	 *
	 * May impose a slight performance penalty but increases catches.
	 *
	 * @see CommonCore#compilePattern(String)
	 *
	 * @return
	 */
	default boolean isRegexCaseInsensitive() {
		return true;
	}

	/**
	 * Should Pattern.UNICODE_CASE be applied when compiling regular expressions in the Common class?
	 *
	 * May impose a slight performance penalty but useful for non-English servers.
	 *
	 * @see CommonCore#compilePattern(String)
	 *
	 * @return
	 */
	default boolean isRegexUnicode() {
		return true;
	}

	/**
	 * Should we replace accents with their non accented friends when
	 * checking two strings for similarity in {@link ChatUtil}?
	 *
	 * @return defaults to true
	 */
	default boolean isSimilarityStrippingAccents() {
		return true;
	}

	/**
	 * Loads a library jar into the classloader classpath. If the library jar
	 * doesn't exist locally, it will be downloaded.
	 *
	 * If the provided library has any relocations, they will be applied to
	 * create a relocated jar and the relocated jar will be loaded instead.
	 *
	 * @param library
	 */
	default void loadLibrary(final Library library) {
		this.getLibraryManager().loadLibrary(library);
	}

	/**
	 * Loads a library jar into the classloader classpath. If the library jar
	 * doesn't exist locally, it will be downloaded.
	 *
	 * If the provided library has any relocations, they will be applied to
	 * create a relocated jar and the relocated jar will be loaded instead.
	 *
	 * @param groupId
	 * @param artifactId
	 * @param version
	 */
	default void loadLibrary(final String groupId, final String artifactId, final String version) {
		this.loadLibrary(Library
				.builder()
				.groupId(groupId)
				.artifactId(artifactId)
				.resolveTransitiveDependencies(true)
				.version(version)
				.build());
	}

	/**
	 * Convenience method for registering a command.
	 *
	 * @see FancyCommandCore#register()
	 *
	 * @param command
	 */
	default void registerCommand(final FancyCommandCore command) {
		ValidCore.checkBoolean(!this.isInitializing(), "Cannot register commands during plugin initialization! Use onPluginStart() instead.");

		command.register();
	}

	/**
	 * Shortcut for calling {@link FancyCommandGroup#register()}
	 *
	 * @param group
	 */
	default void registerCommands(final FancyCommandGroup group) {
		ValidCore.checkBoolean(!this.isInitializing(), "Cannot register commands during plugin initialization! Use onPluginStart() instead.");

		group.register();
	}

	/**
	 * Convenience method for quickly registering events for this plugin
	 *
	 * @param listener
	 */
	default void registerEvents(final Object listener) {
		ValidCore.checkBoolean(!this.isInitializing(), "Cannot register events during plugin initialization! Use onPluginStart() instead.");

		Platform.registerEvents(listener);
	}

	/**
	 * Reload this plugin's settings files.
	 */
	void reload();

	/**
	 * Set the default command group used in registering a {@link FancySubCommandCore} using {@link AutoRegister}
	 * annotation when no group is provided in its constructor.
	 *
	 * @param group
	 */
	void setDefaultCommandGroup(FancyCommandGroup group);

	/**
	 * Set the default proxy used in {@link OutgoingMessage} when no group is provided.
	 *
	 * @param listener
	 */
	void setDefaultProxyListener(ProxyListener listener);
}
