package org.mcsmp.de.fancyPluginsCore.platform;

import lombok.NonNull;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Emitter;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextDecoration.State;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import net.kyori.adventure.title.TitlePart;
import org.mcsmp.de.fancyPluginsCore.exception.FoException;
import org.mcsmp.de.fancyPluginsCore.model.CompChatColor;
import org.mcsmp.de.fancyPluginsCore.model.FancyComponent;
import org.mcsmp.de.fancyPluginsCore.model.FancyLocation;
import org.mcsmp.de.fancyPluginsCore.settings.Lang;
import org.mcsmp.de.fancyPluginsCore.utility.ChatUtil;
import org.mcsmp.de.fancyPluginsCore.utility.CommonCore;
import org.mcsmp.de.fancyPluginsCore.utility.Messenger;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Similar to Audience in Adventure, the MinecraftPlayer represents a platform-neutral
 * audience that can receive messages and interact with the server.
 *
 * However, it can only hold ONE player at a time to prevent confusion (Audience
 * can also hold the entire server's audience which is a new concept unused in Bukkit)
 *
 * To create one, use {@link Platform#toPlayer(Object)} and pass in the familiar object
 * such as a Player or a CommandSender.
 */
public abstract class MinecraftPlayer implements Audience {

	/**
	 * The minimum protocol version required for HEX colors to work. Added in 20w17a.
	 */
	protected static final int MINIMUM_PROTOCOL_FOR_HEX = 713;

	/**
	 * Spoofs a chat message from the player.
	 *
	 * @param message
	 */
	public abstract void chat(String message);

	/**
	 * Clears the title from the player.
	 */
	@Override
	public final void clearTitle() {
		this.resetTitle();
	}

	/**
	 * @deprecated unsupported operation
	 */
	@Deprecated
	@Override
	public final void deleteMessage(final SignedMessage.Signature signature) {
		throw new UnsupportedOperationException("deleteMessage");
	}

	/**
	 * Runs the given command (without /) as the player, replacing {player} and {player_name} with his name.
	 *
	 * You can prefix the command with @(announce|warn|error|info|question|success) to send a formatted
	 * message to playerReplacement directly.
	 *
	 * @param command
	 */
	public final void dispatchCommand(String command) {
		if (command.isEmpty() || command.equalsIgnoreCase("none"))
			return;

		// Only offer bare minimum variable support for best performance
		// Plugins are expected to parse variables on their own
		command = command
				.replace("{player_name}", this.getName())
				.replace("{player}", this.getName());

		if (command.startsWith("@announce "))
			Messenger.announce(this, command.replace("@announce ", ""));

		else if (command.startsWith("@warn "))
			Messenger.warn(this, command.replace("@warn ", ""));

		else if (command.startsWith("@error "))
			Messenger.error(this, command.replace("@error ", ""));

		else if (command.startsWith("@info "))
			Messenger.info(this, command.replace("@info ", ""));

		else if (command.startsWith("@question "))
			Messenger.question(this, command.replace("@question ", ""));

		else if (command.startsWith("@success "))
			Messenger.success(this, command.replace("@success ", ""));

		else {
			command = command.charAt(0) == '/' && command.charAt(1) != '/' ? command.substring(1) : command;

			if (this.isPlayer())
				this.performPlayerCommand0(command.replace("§", "&"));
			else
				Platform.dispatchConsoleCommand(this, command);
		}
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof MinecraftPlayer) {
			final MinecraftPlayer other = (MinecraftPlayer) obj;

			if (other.isPlayer() && !this.isPlayer())
				return false;

			if (!other.isPlayer() && this.isPlayer())
				return false;

			return other.isPlayer() ? other.getUniqueId().equals(this.getUniqueId()) : other.getName().equals(this.getName());
		}

		return false;
	}

	/**
	 * Returns the player's IP address and port or null if not a player or not supported by platform.
	 *
	 * @return
	 */
	public abstract InetSocketAddress getAddress();

	/**
	 * Returns the player's location or throws error if console or not running on Bukkit.
	 *
	 * @see #isPlayer()
	 * @deprecated platform-specific
	 *
	 * @return
	 */
	@Deprecated
	public FancyLocation getLocation() {
		throw new UnsupportedOperationException("getBukkitLocation not implemented for " + Platform.getType());
	}

	/**
	 * Returns the player's name, or the "part-console" lang key if the player is a console.
	 *
	 * @see Lang
	 * @return
	 */
	public final String getName() {
		return this.isConsole() ? Lang.legacy("part-console") : this.getSenderName0();
	}

	/**
	 * Get the player implementation object, such as Player on Bukkit.
	 * Returns null if not applicable.
	 *
	 * @param <T>
	 * @return
	 */
	public abstract <T> T getPlayer();

	/**
	 * Get the command sender implementation object, such as CommandSender on Bukkit.
	 *
	 * @param <T>
	 * @return
	 */
	@NonNull
	public abstract <T> T getSender();

	/**
	 * Implementation of getName() for players.
	 */
	protected abstract String getSenderName0();

	/**
	 * Return the current server His Majesty is on,
	 * null for poor consoles or weird Velocity players.
	 *
	 * @return
	 */
	public abstract MinecraftServer getServer();

	/**
	 * Returns the player's unique ID, or error if we are not a player.
	 *
	 * @return
	 */
	public abstract UUID getUniqueId();

	/**
	 * Return true if the player has RGB support
	 *
	 * @return
	 */
	public abstract boolean hasHexColorSupport();

	/**
	 * Returns true if the player has the given permission.
	 *
	 * @param permission
	 * @return
	 */
	public final boolean hasPermission(final String permission) {
		if (permission.contains("{") || permission.contains("}"))
			throw new FoException("Permission cannot contain variables: " + permission);

		return this.hasPermission0(permission);
	}

	/**
	 * Implementation of hasPermission().
	 */
	protected abstract boolean hasPermission0(String permission);

	/**
	 * Hides the boss bar from the player.
	 */
	@Override
	public final void hideBossBar(final BossBar bar) {
		if (this.isPlayer())
			BossBarTask.getInstance().hide(this, bar);
	}

	/**
	 * Implementation of hideBossBar().
	 *
	 * @param bar
	 */
	protected abstract void hideBossBar0(BossBarTask.TimedBar bar);

	/**
	 * Removes all boss bars from the player.
	 */
	public final void hideBossBars() {
		if (this.isPlayer())
			BossBarTask.getInstance().hideAll(this);
	}

	/**
	 * Returns true if the player is a command sender. For most platforms, Player
	 * is also a command sender.
	 *
	 * @return
	 */
	public abstract boolean isCommandSender();

	/**
	 * Returns true if the player is the console.
	 *
	 * @return
	 */
	public abstract boolean isConsole();

	/**
	 * Returns true if the player is a Discord sender.
	 *
	 * @return
	 */
	public abstract boolean isDiscord();

	/**
	 * Returns true if the player is a DynMap sender.
	 *
	 * @return
	 */
	public abstract boolean isDynmap();

	/**
	 * Returns true if the player is a player, false if console or command sender.
	 *
	 * @return
	 */
	public abstract boolean isPlayer();

	/**
	 * Return true if this is a player and the player is online
	 *
	 * @return
	 */
	public abstract boolean isPlayerOnline();

	/**
	 * Kicks this player, or throws error if not a player.
	 *
	 * @param reason
	 */
	public abstract void kick(FancyComponent reason);

	/**
	 * Kicks this player, or throws error if not a player.
	 *
	 * @param reason
	 */
	public final void kick(final String reason) {
		this.kick(FancyComponent.fromMiniAmpersand(reason));
	}

	/**
	 * Opens the book for the player.
	 */
	@Override
	public abstract void openBook(Book book);

	/**
	 * Opens the book for the player.
	 *
	 * @param title
	 * @param author
	 * @param pages
	 */
	public final void openBook(final FancyComponent title, final FancyComponent author, final Collection<FancyComponent> pages) {
		this.openBook(Book.book(title.toAdventure(this), author.toAdventure(this), pages.stream().map(page -> page.toAdventure(this)).collect(Collectors.toList())));
	}

	/**
	 * Opens the book for the player.
	 *
	 * @param title
	 * @param author
	 * @param pages
	 */
	public final void openBook(final String title, final String author, final String... pages) {
		this.openBook(FancyComponent.fromMiniAmpersand(title), FancyComponent.fromMiniAmpersand(author), Arrays.stream(pages).map(FancyComponent::fromMiniAmpersand).collect(Collectors.toList()));
	}

	/*
	 * Implementation of dispatchCommand() for players.
	 */
	protected abstract void performPlayerCommand0(String replacedCommand);

	/**
	 * @deprecated unsupported operation
	 */
	@Deprecated
	@Override
	public final void playSound(final Sound sound) {
		throw new UnsupportedOperationException("playSound");
	}

	/**
	 * @deprecated unsupported operation
	 */
	@Deprecated
	@Override
	public final void playSound(final Sound sound, final double x, final double y, final double z) {
		this.playSound(sound);
	}

	/**
	 * @deprecated unsupported operation
	 */
	@Deprecated
	@Override
	public final void playSound(final Sound sound, final Emitter emitter) {
		this.playSound(sound);
	}

	/**
	 * Resets the title that is being displayed to the player.
	 */
	@Override
	public abstract void resetTitle();

	/**
	 * Sends an actionbar message to the player.
	 */
	@Override
	public final void sendActionBar(final Component message) {
		this.sendActionBar(FancyComponent.fromAdventure(message));
	}

	/**
	 * Sends a message to the player.
	 *
	 * @param message
	 */
	public abstract void sendActionBar(FancyComponent message);

	/**
	 * Sends a message to the player.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @param message
	 */
	public final void sendActionBar(final String message) {
		this.sendActionBar(FancyComponent.fromMiniAmpersand(message));
	}

	/**
	 * Sends a JSON component message to the player.
	 *
	 * @param json
	 */
	public final void sendJson(final String json) {
		this.sendMessage(FancyComponent.fromAdventureJson(json, !this.hasHexColorSupport()));
	}

	/**
	 * Sends a message to the player.
	 *
	 * @param component
	 */
	@Override
	public final void sendMessage(@NonNull Component component) {
		if (!this.hasHexColorSupport())
			component = fixMultilineHoverText(component);

		this.sendMessage0(component);
	}

	protected abstract void sendMessage0(@NonNull Component component);

	private static Component fixMultilineHoverText(Component adventure) {
		if (adventure.hoverEvent() != null) {
			final HoverEvent<?> hover = adventure.hoverEvent();

			if (hover.action() == HoverEvent.Action.SHOW_TEXT) {
				final Component oldHover = (Component) hover.value();
				final String oldMini = MiniMessage.miniMessage().serialize(oldHover);

				if (oldMini.contains("\n")) {
					final String[] oldLines = oldMini.split("\n");
					Style lastStyle = null;

					for (int i = 0; i < oldLines.length; i++) {
						final String line = oldLines[i];

						if (lastStyle != null) {

							// Append decorations
							for (final Map.Entry<TextDecoration, State> entry : lastStyle.decorations().entrySet())
								if (entry.getValue() == State.TRUE)
									oldLines[i] = "<" + entry.getKey().name() + ">" + line;

							// Append color
							if (lastStyle.color() != null)
								oldLines[i] = "<" + darkenOneShade(lastStyle.color()).asHexString() + ">" + line;
						}

						lastStyle = FancyComponent.LastMessageStyleParser.parseStyle(line);
					}

					String newMiniHover = String.join("\n", oldLines);
					newMiniHover = CompChatColor.convertLegacyToMini(newMiniHover, false);

					adventure = adventure.hoverEvent(HoverEvent.showText(MiniMessage.miniMessage().deserialize(newMiniHover)));
				}
			}
		}

		if (!adventure.children().isEmpty()) {
			final List<Component> newChildren = new ArrayList<>();

			for (final Component child : adventure.children())
				newChildren.add(fixMultilineHoverText(child));

			adventure = adventure.children(newChildren);
		}

		return adventure;
	}

	private static TextColor darkenOneShade(final TextColor color) {
		final String hex = color.asHexString();

		final int r = Math.max(0, Integer.parseInt(hex.substring(1, 3), 16) - 1);
		final int g = Integer.parseInt(hex.substring(3, 5), 16);
		final int b = Integer.parseInt(hex.substring(5, 7), 16);

		return TextColor.color(r, g, b);
	}

	/**
	 * Sends a message to the player.
	 *
	 * @param component
	 */
	@Override
	public final void sendMessage(final ComponentLike component) {
		this.sendMessage(component.asComponent());
	}

	/**
	 * Sends a message to the player.
	 *
	 * If message start with {@literal <actionbar>, <toast>, <title>, <bossbar>} or {@literal <center>},
	 * it are sent interactively or centered.
	 *
	 * This method also sends the message to the player if he is having a modal conversation in Bukkit.
	 *
	 * @param component
	 */
	public final void sendMessage(final FancyComponent component) {
		this.sendMessageWithPrefix(null, component);
	}

	/**
	 * Sends a message to the player.
	 *
	 * If message start with {@literal <actionbar>, <toast>, <title>, <bossbar>} or {@literal <center>},
	 * it are sent interactively or centered.
	 *
	 * This method also sends the message to the player if he is having a modal conversation in Bukkit.
	 *
	 * @param prefix
	 * @param component
	 */
	public final void sendMessageWithPrefix(final FancyComponent prefix, FancyComponent component) {
		final String plainMessage = component.toPlain(this);

		if (plainMessage.equals("none"))
			return;

		if (prefix != null)
			component = prefix.appendPlain(" ").append(component);

		if (plainMessage.startsWith("<actionbar>"))
			this.sendActionBar(component.replaceLiteral("<actionbar>", ""));

		else if (plainMessage.startsWith("<title>")) {
			final String stripped = component.toLegacySection(this).replace("<title>", "").trim();

			if (!stripped.isEmpty()) {
				final String[] split = stripped.split("\\|");
				final String title = split[0];
				final String subtitle = split.length > 1 ? CommonCore.joinRange(1, split) : null;

				this.showTitle(0, 60, 0, title, subtitle);
			}

		} else if (plainMessage.startsWith("<bossbar>"))
			this.showBossbarTimed(component.replaceLiteral("<bossbar>", ""), 10, 1F, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);

		else if (plainMessage.startsWith("<center>")) {
			final String centeredLegacyMessage = ChatUtil.center(component.toLegacySection(this).replaceAll("\\<center\\>(\\s|)", ""));

			this.sendMessage(FancyComponent.fromSection(centeredLegacyMessage));

		} else if (!plainMessage.equals("none"))
			this.sendMessage(component.toAdventure(this));
	}

	/**
	 * Sends a MiniMessage message to the player. Legacy and mini tags are both supported.
	 *
	 * @param message
	 */
	public final void sendMiniMessage(final String message) {
		this.sendMessage(FancyComponent.fromMiniAmpersand(message));
	}

	/**
	 * Sends a plain message to the player. No colors are supported.
	 *
	 * @param message
	 */
	public final void sendPlainMessage(final String message) {
		this.sendMessage(FancyComponent.fromPlain(message));
	}

	/**
	 * Sets tab-list header and/or footer. Header or footer can be null.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 */
	@Override
	public final void sendPlayerListHeaderAndFooter(final Component header, final Component footer) {
		this.sendPlayerListHeaderAndFooter(FancyComponent.fromAdventure(header), FancyComponent.fromAdventure(footer));
	}

	/**
	 * Sets tab-list header and/or footer. Header or footer can be null.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @param header the header
	 * @param footer the footer
	 */
	public abstract void sendPlayerListHeaderAndFooter(FancyComponent header, FancyComponent footer);

	/**
	 * Sets tab-list header and/or footer. Header or footer can be null.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @param header
	 * @param footer
	 */
	public final void sendPlayerListHeaderAndFooter(final String header, final String footer) {
		this.sendPlayerListHeaderAndFooter(FancyComponent.fromMiniAmpersand(header), FancyComponent.fromMiniAmpersand(footer));
	}

	/**
	 * @deprecated use {@link #showTitle(Title)}i
	 */
	@Deprecated
	@Override
	public final <T> void sendTitlePart(final TitlePart<T> part, final T value) {
		throw new UnsupportedOperationException("sendTitlePart, use sendTitle instead");
	}

	/**
	 * Sets a temporary metadata for the player that will be lost after the player quits or server reloads.
	 *
	 * @deprecated internal use only. On Bukkit, use CompMetadata instead
	 * @param key
	 * @param value
	 */
	@Deprecated
	public abstract void setTempMetadata(String key, Object value);

	/**
	 * Shows a bossbar to the player.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 */
	@Override
	public final void showBossBar(final BossBar bar) {
		if (this.isPlayer())
			BossBarTask.getInstance().show(this, BossBarTask.TimedBar.permanent(bar));
	}

	/**
	 * Sends a bossbar to the player.
	 *
	 * @param component
	 * @param progress
	 * @param color
	 * @param overlay
	 *
	 * @return the bossbar
	 */
	public final BossBar showBossBar(final FancyComponent component, final float progress, final BossBar.Color color, final BossBar.Overlay overlay) {
		final BossBar bar = BossBar.bossBar(component.toAdventure(this), progress, color, overlay);
		this.showBossBar(bar);

		return bar;
	}

	/**
	 * Sends a bossbar to the player.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @param message
	 * @param progress
	 * @param color
	 * @param overlay
	 *
	 * @return
	 */
	public final BossBar showBossBar(final String message, final float progress, final BossBar.Color color, final BossBar.Overlay overlay) {
		return this.showBossBar(FancyComponent.fromMiniAmpersand(message), progress, color, overlay);
	}

	/**
	 * Implementation of showBossBar().
	 *
	 * @param bar
	 */
	protected abstract void showBossBar0(BossBarTask.TimedBar bar);

	/**
	 * Shows a bossbar to the player for a certain amount of seconds.
	 *
	 * @param secondsToShow
	 * @param bar
	 */
	public final void showBossbarTimed(final int secondsToShow, final BossBar bar) {
		if (this.isPlayer())
			BossBarTask.getInstance().show(this, BossBarTask.TimedBar.timed(bar, secondsToShow));
	}

	/**
	 * Sends a bossbar to the player for a certain amount of seconds.
	 *
	 * @param message
	 * @param seconds
	 * @param progress
	 * @param color
	 * @param overlay
	 *
	 * @return the bossbar
	 */
	public final BossBar showBossbarTimed(final FancyComponent message, final int seconds, final float progress, final BossBar.Color color, final BossBar.Overlay overlay) {
		final BossBar bar = BossBar.bossBar(message.toAdventure(this), progress, color, overlay);
		this.showBossbarTimed(seconds, bar);

		return bar;
	}

	/**
	 * Sends a bossbar to the player for a certain amount of seconds.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @param message
	 * @param seconds
	 * @param progress
	 * @param color
	 * @param overlay
	 *
	 * @return the bossbar
	 */
	public final BossBar showBossbarTimed(final String message, final int seconds, final float progress, final BossBar.Color color, final BossBar.Overlay overlay) {
		return this.showBossbarTimed(FancyComponent.fromMiniAmpersand(message), seconds, progress, color, overlay);
	}

	/**
	 * Sends a title to the player.
	 *
	 * @param fadeIn   how long to fade in the title (in ticks)
	 * @param stay     how long to make the title stay (in ticks)
	 * @param fadeOut  how long to fade out (in ticks)
	 * @param title    the title, will be colorized
	 * @param subtitle the subtitle, will be colorized
	 */
	public final void showTitle(final int fadeIn, final int stay, final int fadeOut, final FancyComponent title, final FancyComponent subtitle) {
		this.showTitle(Title.title(title.toAdventure(this), subtitle.toAdventure(this), Times.of(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L))));
	}

	/**
	 * Sends a title to the player.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @param fadeIn   how long to fade in the title (in ticks)
	 * @param stay     how long to make the title stay (in ticks)
	 * @param fadeOut  how long to fade out (in ticks)
	 * @param title    the title, will be colorized
	 * @param subtitle the subtitle, will be colorized
	 */
	public final void showTitle(final int fadeIn, final int stay, final int fadeOut, final String title, final String subtitle) {
		this.showTitle(fadeIn, stay, fadeOut, FancyComponent.fromMiniAmpersand(title), FancyComponent.fromMiniAmpersand(subtitle));
	}

	/**
	 * Sends a title to the player for three seconds
	 *
	 * @param title
	 * @param subtitle
	 */
	public final void showTitle(final FancyComponent title, final FancyComponent subtitle) {
		this.showTitle(20, 3 * 20, 20, title, subtitle);
	}

	/**
	 * Sends a title to the player for three seconds.
	 *
	 * Legacy and MiniMessage tags will be replaced.
	 *
	 * @param title
	 * @param subtitle
	 */
	public final void showTitle(final String title, final String subtitle) {
		this.showTitle(20, 3 * 20, 20, title, subtitle);
	}

	/**
	 * Shows a title to the player.
	 */
	@Override
	public abstract void showTitle(Title title);

	/**
	 * @deprecated unsupported operation
	 */
	@Deprecated
	@Override
	public void stopSound(final SoundStop sound) {
		throw new UnsupportedOperationException("stopSound");
	}

	/**
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		return "MinecraftPlayer{source=" + this.getSender().getClass().getSimpleName() + ", name=" + this.getName() + "}";
	}

}
