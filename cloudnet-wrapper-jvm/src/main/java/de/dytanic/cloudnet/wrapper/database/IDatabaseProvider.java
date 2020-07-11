package de.dytanic.cloudnet.wrapper.database;

import de.dytanic.cloudnet.driver.database.DatabaseProvider;
import org.jetbrains.annotations.ApiStatus;

/**
 * @deprecated Replace with {@link DatabaseProvider}
 */
@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "3.6")
public interface IDatabaseProvider extends DatabaseProvider {
}
