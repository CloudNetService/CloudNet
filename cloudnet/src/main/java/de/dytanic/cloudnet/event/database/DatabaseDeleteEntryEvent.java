package de.dytanic.cloudnet.event.database;

import de.dytanic.cloudnet.database.IDatabase;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DatabaseDeleteEntryEvent extends DatabaseEvent {

  private String key;

  public DatabaseDeleteEntryEvent(IDatabase database, String key) {
    super(database);

    this.key = key;
  }
}