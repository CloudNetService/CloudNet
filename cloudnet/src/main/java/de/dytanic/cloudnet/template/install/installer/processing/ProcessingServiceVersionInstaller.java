package de.dytanic.cloudnet.template.install.installer.processing;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.installer.ServiceVersionInstaller;
import de.dytanic.cloudnet.util.VisualFileUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ProcessingServiceVersionInstaller implements ServiceVersionInstaller {

    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();
    private static final String DOWNLOAD_ARTIFACT_NAME = "download.jar";
    private static final ExecutorService OUTPUT_READER_EXECUTOR = Executors.newFixedThreadPool(2);

    @Override
    public void install(ServiceVersion version, Path workingDirectory, Callable<OutputStream[]> targetStreamCallable) throws Exception {
        String copy = version.getProperties().getString("copy");
        if (copy == null) {
            throw new IllegalStateException(String.format("Missing copy property on service version %s!", version.getName()));
        }

        List<String> parameters = version.getProperties().get("parameters", STRING_LIST_TYPE, new ArrayList<>());
        int expectedExitCode = version.getProperties().getInt("exitCode", 0);

        this.download(version.getUrl(), workingDirectory.resolve(DOWNLOAD_ARTIFACT_NAME));

        List<String> processArguments = new ArrayList<>();
        processArguments.add(CloudNet.getInstance().getConfig().getJVMCommand());
        processArguments.add("-jar");
        processArguments.add(DOWNLOAD_ARTIFACT_NAME);
        processArguments.addAll(
                parameters.stream()
                        .map(parameter -> parameter.replace("%version%", version.getName()))
                        .collect(Collectors.toList())
        );

        int exitCode = this.buildProcessAndWait(processArguments, workingDirectory);

        if (exitCode != expectedExitCode) {
            throw new IllegalStateException(String.format("Process returned unexpected exit code! Got %d, expected %d", exitCode, expectedExitCode));
        }

        Pattern pattern = Pattern.compile(copy);
        OutputStream[] targetStreams = targetStreamCallable.call();

        Files.walkFileTree(workingDirectory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                Path relativePath = workingDirectory.relativize(path);

                if (pattern.matcher(relativePath.toString().replace("\\", "/")).matches()) {

                    for (OutputStream targetStream : targetStreams) {
                        Files.copy(path, targetStream);

                        targetStream.close();
                    }

                    return FileVisitResult.TERMINATE;
                }

                return FileVisitResult.CONTINUE;
            }
        });

    }

    protected void download(String url, Path targetFile) throws IOException {
        System.out.println(LanguageManager.getMessage("template-installer-downloading-begin").replace("%url%", url));
        VisualFileUtils.downloadFile(CloudNet.getInstance().getConsole(), new URL(url), targetFile);
        System.out.println(LanguageManager.getMessage("template-installer-downloading-completed").replace("%url%", url));
    }

    protected int buildProcessAndWait(List<String> arguments, Path workingDir) {
        try {
            Process process = new ProcessBuilder()
                    .command(arguments)
                    .directory(workingDir.toFile())
                    .start();

            OUTPUT_READER_EXECUTOR.execute(new ProcessingInstallerOutput(process.getInputStream(), System.out));
            OUTPUT_READER_EXECUTOR.execute(new ProcessingInstallerOutput(process.getErrorStream(), System.err));

            return process.waitFor();
        } catch (IOException | InterruptedException exception) {
            exception.printStackTrace();
        }
        return -1;
    }

}
