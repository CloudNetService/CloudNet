package de.dytanic.cloudnet.template;

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * The template storage manage the management of service specific templates that should copy or deploy on
 * the implementation
 * <p>
 * The implementation allows to deploy, copy and convert to a zip compressed array any template that should handle
 */
public interface ITemplateStorage extends AutoCloseable, INameable {

    /**
     * Deploys a zip compressed into a target template storage that should decompressed and deploy on the target template
     *
     * @param zipInput the target zip compressed byte array within all files are included for the target template
     * @param target   the target serviceTemplate to that should deploy
     * @return true if the deployment was successful
     */
    boolean deploy(@NotNull byte[] zipInput, @NotNull ServiceTemplate target);

    /**
     * Deploys the following directory files to the target template storage.
     *
     * @param directory the
     * @param target
     * @return
     */
    boolean deploy(@NotNull File directory, @NotNull ServiceTemplate target, @Nullable Predicate<File> fileFilter);

    default boolean deploy(@NotNull File directory, @NotNull ServiceTemplate target) {
        return this.deploy(directory, target, null);
    }

    boolean deploy(@NotNull Path[] paths, @NotNull ServiceTemplate target);

    boolean deploy(@NotNull File[] files, @NotNull ServiceTemplate target);

    boolean copy(@NotNull ServiceTemplate template, @NotNull File directory);

    boolean copy(@NotNull ServiceTemplate template, @NotNull Path directory);

    boolean copy(@NotNull ServiceTemplate template, @NotNull File[] directories);

    boolean copy(@NotNull ServiceTemplate template, @NotNull Path[] directories);

    byte[] toZipByteArray(@NotNull ServiceTemplate template);

    boolean delete(@NotNull ServiceTemplate template);

    boolean create(@NotNull ServiceTemplate template);

    boolean has(@NotNull ServiceTemplate template);

    @Nullable
    OutputStream appendOutputStream(@NotNull ServiceTemplate template, @NotNull String path) throws IOException;

    @Nullable
    OutputStream newOutputStream(@NotNull ServiceTemplate template, @NotNull String path) throws IOException;

    boolean createFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException;

    boolean createDirectory(@NotNull ServiceTemplate template, @NotNull String path) throws IOException;

    boolean hasFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException;

    boolean deleteFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException;

    String[] listFiles(@NotNull ServiceTemplate template, @NotNull String dir) throws IOException;

    default String[] listFiles(@NotNull ServiceTemplate template) throws IOException {
        return this.listFiles(template, "");
    }

    Collection<ServiceTemplate> getTemplates();

    default boolean shouldSyncInCluster() {
        return false;
    }

    @Override
    void close() throws IOException;

}