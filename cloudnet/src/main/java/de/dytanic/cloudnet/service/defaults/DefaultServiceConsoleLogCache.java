package de.dytanic.cloudnet.service.defaults;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.event.service.CloudServiceConsoleLogReceiveEntryEvent;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.IServiceConsoleLogCache;
import de.dytanic.cloudnet.service.ServiceConsoleLineHandler;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;

public final class DefaultServiceConsoleLogCache implements IServiceConsoleLogCache {

    private final byte[] buffer = new byte[1024];
    private final Queue<String> cachedLogMessages = new ConcurrentLinkedQueue<>();

    private final ICloudService cloudService;
    private final StringBuffer stringBuffer = new StringBuffer();

    private boolean autoPrintReceivedInput;
    private boolean screenEnabled;

    private Set<ServiceConsoleLineHandler> handlers;

    private int len;

    public DefaultServiceConsoleLogCache(ICloudService cloudService) {
        this.cloudService = cloudService;
    }

    @Override
    public synchronized IServiceConsoleLogCache update() {
        if (this.cloudService.getLifeCycle() == ServiceLifeCycle.RUNNING && this.cloudService.isAlive() && this.cloudService.getProcess() != null) {
            this.readStream(this.cloudService.getProcess().getInputStream(), false);
            this.readStream(this.cloudService.getProcess().getErrorStream(), CloudNet.getInstance().getConfig().isPrintErrorStreamLinesFromServices());
        }
        return this;
    }

    private synchronized void readStream(InputStream inputStream, boolean printErrorIntoConsole) {
        try {
            while (inputStream.available() > 0 && (this.len = inputStream.read(this.buffer, 0, this.buffer.length)) != -1) {
                this.stringBuffer.append(new String(this.buffer, 0, this.len, StandardCharsets.UTF_8));
            }

            String stringText = this.stringBuffer.toString();
            if (!stringText.contains("\n") && !stringText.contains("\r")) {
                return;
            }

            for (String input : stringText.split("\r")) {
                for (String text : input.split("\n")) {
                    if (!text.trim().isEmpty()) {
                        this.addCachedItem(text, printErrorIntoConsole);
                    }
                }
            }

            this.stringBuffer.setLength(0);

        } catch (Exception ignored) {
            this.stringBuffer.setLength(0);
        }
    }

    private void addCachedItem(String text, boolean printErrorIntoConsole) {
        if (text == null) {
            return;
        }
        ServiceEnvironmentType environment = this.cloudService.getServiceConfiguration().getProcessConfig().getEnvironment();
        String trimmedText = text.trim();
        if (!environment.getIgnoredConsoleLines().isEmpty() && environment.getIgnoredConsoleLines().contains(trimmedText)) {
            return;
        }

        while (this.cachedLogMessages.size() >= CloudNet.getInstance().getConfig().getMaxServiceConsoleLogCacheSize()) {
            this.cachedLogMessages.poll();
        }

        this.cachedLogMessages.offer(text);
        if (this.handlers != null && !this.handlers.isEmpty()) {
            for (ServiceConsoleLineHandler handler : this.handlers) {
                handler.handleLine(this, text);
            }
        }

        CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServiceConsoleLogReceiveEntryEvent(this.cloudService.getServiceInfoSnapshot(), text, printErrorIntoConsole));

        if (this.autoPrintReceivedInput || this.screenEnabled || printErrorIntoConsole) {
            CloudNetDriver.getInstance().getLogger().log((printErrorIntoConsole ? LogLevel.WARNING : LogLevel.INFO), "[" + this.cloudService.getServiceId().getName() + "] " + text);
        }
    }

    public Queue<String> getCachedLogMessages() {
        return this.cachedLogMessages;
    }

    public byte[] getBuffer() {
        return this.buffer;
    }

    public StringBuffer getStringBuffer() {
        return this.stringBuffer;
    }

    public ICloudService getCloudService() {
        return this.cloudService;
    }

    public int getLen() {
        return this.len;
    }

    public boolean isAutoPrintReceivedInput() {
        return this.autoPrintReceivedInput;
    }

    public void setAutoPrintReceivedInput(boolean autoPrintReceivedInput) {
        this.autoPrintReceivedInput = autoPrintReceivedInput;
    }

    @Override
    public boolean isScreenEnabled() {
        return this.screenEnabled;
    }

    @Override
    public void setScreenEnabled(boolean screenEnabled) {
        this.screenEnabled = screenEnabled;
    }

    @Override
    public void addHandler(ServiceConsoleLineHandler handler) {
        if (this.handlers == null) {
            this.handlers = new CopyOnWriteArraySet<>();
        }
        this.handlers.add(handler);
    }

    @Override
    public void removeHandler(ServiceConsoleLineHandler handler) {
        if (this.handlers != null) {
            this.handlers.remove(handler);
        }
    }
}