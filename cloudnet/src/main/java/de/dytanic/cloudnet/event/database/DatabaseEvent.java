package de.dytanic.cloudnet.event.database;

import de.dytanic.cloudnet.database.IDatabase;
import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
abstract class DatabaseEvent extends DriverEvent {

    private final IDatabase database;

}