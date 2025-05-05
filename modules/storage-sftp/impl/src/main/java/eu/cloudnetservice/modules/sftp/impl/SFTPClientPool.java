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

package eu.cloudnetservice.modules.sftp.impl;

import java.io.Closeable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import lombok.NonNull;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SFTPClientPool implements Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(SFTPClientPool.class);

  private final int maxClients;
  private final Callable<SSHClient> clientFactory;

  // marker to check if this pool is still active
  private final AtomicBoolean open = new AtomicBoolean(true);

  private final AtomicInteger createdClients = new AtomicInteger();
  private final ReentrantLock clientCreateLock = new ReentrantLock();

  private final Queue<SFTPClientWrapper> pooledClients = new LinkedList<>();
  private final Queue<CompletableFuture<SFTPClientWrapper>> clientReturnWaiters = new LinkedList<>();

  public SFTPClientPool(int maxClients, @NonNull Callable<SSHClient> clientFactory) {
    this.maxClients = maxClients;
    this.clientFactory = clientFactory;
  }

  public @NonNull SFTPClientWrapper takeClient() {
    try {
      // ensure that we call this method only once at a time & that this pool is still open
      this.clientCreateLock.lock();
      this.checkClosed();

      // try to get a client from the pool
      var client = this.pooledClients.poll();
      if (client != null) {
        // validate the client
        if (client.getSFTPEngine().getSubsystem().isOpen()) {
          return client;
        }
        // we cannot use this client anymore
        client.doClose();
        // the client is no longer created - free the space
        this.createdClients.decrementAndGet();
      }

      // check if we are allowed to create more clients
      if (this.createdClients.get() < this.maxClients) {
        try {
          // try to create a new client
          return this.createAndRegisterClient();
        } catch (Exception exception) {
          throw new IllegalStateException("Unable to open new session", exception);
        }
      }

      // wait for a new client to become available
      CompletableFuture<SFTPClientWrapper> future = new CompletableFuture<>();
      this.clientReturnWaiters.add(future);
      return future.join();
    } finally {
      this.clientCreateLock.unlock();
    }
  }

  public void returnClient(@NonNull SFTPClientWrapper client) {
    try {
      // ensure that we call this method only once at a time & that this pool is still open
      this.clientCreateLock.lock();
      if (!this.open.get()) {
        client.doClose();
        return;
      }

      // check if there is any caller waiting for a client
      var waitingCreateFuture = this.clientReturnWaiters.poll();
      if (client.getSFTPEngine().getSubsystem().isOpen()) {
        if (waitingCreateFuture != null) {
          // deliver the client directly to the promise
          waitingCreateFuture.complete(client);
        } else {
          // return the client to the pool
          this.pooledClients.add(client);
        }
      } else {
        // not usable - close the client instead of returning it
        client.doClose();
        // try to create a new client if a promise is waiting
        if (waitingCreateFuture != null) {
          try {
            // try to create the client
            waitingCreateFuture.complete(this.createAndRegisterClient());
            return;
          } catch (Exception exception) {
            // unable to create the client - log the exception and return
            LOGGER.error("Unable to create new client to deliver waiting promise", exception);
          }
        }

        // no promise waiting or unable to create a new client - count the created clients down
        this.createdClients.decrementAndGet();
      }
    } finally {
      this.clientCreateLock.unlock();
    }
  }

  public boolean stillActive() {
    return this.open.get();
  }

  private @NonNull SFTPClientWrapper createAndRegisterClient() throws Exception {
    var client = new SFTPClientWrapper(new SFTPEngine(this.clientFactory.call()).init());
    this.createdClients.incrementAndGet();
    return client;
  }

  private void checkClosed() {
    if (!this.open.get()) {
      throw new IllegalStateException("pool closed");
    }
  }

  @Override
  public void close() {
    if (this.open.compareAndSet(true, false)) {
      // prevent future returns of clients to callers
      CompletableFuture<SFTPClientWrapper> future;
      while ((future = this.clientReturnWaiters.poll()) != null) {
        future.cancel(true);
      }

      // close all pooled clients
      SFTPClientWrapper client;
      while ((client = this.pooledClients.poll()) != null) {
        client.doClose();
      }

      // reset the client counter
      this.createdClients.set(0);
    }
  }

  public final class SFTPClientWrapper extends SFTPClient {

    public SFTPClientWrapper(@NonNull SFTPEngine engine) {
      super(engine);
    }

    @Override
    public void close() {
      SFTPClientPool.this.returnClient(this);
    }

    public void doClose() {
      try {
        super.close();
      } catch (Exception ignored) {
      }
    }
  }
}
