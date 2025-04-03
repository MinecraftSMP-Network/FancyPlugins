package org.mcsmp.de.fancyPluginsCore.model;

import lombok.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import org.mcsmp.de.fancyPluginsCore.MinecraftVersion;
import org.mcsmp.de.fancyPluginsCore.collection.SerializedMap;
import org.mcsmp.de.fancyPluginsCore.exception.FoException;
import org.mcsmp.de.fancyPluginsCore.exception.FoScriptException;
import org.mcsmp.de.fancyPluginsCore.platform.MinecraftPlayer;
import org.mcsmp.de.fancyPluginsCore.platform.Platform;
import org.mcsmp.de.fancyPluginsCore.settings.Lang;
import org.mcsmp.de.fancyPluginsCore.utility.CommonCore;
import org.mcsmp.de.fancyPluginsCore.utility.FileUtil;
import org.mcsmp.de.fancyPluginsCore.utility.TimeUtil;
import org.mcsmp.de.fancyPluginsCore.utility.ValidCore;
import org.mcsmp.de.fancyPluginsCore.MinecraftVersion.V;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * An adaption of {@link Component} that allows for easier creation of chat
 * components with click/hover events, colors and even per-receiver condition/permissions.
 *
 * It also fixes the issue where if you place a color at the end of one component and append
 * new text to it, the new text won't have the color.
 */
public final class FancyComponent implements ConfigSerializable {

	/**
	 * Our instance of MiniMessage parser without compactor (i.e.
	 * prevents removing the second tag from \<red\>hello {player}\<red\>
	 * which makes colored placeholders revert back properly.
	 */
	private static MiniMessage MINIMESSAGE_PARSER;

	static {
		try {
			TextDecoration.class.getMethod("withState", boolean.class);

			MINIMESSAGE_PARSER = MiniMessage
					.builder()
					.tags(TagResolver.standard())
					.strict(false)
					.preProcessor(UnaryOperator.identity())
					.postProcessor(UnaryOperator.identity())
					.debug(null)
					.build();

		} catch (final Throwable t) {
			if (MinecraftVersion.equals(V.v1_16) || MinecraftVersion.equals(V.v1_17))
				CommonCore.warning("Using an older version of MiniMessage, some features might not be available.");

			else
				CommonCore.logFramed(
						"Error initializing MiniMessage. In most cases, this is",
						"caused by a third party plugin shading outdated Adventure",
						"library without relocating it, which is a bad coding",
						"practice. Some custom Spigot forks are known to do",
						"that too. See the below article for more information:",
						"https://github.com/kangarko/ChatControl/wiki/JAR-hell",
						"",
						"We will continue loading, some features might not",
						"be available.");

			try {
				MINIMESSAGE_PARSER = MiniMessage.miniMessage();

			} catch (final NoSuchMethodError tt) {
				CommonCore.throwErrorUnreported(tt,
						"Fatal error initializing legacy MiniMessage. In most cases, this is",
						"caused by a third party plugin shading outdated Adventure",
						"library without relocating it, which is a bad coding",
						"practice. Some custom Spigot forks are known to do",
						"that too. See the below article for more information:",
						"https://github.com/kangarko/ChatControl/wiki/JAR-hell");
			}
		}
	}

	/**
	 * The empty component
	 */
	private static final FancyComponent EMPTY = new FancyComponent(ConditionalComponent.fromAdventure(Component.empty()), Style.empty());

	/**
	 * The limit of characters per line for hover events in legacy versions
	 * of Minecraft where there is no automatic line wrapping.
	 */
	public static final int LEGACY_HOVER_LINE_LENGTH_LIMIT = 55;

	/**
	 * The components we are creating
	 */
	private final List<ConditionalComponent> subcomponents;

	/**
	 * The last style used, null if none
	 */
	@Getter
	private Style lastStyle = null;

	/*
	 * Create a new simple component.
	 */
	private FancyComponent(final List<ConditionalComponent> components, final Style lastStyle) {
		this.subcomponents = components;
		this.lastStyle = lastStyle;
	}

	/*
	 * Create a new simple component.
	 */
	private FancyComponent(final ConditionalComponent component, final Style lastStyle) {
		this.subcomponents = Collections.singletonList(component);
		this.lastStyle = lastStyle;
	}

	// --------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------

	/**
	 * Add a hover event.
	 *
	 * @param components
	 * @return
	 */
	public FancyComponent onHover(final List<FancyComponent> components) {
		return this.onHover(components.toArray(new FancyComponent[components.size()]));
	}

	/**
	 * Add a hover event.
	 *
	 * @param components
	 * @return
	 */
	public FancyComponent onHover(final FancyComponent... components) {
		final MinecraftPlayer receiver = null; // possibly fix in the future
		Component joined = Component.empty();

		for (int i = 0; i < components.length; i++) {
			if (MinecraftVersion.hasVersion() && MinecraftVersion.olderThan(V.v1_13)) {
				String legacy = components[i].toLegacySection(receiver);

				if (legacy.length() > LEGACY_HOVER_LINE_LENGTH_LIMIT)
					legacy = String.join("\n", CommonCore.split(legacy, LEGACY_HOVER_LINE_LENGTH_LIMIT));

				joined = joined.append(FancyComponent.fromSection(legacy).toAdventure(receiver));

			} else
				joined = joined.append(components[i].toAdventure(receiver));

			if (i < components.length - 1)
				joined = joined.append(Component.newline());
		}

		final Component finalComponent = joined;
		return this.modifyLastComponentAndReturn(component -> component.hoverEvent(finalComponent));
	}

	/**
	 * Add a hover event.
	 *
	 * @param messages
	 * @return
	 */
	public FancyComponent onHoverLegacy(final List<String> messages) {
		return this.onHoverLegacy(messages.toArray(new String[messages.size()]));
	}

	/**
	 * Add a hover event.
	 *
	 * @param messages
	 * @return
	 */
	public FancyComponent onHoverLegacy(final String... messages) {
		Component joined = Component.empty();

		for (int i = 0; i < messages.length; i++) {
			String legacy = messages[i];
			ValidCore.checkBoolean(!legacy.contains("\n"), "onHoverLegacy cannot contain new lines in the array");

			legacy = CompChatColor.convertMiniToLegacy("<gray>" + CompChatColor.translateColorCodes(legacy));

			// Receiver conditions and hover/click (unsupported) tags will be lost
			if (MinecraftVersion.hasVersion() && MinecraftVersion.olderThan(V.v1_13) && MiniMessage.miniMessage().stripTags(legacy).length() > LEGACY_HOVER_LINE_LENGTH_LIMIT)
				legacy = String.join("\n", CommonCore.split(legacy, LEGACY_HOVER_LINE_LENGTH_LIMIT));

			// This is up to 1.5-2x faster
			joined = joined.append(Component.text(legacy));
			//joined = joined.append(MINIMESSAGE_PARSER.deserialize(CompChatColor.convertLegacyToMini("<gray>" + legacy, true)));

			if (i < messages.length - 1)
				joined = joined.append(Component.newline());
		}

		final Component finalComponent = joined;

		return this.modifyLastComponentAndReturn(component -> component.hoverEvent(finalComponent));
	}

	/**
	 * Add a hover event. To put an ItemStack here, see {@link Platform#convertItemStackToHoverEvent(Object)}.
	 *
	 * @param hover
	 * @return
	 */
	public FancyComponent onHover(final HoverEventSource<?> hover) {
		return this.modifyLastComponentAndReturn(component -> component.hoverEvent(hover));
	}

	/**
	 * Add a run command event.
	 *
	 * @param text
	 * @return
	 */
	public FancyComponent onClickRunCmd(final String text) {
		return this.modifyLastComponentAndReturn(component -> component.clickEvent(ClickEvent.runCommand(text)));
	}

	/**
	 * Add a suggest command event.
	 *
	 * @param text
	 * @return
	 */
	public FancyComponent onClickSuggestCmd(final String text) {
		return this.modifyLastComponentAndReturn(component -> component.clickEvent(ClickEvent.suggestCommand(text)));
	}

	/**
	 * Open the given URL.
	 *
	 * @param url
	 * @return
	 */
	public FancyComponent onClickOpenUrl(final String url) {
		return this.modifyLastComponentAndReturn(component -> component.clickEvent(ClickEvent.openUrl(url)));
	}

	/**
	 * Open the given URL.
	 *
	 * @param url
	 * @return
	 */
	public FancyComponent onClickCopyToClipboard(final String url) {
		return this.modifyLastComponentAndReturn(component -> component.clickEvent(ClickEvent.copyToClipboard(url)));
	}

	/**
	 * Invoke FancyComponent setInsertion.
	 *
	 * @param insertion
	 * @return
	 */
	public FancyComponent onClickInsert(final String insertion) {
		return this.modifyLastComponentAndReturn(component -> component.insertion(insertion));
	}

	/**
	 * Set the view condition for this component.
	 *
	 * @param viewCondition
	 * @return
	 */
	public FancyComponent viewCondition(final String viewCondition) {
		this.subcomponents.get(this.subcomponents.size() - 1).setViewCondition(viewCondition);

		return this;
	}

	/**
	 * Set the view permission for this component.
	 *
	 * @param viewPermission
	 * @return
	 */
	public FancyComponent viewPermission(final String viewPermission) {
		this.subcomponents.get(this.subcomponents.size() - 1).setViewPermission(viewPermission);

		return this;
	}

	/**
	 * Set the view permission for this component.
	 *
	 * @param requiredVariable
	 * @return
	 */
	public FancyComponent viewRequireVariable(final RequireVariable requiredVariable) {
		this.subcomponents.get(this.subcomponents.size() - 1).setViewVariable(requiredVariable);

		return this;
	}

	/**
	 * Set the text color for this component.
	 *
	 * @param color
	 * @return
	 */
	public FancyComponent color(TextColor color) {

		// No RGB support in older versions
		if (color instanceof CompChatColor && MinecraftVersion.hasVersion() && MinecraftVersion.olderThan(V.v1_16))
			color = NamedTextColor.nearestTo(color);

		final TextColor finalColor = color;
		return this.modifyLastComponentAndReturn(component -> component.color(finalColor));
	}

	/**
	 * Set the text decoration for this component
	 *
	 * @param color
	 * @return
	 */
	public FancyComponent decoration(final TextDecoration color) {
		return this.modifyLastComponentAndReturn(component -> component.decoration(color, true));
	}

	/**
	 * Quickly replaces an object in all parts of this component, adding
	 * {} around it.
	 *
	 * @param variable the bracket variable
	 * @param value
	 * @return
	 */
	public FancyComponent replaceBracket(final String variable, final String value) {
		return this.replaceBracket(variable, fromPlain(value));
	}

	/**
	 * Quickly replaces an object in all parts of this component, adding
	 * {} around it.
	 *
	 * @param variable the bracket variable
	 * @param value
	 * @return
	 */
	public FancyComponent replaceBracket(final String variable, final FancyComponent value) {
		return this.replaceLiteral("{" + variable + "}", value);
	}

	/**
	 * Quickly replaces the literal in all parts of this component.
	 *
	 * @param variable the bracket variable
	 * @param value
	 * @return
	 */
	public FancyComponent replaceLiteral(final String variable, final String value) {
		return this.replaceLiteral(variable, fromPlain(value));
	}

	/**
	 * Quickly replaces the literal in all parts of this component.
	 *
	 * @param variable the bracket variable
	 * @param value
	 * @return
	 */
	public FancyComponent replaceLiteral(final String variable, final FancyComponent value) {
		final List<ConditionalComponent> copy = new ArrayList<>();

		for (final ConditionalComponent component : this.subcomponents) {
			final Component innerComponent = component.getComponent().replaceText(b -> b.matchLiteral(variable).replacement(value.toAdventure(null)));

			copy.add(new ConditionalComponent(innerComponent, component.getViewPermission(), component.getViewCondition(), component.getViewVariable()));
		}

		return new FancyComponent(copy, this.lastStyle);
	}

	/**
	 * Quickly replaces a pattern in all parts of this component
	 * with the given replacement function.
	 *
	 * @param pattern
	 * @param replacement
	 * @return
	 */
	public FancyComponent replaceMatch(final Pattern pattern, final BiFunction<MatchResult, TextComponent.Builder, ComponentLike> replacement) {
		final List<ConditionalComponent> copy = new ArrayList<>();

		for (final ConditionalComponent component : this.subcomponents) {
			final Component innerComponent = component.getComponent().replaceText(b -> b.match(pattern).replacement(replacement));

			copy.add(new ConditionalComponent(innerComponent, component.getViewPermission(), component.getViewCondition(), component.getViewVariable()));
		}

		return new FancyComponent(copy, this.lastStyle);
	}

	/**
	 * Quickly replaces a pattern in all parts of this component
	 * with the given replacement function.
	 *
	 * @param pattern
	 * @param replacement
	 * @return
	 */
	public FancyComponent replaceMatch(final Pattern pattern, final String replacement) {
		final List<ConditionalComponent> copy = new ArrayList<>();

		for (final ConditionalComponent component : this.subcomponents) {
			final Component innerComponent = component.getComponent().replaceText(b -> b.match(pattern).replacement(replacement));

			copy.add(new ConditionalComponent(innerComponent, component.getViewPermission(), component.getViewCondition(), component.getViewVariable()));
		}

		return new FancyComponent(copy, this.lastStyle);
	}

	/**
	 * Quickly replaces a pattern in all parts of this component
	 * with the given replacement function.
	 *
	 * @param pattern
	 * @param replacement
	 * @return
	 */
	public FancyComponent replaceMatch(final Pattern pattern, final FancyComponent replacement) {
		final List<ConditionalComponent> copy = new ArrayList<>();

		for (final ConditionalComponent component : this.subcomponents) {
			final Component innerComponent = component.getComponent().replaceText(b -> b.match(pattern).replacement(replacement.toAdventure(null)));

			copy.add(new ConditionalComponent(innerComponent, component.getViewPermission(), component.getViewCondition(), component.getViewVariable()));
		}

		return new FancyComponent(copy, this.lastStyle);
	}

	// --------------------------------------------------------------------
	// Building
	// --------------------------------------------------------------------

	/**
	 * Append a new line on the end of the component.
	 *
	 * @return
	 */
	public FancyComponent appendNewLine() {
		return this.appendPlain("\n");
	}

	/**
	 * Append plain text to the component.
	 *
	 * @param text
	 * @return
	 */
	public FancyComponent appendPlain(final String text) {
		return this.append(fromPlain(text));
	}

	/**
	 * Append text with & and § color codes to the component.
	 *
	 * @param text
	 * @return
	 */
	public FancyComponent appendAmpersand(final String text) {
		return this.append(fromAmpersand(text));
	}

	/**
	 * Append text with § color codes to the component.
	 *
	 * @param text
	 * @return
	 */
	public FancyComponent appendSection(final String text) {
		return this.append(fromSection(text));
	}

	/**
	 * Append text with &, section or MiniMessage tags to the component.
	 *
	 * @param text
	 * @return
	 */
	public FancyComponent appendMiniAmpersand(final String text) {
		return this.append(fromMiniLegacy(text, true));
	}

	/**
	 * Append text section or MiniMessage tags to the component.
	 *
	 * @param text
	 * @return
	 */
	public FancyComponent appendMiniSection(final String text) {
		return this.append(fromMiniLegacy(text, false));
	}

	/**
	 * Append text MiniMessage tags to the component.
	 *
	 * @param text
	 * @return
	 */
	public FancyComponent appendMiniNative(final String text) {
		return this.append(fromMiniNative(text));
	}

	/**
	 * Append a new component.
	 *
	 * @param newComponent
	 * @return
	 */
	public FancyComponent append(final Component newComponent) {
		return this.append(FancyComponent.fromAdventure(newComponent));
	}

	/**
	 * Append a new component.
	 *
	 * @param component
	 * @return
	 */
	public FancyComponent append(final FancyComponent component) {
		final List<ConditionalComponent> copy = new ArrayList<>();

		for (final ConditionalComponent old : this.subcomponents)
			copy.add(old);

		Style updatedLastStyle = this.lastStyle;

		if (component.lastStyle.color() != null)
			updatedLastStyle = updatedLastStyle.color(component.lastStyle.color());

		for (final Map.Entry<TextDecoration, TextDecoration.State> entry : component.lastStyle.decorations().entrySet())
			if (entry.getValue() != TextDecoration.State.NOT_SET)
				updatedLastStyle = updatedLastStyle.decoration(entry.getKey(), entry.getValue());

		for (int i = 0; i < component.subcomponents.size(); i++) {
			final ConditionalComponent subcomponent = component.subcomponents.get(i);
			Component adventure = subcomponent.getComponent();

			if (this.lastStyle != null) {
				if (this.lastStyle.color() != null && adventure.color() == null)
					adventure = adventure.color(this.lastStyle.color());

				for (final Map.Entry<TextDecoration, TextDecoration.State> entry : this.lastStyle.decorations().entrySet())
					if (entry.getValue() != TextDecoration.State.NOT_SET)
						adventure = adventure.decoration(entry.getKey(), entry.getValue());
			}

			copy.add(new ConditionalComponent(adventure, subcomponent.getViewPermission(), subcomponent.getViewCondition(), subcomponent.getViewVariable()));
		}

		return new FancyComponent(copy, updatedLastStyle);
	}

	/**
	 * Return if this component is empty.
	 *
	 * @return
	 */
	public boolean isEmpty() {
		return this == EMPTY || this.isEmpty(null);
	}

	/**
	 * Return if this component is empty for the given receiver.
	 *
	 * @param receiver
	 * @return
	 */
	public boolean isEmpty(final MinecraftPlayer receiver) {
		return this.subcomponents.isEmpty() || this.toPlain(receiver).isEmpty();
	}

	/**
	 * Return the plain colorized message combining all components into one
	 * without click/hover events. Using section & codes.
	 *
	 * @return
	 */
	public String toLegacyAmpersand() {
		return this.toLegacyAmpersand(null);
	}

	/**
	 * Return the plain colorized message combining all components into one
	 * without click/hover events for the given receiver. Using & color codes.
	 *
	 * @param receiver
	 *
	 * @return
	 */
	public String toLegacyAmpersand(final MinecraftPlayer receiver) {
		return this.toLegacy(receiver, LegacyComponentSerializer.legacyAmpersand(), true);
	}

	/**
	 * Return the plain colorized message combining all components into one
	 * without click/hover events. Using section color codes.
	 *
	 * @return
	 */
	public String toLegacySection() {
		return this.toLegacySection(null);
	}

	/**
	 * Return the plain colorized message combining all components into one
	 * without click/hover events for the given receiver. Using section color codes.
	 *
	 * @param receiver
	 *
	 * @return
	 */
	public String toLegacySection(final MinecraftPlayer receiver) {
		return this.toLegacy(receiver, LegacyComponentSerializer.legacySection(), true);
	}

	/**
	 * Return the plain colorized message combining all components into one
	 * without click/hover events for the given receiver. Using section color codes.
	 *
	 * @param receiver
	 * @param appendLastStyle true - for example {@literal <red>hello} converts to §chello§c, false excludes the color appending at the end
	 *
	 * @return
	 */
	public String toLegacySection(final MinecraftPlayer receiver, boolean appendLastStyle) {
		return this.toLegacy(receiver, LegacyComponentSerializer.legacySection(), appendLastStyle);
	}

	/*
	 * Return the plain colorized message combining all components into one
	 * without click/hover events for the given receiver.
	 */
	private String toLegacy(final MinecraftPlayer receiver, LegacyComponentSerializer serializer, boolean appendLastStyle) {
		final StringBuilder result = new StringBuilder(serializer.serialize(this.toAdventure(receiver)));

		if (this.lastStyle != null && appendLastStyle) {
			if (this.lastStyle.color() != null) {
				final CompChatColor comp = CompChatColor.fromTextColor(this.lastStyle.color());

				result.append(comp.isHex() ? comp.toClosestLegacy() : comp.toString());
			}

			for (final Map.Entry<TextDecoration, TextDecoration.State> entry : this.lastStyle.decorations().entrySet())
				if (entry.getValue() == TextDecoration.State.TRUE)
					result.append(CompChatColor.fromTextDecoration(entry.getKey()));

		}

		return result.toString();
	}

	/**
	 * Return the MiniMessage representation of the component.
	 *
	 * @deprecated a bug in MiniMessage converts gradients to appending the color before each letter, breaking placeholders
	 *             it is advised to replace placeholders on the raw input first, see {@link Lang#plain(String)}
	 * @return
	 */
	@Deprecated
	public String toMini() {
		return this.toMini(null);
	}

	/**
	 * Return the MiniMessage representation of the component for the given receiver.
	 *
	 * @deprecated a bug in MiniMessage converts gradients to appending the color before each letter, breaking placeholders
	 *             it is advised to replace placeholders on the raw input first, see {@link Lang#plain(String)}
	 * @param receiver
	 * @return
	 */
	@Deprecated
	public String toMini(final MinecraftPlayer receiver) {
		try {
			return FancyComponent.MINIMESSAGE_PARSER.serialize(this.toAdventure(receiver));

		} catch (final Throwable t) {
			final String mini = this.toPlain(receiver);
			final String stripped = mini.replaceAll("(<hover:show_item:[^:>]+):[^>]*?'>", "$1'>");

			CommonCore.log(
					"Adventure failed to convert component to MiniMessage. Will return stripped!",
					"This is a Paper bug (especially if you the error says 'There is no data holder'",
					"and your item has a hover event - in that case, update Paper.",
					"If you are running the latest Paper, open an issue with their team.",
					"",
					"Mini: " + mini,
					"Stripped: " + stripped);

			t.printStackTrace(); // do not report to sentry, likely not our fault
			CommonCore.log("(Do not report the above stacktrace to us, read the log above first)");

			return fromSection(this.toLegacySection(receiver)).toMini(receiver);
		}
	}

	/**
	 * Return the plain colorless message combining all components into one
	 * without click/hover events.
	 *
	 * This effectivelly removes all & and § colors as well as MiniMessage tags.
	 *
	 * @return
	 */
	public String toPlain() {
		return this.toPlain(null);
	}

	/**
	 * Return the plain colorless message combining all components into one
	 * without click/hover events for the given receiver.
	 *
	 * This effectivelly removes all & and § colors as well as MiniMessage tags.
	 *
	 * @param receiver
	 * @return
	 */
	public String toPlain(final MinecraftPlayer receiver) {
		return PlainTextComponentSerializer.plainText().serialize(this.toAdventure(receiver));
	}

	/**
	 * Returns the JSON representation of the component.
	 *
	 * @param legacy
	 * @return
	 */
	public String toAdventureJson(final boolean legacy) {
		return this.toAdventureJson(null, legacy);
	}

	/**
	 * Returns the JSON representation of the component for the given receiver.
	 *
	 * @param receiver
	 * @param legacy
	 *
	 * @return
	 */
	public String toAdventureJson(final MinecraftPlayer receiver, final boolean legacy) {
		try {
			return (legacy ? GsonComponentSerializer.colorDownsamplingGson() : GsonComponentSerializer.gson()).serialize(this.toAdventure(receiver));

		} catch (final Throwable t) {
			final String mini = this.toMini(receiver);
			final String stripped = mini.replaceAll("(<hover:show_item:[^:>]+):[^>]*?'>", "$1'>");

			CommonCore.log(
					"Adventure failed to convert component to JSON. Will return stripped!",
					"This is a Paper bug (especially if you the error says 'There is no data holder'",
					"and your item has a hover event - in that case, update Paper.",
					"If you are running the latest Paper, open an issue with their team.",
					"",
					"Mini: " + mini,
					"Stripped: " + stripped);

			t.printStackTrace(); // do not report to sentry, likely not our fault
			CommonCore.log("(Do not report the above stacktrace to us, read the log above first)");

			return fromSection(this.toLegacySection(receiver)).toAdventureJson(receiver, legacy);
		}
	}

	/**
	 * Convert into Adventure component.
	 *
	 * @return
	 */
	public Component toAdventure() {
		return this.toAdventure(null);
	}

	/**
	 * Convert into Adventure component, executing viewCondition and viewPermission for the given receiver.
	 *
	 * @param receiver the given receiver, can be null
	 * @return
	 */
	public Component toAdventure(final MinecraftPlayer receiver) {
		final List<Component> children = new ArrayList<>();

		for (final ConditionalComponent part : this.subcomponents) {
			Component builtPart = null;

			try {
				builtPart = part.build(receiver);

			} catch (final FoScriptException ex) {
				ex.printStackTrace();
			}

			// If sender condition or permission does not match, we return null
			if (builtPart != null)
				children.add(builtPart);
		}

		// Cannot use TextComponent#testOfChildren as 1.16 does not support this
		TextComponent text = Component.empty();
		text = text.children(children);

		return text;
	}

	/*
	 * Helper method to modify the last component.
	 */
	protected FancyComponent modifyLastComponentAndReturn(final Function<Component, Component> editor) {
		final List<ConditionalComponent> copy = new ArrayList<>();

		for (int i = 0; i < this.subcomponents.size(); i++) {
			ConditionalComponent component = this.subcomponents.get(i);

			if (i == this.subcomponents.size() - 1)
				component = new ConditionalComponent(editor.apply(component.getComponent()), component.getViewPermission(), component.getViewCondition(), component.getViewVariable());

			copy.add(component);
		}

		return new FancyComponent(copy, this.lastStyle);
	}

	/**
	 * @see ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		return SerializedMap.fromArray(
				"Components", this.subcomponents,
				"Last_Style", this.lastStyle);
	}

	/**
	 * Return if this component is equal to another component or
	 * if it is a string, return if the plain representation is equal.
	 *
	 * @param obj
	 * @return
	 */
	@Override
	public boolean equals(final Object obj) {
		if (obj == this)
			return true;

		if (obj instanceof String)
			return this.toPlain(null).equals(obj);

		if (obj instanceof FancyComponent) {
			final FancyComponent other = (FancyComponent) obj;

			return this.toMini(null).equals(other.toMini(null));
		}

		return false;
	}

	/**
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		throw new FoException("FancyComponent#toString() is unsupported, use toMini(), toLegacy() or toPlain() as needed");
	}

	// --------------------------------------------------------------------
	// Static
	// --------------------------------------------------------------------

	/**
	 * Create a new empty component.
	 *
	 * @return
	 */
	public static FancyComponent empty() {
		return EMPTY;
	}

	/**
	 * Create a new component from the given message.
	 *
	 * Replaces & and section color codes and MiniMessage tags.
	 *
	 * @param message
	 * @return
	 */
	public static FancyComponent fromMiniAmpersand(final String message) {
		return fromMiniLegacy(message, true);
	}

	/**
	 * Create a new component from the given message.
	 *
	 * Replaces section color codes and MiniMessage tags.
	 *
	 * @param message
	 * @return
	 */
	public static FancyComponent fromMiniSection(final String message) {
		return fromMiniLegacy(message, false);
	}

	/*
	 * Create a new component from the given message.
	 */
	private static FancyComponent fromMiniLegacy(String message, final boolean ampersand) {
		if (message == null)
			return FancyComponent.empty();

		if (" ".equals(message))
			return fromPlain(" ");

		// Replace legacy & color codes
		message = CompChatColor.convertLegacyToMini(message, ampersand);

		return fromMiniNative(message);
	}

	/**
	 * Create a new component from the given message. This will throw error if legacsy tags are found.
	 *
	 * @param message
	 * @return
	 */
	public static FancyComponent fromMiniNative(String message) {
		if (message == null)
			return FancyComponent.empty();

		if (" ".equals(message))
			return fromPlain(" ");

		Component mini;

		try {
			// Correct MiniMessage potentially dangerous behavior where multiple backslashes will
			// make the variable parse so we slash it to one.
			message = message.replaceAll("(\\\\){2,}(?=<)", "\\\\");

			// See resetColors() below for explainer
			mini = MINIMESSAGE_PARSER.deserialize(message.replace("<reset>", "<#180f0d>").replace("\\n", "\n"));

		} catch (final Throwable t) {
			if (MinecraftVersion.equals(V.v1_16))
				CommonCore.throwErrorUnreported(t, "Error parsing mini message tags in: " + message);
			else
				CommonCore.throwError(t, "Error parsing mini message tags in: " + message);

			return null;
		}

		mini = resetColors(mini);

		return new FancyComponent(ConditionalComponent.fromAdventure(mini), LastMessageStyleParser.parseStyle(message));
	}

	// Explanation for this nonsense:
	// Apparently serialization performs some trunctating to put everything on one line
	// When appending a FancyComponent to this component, this leads to a bug where color/deco is overflown
	// so we just force the component to split using a fake color and then set all decors/colors to "resetting" state manually
	// ... contributions welcome :)
	private static Component resetColors(Component component) {
		final List<Component> childrenCopy = new ArrayList<>();

		for (Component child : component.children()) {
			if (child.color() != null && child.color().equals(TextColor.color(0x180f0d))) {
				child = child.color(NamedTextColor.WHITE);

				for (final TextDecoration decoration : TextDecoration.values())
					child = child.decoration(decoration, TextDecoration.State.FALSE);
			}

			child = resetColors(child);
			childrenCopy.add(child);
		}

		component = component.children(childrenCopy);
		return component;
	}

	/**
	 * Create a new component from the given message.
	 *
	 * Replaces & and § color codes.
	 *
	 * @param legacyText
	 * @return
	 */
	public static FancyComponent fromAmpersand(@NonNull String legacyText) {
		legacyText = legacyText.replaceAll("(?i)&([0-9A-FK-OR])", "§$1");

		return fromSection(legacyText);
	}

	/**
	 * Create a new component from the given message.
	 *
	 * Replaces § color codes.
	 *
	 * @param legacyText
	 * @return
	 */
	public static FancyComponent fromSection(@NonNull final String legacyText) {
		final Component mini = LegacyComponentSerializer.legacySection().deserialize(legacyText);
		final String withMiniTags = CompChatColor.convertLegacyToMini(legacyText, false);

		return new FancyComponent(ConditionalComponent.fromAdventure(mini), LastMessageStyleParser.parseStyle(withMiniTags));
	}


	/**
	 * Create a new component from JSON.
	 *
	 * @param json
	 * @param legacy
	 *
	 * @return
	 */
	public static FancyComponent fromAdventureJson(@NonNull final String json, final boolean legacy) {
		try {
			return fromAdventure((legacy ? GsonComponentSerializer.colorDownsamplingGson() : GsonComponentSerializer.gson()).deserialize(json));

		} catch (final Throwable t) {
			CommonCore.logFramed(
					"Failed to parse JSON into FancyComponent",
					"Legacy: " + legacy,
					"Json: " + json);

			if (json.length() > 200) {
				final String fileName = "malformed-json-" + TimeUtil.getFormattedDate() + ".txt";

				FileUtil.write(fileName, json);
				CommonCore.log("Saved malformed JSON to " + fileName + " - please report this to the plugin developer.");
			}

			t.printStackTrace();

			return FancyComponent.empty();
		}
	}

	/**
	 * Create a new component from adventure component.
	 *
	 * @param component
	 * @return
	 */
	public static FancyComponent fromAdventure(@NonNull final Component component) {
		return new FancyComponent(ConditionalComponent.fromAdventure(component), Style.empty());
	}

	/**
	 * Create a new component from plain text.
	 *
	 * @param plainText
	 * @return
	 */
	public static FancyComponent fromPlain(@NonNull final String plainText) {
		return new FancyComponent(ConditionalComponent.fromPlain(plainText), Style.empty());
	}

	/**
	 * Turns the given map into a component.
	 *
	 * @param map
	 * @return
	 */
	public static FancyComponent deserialize(final SerializedMap map) {
		final List<ConditionalComponent> components = map.getList("Components", ConditionalComponent.class);
		final FancyComponent component = new FancyComponent(components, map.get("Last_Style", Style.class));

		return component;
	}

	/**
	 * Join multiple components into one by appending them and adding new lines.
	 *
	 * @param components
	 * @return
	 */
	public static FancyComponent join(final Collection<FancyComponent> components) {
		return join(components.toArray(new FancyComponent[components.size()]));
	}

	/**
	 * Join multiple components into one by appending them and adding new lines.
	 *
	 * @param separator
	 * @param components
	 * @return
	 */
	public static FancyComponent join(final FancyComponent separator, final Collection<FancyComponent> components) {
		return join(separator, components.toArray(new FancyComponent[components.size()]));
	}

	/**
	 * Join multiple components into one by appending them and adding new lines.
	 *
	 * @param components
	 * @return
	 */
	public static FancyComponent join(final FancyComponent... components) {
		return join(FancyComponent.fromMiniNative("<br><reset><white>"), components);
	}

	/**
	 * Join multiple components into one by appending them and adding new lines.
	 *
	 * @param separator
	 * @param components
	 * @return
	 */
	public static FancyComponent join(FancyComponent separator, final FancyComponent... components) {
		FancyComponent main = empty();

		for (int i = 0; i < components.length; i++) {
			main = main.append(components[i]);

			if (i < components.length - 1)
				main = main.append(separator);
		}

		return main;
	}

	/**
	 * Serialize the component to MiniMessage format.
	 *
	 * @param component
	 * @return
	 */
	public static String serializeAdventureToMini(Component component) {
		try {
			return MINIMESSAGE_PARSER.serialize(component);

		} catch (final Throwable t) {
			final String plain = PlainTextComponentSerializer.plainText().serialize(component);

			CommonCore.log(
					"Adventure failed to convert Component to MiniMessage. Will return as plain!",
					"This is a Paper bug (especially if you the error says No data converted",
					"and your item has a hover event - in that case, update Paper.",
					"If you are running the latest Paper, open an issue with their team.",
					"",
					"Plain: " + plain,
					"Error: " + t.getMessage());

			t.printStackTrace(); // do not report to sentry, likely not our fault
			CommonCore.log("(Do not report the above stacktrace to us, read the log above first)");

			return plain;
		}
	}

	/**
	 * Deserialize the component from MiniMessage format.
	 *
	 * @param text
	 * @return
	 */
	public static Component deserializeMiniToAdventure(String text) {
		try {
			return MINIMESSAGE_PARSER.deserialize(text);

		} catch (final Throwable t) {
			CommonCore.log(
					"Adventure failed to convert MiniMessage to Component. Will return as literal!",
					"This is a Paper bug (especially if you the error says No data converted",
					"and your item has a hover event - in that case, update Paper.",
					"If you are running the latest Paper, open an issue with their team.",
					"",
					"Text: " + text,
					"Error: " + t.getMessage());

			t.printStackTrace(); // do not report to sentry, likely not our fault
			CommonCore.log("(Do not report the above stacktrace to us, read the log above first)");

			return Component.text(text);
		}
	}

	/**
	 * Strip all tags from the message.
	 *
	 * @param message
	 * @return
	 */
	public static String stripMiniMessageTags(String message) {
		return MINIMESSAGE_PARSER.stripTags(message);
	}

	// --------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------

	/**
	 * Helper class to store a component with view condition and view permission.
	 */
	@Setter
	@Getter
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	static final class ConditionalComponent implements ConfigSerializable {

		/**
		 * The adventure component.
		 */
		private final Component component;

		/**
		 * The view permission executed for receivers.
		 */
		private String viewPermission;

		/**
		 * The view condition executed for receivers.
		 */
		private String viewCondition;

		/**
		 * The view condition executed for receivers.
		 */
		private RequireVariable viewVariable;

		/**
		 * @see ConfigSerializable#serialize()
		 */
		@Override
		public SerializedMap serialize() {
			final SerializedMap map = new SerializedMap();

			map.put("Component", FancyComponent.MINIMESSAGE_PARSER.serialize(this.component));
			map.putIfExists("Permission", this.viewPermission);
			map.putIfExists("Condition", this.viewCondition);

			if (this.viewVariable != null)
				map.putIfExists("Variable", this.viewVariable.getVariable() + " " + this.viewVariable.getRequiredValue());

			return map;
		}

		/**
		 * Turn the map into a conditional component.
		 *
		 * @param map
		 * @return
		 */
		public static ConditionalComponent deserialize(final SerializedMap map) {
			final Component component = FancyComponent.MINIMESSAGE_PARSER.deserialize(CompChatColor.convertLegacyToMini(map.getString("Component"), false));
			final ConditionalComponent part = new ConditionalComponent(component);

			part.viewPermission = map.getString("Permission");
			part.viewCondition = map.getString("Condition");
			part.viewVariable = map.containsKey("Variable") ? RequireVariable.fromLine(map.getString("Variable")) : null;

			return part;
		}

		/*
		 * Build the component for the given receiver.
		 */
		private Component build(final MinecraftPlayer receiver) {
			if (this.viewPermission != null && !this.viewPermission.isEmpty() && (receiver == null || !receiver.hasPermission(this.viewPermission)))
				return null;

			if (this.viewCondition != null && !this.viewCondition.isEmpty()) {
				if (receiver == null)
					return null;

				final String replacedCondition = Variables.builder(receiver).replaceLegacy(this.viewCondition);

				try {
					final Object result = JavaScriptExecutor.run(replacedCondition, receiver);

					if (result != null) {
						ValidCore.checkBoolean(result instanceof Boolean, "View condition must return Boolean not " + (result == null ? "null" : result.getClass()) + " for component: " + this);

						if (!((boolean) result))
							return null;
					}

				} catch (final FoScriptException ex) {
					CommonCore.logFramed(
							"Failed parsing view condition for component!",
							"",
							"The view condition must be a JavaScript code that returns a boolean!",
							"Raw code: " + this.viewCondition,
							"Evaluated code: " + replacedCondition,
							"Line: " + ex.getErrorLine(),
							"Error: " + ex.getMessage());

					throw ex;
				}
			}

			if (this.viewVariable != null && receiver != null)
				if (!this.viewVariable.matches(value -> Variables.builder(receiver).replaceLegacy(value)))
					return null;

			return this.component;
		}

		@Override
		public String toString() {
			return this.serialize().toStringFormatted();
		}

		/**
		 * Create a new conditional component from adventure component.
		 *
		 * @param component
		 * @return
		 */
		static ConditionalComponent fromAdventure(final Component component) {
			return new ConditionalComponent(component);
		}

		/**
		 * Create a new conditional component from plain text.
		 *
		 * @param plainText
		 * @return
		 */
		static ConditionalComponent fromPlain(final String plainText) {
			return new ConditionalComponent(Component.text(plainText));
		}
	}

	/**
	 * Helps to resolve last message style from MiniMessage tags.
	 */
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class LastMessageStyleParser {

		/**
		 * The pattern for RGB color codes.
		 */
		private final static Pattern RGB_PATTERN = Pattern.compile("<#[0-9a-fA-F]{6}>");

		/**
		 * The last color.
		 */
		private TextColor lastColor;

		/**
		 * The last decorations.
		 */
		private final Map<TextDecoration, TextDecoration.State> lastDecorations = new EnumMap<>(TextDecoration.class);

		/*
		 * Parse the given message.
		 */
		private void parseMessage(final String message) {
			this.lastColor = null;
			this.lastDecorations.clear();

			final StringBuilder cleanedMessage = new StringBuilder();
			final int length = message.length();
			boolean insideTag = false;
			final StringBuilder currentTag = new StringBuilder();

			for (int i = 0; i < length; i++) {
				final char ch = message.charAt(i);

				if (ch == '<') {
					insideTag = true;
					currentTag.setLength(0);

				} else if (ch == '>' && insideTag) {
					// Tag content without < and >
					final String tagContent = currentTag.toString();

					if (tagContent.equalsIgnoreCase("reset") || tagContent.equalsIgnoreCase("r")) {
						this.lastColor = NamedTextColor.WHITE;
						this.lastDecorations.clear();

						// Add all decorations with FALSE state
						for (final TextDecoration decoration : TextDecoration.values())
							this.lastDecorations.put(decoration, TextDecoration.State.FALSE);

					} else if (tagContent.startsWith("/")) {
						// It's a closing tag, remove the '/'
						String closingTagName = tagContent.substring(1);

						closingTagName = closingTagName.toLowerCase(Locale.ROOT);
						closingTagName = "<" + closingTagName + ">";

						if (CompChatColor.MINI_TO_COLOR.containsKey(closingTagName) || RGB_PATTERN.matcher(closingTagName).matches())
							// Closing a color tag
							this.lastColor = null;

						else if (CompChatColor.MINI_TO_DECORATION.containsKey(closingTagName))
							// Closing a decoration tag
							this.lastDecorations.remove(CompChatColor.MINI_TO_DECORATION.get(closingTagName));

						// else ignore unknown closing tags

					} else {
						// Handle opening tags
						String openingTagName = tagContent.toLowerCase(Locale.ROOT);
						openingTagName = "<" + openingTagName + ">";

						if (CompChatColor.MINI_TO_COLOR.containsKey(openingTagName))
							this.lastColor = CompChatColor.MINI_TO_COLOR.get(openingTagName);

						else if (CompChatColor.MINI_TO_DECORATION.containsKey(openingTagName))
							this.lastDecorations.put(CompChatColor.MINI_TO_DECORATION.get(openingTagName), TextDecoration.State.TRUE);

						else if (RGB_PATTERN.matcher(openingTagName).matches())
							this.lastColor = TextColor.fromHexString(openingTagName.substring(1, openingTagName.length() - 1));

						// else ignore unknown tags
					}

					insideTag = false;

				} else if (insideTag)

					// Building tag content
					currentTag.append(ch);

				else

					// Normal text
					cleanedMessage.append(ch);
			}
		}

		/**
		 * Parse the last message style from the given message.
		 *
		 * @param message the message to parse
		 * @return the last message style
		 */
		public static Style parseStyle(final String message) {
			final LastMessageStyleParser parser = new LastMessageStyleParser();

			parser.parseMessage(message);

			return Style.style(parser.lastColor).decorations(parser.lastDecorations);
		}
	}
}
