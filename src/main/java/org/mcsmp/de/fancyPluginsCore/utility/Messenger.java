package org.mcsmp.de.fancyPluginsCore.utility;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.mcsmp.de.fancyPluginsCore.model.FancyComponent;
import org.mcsmp.de.fancyPluginsCore.platform.MinecraftPlayer;
import org.mcsmp.de.fancyPluginsCore.platform.Platform;
import org.mcsmp.de.fancyPluginsCore.settings.Lang;

/**
 * Utility class for sending messages with different prefixes specified in {@link Lang}.
 *
 * This is a platform-neutral class, which is extended by "Messenger" classes for different
 * platforms, such as Bukkit.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Messenger {

	// ----------------------------------------------------------------------------------------------------
	// Prefixes
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Get the prefix used for success messages.
	 *
	 * This can be changed in your lang/xx_XX.json file under 'prefix-success'.
	 *
	 * @see Lang#component(String, Object...)
	 * @return
	 */
	public static FancyComponent getSuccessPrefix() {
		return Lang.prefixOrEmpty("prefix-success");
	}

	/**
	 * Get the prefix used for information messages.
	 *
	 * This can be changed in your lang/xx_XX.json file under 'prefix-info'.
	 *
	 * @see Lang#component(String, Object...)
	 * @return
	 */
	public static FancyComponent getInfoPrefix() {
		return Lang.prefixOrEmpty("prefix-info");
	}

	/**
	 * Get the prefix used for warning messages.
	 *
	 * This can be changed in your lang/xx_XX.json file under 'prefix-warn'.
	 *
	 * @see Lang#component(String, Object...)
	 * @return
	 */
	public static FancyComponent getWarnPrefix() {
		return Lang.prefixOrEmpty("prefix-warn");
	}

	/**
	 * Get the prefix used for error messages.
	 *
	 * This can be changed in your lang/xx_XX.json file under 'prefix-error'.
	 *
	 * @see Lang#component(String, Object...)
	 * @return
	 */
	public static FancyComponent getErrorPrefix() {
		return Lang.prefixOrEmpty("prefix-error");
	}

	/**
	 * Get the prefix used for questions.
	 *
	 * This can be changed in your lang/xx_XX.json file under 'prefix-question'.
	 *
	 * @see Lang#component(String, Object...)
	 * @return
	 */
	public static FancyComponent getQuestionPrefix() {
		return Lang.prefixOrEmpty("prefix-question");
	}

	/**
	 * Get the prefix used for announcements.
	 *
	 * This can be changed in your lang/xx_XX.json file under 'prefix-announce'.
	 *
	 * @see Lang#component(String, Object...)
	 * @return
	 */
	public static FancyComponent getAnnouncePrefix() {
		return Lang.prefixOrEmpty("prefix-announce");
	}

	// ----------------------------------------------------------------------------------------------------
	// Broadcasting
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Broadcast a message to online players prepended with the {@link #getInfoPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param message
	 */
	public static void broadcastInfo(final String message) {
		for (final MinecraftPlayer online : Platform.getOnlinePlayers())
			info(online, message);
	}

	/**
	 * Broadcast a message to online players prepended with the {@link #getInfoPrefix()} prefix.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param component
	 */
	public static void broadcastInfo(final FancyComponent component) {
		for (final MinecraftPlayer online : Platform.getOnlinePlayers())
			info(online, component);
	}

	/**
	 * Broadcast a message to online players prepended with the {@link #getSuccessPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param message
	 */
	public static void broadcastSuccess(final String message) {
		for (final MinecraftPlayer online : Platform.getOnlinePlayers())
			success(online, message);
	}

	/**
	 * Broadcast a message to online players prepended with the {@link #getSuccessPrefix()} prefix.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param component
	 */
	public static void broadcastSuccess(final FancyComponent component) {
		for (final MinecraftPlayer online : Platform.getOnlinePlayers())
			success(online, component);
	}

	/**
	 * Broadcast a message to online players prepended with the {@link #getWarnPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param message
	 */
	public static void broadcastWarn(final String message) {
		for (final MinecraftPlayer online : Platform.getOnlinePlayers())
			warn(online, message);
	}

	/**
	 * Broadcast a message to online players prepended with the {@link #getWarnPrefix()} prefix.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param component
	 */
	public static void broadcastWarn(final FancyComponent component) {
		for (final MinecraftPlayer online : Platform.getOnlinePlayers())
			warn(online, component);
	}

	/**
	 * Broadcast a message to online players prepended with the {@link #getErrorPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param message
	 */
	public static void broadcastError(final String message) {
		for (final MinecraftPlayer online : Platform.getOnlinePlayers())
			error(online, message);
	}

	/**
	 * Broadcast a message to online players prepended with the {@link #getErrorPrefix()} prefix.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param component
	 */
	public static void broadcastError(final FancyComponent component) {
		for (final MinecraftPlayer online : Platform.getOnlinePlayers())
			error(online, component);
	}

	/**
	 * Broadcast a message to online players prepended with the {@link #getQuestionPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param message
	 */
	public static void broadcastQuestion(final String message) {
		for (final MinecraftPlayer online : Platform.getOnlinePlayers())
			question(online, message);
	}

	/**
	 * Broadcast a message to online players prepended with the {@link #getQuestionPrefix()} prefix.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param component
	 */
	public static void broadcastQuestion(final FancyComponent component) {
		for (final MinecraftPlayer online : Platform.getOnlinePlayers())
			question(online, component);
	}

	/**
	 * Broadcast a message to online players prepended with the {@link #getAnnouncePrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param message
	 */
	public static void broadcastAnnounce(final String message) {
		for (final MinecraftPlayer online : Platform.getOnlinePlayers())
			announce(online, message);
	}

	/**
	 * Broadcast a message to online players prepended with the {@link #getAnnouncePrefix()} prefix.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param component
	 */
	public static void broadcastAnnounce(final FancyComponent component) {
		for (final MinecraftPlayer online : Platform.getOnlinePlayers())
			announce(online, component);
	}

	// ----------------------------------------------------------------------------------------------------
	// Telling by converting the object into a MinecraftPlayer
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Send a message prepended with the {@link #getInfoPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param message
	 */
	public static <T> void info(final T sender, final String message) {
		info(Platform.toPlayer(sender), message);
	}

	/**
	 * Send a message prepended with the {@link #getInfoPrefix()} prefix.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param component
	 */
	public static <T> void info(final T sender, final FancyComponent component) {
		info(Platform.toPlayer(sender), component);
	}

	/**
	 * Send a message prepended with the {@link #getSuccessPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param message
	 */
	public static <T> void success(final T sender, final String message) {
		success(Platform.toPlayer(sender), message);
	}

	/**
	 * Send a message prepended with the {@link #getSuccessPrefix()} prefix.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param component
	 */
	public static <T> void success(final T sender, final FancyComponent component) {
		success(Platform.toPlayer(sender), component);
	}

	/**
	 * Send a message prepended with the {@link #getWarnPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param message
	 */
	public static <T> void warn(final T sender, final String message) {
		warn(Platform.toPlayer(sender), message);
	}

	/**
	 * Send a message prepended with the {@link #getWarnPrefix()} prefix.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param component
	 */
	public static <T> void warn(final T sender, final FancyComponent component) {
		warn(Platform.toPlayer(sender), component);
	}

	/**
	 * Send a message prepended with the {@link #getErrorPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param message
	 */
	public static <T> void error(final T sender, final String message) {
		error(Platform.toPlayer(sender), message);
	}

	/**
	 * Send a message prepended with the {@link #getErrorPrefix()} prefix.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param component
	 */
	public static <T> void error(final T sender, final FancyComponent component) {
		error(Platform.toPlayer(sender), component);
	}

	/**
	 * Send a message prepended with the {@link #getQuestionPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param message
	 */
	public static <T> void question(final T sender, final String message) {
		question(Platform.toPlayer(sender), message);
	}

	/**
	 * Send a message prepended with the {@link #getQuestionPrefix()} prefix.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param component
	 */
	public static <T> void question(final T sender, final FancyComponent component) {
		question(Platform.toPlayer(sender), component);
	}

	/**
	 * Send a message prepended with the {@link #getAnnouncePrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param message
	 */
	public static <T> void announce(final T sender, final String message) {
		announce(Platform.toPlayer(sender), message);
	}

	/**
	 * Send a message prepended with the {@link #getAnnouncePrefix()} prefix.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param <T>
	 * @param sender
	 * @param component
	 */
	public static <T> void announce(final T sender, final FancyComponent component) {
		announce(Platform.toPlayer(sender), component);
	}

	// ----------------------------------------------------------------------------------------------------
	// Telliung
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Send a message prepended with the {@link #getInfoPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param audience
	 * @param message
	 */
	public static void info(final MinecraftPlayer audience, final String message) {
		tell(audience, Lang.prefix("prefix-info"), message);
	}

	/**
	 * Send a message prepended with the {@link #getInfoPrefix()} prefix.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param audience
	 * @param component
	 */
	public static void info(final MinecraftPlayer audience, final FancyComponent component) {
		tell(audience, Lang.prefix("prefix-info"), component);
	}

	/**
	 * Send a message prepended with the {@link #getSuccessPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param audience
	 * @param message
	 */
	public static void success(final MinecraftPlayer audience, final String message) {
		tell(audience, Lang.prefix("prefix-success"), message);
	}

	/**
	 * Send a message prepended with the {@link #getSuccessPrefix()} prefix.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param audience
	 * @param component
	 */
	public static void success(final MinecraftPlayer audience, final FancyComponent component) {
		tell(audience, Lang.prefix("prefix-success"), component);
	}

	/**
	 * Send a message prepended with the {@link #getWarnPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param audience
	 * @param message
	 */
	public static void warn(final MinecraftPlayer audience, final String message) {
		tell(audience, Lang.prefix("prefix-warn"), message);
	}

	/**
	 * Send a message prepended with the {@link #getWarnPrefix()} prefix.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param audience
	 * @param component
	 */
	public static void warn(final MinecraftPlayer audience, final FancyComponent component) {
		tell(audience, Lang.prefix("prefix-warn"), component);
	}

	/**
	 * Send a message prepended with the {@link #getErrorPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param audience
	 * @param message
	 */
	public static void error(final MinecraftPlayer audience, final String message) {
		tell(audience, Lang.prefix("prefix-error"), message);
	}

	/**
	 * Send a message prepended with the {@link #getErrorPrefix()} prefix.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param audience
	 * @param component
	 */
	public static void error(final MinecraftPlayer audience, final FancyComponent component) {
		tell(audience, Lang.prefix("prefix-error"), component);
	}

	/**
	 * Send a message prepended with the {@link #getQuestionPrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param audience
	 * @param message
	 */
	public static void question(final MinecraftPlayer audience, final String message) {
		tell(audience, Lang.prefix("prefix-question"), message);
	}

	/**
	 * Send a message prepended with the {@link #getQuestionPrefix()} prefix.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param audience
	 * @param component
	 */
	public static void question(final MinecraftPlayer audience, final FancyComponent component) {
		tell(audience, Lang.prefix("prefix-question"), component);
	}

	/**
	 * Send a message prepended with the {@link #getAnnouncePrefix()} prefix.
	 *
	 * The message is converted into a component with legacy and MiniMessage tags supported.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param audience
	 * @param message
	 */
	public static void announce(final MinecraftPlayer audience, final String message) {
		tell(audience, Lang.prefix("prefix-announce"), message);
	}

	/**
	 * Send a message prepended with the {@link #getAnnouncePrefix()} prefix.
	 *
	 * @see MinecraftPlayer#sendMessage(FancyComponent)
	 *
	 * @param audience
	 * @param component
	 */
	public static void announce(final MinecraftPlayer audience, final FancyComponent component) {
		tell(audience, Lang.prefix("prefix-announce"), component);
	}

	/*
	 * Send a message to the player with the given prefix.
	 */
	private static void tell(final MinecraftPlayer audience, final FancyComponent prefix, @NonNull final String message) {
		tell(audience, prefix, FancyComponent.fromMiniAmpersand(message));
	}

	/*
	 * Send a message to the player with the given prefix.
	 */
	private static void tell(final MinecraftPlayer audience, final FancyComponent prefix, @NonNull final FancyComponent component) {
		audience.sendMessageWithPrefix(prefix, component);
	}
}
