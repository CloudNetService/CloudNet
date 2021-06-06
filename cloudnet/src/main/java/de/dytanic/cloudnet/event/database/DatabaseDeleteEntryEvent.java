package de.dytanic.cloudnet.event.database;

import de.dytanic.cloudnet.database.IDatabase;

public class DatabaseDeleteEntryEvent extends DatabaseEvent {

  private String key;

  public DatabaseDeleteEntryEvent(IDatabase database, String key) {
    super(database);

    this.key = key;
  }

  public String getKey() {
    return this.key;
  }

  public void setKey(String key) {
    this.key = key;
  }
}
