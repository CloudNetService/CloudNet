/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.driver.impl.network.netty.memory;

import io.netty5.buffer.AllocationType;
import io.netty5.buffer.AllocatorControl;
import io.netty5.buffer.Buffer;
import io.netty5.buffer.Drop;
import io.netty5.buffer.MemoryManager;
import io.netty5.buffer.StandardAllocationTypes;
import io.netty5.buffer.internal.ArcDrop;
import io.netty5.buffer.internal.InternalBufferUtils;
import io.netty5.buffer.internal.WrappingAllocation;
import java.lang.foreign.MemorySegment;
import java.util.Objects;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Custom implementation of a netty memory manager based on memory segments. Direct buffers obtained from this
 * implementation are, in contrast to the netty implementation, not obtained from a shared arena. This allows for more
 * efficient freeing of the allocated memory segments.
 *
 * @since 4.0
 */
@ApiStatus.Internal // public for SPI, shouldn't be used directly
public final class MemorySegmentMemoryManager implements MemoryManager {

  /**
   * Constructs a new buffer instance for the given memory segment.
   *
   * @param segment          the segment to use for the constructed buffer.
   * @param allocatorControl the controller for the allocator that requested the allocation.
   * @param dropDecorator    function to decorate the drop that should be used by the constructed buffer.
   * @param segmentDrop      the base drop to free the given segment, null in case there is nothing to drop.
   * @return a new buffer instance backed by the given memory segment.
   * @throws NullPointerException if the given segment, alloc control or drop decorator is null.
   */
  private static @NonNull Buffer constructBuffer(
    @NonNull MemorySegment segment,
    @NonNull AllocatorControl allocatorControl,
    @NonNull Function<Drop<Buffer>, Drop<Buffer>> dropDecorator,
    @Nullable Drop<Buffer> segmentDrop
  ) {
    var drop = Objects.requireNonNullElse(segmentDrop, InternalBufferUtils.NO_OP_DROP);
    var decoratedDrop = dropDecorator.apply(drop);
    Drop<MemorySegmentBuffer> concreteDrop = InternalBufferUtils.convert(decoratedDrop);

    var buffer = new MemorySegmentBuffer(segment, allocatorControl, concreteDrop);
    concreteDrop.attach(buffer);
    return buffer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Buffer allocateShared(
    @NonNull AllocatorControl allocatorControl,
    long size,
    @NonNull Function<Drop<Buffer>, Drop<Buffer>> dropDecorator,
    @NonNull AllocationType allocationType
  ) {
    return switch (allocationType) {
      case WrappingAllocation wa -> {
        var segment = MemorySegment.ofArray(wa.getArray());
        yield constructBuffer(segment, allocatorControl, dropDecorator, null);
      }
      case StandardAllocationTypes standardAllocation -> switch (standardAllocation) {
        case ON_HEAP -> {
          var arrSize = Math.toIntExact(size);
          var segment = MemorySegment.ofArray(new byte[arrSize]);
          yield constructBuffer(segment, allocatorControl, dropDecorator, null);
        }
        case OFF_HEAP -> {
          var segment = MemorySegmentAllocator.malloc(size);
          var freeingDrop = new MemorySegmentFreeDrop(segment);
          var arcFreeingDrop = ArcDrop.wrap(freeingDrop);
          yield constructBuffer(segment, allocatorControl, dropDecorator, arcFreeingDrop);
        }
      };
      default -> throw new IllegalArgumentException("Unsupported allocation type: " + allocationType);
    };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Buffer allocateConstChild(@NonNull Buffer readOnlyConstParent) {
    var parent = (MemorySegmentBuffer) readOnlyConstParent;
    return parent.newConstChild();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Object unwrapRecoverableMemory(@NonNull Buffer buf) {
    var buffer = (MemorySegmentBuffer) buf;
    return buffer.unsafeGetBase();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Buffer recoverMemory(
    @NonNull AllocatorControl allocatorControl,
    @NonNull Object recoverableMemory,
    @NonNull Drop<Buffer> drop
  ) {
    var segment = (MemorySegment) recoverableMemory;
    return constructBuffer(segment, allocatorControl, Function.identity(), drop);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Object sliceMemory(@NonNull Object memory, int offset, int length) {
    var segment = (MemorySegment) memory;
    return segment.asSlice(offset, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clearMemory(@NonNull Object memory) {
    var segment = (MemorySegment) memory;
    segment.fill((byte) 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int sizeOf(@NonNull Object memory) {
    var segment = (MemorySegment) memory;
    return Math.toIntExact(segment.byteSize());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String implementationName() {
    return "CloudNet_MemorySegment";
  }
}
