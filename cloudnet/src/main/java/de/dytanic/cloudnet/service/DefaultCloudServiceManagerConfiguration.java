package de.dytanic.cloudnet.service;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceTask;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;

final class DefaultCloudServiceManagerConfiguration {

    private static final Type
            COLL_SERVICE_TASK = new TypeToken<Collection<ServiceTask>>() {
    }.getType(),
            COLL_GROUP_CONFIGURATION = new TypeToken<Collection<GroupConfiguration>>() {
            }.getType();

    private static final Path OLD_TASK_CONFIG_FILE = Paths.get(System.getProperty("cloudnet.config.task.path", "local/tasks.json"));
    private static final Path GROUPS_CONFIG_FILE = Paths.get(System.getProperty("cloudnet.config.groups.path", "local/groups.json"));
    private static final Path TASKS_DIRECTORY = Paths.get(System.getProperty("cloudnet.config.tasks.directory.path", "local/tasks"));

    private final List<ServiceTask> tasks = Iterables.newCopyOnWriteArrayList();

    private final List<GroupConfiguration> groups = Iterables.newCopyOnWriteArrayList();

    public void load() {
        if (Files.exists(OLD_TASK_CONFIG_FILE)) {
            JsonDocument document = JsonDocument.newDocument(OLD_TASK_CONFIG_FILE);
            this.tasks.addAll(document.get("tasks", COLL_SERVICE_TASK));
            this.groups.addAll(document.get("groups", COLL_GROUP_CONFIGURATION));
            this.save();

            try {
                Files.delete(OLD_TASK_CONFIG_FILE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.loadGroups();
        this.loadTasks();
    }

    private void loadGroups() {
        this.groups.clear();
        JsonDocument document = JsonDocument.newDocument(GROUPS_CONFIG_FILE);
        if (document.contains("groups")) {
            this.groups.addAll(document.get("groups", COLL_GROUP_CONFIGURATION));
        }
    }

    private void loadTasks() {
        try {
            this.tasks.clear();

            if (!Files.exists(TASKS_DIRECTORY)) {
                Files.createDirectories(TASKS_DIRECTORY);
                return;
            }
            Files.walkFileTree(TASKS_DIRECTORY, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    System.out.println(LanguageManager.getMessage("cloudnet-load-task").replace("%path%", path.toString()));
                    JsonDocument document = JsonDocument.newDocument(path);
                    ServiceTask task = document.toInstanceOf(ServiceTask.class);
                    if (task != null && task.getName() != null) {
                        tasks.add(task);
                        Files.write(path, new JsonDocument(task).toPrettyJson().getBytes(StandardCharsets.UTF_8));
                        System.out.println(LanguageManager.getMessage("cloudnet-load-task-success").replace("%path%", path.toString()).replace("%name%", task.getName()));
                    } else {
                        System.err.println(LanguageManager.getMessage("cloudnet-load-task-failed").replace("%path%", path.toString()));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void deleteTask(String name) {
        try {
            Files.delete(TASKS_DIRECTORY.resolve(name + ".json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeTask(ServiceTask task) {
        new JsonDocument(task).write(TASKS_DIRECTORY.resolve(task.getName() + ".json"));
    }

    public void save() {
        try {
            Files.createDirectories(TASKS_DIRECTORY);
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        for (ServiceTask task : this.tasks) {
            this.writeTask(task);
        }

        try {
            Files.walkFileTree(TASKS_DIRECTORY, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String name = file.getFileName().toString();
                    if (tasks.stream().noneMatch(serviceTask -> (serviceTask.getName() + ".json").equalsIgnoreCase(name))) {
                        Files.delete(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        new JsonDocument().append("groups", this.groups).write(GROUPS_CONFIG_FILE);
    }

    public List<ServiceTask> getTasks() {
        return this.tasks;
    }

    public List<GroupConfiguration> getGroups() {
        return this.groups;
    }
}
