package de.dytanic.cloudnet.driver.network.protocol.chunk.server;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ITaskListener;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.chunk.ChunkedPacket;
import org.jetbrains.annotations.NotNull;

import java.nio.channels.ClosedChannelException;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public final class ServerChunkedPacketSession {

    private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();
    private static final ITaskListener<Boolean> GC_LISTENER = new ITaskListener<Boolean>() {
        @Override
        public void onComplete(ITask<Boolean> task, Boolean aBoolean) {
            System.gc();
        }

        @Override
        public void onFailure(ITask<Boolean> task, Throwable th) {
            System.gc();
        }
    };

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(THREAD_POOL::shutdownNow));
    }

    private final Queue<PendingChunk> queue = new PriorityQueue<>(Comparator.comparingInt(p -> p.packet.getChunkId()));
    private final CompletableTask<Boolean> promise = new CompletableTask<>();
    private final INetworkChannel target;

    public static @NotNull ServerChunkedPacketSession createSession(@NotNull INetworkChannel target) {
        return new ServerChunkedPacketSession(target);
    }

    private ServerChunkedPacketSession(INetworkChannel target) {
        this.target = target;
        this.promise.addListener(GC_LISTENER);
    }

    public @NotNull CompletableTask<Boolean> getPromise() {
        return this.promise;
    }

    public @NotNull INetworkChannel getTarget() {
        return this.target;
    }

    public void enqueue(@NotNull ChunkedPacket packet) {
        this.queue.add(new PendingChunk(packet));
    }

    public int getRemainingChunkCount() {
        return this.queue.size();
    }

    public void resume() {
        while (this.target.isWriteable() && !this.promise.isDone()) {
            if (!this.target.isActive()) {
                this.discard(new ClosedChannelException());
                return;
            }

            PendingChunk chunk = this.queue.poll();
            if (chunk == null) {
                this.promise.complete(true);
                break;
            }

            if (chunk.promise.isDone()) {
                continue;
            }

            this.target.sendPacketSync(chunk.packet.fillBuffer()).addListener(new ITaskListener<Void>() {
                @Override
                public void onComplete(ITask<Void> task, Void aVoid) {
                    chunk.packet.clearData();
                    chunk.promise.complete(true);
                }

                @Override
                public void onFailure(ITask<Void> task, Throwable th) {
                    chunk.packet.clearData();
                    chunk.promise.fail(th);
                    ServerChunkedPacketSession.this.discard(th);
                }
            });
        }

        if (this.queue.isEmpty()) { // all chunks were sent
            if (!this.promise.isDone()) {
                this.promise.complete(true);
            }

            return;
        }

        if (!this.promise.isDone()) { // We had no issues during the send process so try resume later
            this.awaitAvailableAndResume();
        }
    }

    private void discard(@NotNull Throwable throwable) {
        boolean failed = !this.queue.isEmpty() && !this.promise.isDone();
        while (!this.queue.isEmpty()) {
            PendingChunk pendingChunk = this.queue.poll();
            if (pendingChunk == null) {
                break;
            }

            pendingChunk.packet.clearData();
            pendingChunk.promise.fail(throwable);
        }

        if (failed) {
            this.promise.fail(throwable);
        } else {
            this.promise.complete(true);
        }
    }

    private void awaitAvailableAndResume() {
        if (THREAD_POOL.isShutdown()) {
            this.discard(new TimeoutException());
            return;
        }

        THREAD_POOL.execute(() -> {
            while (!this.target.isWriteable()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException exception) {
                    break;
                }
            }

            if (this.target.isWriteable() && this.target.isActive()) {
                this.resume();
            }
        });
    }

    private static final class PendingChunk {

        private final ChunkedPacket packet;
        private final CompletableTask<Boolean> promise = new CompletableTask<>();

        public PendingChunk(ChunkedPacket packet) {
            this.packet = packet;
        }
    }
}
