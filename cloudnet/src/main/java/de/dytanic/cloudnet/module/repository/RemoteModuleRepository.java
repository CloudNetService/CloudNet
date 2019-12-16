package de.dytanic.cloudnet.module.repository;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.module.ModuleId;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Collections;

public class RemoteModuleRepository implements ModuleRepository {

    private String baseUrl = System.getProperty("cloudnet.modules.repository.url", "https://api.cloudnetservice.eu/");

    @Override
    public boolean isReachable() {
        try {
            URLConnection connection = new URL(this.baseUrl + "modules").openConnection();
            try (InputStream inputStream = connection.getInputStream()) {
                JsonDocument document = JsonDocument.newDocument().read(inputStream);
                return document.getBoolean("available");
            }
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    @Override
    public Collection<RepositoryModuleInfo> loadAvailableModules() {
        try {
            URLConnection connection = new URL(this.baseUrl + "modules/list").openConnection();
            try (InputStream inputStream = connection.getInputStream()) {
                JsonDocument document = JsonDocument.newDocument().read(inputStream);
                return document.get("modules", TypeToken.getParameterized(Collection.class, RepositoryModuleInfo.class).getType());
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return Collections.emptyList();
    }

    @Override
    public RepositoryModuleInfo loadRepositoryModuleInfo(ModuleId moduleId) {
        try {
            URLConnection connection = new URL(moduleId.getVersion() != null ?
                    this.baseUrl + String.format("modules/list/%s/%s", moduleId.getGroup().replace('.', '/'), moduleId.getName()) :
                    this.baseUrl + String.format("modules/list/%s/%s/%s", moduleId.getGroup().replace('.', '/'), moduleId.getName(), moduleId.getVersion())
            ).openConnection();
            try (InputStream inputStream = connection.getInputStream()) {
                JsonDocument document = JsonDocument.newDocument().read(inputStream);
                return document.toInstanceOf(RepositoryModuleInfo.class);
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return null;
    }
}
