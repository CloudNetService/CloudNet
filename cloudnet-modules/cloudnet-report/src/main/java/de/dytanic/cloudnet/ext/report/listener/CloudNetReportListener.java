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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public final class CloudNetReportListener {

    @EventListener
    public void handle(Event event) {
        CloudNetReportModule.getInstance().setEventClass(event.getClass());
    }

    @EventListener
    public void handle(CloudServicePreDeleteEvent event) {
        if (CloudNetReportModule.getInstance().getConfig().getBoolean("savingRecords")) {
            File subDir = new File(CloudNetReportModule.getInstance().getSavingRecordsDirectory(),
                    event.getCloudService().getServiceId().getName() + "." + event.getCloudService().getServiceId().getUniqueId());

            if (!subDir.exists()) {
                subDir.mkdirs();

                System.out.println(LanguageManager.getMessage("module-report-create-record-start")
                        .replace("%service%", event.getCloudService().getServiceId().getName())
                        .replace("%file%", subDir.getAbsolutePath())
                );

                copyLogFiles(subDir, event.getCloudService());
                writeFileList(subDir, event.getCloudService());
                writeWaitingIncludesAndDeployments(subDir, event.getCloudService());
                writeServiceConfiguration(subDir, event.getCloudService());
                writeCachedConsoleLog(subDir, event.getCloudService());
                writeServiceInfoSnapshot(subDir, event.getCloudService());

                System.out.println(LanguageManager.getMessage("module-report-create-record-success")
                        .replace("%service%", event.getCloudService().getServiceId().getName())
                        .replace("%file%", subDir.getAbsolutePath())
                );
            }
        }
    }

    private void copyLogFiles(File directory, ICloudService cloudService) {
        try {
            if (cloudService.getServiceId().getEnvironment() == ServiceEnvironmentType.BUNGEECORD) {
                File[] files = cloudService.getDirectory().listFiles(pathname -> !pathname.isDirectory() && pathname.getName().startsWith("proxy.log"));

                if (files != null) {
                    File subDir = new File(directory, "logs");
                    subDir.mkdirs();

                    for (File file : files) {
                        try {
                            FileUtils.copy(file, new File(subDir, file.getName()));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            } else {
                FileUtils.copyFilesToDirectory(new File(cloudService.getDirectory(), "logs"), new File(directory, "logs"));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void writeFileList(File directory, ICloudService cloudService) {
        try (FileWriter fileWriter = new FileWriter(new File(directory, "files.txt"))) {
            FileUtils.workFileTree(cloudService.getDirectory(), file -> {
                try {
                    fileWriter.write(file.getAbsolutePath() + " | " + file.length() + " Bytes\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeWaitingIncludesAndDeployments(File directory, ICloudService cloudService) {
        new JsonDocument()
                .append("waitingIncludes", cloudService.getWaitingIncludes())
                .append("waitingTemplates", cloudService.getWaitingTemplates())
                .append("deployments", cloudService.getDeployments())
                .write(new File(directory, "waitingIncludesAndDeployments.json"))
        ;
    }

    private void writeServiceConfiguration(File directory, ICloudService cloudService) {
        new JsonDocument()
                .append("serviceConfiguration", cloudService.getServiceConfiguration())
                .write(new File(directory, "serviceConfiguration.json"))
        ;
    }

    private void writeCachedConsoleLog(File directory, ICloudService cloudService) {
        try (FileWriter fileWriter = new FileWriter(new File(directory, "cachedConsoleLog.txt"))) {
            for (String message : cloudService.getServiceConsoleLogCache().getCachedLogMessages()) {
                fileWriter.write(message + "\n");
                fileWriter.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeServiceInfoSnapshot(File directory, ICloudService cloudService) {
        new JsonDocument()
                .append("serviceInfoSnapshot", cloudService.getServiceInfoSnapshot())
                .append("lastServiceInfoSnapshot", cloudService.getLastServiceInfoSnapshot())
                .write(new File(directory, "serviceInfoSnapshots.json"))
        ;
    }
}