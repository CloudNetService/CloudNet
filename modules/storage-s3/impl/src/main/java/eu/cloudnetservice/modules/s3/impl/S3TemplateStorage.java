/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.s3.impl;

import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.FileInfo;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.modules.s3.config.S3TemplateStorageConfig;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import eu.cloudnetservice.utils.base.io.FileUtil;
import eu.cloudnetservice.utils.base.io.ListenableOutputStream;
import eu.cloudnetservice.utils.base.io.ZipUtil;
import io.vavr.CheckedConsumer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3TemplateStorage implements TemplateStorage {

  private static final Logger LOGGER = LoggerFactory.getLogger(S3TemplateStorage.class);

  private final S3Client client;
  private final S3TemplateStorageModule module;

  public S3TemplateStorage(@NonNull S3TemplateStorageModule module) {
    this.module = module;
    this.client = S3Client.builder()
      .region(Region.of(this.config().region()))
      .endpointOverride(this.config().resolveEndpointOverride())
      .dualstackEnabled(this.config().dualstackEndpointEnabled())
      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
        this.config().accessKey(),
        this.config().secretKey())))
      .serviceConfiguration(S3Configuration.builder()
        .accelerateModeEnabled(this.config().accelerateMode())
        .pathStyleAccessEnabled(this.config().pathStyleAccess())
        .chunkedEncodingEnabled(this.config().chunkedEncoding())
        .checksumValidationEnabled(this.config().checksumValidation())
        .build())
      .build();

    // init the bucket
    try {
      this.client.headBucket(HeadBucketRequest.builder().bucket(this.config().bucket()).build());
    } catch (NoSuchBucketException exception) {
      // try to create the bucket
      try {
        this.client.createBucket(CreateBucketRequest.builder().bucket(this.config().bucket()).build());
      } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException ignored) {
        // unlikely to happen - not an error
      }
    }
  }

  @Override
  public @NonNull String name() {
    return this.config().name();
  }

  @Override
  public boolean deployDirectory(
    @NonNull ServiceTemplate target,
    @NonNull Path directory,
    @Nullable Predicate<Path> filter
  ) {
    var result = new AtomicBoolean(true);
    // walk down the file tree
    FileUtil.walkFileTree(directory, ($, file) -> {
      if (!Files.isDirectory(file)) {
        try {
          var request = PutObjectRequest.builder()
            .bucket(this.config().bucket())
            .key(this.getBucketPath(target, directory, file))
            .contentType(this.getContentType(file))
            .contentLength(Files.size(file))
            .build();
          this.client.putObject(request, RequestBody.fromFile(file));
        } catch (Exception exception) {
          LOGGER.error("Exception putting file {} into s3 bucket {}",
            file.toAbsolutePath(),
            this.config().bucket(),
            exception);
          result.set(false);
        }
      }
    }, true, filter == null ? path -> true : filter::test);
    return result.get();
  }

  @Override
  public boolean deploy(@NonNull ServiceTemplate target, @NonNull InputStream inputStream) {
    var temp = ZipUtil.extract(inputStream, FileUtil.createTempFile());
    if (temp != null) {
      try {
        return this.deployDirectory(target, temp, null);
      } finally {
        FileUtil.delete(temp);
      }
    }
    return false;
  }

  @Override
  public boolean pull(@NonNull ServiceTemplate template, @NonNull Path directory) {
    try {
      // get the repo path
      var templatePath = this.getBucketPath(template);
      // list all files
      return this.listAllObjects(templatePath, null, content -> {
        // filter the content key
        var target = directory.resolve(content.key().substring(templatePath.length() + 1));

        // this prevents accidental exceptions created due to dum s3 guis which are creating "directories". As we all
        // know s3 has no directories but the guis just create an object on the s3 and put further objects on the
        // storage by just setting the file as an object. This results in responses like:
        //   - Lobby/default/plugins
        //   - Lobby/default/plugins/ProtocolLib.jar
        // As all objects are handled as files, and the first call would create a new file this will result in an
        // exception when pulling the ProtocolLib jar file as we would try to put it "into" a file.
        // This check technically might break some structures as it will prioritize directories over files, but it's the
        // best solution we have... Aside from just uploading files correctly :)
        if (Files.exists(target) && Files.isDirectory(target)) {
          return;
        }

        // check if the parent file already exists and is not a directory
        var parent = target.getParent();
        if (parent != null && Files.exists(parent) && !Files.isDirectory(parent)) {
          FileUtil.delete(parent);
        }

        // now we can just create the parent as a directory (if we need to)
        FileUtil.createDirectory(parent);

        // get the file
        var req = GetObjectRequest.builder()
          .key(content.key())
          .bucket(this.config().bucket())
          .build();
        try (InputStream stream = this.client.getObject(req); var out = Files.newOutputStream(target)) {
          FileUtil.copy(stream, out);
        }
      });
    } catch (Exception exception) {
      LOGGER.error("Exception requesting object list from bucket for downloading", exception);
      return false;
    }
  }

  @Override
  public @Nullable InputStream zipTemplate(@NonNull ServiceTemplate template) {
    var localTarget = FileUtil.createTempFile();
    if (this.pull(template, localTarget)) {
      return ZipUtil.zipToStream(localTarget);
    } else {
      return null;
    }
  }

  @Override
  public boolean delete(@NonNull ServiceTemplate template) {
    // get the contents we want to delete
    Set<ObjectIdentifier> toDelete = new HashSet<>();
    this.listAllObjects(
      this.getBucketPath(template),
      null,
      object -> toDelete.add(ObjectIdentifier.builder().key(object.key()).build()));

    try {
      // build the delete request
      var deleteRequest = DeleteObjectsRequest.builder()
        .bucket(this.config().bucket())
        .delete(Delete.builder().quiet(true).objects(toDelete).build())
        .build();
      this.client.deleteObjects(deleteRequest);
      // success
      return true;
    } catch (Exception exception) {
      LOGGER.error("Exception deleting files template files", exception);
      return false;
    }
  }

  @Override
  public boolean create(@NonNull ServiceTemplate template) {
    return true; // there are no directories
  }

  @Override
  public boolean contains(@NonNull ServiceTemplate template) {
    try {
      // check if we can get at least one object
      var request = ListObjectsV2Request.builder()
        .maxKeys(1)
        .fetchOwner(false)
        .bucket(this.config().bucket())
        .prefix(this.getBucketPath(template))
        .build();
      return !this.client.listObjectsV2(request).contents().isEmpty();
    } catch (Exception exception) {
      return false;
    }
  }

  @Override
  public @Nullable OutputStream appendOutputStream(
    @NonNull ServiceTemplate template,
    @NonNull String path
  ) throws IOException {
    ByteArrayOutputStream original;
    // try to get the old data
    try {
      var request = GetObjectRequest.builder()
        .bucket(this.config().bucket())
        .key(this.getBucketPath(template, path))
        .build();
      try (InputStream inputStream = this.client.getObject(request)) {
        original = new ByteArrayOutputStream(inputStream.available());
        inputStream.transferTo(original);
      }
    } catch (NoSuchKeyException exception) {
      original = new ByteArrayOutputStream();
    }
    // create a wrapped stream that replaced the file at the path when closing
    return this.wrapStream(template, path, original);
  }

  @Override
  public @Nullable OutputStream newOutputStream(@NonNull ServiceTemplate template, @NonNull String path) {
    return this.wrapStream(template, path, new ByteArrayOutputStream());
  }

  protected @NonNull OutputStream wrapStream(
    @NonNull ServiceTemplate template,
    @NonNull String filePath,
    @NonNull ByteArrayOutputStream original
  ) {
    return new ListenableOutputStream<>(original, stream -> {
      var content = stream.toByteArray();
      try (InputStream inputStream = new ByteArrayInputStream(content)) {
        var request = PutObjectRequest.builder()
          .bucket(this.config().bucket())
          .key(this.getBucketPath(template, filePath))
          .contentLength((long) content.length)
          .contentType("application/octet-stream")
          .build();
        this.client.putObject(request, RequestBody.fromInputStream(inputStream, content.length));
      }
    });
  }

  @Override
  public boolean createFile(@NonNull ServiceTemplate template, @NonNull String path) {
    try {
      var request = PutObjectRequest.builder()
        .bucket(this.config().bucket())
        .key(this.getBucketPath(template, path))
        .contentLength(0L)
        .contentType("text/plain")
        .build();
      this.client.putObject(request, RequestBody.fromBytes(new byte[0]));
      // success
      return true;
    } catch (Exception exception) {
      return false;
    }
  }

  @Override
  public boolean createDirectory(@NonNull ServiceTemplate template, @NonNull String path) {
    return true; // there are no folders
  }

  @Override
  public boolean hasFile(@NonNull ServiceTemplate template, @NonNull String path) {
    try {
      var request = GetObjectRequest.builder()
        .bucket(this.config().bucket())
        .key(this.getBucketPath(template, path))
        .build();
      this.client.getObject(request).close();
      // the file was present
      return true;
    } catch (NoSuchKeyException | IOException exception) {
      return false;
    }
  }

  @Override
  public boolean deleteFile(@NonNull ServiceTemplate template, @NonNull String path) {
    try {
      var request = DeleteObjectRequest.builder()
        .bucket(this.config().bucket())
        .key(this.getBucketPath(template, path))
        .build();
      this.client.deleteObject(request);
      // success
      return true;
    } catch (Exception exception) {
      return false;
    }
  }

  @Override
  public @Nullable InputStream newInputStream(@NonNull ServiceTemplate template, @NonNull String path) {
    try {
      var request = GetObjectRequest.builder()
        .bucket(this.config().bucket())
        .key(this.getBucketPath(template, path))
        .build();
      return this.client.getObject(request);
    } catch (NoSuchKeyException exception) {
      return null;
    }
  }

  @Override
  public @Nullable FileInfo fileInfo(@NonNull ServiceTemplate template, @NonNull String path) {
    try {
      var bucketPath = this.getBucketPath(template, path);
      // get the object info
      var request = HeadObjectRequest.builder()
        .bucket(this.config().bucket())
        .key(bucketPath)
        .build();
      var response = this.client.headObject(request);
      // convert to a file info
      var parts = bucketPath.split("/");
      return new FileInfo(
        path,
        parts[parts.length - 1],
        false, // there are no directories
        false,
        // creating & updating are the same thing - accesses are not given by the api
        response.lastModified().toEpochMilli(),
        response.lastModified().toEpochMilli(),
        response.lastModified().toEpochMilli(),
        response.contentLength());
    } catch (NoSuchKeyException exception) {
      return null;
    }
  }

  @Override
  public @NonNull Collection<FileInfo> listFiles(
    @NonNull ServiceTemplate template,
    @NonNull String dir,
    boolean deep
  ) {
    // get the initial data we need to strip off
    var initialStrip = this.getBucketPath(template).length();
    // collect all files
    Collection<FileInfo> files = new ArrayList<>();
    this.listAllObjects(this.getBucketPath(template, dir), null, object -> {
      var parts = object.key().split("/");
      files.add(new FileInfo(
        object.key().substring(initialStrip),
        parts[parts.length - 1],
        false, // there are no directories
        false,
        object.lastModified().toEpochMilli(),
        object.lastModified().toEpochMilli(),
        object.lastModified().toEpochMilli(),
        object.size()));
    });
    // finish the collection
    return files;
  }

  @Override
  public @NonNull Collection<ServiceTemplate> templates() {
    Set<ServiceTemplate> result = new HashSet<>();
    // list all files - filter out the possible template prefixes
    this.listAllObjects("", null, object -> {
      var parts = object.key().split("/");
      if (parts.length >= 2) {
        result.add(ServiceTemplate.builder()
          .storage(this.config().name())
          .prefix(parts[0])
          .name(parts[1])
          .build());
      }
    });
    return result;
  }

  @Override
  public void close() {
    this.client.close();
  }

  @Override
  public @NonNull CompletableFuture<Boolean> deployDirectoryAsync(
    @NonNull ServiceTemplate target,
    @NonNull Path directory,
    @Nullable Predicate<Path> filter
  ) {
    return TaskUtil.supplyAsync(() -> this.deployDirectory(target, directory, filter));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> deployAsync(
    @NonNull ServiceTemplate target,
    @NonNull InputStream inputStream
  ) {
    return TaskUtil.supplyAsync(() -> this.deploy(target, inputStream));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> pullAsync(@NonNull ServiceTemplate template, @NonNull Path directory) {
    return TaskUtil.supplyAsync(() -> this.pull(template, directory));
  }

  @Override
  public @NonNull CompletableFuture<InputStream> zipTemplateAsync(@NonNull ServiceTemplate template) {
    return TaskUtil.supplyAsync(() -> this.zipTemplate(template));
  }

  @Override
  public @NonNull CompletableFuture<ZipInputStream> openZipInputStreamAsync(@NonNull ServiceTemplate template) {
    return TaskUtil.supplyAsync(() -> this.openZipInputStream(template));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> deleteAsync(@NonNull ServiceTemplate template) {
    return TaskUtil.supplyAsync(() -> this.delete(template));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> createAsync(@NonNull ServiceTemplate template) {
    return TaskUtil.supplyAsync(() -> this.create(template));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> containsAsync(@NonNull ServiceTemplate template) {
    return TaskUtil.supplyAsync(() -> this.contains(template));
  }

  @Override
  public @NonNull CompletableFuture<OutputStream> appendOutputStreamAsync(
    @NonNull ServiceTemplate template,
    @NonNull String path
  ) {
    return TaskUtil.supplyAsync(() -> this.appendOutputStream(template, path));
  }

  @Override
  public @NonNull CompletableFuture<OutputStream> newOutputStreamAsync(
    @NonNull ServiceTemplate template,
    @NonNull String path
  ) {
    return TaskUtil.supplyAsync(() -> this.newOutputStream(template, path));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> createFileAsync(@NonNull ServiceTemplate template, @NonNull String path) {
    return TaskUtil.supplyAsync(() -> this.createFile(template, path));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> createDirectoryAsync(
    @NonNull ServiceTemplate template,
    @NonNull String path
  ) {
    return TaskUtil.supplyAsync(() -> this.createDirectory(template, path));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> hasFileAsync(@NonNull ServiceTemplate template, @NonNull String path) {
    return TaskUtil.supplyAsync(() -> this.hasFile(template, path));
  }

  @Override
  public @NonNull CompletableFuture<Boolean> deleteFileAsync(@NonNull ServiceTemplate template, @NonNull String path) {
    return TaskUtil.supplyAsync(() -> this.deleteFile(template, path));
  }

  @Override
  public @NonNull CompletableFuture<InputStream> newInputStreamAsync(
    @NonNull ServiceTemplate template,
    @NonNull String path
  ) {
    return TaskUtil.supplyAsync(() -> this.newInputStream(template, path));
  }

  @Override
  public @NonNull CompletableFuture<FileInfo> fileInfoAsync(@NonNull ServiceTemplate template, @NonNull String path) {
    return TaskUtil.supplyAsync(() -> this.fileInfo(template, path));
  }

  @Override
  public @NonNull CompletableFuture<Collection<FileInfo>> listFilesAsync(
    @NonNull ServiceTemplate template,
    @NonNull String dir,
    boolean deep
  ) {
    return TaskUtil.supplyAsync(() -> this.listFiles(template, dir, deep));
  }

  @Override
  public @NonNull CompletableFuture<Collection<ServiceTemplate>> templatesAsync() {
    return TaskUtil.supplyAsync(this::templates);
  }

  @Override
  public @NonNull CompletableFuture<Void> closeAsync() {
    return TaskUtil.runAsync(this::close);
  }

  protected boolean listAllObjects(
    @NonNull String prefix,
    @Nullable String marker,
    @NonNull CheckedConsumer<S3Object> handler
  ) {
    try {
      var response = this.client.listObjectsV2(ListObjectsV2Request.builder()
        .prefix(prefix)
        .fetchOwner(false)
        .continuationToken(marker)
        .bucket(this.config().bucket())
        .build());
      // handle all results
      for (var content : response.contents()) {
        handler.accept(content);
      }

      // check if there is a need to continue
      if (response.isTruncated() && response.continuationToken() != null) {
        return this.listAllObjects(prefix, response.continuationToken(), handler);
      } else {
        // no need to continue - success!
        return true;
      }
    } catch (Throwable exception) {
      LOGGER.error(
        "Exception listing content of bucket {} with prefix {}",
        this.config().bucket(),
        prefix,
        exception);
      return false;
    }
  }

  protected @NonNull String getContentType(@NonNull Path file) {
    try {
      return Files.probeContentType(file);
    } catch (IOException exception) {
      return "application/octet-stream";
    }
  }

  protected @NonNull String getBucketPath(@NonNull ServiceTemplate template) {
    return String.format("%s/%s", template.prefix(), template.name());
  }

  protected @NonNull String getBucketPath(@NonNull ServiceTemplate template, @NonNull String subPath) {
    return String.format("%s/%s", this.getBucketPath(template), subPath.replace('\\', '/'));
  }

  protected @NonNull String getBucketPath(@NonNull ServiceTemplate template, @NonNull Path root, @NonNull Path file) {
    return this.getBucketPath(template, root.relativize(file).toString());
  }

  protected @NonNull S3TemplateStorageConfig config() {
    return this.module.config();
  }
}
