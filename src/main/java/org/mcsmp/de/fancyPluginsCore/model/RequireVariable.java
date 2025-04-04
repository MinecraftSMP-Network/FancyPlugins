package org.mcsmp.de.fancyPluginsCore.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.mcsmp.de.fancyPluginsCore.exception.FoException;

import java.util.function.Function;

/**
 * Represents a variable that must match a certain value
 * for example in ChatControl's rules this is used as "require variable {player_gamemode} CREATIVE"
 * and so on.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequireVariable implements ConfigStringSerializable {

	/**
	 * The variable to check
	 */
	private final String variable;

	/**
	 * The required value, if starts with !, it means it should not match
	 */
	private final String requiredValue;

	/**
	 * If the required value is negated (starts with !)
	 */
	private final boolean negated;

	/**
	 * Check if the variable matches the required value
	 *
	 * @param replacer the function to replace the variable with its actual value, parse variables here
	 * @return
	 */
	public boolean matches(final Function<String, String> replacer) {
		String result = replacer.apply(this.variable);

		if ("yes".equals(result) || "1".equals(result))
			result = "true";

		else if ("no".equals(result) || "0".equals(result) || "".equals(result))
			result = "false";

		if (!this.negated && !result.equalsIgnoreCase(this.requiredValue))
			return false;

		if (this.negated && result.equalsIgnoreCase(this.requiredValue))
			return false;

		return true;
	}

	@Override
	public String serialize() {
		return this.variable + " " + (this.negated ? "!" : "") + this.requiredValue;
	}

	/**
	 * Parse the given line into a new RequireVariable
	 *
	 * @param line
	 * @return
	 */
	public static RequireVariable fromLine(final String line) {
		final String[] split = line.split(" ");

		if (split.length != 1 && split.length != 2)
			throw new FoException("Invalid require variable syntax - it must be in the form '<variable> <true/false>' or '<variable>' (to match if it is true), got: '" + line + "'", false);

		else {
			final String variable = split[0];
			final String requiredValue = split.length == 2 ? split[1] : "true";

			return from(variable, requiredValue);
		}
	}

	/**
	 * Create a new RequireVariable from the given variable and required value
	 *
	 * @param variable
	 * @param requiredValue
	 * @return
	 */
	public static RequireVariable from(final String variable, String requiredValue) {
		if ("yes".equals(requiredValue) || "1".equals(requiredValue))
			requiredValue = "true";

		else if ("no".equals(requiredValue) || "0".equals(requiredValue))
			requiredValue = "false";

		final boolean negated = requiredValue.charAt(0) == '!';

		return new RequireVariable(variable, negated ? requiredValue.substring(1) : requiredValue, negated);
	}
}
