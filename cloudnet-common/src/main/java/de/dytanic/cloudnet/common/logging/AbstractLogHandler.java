package de.dytanic.cloudnet.common.logging;

import org.jetbrains.annotations.NotNull;

/**
 * This is a basic abstract implementation of the ILogHandler class.
 * It should help, to create a simple
 */
public abstract class AbstractLogHandler implements ILogHandler {

    /**
     * A formatter with a default initialization value with the DefaultLogFormatter class.
     *
     * @see DefaultLogFormatter
     */
    protected IFormatter formatter;

    public AbstractLogHandler() {
        this(new DefaultLogFormatter());
    }

    public AbstractLogHandler(@NotNull IFormatter formatter) {
        this.formatter = formatter;
    }

    public @NotNull IFormatter getFormatter() {
        return this.formatter;
    }

    /**
     * Set the new formatter
     *
     * @return the current instance of the AbstractLogHandler class
     */
    public @NotNull AbstractLogHandler setFormatter(@NotNull IFormatter formatter) {
        this.formatter = formatter;
        return this;
    }
}