package org.mcsmp.de.fancyPluginsCore.collection;

import lombok.NonNull;
import org.mcsmp.de.fancyPluginsCore.utility.ValidCore;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * A thread-safe map that expires entries. Optional features include expiration
 * policies, variable entry expiration, lazy entry loading, and expiration
 * listeners.
 *
 * <p>
 * Entries are tracked by expiration time and expired by a single thread.
 *
 * <p>
 * Expiration listeners are called synchronously as entries are expired and
 * block write operations to the map until they completed. Asynchronous
 * expiration listeners are called on a separate thread pool and do not block
 * map operations.
 *
 * <p>
 * When variable expiration is disabled (default), put/remove operations have a
 * time complexity <i>O(1)</i>. When variable expiration is enabled, put/remove
 * operations have time complexity of <i>O(log n)</i>.
 *
 * <p>
 * Example usages:
 *
 * <pre>
 * {
 * 	&#64;code
 * 	Map<String, Integer> map = ExpiringMap.create();
 * 	Map<String, Integer> map = ExpiringMap.builder().expiration(30, TimeUnit.SECONDS).build();
 * 	Map<String, Connection> map = ExpiringMap.builder()
 * 			.expiration(10, TimeUnit.MINUTES)
 * 			.entryLoader(new EntryLoader<String, Connection>() {
 * 				public Connection load(String address) {
 * 					return new Connection(address);
 *                }
 *            })
 * 			.expirationListener(new ExpirationListener<String, Connection>() {
 * 				public void expired(String key, Connection connection) {
 * 					connection.close();
 *                }
 *            })
 * 			.build();
 * }
 * </pre>
 *
 * @param <K> Key type
 * @param <V> Value type
 * @author Jonathan Halterman
 */
public final class ExpiringMap<K, V> implements ConcurrentMap<K, V> {
	static volatile ScheduledExecutorService EXPIRER;
	static volatile ThreadPoolExecutor LISTENER_SERVICE;
	static ThreadFactory THREAD_FACTORY;

	List<ExpirationListener<K, V>> expirationListeners;
	List<ExpirationListener<K, V>> asyncExpirationListeners;
	private final AtomicLong expirationNanos;
	private int maxSize;
	private final AtomicReference<ExpirationPolicy> expirationPolicy;
	private final Function<? super K, ? extends V> entryLoader;
	private final ExpiringEntryLoader<? super K, ? extends V> expiringEntryLoader;
	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private final Lock readLock = this.readWriteLock.readLock();
	private final Lock writeLock = this.readWriteLock.writeLock();
	/**
	 * Guarded by "readWriteLock"
	 */
	private final EntryMap<K, V> entries;
	private final boolean variableExpiration;

	public interface ExpirationListener<K, V> {
		void expired(K var1, V var2);
	}

	/**
	 * Sets the {@link ThreadFactory} that is used to create expiration and listener
	 * callback threads for all ExpiringMap instances.
	 *
	 * @param threadFactory
	 * @throws NullPointerException if {@code threadFactory} is null
	 */
	public static void setThreadFactory(@NonNull final ThreadFactory threadFactory) {
		THREAD_FACTORY = threadFactory;
	}

	/**
	 * Creates a new instance of ExpiringMap.
	 *
	 * @param builder The map builder
	 */
	private ExpiringMap(final Builder<K, V> builder) {
		if (EXPIRER == null)
			synchronized (ExpiringMap.class) {
				if (EXPIRER == null)
					EXPIRER = Executors.newSingleThreadScheduledExecutor(
							THREAD_FACTORY == null ? new NamedThreadFactory("ExpiringMap-Expirer") : THREAD_FACTORY);
			}

		if (LISTENER_SERVICE == null && builder.asyncExpirationListeners != null)
			synchronized (ExpiringMap.class) {
				if (LISTENER_SERVICE == null)
					LISTENER_SERVICE = (ThreadPoolExecutor) Executors.newCachedThreadPool(
							THREAD_FACTORY == null ? new NamedThreadFactory("ExpiringMap-Listener-%s") : THREAD_FACTORY);
			}

		this.variableExpiration = builder.variableExpiration;
		this.entries = this.variableExpiration ? new EntryTreeHashMap<>() : new EntryLinkedHashMap<>();
		if (builder.expirationListeners != null)
			this.expirationListeners = new CopyOnWriteArrayList<>(builder.expirationListeners);
		if (builder.asyncExpirationListeners != null)
			this.asyncExpirationListeners = new CopyOnWriteArrayList<>(builder.asyncExpirationListeners);
		this.expirationPolicy = new AtomicReference<>(builder.expirationPolicy);
		this.expirationNanos = new AtomicLong(TimeUnit.NANOSECONDS.convert(builder.duration, builder.timeUnit));
		this.maxSize = builder.maxSize;
		this.entryLoader = builder.entryLoader;
		this.expiringEntryLoader = builder.expiringEntryLoader;
	}

	/**
	 * Builds ExpiringMap instances. Defaults to ExpirationPolicy.CREATED,
	 * expiration of 60 TimeUnit.SECONDS and a maxSize of Integer.MAX_VALUE.
	 * @param <K>
	 * @param <V>
	 */
	public static final class Builder<K, V> {
		private ExpirationPolicy expirationPolicy = ExpirationPolicy.CREATED;
		private List<ExpirationListener<K, V>> expirationListeners;
		private List<ExpirationListener<K, V>> asyncExpirationListeners;
		private TimeUnit timeUnit = TimeUnit.SECONDS;
		private boolean variableExpiration;
		private long duration = 60;
		private int maxSize = Integer.MAX_VALUE;
		private Function<K, V> entryLoader;
		private ExpiringEntryLoader<K, V> expiringEntryLoader;

		/**
		 * Creates a new Builder object.
		 */
		private Builder() {
		}

		/**
		 * Builds and returns an expiring map.
		 *
		 * @param <K1> Key type
		 * @param <V1> Value type
		 * @return
		 */
		public <K1 extends K, V1 extends V> ExpiringMap<K1, V1> build() {
			return new ExpiringMap<>((Builder<K1, V1>) this);
		}

		/**
		 * Sets the default map entry expiration.
		 *
		 * @param duration the length of time after an entry is created that it should
		 *                 be removed
		 * @param timeUnit the unit that {@code duration} is expressed in
		 * @return
		 * @throws NullPointerException if {@code timeUnit} is null
		 */
		public Builder<K, V> expiration(final long duration, @NonNull final TimeUnit timeUnit) {
			this.duration = duration;
			this.timeUnit = timeUnit;
			return this;
		}

		/**
		 * Sets the maximum size of the map. Once this size has been reached, adding an
		 * additional entry will expire the first entry in line for expiration based on
		 * the expiration policy.
		 *
		 * @param maxSize The maximum size of the map.
		 * @return
		 */
		public Builder<K, V> maxSize(final int maxSize) {
			ValidCore.checkBoolean(maxSize > 0, "maxSize");
			this.maxSize = maxSize;
			return this;
		}

		/**
		 * Sets the EntryLoader to use when loading entries. Either an EntryLoader or
		 * ExpiringEntryLoader may be set, not both.
		 *
		 * @param <K1>
		 * @param <V1>
		 * @param loader to set
		 * @throws NullPointerException  if {@code loader} is null
		 * @throws IllegalStateException if an
		 *                               {@link #expiringEntryLoader(ExpiringEntryLoader)
		 *                               ExpiringEntryLoader} is set
		 *
		 * @return
		 */
		public <K1 extends K, V1 extends V> Builder<K1, V1> entryLoader(@NonNull final Function<? super K1, ? super V1> loader) {
			this.assertNoLoaderSet();
			this.entryLoader = (Function<K, V>) loader;
			return (Builder<K1, V1>) this;
		}

		/**
		 * Sets the ExpiringEntryLoader to use when loading entries and configures
		 * {@link #variableExpiration() variable expiration}. Either an EntryLoader or
		 * ExpiringEntryLoader may be set, not both.
		 * @param <K1>
		 * @param <V1>
		 *
		 * @param loader to set
		 * @return
		 * @throws NullPointerException  if {@code loader} is null
		 * @throws IllegalStateException if an {@link #entryLoader(Function)
		 *                               EntryLoader} is set
		 */
		public <K1 extends K, V1 extends V> Builder<K1, V1> expiringEntryLoader(@NonNull final ExpiringEntryLoader<? super K1, ? super V1> loader) {
			this.assertNoLoaderSet();
			this.expiringEntryLoader = (ExpiringEntryLoader<K, V>) loader;
			this.variableExpiration();
			return (Builder<K1, V1>) this;
		}

		/**
		 * Configures the expiration listener that will receive notifications upon each
		 * map entry's expiration. Notifications are delivered synchronously and block
		 * map write operations.
		 * @param <K1>
		 * @param <V1>
		 *
		 * @param listener to set
		 * @return
		 * @throws NullPointerException if {@code listener} is null
		 */
		public <K1 extends K, V1 extends V> Builder<K1, V1> expirationListener(
				final ExpirationListener<? super K1, ? super V1> listener) {
			ValidCore.checkNotNull(listener, "listener");
			if (this.expirationListeners == null)
				this.expirationListeners = new ArrayList<>();
			this.expirationListeners.add((ExpirationListener<K, V>) listener);
			return (Builder<K1, V1>) this;
		}

		/**
		 * Configures the expiration listeners which will receive notifications upon
		 * each map entry's expiration. Notifications are delivered synchronously and
		 * block map write operations.
		 * @param <K1>
		 * @param <V1>
		 *
		 * @param listeners to set
		 * @return
		 * @throws NullPointerException if {@code listener} is null
		 */

		public <K1 extends K, V1 extends V> Builder<K1, V1> expirationListeners(
				final List<ExpirationListener<? super K1, ? super V1>> listeners) {
			ValidCore.checkNotNull(listeners, "listeners");
			if (this.expirationListeners == null)
				this.expirationListeners = new ArrayList<>(listeners.size());
			for (final ExpirationListener<? super K1, ? super V1> listener : listeners)
				this.expirationListeners.add((ExpirationListener<K, V>) listener);
			return (Builder<K1, V1>) this;
		}

		/**
		 * Configures the expiration listener which will receive asynchronous
		 * notifications upon each map entry's expiration.
		 * @param <K1>
		 * @param <V1>
		 *
		 * @param listener to set
		 * @return
		 * @throws NullPointerException if {@code listener} is null
		 */
		public <K1 extends K, V1 extends V> Builder<K1, V1> asyncExpirationListener(
				final ExpirationListener<? super K1, ? super V1> listener) {
			ValidCore.checkNotNull(listener, "listener");
			if (this.asyncExpirationListeners == null)
				this.asyncExpirationListeners = new ArrayList<>();
			this.asyncExpirationListeners.add((ExpirationListener<K, V>) listener);
			return (Builder<K1, V1>) this;
		}

		/**
		 * Configures the expiration listeners which will receive asynchronous
		 * notifications upon each map entry's expiration.
		 * @param <K1>
		 * @param <V1>
		 *
		 * @param listeners to set
		 * @return
		 * @throws NullPointerException if {@code listener} is null
		 */
		public <K1 extends K, V1 extends V> Builder<K1, V1> asyncExpirationListeners(
				final List<ExpirationListener<? super K1, ? super V1>> listeners) {
			ValidCore.checkNotNull(listeners, "listeners");
			if (this.asyncExpirationListeners == null)
				this.asyncExpirationListeners = new ArrayList<>(listeners.size());
			for (final ExpirationListener<? super K1, ? super V1> listener : listeners)
				this.asyncExpirationListeners.add((ExpirationListener<K, V>) listener);
			return (Builder<K1, V1>) this;
		}

		/**
		 * Configures the map entry expiration policy.
		 *
		 * @param expirationPolicy
		 * @return
		 * @throws NullPointerException if {@code expirationPolicy} is null
		 */
		public Builder<K, V> expirationPolicy(@NonNull final ExpirationPolicy expirationPolicy) {
			this.expirationPolicy = expirationPolicy;
			return this;
		}

		/**
		 * Allows for map entries to have individual expirations and for expirations to
		 * be changed.
		 * @return
		 */
		public Builder<K, V> variableExpiration() {
			this.variableExpiration = true;
			return this;
		}

		private void assertNoLoaderSet() {
			ValidCore.checkBoolean(this.entryLoader == null && this.expiringEntryLoader == null,
					"Either entryLoader or expiringEntryLoader may be set, not both");
		}
	}

	/**
	 * Entry map definition.
	 */
	private interface EntryMap<K, V> extends Map<K, ExpiringEntry<K, V>> {
		/**
		 * Returns the first entry in the map or null if the map is empty.
		 * @return
		 */
		ExpiringEntry<K, V> first();

		/**
		 * Reorders the given entry in the map.
		 *
		 * @param entry to reorder
		 */
		void reorder(ExpiringEntry<K, V> entry);

		/**
		 * Returns a values iterator.
		 * @return
		 */
		Iterator<ExpiringEntry<K, V>> valuesIterator();
	}

	/**
	 * Entry LinkedHashMap implementation.
	 */
	private static class EntryLinkedHashMap<K, V> extends LinkedHashMap<K, ExpiringEntry<K, V>>
			implements EntryMap<K, V> {
		private static final long serialVersionUID = 1L;

		@Override
		public boolean containsValue(final Object value) {
			for (final ExpiringEntry<K, V> entry : this.values()) {
				final V v = entry.value;
				if (v == value || value != null && value.equals(v))
					return true;
			}
			return false;
		}

		@Override
		public ExpiringEntry<K, V> first() {
			return this.isEmpty() ? null : this.values().iterator().next();
		}

		@Override
		public void reorder(final ExpiringEntry<K, V> value) {
			this.remove(value.key);
			value.resetExpiration();
			this.put(value.key, value);
		}

		@Override
		public Iterator<ExpiringEntry<K, V>> valuesIterator() {
			return this.values().iterator();
		}

		abstract class AbstractHashIterator {
			private final Iterator<Map.Entry<K, ExpiringEntry<K, V>>> iterator = EntryLinkedHashMap.this.entrySet().iterator();
			private ExpiringEntry<K, V> next;

			public boolean hasNext() {
				return this.iterator.hasNext();
			}

			public ExpiringEntry<K, V> getNext() {
				this.next = this.iterator.next().getValue();
				return this.next;
			}

			public void remove() {
				this.iterator.remove();
			}
		}

		final class KeyIterator extends AbstractHashIterator implements Iterator<K> {
			@Override
			public K next() {
				return this.getNext().key;
			}
		}

		final class ValueIterator extends AbstractHashIterator implements Iterator<V> {
			@Override
			public V next() {
				return this.getNext().value;
			}
		}

		public final class EntryIterator extends AbstractHashIterator implements Iterator<Map.Entry<K, V>> {
			@Override
			public Map.Entry<K, V> next() {
				return mapEntryFor(this.getNext());
			}
		}
	}

	/**
	 * Entry TreeHashMap implementation for variable expiration ExpiringMap entries.
	 */
	private static class EntryTreeHashMap<K, V> extends HashMap<K, ExpiringEntry<K, V>> implements EntryMap<K, V> {
		private static final long serialVersionUID = 1L;
		SortedSet<ExpiringEntry<K, V>> sortedSet = new ConcurrentSkipListSet<>();

		@Override
		public void clear() {
			super.clear();
			this.sortedSet.clear();
		}

		@Override
		public boolean containsValue(final Object value) {
			for (final ExpiringEntry<K, V> entry : this.values()) {
				final V v = entry.value;
				if (v == value || value != null && value.equals(v))
					return true;
			}
			return false;
		}

		@Override
		public ExpiringEntry<K, V> first() {
			return this.sortedSet.isEmpty() ? null : this.sortedSet.first();
		}

		@Override
		public ExpiringEntry<K, V> put(final K key, final ExpiringEntry<K, V> value) {
			this.sortedSet.add(value);
			return super.put(key, value);
		}

		@Override
		public ExpiringEntry<K, V> remove(final Object key) {
			final ExpiringEntry<K, V> entry = super.remove(key);
			if (entry != null)
				this.sortedSet.remove(entry);
			return entry;
		}

		@Override
		public void reorder(final ExpiringEntry<K, V> value) {
			this.sortedSet.remove(value);
			value.resetExpiration();
			this.sortedSet.add(value);
		}

		@Override
		public Iterator<ExpiringEntry<K, V>> valuesIterator() {
			return new ExpiringEntryIterator();
		}

		abstract class AbstractHashIterator {
			private final Iterator<ExpiringEntry<K, V>> iterator = EntryTreeHashMap.this.sortedSet.iterator();
			protected ExpiringEntry<K, V> next;

			public boolean hasNext() {
				return this.iterator.hasNext();
			}

			public ExpiringEntry<K, V> getNext() {
				this.next = this.iterator.next();
				return this.next;
			}

			public void remove() {
				EntryTreeHashMap.super.remove(this.next.key);
				this.iterator.remove();
			}
		}

		final class ExpiringEntryIterator extends AbstractHashIterator implements Iterator<ExpiringEntry<K, V>> {
			@Override
			public ExpiringEntry<K, V> next() {
				return this.getNext();
			}
		}

		final class KeyIterator extends AbstractHashIterator implements Iterator<K> {
			@Override
			public K next() {
				return this.getNext().key;
			}
		}

		final class ValueIterator extends AbstractHashIterator implements Iterator<V> {
			@Override
			public V next() {
				return this.getNext().value;
			}
		}

		final class EntryIterator extends AbstractHashIterator implements Iterator<Map.Entry<K, V>> {
			@Override
			public Map.Entry<K, V> next() {
				return mapEntryFor(this.getNext());
			}
		}
	}

	/**
	 * Expiring map entry implementation.
	 */
	static class ExpiringEntry<K, V> implements Comparable<ExpiringEntry<K, V>> {
		final AtomicLong expirationNanos;
		/**
		 * Epoch time at which the entry is expected to expire
		 */
		final AtomicLong expectedExpiration;
		final AtomicReference<ExpirationPolicy> expirationPolicy;
		final K key;
		/**
		 * Guarded by "this"
		 */
		volatile Future<?> entryFuture;
		/**
		 * Guarded by "this"
		 */
		V value;
		/**
		 * Guarded by "this"
		 */
		volatile boolean scheduled;

		/**
		 * Creates a new ExpiringEntry object.
		 *
		 * @param key              for the entry
		 * @param value            for the entry
		 * @param expirationPolicy for the entry
		 * @param expirationNanos  for the entry
		 */
		ExpiringEntry(final K key, final V value, final AtomicReference<ExpirationPolicy> expirationPolicy, final AtomicLong expirationNanos) {
			this.key = key;
			this.value = value;
			this.expirationPolicy = expirationPolicy;
			this.expirationNanos = expirationNanos;
			this.expectedExpiration = new AtomicLong();
			this.resetExpiration();
		}

		@Override
		public int compareTo(final ExpiringEntry<K, V> other) {
			if (this.key.equals(other.key))
				return 0;
			return this.expectedExpiration.get() < other.expectedExpiration.get() ? -1 : 1;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.key, this.value);
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (this.getClass() != obj.getClass())
				return false;
			final ExpiringEntry<?, ?> other = (ExpiringEntry<?, ?>) obj;
			if (!this.key.equals(other.key))
				return false;
			if (!Objects.equals(this.value, other.value))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return this.value != null ? this.value.toString() : "";
		}

		/**
		 * Marks the entry as canceled.
		 *
		 * @return true if the entry was scheduled
		 */
		synchronized boolean cancel() {
			final boolean result = this.scheduled;
			if (this.entryFuture != null)
				this.entryFuture.cancel(false);

			this.entryFuture = null;
			this.scheduled = false;
			return result;
		}

		/**
		 * Gets the entry value.
		 */
		synchronized V getValue() {
			return this.value;
		}

		/**
		 * Resets the entry's expected expiration.
		 */
		void resetExpiration() {
			this.expectedExpiration.set(this.expirationNanos.get() + System.nanoTime());
		}

		/**
		 * Marks the entry as scheduled.
		 */
		synchronized void schedule(final Future<?> entryFuture) {
			this.entryFuture = entryFuture;
			this.scheduled = true;
		}

		/**
		 * Sets the entry value.
		 */
		synchronized void setValue(final V value) {
			this.value = value;
		}
	}

	/**
	 * Creates an ExpiringMap builder.
	 *
	 * @return New ExpiringMap builder
	 */
	public static Builder<Object, Object> builder() {
		return new Builder<>();
	}

	/**
	 * Creates a new instance of ExpiringMap with ExpirationPolicy.CREATED and an
	 * expiration of 60 seconds.
	 * @param <K>
	 * @param <V>
	 * @return
	 */

	public static <K, V> ExpiringMap<K, V> create() {
		return new ExpiringMap<>((Builder<K, V>) ExpiringMap.builder());
	}

	/**
	 * Adds an expiration listener.
	 *
	 * @param listener to add
	 * @throws NullPointerException if {@code listener} is null
	 */
	public synchronized void addExpirationListener(final ExpirationListener<K, V> listener) {
		ValidCore.checkNotNull(listener, "listener");
		if (this.expirationListeners == null)
			this.expirationListeners = new CopyOnWriteArrayList<>();
		this.expirationListeners.add(listener);
	}

	/**
	 * Adds an asynchronous expiration listener.
	 *
	 * @param listener to add
	 * @throws NullPointerException if {@code listener} is null
	 */
	public synchronized void addAsyncExpirationListener(final ExpirationListener<K, V> listener) {
		ValidCore.checkNotNull(listener, "listener");
		if (this.asyncExpirationListeners == null)
			this.asyncExpirationListeners = new CopyOnWriteArrayList<>();
		this.asyncExpirationListeners.add(listener);
	}

	@Override
	public void clear() {
		this.writeLock.lock();
		try {
			for (final ExpiringEntry<K, V> entry : this.entries.values())
				entry.cancel();
			this.entries.clear();
		} finally {
			this.writeLock.unlock();
		}
	}

	@Override
	public boolean containsKey(final Object key) {
		this.readLock.lock();
		try {
			return this.entries.containsKey(key);
		} finally {
			this.readLock.unlock();
		}
	}

	@Override
	public boolean containsValue(final Object value) {
		this.readLock.lock();
		try {
			return this.entries.containsValue(value);
		} finally {
			this.readLock.unlock();
		}
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		return new AbstractSet<Map.Entry<K, V>>() {
			@Override
			public void clear() {
				ExpiringMap.this.clear();
			}

			@Override
			public boolean contains(final Object entry) {
				if (!(entry instanceof Map.Entry))
					return false;
				final Map.Entry<?, ?> e = (Map.Entry<?, ?>) entry;
				return ExpiringMap.this.containsKey(e.getKey());
			}

			@Override
			public Iterator<Map.Entry<K, V>> iterator() {
				return ExpiringMap.this.entries instanceof EntryLinkedHashMap ? ((EntryLinkedHashMap<K, V>) ExpiringMap.this.entries).new EntryIterator()
						: ((EntryTreeHashMap<K, V>) ExpiringMap.this.entries).new EntryIterator();
			}

			@Override
			public boolean remove(final Object entry) {
				if (entry instanceof Map.Entry) {
					final Map.Entry<?, ?> e = (Map.Entry<?, ?>) entry;
					return ExpiringMap.this.remove(e.getKey()) != null;
				}
				return false;
			}

			@Override
			public int size() {
				return ExpiringMap.this.size();
			}
		};
	}

	@Override
	public boolean equals(final Object obj) {
		this.readLock.lock();
		try {
			return this.entries.equals(obj);
		} finally {
			this.readLock.unlock();
		}
	}

	@Override
	public V get(final Object key) {
		final ExpiringEntry<K, V> entry = this.getEntry(key);

		if (entry == null)
			return this.load((K) key);
		else if (ExpirationPolicy.ACCESSED.equals(entry.expirationPolicy.get()))
			this.resetEntry(entry, false);

		return entry.getValue();
	}

	private V load(final K key) {
		if (this.entryLoader == null && this.expiringEntryLoader == null)
			return null;

		this.writeLock.lock();
		try {
			// Double check for entry
			final ExpiringEntry<K, V> entry = this.getEntry(key);
			if (entry != null)
				return entry.getValue();

			if (this.entryLoader != null) {
				final V value = this.entryLoader.apply(key);
				this.put(key, value);
				return value;
			} else {
				final ExpiringValue<? extends V> expiringValue = this.expiringEntryLoader.load(key);
				if (expiringValue == null) {
					this.put(key, null);
					return null;
				} else {
					final long duration = expiringValue.getTimeUnit() == null ? this.expirationNanos.get() : expiringValue.getDuration();
					final TimeUnit timeUnit = expiringValue.getTimeUnit() == null ? TimeUnit.NANOSECONDS : expiringValue.getTimeUnit();
					this.put(key, expiringValue.getValue(), expiringValue.getExpirationPolicy() == null ? this.expirationPolicy.get()
							: expiringValue.getExpirationPolicy(), duration, timeUnit);
					return expiringValue.getValue();
				}
			}
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Returns the map's default expiration duration in milliseconds.
	 *
	 * @return The expiration duration (milliseconds)
	 */
	public long getExpiration() {
		return TimeUnit.NANOSECONDS.toMillis(this.expirationNanos.get());
	}

	/**
	 * Gets the expiration duration in milliseconds for the entry corresponding to
	 * the given key.
	 *
	 * @param key
	 * @return The expiration duration in milliseconds
	 * @throws NullPointerException   if {@code key} is null
	 * @throws NoSuchElementException If no entry exists for the given key
	 */
	public long getExpiration(final K key) {
		ValidCore.checkNotNull(key, "key");
		final ExpiringEntry<K, V> entry = this.getEntry(key);

		return TimeUnit.NANOSECONDS.toMillis(entry.expirationNanos.get());
	}

	/**
	 * Gets the ExpirationPolicy for the entry corresponding to the given
	 * {@code key}.
	 *
	 * @param key
	 * @return The ExpirationPolicy for the {@code key}
	 * @throws NullPointerException   if {@code key} is null
	 * @throws NoSuchElementException If no entry exists for the given key
	 */
	public ExpirationPolicy getExpirationPolicy(final K key) {
		ValidCore.checkNotNull(key, "key");
		final ExpiringEntry<K, V> entry = this.getEntry(key);
		ValidCore.checkNotNull(entry);
		return entry.expirationPolicy.get();
	}

	/**
	 * Gets the expected expiration, in milliseconds from the current time, for the
	 * entry corresponding to the given {@code key}.
	 *
	 * @param key
	 * @return The expiration duration in milliseconds
	 * @throws NullPointerException   if {@code key} is null
	 * @throws NoSuchElementException If no entry exists for the given key
	 */
	public long getExpectedExpiration(final K key) {
		ValidCore.checkNotNull(key, "key");
		final ExpiringEntry<K, V> entry = this.getEntry(key);
		ValidCore.checkNotNull(entry);
		return TimeUnit.NANOSECONDS.toMillis(entry.expectedExpiration.get() - System.nanoTime());
	}

	/**
	 * Gets the maximum size of the map. Once this size has been reached, adding an
	 * additional entry will expire the first entry in line for expiration based on
	 * the expiration policy.
	 *
	 * @return The maximum size of the map.
	 */
	public int getMaxSize() {
		return this.maxSize;
	}

	@Override
	public int hashCode() {
		this.readLock.lock();
		try {
			return this.entries.hashCode();
		} finally {
			this.readLock.unlock();
		}
	}

	@Override
	public boolean isEmpty() {
		this.readLock.lock();
		try {
			return this.entries.isEmpty();
		} finally {
			this.readLock.unlock();
		}
	}

	@Override
	public Set<K> keySet() {
		return new AbstractSet<K>() {
			@Override
			public void clear() {
				ExpiringMap.this.clear();
			}

			@Override
			public boolean contains(final Object key) {
				return ExpiringMap.this.containsKey(key);
			}

			@Override
			public Iterator<K> iterator() {
				return ExpiringMap.this.entries instanceof EntryLinkedHashMap ? ((EntryLinkedHashMap<K, V>) ExpiringMap.this.entries).new KeyIterator()
						: ((EntryTreeHashMap<K, V>) ExpiringMap.this.entries).new KeyIterator();
			}

			@Override
			public boolean remove(final Object value) {
				return ExpiringMap.this.remove(value) != null;
			}

			@Override
			public int size() {
				return ExpiringMap.this.size();
			}
		};
	}

	/**
	 * Puts {@code value} in the map for {@code key}. Resets the entry's expiration
	 * unless an entry already exists for the same {@code key} and {@code value}.
	 *
	 * @param key   to put value for
	 * @param value to put for key
	 * @return the old value
	 * @throws NullPointerException if {@code key} is null
	 */
	@Override
	public V put(final K key, final V value) {
		ValidCore.checkNotNull(key, "key");
		return this.putInternal(key, value, this.expirationPolicy.get(), this.expirationNanos.get());
	}

	/**
	 * @param key
	 * @param value
	 * @param expirationPolicy
	 * @return
	 * @see #put(Object, Object, ExpirationPolicy, long, TimeUnit)
	 */
	public V put(final K key, final V value, final ExpirationPolicy expirationPolicy) {
		return this.put(key, value, expirationPolicy, this.expirationNanos.get(), TimeUnit.NANOSECONDS);
	}

	/**
	 * @param key
	 * @param value
	 * @param duration
	 * @param timeUnit
	 * @return
	 * @see #put(Object, Object, ExpirationPolicy, long, TimeUnit)
	 */
	public V put(final K key, final V value, final long duration, final TimeUnit timeUnit) {
		return this.put(key, value, this.expirationPolicy.get(), duration, timeUnit);
	}

	/**
	 * Puts {@code value} in the map for {@code key}. Resets the entry's expiration
	 * unless an entry already exists for the same {@code key} and {@code value}.
	 * Requires that variable expiration be enabled.
	 *
	 * @param key      Key to put value for
	 * @param value    Value to put for key
	 * @param expirationPolicy
	 * @param duration the length of time after an entry is created that it should
	 *                 be removed
	 * @param timeUnit the unit that {@code duration} is expressed in
	 * @return the old value
	 * @throws UnsupportedOperationException If variable expiration is not enabled
	 * @throws NullPointerException          if {@code key},
	 *                                       {@code expirationPolicy} or
	 *                                       {@code timeUnit} are null
	 */
	public V put(final K key, final V value, final ExpirationPolicy expirationPolicy, final long duration, final TimeUnit timeUnit) {
		ValidCore.checkNotNull(key, "key");
		ValidCore.checkNotNull(expirationPolicy, "expirationPolicy");
		ValidCore.checkNotNull(timeUnit, "timeUnit");
		ValidCore.checkBoolean(this.variableExpiration, "Variable expiration is not enabled");
		return this.putInternal(key, value, expirationPolicy, TimeUnit.NANOSECONDS.convert(duration, timeUnit));
	}

	@Override
	public void putAll(final Map<? extends K, ? extends V> map) {
		ValidCore.checkNotNull(map, "map");
		final long expiration = this.expirationNanos.get();
		final ExpirationPolicy expirationPolicy = this.expirationPolicy.get();
		this.writeLock.lock();
		try {
			for (final Map.Entry<? extends K, ? extends V> entry : map.entrySet())
				this.putInternal(entry.getKey(), entry.getValue(), expirationPolicy, expiration);
		} finally {
			this.writeLock.unlock();
		}
	}

	@Override
	public V putIfAbsent(final K key, final V value) {
		ValidCore.checkNotNull(key, "key");
		this.writeLock.lock();
		try {
			if (!this.entries.containsKey(key))
				return this.putInternal(key, value, this.expirationPolicy.get(), this.expirationNanos.get());
			else
				return this.entries.get(key).getValue();
		} finally {
			this.writeLock.unlock();
		}
	}

	@Override
	public V remove(final Object key) {
		ValidCore.checkNotNull(key, "key");
		this.writeLock.lock();
		try {
			final ExpiringEntry<K, V> entry = this.entries.remove(key);
			if (entry == null)
				return null;
			if (entry.cancel())
				this.scheduleEntry(this.entries.first());
			return entry.getValue();
		} finally {
			this.writeLock.unlock();
		}
	}

	@Override
	public boolean remove(final Object key, final Object value) {
		ValidCore.checkNotNull(key, "key");
		this.writeLock.lock();
		try {
			final ExpiringEntry<K, V> entry = this.entries.get(key);
			if (entry != null && entry.getValue().equals(value)) {
				this.entries.remove(key);
				if (entry.cancel())
					this.scheduleEntry(this.entries.first());
				return true;
			} else
				return false;
		} finally {
			this.writeLock.unlock();
		}
	}

	@Override
	public V replace(final K key, final V value) {
		ValidCore.checkNotNull(key, "key");
		this.writeLock.lock();
		try {
			if (this.entries.containsKey(key))
				return this.putInternal(key, value, this.expirationPolicy.get(), this.expirationNanos.get());
			else
				return null;
		} finally {
			this.writeLock.unlock();
		}
	}

	@Override
	public boolean replace(final K key, final V oldValue, final V newValue) {
		ValidCore.checkNotNull(key, "key");
		this.writeLock.lock();
		try {
			final ExpiringEntry<K, V> entry = this.entries.get(key);
			if (entry != null && entry.getValue().equals(oldValue)) {
				this.putInternal(key, newValue, this.expirationPolicy.get(), this.expirationNanos.get());
				return true;
			} else
				return false;
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Removes an expiration listener.
	 *
	 * @param listener
	 * @throws NullPointerException if {@code listener} is null
	 */
	public void removeExpirationListener(final ExpirationListener<K, V> listener) {
		ValidCore.checkNotNull(listener, "listener");
		for (int i = 0; i < this.expirationListeners.size(); i++)
			if (this.expirationListeners.get(i).equals(listener)) {
				this.expirationListeners.remove(i);
				return;
			}
	}

	/**
	 * Removes an asynchronous expiration listener.
	 *
	 * @param listener
	 * @throws NullPointerException if {@code listener} is null
	 */
	public void removeAsyncExpirationListener(final ExpirationListener<K, V> listener) {
		ValidCore.checkNotNull(listener, "listener");
		for (int i = 0; i < this.asyncExpirationListeners.size(); i++)
			if (this.asyncExpirationListeners.get(i).equals(listener)) {
				this.asyncExpirationListeners.remove(i);
				return;
			}
	}

	/**
	 * Resets expiration for the entry corresponding to {@code key}.
	 *
	 * @param key to reset expiration for
	 * @throws NullPointerException if {@code key} is null
	 */
	public void resetExpiration(final K key) {
		ValidCore.checkNotNull(key, "key");
		final ExpiringEntry<K, V> entry = this.getEntry(key);
		if (entry != null)
			this.resetEntry(entry, false);
	}

	/**
	 * Sets the expiration duration for the entry corresponding to the given key.
	 * Supported only if variable expiration is enabled.
	 *
	 * @param key      Key to set expiration for
	 * @param duration the length of time after an entry is created that it should
	 *                 be removed
	 * @param timeUnit the unit that {@code duration} is expressed in
	 * @throws NullPointerException          if {@code key} or {@code timeUnit} are
	 *                                       null
	 * @throws UnsupportedOperationException If variable expiration is not enabled
	 */
	public void setExpiration(final K key, final long duration, final TimeUnit timeUnit) {
		ValidCore.checkNotNull(key, "key");
		ValidCore.checkNotNull(timeUnit, "timeUnit");
		ValidCore.checkBoolean(this.variableExpiration, "Variable expiration is not enabled");
		this.writeLock.lock();
		try {
			final ExpiringEntry<K, V> entry = this.entries.get(key);
			if (entry != null) {
				entry.expirationNanos.set(TimeUnit.NANOSECONDS.convert(duration, timeUnit));
				this.resetEntry(entry, true);
			}
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Updates the default map entry expiration. Supported only if variable
	 * expiration is enabled.
	 *
	 * @param duration the length of time after an entry is created that it should
	 *                 be removed
	 * @param timeUnit the unit that {@code duration} is expressed in
	 * @throws NullPointerException          {@code timeUnit} is null
	 * @throws UnsupportedOperationException If variable expiration is not enabled
	 */
	public void setExpiration(final long duration, final TimeUnit timeUnit) {
		ValidCore.checkNotNull(timeUnit, "timeUnit");
		ValidCore.checkBoolean(this.variableExpiration, "Variable expiration is not enabled");
		this.expirationNanos.set(TimeUnit.NANOSECONDS.convert(duration, timeUnit));
	}

	/**
	 * Sets the global expiration policy for the map. Individual expiration policies
	 * may override the global policy.
	 *
	 * @param expirationPolicy
	 * @throws NullPointerException {@code expirationPolicy} is null
	 */
	public void setExpirationPolicy(final ExpirationPolicy expirationPolicy) {
		ValidCore.checkNotNull(expirationPolicy, "expirationPolicy");
		this.expirationPolicy.set(expirationPolicy);
	}

	/**
	 * Sets the expiration policy for the entry corresponding to the given key.
	 *
	 * @param key              to set policy for
	 * @param expirationPolicy to set
	 * @throws NullPointerException          if {@code key} or
	 *                                       {@code expirationPolicy} are null
	 * @throws UnsupportedOperationException If variable expiration is not enabled
	 */
	public void setExpirationPolicy(final K key, final ExpirationPolicy expirationPolicy) {
		ValidCore.checkNotNull(key, "key");
		ValidCore.checkNotNull(expirationPolicy, "expirationPolicy");
		ValidCore.checkBoolean(this.variableExpiration, "Variable expiration is not enabled");
		final ExpiringEntry<K, V> entry = this.getEntry(key);
		if (entry != null)
			entry.expirationPolicy.set(expirationPolicy);
	}

	/**
	 * Sets the maximum size of the map. Once this size has been reached, adding an
	 * additional entry will expire the first entry in line for expiration based on
	 * the expiration policy.
	 *
	 * @param maxSize The maximum size of the map.
	 */
	public void setMaxSize(final int maxSize) {
		ValidCore.checkBoolean(maxSize > 0, "maxSize");
		this.maxSize = maxSize;
	}

	@Override
	public int size() {
		this.readLock.lock();
		try {
			return this.entries.size();
		} finally {
			this.readLock.unlock();
		}
	}

	@Override
	public String toString() {
		this.readLock.lock();
		try {
			return this.entries.toString();
		} finally {
			this.readLock.unlock();
		}
	}

	@Override
	public Collection<V> values() {
		return new AbstractCollection<V>() {
			@Override
			public void clear() {
				ExpiringMap.this.clear();
			}

			@Override
			public boolean contains(final Object value) {
				return ExpiringMap.this.containsValue(value);
			}

			@Override
			public Iterator<V> iterator() {
				return ExpiringMap.this.entries instanceof EntryLinkedHashMap ? ((EntryLinkedHashMap<K, V>) ExpiringMap.this.entries).new ValueIterator()
						: ((EntryTreeHashMap<K, V>) ExpiringMap.this.entries).new ValueIterator();
			}

			@Override
			public int size() {
				return ExpiringMap.this.size();
			}
		};
	}

	/**
	 * Notifies expiration listeners that the given entry expired. Must not be
	 * called from within a locked context.
	 *
	 * @param entry Entry to expire
	 */
	void notifyListeners(final ExpiringEntry<K, V> entry) {
		if (this.asyncExpirationListeners != null)
			for (final ExpirationListener<K, V> listener : this.asyncExpirationListeners)
				LISTENER_SERVICE.execute(() -> {
					try {
						listener.expired(entry.key, entry.getValue());
					} catch (final Exception ignoreUserExceptions) {
					}
				});

		if (this.expirationListeners != null)
			for (final ExpirationListener<K, V> listener : this.expirationListeners)
				try {
					listener.expired(entry.key, entry.getValue());
				} catch (final Exception ignoreUserExceptions) {
				}
	}

	/**
	 * Returns the internal ExpiringEntry for the {@code key}, obtaining a read
	 * lock.
	 */
	ExpiringEntry<K, V> getEntry(final Object key) {
		this.readLock.lock();
		try {
			return this.entries.get(key);
		} finally {
			this.readLock.unlock();
		}
	}

	/**
	 * Puts the given key/value in storage, scheduling the new entry for expiration
	 * if needed. If a previous value existed for the given key, it is first
	 * cancelled and the entries reordered to reflect the new expiration.
	 */
	V putInternal(final K key, final V value, final ExpirationPolicy expirationPolicy, final long expirationNanos) {
		this.writeLock.lock();
		try {
			ExpiringEntry<K, V> entry = this.entries.get(key);
			V oldValue = null;

			if (entry == null) {
				entry = new ExpiringEntry<>(key, value,
						this.variableExpiration ? new AtomicReference<>(expirationPolicy) : this.expirationPolicy,
						this.variableExpiration ? new AtomicLong(expirationNanos) : this.expirationNanos);
				if (this.entries.size() >= this.maxSize) {
					final ExpiringEntry<K, V> expiredEntry = this.entries.first();
					this.entries.remove(expiredEntry.key);
					this.notifyListeners(expiredEntry);
				}
				this.entries.put(key, entry);
				if (this.entries.size() == 1 || this.entries.first().equals(entry))
					this.scheduleEntry(entry);
			} else {
				oldValue = entry.getValue();
				if (!ExpirationPolicy.ACCESSED.equals(expirationPolicy)
						&& (oldValue == null && value == null || oldValue != null && oldValue.equals(value)))
					return value;

				entry.setValue(value);
				this.resetEntry(entry, false);
			}

			return oldValue;
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Resets the given entry's schedule canceling any existing scheduled expiration
	 * and reordering the entry in the internal map. Schedules the next entry in the
	 * map if the given {@code entry} was scheduled or if {@code scheduleNext} is
	 * true.
	 *
	 * @param entry              to reset
	 * @param scheduleFirstEntry whether the first entry should be automatically
	 *                           scheduled
	 */
	void resetEntry(final ExpiringEntry<K, V> entry, final boolean scheduleFirstEntry) {
		this.writeLock.lock();
		try {
			final boolean scheduled = entry.cancel();
			this.entries.reorder(entry);

			if (scheduled || scheduleFirstEntry)
				this.scheduleEntry(this.entries.first());
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Schedules an entry for expiration. Guards against concurrent
	 * schedule/schedule, cancel/schedule and schedule/cancel calls.
	 *
	 * @param entry Entry to schedule
	 */
	void scheduleEntry(final ExpiringEntry<K, V> entry) {
		if (entry == null || entry.scheduled)
			return;

		Runnable runnable = null;
		synchronized (entry) {
			if (entry.scheduled)
				return;

			final WeakReference<ExpiringEntry<K, V>> entryReference = new WeakReference<>(entry);
			runnable = () -> {
				final ExpiringEntry<K, V> entry1 = entryReference.get();

				this.writeLock.lock();
				try {
					if (entry1 != null && entry1.scheduled) {
						this.entries.remove(entry1.key);
						this.notifyListeners(entry1);
					}

					try {
						// Expires entries and schedules the next entry
						final Iterator<ExpiringEntry<K, V>> iterator = this.entries.valuesIterator();
						boolean schedulePending = true;

						while (iterator.hasNext() && schedulePending) {
							final ExpiringEntry<K, V> nextEntry = iterator.next();
							if (nextEntry.expectedExpiration.get() <= System.nanoTime()) {
								iterator.remove();
								this.notifyListeners(nextEntry);
							} else {
								this.scheduleEntry(nextEntry);
								schedulePending = false;
							}
						}
					} catch (final NoSuchElementException ignored) {
					}
				} finally {
					this.writeLock.unlock();
				}
			};

			final Future<?> entryFuture = EXPIRER.schedule(runnable, entry.expectedExpiration.get() - System.nanoTime(),
					TimeUnit.NANOSECONDS);
			entry.schedule(entryFuture);
		}
	}

	private static <K, V> Map.Entry<K, V> mapEntryFor(final ExpiringEntry<K, V> entry) {
		return new Map.Entry<K, V>() {
			@Override
			public K getKey() {
				return entry.key;
			}

			@Override
			public V getValue() {
				return entry.value;
			}

			@Override
			public V setValue(final V value) {
				throw new UnsupportedOperationException();
			}
		};
	}
}
