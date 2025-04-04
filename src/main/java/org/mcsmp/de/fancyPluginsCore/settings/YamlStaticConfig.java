package org.mcsmp.de.fancyPluginsCore.settings;

import org.mcsmp.de.fancyPluginsCore.collection.SerializedMap;
import org.mcsmp.de.fancyPluginsCore.exception.HandledException;
import org.mcsmp.de.fancyPluginsCore.exception.YamlSyntaxError;
import org.mcsmp.de.fancyPluginsCore.model.CaseNumberFormat;
import org.mcsmp.de.fancyPluginsCore.model.FancyComponent;
import org.mcsmp.de.fancyPluginsCore.model.FancyTime;
import org.mcsmp.de.fancyPluginsCore.model.IsInList;
import org.mcsmp.de.fancyPluginsCore.utility.CommonCore;
import org.mcsmp.de.fancyPluginsCore.utility.ValidCore;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A special case {@link YamlConfig} that allows static access to config.
 * <p>
 * You can only load or set values during initialization. Write "private static void init()"
 * methods in your class (and inner classes), we will invoke it automatically!
 * <p>
 * You cannot set values after the class has been loaded!
 */
public abstract class YamlStaticConfig {

	/**
	 * Represents "null" which you can use as convenience shortcut in loading config
	 * that has no internal from path.
	 */
	public static final String NO_DEFAULT = null;

	/**
	 * The temporary {@link YamlConfig} instance we store here to get values from
	 */
	private static YamlConfig TEMPORARY_INSTANCE;

	/**
	 * Internal use only: Create a new {@link YamlConfig} instance and link it to load fields via
	 * reflection.
	 */
	protected YamlStaticConfig() {
	}

	// -----------------------------------------------------------------------------------------------------
	// Main
	// -----------------------------------------------------------------------------------------------------

	/**
	 * Load the given static config class
	 *
	 * @param clazz
	 */
	public static final void load(final Class<? extends YamlStaticConfig> clazz) {
		try {
			final YamlStaticConfig config = clazz.newInstance();

			TEMPORARY_INSTANCE = new YamlConfig();
			TEMPORARY_INSTANCE.setUncommentedSections(config.getUncommentedSections());

			config.load();
			config.invokeInitMethods();

			TEMPORARY_INSTANCE.save();
			TEMPORARY_INSTANCE = null;

		} catch (final Throwable throwable) {
			if (throwable instanceof YamlSyntaxError) {
				throwable.printStackTrace();

				throw new HandledException(throwable);
			} else
				CommonCore.throwError(throwable, "Failed to load static settings " + clazz);
		}
	}

	/**
	 * Load your configuration here.
	 *
	 * @throws Exception
	 */
	protected abstract void load() throws Exception;

	protected List<String> getUncommentedSections() {
		return new ArrayList<>();
	}

	/*
	 * Loads the class via reflection, scanning for "private static void init()" methods to run
	 */
	private void invokeInitMethods() {
		ValidCore.checkNotNull(TEMPORARY_INSTANCE, "Instance cannot be null " + getFileName());
		ValidCore.checkNotNull(TEMPORARY_INSTANCE.hasDefaults(), "Default config cannot be null for " + getFileName());

		try {
			// Parent class if applicable.
			if (YamlStaticConfig.class.isAssignableFrom(this.getClass().getSuperclass())) {
				final Class<?> superClass = this.getClass().getSuperclass();

				this.invokeAll(superClass);
			}

			// The class itself.
			this.invokeAll(this.getClass());

		} catch (Throwable t) {
			if (t instanceof InvocationTargetException && t.getCause() != null)
				t = t.getCause();

			CommonCore.sneaky(t);
		}
	}

	/*
	 * Invoke all "private static void init()" methods in the class and its subclasses
	 */
	private void invokeAll(final Class<?> clazz) throws Exception {
		this.invokeMethodsIn(clazz);

		// All sub-classes in superclass.
		for (final Class<?> subClazz : clazz.getDeclaredClasses())
			this.invokeAll(subClazz);
	}

	/*
	 * Invoke all "private static void init()" methods in the class
	 */
	private void invokeMethodsIn(final Class<?> clazz) throws Exception {
		for (final Method method : clazz.getDeclaredMethods()) {
			final int mod = method.getModifiers();

			if (method.getName().equals("init")) {
				ValidCore.checkBoolean(Modifier.isPrivate(mod) &&
								Modifier.isStatic(mod) &&
								method.getReturnType() == Void.TYPE &&
								method.getParameterTypes().length == 0,
						"Method '" + method.getName() + "' in " + clazz + " must be 'private static void init()'");

				method.setAccessible(true);
				method.invoke(null);
			}
		}

		this.checkFields(clazz);
	}

	/*
	 * Safety check whether all fields have been set
	 */
	private void checkFields(final Class<?> clazz) throws Exception {
		if (clazz == YamlStaticConfig.class)
			return;

		for (final Field field : clazz.getDeclaredFields()) {
			field.setAccessible(true);

			if (Modifier.isPublic(field.getModifiers()))
				ValidCore.checkBoolean(!field.getType().isPrimitive(), "Field '" + field.getName() + "' in " + clazz + " must not be primitive!");

			Object result = null;

			try {
				result = field.get(null);
			} catch (final NullPointerException ex) {
			}

			ValidCore.checkNotNull(result, "Null " + field.getType().getSimpleName() + " field '" + field.getName() + "' in " + clazz);
		}
	}

	// -----------------------------------------------------------------------------------------------------
	// Delegate methods
	// -----------------------------------------------------------------------------------------------------

	/**
	 * @see YamlConfig#getFile()
	 *
	 * @return
	 */
	protected static final String getFileName() {
		return TEMPORARY_INSTANCE.getFile().getName();
	}

	/**
	 * @see YamlConfig#isSet(String)
	 *
	 * @param path
	 * @return
	 */
	protected static final boolean isSet(final String path) {
		return TEMPORARY_INSTANCE.isSet(path);
	}

	/**
	 * @see YamlConfig#isSetDefault(String)
	 *
	 * @param path
	 * @return
	 */
	protected static final boolean isSetDefault(final String path) {
		return TEMPORARY_INSTANCE.isSetDefault(path);
	}

	/**
	 * @see YamlConfig#move(String, String)
	 *
	 * @param fromRelative
	 * @param toAbsolute
	 */
	protected static final void move(final String fromRelative, final String toAbsolute) {
		TEMPORARY_INSTANCE.move(fromRelative, toAbsolute);
	}

	/**
	 * @see YamlConfig#set(String, Object)
	 *
	 * @param path
	 * @param value
	 */
	protected static final void set(final String path, final Object value) {
		TEMPORARY_INSTANCE.set(path, value);
	}

	/**
	 * @see YamlConfig#setPathPrefix(String)
	 *
	 * @param pathPrefix
	 */
	protected static final void setPathPrefix(final String pathPrefix) {
		TEMPORARY_INSTANCE.setPathPrefix(pathPrefix);
	}

	/**
	 * @see YamlConfig#loadAndExtract(String)
	 *
	 * @param internalPath
	 */
	protected final void loadConfiguration(final String internalPath) {
		TEMPORARY_INSTANCE.loadAndExtract(internalPath, internalPath);
	}

	/**
	 * @see YamlConfig#loadAndExtract(String, String)
	 *
	 * @param from
	 * @param to
	 */
	protected final void loadConfiguration(final String from, final String to) {
		TEMPORARY_INSTANCE.loadAndExtract(from, to);
	}

	// -----------------------------------------------------------------------------------------------------
	// Delegate getters
	// -----------------------------------------------------------------------------------------------------

	/**
	 * @see YamlConfig#get(String, Class)
	 *
	 * @param <E>
	 * @param path
	 * @param typeOf
	 * @return
	 */
	protected static final <E> E get(final String path, final Class<E> typeOf) {
		return TEMPORARY_INSTANCE.get(path, typeOf);
	}

	/**
	 * @see YamlConfig#getBoolean(String)
	 *
	 * @param path
	 * @return
	 */
	protected static final Boolean getBoolean(final String path) {
		return TEMPORARY_INSTANCE.getBoolean(path);
	}

	/**
	 * @see YamlConfig#getCaseNumberFormat(String)
	 *
	 * @param path
	 * @return
	 */
	protected static final CaseNumberFormat getCaseNumberFormat(final String path) {
		return TEMPORARY_INSTANCE.getCaseNumberFormat(path);
	}

	/**
	 * @see YamlConfig#getCommandList(String)
	 *
	 * @return
	 */
	protected static final List<String> getCommandList(final String path) {
		return TEMPORARY_INSTANCE.getCommandList(path);
	}

	/**
	 * @see YamlConfig#getComponent(String)
	 *
	 * @param path
	 * @return
	 */
	protected static final FancyComponent getComponent(final String path) {
		return TEMPORARY_INSTANCE.getComponent(path);
	}

	/**
	 * @see YamlConfig#getDouble(String)
	 *
	 * @param path
	 * @return
	 */
	protected static final Double getDouble(final String path) {
		return TEMPORARY_INSTANCE.getDouble(path);
	}

	/**
	 * @see YamlConfig#getInteger(String)
	 *
	 * @param path
	 * @return
	 */
	protected static final Integer getInteger(final String path) {
		return TEMPORARY_INSTANCE.getInteger(path);
	}

	/**
	 * @see YamlConfig#getInteger(String)
	 *
	 * @param path
	 * @return
	 */
	protected static final Long getLong(final String path) {
		return TEMPORARY_INSTANCE.getLong(path);
	}

	/**
	 * @see YamlConfig#getIsInList(String, Class)
	 *
	 * @param <E>
	 * @param path
	 * @param listType
	 * @return
	 */
	protected static final <E> IsInList<E> getIsInList(final String path, final Class<E> listType) {
		return TEMPORARY_INSTANCE.getIsInList(path, listType);
	}

	/**
	 *
	 * @param <E>
	 * @param path
	 * @param listType
	 * @return
	 */
	protected static final <E> List<E> getList(final String path, final Class<E> listType) {
		return TEMPORARY_INSTANCE.getList(path, listType);
	}

	/**
	 *
	 * @see YamlConfig#getMap(String)
	 *
	 * @param path
	 * @return
	 */
	protected static final SerializedMap getMap(final String path) {
		return TEMPORARY_INSTANCE.getMap(path);
	}

	/**
	 *
	 * @param <Key>
	 * @param <Value>
	 * @param path
	 * @param keyType
	 * @param valueType
	 * @return
	 */
	protected static final <Key, Value> Map<Key, Value> getMap(final String path, final Class<Key> keyType, final Class<Value> valueType) {
		return TEMPORARY_INSTANCE.getMap(path, keyType, valueType);
	}

	/**
	 * @see YamlConfig#getMapList(String)
	 *
	 * @param path
	 * @return
	 */
	protected static final List<SerializedMap> getMapList(final String path) {
		return TEMPORARY_INSTANCE.getMapList(path);
	}

	/**
	 *
	 * @param <K>
	 * @param <V>
	 * @param path
	 * @param keyType
	 * @param setType
	 * @param setDeserializerParams
	 * @return
	 */
	protected static final <K, V> Map<K, List<V>> getMapList(final String path, final Class<K> keyType, final Class<V> setType, final Object setDeserializerParams) {
		return TEMPORARY_INSTANCE.getMapList(path, keyType, setType, setDeserializerParams);
	}

	/**
	 * @see YamlConfig#getObject(String)
	 *
	 * @param path
	 * @return
	 */
	protected static final Object getObject(final String path) {
		return TEMPORARY_INSTANCE.getObject(path);
	}

	/**
	 * @see YamlConfig#getPercentage(String)
	 *
	 * @param path
	 * @return
	 */
	protected static final Double getPercentage(final String path) {
		return TEMPORARY_INSTANCE.getPercentage(path);
	}

	/**
	 *
	 * @param <E>
	 * @param path
	 * @param typeOf
	 * @return
	 */
	protected static final <E> Set<E> getSet(final String path, final Class<E> typeOf) {
		return TEMPORARY_INSTANCE.getSet(path, typeOf);
	}

	/**
	 * @see YamlConfig#getString(String)
	 *
	 * @param path
	 * @return
	 */
	protected static final String getString(final String path) {
		return TEMPORARY_INSTANCE.getString(path);
	}

	/**
	 * @see YamlConfig#getStringList(String)
	 *
	 * @param path
	 * @return
	 */
	protected static final List<String> getStringList(final String path) {
		return TEMPORARY_INSTANCE.getStringList(path);
	}

	/**
	 * @see YamlConfig#getTime(String)
	 *
	 * @param path
	 * @return
	 */
	protected static final FancyTime getTime(final String path) {
		return TEMPORARY_INSTANCE.getTime(path);
	}
}
