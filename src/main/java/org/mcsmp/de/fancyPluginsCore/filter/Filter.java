package org.mcsmp.de.fancyPluginsCore.filter;

import lombok.Getter;
import org.mcsmp.de.fancyPluginsCore.database.Row;
import org.mcsmp.de.fancyPluginsCore.database.Table;
import org.mcsmp.de.fancyPluginsCore.platform.MinecraftPlayer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Represents a goddamn filter
 */
@Getter
public abstract class Filter {

	/**
	 * The registered filters
	 */
	private static final Map<String, Filter> registeredFilters = new HashMap<>();

	/**
	 * The name of the filter
	 */
	private final String identifier;

	/**
	 * Create a new filter
	 *
	 * @param identifier
	 */
	protected Filter(final String identifier) {
		this.identifier = identifier;
	}

	/**
	 * Return true if the given table supports this filter.
	 * Return true for universal filters.
	 *
	 * @param table
	 * @return
	 */
	public abstract boolean isApplicable(Table table);

	/**
	 * Return a list of usages shown in /protect log tableType ?"
	 *
	 * @return
	 */
	public abstract String[] getUsages();

	/**
	 * Check the value of the filter and return if it is valid.
	 * The value will be stripped off of identifier, i.e. gamemode:creative -> creative
	 *
	 * @param audience
	 * @param value
	 * @return
	 */
	public abstract boolean validate(MinecraftPlayer audience, String value);

	/**
	 * Return true if the given row can be displayed when this filter is on.
	 *
	 * @param row
	 * @return
	 */
	public abstract boolean canDisplay(Row row);

	/**
	 * Return the tab complete for the filter, i.e. creative, survival, adventure, spectator for gamemode filter.
	 *
	 * @param audience
	 * @return
	 */
	public Collection<String> tabComplete(final MinecraftPlayer audience) {
		return null;
	}

	/**
	 * Return the filter by its name
	 *
	 * @param name
	 * @return
	 */
	public static Filter getByName(final String name) {
		return registeredFilters.get(name);
	}

	/**
	 * Register a new filter
	 *
	 * @param identifier
	 * @param filter
	 */
	public static void register(final String identifier, final Filter filter) {
		registeredFilters.put(identifier, filter);
	}

	/**
	 * Return all registered filter names
	 *
	 * @return
	 */
	public static Set<String> getFilterNames() {
		return Collections.unmodifiableSet(registeredFilters.keySet());
	}

	/**
	 * Return all registered filters
	 *
	 * @return
	 */
	public static Collection<Filter> getFilters() {
		return Collections.unmodifiableCollection(registeredFilters.values());
	}

	/**
	 * Helper method to parse a date from the given value.
	 *
	 * @param value
	 * @return
	 */
	protected static Date parseDate(String value) {
		value = value.toLowerCase();

		SimpleDateFormat dateFormat = null;
		Date startDate = null;

		if (value.matches("\\d{2}-\\d{2}-\\d{4}"))
			dateFormat = new SimpleDateFormat("dd-MM-yyyy");

		else if (value.matches("\\d{2}-\\d{2}-\\d{4}_\\d{2}-\\d{2}"))
			dateFormat = new SimpleDateFormat("dd-MM-yyyy_HH-mm");

		else if (value.matches("\\d{2}-\\d{2}"))
			dateFormat = new SimpleDateFormat("HH-mm");

		else
			return null;

		dateFormat.setLenient(false);

		try {
			startDate = dateFormat.parse(value);

		} catch (final ParseException e) {
			return null;
		}

		return startDate;
	}
}
