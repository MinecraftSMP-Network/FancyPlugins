package org.mcsmp.de.fancyPluginsCore.command.subcommand;

import lombok.Getter;
import org.mcsmp.de.fancyPluginsCore.command.FancyCommandCore;
import org.mcsmp.de.fancyPluginsCore.command.FancyCommandGroup;
import org.mcsmp.de.fancyPluginsCore.platform.MinecraftPlugin;
import org.mcsmp.de.fancyPluginsCore.platform.Platform;
import org.mcsmp.de.fancyPluginsCore.utility.ValidCore;

import java.util.Arrays;
import java.util.Map;

/**
 * A simple subcommand belonging to a {@link FancyCommandGroup}.
 */
@Getter
public abstract class FancySubCommandCore extends FancyCommandCore {

	/**
	 * All registered sublabels this subcommand can have
	 */
	private final String[] sublabels;

	/**
	 * Create a new subcommand for the {@link MinecraftPlugin#getDefaultCommandGroup()} group.
	 *
	 * @param sublabel
	 */
	protected FancySubCommandCore(final String sublabel) {
		this(getMainCommandGroup0(), sublabel);
	}

	/*
	 * Attempts to get the main command group, failing with an error if not defined
	 */
	private static FancyCommandGroup getMainCommandGroup0() {
		final FancyCommandGroup main = Platform.getPlugin().getDefaultCommandGroup();

		ValidCore.checkNotNull(main, Platform.getPlugin().getName() + " does not define a main command group!"
				+ " You need to put @AutoRegister over your class extending a SimpleCommandGroup that has a no args constructor to register it automatically");

		return main;
	}

	/**
	 * Creates a new subcommand belonging to a command group
	 *
	 * @param parent
	 * @param sublabel
	 */
	protected FancySubCommandCore(final FancyCommandGroup parent, final String sublabel) {
		super(parent.getLabel());

		final String[] split = sublabel.split("(\\||\\/)");
		ValidCore.checkBoolean(split.length > 0, "Please set at least 1 sublabel");

		this.sublabels = split;

		if (Platform.getPlugin().getDefaultCommandGroup() != null && Platform.getPlugin().getDefaultCommandGroup().getLabel().equals(this.getLabel()))
			this.setPermission(Platform.getPlugin().getName().toLowerCase() + ".command." + this.getSublabel()); // simply replace label with sublabel
		else
			this.setPermission(this.getPermission() + "." + this.getSublabel()); // append the sublabel at the end since this is not our main command
	}

	/**
	 * Shall we display the subcommand in the "/{label} help|?" menu?
	 *
	 * @see FancyCommandGroup#autoHandleHelp()
	 *
	 * @return
	 */
	public boolean showInHelp() {
		return true;
	}

	/**
	 * @see FancyCommandCore#preparePlaceholders()
	 *
	 * @return
	 */
	@Override
	public Map<String, Object> preparePlaceholders() {
		final Map<String, Object> map = super.preparePlaceholders();
		map.put("sublabel", this.getSublabel());

		return map;
	}

	/**
	 * Get the first sublabel of this subcommand
	 *
	 * @return
	 */
	public final String getSublabel() {
		return this.sublabels[0];
	}

	@Override
	public String toString() {
		return "SubCommand{parent=/" + this.getLabel() + ", label=" + this.getSublabel() + "}";
	}

	/**
	 * Return true if the given object is a {@link FancySubCommandCore} and has the same sublabel and sublabel aliases.
	 *
	 * @param obj
	 * @return
	 */
	@Override
	public final boolean equals(final Object obj) {
		if (obj instanceof FancySubCommandCore) {
			final FancySubCommandCore other = (FancySubCommandCore) obj;

			return Arrays.equals(other.getSublabels(), this.sublabels);
		}

		return false;
	}
}