package org.mcsmp.de.fancyPluginsCore.proxy.message;

import lombok.Getter;
import lombok.NonNull;
import org.mcsmp.de.fancyPluginsCore.collection.SerializedMap;
import org.mcsmp.de.fancyPluginsCore.model.FancyComponent;
import org.mcsmp.de.fancyPluginsCore.proxy.ProxyListener;
import org.mcsmp.de.fancyPluginsCore.proxy.ProxyMessage;
import org.mcsmp.de.fancyPluginsCore.utility.CommonCore;
import org.mcsmp.de.fancyPluginsCore.utility.ReflectionUtil;
import org.mcsmp.de.fancyPluginsCore.utility.SerializeUtilCore.Language;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.IOException;
import java.util.UUID;

/**
 * Represents an incoming plugin message.
 * <p>
 * NB: This uses the standardized Foundation model where the first
 * string is the server name and the second string is the
 * {@link ProxyMessage} by its name *read automatically*.
 */
public final class IncomingMessage extends Message {

	/**
	 * The raw byte array to read from
	 */
	@Getter
	private final byte[] data;

	/**
	 * The sender UUID
	 */
	@Getter
	private final UUID senderUid;

	/**
	 * The serverName
	 */
	@Getter
	private final String serverName;

	/**
	 * The input we use to read our data array
	 */
	private final DataInput input;

	/**
	 * The internal stream
	 */
	private final ByteArrayInputStream stream;

	/**
	 * Create a new incoming message from the given array
	 *
	 * NB: This uses the standardized Foundation header, see {@link OutgoingMessage#send(UUID)}
	 *
	 * @param listener
	 * @param senderUid
	 * @param serverName
	 * @param type
	 * @param data
	 * @param input
	 * @param stream
	 */
	public IncomingMessage(@NonNull final ProxyListener listener, @NonNull final UUID senderUid, @NonNull final String serverName, @NonNull final ProxyMessage type, @NonNull final byte[] data, @NonNull final DataInput input, @NonNull final ByteArrayInputStream stream) {
		super(listener, type);

		this.data = data;
		this.senderUid = senderUid;
		this.serverName = serverName;
		this.input = input;
		this.stream = stream;
	}

	/**
	 * Read a string from the data
	 *
	 * @return
	 */
	public String readString() {
		this.moveHead(String.class);

		try {
			return this.input.readUTF();

		} catch (final IOException ex) {
			CommonCore.sneaky(ex);

			return "";
		}
	}

	/**
	 * Read a simple component from the string data if json
	 *
	 * @return
	 */
	public FancyComponent readSimpleComponent() {
		this.moveHead(FancyComponent.class);

		try {
			final String raw = this.input.readUTF();

			return FancyComponent.deserialize(SerializedMap.fromObject(Language.JSON, raw));

		} catch (final IOException ex) {
			CommonCore.sneaky(ex);

			return FancyComponent.empty();
		}
	}

	/**
	 * Read a map from the string data if json
	 *
	 * @return
	 */
	public SerializedMap readMap() {
		this.moveHead(SerializedMap.class);

		try {
			final String raw = this.input.readUTF();

			return SerializedMap.fromObject(Language.JSON, raw);

		} catch (final IOException ex) {
			CommonCore.sneaky(ex);

			return new SerializedMap();
		}
	}

	/**
	 * Read a UUID from the string data
	 *
	 * @return
	 */
	public UUID readUUID() {
		this.moveHead(UUID.class);

		try {
			return UUID.fromString(this.input.readUTF());

		} catch (final IOException ex) {
			CommonCore.sneaky(ex);

			return null;
		}
	}

	/**
	 * Read an enumerator from the given string data
	 *
	 * @param <T>
	 * @param typeOf
	 * @return
	 */
	public <T extends Enum<T>> T readEnum(final Class<T> typeOf) {
		this.moveHead(String.class); // Read enums as Strings

		try {
			final String raw = this.input.readUTF();

			return ReflectionUtil.lookupEnum(typeOf, raw);

		} catch (final IOException ex) {
			CommonCore.sneaky(ex);

			return null;
		}
	}

	/**
	 * Read a boolean from the data
	 *
	 * @return
	 */
	public boolean readBoolean() {
		this.moveHead(Boolean.class);

		try {
			return this.input.readBoolean();

		} catch (final IOException ex) {
			CommonCore.sneaky(ex);

			return false;
		}
	}

	/**
	 * Read a byte from the data
	 *
	 * @return
	 */
	public byte readByte() {
		this.moveHead(Byte.class);

		try {
			return this.input.readByte();

		} catch (final IOException ex) {
			CommonCore.sneaky(ex);

			return 0;
		}
	}

	/**
	 * Reads the rest of the bytes
	 *
	 * @return
	 */
	public byte[] readBytes() {
		this.moveHead(byte[].class);

		final byte[] array = new byte[this.stream.available()];

		try {
			this.stream.read(array);

		} catch (final IOException e) {
			e.printStackTrace();
		}

		return array;
	}

	/**
	 * Read a double from the data
	 *
	 * @return
	 */
	public double readDouble() {
		this.moveHead(Double.class);

		try {
			return this.input.readDouble();

		} catch (final IOException ex) {
			CommonCore.sneaky(ex);

			return 0;
		}
	}

	/**
	 * Read a float from the data
	 *
	 * @return
	 */
	public float readFloat() {
		this.moveHead(Float.class);

		try {
			return this.input.readFloat();

		} catch (final IOException ex) {
			CommonCore.sneaky(ex);

			return 0;
		}
	}

	/**
	 * Read an integer from the data
	 *
	 * @return
	 */
	public int readInt() {
		this.moveHead(Integer.class);

		try {
			return this.input.readInt();

		} catch (final IOException ex) {
			CommonCore.sneaky(ex);

			return 0;
		}
	}

	/**
	 * Read a long from the data
	 *
	 * @return
	 */
	public long readLong() {
		this.moveHead(Long.class);

		try {
			return this.input.readLong();

		} catch (final IOException ex) {
			CommonCore.sneaky(ex);

			return 0;
		}
	}

	/**
	 * Read a short from the data
	 *
	 * @return
	 */
	public short readShort() {
		this.moveHead(Short.class);

		try {
			return this.input.readShort();

		} catch (final IOException ex) {
			CommonCore.sneaky(ex);

			return 0;
		}
	}

	/**
	 *
	 * @return
	 */
	@Override
	public String getChannel() {
		return this.getListener().getChannel();
	}
}
