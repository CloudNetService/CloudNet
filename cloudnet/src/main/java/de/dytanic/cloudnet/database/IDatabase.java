package de.dytanic.cloudnet.database;

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.driver.database.Database;
import org.jetbrains.annotations.ApiStatus;

/**
 * @deprecated Replace with {@link Database}
 */
@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "3.6")
public interface IDatabase extends Database, INameable, AutoCloseable {

    AbstractDatabaseProvider getDatabaseProvider();

}