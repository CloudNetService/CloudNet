package de.dytanic.cloudnet.common;

/**
 * Allows a class, to has a name removes the declaration that a class should have a name
 */
@Deprecated
public interface INameable {

    /**
     * Returns the name of the current class instance
     *
     * @return the name of the instance and cannot be null
     */
    String getName();

}