package org.mcsmp.de.fancyPluginsCore.command.subcommand;

import org.mcsmp.de.fancyPluginsCore.command.FancyCommandCore;
import org.mcsmp.de.fancyPluginsCore.platform.Platform;
import org.mcsmp.de.fancyPluginsCore.settings.Lang;
import org.mcsmp.de.fancyPluginsCore.settings.YamlConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple predefined sub-command for quickly reloading the plugin
 * using /{label} reload|rl
 */
public final class ReloadCommand extends FancyCommandCore {

	/**
	 * Create a new sub-command with the given label registered in the given command group.
	 *
	 * @param label
	 */
	public ReloadCommand(final String label) {
		this(label, null);
	}

	/**
	 * Create a new sub-command with the given label registered in the given command group.
	 *
	 * @param label
	 * @param permission
	 */
	public ReloadCommand(final String label, final String permission) {
		super(label);

		if (permission != null)
			this.setPermission(permission);

		this.setMaxArguments(0);
		this.setDescription(Lang.component("command-reload-description"));
	}

	@Override
	protected void onCommand() {
		handleCommand(this);
	}

	static void handleCommand(final FancyCommandCore command) {
		try {
			command.tellInfo(Lang.component("command-reload-started"));
			final List<String> erroredFiles = new ArrayList<>();

			final List<File> yamlFiles = new ArrayList<>();

			collectYamlFiles(Platform.getPlugin().getDataFolder(), yamlFiles);

			for (final File file : yamlFiles)
				try {
					YamlConfig.fromFile(file);

				} catch (final Throwable t) {
					t.printStackTrace();

					erroredFiles.add(file.getName());
				}

			if (!erroredFiles.isEmpty()) {
				command.tellError(Lang.component("command-reload-file-load-error", "files", String.join(", ", erroredFiles)));

				return;
			}

			Platform.getPlugin().reload();

			command.tellSuccess(Lang.component("command-reload-success"));

		} catch (final Throwable t) {
			command.tellError(Lang.component("command-reload-fail", "error", t.getMessage() != null ? t.getMessage() : "unknown"));

			t.printStackTrace();
		}
	}

	/*
	 * Get a list of all files ending with "yml" in the given directory
	 * and its subdirectories
	 */
	static List<File> collectYamlFiles(final File directory, final List<File> list) {
		if (directory.exists())
			for (final File file : directory.listFiles()) {
				if (file.getName().endsWith("yml"))
					list.add(file);

				if (file.isDirectory())
					collectYamlFiles(file, list);
			}

		return list;
	}

	/**
	 * @see FancyCommandCore#tabComplete()
	 */
	@Override
	protected List<String> tabComplete() {
		return NO_COMPLETE;
	}
}
