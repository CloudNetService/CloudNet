/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnet.common.logging;

import lombok.Getter;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * All println and print methods can be executed completely asynchronously at each instance of this class,
 * and these are then executed synchronously in the single thread.
 * It should optimize performance and avoid blocking between thread contexts
 * <p>
 * The actual console output is still executed in a thread where its priority
 * is as low as possible to affect the program even less
 */
@Getter
public class AsyncPrintStream extends PrintStream {

    static final BlockingQueue<Runnable> ASYNC_QUEUE = new LinkedBlockingQueue<>();

    private static final Thread worker = new Thread() {

        {
            setName("AsyncPrint-Thread");
            setPriority(Thread.MIN_PRIORITY);
            setDaemon(true);
            start();
        }

        @Override
        public void run()
        {
            while (!isInterrupted())
            {
                try
                {
                    Runnable runnable = ASYNC_QUEUE.take();
                    runnable.run();
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
    };

    public AsyncPrintStream(OutputStream out) throws UnsupportedEncodingException
    {
        super(out, true, StandardCharsets.UTF_8.name());
    }

    private void println0()
    {
        super.println();
    }

    @Override
    public void println()
    {
        ASYNC_QUEUE.offer(this::println0);
    }

    private void println0(int x)
    {
        super.println(x);
    }

    @Override
    public void println(int x)
    {
        ASYNC_QUEUE.offer(() -> println0(x));
    }

    private void println0(String x)
    {
        super.println(x);
    }

    @Override
    public void println(String x)
    {
        ASYNC_QUEUE.offer(() -> println0(x));
    }

    private void println0(long x)
    {
        super.println(x);
    }

    @Override
    public void println(long x)
    {
        ASYNC_QUEUE.offer(() -> println0(x));
    }

    private void println0(char x)
    {
        super.println(x);
    }

    @Override
    public void println(char x)
    {
        ASYNC_QUEUE.offer(() -> println0(x));
    }

    private void println0(double x)
    {
        super.println(x);
    }

    @Override
    public void println(double x)
    {
        ASYNC_QUEUE.offer(() -> println0(x));
    }

    private void println0(float x)
    {
        super.println(x);
    }

    @Override
    public void println(float x)
    {
        ASYNC_QUEUE.offer(() -> println0(x));
    }

    private void println0(Object x)
    {
        super.println(x);
    }

    @Override
    public void println(Object x)
    {
        ASYNC_QUEUE.offer(() -> println0(x));
    }

    private void println0(char[] x)
    {
        super.println(x);
    }

    @Override
    public void println(char[] x)
    {
        ASYNC_QUEUE.offer(() -> println0(x));
    }

    private void println0(boolean x)
    {
        super.println(x);
    }

    @Override
    public void println(boolean x)
    {
        ASYNC_QUEUE.offer(() -> println0(x));
    }

    /* ============================================== */

    private void print0(int x)
    {
        super.print(x);
    }

    @Override
    public void print(int x)
    {
        if (!isWorkerThread())
            ASYNC_QUEUE.offer(() -> print0(x));
        else
            super.print(x);
    }

    private void print0(String x)
    {
        super.print(x);
    }

    @Override
    public void print(String x)
    {
        if (!isWorkerThread())
            ASYNC_QUEUE.offer(() -> print0(x));
        else
            super.print(x);
    }

    private void print0(long x)
    {
        super.print(x);
    }

    @Override
    public void print(long x)
    {
        if (!isWorkerThread())
            ASYNC_QUEUE.offer(() -> print0(x));
        else
            super.print(x);
    }

    private void print0(char x)
    {
        super.print(x);
    }

    @Override
    public void print(char x)
    {
        if (!isWorkerThread())
            ASYNC_QUEUE.offer(() -> print0(x));
        else
            super.print(x);
    }

    private void print0(double x)
    {
        super.print(x);
    }

    @Override
    public void print(double x)
    {
        if (!isWorkerThread())
            ASYNC_QUEUE.offer(() -> print0(x));
        else
            super.print(x);
    }

    private void print0(float x)
    {
        super.print(x);
    }

    @Override
    public void print(float x)
    {
        if (!isWorkerThread())
            ASYNC_QUEUE.offer(() -> print0(x));
        else
            super.print(x);
    }

    private void print0(Object x)
    {
        super.print(x);
    }

    @Override
    public void print(Object x)
    {
        if (!isWorkerThread())
            ASYNC_QUEUE.offer(() -> print0(x));
        else
            super.print(x);
    }

    private void print0(char[] x)
    {
        super.print(x);
    }

    @Override
    public void print(char[] x)
    {
        if (!isWorkerThread())
            ASYNC_QUEUE.offer(() -> print0(x));
        else
            super.print(x);
    }

    private void print0(boolean x)
    {
        super.print(x);
    }

    @Override
    public void print(boolean x)
    {
        if (!isWorkerThread())
            ASYNC_QUEUE.offer(() -> print0(x));
        else
            super.print(x);
    }

    /*= -------------------------------------------------------- =*/

    private boolean isWorkerThread()
    {
        return Thread.currentThread() == worker;
    }
}