package de.dytanic.cloudnet.util;

public interface Identity<T> {

    T instance();

    boolean isInstance(T t);
}
