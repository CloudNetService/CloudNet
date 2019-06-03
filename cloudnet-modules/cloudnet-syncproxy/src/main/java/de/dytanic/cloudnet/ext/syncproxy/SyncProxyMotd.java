package de.dytanic.cloudnet.ext.syncproxy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncProxyMotd {

    protected String firstLine, secondLine;

    protected boolean autoSlot;

    protected int autoSlotMaxPlayersDistance;

    protected String[] playerInfo;

    protected String protocolText;

}