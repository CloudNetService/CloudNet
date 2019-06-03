package de.dytanic.cloudnet.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A class that provides the reference of an object.
 * It is interesting for asynchronous operations or anonymous classes.
 *
 * @param <T> the type of value, that you want to set
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Value<T> {

    /**
     * The reference of an object, which is held and retrievable by an instance of this class.
     * It is protected to allow inherit
     */
    protected volatile T value;

}