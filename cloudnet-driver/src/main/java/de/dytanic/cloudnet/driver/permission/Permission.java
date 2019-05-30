package de.dytanic.cloudnet.driver.permission;

import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public final class Permission {

  private final String name;

  private int potency;

  private long timeOutMillis;

  public Permission(String name, int potency) {
    this.name = name;
    this.potency = potency;
  }

  public Permission(String name, int potency, long time, TimeUnit timeUnit) {
    this.name = name;
    this.potency = potency;
    this.timeOutMillis = System.currentTimeMillis() + timeUnit.toMillis(time);
  }
}