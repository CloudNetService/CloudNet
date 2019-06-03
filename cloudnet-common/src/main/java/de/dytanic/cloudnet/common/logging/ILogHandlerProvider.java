package de.dytanic.cloudnet.common.logging;

/**
 * An interface for a provider, which provides all log handlers for a log handlers
 */
public interface ILogHandlerProvider<T extends ILogHandlerProvider> {

    /**
     * Adds a new ILogHandler instance, into the collection by the LogHandlerProvider implementation
     *
     * @param logHandler the ILogHandler implementation, which should append
     * @return the current implementation of the ILogHandlerProvider
     */
    T addLogHandler(ILogHandler logHandler);

    /**
     * Adds an array of ILogHandler instances, into the collection by the LogHandlerProvider implementation
     *
     * @param logHandlers the ILogHandler's implementation, which should append
     * @return the current implementation of the ILogHandlerProvider
     */
    T addLogHandlers(ILogHandler... logHandlers);

    /**
     * Adds an Iterable of ILogHandler instances, into the collection by the LogHandlerProvider implementation
     *
     * @param logHandlers the ILogHandler's implementation, which should append
     * @return the current implementation of the ILogHandlerProvider
     */
    T addLogHandlers(Iterable<ILogHandler> logHandlers);

    /**
     * Removes when contains the ILogHandler reference into the internal registry
     *
     * @param logHandler the logHandler, which should be removed
     * @return the current implementation of the ILogHandlerProvider
     */
    T removeLogHandler(ILogHandler logHandler);

    /**
     * Removes when contains the ILogHandler's reference into the internal registry
     *
     * @param logHandlers the ILogHandler's, which should be removed
     * @return the current implementation of the ILogHandlerProvider
     */
    T removeLogHandlers(ILogHandler... logHandlers);

    /**
     * Removes when contains the ILogHandler's reference into the internal registry
     *
     * @param logHandlers the ILogHandler's, which should be removed
     * @return the current implementation of the ILogHandlerProvider
     */
    T removeLogHandlers(Iterable<ILogHandler> logHandlers);

    /**
     * Returns all registered ILogHandler instances as an Iterable
     */
    Iterable<ILogHandler> getLogHandlers();

    /**
     * Check that the ILogHandler exists on this provider
     *
     * @param logHandler the ILogHandler, that should test
     * @return true if the ILogHandler instance contain on this LogHandlerProvider object
     */
    boolean hasLogHandler(ILogHandler logHandler);

    /**
     * Check that the ILogHandler's exists on this provider
     *
     * @param logHandlers the ILogHandler's, that should test
     * @return true if the ILogHandler's instances contain on this LogHandlerProvider object
     */
    boolean hasLogHandlers(ILogHandler... logHandlers);
}