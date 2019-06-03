package de.dytanic.cloudnet.common.logging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * This is a basic abstract implementation of the ILogHandler class.
 * It should help, to create a simple
 */
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractLogHandler implements ILogHandler {

    /**
     * A formatter with a default initialization value with the DefaultLogFormatter class.
     *
     * @see DefaultLogFormatter
     */
    @Getter
    protected IFormatter formatter = new DefaultLogFormatter();

    /**
     * Set the new formatter
     *
     * @return the current instance of the AbstractLogHandler class
     */
    public AbstractLogHandler setFormatter(IFormatter formatter) {
        this.formatter = formatter;
        return this;
    }

    @Override
    public void close() throws Exception {

    }
}