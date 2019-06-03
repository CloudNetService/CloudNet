package de.dytanic.cloudnet.common.collection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;

public final class Maps {

    private Maps() {
        throw new UnsupportedOperationException();
    }

    public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<>();
    }

    public static <K, V> HashMap<K, V> newHashMap(int initialSize) {
        return new HashMap<>(initialSize);
    }

    public static <K, V> HashMap<K, V> newHashMap(Map<K, V> map) {
        return new HashMap<>(map);
    }

    public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap() {
        return new ConcurrentHashMap<>(0);
    }

    public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap(int initialSize) {
        return new ConcurrentHashMap<>(initialSize);
    }

    public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap(Map<K, V> map) {
        return new ConcurrentHashMap<>(map);
    }

    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap() {
        return new LinkedHashMap<>();
    }

    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(int initialSize) {
        return new LinkedHashMap<>(initialSize);
    }

    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(Map<K, V> map) {
        return new LinkedHashMap<>(map);
    }

    public static <K, V> ConcurrentSkipListMap<K, V> newConcurrentSkipListMap() {
        return new ConcurrentSkipListMap<>();
    }

    public static <K, V> ConcurrentSkipListMap<K, V> newConcurrentSkipListMap(Map<K, V> map) {
        return new ConcurrentSkipListMap<>(map);
    }

    public static <K, V> WeakHashMap<K, V> newWeakHashMap() {
        return new WeakHashMap<>(0);
    }

    public static <K, V> WeakHashMap<K, V> newWeakHashMap(int capacity) {
        return new WeakHashMap<>(capacity);
    }

    public static <K, V> WeakHashMap<K, V> newWeakHashMap(Map<K, V> map) {
        return new WeakHashMap<>(map);
    }

    public static <K, V> IdentityHashMap<K, V> newIdentityHashMap() {
        return new IdentityHashMap<>();
    }

    public static <K, V> IdentityHashMap<K, V> newIdentityHashMap(int initialSize) {
        return new IdentityHashMap<>(initialSize);
    }

    public static <K, V> IdentityHashMap<K, V> newIdentityHashMap(Map<K, V> map) {
        return new IdentityHashMap<>(map);
    }

    public static <K, V> TreeMap<K, V> newTreeMap() {
        return new TreeMap<>();
    }

    public static <K, V> TreeMap<K, V> newTreeMap(Map<K, V> map) {
        return new TreeMap<>(map);
    }

    public static <K extends Enum<K>, V> EnumMap<K, V> newEnumMap(Class<K> clazz) {
        return new EnumMap<>(clazz);
    }

    /*= --------------------------------------------------------------------------- =*/

    public static <K, V> Map<K, V> newMapByValues(Collection<V> collection, Function<V, K> function) {
        if (collection == null || function == null) return null;

        Map<K, V> map = newHashMap(collection.size());

        for (V entry : collection) map.put(function.apply(entry), entry);

        return map;
    }

    public static <K, V> Map<K, V> newMapByKeys(Collection<K> collection, Function<K, V> function) {
        if (collection == null || function == null) return null;

        Map<K, V> map = newHashMap(collection.size());

        for (K entry : collection) map.put(entry, function.apply(entry));

        return map;
    }

    public static <K, V> Map<K, V> of(Pair<K, V>... pairs) {
        if (pairs == null) return newHashMap();

        Map<K, V> map = newHashMap(pairs.length);

        for (Pair<K, V> pair : pairs)
            if (pair != null)
                map.put(pair.getFirst(), pair.getSecond());

        return map;
    }
}