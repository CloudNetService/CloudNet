package de.dytanic.cloudnet.ext.storage.ftp.client;

import de.dytanic.cloudnet.ext.storage.ftp.storage.AbstractFTPStorage;
import de.dytanic.cloudnet.ext.storage.ftp.storage.FTPTemplateStorage;
import de.dytanic.cloudnet.ext.storage.ftp.storage.SFTPTemplateStorage;

import java.util.function.BiFunction;

public enum FTPType {
    SFTP(SFTPTemplateStorage::new),
    FTP((name, credentials) -> new FTPTemplateStorage(name, credentials, false)),
    FTPS((name, credentials) -> new FTPTemplateStorage(name, credentials, true));

    private BiFunction<String, FTPCredentials, AbstractFTPStorage> storageProvider;

    FTPType(BiFunction<String, FTPCredentials, AbstractFTPStorage> storageProvider) {
        this.storageProvider = storageProvider;
    }

    public AbstractFTPStorage createNewTemplateStorage(String storage, FTPCredentials credentials) {
        return this.storageProvider.apply(storage, credentials);
    }

}
