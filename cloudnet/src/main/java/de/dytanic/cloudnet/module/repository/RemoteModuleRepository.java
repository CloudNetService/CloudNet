package de.dytanic.cloudnet.module.repository;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.module.ModuleId;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

public class RemoteModuleRepository implements ModuleRepository {

    public static final String VERSION_PARENT = "v3";

    private String baseUrl = System.getProperty("cloudnet.modules.repository.url", "https://cloudnetservice.eu/api");

    @Override
    public boolean isReachable() {
        try {
            URLConnection connection = new URL(this.baseUrl).openConnection();
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
    public String getBaseURL() {
        return this.baseUrl;
    }

    @Override
    public Collection<RepositoryModuleInfo> loadAvailableModules() {
        try {
            URLConnection connection = new URL(this.baseUrl + VERSION_PARENT + "/modules/list").openConnection();
            try (InputStream inputStream = connection.getInputStream();
                 Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                Collection<RepositoryModuleInfo> moduleInfos = JsonDocument.GSON.fromJson(reader, TypeToken.getParameterized(Collection.class, RepositoryModuleInfo.class).getType());
                if (moduleInfos != null) {
                    return moduleInfos;
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return Collections.emptyList();
    }

    @Override
    public RepositoryModuleInfo loadRepositoryModuleInfo(ModuleId moduleId) {
        try {
            URLConnection connection = new URL(
                    this.baseUrl + String.format("%s/modules/latest/%s/%s", VERSION_PARENT, moduleId.getGroup(), moduleId.getName())
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
