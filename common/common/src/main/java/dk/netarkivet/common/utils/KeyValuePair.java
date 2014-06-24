
package dk.netarkivet.common.utils;

import java.util.Map;

/**
 * A generic Map.Entry class, useful for returning key-value-like results.
 * @param <K> the Object type used as key
 * @param <V> the Object type used as value
 */
public class KeyValuePair<K, V> implements Map.Entry<K, V> {
    /** The key in this key-value pair. */
    private final K key;
    /** The value in this key-value pair. */
    private final V value;
    
    /**
     * Constructs a Key-Value pair using the given key and value.
     * @param k The key object
     * @param v The value object
     */
    public KeyValuePair(K k, V v) {
        this.key = k;
        this.value = v;
    }

    /**
     * Returns the key corresponding to this entry.
     *
     * @return the key corresponding to this entry.
     */
    public K getKey() {
        return key;
    }

    /**
     * Returns the value corresponding to this entry.
     *
     * @return the value corresponding to this entry.
     */
    public V getValue() {
        return value;
    }

    /**
     * Replaces the value corresponding to this entry with the specified value
     * (optional operation).
     * 
     * @param newValue
     *            new value to be stored in this entry.
     * @return old value corresponding to the entry.
     * @throws UnsupportedOperationException
     *             if the <tt>put</tt> operation is not supported by the backing
     *             map.
     */
    public V setValue(V newValue) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Stand-alone entries cannot "
                + "be changed");
    }
}
