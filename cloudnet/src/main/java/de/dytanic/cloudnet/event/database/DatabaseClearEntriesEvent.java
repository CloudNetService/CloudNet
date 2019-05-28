package de.dytanic.cloudnet.event.database;

import de.dytanic.cloudnet.database.IDatabase;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DatabaseClearEntriesEvent extends DatabaseEvent {

  public DatabaseClearEntriesEvent(IDatabase database) {
    super(database);
  }
}