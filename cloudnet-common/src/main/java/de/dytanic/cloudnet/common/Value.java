package de.dytanic.cloudnet.common;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A class that provides the reference of an object.
 * It is interesting for asynchronous operations or anonymous classes.
 *
 * @param <T> the type of value, that you want to set
 */
@ToString
@EqualsAndHashCode
public class Value<T> {

    /**
     * The reference of an object, which is held and retrievable by an instance of this class.
     * It is protected to allow inherit
     */
    protected volatile T value;

    public Value(T value) {
        this.value = value;
    }

    public Value() {
    }

    public T getValue() {
        return this.value;
    }

    public void setValue(T value) {
        this.value = value;
    }

}