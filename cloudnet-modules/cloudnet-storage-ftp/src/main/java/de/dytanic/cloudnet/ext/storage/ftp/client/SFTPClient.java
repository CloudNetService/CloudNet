/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.ext.storage.ftp.client;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class SFTPClient implements Closeable {

  private static final LogLevel LOG_LEVEL = new LogLevel("ftp", "FTP", 1, true, true);

  private static final FTPType FTP_TYPE = FTPType.SFTP;

  private Session session;

  private ChannelSftp channel;

  public boolean connect(String host, int port, String username, String password) {
    ILogger logger = CloudNetDriver.getInstance().getLogger();

    try {
      this.session = new JSch().getSession(username, host, port);

      this.session.setPassword(password);
      this.session.setConfig("StrictHostKeyChecking", "no");

      this.session.connect(2500);
    } catch (JSchException exception) {
      logger.log(LOG_LEVEL, LanguageManager.getMessage("module-storage-ftp-connect-failed")
        .replace("%ftpType%", FTP_TYPE.toString()), exception);
      return false;
    }

    try {
      this.channel = (ChannelSftp) this.session.openChannel("sftp");

      if (this.channel == null) {
        this.close();
        return false;
      }

      this.channel.connect();
    } catch (JSchException exception) {
      logger.log(LOG_LEVEL, LanguageManager.getMessage("module-storage-ftp-connect-failed")
        .replace("%ftpType%", FTP_TYPE.toString()), exception);
      return false;
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

  public boolean createFile(String remotePath) {
    try {
      this.channel.put(remotePath);
      return true;
    } catch (SftpException exception) {
      exception.printStackTrace();
      return false;
    }
  }

  public void createDirectories(String remotePath) {
    StringBuilder builder = new StringBuilder();
    for (String pathSegment : remotePath.split("/")) {
      builder.append('/').append(pathSegment);
      try {
        this.channel.mkdir(builder.toString());
      } catch (SftpException ignored) {
        //dir already exists
      }
    }
  }

  public void uploadFile(String localPath, String remotePath) {
    this.createParent(remotePath);
    try {
      this.channel.put(localPath, remotePath);
    } catch (SftpException exception) {
      exception.printStackTrace();
    }
  }

  public boolean uploadFile(Path localPath, String remotePath) {
    if (!Files.exists(localPath)) {
      return false;
    }
    try (InputStream inputStream = Files.newInputStream(localPath)) {
      this.uploadFile(inputStream, remotePath);
      return true;
    } catch (IOException exception) {
      exception.printStackTrace();
      return false;
    }
  }

  private void uploadFile(InputStream inputStream, String remotePath) {
    this.createParent(remotePath);
    try {
      this.channel.put(inputStream, remotePath);
    } catch (SftpException exception) {
      exception.printStackTrace();
    }
  }

  public OutputStream appendOutputStream(String remotePath) {
    this.createParent(remotePath);
    try {
      return this.channel.put(remotePath, ChannelSftp.APPEND);
    } catch (SftpException exception) {
      exception.printStackTrace();
    }
    return null;
  }

  public OutputStream openOutputStream(String remotePath) {
    this.createParent(remotePath);
    try {
      return this.channel.put(remotePath);
    } catch (SftpException exception) {
      exception.printStackTrace();
    }
    return null;
  }

  private void createParent(String remotePath) {
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
    } catch (SftpException exception) {
      return false;
    }
  }

  public boolean downloadFile(String remotePath, OutputStream outputStream) {
    try {
      this.channel.get(remotePath, outputStream);
      return true;
    } catch (SftpException exception) {
      return false;
    }
  }

  public InputStream loadFile(String remotePath) {
    try {
      return this.channel.get(remotePath);
    } catch (SftpException exception) {
      return null;
    }
  }

  public boolean existsFile(String path) {
    try {
      SftpATTRS attrs = this.channel.stat(path);
      return attrs != null;
    } catch (SftpException exception) {
      return false;
    }
  }

  public boolean existsDirectory(String path) {
    try {
      SftpATTRS attrs = this.channel.stat(path);
      return attrs != null && (attrs.isDir());
    } catch (SftpException exception) {
      return false;
    }
  }


  public boolean downloadDirectory(String remotePath, String localPath) {
    if (!remotePath.endsWith("/")) {
      remotePath += "/";
    }
    if (!localPath.endsWith("/")) {
      localPath += "/";
    }

    try {
      Collection<ChannelSftp.LsEntry> entries = this.listFiles(remotePath);
      if (entries == null) {
        return false;
      }

      Path dir = Paths.get(localPath);
      if (!Files.exists(dir)) {
        Files.createDirectories(dir);
      }

      for (ChannelSftp.LsEntry entry : entries) {
        if (entry.getAttrs().isDir()) {
          if (!this.downloadDirectory(remotePath + entry.getFilename(), localPath + entry.getFilename())) {
            return false;
          }
        } else {
          try (OutputStream outputStream = Files.newOutputStream(Paths.get(localPath, entry.getFilename()))) {
            this.channel.get(remotePath + entry.getFilename(), outputStream);
          }
        }
      }

    } catch (SftpException | IOException exception) {
      exception.printStackTrace();
      return false;
    }
    return true;
  }

  public void zipDirectory(String remotePath, OutputStream outputStream) {
    if (remotePath.endsWith("/")) {
      remotePath = remotePath.substring(0, remotePath.length() - 1);
    }

    try {
      try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
        this.zip(zipOutputStream, remotePath, "");
      }
    } catch (SftpException | IOException exception) {
      exception.printStackTrace();
    }
  }

  private boolean zip(ZipOutputStream zipOutputStream, String remotePath, String relativePath)
    throws IOException, SftpException {
    Collection<ChannelSftp.LsEntry> entries = this.listFiles(remotePath);
    if (entries == null) {
      return false;
    }

    for (ChannelSftp.LsEntry entry : entries) {
      if (!entry.getAttrs().isDir() && !entry.getAttrs().isLink()) {
        zipOutputStream.putNextEntry(new ZipEntry(relativePath + "/" + entry.getFilename()));
        this.channel.get(remotePath + "/" + entry.getFilename(), zipOutputStream);
        zipOutputStream.closeEntry();
      } else if (entry.getAttrs().isDir()) {
        if (!this
          .zip(zipOutputStream, remotePath + "/" + entry.getFilename(), relativePath + "/" + entry.getFilename())) {
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
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            String path = remotePath + "/" + localPath.relativize(dir).toString();
            path = path.replace("\\", "/");

            SFTPClient.this.createDirectories(path);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (fileFilter == null || fileFilter.test(file)) {

              String path = remotePath + "/" + localPath.relativize(file).toString();
              path = path.replace("/..", "").replace("\\", "/");

              try {
                SFTPClient.this.channel.put(file.toString(), path);
              } catch (SftpException exception) {
                exception.printStackTrace();
              }

            }
            return FileVisitResult.CONTINUE;
          }
        }
      );
      return true;
    } catch (IOException exception) {
      exception.printStackTrace();
      return false;
    }
  }

  public boolean uploadDirectory(ZipInputStream zipInputStream, String remotePath) {
    if (remotePath.endsWith("/")) {
      remotePath = remotePath.substring(0, remotePath.length() - 1);
    }

    this.createDirectories(remotePath);
    try {
      ZipEntry zipEntry;
      while ((zipEntry = zipInputStream.getNextEntry()) != null) {
        this.uploadFile(zipInputStream, remotePath + "/" + zipEntry.getName());
        zipInputStream.closeEntry();
      }
      return true;
    } catch (IOException exception) {
      exception.printStackTrace();
      return false;
    }
  }


  public boolean deleteDirectory(String path) {
    try {
      Collection<ChannelSftp.LsEntry> entries = this.listFiles(path);
      if (entries == null) {
        return false;
      }

      for (ChannelSftp.LsEntry entry : entries) {

        if (entry.getAttrs().isDir()) {
          this.deleteDirectory(path + "/" + entry.getFilename());
        } else {
          try {
            this.channel.rm(path + "/" + entry.getFilename());
          } catch (SftpException exception) {
            exception.printStackTrace();
          }
        }

      }
      this.channel.rmdir(path);
    } catch (SftpException exception) {
      return false;
    }
    return true;
  }

  public boolean deleteFile(String path) {
    try {
      this.channel.rm(path);
      return true;
    } catch (SftpException exception) {
      return false;
    }
  }

  public SftpATTRS getAttrs(String path) {
    try {
      return this.channel.stat(path);
    } catch (SftpException exception) {
      // file does not exist
      return null;
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
    } catch (SftpException exception) {
      //directory does not exist
      return null;
    }
    return entries;
  }

}
