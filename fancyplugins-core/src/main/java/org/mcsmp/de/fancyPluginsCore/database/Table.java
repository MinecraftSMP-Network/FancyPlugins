package org.mcsmp.de.fancyPluginsCore.database;

import org.mcsmp.de.fancyPluginsCore.exception.InvalidRowException;
import org.mcsmp.de.fancyPluginsCore.exception.ReflectionException;
import org.mcsmp.de.fancyPluginsCore.utility.CommonCore;
import org.mcsmp.de.fancyPluginsCore.utility.ReflectionUtil;
import org.mcsmp.de.fancyPluginsCore.utility.ValidCore;

import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.List;

/**
 * Represents a table in the database.
 */
public interface Table {

	/**
	 * Return the name of this table without the plugin's prefix
	 *
	 * @return
	 */
	String getKey();

	/**
	 * Return the full table name prefixed with the name of this plugin
	 *
	 * @return
	 */
	String getName();

	/**
	 * Return the row class for this table.
	 *
	 * @return
	 */
	Class<? extends Row> getRowClass();

	/**
	 * Return the database this table belongs to.
	 *
	 * @return
	 */
	FancyDatabase getDatabase();

	/**
	 * Return the row with the given id.
	 *
	 * @param <T>
	 * @param id
	 * @return
	 */
	default <T extends Row> T getRow(final int id) {
		return this.getDatabase().getRow(this, id);
	}

	/**
	 * Return all rows in this table.
	 *
	 * @param <T>
	 * @return
	 */
	default <T extends Row> List<T> getRows() {
		return this.getDatabase().getRows(this);
	}

	/**
	 * Called when creating the table.
	 *
	 * @param creator
	 */
	void onTableCreate(FancyDatabase.TableCreator creator);

	/**
	 * Create a new row for this table.
	 *
	 * @param <T>
	 * @param resultSet
	 * @return the row or null
	 * @throws SQLException
	 */
	default <T extends Row> T createRowOrNull(final FancyResultSet resultSet) throws SQLException {
		Constructor<?> constructor;
		final Class<? extends Row> clazz = this.getRowClass();

		try {
			constructor = ReflectionUtil.getConstructor(clazz, FancyResultSet.class);

		} catch (final ReflectionException ex) {
			constructor = null;
		}

		ValidCore.checkNotNull(constructor, "Row class " + this.getRowClass() + " must have a constructor with SimpleResultSet parameter");

		try {
			constructor.setAccessible(true);

			return (T) constructor.newInstance(resultSet);

		} catch (final Throwable t) {

			// Ignore since it's handled upstream
			if (t.getCause() instanceof InvalidRowException)
				return null;

			CommonCore.throwError(t, "Failed to create a " + clazz.getSimpleName() + " object from result set!");

			return null;
		}
	}
}
