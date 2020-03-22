package de.dytanic.cloudnet.ext.storage.ftp.client;

import de.dytanic.cloudnet.ext.storage.ftp.storage.AbstractFTPStorage;
import de.dytanic.cloudnet.ext.storage.ftp.storage.FTPTemplateStorage;
import de.dytanic.cloudnet.ext.storage.ftp.storage.SFTPTemplateStorage;

public enum FTPType {
    SFTP {
        @Override
        public AbstractFTPStorage createNewTemplateStorage(String storage, FTPCredentials credentials) {
            return new SFTPTemplateStorage(storage, credentials);
        }
    },
    FTP {
        @Override
        public AbstractFTPStorage createNewTemplateStorage(String storage, FTPCredentials credentials) {
            return new FTPTemplateStorage(storage, credentials, false);
        }
    },
    FTPS {
        @Override
        public AbstractFTPStorage createNewTemplateStorage(String storage, FTPCredentials credentials) {
            return new FTPTemplateStorage(storage, credentials, true);
        }
    };


    public abstract AbstractFTPStorage createNewTemplateStorage(String storage, FTPCredentials credentials);

}
