package org.mcsmp.de.fancyPluginsCore.annotation;

// TODO: Edit List

import org.mcsmp.de.fancyPluginsCore.platform.Platform;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * An annotation that instructs Foundation to automatically register the following
 * classes when the plugin starts or is reloaded:
 * <p>
 * - SimpleListener on Bukkit
 * - PacketListener
 * - ProxyListener
 * - DiscordListener
 * - SimpleCommand
 * - SimpleCommandGroup
 * - SimpleExpansion
 * - YamlConfig (we will load your config when the plugin starts and reload it properly)
 * - any class that "implements Listener" on Bukkit
 * <p>
 * These classes must be made final. Some of them must have a public no argumnets
 * constructor or be a singleton, you will be notified in the console about this.
 * <p>
 * In addition, the following classes will self-register automatically regardless
 * if you place this annotation on them or not:
 * <p>
 * - Tool (and its children such as Rocket)
 * - SimpleEnchantment
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface AutoRegister {

	/**
	 * When false, we won't print console warnings such as that registration failed
	 * because the server is outdated or lacks the necessary plugins to be hooked into
	 * (example: PacketListener needs ProtocolLib)
	 *
	 */
	boolean hideIncompatibilityWarnings() default false;

	/**
	 * When true, we won't register the class automatically.
	 * I mean that's the whole point of this annotation, right?
	 * You're wrong, I need to give haters some food.
	 *
	 * Actually, some classes are registered automatically regardless of this annotation
	 * for legacy compatibility reasons, for example, SimpleEnchantment. So if you want to prevent a class from being registered
	 * automatically, use this annotation and set this to true.
	 *
	 */
	boolean doNotAutoRegister() default false;

	/**
	 * When set, we will only register the class on the specified platforms.
	 *
	 */
	Platform.Type[] requirePlatform() default {};
}
