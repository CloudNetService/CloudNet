package de.dytanic.cloudnet.driver.module.repository;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.module.ModuleId;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

public class RemoteModuleRepository implements ModuleRepository {

    public static final String VERSION_PARENT = "v3";

    private final String baseUrl = System.getProperty("cloudnet.modules.repository.url", "https://cloudnetservice.eu/api");

    private Collection<RepositoryModuleInfo> remoteInfos;
    private boolean cache;

    public void enableCache() {
        this.cache = true;
    }

    @Override
    public boolean isReachable() {
        try {
            try (InputStream inputStream = new URL(this.baseUrl).openStream()) {
                JsonDocument document = JsonDocument.newDocument().read(inputStream);
                return document.getBoolean("available");
            }
        } catch (IOException exception) {
            System.err.println(LanguageManager.getMessage("cloudnet-module-repository-load-failed").replace("%url%", this.baseUrl).replace("%error%", exception.getLocalizedMessage()));
            return false;
        }
    }

    @Override
    public @NotNull String getBaseURL() {
        return this.baseUrl;
    }

    @Override
    public @NotNull String getModuleURL(@NotNull ModuleId moduleId) {
        return String.format("%s/%s/modules/file/%s/%s", this.baseUrl, VERSION_PARENT, moduleId.getGroup(), moduleId.getName());
    }

    @Override
    public @NotNull Collection<RepositoryModuleInfo> loadAvailableModules() {
        try {
            try (InputStream inputStream = new URL(this.baseUrl + "/" + VERSION_PARENT + "/modules/list").openStream();
                 Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                Collection<RepositoryModuleInfo> moduleInfos = JsonDocument.GSON.fromJson(reader, TypeToken.getParameterized(Collection.class, RepositoryModuleInfo.class).getType());
                if (moduleInfos != null) {
                    return this.remoteInfos = moduleInfos;
                }
            }
        } catch (IOException exception) {
            System.err.println(LanguageManager.getMessage("cloudnet-module-repository-load-failed").replace("%url%", this.baseUrl).replace("%error%", exception.getLocalizedMessage()));
        }
        return this.remoteInfos = Collections.emptyList();
    }

    @Override
    public @NotNull Collection<RepositoryModuleInfo> getAvailableModules() {
        if (!this.cache || this.remoteInfos == null) {
            return this.loadAvailableModules();
        }

        return this.remoteInfos;
    }

    @Override
    public RepositoryModuleInfo loadRepositoryModuleInfo(@NotNull ModuleId moduleId) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(
                    this.baseUrl + String.format("/%s/modules/latest/%s/%s", VERSION_PARENT, moduleId.getGroup(), moduleId.getName())
            ).openConnection();

            connection.connect();
            if (connection.getResponseCode() != 200) {
                return null;
            }

            try (InputStream inputStream = connection.getInputStream()) {
                JsonDocument document = JsonDocument.newDocument().read(inputStream);
                return document.toInstanceOf(RepositoryModuleInfo.class);
            }
        } catch (IOException exception) {
            System.err.println(LanguageManager.getMessage("cloudnet-module-repository-load-failed").replace("%url%", this.baseUrl).replace("%error%", exception.getLocalizedMessage()));
        }
        return null;
    }

    @Override
    public RepositoryModuleInfo getRepositoryModuleInfo(@NotNull ModuleId moduleId) {
        if (!this.cache) {
            return this.loadRepositoryModuleInfo(moduleId);
        }
        if (this.remoteInfos == null) {
            this.loadAvailableModules();
        }
        return this.remoteInfos.stream()
                .filter(moduleInfo -> moduleInfo.getModuleId().equalsIgnoreVersion(moduleId))
                .findFirst()
                .orElse(null);
    }

}
