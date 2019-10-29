package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.StringUtil;
import de.dytanic.cloudnet.common.Value;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.encrypt.EncryptTo;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.conf.ConfigurationOptionSSL;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.service.*;
import de.dytanic.cloudnet.event.service.*;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.LocalTemplateStorage;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;
import java.util.jar.JarFile;

final class JVMCloudService implements ICloudService {

    private static final String RUNTIME = "jvm";

    private static final String TEMP_NAME_SPLITTER = "_";

    private static final long SERVICE_ERROR_RESTART_DELAY = 30;

    private static final Lock START_SEQUENCE_LOCK = new ReentrantLock();


    private final List<ServiceRemoteInclusion> includes = Iterables.newArrayList();

    private final List<ServiceTemplate> templates = Iterables.newArrayList();

    private final List<ServiceDeployment> deployments = Iterables.newCopyOnWriteArrayList();

    private final Queue<ServiceRemoteInclusion> waitingIncludes = Iterables.newConcurrentLinkedQueue();

    private final Queue<ServiceTemplate> waitingTemplates = Iterables.newConcurrentLinkedQueue();

    private final DefaultServiceConsoleLogCache serviceConsoleLogCache = new DefaultServiceConsoleLogCache(this);

    private final List<String> groups = Iterables.newArrayList();

    private final Lock lifeCycleLock = new ReentrantLock();


    private volatile ServiceLifeCycle lifeCycle;

    private ICloudServiceManager cloudServiceManager;

    private ServiceConfiguration serviceConfiguration;

    private ServiceId serviceId;

    private File directory;

    private String connectionKey;

    private int configuredMaxHeapMemory;


    private volatile INetworkChannel networkChannel;

    private volatile ServiceInfoSnapshot serviceInfoSnapshot, lastServiceInfoSnapshot;

    private boolean firstStartupOnStaticService = false;


    private Process process;

    private volatile boolean restartState = false;

    JVMCloudService(ICloudServiceManager cloudServiceManager, ServiceConfiguration serviceConfiguration) {
        this.cloudServiceManager = cloudServiceManager;
        this.serviceConfiguration = serviceConfiguration;
        this.connectionKey = StringUtil.generateRandomString(256);

        this.lifeCycle = ServiceLifeCycle.DEFINED;
        this.serviceId = serviceConfiguration.getServiceId();

        this.waitingIncludes.addAll(Arrays.asList(this.serviceConfiguration.getIncludes()));
        this.waitingTemplates.addAll(Arrays.asList(this.serviceConfiguration.getTemplates()));

        this.deployments.addAll(Arrays.asList(this.serviceConfiguration.getDeployments()));

        this.groups.addAll(Arrays.asList(this.serviceConfiguration.getGroups()));
        this.configuredMaxHeapMemory = this.serviceConfiguration.getProcessConfig().getMaxHeapMemorySize();

        this.serviceInfoSnapshot = this.lastServiceInfoSnapshot = createServiceInfoSnapshot(ServiceLifeCycle.DEFINED);

        this.directory =
                serviceConfiguration.isStaticService() ?
                        new File(cloudServiceManager.getPersistenceServicesDirectory(), this.serviceId.getName())
                        :
                        new File(cloudServiceManager.getTempDirectory(), this.serviceId.getName() + TEMP_NAME_SPLITTER + this.serviceId.getUniqueId().toString());

        if (this.serviceConfiguration.isStaticService()) {
            this.firstStartupOnStaticService = !this.directory.exists();
        }

        this.directory.mkdirs();

        this.initAndPrepareService();
    }

    @Override
    public void runCommand(String commandLine) {
        if (this.lifeCycle == ServiceLifeCycle.RUNNING && this.process != null) {
            try {
                OutputStream outputStream = this.process.getOutputStream();
                outputStream.write((commandLine + "\n").getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void start() throws Exception {
        try {
            lifeCycleLock.lock();

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
            lifeCycleLock.unlock();
        }
    }

    @Override
    public void restart() throws Exception {
        restartState = true;

        this.stop();
        this.start();

        restartState = false;
    }

    @Override
    public int stop() {
        try {
            lifeCycleLock.lock();
            return this.stop0(false);
        } finally {
            lifeCycleLock.unlock();

            invokeAutoDeleteOnStopIfNotRestart();
        }
    }

    @Override
    public int kill() {
        try {
            lifeCycleLock.lock();
            return this.stop0(true);
        } finally {
            lifeCycleLock.unlock();

            invokeAutoDeleteOnStopIfNotRestart();
        }
    }

    private void invokeAutoDeleteOnStopIfNotRestart() {
        if (serviceConfiguration.isAutoDeleteOnStop() && !restartState) {
            delete();
        } else {
            this.initAndPrepareService();
        }
    }

    @Override
    public void delete() {
        try {
            lifeCycleLock.lock();
            this.delete0();
        } finally {
            lifeCycleLock.unlock();
        }
    }

    @Override
    public boolean isAlive() {
        return this.lifeCycle == ServiceLifeCycle.DEFINED || this.lifeCycle == ServiceLifeCycle.PREPARED ||
                (this.lifeCycle == ServiceLifeCycle.RUNNING && this.process != null && this.process.isAlive());
    }

    @Override
    public final String getRuntime() {
        return RUNTIME;
    }

    @Override
    public List<ServiceRemoteInclusion> getIncludes() {
        return Collections.unmodifiableList(includes);
    }

    @Override
    public List<ServiceTemplate> getTemplates() {
        return Collections.unmodifiableList(templates);
    }

    private void initAndPrepareService() {
        if (this.lifeCycle == ServiceLifeCycle.DEFINED || this.lifeCycle == ServiceLifeCycle.STOPPED) {
            System.out.println(LanguageManager.getMessage("cloud-service-pre-prepared-message")
                    .replace("%task%", this.serviceId.getTaskName())
                    .replace("%serviceId%", String.valueOf(this.serviceId.getTaskServiceId()))
                    .replace("%id%", this.serviceId.getUniqueId().toString())
            );
            CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePrePrepareEvent(this));

            new File(this.directory, ".wrapper").mkdirs();

            if (CloudNet.getInstance().getConfig().getServerSslConfig().isEnabled()) {
                try {
                    ConfigurationOptionSSL ssl = CloudNet.getInstance().getConfig().getServerSslConfig();

                    File file;
                    if (ssl.getCertificatePath() != null) {
                        file = new File(ssl.getCertificatePath());

                        if (file.exists()) {
                            FileUtils.copy(file, new File(this.directory, ".wrapper/certificate"));
                        }
                    }

                    if (ssl.getPrivateKeyPath() != null) {
                        file = new File(ssl.getPrivateKeyPath());

                        if (file.exists()) {
                            FileUtils.copy(file, new File(this.directory, ".wrapper/privateKey"));
                        }
                    }

                    if (ssl.getTrustCertificatePath() != null) {
                        file = new File(ssl.getTrustCertificatePath());

                        if (file.exists()) {
                            FileUtils.copy(file, new File(this.directory, ".wrapper/trustCertificate"));
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            this.lifeCycle = ServiceLifeCycle.PREPARED;
            CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePostPrepareEvent(this));

            serviceInfoSnapshot.setLifeCycle(ServiceLifeCycle.PREPARED);
            cloudServiceManager.getGlobalServiceInfoSnapshots().put(serviceId.getUniqueId(), serviceInfoSnapshot);
            CloudNet.getInstance().sendAll(new PacketClientServerServiceInfoPublisher(serviceInfoSnapshot, PacketClientServerServiceInfoPublisher.PublisherType.REGISTER));

            System.out.println(LanguageManager.getMessage("cloud-service-post-prepared-message")
                    .replace("%serviceId%", String.valueOf(this.serviceId.getTaskServiceId()))
                    .replace("%task%", this.serviceId.getTaskName())
                    .replace("%id%", this.serviceId.getUniqueId().toString())
            );
        }
    }


    private void start0() throws Exception {
        if (this.lifeCycle == ServiceLifeCycle.PREPARED || this.lifeCycle == ServiceLifeCycle.STOPPED) {
            if (!hasAccessFromNode()) {
                return;
            }

            System.out.println(LanguageManager.getMessage("cloud-service-pre-start-prepared-message")
                    .replace("%task%", this.serviceId.getTaskName())
                    .replace("%serviceId%", String.valueOf(this.serviceId.getTaskServiceId()))
                    .replace("%id%", this.serviceId.getUniqueId().toString())
            );
            CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePreStartPrepareEvent(this));

            this.includeInclusions();
            this.includeTemplates();

            this.serviceConfiguration = new ServiceConfiguration(
                    this.serviceId,
                    this.getRuntime(),
                    this.serviceConfiguration.isAutoDeleteOnStop(),
                    this.serviceConfiguration.isStaticService(),
                    this.serviceConfiguration.getGroups(),
                    this.includes.toArray(new ServiceRemoteInclusion[0]),
                    this.templates.toArray(new ServiceTemplate[0]),
                    this.deployments.toArray(new ServiceDeployment[0]),
                    this.serviceConfiguration.getDeletedFilesAfterStop(),
                    this.serviceConfiguration.getProcessConfig(),
                    this.serviceConfiguration.getProperties(),
                    this.serviceConfiguration.getPort()
            );

            this.serviceInfoSnapshot = this.createServiceInfoSnapshot(ServiceLifeCycle.PREPARED);
            this.cloudServiceManager.getGlobalServiceInfoSnapshots().put(this.serviceInfoSnapshot.getServiceId().getUniqueId(), this.serviceInfoSnapshot);

            new JsonDocument()
                    .append("connectionKey", this.connectionKey)
                    .append("listener", CloudNet.getInstance().getConfig().getIdentity().getListeners()
                            [ThreadLocalRandom.current().nextInt(CloudNet.getInstance().getConfig().getIdentity().getListeners().length)])
                    //-
                    .append("serviceConfiguration", this.serviceConfiguration)
                    .append("serviceInfoSnapshot", this.serviceInfoSnapshot)
                    .append("sslConfig", CloudNet.getInstance().getConfig().getServerSslConfig())
                    .write(new File(this.directory, ".wrapper/wrapper.json"));

            CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePostStartPrepareEvent(this));
            System.out.println(LanguageManager.getMessage("cloud-service-post-start-prepared-message")
                    .replace("%task%", this.serviceId.getTaskName())
                    .replace("%serviceId%", String.valueOf(this.serviceId.getTaskServiceId()))
                    .replace("%id%", this.serviceId.getUniqueId().toString())
            );

            System.out.println(LanguageManager.getMessage("cloud-service-pre-start-message")
                    .replace("%task%", this.serviceId.getTaskName())
                    .replace("%serviceId%", String.valueOf(this.serviceId.getTaskServiceId()))
                    .replace("%id%", this.serviceId.getUniqueId().toString())
            );
            CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePreStartEvent(this));

            this.configureServiceEnvironment();
            this.startWrapper();

            this.lifeCycle = ServiceLifeCycle.RUNNING;
            CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePostStartEvent(this));
            System.out.println(LanguageManager.getMessage("cloud-service-post-start-message")
                    .replace("%serviceId%", String.valueOf(this.serviceId.getTaskServiceId()))
                    .replace("%task%", this.serviceId.getTaskName())
                    .replace("%id%", this.serviceId.getUniqueId().toString())
            );

            this.serviceInfoSnapshot.setLifeCycle(ServiceLifeCycle.RUNNING);
            CloudNet.getInstance().sendAll(new PacketClientServerServiceInfoPublisher(this.serviceInfoSnapshot, PacketClientServerServiceInfoPublisher.PublisherType.STARTED));
        }
    }

    private boolean hasAccessFromNode() {
        if (cloudServiceManager.getCurrentUsedHeapMemory() >= CloudNet.getInstance().getConfig().getMaxMemory()) {
            if (CloudNet.getInstance().getConfig().isRunBlockedServiceStartTryLaterAutomatic()) {
                CloudNet.getInstance().runTask(() -> {
                    try {
                        start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {
                System.out.println(LanguageManager.getMessage("cloud-service-manager-max-memory-error"));
            }

            return false;
        }

        if (CPUUsageResolver.getSystemCPUUsage() >= CloudNet.getInstance().getConfig().getMaxCPUUsageToStartServices()) {
            if (CloudNet.getInstance().getConfig().isRunBlockedServiceStartTryLaterAutomatic()) {
                CloudNet.getInstance().runTask(() -> {
                    try {
                        start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {
                System.out.println(LanguageManager.getMessage("cloud-service-manager-cpu-usage-to-high-error"));
            }

            return false;
        }

        return true;
    }

    private ServiceInfoSnapshot createServiceInfoSnapshot(ServiceLifeCycle lifeCycle) {
        return new ServiceInfoSnapshot(
                System.currentTimeMillis(),
                this.serviceId,
                new HostAndPort(CloudNet.getInstance().getConfig().getHostAddress(), this.serviceConfiguration.getPort()),
                false,
                lifeCycle,
                new ProcessSnapshot(
                        -1,
                        -1,
                        -1,
                        -1,
                        -1,
                        -1,
                        Collections.emptyList(),
                        -1),
                this.serviceConfiguration.getProperties(),
                this.serviceConfiguration
        );
    }

    @Override
    public void includeInclusions() {
        byte[] buffer = new byte[16384];

        while (!this.waitingIncludes.isEmpty()) {
            ServiceRemoteInclusion inclusion = this.waitingIncludes.poll();

            if (inclusion != null && inclusion.getDestination() != null && inclusion.getUrl() != null) {
                try {
                    System.out.println(LanguageManager.getMessage("cloud-service-include-inclusion-message")
                            .replace("%task%", this.serviceId.getTaskName())
                            .replace("%id%", this.serviceId.getUniqueId().toString())
                            .replace("%serviceId%", String.valueOf(this.serviceId.getTaskServiceId()))
                            .replace("%url%", inclusion.getUrl())
                            .replace("%destination%", inclusion.getDestination())
                    );

                    File cacheDestination = new File(
                            System.getProperty("cloudnet.tempDir.includes", "temp/includes"),
                            Base64.getEncoder().encodeToString(EncryptTo.encryptToSHA256(inclusion.getUrl()))
                    );
                    cacheDestination.getParentFile().mkdirs();

                    if (!cacheDestination.exists()) {
                        if (!includeInclusions0(inclusion, cacheDestination, buffer)) {
                            continue;
                        }
                    }

                    try (InputStream inputStream = new FileInputStream(cacheDestination)) {
                        File destination = new File(this.directory, inclusion.getDestination());
                        destination.getParentFile().mkdirs();

                        try (OutputStream outputStream = Files.newOutputStream(destination.toPath())) {
                            FileUtils.copy(inputStream, outputStream, buffer);
                        }
                    }

                    this.includes.add(inclusion);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private boolean includeInclusions0(ServiceRemoteInclusion inclusion, File destination, byte[] buffer) throws Exception {
        URLConnection connection = new URL(inclusion.getUrl()).openConnection();

        CloudServicePreLoadInclusionEvent cloudServicePreLoadInclusionEvent = new CloudServicePreLoadInclusionEvent(this, inclusion, connection);
        CloudNetDriver.getInstance().getEventManager().callEvent(cloudServicePreLoadInclusionEvent);

        if (cloudServicePreLoadInclusionEvent.isCancelled()) {
            return false;
        }

        if (inclusion.getProperties() != null) {
            if (inclusion.getProperties().contains("httpHeaders")) {
                JsonDocument document = inclusion.getProperties().getDocument("httpHeaders");

                for (String key : document) {
                    connection.setRequestProperty(key, document.get(key).toString());
                }
            }
        }

        connection.setDoOutput(false);
        connection.setUseCaches(false);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");

        connection.connect();

        try (InputStream inputStream = connection.getInputStream(); OutputStream outputStream = Files.newOutputStream(destination.toPath())) {
            FileUtils.copy(inputStream, outputStream, buffer);
        }

        return true;
    }

    @Override
    public void includeTemplates() {
        while (!this.waitingTemplates.isEmpty()) {
            ServiceTemplate template = this.waitingTemplates.poll();

            if (template != null && template.getName() != null && template.getPrefix() != null && template.getStorage() != null) {
                ITemplateStorage storage = getStorage(template.getStorage());

                if (!storage.has(template)) {
                    continue;
                }

                CloudServiceTemplateLoadEvent cloudServiceTemplateLoadEvent = new CloudServiceTemplateLoadEvent(this, storage, template);
                CloudNetDriver.getInstance().getEventManager().callEvent(cloudServiceTemplateLoadEvent);

                if (cloudServiceTemplateLoadEvent.isCancelled()) {
                    continue;
                }

                try {
                    if (!this.serviceConfiguration.isStaticService() || template.shouldAlwaysCopyToStaticServices() || this.firstStartupOnStaticService) {
                        System.out.println(LanguageManager.getMessage("cloud-service-include-template-message")
                                .replace("%task%", this.serviceId.getTaskName())
                                .replace("%id%", this.serviceId.getUniqueId().toString())
                                .replace("%serviceId%", String.valueOf(this.serviceId.getTaskServiceId()))
                                .replace("%template%", template.getTemplatePath())
                                .replace("%storage%", template.getStorage())
                        );

                        storage.copy(template, this.directory);
                    }

                    this.templates.add(template);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    @Override
    public void deployResources(boolean removeDeployments) {
        for (ServiceDeployment deployment : this.deployments) {
            if (deployment != null) {
                if (deployment.getTemplate() != null && deployment.getTemplate().getStorage() != null && deployment.getTemplate().getPrefix() != null &&
                        deployment.getTemplate().getName() != null) {
                    ITemplateStorage storage = getStorage(deployment.getTemplate().getStorage());

                    CloudServiceDeploymentEvent cloudServiceDeploymentEvent = new CloudServiceDeploymentEvent(this, storage, deployment);
                    CloudNetDriver.getInstance().getEventManager().callEvent(cloudServiceDeploymentEvent);

                    if (cloudServiceDeploymentEvent.isCancelled()) {
                        continue;
                    }

                    System.out.println(LanguageManager.getMessage("cloud-service-deploy-message")
                            .replace("%task%", this.serviceId.getTaskName())
                            .replace("%id%", this.serviceId.getUniqueId().toString())
                            .replace("%serviceId%", String.valueOf(this.serviceId.getTaskServiceId()))
                            .replace("%template%", deployment.getTemplate().getTemplatePath())
                            .replace("%storage%", deployment.getTemplate().getStorage())
                    );

                    storage.deploy(
                            Objects.requireNonNull(this.directory.listFiles(pathname -> {
                                if (deployment.getExcludes() != null) {
                                    return !deployment.getExcludes().contains(pathname.isDirectory() ? pathname.getName() + "/" : pathname.getName()) && !pathname
                                            .getName().equals("wrapper.jar") && !pathname.getName().equals(".wrapper");
                                } else {
                                    return true;
                                }
                            })),
                            deployment.getTemplate()
                    );

                    if (removeDeployments) {
                        this.deployments.remove(deployment);
                    }

                    if (storage instanceof LocalTemplateStorage) {
                        CloudNet.getInstance().deployTemplateInCluster(deployment.getTemplate(), storage.toZipByteArray(deployment.getTemplate()));
                    }
                }
            }
        }
    }

    private void startWrapper() throws Exception {
        this.configuredMaxHeapMemory = this.serviceConfiguration.getProcessConfig().getMaxHeapMemorySize();

        List<String> commandArguments = Iterables.newArrayList();

        commandArguments.add(CloudNet.getInstance().getConfig().getJVMCommand());

        if (CloudNet.getInstance().getConfig().isDefaultJVMOptionParameters()) {
            commandArguments.addAll(Arrays.asList(
                    "-XX:+UseG1GC",
                    "-XX:MaxGCPauseMillis=50",
                    "-XX:-UseAdaptiveSizePolicy",
                    "-XX:CompileThreshold=100",
                    "-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseCompressedOops"
            ));
        }

        commandArguments.addAll(Arrays.asList(
                // sys properties
                "-Dcom.mojang.eula.agree=true",
                "-Djline.terminal=jline.UnsupportedTerminal",
                "-Dfile.encoding=UTF-8",
                "-Dio.netty.noPreferDirect=true",
                "-Dclient.encoding.override=UTF-8",
                "-Dio.netty.maxDirectMemory=0",
                "-Dio.netty.leakDetectionLevel=DISABLED",
                "-Dio.netty.recycler.maxCapacity=0",
                "-Dio.netty.recycler.maxCapacity.default=0",
                "-DIReallyKnowWhatIAmDoingISwear=true",
                "-Dcloudnet.wrapper.receivedMessages.language=" + LanguageManager.getLanguage()
        ));

        File wrapperFile = new File(System.getProperty("cloudnet.tempDir", "temp"), "caches/wrapper.jar");

        Optional<File> applicationFileOptional = Files.list(this.directory.toPath())
                .map(Path::toFile)
                .filter(file -> file.getName().endsWith(".jar"))
                .filter(file -> {
                    for (ServiceEnvironment environment : this.serviceConfiguration.getProcessConfig().getEnvironment().getEnvironments()) {
                        if (file.getName().toLowerCase()
                                .contains(environment.getName().toLowerCase())) {
                            return true;
                        }
                    }
                    return false;
                })
                .findFirst();

        if (!applicationFileOptional.isPresent()) {
            CloudNetDriver.getInstance().getLogger().error(LanguageManager.getMessage("cloud-service-jar-file-not-found-error")
                    .replace("%task%", this.serviceId.getTaskName())
                    .replace("%serviceId%", String.valueOf(this.serviceId.getTaskServiceId()))
                    .replace("%id%", this.serviceId.getUniqueId().toString())
                    .replace("%time%", String.valueOf(SERVICE_ERROR_RESTART_DELAY)));

            ServiceTask serviceTask = this.getCloudServiceManager().getServiceTask(this.getServiceId().getTaskName());
            serviceTask.forbidServiceStarting(SERVICE_ERROR_RESTART_DELAY * 1000);

            this.stop();
            return;
        }

        File applicationFile = applicationFileOptional.get();

        commandArguments.addAll(this.serviceConfiguration.getProcessConfig().getJvmOptions());
        commandArguments.addAll(Arrays.asList(
                "-Xmx" + this.serviceConfiguration.getProcessConfig().getMaxHeapMemorySize() + "M",
                "-javaagent:" + wrapperFile.getAbsolutePath(),
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
                .directory(directory)
                .start();
    }

    private void postConfigureServiceEnvironmentStartParameters(List<String> commandArguments) {
        switch (this.serviceConfiguration.getProcessConfig().getEnvironment()) {
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
                line = "    host: " + CloudNet.getInstance().getConfig().getHostAddress() + ":" + serviceConfiguration.getPort();
            } else if (line.startsWith("  host: ")) {
                line = "  host: " + CloudNet.getInstance().getConfig().getHostAddress() + ":" + serviceConfiguration.getPort();
            }

            return line;
        });
    }

    private void configureServiceEnvironment() throws Exception {
        switch (this.serviceConfiguration.getProcessConfig().getEnvironment()) {
            case BUNGEECORD: {
                File file = new File(this.directory, "config.yml");
                this.copyDefaultFile("files/bungee/config.yml", file);

                this.rewriteBungeeConfig(file);
            }
            break;
            case WATERDOG: {
                File file = new File(this.directory, "config.yml");
                this.copyDefaultFile("files/waterdog/config.yml", file);

                this.rewriteBungeeConfig(file);
            }
            break;
            case VELOCITY: {
                File file = new File(this.directory, "velocity.toml");
                this.copyDefaultFile("files/velocity/velocity.toml", file);

                Value<Boolean> value = new Value<>(true);

                this.rewriteServiceConfigurationFile(file, line -> {
                    if (value.getValue() && line.startsWith("bind =")) {
                        value.setValue(false);
                        return "bind = \"" + CloudNet.getInstance().getConfig().getHostAddress() + ":" + serviceConfiguration.getPort() + "\"";
                    }

                    return line;
                });
            }
            break;
            case PROX_PROX: {
                File file = new File(this.directory, "config.yml");
                this.copyDefaultFile("files/proxprox/config.yml", file);

                this.rewriteServiceConfigurationFile(file, line -> {
                    if (line.startsWith("ip: ")) {
                        line = "ip: " + CloudNet.getInstance().getConfig().getHostAddress();
                    }

                    if (line.startsWith("port: ")) {
                        line = "port: " + serviceConfiguration.getPort();
                    }

                    return line;
                });
            }
            break;
            case MINECRAFT_SERVER: {
                File file = new File(this.directory, "server.properties");
                this.copyDefaultFile("files/nms/server.properties", file);

                Properties properties = new Properties();

                try (InputStream inputStream = new FileInputStream(file)) {
                    properties.load(inputStream);
                }

                properties.setProperty("server-name", this.serviceId.getName());
                properties.setProperty("server-port", String.valueOf(this.serviceConfiguration.getPort()));
                properties.setProperty("server-ip", CloudNet.getInstance().getConfig().getHostAddress());

                try (OutputStream outputStream = new FileOutputStream(file);
                     OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                    properties.store(writer, "Edit by CloudNet");
                }

                properties = new Properties();

                file = new File(this.directory, "eula.txt");
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
                File file = new File(this.directory, "server.properties");
                this.copyDefaultFile("files/nukkit/server.properties", file);

                Properties properties = new Properties();

                try (InputStream inputStream = new FileInputStream(file)) {
                    properties.load(inputStream);
                }

                properties.setProperty("server-port", String.valueOf(this.serviceConfiguration.getPort()));
                properties.setProperty("server-ip", CloudNet.getInstance().getConfig().getHostAddress());

                try (OutputStream outputStream = new FileOutputStream(file);
                     OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                    properties.store(writer, "Edit by CloudNet");
                }
            }
            break;
            case GO_MINT: {
                File file = new File(this.directory, "server.yml");
                this.copyDefaultFile("files/gomint/server.yml", file);

                this.rewriteServiceConfigurationFile(file, line -> {
                    if (line.startsWith("  ip: ")) {
                        line = "  ip: " + CloudNet.getInstance().getConfig().getHostAddress();
                    }

                    if (line.startsWith("  port: ")) {
                        line = "  port: " + serviceConfiguration.getPort();
                    }

                    return line;
                });
            }
            break;
            case GLOWSTONE: {
                File file = new File(this.directory, "config/glowstone.yml");
                file.getParentFile().mkdirs();

                copyDefaultFile("files/glowstone/glowstone.yml", file);

                this.rewriteServiceConfigurationFile(file, line -> {
                    if (line.startsWith("    ip: ")) {
                        line = "    ip: '" + CloudNet.getInstance().getConfig().getHostAddress() + "'";
                    }

                    if (line.startsWith("    port: ")) {
                        line = "    port: " + serviceConfiguration.getPort();
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
        List<String> replacedLines = Iterables.newArrayList(lines.size());

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

    private int stop0(boolean force) {
        if (this.lifeCycle == ServiceLifeCycle.RUNNING) {

            System.out.println(LanguageManager.getMessage("cloud-service-pre-stop-message")
                    .replace("%task%", this.serviceId.getTaskName())
                    .replace("%serviceId%", String.valueOf(this.serviceId.getTaskServiceId()))
                    .replace("%id%", this.serviceId.getUniqueId().toString())
            );
            CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePreStopEvent(this));

            int exitValue = this.stopProcess(force);

            if (this.networkChannel != null) {
                try {
                    this.networkChannel.close();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }

            this.lifeCycle = ServiceLifeCycle.STOPPED;

            if (this.serviceConfiguration.getDeletedFilesAfterStop() != null) {
                for (String path : this.serviceConfiguration.getDeletedFilesAfterStop()) {
                    if (path != null) {
                        File file = new File(this.directory, path);
                        if (file.exists()) {
                            FileUtils.delete(file);
                        }
                    }
                }
            }

            CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePostStopEvent(this, exitValue));
            System.out.println(LanguageManager.getMessage("cloud-service-post-stop-message")
                    .replace("%task%", this.serviceId.getTaskName())
                    .replace("%serviceId%", String.valueOf(this.serviceId.getTaskServiceId()))
                    .replace("%id%", this.serviceId.getUniqueId().toString())
                    .replace("%exit_value%", String.valueOf(exitValue))
            );

            this.serviceInfoSnapshot = createServiceInfoSnapshot(ServiceLifeCycle.STOPPED);

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

                    if (process.waitFor(5, TimeUnit.SECONDS)) {
                        return process.exitValue();
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

        if (this.lifeCycle == ServiceLifeCycle.RUNNING) {
            this.stop0(true);
        }

        System.out.println(LanguageManager.getMessage("cloud-service-pre-delete-message")
                .replace("%task%", this.serviceId.getTaskName())
                .replace("%serviceId%", String.valueOf(this.serviceId.getTaskServiceId()))
                .replace("%id%", this.serviceId.getUniqueId().toString())
        );
        CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePreDeleteEvent(this));

        this.deployResources();

        if (!this.serviceConfiguration.isStaticService()) {
            FileUtils.delete(this.directory);
        }

        this.lifeCycle = ServiceLifeCycle.DELETED;
        this.cloudServiceManager.getCloudServices().remove(this.serviceId.getUniqueId());
        this.cloudServiceManager.getGlobalServiceInfoSnapshots().remove(this.serviceId.getUniqueId());

        CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePostDeleteEvent(this));
        System.out.println(LanguageManager.getMessage("cloud-service-post-delete-message")
                .replace("%task%", this.serviceId.getTaskName())
                .replace("%serviceId%", String.valueOf(this.serviceId.getTaskServiceId()))
                .replace("%id%", this.serviceId.getUniqueId().toString())
        );

        this.serviceInfoSnapshot.setLifeCycle(ServiceLifeCycle.DELETED);
        CloudNet.getInstance().publishNetworkClusterNodeInfoSnapshotUpdate();
        CloudNet.getInstance().sendAll(new PacketClientServerServiceInfoPublisher(this.serviceInfoSnapshot, PacketClientServerServiceInfoPublisher.PublisherType.UNREGISTER));
    }

    private ITemplateStorage getStorage(String storageName) {
        ITemplateStorage storage;

        if (CloudNetDriver.getInstance().getServicesRegistry().containsService(ITemplateStorage.class, storageName)) {
            storage = CloudNetDriver.getInstance().getServicesRegistry().getService(ITemplateStorage.class, storageName);
        } else {
            storage = CloudNetDriver.getInstance().getServicesRegistry().getService(ITemplateStorage.class, LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE);
        }

        return storage;
    }

    public List<ServiceDeployment> getDeployments() {
        return this.deployments;
    }

    public Queue<ServiceRemoteInclusion> getWaitingIncludes() {
        return this.waitingIncludes;
    }

    public Queue<ServiceTemplate> getWaitingTemplates() {
        return this.waitingTemplates;
    }

    public DefaultServiceConsoleLogCache getServiceConsoleLogCache() {
        return this.serviceConsoleLogCache;
    }

    public List<String> getGroups() {
        return this.groups;
    }

    public Lock getLifeCycleLock() {
        return this.lifeCycleLock;
    }

    public ServiceLifeCycle getLifeCycle() {
        return this.lifeCycle;
    }

    public ICloudServiceManager getCloudServiceManager() {
        return this.cloudServiceManager;
    }

    public ServiceConfiguration getServiceConfiguration() {
        return this.serviceConfiguration;
    }

    public ServiceId getServiceId() {
        return this.serviceId;
    }

    public File getDirectory() {
        return this.directory;
    }

    public String getConnectionKey() {
        return this.connectionKey;
    }

    public int getConfiguredMaxHeapMemory() {
        return this.configuredMaxHeapMemory;
    }

    public INetworkChannel getNetworkChannel() {
        return this.networkChannel;
    }

    public void setNetworkChannel(INetworkChannel networkChannel) {
        this.networkChannel = networkChannel;
    }

    public ServiceInfoSnapshot getServiceInfoSnapshot() {
        return this.serviceInfoSnapshot;
    }

    @Override
    public void setServiceInfoSnapshot(ServiceInfoSnapshot serviceInfoSnapshot) {
        this.lastServiceInfoSnapshot = this.serviceInfoSnapshot;
        this.serviceInfoSnapshot = serviceInfoSnapshot;
    }

    public ServiceInfoSnapshot getLastServiceInfoSnapshot() {
        return this.lastServiceInfoSnapshot;
    }

    public Process getProcess() {
        return this.process;
    }

    public boolean isRestartState() {
        return this.restartState;
    }
}
