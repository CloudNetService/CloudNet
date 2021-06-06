package de.dytanic.cloudnet.ext.storage.ftp.storage;

import de.dytanic.cloudnet.driver.template.defaults.DefaultSyncTemplateStorage;
import de.dytanic.cloudnet.ext.storage.ftp.client.FTPCredentials;
import de.dytanic.cloudnet.ext.storage.ftp.client.FTPType;

public abstract class AbstractFTPStorage extends DefaultSyncTemplateStorage {

  protected final FTPCredentials credentials;
  protected final FTPType ftpType;
  protected final String baseDirectory;
  private final String name;

  AbstractFTPStorage(String name, FTPCredentials credentials, FTPType ftpType) {
    this.name = name;
    this.credentials = credentials;
    this.ftpType = ftpType;

    String baseDirectory = credentials.getBaseDirectory();

    this.baseDirectory =
      baseDirectory.endsWith("/") ? baseDirectory.substring(0, baseDirectory.length() - 1) : baseDirectory;
  }

  public abstract boolean connect();

  public abstract boolean isAvailable();

  public abstract void completeDataTransfer();

  @Override
  public String getName() {
    return this.name;
  }

  public FTPCredentials getCredentials() {
    return this.credentials;
  }

  public FTPType getFtpType() {
    return this.ftpType;
  }

  public String getBaseDirectory() {
    return this.baseDirectory;
  }

}
