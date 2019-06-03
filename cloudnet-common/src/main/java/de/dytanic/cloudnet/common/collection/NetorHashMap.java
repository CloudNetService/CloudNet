package de.dytanic.cloudnet.common.collection;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NetorHashMap<K, F, S> {

    protected ConcurrentHashMap<K, Pair<F, S>> wrapped = new ConcurrentHashMap<>();

    public Set<Map.Entry<K, Pair<F, S>>> entrySet() {
        return wrapped.entrySet();
    }

    public void clear() {
        wrapped.clear();
    }

    public int size() {
        return wrapped.size();
    }

    public void add(K key, F valueF, S valueS) {
        wrapped.put(key, new Pair<>(valueF, valueS));
    }

    public void remove(K key) {
        wrapped.remove(key);
    }

    public Set<K> keySet() {
        return wrapped.keySet();
    }

    public boolean contains(K key) {
        return wrapped.containsKey(key);
    }

    public Pair<F, S> get(K key) {
        return wrapped.get(key);
    }

    public F getFirst(K key) {
        return wrapped.get(key).getFirst();
    }

    public S getSecond(K key) {
        return wrapped.get(key).getSecond();
    }

    public void replaceFirst(K key, F value) {
        wrapped.get(key).setFirst(value);
    }

    public void replaceSecond(K key, S value) {
        wrapped.get(key).setSecond(value);
    }

}
