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

package eu.cloudnetservice.ext.sftp;

import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import net.schmizz.concurrent.Promise;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPEngine;
import org.jetbrains.annotations.NotNull;

public class SFTPClientPool {

  private static final Logger LOGGER = LogManager.getLogger(SFTPClientPool.class);

  private final int maxClients;
  private final Callable<SSHClient> clientFactory;

  private final ReentrantLock lock = new ReentrantLock();
  private final AtomicInteger createdClients = new AtomicInteger();
  private final Queue<SFTPClientWrapper> pooledClients = new LinkedList<>();
  private final Queue<Promise<SFTPClientWrapper, RuntimeException>> clientReturnWaiters = new LinkedList<>();

  public SFTPClientPool(int maxClients, @NotNull Callable<SSHClient> clientFactory) {
    this.maxClients = maxClients;
    this.clientFactory = clientFactory;
  }

  public @NotNull SFTPClientWrapper takeClient() {
    try {
      this.lock.lock();
      // try to get a client from the pool
      SFTPClientWrapper client = this.pooledClients.poll();
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
      Promise<SFTPClientWrapper, RuntimeException> promise = new Promise<>(
        "",
        RuntimeException::new,
        NopLoggerFactory.INSTANCE);
      this.clientReturnWaiters.add(promise);
      // wait for the promise to deliver
      return promise.retrieve();
    } finally {
      this.lock.unlock();
    }
  }

  public void returnClient(@NotNull SFTPClientWrapper client) {
    try {
      this.lock.lock();
      // check if there is any promise waiting for a client
      Promise<SFTPClientWrapper, ?> promise = this.clientReturnWaiters.poll();
      // check if the client is still usable
      if (client.getSFTPEngine().getSubsystem().isOpen()) {
        if (promise != null) {
          // deliver the client directly to the promise
          promise.deliver(client);
        } else {
          // return the client to the pool
          this.pooledClients.add(client);
        }
      } else {
        // not usable - close the client instead of returning it
        client.doClose();
        // try to create a new client if a promise is waiting
        if (promise != null) {
          try {
            // try to create the client
            promise.deliver(this.createAndRegisterClient());
            return;
          } catch (Exception exception) {
            // unable to create the client - log the exception and return
            LOGGER.severe("Unable to create new client to deliver waiting promise", exception);
          }
        }
        // no promise waiting or unable to create a new client - count the created clients down
        this.createdClients.decrementAndGet();
      }
    } finally {
      this.lock.unlock();
    }
  }

  private @NotNull SFTPClientWrapper createAndRegisterClient() throws Exception {
    // try to create the client first
    SFTPClientWrapper client = new SFTPClientWrapper(new SFTPEngine(this.clientFactory.call()).init());
    // the client was created successfully - increase the created count
    this.createdClients.incrementAndGet();
    // use that client now
    return client;
  }

  public final class SFTPClientWrapper extends SFTPClient {

    public SFTPClientWrapper(SFTPEngine engine) {
      super(engine);
    }

    @Override
    public void close() {
      SFTPClientPool.this.returnClient(this);
    }

    public void doClose() {
      try {
        super.close();
      } catch (Exception exception) {
        LOGGER.severe("Unable to force-close underlying SFTP engine", exception);
      }
    }
  }
}
