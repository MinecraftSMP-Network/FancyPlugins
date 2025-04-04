package org.mcsmp.de.fancyPluginsCore.command.subcommand;

import lombok.NonNull;
import org.mcsmp.de.fancyPluginsCore.command.FancyCommandCore;
import org.mcsmp.de.fancyPluginsCore.command.FancyCommandGroup;
import org.mcsmp.de.fancyPluginsCore.command.annotation.Permission;
import org.mcsmp.de.fancyPluginsCore.command.annotation.PermissionGroup;
import org.mcsmp.de.fancyPluginsCore.exception.FoException;
import org.mcsmp.de.fancyPluginsCore.model.ChatPaginator;
import org.mcsmp.de.fancyPluginsCore.model.FancyComponent;
import org.mcsmp.de.fancyPluginsCore.model.Variables;
import org.mcsmp.de.fancyPluginsCore.platform.MinecraftPlugin;
import org.mcsmp.de.fancyPluginsCore.platform.Platform;
import org.mcsmp.de.fancyPluginsCore.settings.Lang;
import org.mcsmp.de.fancyPluginsCore.utility.ValidCore;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple predefined command for quickly listing all permissions
 * the plugin uses, given they are stored in a class.
 */
public final class PermsSubCommand extends FancySubCommandCore {

	/**
	 * Classes with permissions listed as fields
	 */
	private final Class<?> classToList;

	/**
	 * Create a new subcommand using the {@link MinecraftPlugin#getDefaultCommandGroup()} command group and
	 * "permissions" and "perms" aliases.
	 *
	 * The class must have the {@link PermissionGroup} annotation over subclasses and {@link Permission} annotations
	 * over each "public static final String" field.
	 *
	 * @param classToList
	 */
	public PermsSubCommand(final Class<?> classToList) {
		this("permissions|perms", classToList);
	}

	/**
	 * Create a new subcommand using the {@link MinecraftPlugin#getDefaultCommandGroup()} command group and label.
	 * The class must have the {@link PermissionGroup} annotation over subclasses and {@link Permission} annotations
	 * over each "public static final String" field.
	 *
	 * @param label
	 * @param classToList
	 */
	private PermsSubCommand(final String label, @NonNull final Class<?> classToList) {
		super(label);

		this.classToList = classToList;

		this.setProperties();
	}

	/**
	 * Create a new subcommand using the given command group and "permissions" and "perms" aliases. The class must have
	 * the {@link PermissionGroup} annotation over subclasses and {@link Permission} annotations
	 * over each "public static final String" field.
	 *
	 * @param group
	 * @param classToList
	 */
	public PermsSubCommand(final FancyCommandGroup group, final Class<?> classToList) {
		this(group, "permissions|perms", classToList);
	}

	/**
	 * Create a new subcommand using the given command group and label. The class must have
	 * the {@link PermissionGroup} annotation over subclasses and {@link Permission} annotations
	 * over each "public static final String" field.
	 *
	 * @param group
	 * @param label
	 * @param classToList
	 */
	private PermsSubCommand(final FancyCommandGroup group, final String label, @NonNull final Class<?> classToList) {
		super(group, label);

		this.classToList = classToList;

		this.setProperties();
	}

	/*
	 * Set the properties for this command
	 */
	private void setProperties() {
		this.setDescription(Lang.component("command-perms-description"));
		this.setUsage(Lang.component("command-perms-usage"));
	}

	@Override
	protected void onCommand() {
		final String phrase = this.args.length > 0 ? this.joinArgs(0) : null;

		new ChatPaginator(17)
				.setFoundationHeader(Lang.legacy("command-perms-header"))
				.setPages(this.list(phrase))
				.send(this.audience);
	}

	/*
	 * Iterate through all classes and superclasses in the given classes and fill their permissions
	 * that match the given phrase
	 */
	private List<FancyComponent> list(final String phrase) {
		final List<FancyComponent> messages = new ArrayList<>();
		Class<?> iteratedClass = this.classToList;

		try {
			do
				this.listIn(iteratedClass, messages, phrase);
			while (!(iteratedClass = iteratedClass.getSuperclass()).isAssignableFrom(Object.class));

		} catch (final Exception ex) {
			ex.printStackTrace();
		}

		return messages;
	}

	/*
	 * Find annotations and compile permissions list from the given class and given existing
	 * permissions that match the given phrase
	 */
	private void listIn(final Class<?> clazz, final List<FancyComponent> messages, final String phrase) throws ReflectiveOperationException {
		final PermissionGroup group = clazz.getAnnotation(PermissionGroup.class);

		if (!messages.isEmpty() && !clazz.isAnnotationPresent(PermissionGroup.class))
			throw new FoException("Please place @PermissionGroup over " + clazz);

		final List<FancyComponent> subsectionMessages = new ArrayList<>();
		final Variables variables = Variables.builder();

		for (final Field field : clazz.getDeclaredFields()) {
			if (!field.isAnnotationPresent(Permission.class))
				continue;

			final Permission annotation = field.getAnnotation(Permission.class);

			String info = annotation.value();

			if (info.contains("{label}")) {
				final FancyCommandGroup defaultGroup = Platform.getPlugin().getDefaultCommandGroup();

				ValidCore.checkNotNull(defaultGroup, "Found {label} in @Permission under " + field + " while no default command group is set!");
			}

			info = variables.replaceLegacy(info);

			final boolean def = annotation.def();

			if (info.contains("{plugin_name}") || info.contains("{plugin}"))
				throw new FoException("Forgotten unsupported variable in " + info + " for field " + field + " in " + clazz);

			final String node = (String) field.get(null);

			if (node.contains("{plugin_name}") || node.contains("{plugin}"))
				throw new FoException("Forgotten unsupported variable in " + info + " for field " + field + " in " + clazz);

			final boolean has = this.audience == null ? false : this.hasPerm(node.replaceAll("\\.\\{.*?\\}", ""));

			if (phrase == null || node.contains(phrase))
				subsectionMessages.add(FancyComponent
						.fromMiniAmpersand("  " + (has ? "&a" : "&7") + node + (def ? " " + Lang.legacy("command-perms-true-by-default") : ""))
						.onClickOpenUrl("")
						.onClickSuggestCmd(node)
						.onHover(Lang.component("command-perms-info",
								"info", info,
								"default", def ? Lang.component("command-perms-yes") : Lang.component("command-perms-no"),
								"state", has ? Lang.component("command-perms-yes") : Lang.component("command-perms-no"))));
		}

		if (!subsectionMessages.isEmpty()) {
			messages.add(FancyComponent
					.fromMiniAmpersand("&7- ").append(messages.isEmpty() ? Lang.component("command-perms-main") : FancyComponent.fromPlain(group.value()))
					.onClickOpenUrl(""));

			messages.addAll(subsectionMessages);
		}

		for (final Class<?> inner : clazz.getDeclaredClasses()) {
			messages.add(FancyComponent.fromMiniNative("<reset> "));

			this.listIn(inner, messages, phrase);
		}
	}

	/**
	 * @see FancyCommandCore#tabComplete()
	 */
	@Override
	protected List<String> tabComplete() {
		return NO_COMPLETE;
	}
}
