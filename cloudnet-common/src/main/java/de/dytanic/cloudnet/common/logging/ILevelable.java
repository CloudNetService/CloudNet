package de.dytanic.cloudnet.common.logging;

import org.jetbrains.annotations.NotNull;

/**
 * Allows an object to has an specific LogLevel as integer value
 *
 * @see LogLevel
 */
interface ILevelable {

    /**
     * Returns the current configured access level. All log entries under this level can be
     * only noticed
     */
    int getLevel();

    default boolean isLogging(int level) {
        return this.getLevel() >= level;
    }

    default boolean isLogging(@NotNull LogLevel level) {
        return this.isLogging(level.getLevel());
    }

}