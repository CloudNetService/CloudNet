package de.dytanic.cloudnet.template.install.installer.processing;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.progressbar.ProgressBarInputStream;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.installer.ServiceVersionInstaller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ProcessingServiceVersionInstaller implements ServiceVersionInstaller {

    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();
    private static final String DOWNLOAD_ARTIFACT_NAME = "download.jar";
    private static final ExecutorService OUTPUT_READER_EXECUTOR = Executors.newCachedThreadPool();

    private Collection<Process> runningBuildProcesses = new CopyOnWriteArrayList<>();

    @Override
    public void install(ServiceVersion version, String fileName, Path workingDirectory, ITemplateStorage storage, ServiceTemplate targetTemplate, Path cachePath) throws Exception {
        String[] copy = version.getProperties().get("copy", String[].class);

        if (copy == null) {
            throw new IllegalStateException(String.format("Missing copy property on service version %s!", version.getName()));
        }

        this.download(version.getUrl(), workingDirectory.resolve(DOWNLOAD_ARTIFACT_NAME));

        List<String> parameters = version.getProperties().get("parameters", STRING_LIST_TYPE, new ArrayList<>());

        List<String> processArguments = new ArrayList<>();
        processArguments.add(CloudNet.getInstance().getConfig().getJVMCommand());
        processArguments.add("-jar");
        processArguments.add(DOWNLOAD_ARTIFACT_NAME);
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

        List<Pattern> patterns = Arrays.stream(copy)
                .map(Pattern::compile)
                .collect(Collectors.toList());

        boolean copyOnce = patterns.size() == 1;

        Files.walkFileTree(workingDirectory, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                String relativePath = workingDirectory.relativize(path).toString().replace("\\", "/").toLowerCase();

                for (Pattern pattern : patterns) {
                    if (pattern.matcher(relativePath).matches()) {

                        if (copyOnce) {
                            relativePath = fileName;
                        }

                        try (OutputStream outputStream = storage.newOutputStream(targetTemplate, relativePath)) {
                            Files.copy(path, outputStream);
                        }

                        if (copyOnce && !version.isLatest()) {
                            Files.copy(path, cachePath);
                            return FileVisitResult.TERMINATE;
                        }

                    }
                }

                return FileVisitResult.CONTINUE;
            }

        });

    }

    protected void download(String url, Path targetFile) throws IOException {
        System.out.println(LanguageManager.getMessage("template-installer-downloading-begin").replace("%url%", url));

        try (InputStream inputStream = ProgressBarInputStream.wrapDownload(CloudNet.getInstance().getConsole(), new URL(url))) {
            Files.copy(inputStream, targetFile);
        }

        System.out.println(LanguageManager.getMessage("template-installer-downloading-completed").replace("%url%", url));
    }

    protected int buildProcessAndWait(List<String> arguments, Path workingDir) {
        try {
            Process process = new ProcessBuilder()
                    .command(arguments)
                    .directory(workingDir.toFile())
                    .start();

            this.runningBuildProcesses.add(process);

            OUTPUT_READER_EXECUTOR.execute(new ProcessingInstallerOutput(process.getInputStream(), System.out));
            OUTPUT_READER_EXECUTOR.execute(new ProcessingInstallerOutput(process.getErrorStream(), System.err));

            int exitCode = process.waitFor();
            this.runningBuildProcesses.remove(process);
            return exitCode;
        } catch (IOException | InterruptedException exception) {
            exception.printStackTrace();
        }
        return -1;
    }

    @Override
    public void shutdown() {
        for (Process runningBuildProcess : this.runningBuildProcesses) {
            runningBuildProcess.destroyForcibly();
        }
    }
}
