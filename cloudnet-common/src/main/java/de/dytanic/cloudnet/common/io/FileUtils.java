package de.dytanic.cloudnet.common.io;

import de.dytanic.cloudnet.common.concurrent.IVoidThrowableCallback;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * The FileUtils class has a lot of utility methods, for
 * <ol>
 * <li>Byte Streams IO</li>
 * <li>File IO (Coping, Deleting)</li>
 * <li>Zip IO</li>
 * </ol>
 */
public final class FileUtils {

    public static final InputStream EMPTY_STREAM = new ByteArrayInputStream(new byte[0]);

    private static final Map<String, String> zipFileSystemProperties = new HashMap<>();

    static {
        zipFileSystemProperties.put("create", "false");
        zipFileSystemProperties.put("encoding", "UTF-8");
    }

    private FileUtils() {
        throw new UnsupportedOperationException();
    }

    public static byte[] toByteArray(InputStream inputStream) {
        return toByteArray(inputStream, new byte[8192]);
    }

    public static byte[] toByteArray(InputStream inputStream, byte[] buffer) {
        if (inputStream == null) {
            return null;
        }

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            copy(inputStream, byteArrayOutputStream, buffer);

            return byteArrayOutputStream.toByteArray();

        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    public static void openZipFileSystem(File file,
                                         IVoidThrowableCallback<FileSystem> consumer) {
        openZipFileSystem(file.toPath(), consumer);
    }

    public static void openZipFileSystem(Path path,
                                         IVoidThrowableCallback<FileSystem> consumer) {
        try (FileSystem fileSystem = FileSystems
                .newFileSystem(URI.create("jar:" + path.toUri()),
                        zipFileSystemProperties)) {
            consumer.call(fileSystem);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        copy(inputStream, outputStream, new byte[8192]);
    }

    public static void copy(InputStream inputStream, OutputStream outputStream,
                            byte[] buffer) throws IOException {
        copy(inputStream, outputStream, buffer, null);
    }

    public static void copy(InputStream inputStream, OutputStream outputStream,
                            byte[] buffer, Consumer<Integer> lengthInputListener) throws IOException {
        int len;

        while ((len = inputStream.read(buffer, 0, buffer.length)) != -1) {
            if (lengthInputListener != null) {
                lengthInputListener.accept(len);
            }

            outputStream.write(buffer, 0, len);
            outputStream.flush();
        }
    }

    public static void copy(File from, File to) throws IOException {
        copy(from.toPath(), to.toPath());
    }

    public static void copy(File from, File to, byte[] buffer)
            throws IOException {
        copy(from.toPath(), to.toPath(), buffer);
    }

    public static void copy(Path from, Path to) throws IOException {
        copy(from, to, new byte[8192]);
    }

    public static void copy(Path from, Path to, byte[] buffer)
            throws IOException {
        if (from == null || to == null || !Files.exists(from)) {
            return;
        }

        if (!Files.exists(to)) {
            to.toFile().getParentFile().mkdirs();
            to.toFile().delete();

            Files.createFile(to);
        }

        try (InputStream inputStream = Files
                .newInputStream(from); OutputStream outputStream = Files
                .newOutputStream(to)) {
            copy(inputStream, outputStream, buffer);
        }
    }

    public static void copyFilesToDirectory(File from, File to)
            throws IOException {
        copyFilesToDirectory(from, to, new byte[16384]);
    }

    public static void copyFilesToDirectory(File from, File to, Predicate<File> fileFilter)
            throws IOException {
        copyFilesToDirectory(from, to, new byte[16384], fileFilter);
    }

    public static void copyFilesToDirectory(File from, File to, byte[] buffer)
            throws IOException {
        copyFilesToDirectory(from, to, buffer, null);
    }

    public static void copyFilesToDirectory(File from, File to, byte[] buffer, Predicate<File> fileFilter)
            throws IOException {
        if (to == null || from == null || !from.exists()) {
            return;
        }

        if (from.isDirectory()) {
            to.mkdirs();

            File[] list = from.listFiles();

            if (list != null && list.length > 0) {
                for (File file : list) {

                    if (file != null && (fileFilter == null || fileFilter.test(file))) {
                        if (file.isDirectory()) {
                            copyFilesToDirectory(file,
                                    new File(to.getAbsolutePath() + "/" + file.getName()));
                        } else {
                            copy(file.toPath(),
                                    Paths.get(to.getAbsolutePath() + "/" + file.getName()), buffer);
                        }
                    }

                }
            }
        } else {
            copy(from.toPath(),
                    Paths.get(to.getAbsolutePath() + "/" + from.getName()), buffer);
        }
    }

    public static void delete(File file) {
        if (file == null || !file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles();

            if (files != null) {
                for (File entry : files) {
                    if (entry.isDirectory()) {
                        delete(entry);
                    } else {
                        entry.delete();
                    }
                }
            }
        }

        file.delete();
    }

    public static Path convert(Path zipPath, Path... directories)
            throws IOException {
        if (directories == null) {
            return null;
        }

        if (!Files.exists(zipPath)) {
            Files.createFile(zipPath);
        }

        try (OutputStream outputStream = Files.newOutputStream(zipPath);
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream,
                     StandardCharsets.UTF_8)) {
            for (Path dir : directories) {
                if (Files.exists(dir)) {
                    zipDir(zipOutputStream, dir);
                }
            }
        }
        return zipPath;
    }

    /**
     * Converts a bunch of directories to a byte array
     *
     * @param directories The directories which should get converted
     * @return A byte array of a zip file created from the provided directories
     * @deprecated May cause a heap space (over)load
     */
    @Deprecated
    public static byte[] convert(Path... directories) {
        if (directories == null) {
            return emptyZipByteArray();
        }

        try (ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream()) {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteBuffer,
                    StandardCharsets.UTF_8)) {
                for (Path dir : directories) {
                    zipDir(zipOutputStream, dir);
                }
            }

            return byteBuffer.toByteArray();

        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return emptyZipByteArray();
    }

    @Nullable
    public static Path zipToFile(Path directory, Path target) {
        if (directory == null || !Files.exists(directory)) {
            return null;
        }

        delete(target.toFile());
        try (OutputStream outputStream = Files.newOutputStream(target, StandardOpenOption.CREATE)) {
            zipStream(directory, outputStream);
            return target;
        } catch (final IOException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    private static void zipStream(Path source, OutputStream buffer) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
            if (Files.exists(source)) {
                zipDir(zipOutputStream, source);
            }
        }
    }

    private static void zipDir(ZipOutputStream zipOutputStream, Path directory) throws IOException {
        Files.walkFileTree(
                directory,
                new SimpleFileVisitor<Path>() {
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        try {
                            zipOutputStream.putNextEntry(new ZipEntry(directory.relativize(file).toString().replace("\\", "/")));
                            Files.copy(file, zipOutputStream);
                            zipOutputStream.closeEntry();
                        } catch (IOException ex) {
                            zipOutputStream.closeEntry();
                            throw ex;
                        }
                        return FileVisitResult.CONTINUE;
                    }
                }
        );
    }

    public static Path extract(Path zipPath, Path targetDirectory) throws IOException {
        if (zipPath == null || targetDirectory == null || !Files.exists(zipPath)) {
            return targetDirectory;
        }

        try (InputStream inputStream = Files.newInputStream(zipPath)) {
            return extract(inputStream, targetDirectory);
        }
    }

    public static Path extract(InputStream inputStream, Path targetDirectory) throws IOException {
        if (inputStream == null || targetDirectory == null) {
            return targetDirectory;
        }

        extract0(new ZipInputStream(inputStream, StandardCharsets.UTF_8), targetDirectory);

        return targetDirectory;
    }

    public static Path extract(byte[] zipData, Path targetDirectory) throws IOException {
        if (zipData == null || zipData.length == 0 || targetDirectory == null) {
            return targetDirectory;
        }

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                zipData)) {
            extract0(new ZipInputStream(byteArrayInputStream, StandardCharsets.UTF_8), targetDirectory);
        }

        return targetDirectory;
    }

    public static void extract0(ZipInputStream zipInputStream, Path targetDirectory) throws IOException {
        ZipEntry zipEntry;
        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            extractEntry(zipInputStream, zipEntry, targetDirectory);
            zipInputStream.closeEntry();
        }
    }

    public static byte[] emptyZipByteArray() {
        byte[] bytes = null;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream, StandardCharsets.UTF_8);
            zipOutputStream.close();

            bytes = byteArrayOutputStream.toByteArray();

        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return bytes;
    }

    private static void extractEntry(ZipInputStream zipInputStream, ZipEntry zipEntry, Path targetDirectory) throws IOException {
        Path file = targetDirectory.resolve(zipEntry.getName());

        if (zipEntry.isDirectory()) {
            if (!Files.exists(file)) {
                Files.createDirectories(file);
            }
        } else {
            Path parent = file.getParent();
            if (!Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            if (Files.exists(file)) {
                Files.delete(file);
            }

            Files.createFile(file);
            try (OutputStream outputStream = Files.newOutputStream(file)) {
                copy(zipInputStream, outputStream);
            }
        }
    }

    public static void workFileTree(Path path, Consumer<File> consumer) {
        workFileTree(path.toFile(), consumer);
    }

    public static void workFileTree(File file, Consumer<File> consumer) {
        if (!file.exists()) {
            return;
        }

        consumer.accept(file);

        if (file.isDirectory()) {
            File[] files = file.listFiles();

            if (files != null && files.length > 0) {
                for (File entry : files) {
                    workFileTree(entry, consumer);
                }
            }
        }
    }
}