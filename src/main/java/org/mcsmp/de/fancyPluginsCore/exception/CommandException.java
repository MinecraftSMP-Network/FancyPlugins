package org.mcsmp.de.fancyPluginsCore.exception;

import org.mcsmp.de.fancyPluginsCore.model.CompChatColor;
import org.mcsmp.de.fancyPluginsCore.model.FancyComponent;
import org.mcsmp.de.fancyPluginsCore.platform.MinecraftPlayer;
import org.mcsmp.de.fancyPluginsCore.utility.Messenger;

/**
 * Represents a silent exception with a localizable message.
 */
public class CommandException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * The messages to send to the command sender, null if not set
	 */
	private final FancyComponent[] components;

	/**
	 * Create a new command exception
	 */
	public CommandException() {
		this((FancyComponent[]) null);
	}

	/**
	 * Create a new command exception with message for the command sender
	 *
	 * @param components
	 */
	public CommandException(final FancyComponent... components) {
		super("");

		this.components = components;
	}

	/**
	 * Return the components to send to the command sender.
	 *
	 * @return
	 */
	public final FancyComponent[] getComponents() {
		return this.components == null ? new FancyComponent[0] : this.components;
	}

	/**
	 * Send the error message to the given audience.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param audience
	 */
	public final void sendErrorMessage(final MinecraftPlayer audience) {
		if (this.components != null)
			if (this.components.length == 1) {
				final FancyComponent error = this.components[0];

				if (!error.isEmpty())
					Messenger.error(audience, this.components[0]);
			} else
				for (final FancyComponent component : this.components)
					if (!component.isEmpty())
						audience.sendMessage(component.color(CompChatColor.RED));
	}

	/**
	 * Get the message as a string.
	 *
	 * @return
	 */
	@Override
	public final String getMessage() {
		final StringBuilder builder = new StringBuilder();

		if (this.components != null)
			for (final FancyComponent component : this.components)
				builder.append(component.toLegacySection(null));

		return builder.toString();
	}
}
