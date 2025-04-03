package org.mcsmp.de.fancyPluginsCore.proxy.message;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.mcsmp.de.fancyPluginsCore.collection.SerializedMap;
import org.mcsmp.de.fancyPluginsCore.debug.Debugger;
import org.mcsmp.de.fancyPluginsCore.model.FancyComponent;
import org.mcsmp.de.fancyPluginsCore.platform.MinecraftServer;
import org.mcsmp.de.fancyPluginsCore.platform.Platform;
import org.mcsmp.de.fancyPluginsCore.proxy.ProxyListener;
import org.mcsmp.de.fancyPluginsCore.proxy.ProxyMessage;
import org.mcsmp.de.fancyPluginsCore.utility.CommonCore;
import org.mcsmp.de.fancyPluginsCore.utility.SerializeUtilCore;
import org.mcsmp.de.fancyPluginsCore.utility.ValidCore;

/**
 * NB: This uses the standardized Foundation model where the first
 * String is the server name and the second String is the
 * {@link ProxyMessage} by its name *written automatically*.
 */
public final class OutgoingMessage extends Message {

	/**
	 * The pending queue to write the message.
	 */
	private final List<Object> queue = new ArrayList<>();

	/**
	 * Create a new outgoing message, see header of this class.
	 *
	 * @param message
	 */
	public OutgoingMessage(final ProxyMessage message) {
		this(getDefaultListener(), message);
	}

	/*
	 * Get the default listener and ensure it is set.
	 */
	private static ProxyListener getDefaultListener() {
		final ProxyListener defaultListener = Platform.getPlugin().getDefaultProxyListener();
		ValidCore.checkNotNull(defaultListener, "Cannot call OutgoingMessage with no params because getDefaultProxyListener() in your main plugin's class is not set!");

		return defaultListener;
	}

	/**
	 * Create a new outgoing message, see header of this class
	 *
	 * @param listener
	 * @param message
	 */
	public OutgoingMessage(final ProxyListener listener, final ProxyMessage message) {
		super(listener, message);
	}

	/**
	 * Write the given strings into the message
	 *
	 * @param messages
	 */
	public void writeString(final String... messages) {
		for (final String message : messages)
			this.write(message, String.class);
	}

	/**
	 * Write the map into the message
	 *
	 * @param component
	 */
	public void writeSimpleComponent(final FancyComponent component) {
		this.write(component, FancyComponent.class);
	}

	/**
	 * Write the map into the message
	 *
	 * @param map
	 */
	public void writeMap(final SerializedMap map) {
		this.write(map, SerializedMap.class);
	}

	/**
	 * Write a boolean into the message
	 *
	 * @param bool
	 */
	public void writeBoolean(final boolean bool) {
		this.write(bool, Boolean.class);
	}

	/**
	 * Write a byte into the message
	 *
	 * @param number
	 */
	public void writeByte(final byte number) {
		this.write(number, Byte.class);
	}

	/**
	 * Write a double into the message
	 *
	 * @param number
	 */
	public void writeDouble(final double number) {
		this.write(number, Double.class);
	}

	/**
	 * Write a float into the message
	 *
	 * @param number
	 */
	public void writeFloat(final float number) {
		this.write(number, Float.class);
	}

	/**
	 * Write an integer into the message
	 *
	 * @param number
	 */
	public void writeInt(final int number) {
		this.write(number, Integer.class);
	}

	/**
	 * Write a float into the message
	 *
	 * @param number
	 */
	public void writeLong(final long number) {
		this.write(number, Long.class);
	}

	/**
	 * Write a short into the message
	 *
	 * @param number
	 */
	public void writeShort(final short number) {
		this.write(number, Short.class);
	}

	/**
	 * Write an uuid into the message
	 *
	 * @param uuid
	 */
	public void writeUUID(final UUID uuid) {
		this.write(uuid, UUID.class);
	}

	/**
	 * Write an uuid into the message
	 *
	 * @param enumInstance
	 */
	public void writeEnum(final Enum<?> enumInstance) {
		this.writeString(enumInstance.toString()); // Write enums as strings
	}

	/**
	 * Write an object of the given type into the message
	 *
	 * @param object
	 * @param typeOf
	 */
	public void write(final Object object, final Class<?> typeOf) {
		ValidCore.checkNotNull(object, "Added object must not be null!");

		this.moveHead(typeOf);
		this.queue.add(object);
	}

	/**
	 * Compiles a byte array of the message from the given sender.
	 *
	 * @param senderUid
	 * @param serverName
	 * @return
	 */
	public byte[] toByteArray(final UUID senderUid, final String serverName) {
		final String channel = this.getChannel();
		final ProxyMessage message = this.getMessage();
		final Object[] dataArray = this.queue.toArray();
		ValidCore.checkBoolean(dataArray.length == message.getContent().length, "Proxy message " + message + " on channel " + channel + " has invalid data lenght! Expected: " + message.getContent().length + ". Got: " + dataArray.length);

		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		final DataOutputStream out = new DataOutputStream(byteArrayOutputStream);

		try {
			out.writeUTF(channel);
			out.writeUTF(senderUid.toString());
			out.writeUTF(serverName);
			out.writeUTF(message.toString());

		} catch (final IOException ex) {
			CommonCore.throwError(ex, "Error writing header in proxy plugin message!", "Message: " + message, "Channel: " + channel, "Error: {error}", "All data: " + CommonCore.join(dataArray, data2 -> CommonCore.getOrDefault(SerializeUtilCore.serialize(SerializeUtilCore.Language.YAML, data2), "").toString()));
		}

		for (final Object data : dataArray)
			try {
				if (data instanceof Integer)
					out.writeInt((Integer) data);
				else if (data instanceof Double)
					out.writeDouble((Double) data);
				else if (data instanceof Long)
					out.writeLong((Long) data);
				else if (data instanceof Boolean)
					out.writeBoolean((Boolean) data);
				else if (data instanceof String)
					out.writeUTF((String) data);
				else if (data instanceof FancyComponent)
					out.writeUTF(((FancyComponent) data).serialize().toJson());
				else if (data instanceof SerializedMap)
					out.writeUTF(((SerializedMap) data).toJson());
				else if (data instanceof UUID)
					out.writeUTF(((UUID) data).toString());
				else if (data instanceof Enum)
					out.writeUTF(((Enum<?>) data).toString());
				else if (data instanceof byte[])
					out.write((byte[]) data);
				else
					throw new IllegalArgumentException("Unknown data type to write as plugin message: " + data.getClass());

			} catch (final Throwable t) {
				CommonCore.throwError(t, "Error writing data in proxy plugin message!", "Message: " + message, "Channel: " + channel, "Errored data: " + (data instanceof FancyComponent ? ((FancyComponent) data).toPlain() : data), "Error: {error}", "All data: " + CommonCore.join(dataArray, data2 -> CommonCore.getOrDefault(SerializeUtilCore.serialize(SerializeUtilCore.Language.YAML, data2), "null").toString()));

				return null;
			}

		return byteArrayOutputStream.toByteArray();
	}

	/**
	 * Sends this outgoing message into proxy in the following format:
	 * <p>
	 * You need an implementation in proxy to handle it, otherwise nothing will happen.
	 *
	 * <ul>
	 *   <li><b>Channel</b>: The identifier for the communication channel through
	 *       which the message is sent.</li>
	 *   <li><b>Sender UUID</b>: The unique identifier of the player or entity
	 *       that originates the message.</li>
	 *   <li><b>Server Name</b>: The name of the server sending the message, as
	 *       defined by the platform. Please call {@link Platform#setCustomServerName(String)} first.</li>
	 *   <li><b>Message Content</b>: A string representation of the proxy message
	 *       that describes its type or action.</li>
	 * 	 <li><b>Data Array</b>: Byte array of data set in this class.</li>
	 * </ul>
	 * </code></pre>
	 *
	 * @param senderUid
	 */
	public void send(final UUID senderUid) {
		final String channel = this.getChannel();
		final ProxyMessage message = this.getMessage();
		final byte[] byteArray = this.toByteArray(senderUid, Platform.getCustomServerName());

		if (byteArray.length >= MAX_MESSAGE_SIZE) {
			CommonCore.log("Outgoing proxy message '" + message + "' was oversized, not sending. Max length: " + MAX_MESSAGE_SIZE + " bytes, got " + byteArray.length + " bytes.");

			return;
		}

		try {
			Platform.sendPluginMessage(senderUid, ProxyListener.DEFAULT_CHANNEL, byteArray);

		} catch (final Throwable throwable) {
			final String throwableClass = throwable.getClass().getSimpleName();

			if (throwableClass.equals("NotRegistered"))
				CommonCore.log("Cannot send proxy message " + message + " because channel '" + ProxyListener.DEFAULT_CHANNEL + "/" + channel + "' is not registered. " + "Use @AutoRegister above your class extending ProxyListener and return its instance in getProxy in your main plugin class.");

			else
				CommonCore.error(throwable, "Failed to send proxy message " + message + " on channel " + channel);
		}
	}

	/**
	 * Forwards this message to another server
	 *
	 * @param fromServer
	 * @param server
	 */
	public void sendToServer(final String fromServer, final MinecraftServer server) {
		synchronized (ProxyListener.DEFAULT_CHANNEL) {
			final String channel = this.getChannel();
			final byte[] byteArray = this.toByteArray(CommonCore.ZERO_UUID, fromServer);
			final boolean isSpammyPacket = this.getMessage().name().startsWith("SYNCED_CACHE");

			if (server.isEmpty()) {
				if (!isSpammyPacket)
					Debugger.debug("proxy", "NOT sending data on " + channel + " channel from " + this + " to " + server.getName() + " server because it is empty.");

				return;
			}

			if (byteArray.length >= Message.MAX_MESSAGE_SIZE) {
				CommonCore.log("Outgoing proxy message '" + this + "' was oversized, not sending. Max length: " + Message.MAX_MESSAGE_SIZE + " bytes, got " + byteArray.length + " bytes.");

				return;
			}

			server.sendData(DEFAULT_CHANNEL, byteArray);

			if (!isSpammyPacket)
				Debugger.debug("proxy", "Forwarding data on " + channel + " channel from " + this + " to " + server.getName() + " server.");
		}
	}

	/**
	 * Broadcasts the message to all servers
	 */
	public void broadcast() {
		this.broadcastExcept(null);
	}

	/**
	 * Broadcasts the message to all servers except the one ignored
	 *
	 * @param ignoredServerName
	 */
	public void broadcastExcept(@Nullable final String ignoredServerName) {
		synchronized (ProxyListener.DEFAULT_CHANNEL) {
			final String channel = this.getChannel();
			final boolean isSpammyPacket = this.getMessage().name().startsWith("SYNCED_CACHE");

			for (final MinecraftServer otherServer : Platform.getServers()) {
				if (otherServer.isEmpty()) {
					if (!isSpammyPacket)
						Debugger.debug("proxy", "NOT sending data on " + channel + " channel from " + this + " to " + otherServer.getName() + " server because it is empty.");

					continue;
				}

				if (ignoredServerName != null && otherServer.getName().equalsIgnoreCase(ignoredServerName)) {
					if (!isSpammyPacket)
						Debugger.debug("proxy", "NOT sending data on " + channel + " channel from " + this + " to " + otherServer.getName() + " server because it is ignored.");

					continue;
				}

				final byte[] byteArray = this.toByteArray(CommonCore.ZERO_UUID, otherServer.getName());

				if (byteArray.length >= Message.MAX_MESSAGE_SIZE) {
					CommonCore.log("Outgoing proxy message '" + this + "' was oversized, not sending. Max length: " + Message.MAX_MESSAGE_SIZE + " bytes, got " + byteArray.length + " bytes.");

					return;
				}

				otherServer.sendData(DEFAULT_CHANNEL, byteArray);

				if (!isSpammyPacket)
					Debugger.debug("proxy", "Sending data on " + channel + " channel from " + this + " to " + otherServer.getName() + " server.");
			}
		}
	}
}
