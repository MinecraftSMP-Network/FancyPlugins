package org.mcsmp.de.fancyPluginsCore.command.annotation;

import org.mcsmp.de.fancyPluginsCore.command.subcommand.PermsSubCommand;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation that the {@link PermsSubCommand} command scans for in your class
 * to generate permission messages.
 *
 * Example:
 * <pre>
 * public final class Permissions {
 *
 *   &#64;PermissionGroup("Permissions related to Boss commands.")
 *   public static final class Command {
 *
 *     &#64;Permission("Open Boss menu by clicking with a Boss egg or use /boss menu command.")
 *     public static final String MENU = "boss.command.menu";
 *   }
 * }
 * </pre>
 *
 * @see PermissionGroup
 * @see PermsSubCommand
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface Permission {

	String value() default "";

	boolean def() default false;
}
