package de.dytanic.cloudnet.driver.network.http;

import com.google.common.io.ByteStreams;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.util.FileMimeTypeHelper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StaticContentHttpHandler implements IHttpHandler {

    private final ClassLoader classLoader;
    private final String resourcesPath;

    private final Map<String, byte[]> loadedFiles = new ConcurrentHashMap<>();

    public StaticContentHttpHandler(ClassLoader classLoader, String classPath) {
        this.classLoader = classLoader;
        this.resourcesPath = classPath;
    }

    @Override
    public void handle(String path, IHttpContext context) throws Exception {
        String unmodifiedFullPath = URI.create(context.request().uri()).getPath();
        String filePath;
        if (unmodifiedFullPath.isEmpty()) {
            filePath = "/";
        } else {
            filePath = unmodifiedFullPath.replaceFirst(context.pathPrefix(), "");
        }

        // Try to handle requests without a defined file by trying for index.html
        if (filePath.endsWith("/") || filePath.isEmpty()) {
            filePath = filePath + "index.html";
        }

        byte[] content = getContentOfFile(this.resourcesPath + filePath);
        if (content != null) {
            String fileMimeType = FileMimeTypeHelper.getFileType(filePath);
            fileMimeType = fileMimeType + "; charset=UTF-8";
            context.response()
                    .statusCode(HttpResponseCode.HTTP_OK)
                    .header("Content-Type", fileMimeType)
                    .body(content);
            context.closeAfter(true).cancelNext();
        }
    }

    private byte[] getContentOfFile(String path) {
        if (this.loadedFiles.containsKey(path)) {
            return this.loadedFiles.get(path);
        }

        InputStream inputStream = this.classLoader.getResourceAsStream(path);
        if (inputStream == null) {
            return null;
        }

        try {
            byte[] content = ByteStreams.toByteArray(inputStream);
            this.loadedFiles.putIfAbsent(path, content);
            inputStream.close();
            return content;
        } catch (IOException e) {
            CloudNetDriver.getInstance().getLogger().error(e.getMessage());
        }

        return null;
    }

}
