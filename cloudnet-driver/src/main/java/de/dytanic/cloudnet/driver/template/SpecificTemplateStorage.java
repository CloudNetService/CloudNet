package de.dytanic.cloudnet.driver.template;

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;

public interface SpecificTemplateStorage extends INameable {

    static SpecificTemplateStorage of(@NotNull ServiceTemplate template) {
        return DefaultSpecificTemplateStorage.of(template);
    }

    static SpecificTemplateStorage of(@NotNull ServiceTemplate template, @NotNull TemplateStorage storage) {
        return new DefaultSpecificTemplateStorage(template, storage);
    }

    @NotNull
    ServiceTemplate getTargetTemplate();

    @NotNull
    TemplateStorage getWrappedStorage();

    /**
     * Deploys a zip compressed into a target template storage that should decompressed and deploy on the target template
     *
     * @param zipInput the target zip compressed byte array within all files are included for the target template
     * @return true if the deployment was successful
     * @deprecated Causes very high heap space (over)load. Use {@link #deploy(ZipInputStream)} instead
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.5")
    boolean deploy(@NotNull byte[] zipInput);

    /**
     * Deploys the following directory files to the target template storage.
     *
     * @param directory the directory to deploy
     * @return if the deployment was successful
     */
    boolean deploy(@NotNull File directory, @Nullable Predicate<File> fileFilter);

    default boolean deploy(@NotNull File directory) {
        return this.deploy(directory, null);
    }

    boolean deploy(@NotNull ZipInputStream inputStream);

    boolean deploy(@NotNull Path[] paths);

    boolean deploy(@NotNull File[] files);

    boolean copy(@NotNull File directory);

    boolean copy(@NotNull Path directory);

    boolean copy(@NotNull File[] directories);

    boolean copy(@NotNull Path[] directories);

    /**
     * Zips a template in the current template storage and converts it to a byte array
     *
     * @return The byte array of the zipped template
     * @deprecated Causes very high heap space (over)load. Use {@link #asZipInputStream()} instead
     */
    @Deprecated
    byte[] toZipByteArray();

    @Nullable
    ZipInputStream asZipInputStream() throws IOException;

    @Nullable
    InputStream zipTemplate() throws IOException;

    boolean delete();

    boolean create();

    boolean exists();

    @Nullable
    OutputStream appendOutputStream(@NotNull String path) throws IOException;

    @Nullable
    OutputStream newOutputStream(@NotNull String path) throws IOException;

    boolean createFile(@NotNull String path) throws IOException;

    boolean createDirectory(@NotNull String path) throws IOException;

    boolean hasFile(@NotNull String path) throws IOException;

    boolean deleteFile(@NotNull String path) throws IOException;

    @Nullable
    InputStream newInputStream(@NotNull String path) throws IOException;

    @Nullable
    FileInfo getFileInfo(@NotNull String path) throws IOException;

    @Nullable
    FileInfo[] listFiles(@NotNull String dir, boolean deep) throws IOException;

    @Nullable
    default FileInfo[] listFiles(boolean deep) throws IOException {
        return this.listFiles("", deep);
    }

    @Nullable
    default FileInfo[] listFiles(@NotNull String dir) throws IOException {
        return this.listFiles(dir, true);
    }

    @Nullable
    default FileInfo[] listFiles() throws IOException {
        return this.listFiles(true);
    }

    /**
     * Deploys a zip compressed into a target template storage that should decompressed and deploy on the target template
     *
     * @param zipInput the target zip compressed byte array within all files are included for the target template
     * @return true if the deployment was successful
     * @deprecated Causes very high heap space (over)load. Use {@link #deploy(ZipInputStream)} instead
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "3.5")
    @NotNull
    ITask<Boolean> deployAsync(@NotNull byte[] zipInput);

    /**
     * Deploys the following directory files to the target template storage.
     *
     * @param directory the directory to deploy
     * @return if the deployment was successful
     */
    @NotNull
    ITask<Boolean> deployAsync(@NotNull File directory, @Nullable Predicate<File> fileFilter);

    @NotNull
    default ITask<Boolean> deployAsync(@NotNull File directory) {
        return this.deployAsync(directory, null);
    }

    @NotNull
    ITask<Boolean> deployAsync(@NotNull ZipInputStream inputStream);

    @NotNull
    ITask<Boolean> deployAsync(@NotNull Path[] paths);

    @NotNull
    ITask<Boolean> deployAsync(@NotNull File[] files);

    @NotNull
    ITask<Boolean> copyAsync(@NotNull File directory);

    @NotNull
    ITask<Boolean> copyAsync(@NotNull Path directory);

    @NotNull
    ITask<Boolean> copyAsync(@NotNull File[] directories);

    @NotNull
    ITask<Boolean> copyAsync(@NotNull Path[] directories);

    /**
     * Zips a template in the current template storage and converts it to a byte array
     *
     * @return The byte array of the zipped template
     * @deprecated Causes very high heap space (over)load. Use {@link #asZipInputStream()} instead
     */
    @Deprecated
    @NotNull
    ITask<byte[]> toZipByteArrayAsync();

    @NotNull
    ITask<ZipInputStream> asZipInputStreamAsync();

    @NotNull
    ITask<InputStream> zipTemplateAsync();

    @NotNull
    ITask<Boolean> deleteAsync();

    @NotNull
    ITask<Boolean> createAsync();

    @NotNull
    ITask<Boolean> existsAsync();

    @NotNull
    ITask<OutputStream> appendOutputStreamAsync(@NotNull String path);

    @NotNull
    ITask<OutputStream> newOutputStreamAsync(@NotNull String path);

    @NotNull
    ITask<Boolean> createFileAsync(@NotNull String path);

    @NotNull
    ITask<Boolean> createDirectoryAsync(@NotNull String path);

    @NotNull
    ITask<Boolean> hasFileAsync(@NotNull String path);

    @NotNull
    ITask<Boolean> deleteFileAsync(@NotNull String path);

    @NotNull
    ITask<InputStream> newInputStreamAsync(@NotNull String path);

    @NotNull
    ITask<FileInfo> getFileInfoAsync(@NotNull String path);

    @NotNull
    ITask<FileInfo[]> listFilesAsync(@NotNull String dir, boolean deep);

    @NotNull
    default ITask<FileInfo[]> listFilesAsync(boolean deep) {
        return this.listFilesAsync("", deep);
    }

    @NotNull
    default ITask<FileInfo[]> listFilesAsync(@NotNull String dir) {
        return this.listFilesAsync(dir, true);
    }

    @NotNull
    default ITask<FileInfo[]> listFilesAsync() {
        return this.listFilesAsync(true);
    }

}
