package org.mcsmp.de.fancyPluginsCore.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.mcsmp.de.fancyPluginsCore.command.subcommand.PermsSubCommand;
import org.mcsmp.de.fancyPluginsCore.platform.MinecraftPlayer;
import org.mcsmp.de.fancyPluginsCore.platform.Platform;
import org.mcsmp.de.fancyPluginsCore.settings.Lang;
import org.mcsmp.de.fancyPluginsCore.utility.ChatUtil;
import org.mcsmp.de.fancyPluginsCore.utility.CommonCore;

import java.util.*;

/**
 * A draft API for enumerating chat messages into pages.
 *
 * See {@link PermsSubCommand} for an early implementation.
 */
@Getter
@RequiredArgsConstructor
public final class ChatPaginator {

	/**
	 * This is the height that will fill all chat lines (20)
	 * if you use {@link #setFoundationHeader(String)}.
	 *
	 * It is 17 because our header is 3 lines wide.
	 */
	public static final int FOUNDATION_HEIGHT = 15;

	/**
	 * How many lines per page? Maximum on screen is 20 minus header and footer.
	 */
	private final int linesPerPage;

	/**
	 * The header included on every page.
	 */
	private final List<FancyComponent> header = new ArrayList<>();

	/**
	 * The pages with their content.
	 */
	private final Map<Integer, List<FancyComponent>> pages = new LinkedHashMap<>();

	/**
	 * The footer included on every page.
	 */
	private final List<FancyComponent> footer = new ArrayList<>();

	/**
	 * Construct chat pagination taking the entire visible chat portion when chat is maximized.
	 */
	public ChatPaginator() {
		this(FOUNDATION_HEIGHT);
	}

	/**
	 * Sets the standard Foundation header used across plugins.
	 * ----------------
	 * \<center\>title
	 * ---------------
	 *
	 * @param title
	 * @return
	 */
	public ChatPaginator setFoundationHeader(final String title) {
		return this.setHeader("&8&m" + ChatUtil.center("&r " + title + " &8&m", Lang.legacy("command-header-center-letter").charAt(0), Integer.parseInt(Lang.legacy("command-header-center-padding"))));
	}

	/**
	 * Set the content type
	 *
	 * @param components
	 * @return
	 */
	public ChatPaginator setHeader(final FancyComponent... components) {
		Collections.addAll(this.header, components);

		return this;
	}

	/**
	 * Set the content type
	 *
	 * @param messages
	 * @return
	 */
	public ChatPaginator setHeader(final String... messages) {
		for (final String message : messages)
			this.header.add(FancyComponent.fromMiniAmpersand(message));

		return this;
	}

	/**
	 * Set the content type
	 *
	 * @param messages
	 * @return
	 */
	public ChatPaginator setPages(final String... messages) {
		final List<FancyComponent> pages = new ArrayList<>();

		for (final String message : messages)
			pages.add(FancyComponent.fromMiniAmpersand(message));

		return this.setPages(pages.toArray(new FancyComponent[pages.size()]));
	}

	/**
	 * Set the content type
	 *
	 * @param components
	 * @return
	 */
	public ChatPaginator setPages(final FancyComponent... components) {
		this.pages.clear();
		this.pages.putAll(CommonCore.fillPages(this.linesPerPage, Arrays.asList(components)));

		return this;
	}

	/**
	 * Set the content type
	 *
	 * @param components
	 * @return
	 */
	public ChatPaginator setPages(final List<FancyComponent> components) {
		this.pages.clear();
		this.pages.putAll(CommonCore.fillPages(this.linesPerPage, components));

		return this;
	}

	/**
	 * Set the content type
	 *
	 * @param messages
	 * @return
	 */
	public ChatPaginator setFooter(final String... messages) {
		for (final String message : messages)
			this.footer.add(FancyComponent.fromMiniAmpersand(message));

		return this;
	}

	/**
	 * Set the content type
	 *
	 * @param components
	 * @return
	 */
	public ChatPaginator setFooter(final FancyComponent... components) {
		Collections.addAll(this.footer, components);

		return this;
	}

	/**
	 * Start showing the first page to the sender
	 *
	 * @param audience
	 */
	public void send(final MinecraftPlayer audience) {
		this.send(audience, 1);
	}

	/**
	 * Show the given page to the sender, either paginated or a full dumb when this is a console
	 *
	 * @param audience
	 * @param page
	 */
	public void send(final MinecraftPlayer audience, final int page) {
		if (audience.isPlayer()) {
			audience.setTempMetadata(Platform.getPlugin().getName() + "_Pages", this);
			audience.dispatchCommand("/#flp " + page);

		} else {
			for (final FancyComponent component : this.header)
				audience.sendMessage(component);

			int amount = 1;

			for (final List<? extends FancyComponent> components : this.pages.values())
				for (final FancyComponent component : components)
					audience.sendMessage(component.replaceBracket("count", String.valueOf(amount++)));

			for (final FancyComponent component : this.footer)
				audience.sendMessage(component);
		}
	}
}
