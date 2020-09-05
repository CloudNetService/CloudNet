package de.dytanic.cloudnet.provider;

import com.google.common.base.Preconditions;
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.NetworkUpdateType;
import de.dytanic.cloudnet.driver.provider.ServiceTaskProvider;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.service.task.LocalServiceTaskAddEvent;
import de.dytanic.cloudnet.event.service.task.LocalServiceTaskRemoveEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

public class NodeServiceTaskProvider implements ServiceTaskProvider {

    private static final Path OLD_TASK_CONFIG_FILE = Paths.get(System.getProperty("cloudnet.config.task.path", "local/tasks.json"));
    private static final Path TASKS_DIRECTORY = Paths.get(System.getProperty("cloudnet.config.tasks.directory.path", "local/tasks"));

    private final CloudNet cloudNet;

    private final Collection<ServiceTask> permanentServiceTasks = new CopyOnWriteArrayList<>();

    public NodeServiceTaskProvider(CloudNet cloudNet) {
        this.cloudNet = cloudNet;
    }

    public boolean isFileCreated() {
        return Files.exists(TASKS_DIRECTORY);
    }

    private void load() throws IOException {
        this.permanentServiceTasks.clear();

        if (!Files.exists(TASKS_DIRECTORY)) {
            return;
        }
        Files.walkFileTree(TASKS_DIRECTORY, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                System.out.println(LanguageManager.getMessage("cloudnet-load-task").replace("%path%", path.toString()));
                JsonDocument document = JsonDocument.newDocument(path);
                ServiceTask task = document.toInstanceOf(ServiceTask.class);
                if (task != null && task.getName() != null) {
                    permanentServiceTasks.add(task);
                    Files.write(path, new JsonDocument(task).toPrettyJson().getBytes(StandardCharsets.UTF_8));
                    System.out.println(LanguageManager.getMessage("cloudnet-load-task-success").replace("%path%", path.toString()).replace("%name%", task.getName()));
                    if (task.isMaintenance()) {
                        CloudNet.getInstance().getLogger().warning(LanguageManager.getMessage("cloudnet-load-task-maintenance-warning").replace("%task%", task.getName()));
                    }
                } else {
                    System.err.println(LanguageManager.getMessage("cloudnet-load-task-failed").replace("%path%", path.toString()));
                }
                return FileVisitResult.CONTINUE;
            }
        });

        this.upgrade();
    }

    private void upgrade() throws IOException {
        if (Files.exists(OLD_TASK_CONFIG_FILE)) {
            JsonDocument document = JsonDocument.newDocument(OLD_TASK_CONFIG_FILE);
            this.permanentServiceTasks.addAll(document.get("tasks", TypeToken.getParameterized(Collection.class, ServiceTask.class).getType()));
            this.save();

            try {
                Files.delete(OLD_TASK_CONFIG_FILE);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    private void save() throws IOException {
        Files.createDirectories(TASKS_DIRECTORY);

        for (ServiceTask task : this.permanentServiceTasks) {
            this.writeTask(task);
        }

        Files.walkFileTree(TASKS_DIRECTORY, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String name = file.getFileName().toString();
                if (permanentServiceTasks.stream().noneMatch(serviceTask -> (serviceTask.getName() + ".json").equalsIgnoreCase(name))) {
                    Files.delete(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteTaskFile(String name) {
        try {
            Files.deleteIfExists(TASKS_DIRECTORY.resolve(name + ".json"));
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void writeTask(ServiceTask task) {
        new JsonDocument(task).write(TASKS_DIRECTORY.resolve(task.getName() + ".json"));
    }

    @Override
    public void reload() {
        try {
            this.load();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public Collection<ServiceTask> getPermanentServiceTasks() {
        return Collections.unmodifiableCollection(this.permanentServiceTasks);
    }

    @Override
    public void setPermanentServiceTasks(@NotNull Collection<ServiceTask> serviceTasks) {
        this.setServiceTasksWithoutClusterSync(serviceTasks);
        this.cloudNet.updateServiceTasksInCluster(serviceTasks, NetworkUpdateType.SET);
    }

    public void setServiceTasksWithoutClusterSync(@NotNull Collection<ServiceTask> tasks) {
        Preconditions.checkNotNull(tasks);

        this.permanentServiceTasks.clear();
        this.permanentServiceTasks.addAll(tasks);
        try {
            this.save();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public ServiceTask getServiceTask(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.permanentServiceTasks.stream().filter(serviceTask -> serviceTask.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    @Override
    public boolean isServiceTaskPresent(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.getServiceTask(name) != null;
    }

    @Override
    public boolean addPermanentServiceTask(@NotNull ServiceTask serviceTask) {
        Preconditions.checkNotNull(serviceTask);

        if (this.addServiceTaskWithoutClusterSync(serviceTask)) {
            this.cloudNet.updateServiceTasksInCluster(Collections.singletonList(serviceTask), NetworkUpdateType.ADD);
            return true;
        }
        return false;
    }

    public boolean addServiceTaskWithoutClusterSync(ServiceTask serviceTask) {
        LocalServiceTaskAddEvent event = new LocalServiceTaskAddEvent(serviceTask);
        CloudNetDriver.getInstance().getEventManager().callEvent(event);

        if (!event.isCancelled()) {
            if (this.isServiceTaskPresent(serviceTask.getName())) {
                this.permanentServiceTasks.removeIf(task -> task.getName().equalsIgnoreCase(serviceTask.getName()));
            }

            this.permanentServiceTasks.add(serviceTask);

            this.writeTask(serviceTask);

            return true;
        }
        return false;
    }

    @Override
    public void removePermanentServiceTask(@NotNull String name) {
        Preconditions.checkNotNull(name);

        ServiceTask serviceTask = this.removeServiceTaskWithoutClusterSync(name);
        if (serviceTask != null) {
            this.cloudNet.updateServiceTasksInCluster(Collections.singletonList(serviceTask), NetworkUpdateType.REMOVE);
        }
    }

    public ServiceTask removeServiceTaskWithoutClusterSync(String name) {
        for (ServiceTask serviceTask : this.permanentServiceTasks) {
            if (serviceTask.getName().equalsIgnoreCase(name)) {
                if (!CloudNetDriver.getInstance().getEventManager().callEvent(new LocalServiceTaskRemoveEvent(serviceTask)).isCancelled()) {
                    this.permanentServiceTasks.remove(serviceTask);
                    this.deleteTaskFile(name);
                    return serviceTask;
                }
            }
        }
        return null;
    }

    @Override
    public void removePermanentServiceTask(@NotNull ServiceTask serviceTask) {
        Preconditions.checkNotNull(serviceTask);

        this.removePermanentServiceTask(serviceTask.getName());
    }

    @Override
    public @NotNull ITask<Void> reloadAsync() {
        return this.cloudNet.scheduleTask(() -> {
            this.reload();
            return null;
        });
    }

    @Override
    @NotNull
    public ITask<Collection<ServiceTask>> getPermanentServiceTasksAsync() {
        return this.cloudNet.scheduleTask(this::getPermanentServiceTasks);
    }

    @Override
    public @NotNull ITask<Void> setPermanentServiceTasksAsync(@NotNull Collection<ServiceTask> serviceTasks) {
        return this.cloudNet.scheduleTask(() -> {
            this.setPermanentServiceTasks(serviceTasks);
            return null;
        });
    }

    @Override
    @NotNull
    public ITask<ServiceTask> getServiceTaskAsync(@NotNull String name) {
        return this.cloudNet.scheduleTask(() -> this.getServiceTask(name));
    }

    @Override
    @NotNull
    public ITask<Boolean> isServiceTaskPresentAsync(@NotNull String name) {
        return this.cloudNet.scheduleTask(() -> this.isServiceTaskPresent(name));
    }

    @Override
    @NotNull
    public ITask<Boolean> addPermanentServiceTaskAsync(@NotNull ServiceTask serviceTask) {
        return this.cloudNet.scheduleTask(() -> {
            this.addPermanentServiceTask(serviceTask);
            return null;
        });
    }

    @Override
    @NotNull
    public ITask<Void> removePermanentServiceTaskAsync(@NotNull String name) {
        return this.cloudNet.scheduleTask(() -> {
            this.removePermanentServiceTask(name);
            return null;
        });
    }

    @Override
    @NotNull
    public ITask<Void> removePermanentServiceTaskAsync(@NotNull ServiceTask serviceTask) {
        return this.cloudNet.scheduleTask(() -> {
            this.removePermanentServiceTask(serviceTask);
            return null;
        });
    }

}
