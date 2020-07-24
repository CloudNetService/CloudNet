package de.dytanic.cloudnet.common.collection;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NetorHashMap<K, F, S> {

    protected ConcurrentHashMap<K, Pair<F, S>> wrapped = new ConcurrentHashMap<>();

    public Set<Map.Entry<K, Pair<F, S>>> entrySet() {
        return this.wrapped.entrySet();
    }

    public void clear() {
        this.wrapped.clear();
    }

    public int size() {
        return this.wrapped.size();
    }

    public void add(K key, F valueF, S valueS) {
        this.wrapped.put(key, new Pair<>(valueF, valueS));
    }

    public void remove(K key) {
        this.wrapped.remove(key);
    }

    public Set<K> keySet() {
        return this.wrapped.keySet();
    }

    public boolean contains(K key) {
        return this.wrapped.containsKey(key);
    }

    public Pair<F, S> get(K key) {
        return this.wrapped.get(key);
    }

    public F getFirst(K key) {
        return this.wrapped.get(key).getFirst();
    }

    public S getSecond(K key) {
        return this.wrapped.get(key).getSecond();
    }

    public void replaceFirst(K key, F value) {
        this.wrapped.get(key).setFirst(value);
    }

    public void replaceSecond(K key, S value) {
        this.wrapped.get(key).setSecond(value);
    }

}
