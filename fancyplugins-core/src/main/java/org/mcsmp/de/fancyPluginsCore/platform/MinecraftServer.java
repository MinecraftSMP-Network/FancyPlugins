package org.mcsmp.de.fancyPluginsCore.platform;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a server running FancyPlugins
 */
public abstract class MinecraftServer {

	/**
	 * Returns the address of the server.
	 *
	 * @return
	 */
	public abstract InetSocketAddress getAddress();

	/**
	 * Return the name of the server.
	 *
	 * @return
	 */
	public abstract String getName();

	/**
	 * Return true if the server is empty.
	 *
	 * @return
	 */
	public  abstract boolean isEmpty();

	/**
	 * Return the player count on the server.
	 *
	 * @return
	 */
	public abstract int getPlayerCount();

	/**
	 * Return the unique ids of all players on the server.
	 *
	 * @return
	 */
	public abstract Set<UUID> getPlayerUniqueIDs();

	/**
	 * Send a message to the server.
	 * Throws error on Bukkit since it requires a proxy.
	 *
	 * @param channel
	 * @param data
	 */
	public abstract void sendData(String channel, byte[] data);
}
