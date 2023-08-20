/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.wrapper.network.chunk;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.github.benmanes.caffeine.cache.Scheduler;
import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.chunk.ChunkedPacketSessionOpenEvent;
import eu.cloudnetservice.driver.network.chunk.ChunkedPacketHandler;
import eu.cloudnetservice.driver.network.chunk.data.ChunkSessionInformation;
import eu.cloudnetservice.driver.network.chunk.defaults.DefaultFileChunkedPacketHandler;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public final class TemplateStorageCallbackListener implements ChunkedPacketHandler.Callback {

  private final Cache<UUID, Task<InputStream>> activeSessions;

  @Inject
  public TemplateStorageCallbackListener() {
    // We have 5 minutes to receive a file
    this.activeSessions = Caffeine.newBuilder()
      .expireAfterWrite(5, TimeUnit.MINUTES)
      .scheduler(Scheduler.systemScheduler())
      .removalListener(this.newRemovalListener()).build();
  }

  private RemovalListener<UUID, Task<InputStream>> newRemovalListener() {
    return ($, value, cause) -> {
      if (cause.wasEvicted() && value != null) {
        value.completeExceptionally(new TimeoutException());
      }
    };
  }

  @EventListener
  public void handle(@NonNull ChunkedPacketSessionOpenEvent event) {
    if (event.session().transferChannel().equals("request_template_file_result")) {
      if (!this.sessionExists(event.session().sessionUniqueId())) {
        // No point in continuing when there is no running session.
        // Might want to add some logging here as this is almost certainly caused by an error
        return;
      }
      event.handler(new DefaultFileChunkedPacketHandler(
        event.session(),
        this,
        FileUtil.createTempFile(event.session().sessionUniqueId().toString())));
    }
  }

  /**
   * This waits for a session started with {@link #startSession(UUID)} to complete. This also stops the session
   *
   * @param sessionId the session id
   * @return the {@link InputStream} for the file in the session, or null if session was not found or failed
   */
  public @Nullable InputStream waitForFile(UUID sessionId) {
    var task = this.activeSessions.getIfPresent(sessionId);
    if (task == null) {
      return null;
    }
    var stream = task.getDef(null);
    // Invalidate the session. No need to keep it in memory at this point
    this.activeSessions.invalidate(sessionId);
    return stream;
  }

  /**
   * Stops the session
   *
   * @param sessionId the session id
   */
  public void stopSession(UUID sessionId) {
    this.activeSessions.invalidate(sessionId);
  }

  /**
   * Check if a session exists
   *
   * @param sessionId the session id
   * @return whether the session exists
   */
  public boolean sessionExists(UUID sessionId) {
    return this.activeSessions.getIfPresent(sessionId) != null;
  }

  /**
   * This starts a session. This session will expire after 5 Minutes. {@link #stopSession(UUID)} should be called after
   * this to prevent using memory until the session expires
   *
   * @param sessionId the session id
   */
  public void startSession(UUID sessionId) {
    this.activeSessions.put(sessionId, new Task<>());
  }

  @Override
  public boolean autoClose() {
    // We want to close this manually to maintain API
    return false;
  }

  @Override
  public void handleSessionComplete(ChunkSessionInformation information, InputStream dataInput) {
    var sessionId = information.sessionUniqueId();
    var task = this.activeSessions.getIfPresent(sessionId);
    // Complete the session if we find it
    if (task != null) {
      task.complete(dataInput);
    }
  }
}
