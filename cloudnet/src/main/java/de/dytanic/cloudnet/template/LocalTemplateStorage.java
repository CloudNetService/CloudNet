package de.dytanic.cloudnet.template;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public final class LocalTemplateStorage implements ITemplateStorage {

    public static final String LOCAL_TEMPLATE_STORAGE = "local";

    private final File storageDirectory;

    public LocalTemplateStorage(File storageDirectory) {
        this.storageDirectory = storageDirectory;
        this.storageDirectory.mkdirs();
    }

    @Override
    public boolean deploy(byte[] zipInput, ServiceTemplate target) {
        Validate.checkNotNull(target);

        try {
            FileUtils.extract(zipInput, new File(this.storageDirectory, target.getTemplatePath()).toPath());
            return true;
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean deploy(File directory, ServiceTemplate target, Predicate<File> fileFilter) {
        Validate.checkNotNull(directory);
        Validate.checkNotNull(target);

        if (!directory.isDirectory()) {
            return false;
        }

        try {
            FileUtils.copyFilesToDirectory(directory, new File(this.storageDirectory, target.getTemplatePath()), fileFilter);
            return true;
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean deploy(Path[] paths, ServiceTemplate target) {
        Validate.checkNotNull(paths);
        Validate.checkNotNull(target);

        return this.deploy(Iterables.map(Arrays.asList(paths), Path::toFile).toArray(new File[0]), target);
    }

    @Override
    public boolean deploy(File[] files, ServiceTemplate target) {
        Validate.checkNotNull(files);
        Validate.checkNotNull(target);

        byte[] buffer = new byte[32768];

        File templateDirectory = new File(this.storageDirectory, target.getTemplatePath());

        boolean value = true;

        for (File entry : files) {
            try {
                if (entry.isDirectory()) {
                    FileUtils.copyFilesToDirectory(entry, new File(templateDirectory, entry.getName()), buffer);
                } else {
                    FileUtils.copy(entry, new File(templateDirectory, entry.getName()), buffer);
                }

            } catch (Exception ex) {
                ex.printStackTrace();

                value = false;
            }
        }

        return value;
    }

    @Override
    public boolean copy(ServiceTemplate template, File directory) {
        Validate.checkNotNull(template);
        Validate.checkNotNull(directory);

        byte[] buffer = new byte[32768];
        File templateDirectory = new File(this.storageDirectory, template.getTemplatePath());
        boolean value = true;

        try {
            FileUtils.copyFilesToDirectory(templateDirectory, directory, buffer);
        } catch (IOException e) {
            e.printStackTrace();
            value = false;
        }

        return value;
    }

    @Override
    public boolean copy(ServiceTemplate template, Path directory) {
        Validate.checkNotNull(template);
        Validate.checkNotNull(directory);

        return this.copy(template, directory.toFile());
    }

    @Override
    public boolean copy(ServiceTemplate template, File[] directories) {
        Validate.checkNotNull(directories);
        boolean value = true;

        for (File directory : directories) {
            if (!this.copy(template, directory)) {
                value = false;
            }
        }

        return value;
    }

    @Override
    public boolean copy(ServiceTemplate template, Path[] directories) {
        Validate.checkNotNull(directories);
        boolean value = true;

        for (Path path : directories) {
            if (!this.copy(template, path)) {
                value = false;
            }
        }

        return value;
    }

    @Override
    public byte[] toZipByteArray(ServiceTemplate template) {
        File directory = new File(storageDirectory, template.getTemplatePath());
        return directory.exists() ? FileUtils.convert(new Path[]{directory.toPath()}) : null;
    }

    @Override
    public boolean delete(ServiceTemplate template) {
        Validate.checkNotNull(template);

        FileUtils.delete(new File(this.storageDirectory, template.getTemplatePath()));
        return true;
    }

    @Override
    public boolean create(ServiceTemplate template) {
        File diretory = new File(this.storageDirectory, template.getTemplatePath());
        if (diretory.exists()) {
            return false;
        }
        diretory.mkdirs();
        return true;
    }

    @Override
    public boolean has(ServiceTemplate template) {
        Validate.checkNotNull(template);

        return new File(this.storageDirectory, template.getTemplatePath()).exists();
    }

    @Override
    public OutputStream appendOutputStream(ServiceTemplate template, String path) throws IOException {
        Path file = this.storageDirectory.toPath().resolve(template.getTemplatePath()).resolve(path);
        if (!Files.exists(file)) {
            Files.createDirectories(file.getParent());
            Files.createFile(file);
        }

        return Files.newOutputStream(file, StandardOpenOption.APPEND);
    }

    @Override
    public OutputStream newOutputStream(ServiceTemplate template, String path) throws IOException {
        Path file = this.storageDirectory.toPath().resolve(template.getTemplatePath()).resolve(path);
        if (!Files.exists(file)) {
            Files.createDirectories(file.getParent());
        }
        return Files.newOutputStream(file, StandardOpenOption.CREATE);
    }

    @Override
    public boolean createFile(ServiceTemplate template, String path) throws IOException {
        Path file = this.storageDirectory.toPath().resolve(template.getTemplatePath()).resolve(path);
        if (Files.exists(file)) {
            return false;
        }
        Files.createDirectories(file.getParent());
        Files.createFile(file);
        return true;
    }

    @Override
    public boolean createDirectory(ServiceTemplate template, String path) throws IOException {
        Path dir = this.storageDirectory.toPath().resolve(template.getTemplatePath()).resolve(path);
        if (Files.exists(dir)) {
            return false;
        }
        Files.createDirectories(dir);
        return true;
    }

    @Override
    public boolean hasFile(ServiceTemplate template, String path) {
        Path file = this.storageDirectory.toPath().resolve(template.getTemplatePath()).resolve(path);
        return Files.exists(file);
    }

    @Override
    public boolean deleteFile(ServiceTemplate template, String path) throws IOException {
        Path file = this.storageDirectory.toPath().resolve(template.getTemplatePath()).resolve(path);
        if (!Files.exists(file)) {
            return false;
        }
        if (Files.isDirectory(file)) {
            Files.walkFileTree(file, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        Files.delete(file);
        return true;
    }

    @Override
    public String[] listFiles(ServiceTemplate template, String dir) throws IOException {
        List<String> files = new ArrayList<>();
        Path directory = this.storageDirectory.toPath().resolve(template.getTemplatePath()).resolve(dir);
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                files.add(directory.relativize(file).toString());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                files.add(directory.relativize(dir).toString());
                return FileVisitResult.CONTINUE;
            }
        });
        return files.toArray(new String[0]);
    }

    @Override
    public Collection<ServiceTemplate> getTemplates() {
        Collection<ServiceTemplate> templates = Iterables.newArrayList();

        File[] files = this.storageDirectory.listFiles();

        if (files != null) {
            for (File entry : files) {
                if (entry.isDirectory()) {
                    File[] subPathEntries = entry.listFiles();

                    if (subPathEntries != null) {
                        for (File subEntry : subPathEntries) {
                            if (subEntry.isDirectory()) {
                                templates.add(new ServiceTemplate(entry.getName(), subEntry.getName(), LOCAL_TEMPLATE_STORAGE));
                            }
                        }
                    }
                }
            }
        }

        return templates;
    }

    @Override
    public void close() {
    }

    public File getStorageDirectory() {
        return this.storageDirectory;
    }

    @Override
    public String getName() {
        return LOCAL_TEMPLATE_STORAGE;
    }
}