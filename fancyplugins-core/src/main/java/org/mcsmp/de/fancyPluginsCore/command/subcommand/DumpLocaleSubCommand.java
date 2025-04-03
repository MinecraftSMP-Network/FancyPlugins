package org.mcsmp.de.fancyPluginsCore.command.subcommand;

import org.mcsmp.de.fancyPluginsCore.command.FancyCommandCore;
import org.mcsmp.de.fancyPluginsCore.command.FancyCommandGroup;
import org.mcsmp.de.fancyPluginsCore.platform.MinecraftPlugin;
import org.mcsmp.de.fancyPluginsCore.platform.Platform;
import org.mcsmp.de.fancyPluginsCore.settings.FancySettings;
import org.mcsmp.de.fancyPluginsCore.settings.Lang;

import java.io.File;
import java.util.List;

public final class DumpLocaleSubCommand extends FancySubCommandCore {

	/**
	 * Create a new sub-command with the "dumplocale" and "dumploc" aliases registered in your
	 * {@link MinecraftPlugin#getDefaultCommandGroup()} command group.
	 */
	public DumpLocaleSubCommand() {
		this("dumplocale|dumploc");
	}

	/**
	 * Create a new sub-command with the given label registered in your
	 * {@link MinecraftPlugin#getDefaultCommandGroup()} command group.
	 *
	 * @param label
	 */
	public DumpLocaleSubCommand(final String label) {
		super(label);

		this.setProperties();
	}

	/**
	 * Create a new sub-command with the "dumplocale" and "dumploc" aliases registered in the given command group.
	 *
	 * @param group
	 */
	public DumpLocaleSubCommand(final FancyCommandGroup group) {
		this(group, "dumplocale|dumploc");
	}

	/**
	 * Create a new sub-command with the given label registered in the given command group.
	 *
	 * @param group
	 * @param label
	 */
	public DumpLocaleSubCommand(final FancyCommandGroup group, final String label) {
		super(group, label);

		this.setProperties();
	}

	/*
	 * Set the properties for this command
	 */
	private void setProperties() {
		this.setMaxArguments(0);
		this.setDescription("Copy language file to lang/ folder so you can edit it. This uses 'Locale' key from settings.yml. Existing file will be updated with new keys and unused ones will be deleted.");
	}

	@Override
	protected void onCommand() {
		this.tellInfo("Dumping or updating " + FancySettings.LOCALE + " locale file...");

		final File dumped = Lang.Storage.createAndDumpToFile();
		final File rootFile = Platform.getPlugin().getDataFolder();

		this.tellSuccess("Locale file dumped to " + dumped.getAbsolutePath().replace(rootFile.getParentFile().getAbsolutePath(), "") + ". Existing keys were updated, see console for details.");
	}

	/**
	 * @see FancyCommandCore#tabComplete()
	 */
	@Override
	protected List<String> tabComplete() {
		return NO_COMPLETE;
	}
}
