package de.dytanic.cloudnet.template.install.run.step;


import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.console.animation.progressbar.ProgressBarInputStream;
import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.run.InstallInformation;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public enum InstallStep {

    DOWNLOAD {
        @Override
        public @NotNull Set<Path> execute(@NotNull InstallInformation installInformation, @NotNull Path workingDirectory, @NotNull Set<Path> inputPaths) throws IOException {
            Path targetPath = workingDirectory.resolve(Paths.get(installInformation.getServiceVersionType().getTargetEnvironment().getName() + ".jar"));

            try (InputStream inputStream = ProgressBarInputStream.wrapDownload(CloudNet.getInstance().getConsole(), new URL(installInformation.getServiceVersion().getUrl()))) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

                return new HashSet<>(Collections.singleton(targetPath));
            }
        }
    },
    BUILD {
        private final Collection<Process> runningBuildProcesses = new CopyOnWriteArrayList<>();

        @Override
        public @NotNull Set<Path> execute(@NotNull InstallInformation installInformation, @NotNull Path workingDirectory, @NotNull Set<Path> inputPaths) throws IOException {
            ServiceVersion version = installInformation.getServiceVersion();

            Collection<String> jvmOptions = version.getProperties().get("jvmOptions", STRING_LIST_TYPE);
            List<String> parameters = version.getProperties().get("parameters", STRING_LIST_TYPE, new ArrayList<>());

            for (Path path : inputPaths) {
                List<String> processArguments = new ArrayList<>();
                processArguments.add(CloudNet.getInstance().getConfig().getJVMCommand());
                if (jvmOptions != null) {
                    processArguments.addAll(jvmOptions);
                }
                processArguments.add("-jar");
                processArguments.add(path.getFileName().toString());
                processArguments.addAll(
                        parameters.stream()
                                .map(parameter -> parameter.replace("%version%", version.getName()))
                                .collect(Collectors.toList())
                );

                int expectedExitCode = version.getProperties().getInt("exitCode", 0);
                int exitCode = this.buildProcessAndWait(processArguments, workingDirectory);

                if (exitCode != expectedExitCode) {
                    throw new IllegalStateException(String.format("Process returned unexpected exit code! Got %d, expected %d", exitCode, expectedExitCode));
                }
            }

            return Files.walk(workingDirectory).collect(Collectors.toSet());
        }

        @Override
        public void interrupt() {
            for (Process runningBuildProcess : this.runningBuildProcesses) {
                runningBuildProcess.destroyForcibly();
            }
        }

        private int buildProcessAndWait(List<String> arguments, Path workingDir) {
            try {
                Process process = new ProcessBuilder()
                        .command(arguments)
                        .directory(workingDir.toFile())
                        .start();

                this.runningBuildProcesses.add(process);

                OUTPUT_READER_EXECUTOR.execute(new BuildStepOutput(process.getInputStream(), System.out));
                OUTPUT_READER_EXECUTOR.execute(new BuildStepOutput(process.getErrorStream(), System.err));

                int exitCode = process.waitFor();
                this.runningBuildProcesses.remove(process);

                return exitCode;
            } catch (IOException | InterruptedException exception) {
                exception.printStackTrace();
            }
            return -1;
        }
    },
    UNZIP {
        @Override
        public @NotNull Set<Path> execute(@NotNull InstallInformation installInformation, @NotNull Path workingDirectory, @NotNull Set<Path> inputPaths) throws IOException {
            Set<Path> resultPaths = new HashSet<>();

            for (Path path : inputPaths) {
                try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(path))) {
                    ZipEntry entry;
                    while ((entry = zipInputStream.getNextEntry()) != null) {
                        Path targetPath = workingDirectory.resolve(entry.getName());

                        if (!targetPath.normalize().startsWith(workingDirectory)) {
                            throw new IllegalStateException("Zip entry path contains traversal element!");
                        }

                        resultPaths.add(targetPath);

                        if (entry.isDirectory()) {
                            Files.createDirectory(targetPath);
                        } else {
                            Files.copy(zipInputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            }

            return resultPaths;
        }
    },
    COPY_FILTER {
        @Override
        public @NotNull Set<Path> execute(@NotNull InstallInformation installInformation, @NotNull Path workingDirectory, @NotNull Set<Path> inputPaths) throws IOException {
            Map<String, String> copy = installInformation.getServiceVersion().getProperties().get("copy", STRING_MAP_TYPE);

            if (copy == null) {
                throw new IllegalStateException(String.format("Missing copy property on service version %s!", installInformation.getServiceVersion().getName()));
            }

            List<Map.Entry<Pattern, String>> patterns = copy.entrySet().stream()
                    .map(entry -> new AbstractMap.SimpleEntry<>(Pattern.compile(entry.getKey()), entry.getValue()))
                    .collect(Collectors.toList());

            Set<Path> resultPaths = new HashSet<>();

            for (Path path : inputPaths) {
                String relativePath = workingDirectory.relativize(path).toString().replace("\\", "/").toLowerCase();

                for (Map.Entry<Pattern, String> patternEntry : patterns) {
                    Pattern pattern = patternEntry.getKey();
                    Path targetPath = workingDirectory.resolve(patternEntry.getValue().replace("%fileName%", path.getFileName().toString()));

                    if (pattern.matcher(relativePath).matches()) {
                        Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        resultPaths.add(targetPath);
                    }
                }
            }

            return resultPaths;
        }
    },
    DEPLOY {
        @Override
        public @NotNull Set<Path> execute(@NotNull InstallInformation installInformation, @NotNull Path workingDirectory, @NotNull Set<Path> inputPaths) throws IOException {
            for (Path path : inputPaths) {
                if (Files.isDirectory(path)) {
                    continue;
                }

                String relativePath = workingDirectory.relativize(path).toString().replace("\\", "/");

                try (OutputStream outputStream = installInformation.getTemplateStorage().newOutputStream(installInformation.getServiceTemplate(), relativePath)) {
                    Files.copy(path, Objects.requireNonNull(outputStream));
                }
            }
            return inputPaths;
        }
    };

    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();
    private static final Type STRING_MAP_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();
    private static final ExecutorService OUTPUT_READER_EXECUTOR = Executors.newCachedThreadPool();

    public abstract @NotNull Set<Path> execute(@NotNull InstallInformation installInformation, @NotNull Path workingDirectory, @NotNull Set<Path> inputPaths) throws IOException;

    public void interrupt() {
    }

}
