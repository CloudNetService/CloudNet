package de.dytanic.cloudnet.ext.storage.ftp.client;

import de.dytanic.cloudnet.ext.storage.ftp.storage.FTPTemplateStorage;
import de.dytanic.cloudnet.ext.storage.ftp.storage.SFTPTemplateStorage;
import de.dytanic.cloudnet.template.ITemplateStorage;

import java.util.function.BiFunction;

public enum FTPType {
    SFTP(SFTPTemplateStorage::new),
    FTP((name, credentials) -> new FTPTemplateStorage(name, credentials, false)),
    FTPS((name, credentials) -> new FTPTemplateStorage(name, credentials, true));

    private BiFunction<String, FTPCredentials, ITemplateStorage> storageProvider;

    FTPType(BiFunction<String, FTPCredentials, ITemplateStorage> storageProvider) {
        this.storageProvider = storageProvider;
    }

    public ITemplateStorage createNewTemplateStorage(String storage, FTPCredentials credentials) {
        return this.storageProvider.apply(storage, credentials);
    }
}
