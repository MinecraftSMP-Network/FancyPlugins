package org.mcsmp.de.fancyPluginsCore.command.subcommand;

import org.mcsmp.de.fancyPluginsCore.command.FancyCommandCore;
import org.mcsmp.de.fancyPluginsCore.command.FancyCommandGroup;
import org.mcsmp.de.fancyPluginsCore.platform.MinecraftPlugin;
import org.mcsmp.de.fancyPluginsCore.settings.Lang;

import java.util.List;

/**
 * A simple predefined sub-command for quickly reloading the plugin
 * using /{label} reload|rl
 */
public final class ReloadSubCommand extends FancySubCommandCore {

	/**
	 * Create a new sub-command with the "reload" and "rl" aliases registered in your
	 * {@link MinecraftPlugin#getDefaultCommandGroup()} command group.
	 */
	public ReloadSubCommand() {
		this("reload|rl");
	}

	/**
	 * Create a new sub-command with the given label registered in your
	 * {@link MinecraftPlugin#getDefaultCommandGroup()} command group.
	 *
	 * @param label
	 */
	public ReloadSubCommand(final String label) {
		super(label);

		this.setProperties();
	}

	/**
	 * Create a new sub-command with the "reload" and "rl" aliases registered in the given command group.
	 *
	 * @param group
	 */
	public ReloadSubCommand(final FancyCommandGroup group) {
		this(group, "reload|rl");
	}

	/**
	 * Create a new sub-command with the given label registered in the given command group.
	 *
	 * @param group
	 * @param label
	 */
	public ReloadSubCommand(final FancyCommandGroup group, final String label) {
		super(group, label);

		this.setProperties();
	}

	/*
	 * Set the properties for this command
	 */
	private void setProperties() {
		this.setMaxArguments(0);
		this.setDescription(Lang.component("command-reload-description"));
	}

	@Override
	protected void onCommand() {
		ReloadCommand.handleCommand(this);
	}

	/**
	 * @see FancyCommandCore#tabComplete()
	 */
	@Override
	protected List<String> tabComplete() {
		return NO_COMPLETE;
	}
}