package de.dytanic.cloudnet.ext.storage.ftp.client;

import com.jcraft.jsch.*;
import de.dytanic.cloudnet.common.io.FileUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class SFTPClient implements Closeable {

    private Session session;
    private ChannelSftp channel;

    public boolean connect(String host, String username, String password, int port) {
        System.out.println("Trying to connect to SFTP-Server...");
        try {
            this.session = new JSch().getSession(username, host, port);
            this.session.setPassword(password);
            this.session.setConfig("StrictHostKeyChecking", "no");
            this.session.connect(2500);
        } catch (JSchException e) {
            e.printStackTrace();
            System.out.println("&cThere was an error while trying to connect to SFTP-server @" + host + ":" + port);
            return false;
        }

        try {
            this.channel = (ChannelSftp) this.session.openChannel("sftp");
            if (this.channel == null) {
                this.close();
                System.out.println("&cThere was an error while opening the SFTP-session with the user " + username);
                return false;
            }
            this.channel.connect();
        } catch (JSchException e) {
            e.printStackTrace();
            System.out.println("&cThere was an error while opening the SFTP-session with the user " + username);
            return false;
        }

        if (this.isConnected()) {
            System.out.println("&aSuccessfully connected to SFTP @" + host + ":" + port);
        }

        return this.isConnected();
    }

    public boolean isConnected() {
        return this.session != null && this.session.isConnected() && this.channel != null && this.channel.isConnected();
    }

    @Override
    public void close() {
        try {
            if (this.channel != null) {
                this.channel.disconnect();
                this.channel = null;
            }
        } finally {
            if (this.session != null) {
                this.session.disconnect();
                this.session = null;
            }
        }
    }

    public void createFile(String remotePath) {
        try {
            this.channel.put(remotePath);
        } catch (SftpException e) {
            e.printStackTrace();
        }
    }

    public void createDirectories(String remotePath) {
        StringBuilder builder = new StringBuilder();
        for (String s : remotePath.split("/")) {
            builder.append('/').append(s);
            try {
                this.channel.mkdir(builder.toString());
            } catch (SftpException e) {
                //dir already exists
            }
        }
    }

    public void uploadFile(String localPath, String remotePath) {
        this.createParent(remotePath);
        try {
            this.channel.put(localPath, remotePath);
        } catch (SftpException e) {
            e.printStackTrace();
        }
    }

    public boolean uploadFile(Path localPath, String remotePath) {
        if (!Files.exists(localPath))
            return false;
        try (InputStream inputStream = Files.newInputStream(localPath)) {
            this.uploadFile(inputStream, remotePath);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void uploadFile(InputStream inputStream, String remotePath) {
        this.createParent(remotePath);
        try {
            this.channel.put(inputStream, remotePath);
        } catch (SftpException e) {
            e.printStackTrace();
        }
    }

    public OutputStream openOutputStream(String remotePath) {
        this.createParent(remotePath);
        try {
            return this.channel.put(remotePath);
        } catch (SftpException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void createParent(String remotePath) {
        if (remotePath.endsWith("/")) {
            remotePath = remotePath.substring(0, remotePath.length() - 1);
        }
        int slash = remotePath.lastIndexOf('/');
        if (slash > 0) {
            this.createDirectories(remotePath.substring(0, slash));
        }
    }

    public boolean downloadFile(String remotePath, String localPath) {
        try {
            this.channel.get(remotePath, localPath);
            return true;
        } catch (SftpException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean downloadFile(String remotePath, OutputStream outputStream) {
        try {
            this.channel.get(remotePath, outputStream);
            return true;
        } catch (SftpException e) {
            e.printStackTrace();
            return false;
        }
    }

    public InputStream loadFile(String remotePath) {
        try {
            return this.channel.get(remotePath);
        } catch (SftpException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean existsFile(String path) {
        try {
            SftpATTRS attrs = this.channel.stat(path);
            return attrs != null;
        } catch (SftpException e) {
            return false;
        }
    }

    public boolean existsDirectory(String path) {
        try {
            SftpATTRS attrs = this.channel.stat(path);
            return attrs != null && (attrs.isDir());
        } catch (SftpException e) {
            return false;
        }
    }

    public boolean downloadDirectory(String remotePath, String localPath) {
        return this.downloadDirectory(remotePath, localPath, null);
    }

    public boolean downloadDirectory(String remotePath, String localPath, Predicate<String> fileFilter) {
        if (!remotePath.endsWith("/")) {
            remotePath += "/";
        }
        if (!localPath.endsWith("/")) {
            localPath += "/";
        }

        try {
            Collection<ChannelSftp.LsEntry> entries = this.listFiles(remotePath);
            if (entries == null)
                return false;

            Path dir = Paths.get(localPath);
            if (Files.exists(dir)) {
                FileUtils.delete(dir.toFile());
            }
            Files.createDirectories(dir);

            for (ChannelSftp.LsEntry entry : entries) {
                if (entry.getAttrs().isDir()) {
                    if (!this.downloadDirectory(remotePath + entry.getFilename(), localPath + entry.getFilename())) {
                        return false;
                    }
                } else {
                    if (fileFilter == null || fileFilter.test(entry.getFilename())) {
                        System.out.println("get " + (remotePath + entry.getFilename()) + " to " + Paths.get(localPath, entry.getFilename()));
                        try (OutputStream outputStream = Files.newOutputStream(Paths.get(localPath, entry.getFilename()))) {
                            this.channel.get(remotePath + entry.getFilename(), outputStream);
                        }
                    }
                }
            }

        } catch (SftpException | IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean zipDirectory(String remotePath, OutputStream outputStream) {
        if (!remotePath.endsWith("/"))
            remotePath += "/";

        try {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
                return this.zip(zipOutputStream, remotePath);
            }
        } catch (SftpException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean zip(ZipOutputStream zipOutputStream, String remotePath) throws IOException, SftpException {
        Collection<ChannelSftp.LsEntry> entries = this.listFiles(remotePath);
        if (entries == null)
            return false;

        for (ChannelSftp.LsEntry entry : entries) {
            if (!entry.getAttrs().isDir() && !entry.getAttrs().isLink()) {
                zipOutputStream.putNextEntry(new ZipEntry(entry.getFilename()));
                this.channel.get(remotePath + "/" + entry.getFilename(), zipOutputStream);
                zipOutputStream.closeEntry();
            } else if (entry.getAttrs().isDir()) {
                if (!this.zip(zipOutputStream, remotePath + "/" + entry.getFilename())) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean uploadDirectory(Path localPath, String remotePath, Predicate<Path> fileFilter) {
        try {
            this.createDirectories(remotePath);

            Files.walkFileTree(
                    localPath,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            String path = remotePath + "/" + localPath.relativize(dir).toString();
                            path = path.replace("\\", "/");
                            createDirectories(path);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            if (fileFilter == null || fileFilter.test(file)) {
                                String path = remotePath + "/" + localPath.relativize(file).toString();
                                path = path.replace("/..", "").replace("\\", "/");
                                try {
                                    SFTPClient.this.channel.put(file.toString(), path);
                                } catch (SftpException e) {
                                    e.printStackTrace();
                                }
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    }
            );
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean uploadDirectory(InputStream zipped, String remotePath) {
        if (remotePath.endsWith("/")) {
            remotePath = remotePath.substring(0, remotePath.length() - 1);
        }

        this.createDirectories(remotePath);
        try (ZipInputStream zipInputStream = new ZipInputStream(zipped, StandardCharsets.UTF_8)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                this.uploadFile(zipInputStream, remotePath + "/" + zipEntry.getName());
                zipInputStream.closeEntry();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void deleteDirectory(String path) {
        this.deleteDirectory(path, null);
    }

    public boolean deleteDirectory(String path, Predicate<String> fileFilter) {
        try {
            Collection<ChannelSftp.LsEntry> entries = this.listFiles(path);
            if (entries == null)
                return false;

            for (ChannelSftp.LsEntry entry : entries) {
                if (fileFilter == null || fileFilter.test(entry.getFilename())) {
                    if (entry.getAttrs().isDir()) {
                        this.deleteDirectory(path + "/" + entry.getFilename());
                    } else {
                        try {
                            this.channel.rm(path + "/" + entry.getFilename());
                        } catch (SftpException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            this.channel.rmdir(path);
        } catch (SftpException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean deleteFile(String path) {
        try {
            this.channel.rm(path);
            return true;
        } catch (SftpException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Collection<ChannelSftp.LsEntry> listFiles(String directory) {
        Collection<ChannelSftp.LsEntry> entries = new ArrayList<>();
        try {
            this.channel.ls(directory, lsEntry -> {
                if (!lsEntry.getFilename().equals("..") && !lsEntry.getFilename().equals(".")) {
                    entries.add(lsEntry);
                }
                return 0;
            });
        } catch (SftpException e) {
            //directory does not exist
            return null;
        }
        return entries;
    }

}
