package org.mcsmp.de.fancyPluginsCore.proxy;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import org.mcsmp.de.fancyPluginsCore.proxy.message.IncomingMessage;
import org.mcsmp.de.fancyPluginsCore.utility.CommonCore;
import org.mcsmp.de.fancyPluginsCore.utility.ValidCore;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a proxy listener using on which you can listen to receiving messages
 * with Foundation format.
 *
 * This class is also a Listener for Bukkit events for your convenience
 */
@Getter
public abstract class ProxyListener {

	/**
	 * The default channel.
	 */
	public static final String DEFAULT_CHANNEL = "BungeeCord";

	/**
	 * Holds registered listeners.
	 */
	private static final Set<ProxyListener> registeredListeners = new HashSet<>();

	/**
	 * The channel.
	 */
	@Getter
	private final String channel;

	/**
	 * The actions.
	 */
	@Getter
	private final ProxyMessage[] actions;

	/**
	 * Temporary variable for reading data.
	 */
	@Getter(value = AccessLevel.PROTECTED)
	private byte[] data;

	/**
	 * Create a new listener with the given params.
	 *
	 * @param channel
	 * @param actionEnum
	 */
	protected ProxyListener(@NonNull final String channel, final Class<? extends ProxyMessage> actionEnum) {
		this.channel = channel;
		this.actions = toActions(actionEnum);

		for (final ProxyListener listener : registeredListeners)
			if (listener.getChannel().equals(this.getChannel()))
				return;

		registeredListeners.add(this);
	}

	private static ProxyMessage[] toActions(@NonNull final Class<? extends ProxyMessage> actionEnum) {
		ValidCore.checkBoolean(actionEnum != ProxyMessage.class, "When creating a new proxy listener put your own class that extend ProxyMessage there, not ProxyMessage class itself!");
		ValidCore.checkBoolean(actionEnum.isEnum(), "Proxy listener expects ProxyMessage to be an enum, given: " + actionEnum);

		try {
			return (ProxyMessage[]) actionEnum.getMethod("values").invoke(null);

		} catch (final ReflectiveOperationException ex) {
			CommonCore.throwError(ex, "Unable to get values() of " + actionEnum + ", ensure it is an enum or has 'public static T[] values() method'!");

			return null;
		}
	}

	/**
	 * Called automatically when you receive a plugin message from proxy
	 *
	 * @param message
	 */
	public abstract void onMessageReceived(IncomingMessage message);

	/**
	 * Called when an invalid message is received
	 *
	 * @param senderUid
	 * @param serverName
	 * @param actionName
	 */
	public void onInvalidMessageReceived(UUID senderUid, String serverName, String actionName) {
		new NullPointerException("Unknown plugin action '" + actionName + "' from server " + serverName + ". ALL SERVERS NEED TO HAVE THE SAME VERSION.").printStackTrace();
	}

	/**
	 * @deprecated internal use only
	 *
	 * @param data
	 */
	@Deprecated
	public final void setData(final byte[] data) {
		this.data = data;
	}

	@Override
	public final boolean equals(final Object obj) {
		return obj instanceof ProxyListener && ((ProxyListener) obj).getChannel().equals(this.getChannel());
	}

	/**
	 * @deprecated internal use only
	 */
	@Deprecated
	public static final void clearRegisteredListeners() {
		registeredListeners.clear();
	}

	/**
	 * Return all registered listeners
	 *
	 * @return
	 */
	public static final Set<ProxyListener> getRegisteredListeners() {
		return Collections.unmodifiableSet(registeredListeners);
	}
}
