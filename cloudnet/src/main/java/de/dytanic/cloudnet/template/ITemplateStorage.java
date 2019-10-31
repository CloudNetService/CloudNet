package de.dytanic.cloudnet.template;

import de.dytanic.cloudnet.driver.service.ServiceTemplate;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;

/**
 * The template storage manage the management of service specific templates that should copy or deploy on
 * the implementation
 * <p>
 * The implementation allows to deploy, copy and convert to a zip compressed array any template that should handle
 */
public interface ITemplateStorage extends AutoCloseable {

    /**
     * Deploys a zip compressed into a target template storage that should decompressed and deploy on the target template
     *
     * @param zipInput the target zip compressed byte array within all files are included for the target template
     * @param target   the target serviceTemplate to that should deploy
     * @return true if the deployment was successful
     */
    boolean deploy(byte[] zipInput, ServiceTemplate target);

    /**
     * Deploys the following directory files to the target template storage.
     *
     * @param directory the
     * @param target
     * @return
     */
    boolean deploy(File directory, ServiceTemplate target);

    boolean deploy(Path[] paths, ServiceTemplate target);

    boolean deploy(File[] files, ServiceTemplate target);

    boolean copy(ServiceTemplate template, File directory);

    boolean copy(ServiceTemplate template, Path directory);

    boolean copy(ServiceTemplate template, File[] directories);

    boolean copy(ServiceTemplate template, Path[] directories);

    byte[] toZipByteArray(ServiceTemplate template);

    boolean delete(ServiceTemplate template);

    boolean create(ServiceTemplate template);

    boolean has(ServiceTemplate template);

    OutputStream appendOutputStream(ServiceTemplate template, String path);

    OutputStream newOutputStream(ServiceTemplate template, String path);

    Collection<ServiceTemplate> getTemplates();

}