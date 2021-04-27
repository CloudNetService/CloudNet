package de.dytanic.cloudnet.ext.report.listener;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.event.service.CloudServicePreDeleteEvent;
import de.dytanic.cloudnet.ext.report.CloudNetReportModule;
import de.dytanic.cloudnet.service.ICloudService;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CloudNetReportListener {

    @EventListener
    public void handle(Event event) {
        CloudNetReportModule.getInstance().setEventClass(event.getClass());
    }

    @EventListener
    public void handle(CloudServicePreDeleteEvent event) {
        if (CloudNetReportModule.getInstance().getConfig().getBoolean("savingRecords")) {
            String childPath = event.getCloudService().getServiceId().getName() + "." + event.getCloudService().getServiceId().getUniqueId();
            Path subDir = CloudNetReportModule.getInstance().getRecordsSavingDirectory().resolve(childPath).normalize().toAbsolutePath();

            if (Files.notExists(subDir)) {
                FileUtils.createDirectoryReported(subDir);

                System.out.println(LanguageManager.getMessage("module-report-create-record-start")
                        .replace("%service%", event.getCloudService().getServiceId().getName())
                        .replace("%file%", subDir.toString())
                );

                this.copyLogFiles(subDir, event.getCloudService());
                this.writeFileList(subDir, event.getCloudService());
                this.writeWaitingIncludesAndDeployments(subDir, event.getCloudService());
                this.writeServiceConfiguration(subDir, event.getCloudService());
                this.writeCachedConsoleLog(subDir, event.getCloudService());
                this.writeServiceInfoSnapshot(subDir, event.getCloudService());

                System.out.println(LanguageManager.getMessage("module-report-create-record-success")
                        .replace("%service%", event.getCloudService().getServiceId().getName())
                        .replace("%file%", subDir.toString())
                );
            }
        }
    }

    private void copyLogFiles(Path directory, ICloudService cloudService) {
        try {
            Path targetDirectory = directory.resolve("logs");
            FileUtils.createDirectoryReported(targetDirectory);

            if (cloudService.getServiceId().getEnvironment() == ServiceEnvironmentType.BUNGEECORD) {
                FileUtils.walkFileTree(cloudService.getDirectoryPath(), (root, current) -> {
                    try {
                        FileUtils.copy(current, targetDirectory.resolve(root.relativize(current)));
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                }, false, "proxy.log*");
            } else {
                FileUtils.copyFilesToDirectory(cloudService.getDirectoryPath().resolve("logs"), targetDirectory);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void writeFileList(Path directory, ICloudService cloudService) {
        try (OutputStream outputStream = Files.newOutputStream(directory.resolve("files.txt"))) {
            FileUtils.walkFileTree(cloudService.getDirectoryPath(), (root, current) -> {
                try {
                    outputStream.write((current.toAbsolutePath() + " | " + Files.size(current) + " Bytes\n").getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            });
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void writeWaitingIncludesAndDeployments(Path directory, ICloudService cloudService) {
        new JsonDocument()
                .append("waitingIncludes", cloudService.getWaitingIncludes())
                .append("waitingTemplates", cloudService.getWaitingTemplates())
                .append("deployments", cloudService.getDeployments())
                .write(directory.resolve("waitingIncludesAndDeployments.json"))
        ;
    }

    private void writeServiceConfiguration(Path directory, ICloudService cloudService) {
        new JsonDocument()
                .append("serviceConfiguration", cloudService.getServiceConfiguration())
                .write(directory.resolve("serviceConfiguration.json"))
        ;
    }

    private void writeCachedConsoleLog(Path directory, ICloudService cloudService) {
        try (OutputStream outputStream = Files.newOutputStream(directory.resolve("cachedConsoleLog.txt"))) {
            for (String message : cloudService.getServiceConsoleLogCache().getCachedLogMessages()) {
                outputStream.write((message + "\n").getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void writeServiceInfoSnapshot(Path directory, ICloudService cloudService) {
        new JsonDocument()
                .append("serviceInfoSnapshot", cloudService.getServiceInfoSnapshot())
                .append("lastServiceInfoSnapshot", cloudService.getLastServiceInfoSnapshot())
                .write(directory.resolve("serviceInfoSnapshots.json"))
        ;
    }
}