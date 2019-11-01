package de.dytanic.cloudnet.template.install;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.service.ServiceEnvironment;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.install.installer.ProcessingServiceVersionInstaller;
import de.dytanic.cloudnet.template.install.installer.ServiceVersionInstaller;
import de.dytanic.cloudnet.template.install.installer.SimpleDownloadingServiceVersionInstaller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ServiceVersionProvider {

    private final Map<ServiceVersionType.InstallerType, ServiceVersionInstaller> installers = new HashMap<>();

    private final Map<String, ServiceVersionType> serviceVersionTypes = new HashMap<>();

    public ServiceVersionProvider() {
        this.installers.put(ServiceVersionType.InstallerType.DOWNLOAD, new SimpleDownloadingServiceVersionInstaller());
        this.installers.put(ServiceVersionType.InstallerType.BUILD, new ProcessingServiceVersionInstaller());
    }

    public boolean loadServiceVersionTypes(String url) throws IOException {
        this.serviceVersionTypes.clear();

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");

        connection.connect();

        boolean success = false;

        try (InputStream inputStream = connection.getInputStream()) {
            JsonDocument document = new JsonDocument().read(inputStream);

            if (document.contains("versions")) {
                Collection<ServiceVersionType> versions = document.get("versions", TypeToken.getParameterized(Collection.class, ServiceVersionType.class).getType());

                for (ServiceVersionType serviceVersionType : versions) {
                    this.registerServiceVersionType(serviceVersionType);
                }

                success = true;
            }
        }

        connection.disconnect();
        if (success) {
            return true;
        }

        for (ServiceVersionType serviceVersionType : this.createDefaultVersionTypes()) {
            this.registerServiceVersionType(serviceVersionType);
        }

        return false;
    }

    public void registerServiceVersionType(ServiceVersionType serviceVersionType) {
        this.serviceVersionTypes.put(serviceVersionType.getName().toLowerCase(), serviceVersionType);
    }

    public Optional<ServiceVersionType> getServiceVersionType(String name) {
        return Optional.ofNullable(this.serviceVersionTypes.get(name.toLowerCase()));
    }

    public boolean canBuildVersion(ServiceVersion serviceVersion, ServiceVersionType.InstallerType installerType) {
        return !installerType.requiresSpecificJavaVersionToExecute() || serviceVersion.canRunOn(JavaVersion.getRuntimeVersion());
    }

    public void installServiceVersion(ServiceVersionType serviceVersionType, ServiceVersion serviceVersion, ITemplateStorage storage, ServiceTemplate serviceTemplate) {
        if (!this.canBuildVersion(serviceVersion, serviceVersionType.getInstallerType())) {
            throw new IllegalStateException("Cannot run " + serviceVersionType.getName() + "-" + serviceVersion.getName() + "#" + serviceVersionType.getInstallerType() + " on " + JavaVersion.getRuntimeVersion().getName());
        }

        ServiceVersionInstaller installer = this.installers.get(serviceVersionType.getInstallerType());

        if (installer == null) {
            throw new IllegalArgumentException("Installer for type " + serviceVersionType.getInstallerType() + " not found");
        }

        if (!storage.has(serviceTemplate)) {
            storage.create(serviceTemplate);
        }

        Path workingDirectory = Paths.get(System.getProperty("cloudnet.tempDir.build", "temp/build"), UUID.randomUUID().toString());
        try (OutputStream outputStream = storage.newOutputStream(serviceTemplate, serviceVersionType.getTargetEnvironment().getName() + ".jar")) {
            Files.createDirectories(workingDirectory);

            installer.install(serviceVersion, workingDirectory, outputStream);

            FileUtils.delete(workingDirectory.toFile());
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public Collection<ServiceVersionType> createDefaultVersionTypes() {
        return Arrays.asList(
                this.createDefaultPaperVersionType(),
                this.createDefaultBuildToolsVersionType(),
                this.createDefaultSpongeVanillaVersionType(),
                this.createDefaultSpongeForgeVersionType(),
                new ServiceVersionType(
                        "glowstone",
                        ServiceEnvironment.GLOWSTONE_DEFAULT,
                        ServiceVersionType.InstallerType.DOWNLOAD,
                        Collections.singletonList(new ServiceVersion(
                                "1.8.9",
                                "https://github.com/GlowstoneMC/Glowstone/releases/download/v1.8.9/glowstone.-1.8.9-SNAPSHOT.jar"
                        ))
                ),
                new ServiceVersionType(
                        "waterdog",
                        ServiceEnvironment.WATERDOG_DEFAULT,
                        ServiceVersionType.InstallerType.DOWNLOAD,
                        Collections.singletonList(new ServiceVersion(
                                "latest",
                                "https://ci.codemc.org/job/yesdog/job/Waterdog/lastStableBuild/artifact/Waterfall-Proxy/bootstrap/target/Waterdog.jar"
                        ))
                ),
                new ServiceVersionType(
                        "velocity",
                        ServiceEnvironment.VELOCITY_DEFAULT,
                        ServiceVersionType.InstallerType.DOWNLOAD,
                        Collections.singletonList(new ServiceVersion(
                                "latest",
                                "https://ci.velocitypowered.com/job/velocity/lastSuccessfulBuild/artifact/proxy/build/libs/velocity-proxy-1.0.4-SNAPSHOT-all.jar"
                        ))
                ),
                new ServiceVersionType(
                        "nukkit",
                        ServiceEnvironment.NUKKIT_DEFAULT,
                        ServiceVersionType.InstallerType.DOWNLOAD,
                        Collections.singletonList(new ServiceVersion(
                                "latest",
                                "https://ci.nukkitx.com/job/NukkitX/job/Nukkit/job/master/lastSuccessfulBuild/artifact/target/nukkit-1.0-SNAPSHOT.jar"
                        ))
                )
        );
    }

    private ServiceVersionType createDefaultSpongeVanillaVersionType() {
        return new ServiceVersionType(
                "spnogevanilla",
                ServiceEnvironment.MINECRAFT_SERVER_SPONGE_VANILLA,
                ServiceVersionType.InstallerType.DOWNLOAD,
                Arrays.asList(
                        new ServiceVersion(
                                "1.12.2",
                                "https://repo.spongepowered.org/maven/org/spongepowered/spongevanilla/1.12.2-7.1.2/spongevanilla-1.12.2-7.1.2.jar",
                                JavaVersion.JAVA_8, JavaVersion.JAVA_8
                        ),
                        new ServiceVersion(
                                "1.11.2",
                                "https://repo.spongepowered.org/maven/org/spongepowered/spongevanilla/1.11.2-6.1.0-BETA-27/spongevanilla-1.11.2-6.1.0-BETA-27.jar",
                                JavaVersion.JAVA_8, JavaVersion.JAVA_8
                        ),
                        new ServiceVersion(
                                "1.10.2",
                                "https://repo.spongepowered.org/maven/org/spongepowered/spongevanilla/1.10.2-5.2.0-BETA-403/spongevanilla-1.10.2-5.2.0-BETA-403.jar",
                                JavaVersion.JAVA_8, JavaVersion.JAVA_8
                        ),
                        new ServiceVersion(
                                "1.8.9",
                                "https://repo.spongepowered.org/maven/org/spongepowered/spongevanilla/1.10.2-5.2.0-BETA-403/spongevanilla-1.10.2-5.2.0-BETA-403.jar",
                                JavaVersion.JAVA_8, JavaVersion.JAVA_8
                        )
                )
        );
    }

    private ServiceVersionType createDefaultSpongeForgeVersionType() {
        return new ServiceVersionType(
                "spongeforge",
                ServiceEnvironment.MINECRAFT_SERVER_SPONGE_FORGE,
                ServiceVersionType.InstallerType.DOWNLOAD,
                Arrays.asList(
                        new ServiceVersion(
                                "1.12.2",
                                "https://repo.spongepowered.org/maven/org/spongepowered/spongeforge/1.12.2-2768-7.1.2/spongeforge-1.12.2-2768-7.1.2.jar",
                                JavaVersion.JAVA_8, JavaVersion.JAVA_8
                        ),
                        new ServiceVersion(
                                "1.11.2",
                                "https://repo.spongepowered.org/maven/org/spongepowered/spongeforge/1.11.2-2476-6.1.0-BETA-2792/spongeforge-1.11.2-2476-6.1.0-BETA-2792.jar",
                                JavaVersion.JAVA_8, JavaVersion.JAVA_8
                        ),
                        new ServiceVersion(
                                "1.10.2",
                                "https://repo.spongepowered.org/maven/org/spongepowered/spongeforge/1.10.2-2477-5.2.0-BETA-2793/spongeforge-1.10.2-2477-5.2.0-BETA-2793.jar",
                                JavaVersion.JAVA_8, JavaVersion.JAVA_8
                        )
                )
        );
    }

    private ServiceVersionType createDefaultBuildToolsVersionType() {
        JsonDocument properties = new JsonDocument().append("copy", "spigot-.*\\.jar")
                .append("parameters", new String[]{"--rev", "%version%"});
        String url = "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar";
        return new ServiceVersionType(
                "spigot",
                ServiceEnvironment.MINECRAFT_SERVER_SPIGOT,
                ServiceVersionType.InstallerType.BUILD,
                Arrays.asList(
                        new ServiceVersion("latest", url, JavaVersion.JAVA_11, null, properties),
                        new ServiceVersion("1.14.4", url, JavaVersion.JAVA_8, JavaVersion.JAVA_13, properties),
                        new ServiceVersion("1.12.2", url, JavaVersion.JAVA_8, JavaVersion.JAVA_10, properties),
                        new ServiceVersion("1.11.2", url, JavaVersion.JAVA_7, JavaVersion.JAVA_8, properties),
                        new ServiceVersion("1.10.2", url, JavaVersion.JAVA_7, JavaVersion.JAVA_8, properties),
                        new ServiceVersion("1.9.4", url, JavaVersion.JAVA_7, JavaVersion.JAVA_8, properties),
                        new ServiceVersion("1.8.8", url, JavaVersion.JAVA_7, JavaVersion.JAVA_8, properties)
                )
        );
    }

    private ServiceVersionType createDefaultPaperVersionType() {
        JsonDocument paperclipProperties = new JsonDocument().append("copy", "cache/patches.*\\.jar");
        return new ServiceVersionType(
                "paperspigot",
                ServiceEnvironment.MINECRAFT_SERVER_PAPER_SPIGOT,
                ServiceVersionType.InstallerType.BUILD,
                Arrays.asList(
                        new ServiceVersion(
                                "1.14.4",
                                "https://papermc.io/ci/job/Paper-1.14/lastSuccessfulBuild/artifact/paperclip.jar",
                                paperclipProperties
                        ),
                        new ServiceVersion(
                                "1.13.2",
                                "https://papermc.io/ci/job/Paper-1.13/lastSuccessfulBuild/artifact/paperclip.jar",
                                paperclipProperties
                        ),
                        new ServiceVersion(
                                "1.12.2",
                                "https://papermc.io/ci/job/Paper/lastSuccessfulBuild/artifact/paperclip-1618.jar",
                                paperclipProperties
                        ),
                        new ServiceVersion(
                                "1.11.2",
                                "https://papermc.io/ci/job/Paper/1104/artifact/paperclip.jar",
                                paperclipProperties
                        ),
                        new ServiceVersion(
                                "1.10.2",
                                "https://papermc.io/ci/job/Paper/916/artifact/paperclip-916.2.jar",
                                paperclipProperties
                        ),
                        new ServiceVersion(
                                "1.9.4",
                                "https://papermc.io/ci/job/Paper/773/artifact/paperclip-773.jar",
                                paperclipProperties
                        ),
                        new ServiceVersion(
                                "1.8.8",
                                "https://papermc.io/ci/job/Paper/443/artifact/paperclip-1.8.8-fix.jar",
                                JavaVersion.JAVA_7, JavaVersion.JAVA_8,
                                paperclipProperties
                        )
                )
        );
    }

    public Map<String, ServiceVersionType> getServiceVersionTypes() {
        return this.serviceVersionTypes;
    }

    //maybe we can implement those direct downloads for spigot instead of using the buildtools?
    /*

                    new ServiceVersionType(
                        "spigot",
                        ServiceEnvironment.MINECRAFT_SERVER_SPIGOT,
                        ServiceVersionType.InstallerType.DOWNLOAD,
                        Arrays.asList(
                                new ServiceVersion(
                                        "1.14.4",
                                        "https://cdn.getbukkit.org/spigot/spigot-1.14.4.jar"
                                ),
                                new ServiceVersion(
                                        "1.13.2",
                                        "https://cdn.getbukkit.org/spigot/spigot-1.13.2.jar"
                                ),
                                new ServiceVersion(
                                        "1.12.2",
                                        "https://cdn.getbukkit.org/spigot/spigot-1.12.2.jar"
                                ),
                                new ServiceVersion(
                                        "1.11.2",
                                        "https://cdn.getbukkit.org/spigot/spigot-1.11.2.jar"
                                ),
                                new ServiceVersion(
                                        "1.10.2",
                                        "https://cdn.getbukkit.org/spigot/spigot-1.10.2-R0.1-SNAPSHOT-latest.jar"
                                ),
                                new ServiceVersion(
                                        "1.9.4",
                                        "https://cdn.getbukkit.org/spigot/spigot-1.9.4-R0.1-SNAPSHOT-latest.jar"
                                ),
                                new ServiceVersion(
                                        "1.8.8",
                                        "https://cdn.getbukkit.org/spigot/spigot-1.8.8-R0.1-SNAPSHOT-latest.jar"
                                )
                        )
                )

     */

}
