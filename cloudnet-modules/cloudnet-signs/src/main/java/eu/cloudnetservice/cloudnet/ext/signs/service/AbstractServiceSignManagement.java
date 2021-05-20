package eu.cloudnetservice.cloudnet.ext.signs.service;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.ext.bridge.ServiceInfoStateWatcher;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import de.dytanic.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.SignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayout;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayoutsHolder;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.cloudnet.ext.signs.util.LayoutUtil;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public abstract class AbstractServiceSignManagement<T> extends ServiceSignManagement<T> implements SignManagement {

    public static final String REQUEST_CONFIG = "signs_request_config";
    public static final String SIGN_CREATE = "signs_sign_create";
    public static final String SIGN_DELETE = "signs_sign_delete";
    public static final String SIGN_ALL_DELETE = "signs_sign_delete_all";
    public static final String SIGN_BULK_DELETE = "signs_sign_bulk_delete";

    protected static final int TPS = 20;

    protected final AtomicInteger currentTick = new AtomicInteger();
    protected final Queue<ServiceInfoSnapshot> waitingAssignments = new ConcurrentLinkedQueue<>();

    protected AbstractServiceSignManagement() {
        super(loadSignsConfiguration());

        if (this.signsConfiguration != null) {
            CloudNetDriver.getInstance().getTaskExecutor().scheduleAtFixedRate(this::tick, 0,
                    1000 / TPS, TimeUnit.MILLISECONDS);
            this.startKnockbackTask();

            CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServicesAsync().onComplete(services -> {
                for (ServiceInfoSnapshot service : services) {
                    this.handleServiceAdd(service);
                }
            });
        }
    }

    protected static @Nullable SignsConfiguration loadSignsConfiguration() {
        ChannelMessage response = ChannelMessage.builder()
                .message(REQUEST_CONFIG)
                .targetNode(Wrapper.getInstance().getNodeUniqueId())
                .build()
                .sendSingleQuery();
        return response == null ? null : response.getBuffer().readObject(SignsConfiguration.class);
    }

    @Override
    public void createSign(@NotNull Sign sign) {
        this.channelMessage(SIGN_CREATE)
                .buffer(ProtocolBuffer.create().writeObject(sign))
                .build().send();
    }

    @Override
    public void deleteSign(@NotNull WorldPosition position) {
        this.channelMessage(SIGN_DELETE)
                .buffer(ProtocolBuffer.create().writeObject(position))
                .build().send();
    }

    @Override
    public int deleteAllSigns(@NotNull String group, @Nullable String templatePath) {
        ChannelMessage response = this.channelMessage(SIGN_BULK_DELETE)
                .buffer(ProtocolBuffer.create().writeString(group).writeOptionalString(templatePath))
                .build().sendSingleQuery();
        return response == null ? 0 : response.getBuffer().readVarInt();
    }

    @Override
    public int deleteAllSigns() {
        this.channelMessage(SIGN_ALL_DELETE)
                .buffer(ProtocolBuffer.create().writeObjectCollection(this.signs.keySet()))
                .build().send();
        return this.signs.size();
    }

    @Override
    public void handleServiceAdd(@NotNull ServiceInfoSnapshot snapshot) {
        if (this.shouldAssign(snapshot)) {
            this.tryAssign(snapshot);
        }
    }

    @Override
    public void handleServiceUpdate(@NotNull ServiceInfoSnapshot snapshot) {
        if (this.shouldAssign(snapshot)) {
            Sign handlingSign = this.getSignOf(snapshot);
            if (handlingSign != null) {
                handlingSign.setCurrentTarget(snapshot);
                this.updateSign(handlingSign);
            } else {
                this.waitingAssignments.remove(snapshot);
                this.waitingAssignments.add(snapshot);
            }
        }
    }

    @Override
    public void handleServiceRemove(@NotNull ServiceInfoSnapshot snapshot) {
        if (this.shouldAssign(snapshot)) {
            Sign handlingSign = this.getSignOf(snapshot);
            if (handlingSign != null) {
                handlingSign.setCurrentTarget(null);
                this.updateSign(handlingSign);
            } else {
                this.waitingAssignments.remove(snapshot);
            }
        }
    }

    @Override
    public boolean canConnect(@NotNull Sign sign, @NotNull Function<String, Boolean> permissionChecker) {
        return false;
    }

    protected @NotNull String[] replaceLines(@NotNull Sign sign, @NotNull SignLayout layout) {
        String[] lines = layout.getLines();
        if (lines != null && lines.length == 4) {
            String[] replacedLines = new String[4];
            for (int i = 0; i < 4; i++) {
                replacedLines[i] = ChatColor.translateAlternateColorCodes('&',
                        ServiceInfoStateWatcher.replaceServiceInfo(lines[i], sign.getTargetGroup(), sign.getCurrentTarget()));
            }
            return replacedLines;
        }
        return null;
    }

    protected boolean shouldAssign(@NotNull ServiceInfoSnapshot snapshot) {
        ServiceEnvironmentType currentEnvironment = Wrapper.getInstance().getServiceId().getEnvironment();
        ServiceEnvironmentType serviceEnvironment = snapshot.getServiceId().getEnvironment();

        return (serviceEnvironment.isMinecraftJavaServer() && currentEnvironment.isMinecraftJavaServer())
                || (serviceEnvironment.isMinecraftBedrockServer() && currentEnvironment.isMinecraftBedrockServer());
    }

    protected void tryAssign(@NotNull ServiceInfoSnapshot snapshot) {
        // check if the service is already assigned to a sign
        Sign sign = this.getSignOf(snapshot);
        if (sign == null) {
            // check if there is a free sign to handle the service
            sign = this.getNextFreeSign(snapshot);
            if (sign == null) {
                // no free sign, add to the waiting services
                this.waitingAssignments.add(snapshot);
                return;
            }
        }
        // assign the service to the sign and update
        sign.setCurrentTarget(snapshot);
        this.updateSign(sign);
    }

    protected boolean checkTemplatePath(@NotNull ServiceInfoSnapshot snapshot, @NotNull Sign sign) {
        for (ServiceTemplate template : snapshot.getConfiguration().getTemplates()) {
            if (template.getTemplatePath().equals(sign.getTemplatePath())) {
                return true;
            }
        }
        return false;
    }

    protected void updateSign(@NotNull Sign sign) {
        SignConfigurationEntry ownEntry = this.getApplicableSignConfigurationEntry();
        if (ownEntry != null) {
            this.pushUpdate(sign, LayoutUtil.getLayout(ownEntry, sign, sign.getCurrentTarget()));
        } else {
            sign.setCurrentTarget(null);
        }
    }

    protected void tick() {
        this.currentTick.incrementAndGet();

        SignConfigurationEntry ownEntry = this.getApplicableSignConfigurationEntry();
        if (ownEntry != null) {
            Map<SignLayoutsHolder, Set<Sign>> signsNeedingTicking = new HashMap<>();
            for (Sign value : this.signs.values()) {
                SignLayoutsHolder holder = LayoutUtil.getLayoutHolder(ownEntry, value, value.getCurrentTarget());
                if ((this.currentTick.get() % 20) % Math.round(20D / holder.getAnimationsPerSecond()) == 0) {
                    holder.tickAndGetCurrentLayout();
                    signsNeedingTicking.computeIfAbsent(holder, s -> new HashSet<>()).add(value);
                }
            }

            for (Map.Entry<SignLayoutsHolder, Set<Sign>> entry : signsNeedingTicking.entrySet()) {
                this.pushUpdates(entry.getValue(), entry.getKey().getCurrentLayout());
            }

            if (!this.waitingAssignments.isEmpty()) {
                for (ServiceInfoSnapshot waitingAssignment : this.waitingAssignments) {
                    Sign freeSign = this.getNextFreeSign(waitingAssignment);
                    if (freeSign != null) {
                        this.waitingAssignments.remove(waitingAssignment);

                        freeSign.setCurrentTarget(waitingAssignment);
                        this.updateSign(freeSign);
                    }
                }
            }
        }
    }

    protected @Nullable Sign getNextFreeSign(@NotNull ServiceInfoSnapshot snapshot) {
        synchronized (this) {
            List<Sign> signs = new ArrayList<>(this.signs.values());
            Collections.sort(signs);

            for (Sign sign : signs) {
                if (sign.getCurrentTarget() == null && Arrays.asList(snapshot.getConfiguration().getGroups()).contains(sign.getTargetGroup())
                        && (sign.getTemplatePath() == null || this.checkTemplatePath(snapshot, sign))) {
                    return sign;
                }
            }
            return null;
        }
    }

    protected @Nullable Sign getSignOf(@NotNull ServiceInfoSnapshot snapshot) {
        for (Sign value : this.signs.values()) {
            if (value.getCurrentTarget().equals(snapshot)) {
                return value;
            }
        }
        return null;
    }

    protected abstract void pushUpdates(@NotNull Set<Sign> signs, @NotNull SignLayout layout);

    protected abstract void pushUpdate(@NotNull Sign sign, @NotNull SignLayout layout);
}
