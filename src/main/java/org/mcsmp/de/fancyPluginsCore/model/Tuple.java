package org.mcsmp.de.fancyPluginsCore.model;

import lombok.Data;
import org.mcsmp.de.fancyPluginsCore.collection.SerializedMap;
import org.mcsmp.de.fancyPluginsCore.utility.SerializeUtilCore;
import org.mcsmp.de.fancyPluginsCore.utility.ValidCore;

/**
 * A simple class for storing two values.
 *
 * @param <K>
 * @param <V>
 */
@Data
public final class Tuple<K, V> implements ConfigSerializable {

	/**
	 * The key.
	 */
	private final K key;

	/**
	 * The value.
	 */
	private final V value;

	/**
	 * @see org.mineacademy.fo.model.ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		return SerializedMap.fromArray("Key", this.key, "Value", this.value);
	}

	/**
	 * Return this tuple in X - Y syntax.
	 *
	 * @return
	 */
	public String toLine() {
		return this.key + " - " + this.value;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.toLine();
	}

	/**
	 * Transform the given config section to tuple.
	 *
	 * @param <K>
	 * @param <V>
	 * @param map
	 * @param keyType
	 * @param valueType
	 * @return
	 */
	public static <K, V> Tuple<K, V> deserialize(final SerializedMap map, final Class<K> keyType, final Class<V> valueType) {
		final K key = map.containsKey("Key") ? map.get("Key", keyType) : null;
		final V value = map.containsKey("Value") ? map.get("Value", valueType) : null;

		return new Tuple<>(key, value);
	}

	/**
	 * Deserialize the given line (it must have the KEY - VALUE syntax) into the given tuple,
	 * suited for YAML storage not JSON.
	 *
	 * @param <K>
	 * @param <V>
	 * @param line
	 * @param keyType
	 * @param valueType
	 * @return tuple or null if line is null
	 */
	public static <K, V> Tuple<K, V> deserialize(final String line, final Class<K> keyType, final Class<V> valueType) {
		if (line == null)
			return null;

		final String split[] = line.split(" - ");
		ValidCore.checkBoolean(split.length == 2, "Line must have the syntax <" + keyType.getSimpleName() + "> - <" + valueType.getSimpleName() + "> but got: " + line);

		final K key = SerializeUtilCore.deserialize(SerializeUtilCore.Language.YAML, keyType, split[0]);
		final V value = SerializeUtilCore.deserialize(SerializeUtilCore.Language.YAML, valueType, split[1]);

		return new Tuple<>(key, value);
	}
}
