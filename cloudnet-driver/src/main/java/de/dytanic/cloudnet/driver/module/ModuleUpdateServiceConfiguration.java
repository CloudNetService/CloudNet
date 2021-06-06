package de.dytanic.cloudnet.driver.module;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class ModuleUpdateServiceConfiguration {

  protected boolean autoInstall;

  protected String url, currentVersion, infoMessage;

  public ModuleUpdateServiceConfiguration(boolean autoInstall, String url, String currentVersion, String infoMessage) {
    this.autoInstall = autoInstall;
    this.url = url;
    this.currentVersion = currentVersion;
    this.infoMessage = infoMessage;
  }

  public ModuleUpdateServiceConfiguration() {
  }

  public boolean isAutoInstall() {
    return this.autoInstall;
  }

  public void setAutoInstall(boolean autoInstall) {
    this.autoInstall = autoInstall;
  }

  public String getUrl() {
    return this.url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getCurrentVersion() {
    return this.currentVersion;
  }

  public void setCurrentVersion(String currentVersion) {
    this.currentVersion = currentVersion;
  }

  public String getInfoMessage() {
    return this.infoMessage;
  }

  public void setInfoMessage(String infoMessage) {
    this.infoMessage = infoMessage;
  }

}
