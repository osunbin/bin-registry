package com.bin.registry.server.common.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class Pool<K, V> {

    private ConcurrentMap<K, V> pool = new ConcurrentHashMap<>();
    private Object createLock = new Object();

    private Function<K, V> valueFactory;

    public Pool(Function<K, V> factory) {
        this.valueFactory = factory;
    }


    public V put(K k, V v) {
        return pool.put(k, v);
    }


    public V putIfNotExists(K k, V v) {
        return pool.putIfAbsent(k, v);
    }


    public V getAndMaybePut(K key) {
        if (valueFactory == null)
            throw new RuntimeException("Empty value factory in pool.");
       return getAndMaybePut(key, valueFactory.apply(key));
    }

    ;

    public V getAndMaybePut(K key, V createValue) {
        V current = pool.get(key);
        if (current == null) {
            synchronized (createLock) {
                current = pool.get(key);
                if (current == null) {
                    V value = createValue;
                    pool.put(key, value);
                    return value;
                } else
                    return current;
            }
        } else
            return current;
    }

    public boolean contains(K id) {
       return  pool.containsKey(id);
    }


    public V get(K key) {
        return pool.get(key);
    }


    public V remove(K key) {
        return pool.remove(key);
    }


    public boolean remove(K key,V value) {
        return pool.remove(key, value);
    }


    public Set<K> keys() {
        return pool.keySet();
    }

    public Iterable<V> values() {
        return pool.values();
    }


    public void clear() {
        pool.clear();
    }

    public int size() {
        return pool.size();
    }
}