package de.dytanic.cloudnet.service;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceTask;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

final class DefaultCloudServiceManagerConfiguration {

    private static final Type
            COLL_SERVICE_TASK = new TypeToken<Collection<ServiceTask>>() {
    }.getType(),
            COLL_GROUP_CONFIGURATION = new TypeToken<Collection<GroupConfiguration>>() {
            }.getType();

    private static final Path TASK_CONFIG_FILE = Paths.get(System.getProperty("cloudnet.config.task.path", "local/tasks.json"));

    private final List<ServiceTask> tasks = Iterables.newCopyOnWriteArrayList();

    private final List<GroupConfiguration> groups = Iterables.newCopyOnWriteArrayList();

    public DefaultCloudServiceManagerConfiguration() {
        if (!Files.exists(TASK_CONFIG_FILE))
            this.save();

        this.load();
    }

    public void load() {
        this.tasks.clear();
        this.groups.clear();

        JsonDocument document = JsonDocument.newDocument(TASK_CONFIG_FILE);

        this.tasks.addAll(document.get("tasks", COLL_SERVICE_TASK));
        this.groups.addAll(document.get("groups", COLL_GROUP_CONFIGURATION));
    }

    public void save() {
        new JsonDocument("groups", groups).append("tasks", tasks).write(TASK_CONFIG_FILE);
    }

    public List<ServiceTask> getTasks() {
        return this.tasks;
    }

    public List<GroupConfiguration> getGroups() {
        return this.groups;
    }
}