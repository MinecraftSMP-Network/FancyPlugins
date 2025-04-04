package org.mcsmp.de.fancyPluginsCore.proxy.message;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.mcsmp.de.fancyPluginsCore.proxy.ProxyListener;
import org.mcsmp.de.fancyPluginsCore.proxy.ProxyMessage;
import org.mcsmp.de.fancyPluginsCore.utility.ValidCore;

/**
 * Represents a in/out message with a given action and server name
 * and a safety check for writing/reading the data
 * based on the action's content.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
abstract class Message {

	/**
	 * The default channel name. We broadcast on BungeeCord by default.
	 */
	public static final String DEFAULT_CHANNEL = "BungeeCord";

	/**
	 * Represents the largest size that an individual plugin message may be.
	 */
	public static final int MAX_MESSAGE_SIZE = 1048576;

	/**
	 * The listener associated with this message.
	 */
	private final ProxyListener listener;

	/**
	 * The action.
	 */
	private final ProxyMessage message;

	/**
	 * The current position of writing the data based on the
	 * {@link ProxyMessage#getContent()}.
	 */
	private int head = 0;

	/**
	 * Ensures we are reading in the correct order as the given {@link ProxyMessage}
	 * specifies in its {@link ProxyMessage#getContent()} getter.
	 * <p>
	 * This also ensures we are reading the correct data type (both primitives and wrappers
	 * are supported).
	 *
	 * @param givenType
	 */
	protected final void moveHead(final Class<?> givenType) {
		ValidCore.checkNotNull(this.message, "Action not set!");

		final Class<?>[] content = this.message.getContent();
		final Class<?> clazz = content[this.head];
		final String operation = this instanceof OutgoingMessage ? "write" : "read";

		ValidCore.checkBoolean(givenType.isAssignableFrom(clazz), "Cannot " + operation + " " + givenType + " at position " + this.head + " because " + this.getMessage().name() + " requires " + clazz.getSimpleName());
		ValidCore.checkBoolean(this.head < content.length, "Head out of bounds! Max data size for " + this.getMessage().name() + " is " + content.length);

		this.head++;
	}

	/**
	 * Get the channel for this message.
	 *
	 * @return
	 */
	public String getChannel() {
		ValidCore.checkNotNull(this.listener, "Listener cannot be null for " + this);

		return this.listener.getChannel();
	}

	/**
	 * Get the listener.
	 *
	 * @return
	 */
	public final ProxyListener getListener() {
		return this.listener;
	}

	/**
	 * Get the message.
	 *
	 * @param <T>
	 * @return
	 */
	public final <T extends ProxyMessage> T getMessage() {
		return (T) this.message;
	}

	@Override
	public String toString() {
		return this.message.name();
	}
}
