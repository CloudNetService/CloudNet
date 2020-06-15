package de.dytanic.cloudnet.service.defaults;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceEnvironment;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.service.*;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarFile;

final class JVMCloudService extends DefaultMinecraftCloudService implements ICloudService {

    protected static final String RUNTIME = "jvm";

    private final DefaultServiceConsoleLogCache serviceConsoleLogCache = new DefaultServiceConsoleLogCache(this);

    private final Lock lifeCycleLock = new ReentrantLock();

    private Process process;
    private volatile boolean restartState = false;

    JVMCloudService(ICloudServiceManager cloudServiceManager, ServiceConfiguration serviceConfiguration) {
        super(RUNTIME, cloudServiceManager, serviceConfiguration);
    }

    @Override
    public void runCommand(@NotNull String commandLine) {
        if (this.lifeCycle == ServiceLifeCycle.RUNNING && this.process != null) {
            try {
                OutputStream outputStream = this.process.getOutputStream();
                outputStream.write((commandLine + "\n").getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void restart() throws Exception {
        this.restartState = true;

        this.stop();
        this.start();

        this.restartState = false;
    }

    @Override
    protected int shutdown(boolean force) {
        Integer exitCode = 0;

        try {
            this.lifeCycleLock.lock();
            return (exitCode = this.stop0(force)) == null ? -1 : exitCode;
        } finally {
            this.lifeCycleLock.unlock();

            if (exitCode != null) { // user didn't cancel stop event, exitCode is present
                this.invokeAutoDeleteOnStopIfNotRestart();
            }
        }
    }

    private void invokeAutoDeleteOnStopIfNotRestart() {
        if (this.getServiceConfiguration().isAutoDeleteOnStop() && !this.restartState) {
            this.delete();
        } else {
            this.initAndPrepareService();
        }
    }

    @Override
    public void delete() {
        try {
            this.lifeCycleLock.lock();
            this.delete0();
        } finally {
            this.lifeCycleLock.unlock();
        }
    }

    @Override
    public boolean isAlive() {
        return this.lifeCycle == ServiceLifeCycle.DEFINED || this.lifeCycle == ServiceLifeCycle.PREPARED ||
                (this.lifeCycle == ServiceLifeCycle.RUNNING && this.process != null && this.process.isAlive());
    }

    @Override
    protected void startNow() throws Exception {
        try {
            this.lifeCycleLock.lock();
            this.start0();
        } finally {
            this.lifeCycleLock.unlock();
        }
    }

    private void start0() throws Exception {
        if (this.lifeCycle == ServiceLifeCycle.PREPARED || this.lifeCycle == ServiceLifeCycle.STOPPED) {
            if (!this.checkEnoughResources() || CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePreStartPrepareEvent(this)).isCancelled()) {
                return;
            }

            super.prePrepareStart();

            this.includeInclusions();
            this.includeTemplates();

            this.serviceInfoSnapshot = this.createServiceInfoSnapshot(ServiceLifeCycle.PREPARED);
            this.getCloudServiceManager().getGlobalServiceInfoSnapshots().put(this.serviceInfoSnapshot.getServiceId().getUniqueId(), this.serviceInfoSnapshot);

            HostAndPort[] listeners = CloudNet.getInstance().getConfig().getIdentity().getListeners();
            new JsonDocument()
                    .append("connectionKey", this.getConnectionKey())
                    .append("listener", listeners[ThreadLocalRandom.current().nextInt(listeners.length)])
                    //-
                    .append("serviceConfiguration", this.getServiceConfiguration())
                    .append("serviceInfoSnapshot", this.serviceInfoSnapshot)
                    .append("sslConfig", CloudNet.getInstance().getConfig().getServerSslConfig())
                    .write(new File(this.getDirectory(), ".wrapper/wrapper.json"));

            super.postPrepareStart();
            super.preStart();

            this.startWrapper();

            super.postStart();
        }
    }

    private void startWrapper() throws Exception {
        List<String> commandArguments = new ArrayList<>();

        commandArguments.add(CloudNet.getInstance().getConfig().getJVMCommand());
        commandArguments.addAll(CloudNet.getInstance().getConfig().getDefaultJVMFlags().getJvmFlags());

        commandArguments.addAll(Arrays.asList(
                // sys properties
                "-Djline.terminal=jline.UnsupportedTerminal",
                "-Dfile.encoding=UTF-8",
                "-Dio.netty.noPreferDirect=true",
                "-Dclient.encoding.override=UTF-8",
                "-Dio.netty.maxDirectMemory=0",
                "-Dio.netty.leakDetectionLevel=DISABLED",
                "-Dio.netty.recycler.maxCapacity=0",
                "-Dio.netty.recycler.maxCapacity.default=0",
                "-DIReallyKnowWhatIAmDoingISwear=true",
                "-Dcloudnet.wrapper.messages.language=" + LanguageManager.getLanguage()
        ));

        File wrapperFile = new File(System.getProperty("cloudnet.tempDir", "temp"), "caches/wrapper.jar");

        File applicationFile = null;
        File[] files = this.getDirectory().listFiles();

        if (files != null) {
            for (ServiceEnvironment environment : this.getServiceConfiguration().getProcessConfig().getEnvironment().getEnvironments()) {
                for (File file : files) {
                    String fileName = file.getName().toLowerCase();

                    if (fileName.endsWith(".jar") && fileName.contains(environment.getName())) {
                        applicationFile = file;
                        break;
                    }
                }

                if (applicationFile != null) {
                    break;
                }
            }
        }

        if (applicationFile == null) {
            CloudNetDriver.getInstance().getLogger().error(LanguageManager.getMessage("cloud-service-jar-file-not-found-error")
                    .replace("%task%", this.getServiceId().getTaskName())
                    .replace("%serviceId%", String.valueOf(this.getServiceId().getTaskServiceId()))
                    .replace("%id%", this.getServiceId().getUniqueId().toString()));

            ServiceTask serviceTask = CloudNet.getInstance().getServiceTaskProvider().getServiceTask(this.getServiceId().getTaskName());

            if (serviceTask != null) {
                serviceTask.forbidServiceStarting(SERVICE_ERROR_RESTART_DELAY * 1000);
            }

            this.stop();
            return;
        }

        commandArguments.addAll(this.getServiceConfiguration().getProcessConfig().getJvmOptions());
        commandArguments.addAll(Arrays.asList(
                "-Xmx" + this.getServiceConfiguration().getProcessConfig().getMaxHeapMemorySize() + "M",
                "-cp", wrapperFile.getAbsolutePath() + File.pathSeparator + applicationFile.getAbsolutePath())
        );

        try (JarFile jarFile = new JarFile(wrapperFile)) {
            commandArguments.add(jarFile.getManifest().getMainAttributes().getValue("Main-Class"));
        }

        commandArguments.add(applicationFile.getAbsolutePath());
        try (JarFile jarFile = new JarFile(applicationFile)) {
            commandArguments.add(jarFile.getManifest().getMainAttributes().getValue("Main-Class"));
        }

        this.postConfigureServiceEnvironmentStartParameters(commandArguments);

        this.process = new ProcessBuilder()
                .command(commandArguments)
                .directory(this.getDirectory())
                .start();
    }

    private void postConfigureServiceEnvironmentStartParameters(List<String> commandArguments) {
        switch (this.getServiceConfiguration().getProcessConfig().getEnvironment()) {
            case MINECRAFT_SERVER:
                commandArguments.add("nogui");
                break;
            case NUKKIT:
                commandArguments.add("disable-ansi");
                break;
        }
    }

    @Nullable
    private Integer stop0(boolean force) {
        if (this.lifeCycle == ServiceLifeCycle.RUNNING) {
            if (!super.preStop()) {
                return null;
            }

            int exitValue = this.stopProcess(force);

            if (this.getNetworkChannel() != null) {
                try {
                    this.getNetworkChannel().close();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }

            this.lifeCycle = ServiceLifeCycle.STOPPED;

            if (this.getServiceConfiguration().getDeletedFilesAfterStop() != null) {
                for (String path : this.getServiceConfiguration().getDeletedFilesAfterStop()) {
                    if (path != null) {
                        File file = new File(this.getDirectory(), path);
                        if (file.exists()) {
                            FileUtils.delete(file);
                        }
                    }
                }
            }

            super.postStop(exitValue);
            return exitValue;
        }

        return -1;
    }

    private int stopProcess(boolean force) {
        if (this.process != null) {

            if (this.process.isAlive()) {
                try {
                    OutputStream outputStream = this.process.getOutputStream();
                    outputStream.write("stop\n".getBytes());
                    outputStream.flush();
                    outputStream.write("end\n".getBytes());
                    outputStream.flush();

                    if (this.process.waitFor(5, TimeUnit.SECONDS)) {
                        return this.process.exitValue();
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }

            if (!force) {
                this.process.destroy();
            } else {
                this.process.destroyForcibly();
            }

            try {
                return this.process.exitValue();
            } catch (Throwable ignored) {
                try {
                    this.process.destroyForcibly();

                    return this.process.exitValue();
                } catch (Exception ignored0) {
                    return -1;
                }
            }
        }

        return -1;
    }

    private void delete0() {
        if (this.lifeCycle == ServiceLifeCycle.DELETED) {
            return;
        }

        if (this.lifeCycle == ServiceLifeCycle.RUNNING && this.stop0(true) == null) { // User cancelled stop event
            return;
        }

        if (!super.preDelete()) {
            return;
        }

        this.deployResources();

        if (!this.getServiceConfiguration().isStaticService()) {
            FileUtils.delete(this.getDirectory());
        }

        super.postDelete();
    }

    @NotNull
    public DefaultServiceConsoleLogCache getServiceConsoleLogCache() {
        return this.serviceConsoleLogCache;
    }

    public Process getProcess() {
        return this.process;
    }

    public boolean isRestartState() {
        return this.restartState;
    }
}
