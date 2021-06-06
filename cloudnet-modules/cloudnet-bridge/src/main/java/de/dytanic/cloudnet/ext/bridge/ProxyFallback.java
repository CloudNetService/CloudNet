package de.dytanic.cloudnet.ext.bridge;

import java.util.ArrayList;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class ProxyFallback implements Comparable<ProxyFallback> {

  protected String task, permission;

  protected Collection<String> availableOnGroups = new ArrayList<>();

  protected String forcedHost;

  private int priority;

  public ProxyFallback(String task, String permission, int priority) {
    this.task = task;
    this.permission = permission;
    this.priority = priority;
  }

  public ProxyFallback() {
  }

  @Override
  public int compareTo(ProxyFallback o) {
    return Integer.compare(o.priority, this.priority);
  }

  public String getTask() {
    return this.task;
  }

  public void setTask(String task) {
    this.task = task;
  }

  public String getPermission() {
    return this.permission;
  }

  public void setPermission(String permission) {
    this.permission = permission;
  }

  public Collection<String> getAvailableOnGroups() {
    return this.availableOnGroups;
  }

  public void setAvailableOnGroups(Collection<String> availableOnGroups) {
    this.availableOnGroups = availableOnGroups;
  }

  public String getForcedHost() {
    return this.forcedHost;
  }

  public void setForcedHost(String forcedHost) {
    this.forcedHost = forcedHost;
  }

  public int getPriority() {
    return this.priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

}
