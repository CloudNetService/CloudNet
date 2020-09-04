package de.dytanic.cloudnet.ext.signs.configuration.entry;

import de.dytanic.cloudnet.ext.signs.SignLayout;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

@ToString
@EqualsAndHashCode
public class SignConfigurationEntry {

    protected String targetGroup;
    protected boolean switchToSearchingWhenServiceIsFull;
    protected double knockbackDistance = 1.0;
    protected double knockbackStrength = 0.8;
    protected Collection<SignConfigurationTaskEntry> taskLayouts;
    protected SignLayout defaultOnlineLayout, defaultEmptyLayout, defaultFullLayout;
    protected SignLayoutConfiguration startingLayouts, searchLayouts;

    public SignConfigurationEntry(String targetGroup, boolean switchToSearchingWhenServiceIsFull, Collection<SignConfigurationTaskEntry> taskLayouts, SignLayout defaultOnlineLayout, SignLayout defaultEmptyLayout, SignLayout defaultFullLayout, SignLayoutConfiguration startingLayouts, SignLayoutConfiguration searchLayouts) {
        this.targetGroup = targetGroup;
        this.switchToSearchingWhenServiceIsFull = switchToSearchingWhenServiceIsFull;
        this.taskLayouts = taskLayouts;
        this.defaultOnlineLayout = defaultOnlineLayout;
        this.defaultEmptyLayout = defaultEmptyLayout;
        this.defaultFullLayout = defaultFullLayout;
        this.startingLayouts = startingLayouts;
        this.searchLayouts = searchLayouts;
    }

    public SignConfigurationEntry() {
    }

    public static SignConfigurationEntry createDefault(String targetGroup, String onlineBlockType, String fullBlockType, String startingBlock, String searchingBlock) {
        return new SignConfigurationEntry(
                targetGroup,
                true,
                Collections.singletonList(new SignConfigurationTaskEntry(
                        "Template_Group_Layout",
                        new SignLayout(
                                new String[]{
                                        "&eLobby &0- &e%task_id%",
                                        "&8[&eLOBBY&8]",
                                        "%online_players% / %max_players%",
                                        "%motd%"
                                },
                                onlineBlockType,
                                -1
                        ),
                        new SignLayout(
                                new String[]{
                                        "&7Lobby &0- &7%task_id%",
                                        "&8[&7LOBBY&8]",
                                        "%online_players% / %max_players%",
                                        "%motd%"
                                },
                                onlineBlockType,
                                -1
                        ),
                        new SignLayout(
                                new String[]{
                                        "&6Lobby &0- &6%task_id%",
                                        "&8[&6PRIME&8]",
                                        "%online_players% / %max_players%",
                                        "%motd%"
                                },
                                fullBlockType,
                                -1
                        )
                )),
                new SignLayout(
                        new String[]{
                                "-* %name% *-",
                                "&8[&eLOBBY&8]",
                                "%online_players% / %max_players%",
                                "%motd%"
                        },
                        onlineBlockType,
                        -1
                ),
                new SignLayout(
                        new String[]{
                                "-* %name% *-",
                                "&8[&7LOBBY&8]",
                                "%online_players% / %max_players%",
                                "%motd%"
                        },
                        onlineBlockType,
                        -1
                ),
                new SignLayout(
                        new String[]{
                                "-* %name% *-",
                                "&8[&6PRIME&8]",
                                "%online_players% / %max_players%",
                                "%motd%"
                        },
                        fullBlockType,
                        -1
                ),
                new SignLayoutConfiguration(
                        Arrays.asList(
                                createDefaultStartingLayout(startingBlock),
                                createDefaultStartingLayout(startingBlock),
                                createDefaultStartingLayout(startingBlock),
                                createDefaultStartingLayout(startingBlock)
                        ),
                        2
                ),
                new SignLayoutConfiguration(
                        Arrays.asList(
                                createDefaultWaitingLayout(searchingBlock),
                                createDefaultWaitingLayout(searchingBlock),
                                createDefaultWaitingLayout(searchingBlock),
                                createDefaultWaitingLayout(searchingBlock)
                        ),
                        2
                ));

    }

    private static SignLayout createDefaultStartingLayout(String startingBlock) {
        return createDefaultLayout(
                new String[]{
                        "",
                        "Server",
                        "starting ...",
                        ""
                },
                startingBlock
        );
    }

    private static SignLayout createDefaultWaitingLayout(String searchingBlock) {
        return createDefaultLayout(
                new String[]{
                        "",
                        "Waiting for",
                        "server ...",
                        ""
                },
                searchingBlock
        );
    }

    private static SignLayout createDefaultLayout(String[] lines, String block) {
        return new SignLayout(lines, block, -1);
    }

    public String getTargetGroup() {
        return this.targetGroup;
    }

    public void setTargetGroup(String targetGroup) {
        this.targetGroup = targetGroup;
    }

    public boolean isSwitchToSearchingWhenServiceIsFull() {
        return this.switchToSearchingWhenServiceIsFull;
    }

    public void setSwitchToSearchingWhenServiceIsFull(boolean switchToSearchingWhenServiceIsFull) {
        this.switchToSearchingWhenServiceIsFull = switchToSearchingWhenServiceIsFull;
    }

    public Collection<SignConfigurationTaskEntry> getTaskLayouts() {
        return this.taskLayouts;
    }

    public void setTaskLayouts(Collection<SignConfigurationTaskEntry> taskLayouts) {
        this.taskLayouts = taskLayouts;
    }

    public SignLayout getDefaultOnlineLayout() {
        return this.defaultOnlineLayout;
    }

    public void setDefaultOnlineLayout(SignLayout defaultOnlineLayout) {
        this.defaultOnlineLayout = defaultOnlineLayout;
    }

    public SignLayout getDefaultEmptyLayout() {
        return this.defaultEmptyLayout;
    }

    public void setDefaultEmptyLayout(SignLayout defaultEmptyLayout) {
        this.defaultEmptyLayout = defaultEmptyLayout;
    }

    public SignLayout getDefaultFullLayout() {
        return this.defaultFullLayout;
    }

    public void setDefaultFullLayout(SignLayout defaultFullLayout) {
        this.defaultFullLayout = defaultFullLayout;
    }

    public SignLayoutConfiguration getStartingLayouts() {
        return this.startingLayouts;
    }

    public void setStartingLayouts(SignLayoutConfiguration startingLayouts) {
        this.startingLayouts = startingLayouts;
    }

    public double getKnockbackDistance() {
        return this.knockbackDistance;
    }

    public void setKnockbackDistance(double knockbackDistance) {
        this.knockbackDistance = knockbackDistance;
    }

    public double getKnockbackStrength() {
        return this.knockbackStrength;
    }

    public void setKnockbackStrength(double knockbackStrength) {
        this.knockbackStrength = knockbackStrength;
    }

    public SignLayoutConfiguration getSearchLayouts() {
        return this.searchLayouts;
    }

    public void setSearchLayouts(SignLayoutConfiguration searchLayouts) {
        this.searchLayouts = searchLayouts;
    }

}