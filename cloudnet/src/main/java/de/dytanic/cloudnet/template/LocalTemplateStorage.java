package de.dytanic.cloudnet.template;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

public final class LocalTemplateStorage implements ITemplateStorage {

    public static final String LOCAL_TEMPLATE_STORAGE = "local";
    private final Path storageDirectory;

    @Deprecated
    public LocalTemplateStorage(File storageDirectory) {
        this(storageDirectory.toPath());
    }

    public LocalTemplateStorage(Path storageDirectory) {
        this.storageDirectory = storageDirectory;
        FileUtils.createDirectoryReported(storageDirectory);
    }

    @Override
    @Deprecated
    public boolean deploy(byte[] zipInput, @NotNull ServiceTemplate target) {
        Preconditions.checkNotNull(target);

        try {
            FileUtils.extract(zipInput, this.storageDirectory.resolve(target.getTemplatePath()));
            return true;
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean deploy(@NotNull Path directory, @NotNull ServiceTemplate target, DirectoryStream.@Nullable Filter<Path> filter) {
        Preconditions.checkNotNull(directory);
        Preconditions.checkNotNull(target);

        if (Files.isDirectory(directory)) {
            FileUtils.copyFilesToDirectory(directory, this.storageDirectory.resolve(target.getTemplatePath()), filter);
            return true;
        }

        return false;
    }

    @Override
    public boolean deploy(@NotNull ZipInputStream inputStream, @NotNull ServiceTemplate serviceTemplate) {
        Preconditions.checkNotNull(inputStream);
        Preconditions.checkNotNull(serviceTemplate);

        try {
            FileUtils.extract0(inputStream, this.storageDirectory.resolve(serviceTemplate.getTemplatePath()));
            return true;
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean deploy(@NotNull Path[] paths, @NotNull ServiceTemplate target) {
        Preconditions.checkNotNull(paths);
        Preconditions.checkNotNull(target);

        Path templateDirectory = this.storageDirectory.resolve(target.getTemplatePath());

        boolean result = true;
        for (Path path : paths) {
            try {
                if (Files.isDirectory(path)) {
                    FileUtils.copyFilesToDirectory(path, templateDirectory.resolve(path));
                } else {
                    FileUtils.copy(path, templateDirectory.resolve(path));
                }
            } catch (IOException exception) {
                exception.printStackTrace();
                result = false;
            }
        }

        return result;
    }

    @Override
    public boolean copy(@NotNull ServiceTemplate template, @NotNull Path directory) {
        Preconditions.checkNotNull(template);
        Preconditions.checkNotNull(directory);

        FileUtils.copyFilesToDirectory(this.storageDirectory.resolve(template.getTemplatePath()), directory);
        return true;
    }

    @Override
    public boolean copy(@NotNull ServiceTemplate template, @NotNull Path[] directories) {
        Preconditions.checkNotNull(directories);
        boolean value = true;

        for (Path path : directories) {
            if (!this.copy(template, path)) {
                value = false;
            }
        }

        return value;
    }

    @Override
    @Deprecated
    public byte[] toZipByteArray(@NotNull ServiceTemplate template) {
        Path dir = this.storageDirectory.resolve(template.getTemplatePath());
        return Files.exists(dir) ? FileUtils.convert(dir) : null;
    }

    @Override
    @Nullable
    public InputStream zipTemplate(@NotNull ServiceTemplate template) throws IOException {
        if (!this.has(template)) {
            return null;
        }

        Path directory = this.storageDirectory.resolve(template.getTemplatePath());
        Path tempFile = Paths.get(System.getProperty("cloudnet.tempDir", "temp"), UUID.randomUUID().toString());

        Path file = FileUtils.zipToFile(directory, tempFile);
        if (file == null) {
            return null;
        }

        return Files.newInputStream(file, StandardOpenOption.DELETE_ON_CLOSE, LinkOption.NOFOLLOW_LINKS);
    }

    @Override
    public boolean delete(@NotNull ServiceTemplate template) {
        Preconditions.checkNotNull(template);

        FileUtils.delete(this.storageDirectory.resolve(template.getTemplatePath()));
        return true;
    }

    @Override
    public boolean create(@NotNull ServiceTemplate template) {
        Path directory = this.storageDirectory.resolve(template.getTemplatePath());
        if (Files.notExists(directory)) {
            FileUtils.createDirectoryReported(directory);
            return true;
        }
        return false;
    }

    @Override
    public boolean has(@NotNull ServiceTemplate template) {
        Preconditions.checkNotNull(template);
        return Files.exists(this.storageDirectory.resolve(template.getTemplatePath()));
    }

    @Nullable
    @Override
    public OutputStream appendOutputStream(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        Path file = this.storageDirectory.resolve(template.getTemplatePath()).resolve(path);
        if (Files.notExists(file)) {
            Files.createDirectories(file.getParent());
            Files.createFile(file);
        }
        return Files.newOutputStream(file, StandardOpenOption.APPEND);
    }

    @Nullable
    @Override
    public OutputStream newOutputStream(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        Path file = this.storageDirectory.resolve(template.getTemplatePath()).resolve(path);
        if (Files.exists(file)) {
            Files.delete(file);
        } else {
            Files.createDirectories(file.getParent());
        }
        return Files.newOutputStream(file, StandardOpenOption.CREATE);
    }

    @Override
    public boolean createFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        Path file = this.storageDirectory.resolve(template.getTemplatePath()).resolve(path);
        if (Files.exists(file)) {
            return false;
        }
        Files.createDirectories(file.getParent());
        Files.createFile(file);
        return true;
    }

    @Override
    public boolean createDirectory(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        Path dir = this.storageDirectory.resolve(template.getTemplatePath()).resolve(path);
        if (Files.exists(dir)) {
            return false;
        }
        Files.createDirectories(dir);
        return true;
    }

    @Override
    public boolean hasFile(@NotNull ServiceTemplate template, @NotNull String path) {
        Path file = this.storageDirectory.resolve(template.getTemplatePath()).resolve(path);
        return Files.exists(file);
    }

    @Override
    public boolean deleteFile(@NotNull ServiceTemplate template, @NotNull String path) {
        Path file = this.storageDirectory.resolve(template.getTemplatePath()).resolve(path);
        if (Files.notExists(file)) {
            return false;
        }

        FileUtils.delete(file);
        return true;
    }

    @Override
    public String[] listFiles(@NotNull ServiceTemplate template, @NotNull String dir) throws IOException {
        List<String> files = new ArrayList<>();
        Path directory = this.storageDirectory.resolve(template.getTemplatePath()).resolve(dir);
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                files.add(directory.relativize(file).toString());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                files.add(directory.relativize(dir).toString());
                return FileVisitResult.CONTINUE;
            }
        });
        return files.toArray(new String[0]);
    }

    @Override
    public Collection<ServiceTemplate> getTemplates() {
        try {
            return Files.list(this.storageDirectory)
                    .filter(Files::isDirectory)
                    .flatMap(path -> {
                        try {
                            return Files.list(path);
                        } catch (IOException exception) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .map(path -> {
                        Path relativize = this.storageDirectory.relativize(path);
                        return new ServiceTemplate(relativize.getName(0).toString(), relativize.getName(1).toString(), LOCAL_TEMPLATE_STORAGE);
                    })
                    .collect(Collectors.toList());
        } catch (IOException exception) {
            exception.printStackTrace();
            return Collections.emptyList();
        }
    }

    @Override
    public boolean shouldSyncInCluster() {
        return true;
    }

    @Override
    public void close() {
    }

    public Path getStorageDirectory() {
        return this.storageDirectory;
    }

    @Override
    public String getName() {
        return LOCAL_TEMPLATE_STORAGE;
    }
}