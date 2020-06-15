package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceEnvironment;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.service.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;
import java.util.jar.JarFile;

final class JVMCloudService extends DefaultCloudService implements ICloudService {

    protected static final String RUNTIME = "jvm";
    private static final Lock START_SEQUENCE_LOCK = new ReentrantLock();

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
    public void start() throws Exception {
        try {
            this.lifeCycleLock.lock();

            if (!CloudNet.getInstance().getConfig().isParallelServiceStartSequence()) {
                try {

                    START_SEQUENCE_LOCK.lock();
                    this.start0();
                } finally {
                    START_SEQUENCE_LOCK.unlock();
                }
            } else {
                this.start0();
            }

        } finally {
            this.lifeCycleLock.unlock();
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

    private void start0() throws Exception {
        if (this.lifeCycle == ServiceLifeCycle.PREPARED || this.lifeCycle == ServiceLifeCycle.STOPPED) {
            if (!this.checkEnoughResources() || CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePreStartPrepareEvent(this)).isCancelled()) {
                return;
            }

            CloudNetDriver.getInstance().getLogger().extended(LanguageManager.getMessage("cloud-service-pre-start-prepared-message")
                    .replace("%task%", this.getServiceId().getTaskName())
                    .replace("%serviceId%", String.valueOf(this.getServiceId().getTaskServiceId()))
                    .replace("%id%", this.getServiceId().getUniqueId().toString())
            );

            this.includeInclusions();
            this.includeTemplates();

            this.serviceInfoSnapshot = this.createServiceInfoSnapshot(ServiceLifeCycle.PREPARED);
            this.getCloudServiceManager().getGlobalServiceInfoSnapshots().put(this.serviceInfoSnapshot.getServiceId().getUniqueId(), this.serviceInfoSnapshot);

            new JsonDocument()
                    .append("connectionKey", this.getConnectionKey())
                    .append("listener", CloudNet.getInstance().getConfig().getIdentity().getListeners()
                            [ThreadLocalRandom.current().nextInt(CloudNet.getInstance().getConfig().getIdentity().getListeners().length)])
                    //-
                    .append("serviceConfiguration", this.getServiceConfiguration())
                    .append("serviceInfoSnapshot", this.serviceInfoSnapshot)
                    .append("sslConfig", CloudNet.getInstance().getConfig().getServerSslConfig())
                    .write(new File(this.getDirectory(), ".wrapper/wrapper.json"));

            CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePostStartPrepareEvent(this));
            CloudNetDriver.getInstance().getLogger().extended(LanguageManager.getMessage("cloud-service-post-start-prepared-message")
                    .replace("%task%", this.getServiceId().getTaskName())
                    .replace("%serviceId%", String.valueOf(this.getServiceId().getTaskServiceId()))
                    .replace("%id%", this.getServiceId().getUniqueId().toString())
            );

            System.out.println(LanguageManager.getMessage("cloud-service-pre-start-message")
                    .replace("%task%", this.getServiceId().getTaskName())
                    .replace("%serviceId%", String.valueOf(this.getServiceId().getTaskServiceId()))
                    .replace("%id%", this.getServiceId().getUniqueId().toString())
            );
            CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePreStartEvent(this));

            this.configureServiceEnvironment();
            this.startWrapper();

            this.lifeCycle = ServiceLifeCycle.RUNNING;
            CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePostStartEvent(this));
            CloudNetDriver.getInstance().getLogger().extended(LanguageManager.getMessage("cloud-service-post-start-message")
                    .replace("%serviceId%", String.valueOf(this.getServiceId().getTaskServiceId()))
                    .replace("%task%", this.getServiceId().getTaskName())
                    .replace("%id%", this.getServiceId().getUniqueId().toString())
            );

            this.serviceInfoSnapshot.setLifeCycle(ServiceLifeCycle.RUNNING);
            CloudNet.getInstance().sendAll(new PacketClientServerServiceInfoPublisher(this.serviceInfoSnapshot, PacketClientServerServiceInfoPublisher.PublisherType.STARTED));
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

    private void rewriteBungeeConfig(File config) throws Exception {
        this.rewriteServiceConfigurationFile(config, line -> {
            if (line.startsWith("    host: ")) {
                line = "    host: " + CloudNet.getInstance().getConfig().getHostAddress() + ":" + this.getServiceConfiguration().getPort();
            } else if (line.startsWith("  host: ")) {
                line = "  host: " + CloudNet.getInstance().getConfig().getHostAddress() + ":" + this.getServiceConfiguration().getPort();
            }

            return line;
        });
    }

    private void configureServiceEnvironment() throws Exception {
        switch (this.getServiceConfiguration().getProcessConfig().getEnvironment()) {
            case BUNGEECORD: {
                File file = new File(this.getDirectory(), "config.yml");
                this.copyDefaultFile("files/bungee/config.yml", file);

                this.rewriteBungeeConfig(file);
            }
            break;
            case WATERDOG: {
                File file = new File(this.getDirectory(), "config.yml");
                this.copyDefaultFile("files/waterdog/config.yml", file);

                this.rewriteBungeeConfig(file);
            }
            break;
            case VELOCITY: {
                File file = new File(this.getDirectory(), "velocity.toml");
                this.copyDefaultFile("files/velocity/velocity.toml", file);

                AtomicBoolean reference = new AtomicBoolean(true);

                this.rewriteServiceConfigurationFile(file, line -> {
                    if (reference.get() && line.startsWith("bind =")) {
                        reference.set(false);
                        return "bind = \"" + CloudNet.getInstance().getConfig().getHostAddress() + ":" + this.getServiceConfiguration().getPort() + "\"";
                    }

                    return line;
                });
            }
            break;
            case MINECRAFT_SERVER: {
                File file = new File(this.getDirectory(), "server.properties");
                this.copyDefaultFile("files/nms/server.properties", file);

                Properties properties = new Properties();

                try (InputStream inputStream = new FileInputStream(file)) {
                    properties.load(inputStream);
                }

                properties.setProperty("server-name", this.getServiceId().getName());
                properties.setProperty("server-port", String.valueOf(this.getServiceConfiguration().getPort()));
                properties.setProperty("server-ip", CloudNet.getInstance().getConfig().getHostAddress());

                try (OutputStream outputStream = new FileOutputStream(file);
                     OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                    properties.store(writer, "Edit by CloudNet");
                }

                properties = new Properties();

                file = new File(this.getDirectory(), "eula.txt");
                if (file.exists() || file.createNewFile()) {
                    try (InputStream inputStream = new FileInputStream(file)) {
                        properties.load(inputStream);
                    }
                }

                properties.setProperty("eula", "true");

                try (OutputStream outputStream = new FileOutputStream(file);
                     OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                    properties.store(outputStreamWriter, "Auto Eula agreement by CloudNet");
                }
            }
            break;
            case NUKKIT: {
                File file = new File(this.getDirectory(), "server.properties");
                this.copyDefaultFile("files/nukkit/server.properties", file);

                Properties properties = new Properties();

                try (InputStream inputStream = new FileInputStream(file)) {
                    properties.load(inputStream);
                }

                properties.setProperty("server-port", String.valueOf(this.getServiceConfiguration().getPort()));
                properties.setProperty("server-ip", CloudNet.getInstance().getConfig().getHostAddress());

                try (OutputStream outputStream = new FileOutputStream(file);
                     OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                    properties.store(writer, "Edit by CloudNet");
                }
            }
            break;
            case GLOWSTONE: {
                File file = new File(this.getDirectory(), "config/glowstone.yml");
                file.getParentFile().mkdirs();

                this.copyDefaultFile("files/glowstone/glowstone.yml", file);

                this.rewriteServiceConfigurationFile(file, line -> {
                    if (line.startsWith("    ip: ")) {
                        line = "    ip: '" + CloudNet.getInstance().getConfig().getHostAddress() + "'";
                    }

                    if (line.startsWith("    port: ")) {
                        line = "    port: " + this.getServiceConfiguration().getPort();
                    }

                    return line;
                });
            }
            break;
            default:
                break;
        }
    }

    private void copyDefaultFile(String from, File target) throws Exception {
        if (!target.exists() && target.createNewFile()) {
            try (InputStream inputStream = JVMCloudService.class.getClassLoader().getResourceAsStream(from);
                 OutputStream outputStream = new FileOutputStream(target)) {
                if (inputStream != null) {
                    FileUtils.copy(inputStream, outputStream);
                }
            }
        }
    }

    private void rewriteServiceConfigurationFile(File file, UnaryOperator<String> unaryOperator) throws Exception {
        List<String> lines = Files.readAllLines(file.toPath());
        List<String> replacedLines = new ArrayList<>(lines.size());

        for (String line : lines) {
            replacedLines.add(unaryOperator.apply(line));
        }

        try (OutputStream outputStream = new FileOutputStream(file);
             OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             PrintWriter printWriter = new PrintWriter(outputStreamWriter, true)) {
            for (String replacedLine : replacedLines) {
                printWriter.write(replacedLine + "\n");
                printWriter.flush();
            }
        }
    }

    @Nullable
    private Integer stop0(boolean force) {
        if (this.lifeCycle == ServiceLifeCycle.RUNNING) {
            if (CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePreStopEvent(this)).isCancelled()) {
                return null;
            }

            System.out.println(LanguageManager.getMessage("cloud-service-pre-stop-message")
                    .replace("%task%", this.getServiceId().getTaskName())
                    .replace("%serviceId%", String.valueOf(this.getServiceId().getTaskServiceId()))
                    .replace("%id%", this.getServiceId().getUniqueId().toString())
            );

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

            CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePostStopEvent(this, exitValue));
            CloudNetDriver.getInstance().getLogger().extended(LanguageManager.getMessage("cloud-service-post-stop-message")
                    .replace("%task%", this.getServiceId().getTaskName())
                    .replace("%serviceId%", String.valueOf(this.getServiceId().getTaskServiceId()))
                    .replace("%id%", this.getServiceId().getUniqueId().toString())
                    .replace("%exit_value%", String.valueOf(exitValue))
            );

            this.serviceInfoSnapshot = this.createServiceInfoSnapshot(ServiceLifeCycle.STOPPED);

            CloudNet.getInstance().sendAll(new PacketClientServerServiceInfoPublisher(this.serviceInfoSnapshot, PacketClientServerServiceInfoPublisher.PublisherType.STOPPED));
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

        if (CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePreDeleteEvent(this)).isCancelled()) {
            return;
        }

        System.out.println(LanguageManager.getMessage("cloud-service-pre-delete-message")
                .replace("%task%", this.getServiceId().getTaskName())
                .replace("%serviceId%", String.valueOf(this.getServiceId().getTaskServiceId()))
                .replace("%id%", this.getServiceId().getUniqueId().toString())
        );

        this.deployResources();

        if (!this.getServiceConfiguration().isStaticService()) {
            FileUtils.delete(this.getDirectory());
        }

        this.lifeCycle = ServiceLifeCycle.DELETED;
        this.getCloudServiceManager().getCloudServices().remove(this.getServiceId().getUniqueId());
        this.getCloudServiceManager().getGlobalServiceInfoSnapshots().remove(this.getServiceId().getUniqueId());

        CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePostDeleteEvent(this));
        CloudNetDriver.getInstance().getLogger().extended(LanguageManager.getMessage("cloud-service-post-delete-message")
                .replace("%task%", this.getServiceId().getTaskName())
                .replace("%serviceId%", String.valueOf(this.getServiceId().getTaskServiceId()))
                .replace("%id%", this.getServiceId().getUniqueId().toString())
        );

        this.serviceInfoSnapshot.setLifeCycle(ServiceLifeCycle.DELETED);
        CloudNet.getInstance().publishNetworkClusterNodeInfoSnapshotUpdate();
        CloudNet.getInstance().sendAll(new PacketClientServerServiceInfoPublisher(this.serviceInfoSnapshot, PacketClientServerServiceInfoPublisher.PublisherType.UNREGISTER));
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
