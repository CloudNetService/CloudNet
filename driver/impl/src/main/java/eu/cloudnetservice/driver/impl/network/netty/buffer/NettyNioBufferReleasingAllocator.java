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

package eu.cloudnetservice.driver.impl.network.netty.buffer;

import io.netty5.buffer.AllocationType;
import io.netty5.buffer.AllocatorControl;
import io.netty5.buffer.Buffer;
import io.netty5.buffer.BufferAllocator;
import io.netty5.buffer.Drop;
import io.netty5.buffer.StandardAllocationTypes;
import io.netty5.buffer.bytebuffer.ByteBufferMemoryManager;
import io.netty5.buffer.internal.ArcDrop;
import io.netty5.buffer.internal.CleanerDrop;
import io.netty5.buffer.internal.InternalBufferUtils;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.util.function.Supplier;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A buffer allocator that is allocating nio buffers and frees them directly when they are closed.
 *
 * @since 4.0
 */
public final class NettyNioBufferReleasingAllocator implements BufferAllocator, AllocatorControl {

  private final ByteBufferMemoryManager manager;
  private volatile boolean closed;

  /**
   * Constructs a new buffer allocator instance. All instances are backed by a byte buffer memory manager.
   */
  public NettyNioBufferReleasingAllocator() {
    this.manager = new ByteBufferMemoryManager();
  }

  /**
   * Get if this allocator can free direct buffers. If this method returns false, this allocator shouldn't be used as it
   * does nothing. Use the default netty allocator instead.
   *
   * @return if this allocator is able to free direct nio buffers.
   */
  public static boolean notAbleToFreeBuffers() {
    return DirectBufferFreeDrop.DIRECT_BUFFER_CLEANER == null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull AllocationType getAllocationType() {
    return StandardAllocationTypes.OFF_HEAP;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Buffer allocate(int size) {
    if (this.closed) {
      throw InternalBufferUtils.allocatorClosedException();
    }

    InternalBufferUtils.assertValidBufferSize(size);
    return this.manager.allocateShared(this, size, _ -> {
      var freeingDrop = new DirectBufferFreeDrop(this.manager);
      return CleanerDrop.wrap(ArcDrop.wrap(freeingDrop), this.manager);
    }, StandardAllocationTypes.OFF_HEAP);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Supplier<Buffer> constBufferSupplier(byte[] bytes) {
    if (this.closed) {
      throw InternalBufferUtils.allocatorClosedException();
    }

    var constantBuffer = this.manager.allocateShared(this, bytes.length, _ -> {
      var freeingDrop = new DirectBufferFreeDrop(this.manager);
      return CleanerDrop.wrapWithoutLeakDetection(ArcDrop.wrap(freeingDrop), this.manager);
    }, StandardAllocationTypes.OFF_HEAP);
    constantBuffer.writeBytes(bytes).makeReadOnly();
    return () -> this.manager.allocateConstChild(constantBuffer);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull BufferAllocator getAllocator() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isPooling() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    this.closed = true;
  }

  /**
   * A drop implementation that ínvokes the cleaner of ByteBuffers directly upon dropping.
   *
   * @param memoryManager the memory manager that is responsible for allocation.
   * @since 4.0
   */
  private record DirectBufferFreeDrop(@NonNull ByteBufferMemoryManager memoryManager) implements Drop<Buffer> {

    private static final MethodHandle DIRECT_BUFFER_CLEANER;
    private static final Logger LOGGER = LoggerFactory.getLogger(DirectBufferFreeDrop.class);

    static {
      MethodHandle directBufferCleaner;
      try {
        // get the unsafe instance
        var unsafeClass = Class.forName("sun.misc.Unsafe");
        var theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
        theUnsafeField.setAccessible(true);
        var theUnsafe = theUnsafeField.get(null);

        // get a method handle to invoke the "invokeCleaner" method
        var lookup = MethodHandles.lookup();
        var icMethodType = MethodType.methodType(void.class, ByteBuffer.class);
        directBufferCleaner = lookup.findVirtual(unsafeClass, "invokeCleaner", icMethodType).bindTo(theUnsafe);
      } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException | IllegalAccessException ex) {
        LOGGER.warn(
          "Unable to get access to Unsafe.invokeCleaner which could result in higher memory consumption: {}",
          ex.getMessage());
        directBufferCleaner = null;
      }
      DIRECT_BUFFER_CLEANER = directBufferCleaner;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drop(@NonNull Buffer obj) {
      var recoverableMemory = (ByteBuffer) this.memoryManager.unwrapRecoverableMemory(obj);
      if (DIRECT_BUFFER_CLEANER != null && recoverableMemory.isDirect()) {
        try {
          DIRECT_BUFFER_CLEANER.invokeExact(recoverableMemory);
        } catch (Throwable exception) {
          LOGGER.debug("Unable to free direct ByteBuf using Unsafe.invokeCleaner: {}", exception.getMessage());
        }
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Drop<Buffer> fork() {
      throw new IllegalStateException("Cannot fork DirectBufferFreeDrop, must be guarded by an ArcDrop");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void attach(@NonNull Buffer obj) {
    }
  }
}
